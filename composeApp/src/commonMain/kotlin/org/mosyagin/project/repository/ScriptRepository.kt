@file:OptIn(ExperimentalTime::class)

package org.mosyagin.project.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.mosyagin.project.DatabaseQueries
import org.mosyagin.project.ScriptFile
import org.mosyagin.project.crypto.DataEncrypter
import org.mosyagin.project.generateUUID
import org.mosyagin.project.parser.ScriptParser
import org.mosyagin.project.util.currentTimestamp
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

interface ScriptRepository {
    fun getScriptsForProject(projectId: String): Flow<List<ScriptFile>>
    fun getScriptFileById(id: String): Flow<ScriptFile?>
    suspend fun saveParsedScript(
        projectId: String, 
        seriesNumber: Int, 
        filePath: String, 
        fullText: String,
        createdAt: Long
    )
    suspend fun deleteScriptFile(fileId: String)
    suspend fun updateScriptTitle(fileId: String, newTitle: String)
}

class ScriptRepositoryImpl(
    private val queries: DatabaseQueries,
    private val parser: ScriptParser,
    private val syncRepository: SyncRepository,
    private val encrypter: DataEncrypter,
    private val activityRepository: ActivityRepository
) : ScriptRepository {

    override fun getScriptsForProject(projectId: String): Flow<List<ScriptFile>> =
        queries.getScriptsForProject(project_id = projectId)
            .asFlow()
            .mapToList(Dispatchers.IO)

    override fun getScriptFileById(id: String): Flow<ScriptFile?> =
        queries.getScriptFileById(id)
            .asFlow()
            .map { it.executeAsOneOrNull() }

    override suspend fun saveParsedScript(
        projectId: String, 
        seriesNumber: Int, 
        filePath: String, 
        fullText: String,
        createdAt: Long
    ) {
        withContext(Dispatchers.IO) {
            val parsedScenes = parser.parse(fullText, seriesNumber)
            val now = currentTimestamp()

            queries.transaction {
                val scriptFileId = generateUUID()
                queries.insertScriptFile(
                    id = scriptFileId,
                    project_id = projectId,
                    seriesNumber = seriesNumber.toLong(),
                    title = "Серия $seriesNumber",
                    filePath = filePath,
                    createdAt = createdAt,
                    previousVersionId = null,
                    revisionColor = "White",
                    uploadedBy = "User",
                    updatedAt = now
                )

                syncRepository.enqueueSync("INSERT", "ScriptFile", scriptFileId, projectId, null)

                parsedScenes.forEachIndexed { index, scene ->
                    val sceneUserDataId = generateUUID()
                    queries.insertSceneUserData(
                        id = sceneUserDataId,
                        project_id = projectId,
                        seriesNumber = seriesNumber.toLong(),
                        sceneNumber = scene.sceneNumber,
                        location = scene.location,
                        isInterior = if (scene.type == "ИНТ") 1L else 0L,
                        timeOfDay = scene.time,
                        notes = null,
                        needsReview = 0L,
                        updatedAt = now,
                        isDecrypted = 1L
                    )

                    syncRepository.enqueueSync("INSERT", "SceneUserData", sceneUserDataId, projectId, null)

                    val sceneVersionId = generateUUID()
                    val encryptedContent = encrypter.encrypt(scene.content) ?: ""
                    queries.insertSceneVersion(
                        id = sceneVersionId,
                        scriptFileId = scriptFileId,
                        sceneUserDataId = sceneUserDataId,
                        content = encryptedContent,
                        contentHash = "",
                        positionIndex = index.toLong(),
                        updatedAt = now,
                        isDecrypted = 1L
                    )
                    
                    syncRepository.enqueueSync("INSERT", "SceneVersion", sceneVersionId, projectId, null)

                    scene.actors.forEach { actorName ->
                        val cleanName = actorName.trim()
                        if (cleanName.isNotEmpty()) {
                            var actor = queries.getActorByName(project_id = projectId, name = cleanName).executeAsOneOrNull()
                            if (actor == null) {
                                val newActorId = generateUUID()
                                queries.insertActor(id = newActorId, project_id = projectId, name = cleanName, updatedAt = now)
                                syncRepository.enqueueSync("INSERT", "Actor", newActorId, projectId, null)
                                actor = queries.getActorByName(project_id = projectId, name = cleanName).executeAsOneOrNull()
                            }
                            
                            if (actor != null) {
                                queries.linkActorToScene(sceneUserDataId, actor.id)
                                syncRepository.enqueueSync("INSERT", "SceneActor", "${sceneUserDataId}|${actor.id}", projectId, null)
                            }
                        }
                    }
                }
            }
            
            activityRepository.logActivity(
                projectId = projectId,
                type = "SCENARIO",
                action = "UPLOADED",
                entityId = null,
                entityName = "Сценарий (Серия $seriesNumber)",
                description = "загрузил новый сценарий (${parsedScenes.size} сцен)"
            )

            syncRepository.triggerPush()
        }
    }

    override suspend fun deleteScriptFile(fileId: String) {
        withContext(Dispatchers.IO) {
            val file = queries.getScriptFileById(fileId).executeAsOneOrNull() ?: return@withContext
            val projectId = file.project_id
            queries.transaction {
                queries.deleteScenesByScriptFile(fileId)
                queries.deleteScriptFile(fileId)
                syncRepository.enqueueSync("DELETE", "ScriptFile", fileId, projectId, null)
            }
            syncRepository.triggerPush()
        }
    }

    override suspend fun updateScriptTitle(fileId: String, newTitle: String) {
        withContext(Dispatchers.IO) {
            val file = queries.getScriptFileById(fileId).executeAsOneOrNull() ?: return@withContext
            val now = currentTimestamp()
            queries.updateScriptFileTitle(newTitle, now, fileId)
            syncRepository.enqueueSync("UPDATE", "ScriptFile", fileId, file.project_id, null)
            syncRepository.triggerPush()
        }
    }
}
