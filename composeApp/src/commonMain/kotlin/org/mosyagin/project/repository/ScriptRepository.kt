@file:OptIn(ExperimentalTime::class)

package org.mosyagin.project.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.mosyagin.project.DatabaseQueries
import org.mosyagin.project.ScriptFile
import org.mosyagin.project.parser.ScriptParser
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

interface ScriptRepository {
    fun getScriptsForProject(projectId: Long): Flow<List<ScriptFile>>
    fun getScriptFileById(id: Long): Flow<ScriptFile?>
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
    private val parser: ScriptParser,
    private val syncRepository: SyncRepository
) : ScriptRepository {

    override fun getScriptsForProject(projectId: Long): Flow<List<ScriptFile>> =
        queries.getScriptsForProject(projectId)
            .asFlow()
            .mapToList(Dispatchers.Default)

    override fun getScriptFileById(id: Long): Flow<ScriptFile?> =
        queries.getScriptFileById(id)
            .asFlow()
            .map { it.executeAsOneOrNull() }

    @OptIn(ExperimentalTime::class)
    override suspend fun saveParsedScript(
        projectId: Long, 
        seriesNumber: Int, 
        filePath: String, 
        fullText: String,
        createdAt: Long
    ) {
        withContext(Dispatchers.Default) {
            val parsedScenes = parser.parse(fullText, seriesNumber)
            val now = Clock.System.now().toEpochMilliseconds()

            queries.transaction {
                // 1. Создаем запись о файле
                queries.insertScriptFile(
                    projectId = projectId,
                    seriesNumber = seriesNumber.toLong(),
                    title = "Серия $seriesNumber",
                    filePath = filePath,
                    createdAt = createdAt,
                    previousVersionId = null,
                    revisionColor = "White",
                    uploadedBy = "User",
                    updatedAt = now
                )

                val scriptFileId = queries.lastInsertRowId().executeAsOne()
                syncRepository.enqueueSync("INSERT", "ScriptFile", scriptFileId, null)

                // 2. Сохраняем сцены
                parsedScenes.forEachIndexed { index, scene ->
                    queries.insertSceneUserData(
                        projectId = projectId,
                        seriesNumber = seriesNumber.toLong(),
                        sceneNumber = scene.sceneNumber,
                        location = scene.location,
                        isInterior = if (scene.type == "ИНТ") 1L else 0L,
                        timeOfDay = scene.time,
                        notes = null,
                        needsReview = 0L,
                        updatedAt = now
                    )

                    val sceneUserDataId = queries.lastInsertRowId().executeAsOne()
                    syncRepository.enqueueSync("INSERT", "SceneUserData", sceneUserDataId, null)

                    queries.insertSceneVersion(
                        scriptFileId = scriptFileId,
                        sceneUserDataId = sceneUserDataId,
                        content = scene.content,
                        contentHash = "",
                        positionIndex = index.toLong(),
                        updatedAt = now
                    )
                    
                    val sceneVersionId = queries.lastInsertRowId().executeAsOne()
                    syncRepository.enqueueSync("INSERT", "SceneVersion", sceneVersionId, null)

                    scene.actors.forEach { actorName ->
                        val cleanName = actorName.trim()
                        if (cleanName.isNotEmpty()) {
                            queries.insertActor(projectId, cleanName, now)
                            val actor = queries.getActorByName(projectId, cleanName).executeAsOneOrNull()
                            if (actor != null) {
                                syncRepository.enqueueSync("INSERT", "Actor", actor.id, null)
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
                syncRepository.enqueueSync("DELETE", "ScriptFile", fileId, null)
            }
        }
    }

    override suspend fun updateScriptTitle(fileId: Long, newTitle: String) {
        withContext(Dispatchers.Default) {
            val now = Clock.System.now().toEpochMilliseconds()
            queries.updateScriptFileTitle(newTitle, now, fileId)
            syncRepository.enqueueSync("UPDATE", "ScriptFile", fileId, null)
        }
    }
}
