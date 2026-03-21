package org.mosyagin.project.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.mosyagin.project.DatabaseQueries
import org.mosyagin.project.ScriptFile
import org.mosyagin.project.parser.ScriptParser

interface ScriptRepository {
    fun getScriptsForProject(projectId: Long): Flow<List<ScriptFile>>
    suspend fun saveParsedScript(
        projectId: Long, 
        seriesNumber: Int, 
        filePath: String, 
        fullText: String,
        createdAt: Long
    )
    suspend fun deleteScriptFile(fileId: Long)
    suspend fun updateScriptTitle(fileId: Long, newTitle: String)
}

class ScriptRepositoryImpl(
    private val queries: DatabaseQueries,
    private val parser: ScriptParser
) : ScriptRepository {

    override fun getScriptsForProject(projectId: Long): Flow<List<ScriptFile>> =
        queries.getScriptsForProject(projectId)
            .asFlow()
            .mapToList(Dispatchers.Default)

    override suspend fun saveParsedScript(
        projectId: Long, 
        seriesNumber: Int, 
        filePath: String, 
        fullText: String,
        createdAt: Long
    ) {
        withContext(Dispatchers.Default) {
            val parsedScenes = parser.parse(fullText, seriesNumber)

            queries.transaction {
                // 1. Создаем запись о файле
                queries.insertScriptFile(
                    projectId = projectId,
                    title = "Серия $seriesNumber",
                    filePath = filePath,
                    createdAt = createdAt,
                    previousVersionId = null,
                    revisionColor = "White",
                    uploadedBy = null
                )

                val scriptFileId = queries.lastInsertRowId().executeAsOne()

                // 2. Сохраняем сцены
                parsedScenes.forEachIndexed { index, scene ->
                    // Проверяем, существует ли уже такая сцена (по номеру серии и сцены)
                    // Для первой загрузки мы всегда создаем новую SceneUserData, 
                    // но в будущем здесь будет логика матчинга.
                    
                    queries.insertSceneUserData(
                        projectId = projectId,
                        seriesNumber = seriesNumber.toString(),
                        sceneNumber = scene.sceneNumber,
                        location = scene.location,
                        isInterior = if (scene.type == "ИНТ") 1L else 0L,
                        timeOfDay = scene.time,
                        notes = null,
                        needsReview = 0
                    )

                    val sceneUserDataId = queries.lastInsertRowId().executeAsOne()

                    queries.insertSceneVersion(
                        scriptFileId = scriptFileId,
                        sceneUserDataId = sceneUserDataId,
                        content = scene.content,
                        contentHash = "", // Можно добавить вычисление хеша
                        positionIndex = index.toLong()
                    )

                    scene.actors.forEach { actorName ->
                        val cleanName = actorName.trim()
                        if (cleanName.isNotEmpty()) {
                            queries.insertActor(projectId, cleanName)
                            val actor = queries.getActorByName(projectId, cleanName).executeAsOneOrNull()
                            if (actor != null) {
                                queries.linkActorToScene(sceneUserDataId, actor.id)
                            }
                        }
                    }
                }
            }
        }
    }

    override suspend fun deleteScriptFile(fileId: Long) {
        withContext(Dispatchers.Default) {
            queries.transaction {
                queries.deleteScenesByScriptFile(fileId)
                queries.deleteScriptFile(fileId)
            }
        }
    }

    override suspend fun updateScriptTitle(fileId: Long, newTitle: String) {
        withContext(Dispatchers.Default) {
            queries.updateScriptFileTitle(newTitle, fileId)
        }
    }
}
