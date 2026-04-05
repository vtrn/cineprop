package org.mosyagin.project.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.mosyagin.project.DatabaseQueries
import kotlinx.datetime.Instant

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

    private fun Long.toIsoString(): String = 
        Instant.fromEpochMilliseconds(this).toString()

    /**
     * Основной метод пуша изменений с соблюдением иерархии Foreign Keys
     */
    fun push() {
        scope.launch {
            try {
                val pending = syncRepository.getPending().first()
                if (pending.isEmpty()) return@launch

                println("SyncManager: Starting prioritized push for ${pending.size} records...")

                // Группируем по таблицам
                val grouped = pending.groupBy { it.tableName }

                // ВАЖНО: Соблюдаем порядок вставки (Parent -> Child)
                val tableOrder = listOf("Project", "Actor", "ScriptFile", "SceneUserData", "SceneVersion", "Prop")

                tableOrder.forEach { tableName ->
                    val records = grouped[tableName]
                    if (records != null) {
                        syncTable(tableName, records.map { it.recordId }, records.map { it.id })
                    }
                }
                
                println("SyncManager: Push sequence completed.")
            } catch (e: Exception) {
                println("SyncManager: Push failed with error: ${e.message}")
                e.printStackTrace()
            }
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
