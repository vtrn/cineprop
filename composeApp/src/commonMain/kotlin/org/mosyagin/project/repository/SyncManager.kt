@file:OptIn(ExperimentalTime::class)

package org.mosyagin.project.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.mosyagin.project.DatabaseQueries
import kotlinx.datetime.Instant
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Serializable
data class ProjectDto(
    @SerialName("id") val id: Long,
    @SerialName("name") val name: String,
    @SerialName("director") val director: String,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class ActorDto(
    @SerialName("id") val id: Long,
    @SerialName("project_id") val projectId: Long,
    @SerialName("name") val name: String,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class SceneDto(
    @SerialName("id") val id: Long,
    @SerialName("project_id") val projectId: Long,
    @SerialName("series_number") val seriesNumber: Int,
    @SerialName("scene_number") val sceneNumber: String,
    @SerialName("location") val location: String,
    @SerialName("is_interior") val isInterior: Boolean,
    @SerialName("time_of_day") val timeOfDay: String,
    @SerialName("notes") val notes: String?,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class ScriptFileDto(
    @SerialName("id") val id: Long,
    @SerialName("project_id") val projectId: Long,
    @SerialName("series_number") val seriesNumber: Int,
    @SerialName("title") val title: String,
    @SerialName("file_path") val filePath: String?,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class SceneVersionDto(
    @SerialName("id") val id: Long,
    @SerialName("script_file_id") val scriptFileId: Long,
    @SerialName("scene_id") val sceneId: Long,
    @SerialName("content") val content: String,
    @SerialName("content_hash") val contentHash: String?,
    @SerialName("position_index") val positionIndex: Int,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class PropDto(
    @SerialName("id") val id: Long,
    @SerialName("scene_id") val sceneId: Long,
    @SerialName("name") val name: String,
    @SerialName("anchor") val anchor: String,
    @SerialName("status") val status: String,
    @SerialName("category") val category: String,
    @SerialName("quantity") val quantity: Int,
    @SerialName("actor_id") val actorId: Long?,
    @SerialName("is_cross_cutting") val isCrossCutting: Boolean,
    @SerialName("note") val note: String?,
    @SerialName("updated_at") val updatedAt: String
)

class SyncManager(
    private val syncRepository: SyncRepository,
    private val queries: DatabaseQueries,
    private val supabase: SupabaseClient
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val LAST_SYNC_KEY = "last_sync_timestamp"

    private fun Long.toIsoString(): String = 
        Instant.fromEpochMilliseconds(this).toString()

    private fun String.toEpochMillis(): Long = 
        Instant.parse(this).toEpochMilliseconds()

    /**
     * Основной метод пуша изменений
     */
    fun push() {
        scope.launch {
            try {
                val pending = syncRepository.getPending().first()
                if (pending.isEmpty()) {
                    pull() // Если пушить нечего, пробуем тянуть
                    return@launch
                }

                println("SyncManager: Starting push...")
                val grouped = pending.groupBy { it.tableName }
                val tableOrder = listOf("Project", "Actor", "ScriptFile", "SceneUserData", "SceneVersion", "Prop")

                tableOrder.forEach { tableName ->
                    val records = grouped[tableName]
                    if (records != null) {
                        syncTable(tableName, records.map { it.recordId }, records.map { it.id })
                    }
                }
                
                println("SyncManager: Push sequence completed. Starting pull...")
                pull()
            } catch (e: Exception) {
                println("SyncManager: Push failed: ${e.message}")
            }
        }
    }

    /**
     * Метод получения изменений с сервера
     */
    @OptIn(kotlin.time.ExperimentalTime::class)
    suspend fun pull() {
        try {
            val lastSyncStr = queries.getSetting(LAST_SYNC_KEY).executeAsOneOrNull() ?: "1970-01-01T00:00:00Z"
            println("SyncManager: Starting pull from $lastSyncStr...")

            // 1. Projects
            val projects = supabase.postgrest["projects"]
                .select { filter { ProjectDto::updatedAt gt lastSyncStr } }
                .decodeList<ProjectDto>()
            projects.forEach { queries.upsertProject(it.id, it.name, it.director, it.updatedAt.toEpochMillis()) }

            // 2. Actors
            val actors = supabase.postgrest["actors"]
                .select { filter { ActorDto::updatedAt gt lastSyncStr } }
                .decodeList<ActorDto>()
            actors.forEach { queries.upsertActor(it.id, it.projectId, it.name, it.updatedAt.toEpochMillis()) }

            // 3. ScriptFiles
            val scripts = supabase.postgrest["script_files"]
                .select { filter { ScriptFileDto::updatedAt gt lastSyncStr } }
                .decodeList<ScriptFileDto>()
            scripts.forEach { 
                queries.upsertScriptFile(
                    id = it.id,
                    projectId = it.projectId,
                    seriesNumber = it.seriesNumber.toLong(),
                    title = it.title,
                    filePath = it.filePath ?: "",
                    createdAt = it.createdAt,
                    previousVersionId = null,
                    revisionColor = "White",
                    uploadedBy = "Supabase",
                    updatedAt = it.updatedAt.toEpochMillis()
                )
            }

            // 4. Scenes
            val scenes = supabase.postgrest["scenes"]
                .select { filter { SceneDto::updatedAt gt lastSyncStr } }
                .decodeList<SceneDto>()
            scenes.forEach { queries.upsertSceneUserData(it.id, it.projectId, it.seriesNumber.toLong(), it.sceneNumber, it.location, if (it.isInterior) 1L else 0L, it.timeOfDay, it.notes, 0L, it.updatedAt.toEpochMillis()) }

            // 5. Scene Versions
            val versions = supabase.postgrest["scene_versions"]
                .select { filter { SceneVersionDto::updatedAt gt lastSyncStr } }
                .decodeList<SceneVersionDto>()
            versions.forEach { queries.upsertSceneVersion(it.id, it.scriptFileId, it.sceneId, it.content, it.contentHash ?: "", it.positionIndex.toLong(), it.updatedAt.toEpochMillis()) }

            // 6. Props
            val props = supabase.postgrest["props"]
                .select { filter { PropDto::updatedAt gt lastSyncStr } }
                .decodeList<PropDto>()
            props.forEach { queries.upsertProp(it.id, it.sceneId, it.name, it.anchor, it.status, it.category, "Обстановочный", it.note, null, if (it.isCrossCutting) 1L else 0L, it.quantity.toLong(), it.actorId, 0, 0, 0, null, it.updatedAt.toEpochMillis()) }

            // Обновляем метку времени синхронизации (текущее время сервера/клиента в ISO)
            val nowStr = Instant.fromEpochMilliseconds(kotlin.time.Clock.System.now().toEpochMilliseconds()).toString()
            queries.upsertSetting(LAST_SYNC_KEY, nowStr)
            
            println("SyncManager: Pull completed successfully.")
        } catch (e: Exception) {
            println("SyncManager: Pull failed: ${e.message}")
            e.printStackTrace()
        }
    }

    private suspend fun syncTable(tableName: String, recordIds: List<Long>, queueIds: List<Long>) {
        when (tableName) {
            "Project" -> {
                val data = queries.getProjectsByIds(recordIds).executeAsList()
                    .map { ProjectDto(it.id, it.name, it.director, it.updatedAt.toIsoString()) }
                if (data.isNotEmpty()) {
                    supabase.postgrest["projects"].upsert(data)
                    syncRepository.markSynced(queueIds)
                }
            }
            "Actor" -> {
                val data = queries.getActorsByIds(recordIds).executeAsList()
                    .map { ActorDto(it.id, it.projectId, it.name, it.updatedAt.toIsoString()) }
                if (data.isNotEmpty()) {
                    supabase.postgrest["actors"].upsert(data)
                    syncRepository.markSynced(queueIds)
                }
            }
            "ScriptFile" -> {
                val data = queries.getScriptFilesByIds(recordIds).executeAsList()
                    .map { 
                        ScriptFileDto(
                            id = it.id,
                            projectId = it.projectId,
                            seriesNumber = it.seriesNumber.toInt(),
                            title = it.title,
                            filePath = it.filePath,
                            createdAt = it.createdAt,
                            updatedAt = it.updatedAt.toIsoString()
                        )
                    }
                if (data.isNotEmpty()) {
                    supabase.postgrest["script_files"].upsert(data)
                    syncRepository.markSynced(queueIds)
                }
            }
            "SceneUserData" -> {
                val data = queries.getSceneUserDataByIds(recordIds).executeAsList()
                    .map { 
                        SceneDto(
                            id = it.id,
                            projectId = it.projectId,
                            seriesNumber = it.seriesNumber.toInt(),
                            sceneNumber = it.sceneNumber,
                            location = it.location,
                            isInterior = it.isInterior == 1L,
                            timeOfDay = it.timeOfDay,
                            notes = it.notes,
                            updatedAt = it.updatedAt.toIsoString()
                        )
                    }
                if (data.isNotEmpty()) {
                    supabase.postgrest["scenes"].upsert(data)
                    syncRepository.markSynced(queueIds)
                }
            }
            "SceneVersion" -> {
                val data = queries.getSceneVersionsByIds(recordIds).executeAsList()
                    .map { 
                        SceneVersionDto(
                            id = it.id,
                            scriptFileId = it.scriptFileId,
                            sceneId = it.sceneUserDataId,
                            content = it.content,
                            contentHash = it.contentHash,
                            positionIndex = it.positionIndex.toInt(),
                            updatedAt = it.updatedAt.toIsoString()
                        )
                    }
                if (data.isNotEmpty()) {
                    supabase.postgrest["scene_versions"].upsert(data)
                    syncRepository.markSynced(queueIds)
                }
            }
            "Prop" -> {
                val data = queries.getPropsByIds(recordIds).executeAsList()
                    .map { 
                        PropDto(
                            id = it.id, 
                            sceneId = it.sceneUserDataId, 
                            name = it.name, 
                            anchor = it.anchor, 
                            status = it.status, 
                            category = it.category, 
                            quantity = it.quantity.toInt(), 
                            actorId = it.actorId,
                            isCrossCutting = it.isCrossCutting == 1L,
                            note = it.note,
                            updatedAt = it.updatedAt.toIsoString()
                        ) 
                    }
                if (data.isNotEmpty()) {
                    supabase.postgrest["props"].upsert(data)
                    syncRepository.markSynced(queueIds)
                }
            }
        }
    }
}
