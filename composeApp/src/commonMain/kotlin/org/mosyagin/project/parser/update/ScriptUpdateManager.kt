package org.mosyagin.project.parser.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mosyagin.project.DatabaseQueries
import org.mosyagin.project.parser.ParsedScene
import org.mosyagin.project.parser.ScriptParser
import org.mosyagin.project.repository.SyncRepository
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Результат процесса обновления сценария.
 */
sealed class UpdateResult {
    data class Success(
        val stats: UpdateStats, 
        val matches: List<SceneMatch>,
        val previewData: PreviewData
    ) : UpdateResult()
    object NoChanges : UpdateResult()
    data class Error(val message: String) : UpdateResult()
}

/**
 * Данные для фиксации обновления.
 */
data class PreviewData(
    val projectId: Long,
    val seriesNumber: Int,
    val filePath: String,
    val fullText: String,
    val createdAt: Long,
    val matches: List<SceneMatch>
)

/**
 * ScriptUpdateManager — ядро системы версионирования SceneMatch.
 */
class ScriptUpdateManager(
    private val queries: DatabaseQueries,
    private val parser: ScriptParser,
    private val syncRepository: SyncRepository // Добавлена зависимость для синхронизации
) {

    suspend fun prepareUpdate(
        projectId: Long,
        seriesNumber: Int,
        filePath: String,
        fullText: String,
        createdAt: Long
    ): UpdateResult = withContext(Dispatchers.Default) {
        try {
            val newParsedScenes = parser.parse(fullText, seriesNumber)
            if (newParsedScenes.isEmpty()) {
                return@withContext UpdateResult.Error("Сцены не найдены.")
            }

            val lastScriptFile = queries.getLatestScriptVersion(projectId, seriesNumber.toLong()).executeAsOneOrNull()
            
            val matches = if (lastScriptFile == null) {
                newParsedScenes.map { SceneMatch.New(it) }
            } else {
                val oldScenes = queries.getScenesBySeries(projectId, seriesNumber.toLong(), lastScriptFile.id).executeAsList()
                matchScenes(oldScenes, newParsedScenes)
            }

            UpdateResult.Success(
                stats = calculateStats(matches),
                matches = matches,
                previewData = PreviewData(projectId, seriesNumber, filePath, fullText, createdAt, matches)
            )
        } catch (e: Exception) {
            UpdateResult.Error(e.message ?: "Ошибка анализа")
        }
    }

    @OptIn(ExperimentalTime::class)
    suspend fun executeUpdate(data: PreviewData) = withContext(Dispatchers.Default) {
        val now = Clock.System.now().toEpochMilliseconds()
        queries.transaction {
            val lastScriptFile = queries.getLatestScriptVersion(data.projectId, data.seriesNumber.toLong()).executeAsOneOrNull()

            queries.insertScriptFile(
                data.projectId,
                data.seriesNumber.toLong(),
                "Серия ${data.seriesNumber} (Ревизия от ${data.createdAt})",
                data.filePath,
                data.createdAt,
                lastScriptFile?.id,
                "White",
                "User",
                now
            )
            val scriptFileId = queries.lastInsertRowId().executeAsOne()
            syncRepository.enqueueSync("INSERT", "ScriptFile", scriptFileId, null)

            data.matches.forEachIndexed { index, match ->
                when (match) {
                    is SceneMatch.Exact -> {
                        queries.updateSceneUserDataHeader(
                            match.scene.location, 
                            if (match.scene.type == "ИНТ") 1L else 0L, 
                            match.scene.time, 
                            now, 
                            match.oldSceneUserDataId
                        )
                        syncRepository.enqueueSync("UPDATE", "SceneUserData", match.oldSceneUserDataId, null)
                        
                        saveSceneVersion(scriptFileId, match.oldSceneUserDataId, match.scene, index.toLong(), now)
                        queries.clearSceneActors(match.oldSceneUserDataId)
                        updateSceneActors(data.projectId, match.oldSceneUserDataId, match.scene.actors, now)
                        preserveUserData(match.oldSceneUserDataId, match.scene.content, now)
                    }
                    is SceneMatch.Fuzzy -> {
                        queries.updateSceneUserDataHeader(
                            match.scene.location, 
                            if (match.scene.type == "ИНТ") 1L else 0L, 
                            match.scene.time, 
                            now, 
                            match.oldSceneUserDataId
                        )
                        syncRepository.enqueueSync("UPDATE", "SceneUserData", match.oldSceneUserDataId, null)

                        saveSceneVersion(scriptFileId, match.oldSceneUserDataId, match.scene, index.toLong(), now)
                        queries.clearSceneActors(match.oldSceneUserDataId)
                        updateSceneActors(data.projectId, match.oldSceneUserDataId, match.scene.actors, now)
                        queries.updateSceneUserDataReviewStatus(1L, now, match.oldSceneUserDataId)
                        preserveUserData(match.oldSceneUserDataId, match.scene.content, now)
                    }
                    is SceneMatch.New -> {
                        queries.insertSceneUserData(
                            data.projectId, 
                            data.seriesNumber.toLong(), 
                            match.scene.sceneNumber, 
                            match.scene.location, 
                            if (match.scene.type == "ИНТ") 1L else 0L, 
                            match.scene.time, 
                            null, 
                            0L, 
                            now
                        )
                        val userDataId = queries.lastInsertRowId().executeAsOne()
                        syncRepository.enqueueSync("INSERT", "SceneUserData", userDataId, null)

                        saveSceneVersion(scriptFileId, userDataId, match.scene, index.toLong(), now)
                        updateSceneActors(data.projectId, userDataId, match.scene.actors, now)
                    }
                    else -> {}
                }
            }
        }
    }

    private fun matchScenes(oldScenes: List<org.mosyagin.project.GetScenesBySeries>, newScenes: List<ParsedScene>): List<SceneMatch> {
        val finalMatches = mutableListOf<SceneMatch>()
        for (newScene in newScenes) {
            val exactMatch = oldScenes.find { it.sceneNumber == newScene.sceneNumber }
            if (exactMatch != null) {
                finalMatches.add(SceneMatch.Exact(exactMatch.id, newScene))
            } else {
                finalMatches.add(SceneMatch.New(newScene))
            }
        }
        return finalMatches
    }

    private fun preserveUserData(sceneUserDataId: Long, newContent: String, now: Long) {
        val props = queries.getPropsForScene(sceneUserDataId).executeAsList()
        props.forEach { prop ->
            queries.updatePropOrphanedStatus(if (newContent.contains(prop.name, ignoreCase = true)) 0L else 1L, now, prop.id)
            syncRepository.enqueueSync("UPDATE", "Prop", prop.id, null)
        }
    }
    
    private fun updateSceneActors(projectId: Long, sceneUserDataId: Long, actorNames: List<String>, now: Long) {
        actorNames.forEach { name ->
            queries.insertActor(projectId, name, now)
            val actor = queries.getActorByName(projectId, name).executeAsOneOrNull()
            if (actor != null) {
                syncRepository.enqueueSync("INSERT", "Actor", actor.id, null)
                queries.linkActorToScene(sceneUserDataId, actor.id)
            }
        }
    }

    private fun saveSceneVersion(scriptFileId: Long, userDataId: Long, scene: ParsedScene, positionIndex: Long, now: Long) {
        queries.insertSceneVersion(scriptFileId, userDataId, scene.content, "", positionIndex, now)
        val versionId = queries.lastInsertRowId().executeAsOne()
        syncRepository.enqueueSync("INSERT", "SceneVersion", versionId, null)
    }

    private fun calculateStats(matches: List<SceneMatch>) = UpdateStats(
        exactCount = matches.count { it is SceneMatch.Exact },
        fuzzyCount = matches.count { it is SceneMatch.Fuzzy },
        newCount = matches.count { it is SceneMatch.New },
        deletedCount = matches.count { it is SceneMatch.Deleted }
    )
}
