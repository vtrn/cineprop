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
    data class Success(val stats: UpdateStats, val matches: List<SceneMatch>) : UpdateResult()
    object NoChanges : UpdateResult()
    data class Error(val message: String) : UpdateResult()
}

/**
 * Менеджер, дирижирующий процессом обновления сценария до новой версии.
 */
class ScriptUpdateManager(
    private val queries: DatabaseQueries,
    private val parser: ScriptParser
) {

    /**
     * Основной метод обновления сценария.
     */
    suspend fun updateEpisodeScript(
        projectId: Long,
        seriesNumber: Int,
        filePath: String,
        fullText: String,
        createdAt: Long
    ): UpdateResult = withContext(Dispatchers.Default) {
        try {
            val newParsedScenes = parser.parse(fullText, seriesNumber)
            
            // 1. Ищем последнюю версию этого эпизода в БД
            val lastScriptFile = queries.getLatestScriptVersion(projectId, seriesNumber.toLong()).executeAsOneOrNull()

            if (lastScriptFile == null) {
                // Это первая загрузка - выполняем стандартное сохранение
                saveAsInitialVersion(projectId, seriesNumber, filePath, newParsedScenes, createdAt)
                return@withContext UpdateResult.Success(
                    UpdateStats(newCount = newParsedScenes.size), 
                    newParsedScenes.map { SceneMatch.New(it) }
                )
            }

            // 2. Получаем старые сцены для матчинга
            val oldScenes = queries.getScenesBySeries(
                projectId = projectId,
                seriesNumber = seriesNumber.toString(),
                scriptFileId = lastScriptFile.id
            ).executeAsList()

            // 3. Запускаем процесс сопоставления (Matching)
            val matches = matchScenes(oldScenes, newParsedScenes)
            
            val stats = calculateStats(matches)
            if (stats.isEmpty()) return@withContext UpdateResult.NoChanges

            // 4. Сохраняем результат в БД в рамках одной транзакции
            queries.transaction {
                queries.insertScriptFile(
                    projectId = projectId,
                    seriesNumber = seriesNumber.toLong(),
                    title = "Серия $seriesNumber (Ревизия от ${createdAt})",
                    filePath = filePath,
                    createdAt = createdAt,
                    previousVersionId = lastScriptFile.id,
                    revisionColor = "White",
                    uploadedBy = "User"
                )
                val scriptFileId = queries.lastInsertRowId().executeAsOne()

                matches.forEachIndexed { index, match ->
                    when (match) {
                        is SceneMatch.Exact -> {
                            saveSceneVersion(scriptFileId, match.oldSceneUserDataId, match.scene, index.toLong())
                        }
                        is SceneMatch.Fuzzy -> {
                            saveSceneVersion(scriptFileId, match.oldSceneUserDataId, match.scene, index.toLong())
                            // Если изменения значительны, помечаем для проверки
                            if (match.score < 0.95) {
                                queries.updateSceneUserDataReviewStatus(1L, match.oldSceneUserDataId)
                            }
                            // Проверяем реквизит на "сиротство"
                            preserveUserData(match.oldSceneUserDataId, match.scene.content)
                        }
                        is SceneMatch.New -> {
                            queries.insertSceneUserData(
                                projectId = projectId,
                                seriesNumber = match.scene.seriesNumber,
                                sceneNumber = match.scene.sceneNumber,
                                location = match.scene.location,
                                isInterior = if (match.scene.type == "ИНТ") 1L else 0L,
                                timeOfDay = match.scene.time,
                                notes = null,
                                needsReview = 0L
                            )
                            val userDataId = queries.lastInsertRowId().executeAsOne()
                            saveSceneVersion(scriptFileId, userDataId, match.scene, index.toLong())
                        }
                        is SceneMatch.Deleted -> {
                            // Пока просто логируем или меняем статус. 
                            // SceneUserData остается в базе, но в новой SceneVersion её не будет.
                        }
                        else -> {}
                    }
                }
            }

            UpdateResult.Success(stats, matches)
        } catch (e: Exception) {
            UpdateResult.Error(e.message ?: "Unknown error during update")
        }
    }

    private fun matchScenes(
        oldScenes: List<org.mosyagin.project.GetScenesBySeries>,
        newScenes: List<ParsedScene>
    ): List<SceneMatch> {
        val matches = mutableListOf<SceneMatch>()
        val unmatchedNew = newScenes.toMutableList()
        val unmatchedOld = oldScenes.toMutableList()

        // Этап 1: Exact Match (QuickComparator)
        val exactIterator = unmatchedNew.iterator()
        while (exactIterator.hasNext()) {
            val new = exactIterator.next()
            val match = unmatchedOld.find { 
                it.sceneNumber == new.sceneNumber && QuickComparator.compareExact(it.content, new.content) 
            }
            if (match != null) {
                matches.add(SceneMatch.Exact(match.id, new))
                unmatchedOld.remove(match)
                exactIterator.remove()
            }
        }

        // Этап 2: Fuzzy Match (SceneFuzzyComparator)
        val fuzzyIterator = unmatchedNew.iterator()
        while (fuzzyIterator.hasNext()) {
            val new = fuzzyIterator.next()
            // Ищем по номеру сцены (самый надежный признак в кино)
            val potential = unmatchedOld.find { it.sceneNumber == new.sceneNumber }
            if (potential != null) {
                val score = SceneFuzzyComparator.compare(
                    oldSlugline = potential.location,
                    newSlugline = new.location,
                    oldContent = potential.content,
                    newContent = new.content
                )
                if (score > 0.5) { // Порог узнаваемости
                    matches.add(SceneMatch.Fuzzy(potential.id, new, score))
                    unmatchedOld.remove(potential)
                    fuzzyIterator.remove()
                }
            }
        }

        // Этап 3: Остальное
        unmatchedNew.forEach { matches.add(SceneMatch.New(it)) }
        unmatchedOld.forEach { matches.add(SceneMatch.Deleted(it.id, it.sceneNumber, it.location)) }

        return matches
    }

    private fun preserveUserData(sceneUserDataId: Long, newContent: String) {
        val props = queries.getPropsForScene(sceneUserDataId).executeAsList()
        props.forEach { prop ->
            val isFound = newContent.contains(prop.name, ignoreCase = true)
            if (!isFound) {
                queries.updatePropOrphanedStatus(1L, prop.id)
            } else {
                // Можно добавить логику обновления оффсетов, если нужно
                queries.updatePropOrphanedStatus(0L, prop.id)
            }
        }
    }

    private fun saveSceneVersion(scriptFileId: Long, userDataId: Long, scene: ParsedScene, positionIndex: Long) {
        queries.insertSceneVersion(
            scriptFileId = scriptFileId,
            sceneUserDataId = userDataId,
            content = scene.content,
            contentHash = calculateSha256(TextNormalizer.normalize(scene.content)),
            positionIndex = positionIndex
        )
    }

    private fun saveAsInitialVersion(projId: Long, serNum: Int, path: String, scenes: List<ParsedScene>, time: Long) {
        queries.transaction {
            queries.insertScriptFile(projId, serNum.toLong(), "Серия $serNum", path, time, null, "White", "User")
            val fileId = queries.lastInsertRowId().executeAsOne()
            scenes.forEachIndexed { index, scene ->
                queries.insertSceneUserData(projId, serNum.toString(), scene.sceneNumber, scene.location, if (scene.type == "ИНТ") 1L else 0L, scene.time, null, 0L)
                val udId = queries.lastInsertRowId().executeAsOne()
                saveSceneVersion(fileId, udId, scene, index.toLong())
            }
        }
    }

    private fun calculateStats(matches: List<SceneMatch>): UpdateStats {
        return UpdateStats(
            exactCount = matches.count { it is SceneMatch.Exact },
            fuzzyCount = matches.count { it is SceneMatch.Fuzzy },
            newCount = matches.count { it is SceneMatch.New },
            deletedCount = matches.count { it is SceneMatch.Deleted }
        )
    }
}
