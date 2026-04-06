package org.mosyagin.project.parser.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mosyagin.project.DatabaseQueries
import org.mosyagin.project.crypto.DataEncrypter
import org.mosyagin.project.generateUUID
import org.mosyagin.project.parser.ParsedScene
import org.mosyagin.project.parser.ScriptParser
import org.mosyagin.project.repository.SyncRepository
import org.mosyagin.project.util.currentTimestamp

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
    val projectId: String,
    val seriesNumber: Int,
    val filePath: String,
    val fullText: String,
    val createdAt: Long,
    val matches: List<SceneMatch>
)

/**
 * ScriptUpdateManager — ядро системы версионирования сцен.
 */
class ScriptUpdateManager(
    private val queries: DatabaseQueries,
    private val parser: ScriptParser,
    private val syncRepository: SyncRepository,
    private val encrypter: DataEncrypter
) {

    suspend fun prepareUpdate(
        projectId: String,
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
            e.printStackTrace()
            UpdateResult.Error(e.message ?: "Ошибка анализа")
        }
    }

    suspend fun executeUpdate(data: PreviewData) = withContext(Dispatchers.Default) {
        val now = currentTimestamp()
        println("ScriptUpdateManager: Executing update for project ${data.projectId}...")
        
        // 1. Предварительно подготавливаем данные вне транзакции
        val encryptedMatches = data.matches.map { match ->
            val scene = when (match) {
                is SceneMatch.Exact -> match.scene
                is SceneMatch.Fuzzy -> match.scene
                is SceneMatch.New -> match.scene
                else -> null
            }
            if (scene != null) {
                scene to encrypter.encrypt(scene.content)
            } else {
                null to null
            }
        }

        val existingActors = queries.getActorsByProject(data.projectId).executeAsList().associateBy { it.name }
        val actorsToCreate = mutableMapOf<String, String>()

        queries.transaction {
            val lastScriptFile = queries.getLatestScriptVersion(data.projectId, data.seriesNumber.toLong()).executeAsOneOrNull()

            val scriptFileId = generateUUID()
            queries.insertScriptFile(
                id = scriptFileId,
                projectId = data.projectId,
                seriesNumber = data.seriesNumber.toLong(),
                title = "Серия ${data.seriesNumber} (Ревизия от ${data.createdAt})",
                filePath = data.filePath,
                createdAt = data.createdAt,
                previousVersionId = lastScriptFile?.id,
                revisionColor = "White",
                uploadedBy = "User",
                updatedAt = now
            )
            syncRepository.enqueueSync("INSERT", "ScriptFile", scriptFileId, null)

            data.matches.forEachIndexed { index, match ->
                val (parsedScene, encryptedContent) = encryptedMatches[index]
                
                when (match) {
                    is SceneMatch.Exact -> {
                        val userDataId = match.oldSceneUserDataId
                        updateSceneHeader(userDataId, match.scene, now)
                        insertVersion(scriptFileId, userDataId, encryptedContent ?: "", index.toLong(), now)
                        
                        queries.clearSceneActors(userDataId)
                        // Мы не синхронизируем clearSceneActors, так как Supabase upsert перезапишет/дополнит, 
                        // но для чистоты можно было бы добавить DELETE в очередь.
                        
                        linkActors(data.projectId, userDataId, match.scene.actors, existingActors, actorsToCreate, now, match.scene.sceneNumber)
                    }
                    is SceneMatch.Fuzzy -> {
                        val userDataId = match.oldSceneUserDataId
                        updateSceneHeader(userDataId, match.scene, now)
                        queries.updateSceneUserDataReviewStatus(1L, now, userDataId)
                        syncRepository.enqueueSync("UPDATE", "SceneUserData", userDataId, null)
                        
                        insertVersion(scriptFileId, userDataId, encryptedContent ?: "", index.toLong(), now)
                        
                        queries.clearSceneActors(userDataId)
                        linkActors(data.projectId, userDataId, match.scene.actors, existingActors, actorsToCreate, now, match.scene.sceneNumber)
                    }
                    is SceneMatch.New -> {
                        val userDataId = generateUUID()
                        queries.insertSceneUserData(
                            id = userDataId,
                            projectId = data.projectId, 
                            seriesNumber = data.seriesNumber.toLong(), 
                            sceneNumber = match.scene.sceneNumber, 
                            location = match.scene.location, 
                            isInterior = if (match.scene.type == "ИНТ") 1L else 0L, 
                            timeOfDay = match.scene.time, 
                            notes = null, 
                            needsReview = 0L, 
                            updatedAt = now
                        )
                        syncRepository.enqueueSync("INSERT", "SceneUserData", userDataId, null)

                        insertVersion(scriptFileId, userDataId, encryptedContent ?: "", index.toLong(), now)
                        linkActors(data.projectId, userDataId, match.scene.actors, existingActors, actorsToCreate, now, match.scene.sceneNumber)
                    }
                    is SceneMatch.Deleted -> {
                    }
                    else -> {}
                }
            }
        }
        println("ScriptUpdateManager: Update finished. Triggering push...")
        syncRepository.triggerPush()
    }

    private fun updateSceneHeader(id: String, scene: ParsedScene, now: Long) {
        queries.updateSceneUserDataHeader(
            location = scene.location, 
            isInterior = if (scene.type == "ИНТ") 1L else 0L, 
            timeOfDay = scene.time, 
            updatedAt = now, 
            id = id
        )
        syncRepository.enqueueSync("UPDATE", "SceneUserData", id, null)
    }

    private fun insertVersion(scriptId: String, userDataId: String, content: String, index: Long, now: Long) {
        val versionId = generateUUID()
        queries.insertSceneVersion(
            id = versionId,
            scriptFileId = scriptId, 
            sceneUserDataId = userDataId, 
            content = content, 
            contentHash = "", 
            positionIndex = index, 
            updatedAt = now
        )
        syncRepository.enqueueSync("INSERT", "SceneVersion", versionId, null)
    }

    private fun linkActors(
        projectId: String, 
        userDataId: String, 
        names: List<String>, 
        existing: Map<String, org.mosyagin.project.Actor>,
        toCreate: MutableMap<String, String>,
        now: Long,
        sceneNum: String
    ) {
        names.forEach { name ->
            val cleanName = name.trim()
            if (cleanName.isNotEmpty()) {
                val actorId = existing[cleanName]?.id ?: toCreate[cleanName] ?: run {
                    val newId = generateUUID()
                    queries.insertActor(newId, projectId, cleanName, now)
                    syncRepository.enqueueSync("INSERT", "Actor", newId, null)
                    toCreate[cleanName] = newId
                    newId
                }
                queries.linkActorToScene(userDataId, actorId)
                // ИСПРАВЛЕНИЕ: Добавляем связь в очередь синхронизации
                syncRepository.enqueueSync("INSERT", "SceneActor", "${userDataId}|${actorId}", null)
                println("ScriptUpdateManager: Linked actor '$cleanName' to scene $sceneNum")
            }
        }
    }

    private fun matchScenes(
        oldScenes: List<org.mosyagin.project.GetScenesBySeries>, 
        newScenes: List<ParsedScene>
    ): List<SceneMatch> {
        val finalMatches = mutableListOf<SceneMatch>()
        val matchedOldIds = mutableSetOf<String>()

        for (newScene in newScenes) {
            val oldScene = oldScenes.find { it.sceneNumber == newScene.sceneNumber }
            
            if (oldScene != null) {
                matchedOldIds.add(oldScene.id)
                
                // Дешифруем старый контент для честного сравнения
                val oldContentDecrypted = encrypter.decrypt(oldScene.content) ?: ""
                
                val isContentSame = QuickComparator.compareExact(oldContentDecrypted, newScene.content)
                val isMetadataSame = oldScene.location == newScene.location &&
                                     oldScene.timeOfDay == newScene.time &&
                                     (oldScene.isInterior == 1L) == (newScene.type == "ИНТ")

                if (isContentSame && isMetadataSame) {
                    finalMatches.add(SceneMatch.Exact(oldScene.id, newScene))
                } else {
                    finalMatches.add(SceneMatch.Fuzzy(oldScene.id, newScene, 0.5))
                }
            } else {
                finalMatches.add(SceneMatch.New(newScene))
            }
        }

        // Находим те, что были в старой версии, но отсутствуют в новой
        for (oldScene in oldScenes) {
            if (oldScene.id !in matchedOldIds) {
                finalMatches.add(SceneMatch.Deleted(
                    oldSceneUserDataId = oldScene.id,
                    oldSceneNumber = oldScene.sceneNumber,
                    oldSceneTitle = oldScene.location
                ))
            }
        }

        return finalMatches
    }

    private fun calculateStats(matches: List<SceneMatch>) = UpdateStats(
        exactCount = matches.count { it is SceneMatch.Exact },
        fuzzyCount = matches.count { it is SceneMatch.Fuzzy },
        newCount = matches.count { it is SceneMatch.New },
        deletedCount = matches.count { it is SceneMatch.Deleted }
    )
}
