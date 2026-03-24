package org.mosyagin.project.parser.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mosyagin.project.DatabaseQueries
import org.mosyagin.project.parser.ParsedScene
import org.mosyagin.project.parser.ScriptParser

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
 * Данные, необходимые для фиксации обновления в БД после подтверждения.
 */
data class PreviewData(
    val projectId: Long,
    val seriesNumber: Int,
    val filePath: String,
    val fullText: String,
    val createdAt: Long,
    val matches: List<SceneMatch>
)

class ScriptUpdateManager(
    private val queries: DatabaseQueries,
    private val parser: ScriptParser
) {

    /**
     * Этап 1: Анализ PDF и подготовка данных. БД не меняется.
     */
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
                return@withContext UpdateResult.Error("Сцены не найдены. Проверьте формат PDF.")
            }

            val lastScriptFile = queries.getLatestScriptVersion(projectId, seriesNumber.toLong()).executeAsOneOrNull()
            
            val matches = if (lastScriptFile == null) {
                newParsedScenes.map { SceneMatch.New(it) }
            } else {
                val oldScenes = queries.getScenesBySeries(projectId, seriesNumber.toString(), lastScriptFile.id).executeAsList()
                matchScenes(oldScenes, newParsedScenes)
            }

            val stats = calculateStats(matches)
            
            UpdateResult.Success(
                stats = stats,
                matches = matches,
                previewData = PreviewData(projectId, seriesNumber, filePath, fullText, createdAt, matches)
            )
        } catch (e: Exception) {
            UpdateResult.Error(e.message ?: "Ошибка анализа")
        }
    }

    /**
     * Этап 2: Фиксация изменений в БД.
     */
    suspend fun executeUpdate(data: PreviewData) = withContext(Dispatchers.Default) {
        queries.transaction {
            val lastScriptFile = queries.getLatestScriptVersion(data.projectId, data.seriesNumber.toLong()).executeAsOneOrNull()

            queries.insertScriptFile(
                projectId = data.projectId,
                seriesNumber = data.seriesNumber.toLong(),
                title = "Серия ${data.seriesNumber} (Ревизия от ${data.createdAt})",
                filePath = data.filePath,
                createdAt = data.createdAt,
                previousVersionId = lastScriptFile?.id,
                revisionColor = "White",
                uploadedBy = "User"
            )
            val scriptFileId = queries.lastInsertRowId().executeAsOne()

            data.matches.forEachIndexed { index, match ->
                when (match) {
                    is SceneMatch.Exact -> {
                        queries.updateSceneUserDataHeader(
                            location = match.scene.location,
                            isInterior = if (match.scene.type == "ИНТ" || match.scene.type == "INT") 1L else 0L,
                            timeOfDay = match.scene.time,
                            id = match.oldSceneUserDataId
                        )
                        saveSceneVersion(scriptFileId, match.oldSceneUserDataId, match.scene, index.toLong())
                        queries.clearSceneActors(match.oldSceneUserDataId)
                        updateSceneActors(data.projectId, match.oldSceneUserDataId, match.scene.actors)
                        
                        // Обновляем статус сиротских реквизитов для Exact матчей
                        preserveUserData(match.oldSceneUserDataId, match.scene.content)
                    }
                    is SceneMatch.Fuzzy -> {
                        queries.updateSceneUserDataHeader(
                            location = match.scene.location,
                            isInterior = if (match.scene.type == "ИНТ" || match.scene.type == "INT") 1L else 0L,
                            timeOfDay = match.scene.time,
                            id = match.oldSceneUserDataId
                        )
                        saveSceneVersion(scriptFileId, match.oldSceneUserDataId, match.scene, index.toLong())
                        queries.clearSceneActors(match.oldSceneUserDataId)
                        updateSceneActors(data.projectId, match.oldSceneUserDataId, match.scene.actors)
                        
                        // Помечаем для проверки
                        queries.updateSceneUserDataReviewStatus(1L, match.oldSceneUserDataId)
                        
                        // Обновляем статус сиротских реквизитов для Fuzzy матчей
                        preserveUserData(match.oldSceneUserDataId, match.scene.content)
                    }
                    is SceneMatch.New -> {
                        queries.insertSceneUserData(
                            data.projectId, data.seriesNumber.toString(), match.scene.sceneNumber, 
                            match.scene.location, if (match.scene.type == "ИНТ" || match.scene.type == "INT") 1L else 0L, 
                            match.scene.time, null, 0L
                        )
                        val userDataId = queries.lastInsertRowId().executeAsOne()
                        saveSceneVersion(scriptFileId, userDataId, match.scene, index.toLong())
                        updateSceneActors(data.projectId, userDataId, match.scene.actors)
                    }
                    else -> {}
                }
            }
        }
    }

    private fun matchScenes(oldScenes: List<org.mosyagin.project.GetScenesBySeries>, newScenes: List<ParsedScene>): List<SceneMatch> {
        val finalMatches = mutableListOf<SceneMatch>()
        val remainingOld = oldScenes.toMutableList()

        for (newScene in newScenes) {
            val cleanNewNum = cleanNumber(newScene.sceneNumber)
            val exactMatch = remainingOld.find { cleanNumber(it.sceneNumber) == cleanNewNum }
            
            if (exactMatch != null) {
                // Если номер совпал точно - это та же сцена, даже если текст сильно изменился
                if (QuickComparator.compareExact(exactMatch.content, newScene.content)) {
                    finalMatches.add(SceneMatch.Exact(exactMatch.id, newScene))
                } else {
                    val score = SceneFuzzyComparator.compare(exactMatch.location, newScene.location, exactMatch.content, newScene.content)
                    // Для того же номера порог очень низкий (0.2), так как это почти наверняка та же сцена
                    if (score >= 0.2) {
                        finalMatches.add(SceneMatch.Fuzzy(exactMatch.id, newScene, score))
                    } else {
                        finalMatches.add(SceneMatch.New(newScene))
                        continue 
                    }
                }
                remainingOld.remove(exactMatch)
                continue
            }

            // Если номера не совпали, ищем похожую среди оставшихся
            val fuzzyMatch = remainingOld
                .map { old -> old to SceneFuzzyComparator.compare(old.location, newScene.location, old.content, newScene.content) }
                .filter { it.second >= 0.45 } // Порог для нечеткого поиска без совпадения номера
                .maxByOrNull { it.second }

            if (fuzzyMatch != null) {
                finalMatches.add(SceneMatch.Fuzzy(fuzzyMatch.first.id, newScene, fuzzyMatch.second))
                remainingOld.remove(fuzzyMatch.first)
                continue
            }

            finalMatches.add(SceneMatch.New(newScene))
        }

        remainingOld.forEach { finalMatches.add(SceneMatch.Deleted(it.id, it.sceneNumber, it.location)) }
        return finalMatches
    }
    
    private fun cleanNumber(num: String): String {
        return num.trim().lowercase().removePrefix("сцена").trim()
    }

    private fun preserveUserData(sceneUserDataId: Long, newContent: String) {
        val props = queries.getPropsForScene(sceneUserDataId).executeAsList()
        val cleanContent = TextNormalizer.normalize(newContent)
        
        props.forEach { prop ->
            val cleanPropName = TextNormalizer.normalize(prop.name)
            val isFound = cleanContent.contains(cleanPropName)
            
            println("DEBUG: Проверка реквизита ${prop.name} в сцене. Найдено: $isFound")
            queries.updatePropOrphanedStatus(if (isFound) 0L else 1L, prop.id)
        }
    }
    
    private fun updateSceneActors(projectId: Long, sceneUserDataId: Long, actorNames: List<String>) {
        actorNames.forEach { name ->
            queries.insertActor(projectId, name)
            val actor = queries.getActorByName(projectId, name).executeAsOne()
            queries.linkActorToScene(sceneUserDataId, actor.id)
        }
    }

    private fun saveSceneVersion(scriptFileId: Long, userDataId: Long, scene: ParsedScene, positionIndex: Long) {
        queries.insertSceneVersion(
            scriptFileId = scriptFileId,
            sceneUserDataId = userDataId,
            content = scene.content,
            contentHash = calculateSha256(TextNormalizer.sanitizeForHashing(scene.content)),
            positionIndex = positionIndex
        )
    }

    private fun calculateStats(matches: List<SceneMatch>): UpdateStats {
        return UpdateStats(
            exactCount = matches.count { it is SceneMatch.Exact },
            fuzzyCount = matches.count { it is SceneMatch.Fuzzy },
            newCount = matches.count { it is SceneMatch.New },
            deletedCount = matches.count { it is SceneMatch.Deleted }
        )
    }

    private fun calculateSha256(input: String): String {
        return ""
    }
}
