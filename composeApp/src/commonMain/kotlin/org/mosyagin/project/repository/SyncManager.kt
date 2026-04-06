@file:OptIn(ExperimentalTime::class)

package org.mosyagin.project.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonPrimitive
import org.mosyagin.project.DatabaseQueries
import org.mosyagin.project.logSync
import org.mosyagin.project.util.currentTimestamp
import kotlinx.datetime.Instant
import kotlin.time.ExperimentalTime

@Serializable
data class ProjectDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("director") val director: String,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class ActorDto(
    @SerialName("id") val id: String,
    @SerialName("project_id") val projectId: String,
    @SerialName("name") val name: String,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class SceneDto(
    @SerialName("id") val id: String,
    @SerialName("project_id") val projectId: String,
    @SerialName("series_number") val seriesNumber: Int,
    @SerialName("scene_number") val sceneNumber: String,
    @SerialName("location") val location: String,
    @SerialName("is_interior") val isInterior: Boolean,
    @SerialName("time_of_day") val timeOfDay: String,
    @SerialName("notes") val notes: String?,
    @SerialName("needs_review") val needsReview: Int? = 0, // Сделано nullable
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class ScriptFileDto(
    @SerialName("id") val id: String,
    @SerialName("project_id") val projectId: String,
    @SerialName("series_number") val seriesNumber: Int,
    @SerialName("title") val title: String,
    @SerialName("file_path") val filePath: String?,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class SceneVersionDto(
    @SerialName("id") val id: String,
    @SerialName("script_file_id") val scriptFileId: String,
    @SerialName("scene_id") val sceneId: String,
    @SerialName("content") val content: String,
    @SerialName("content_hash") val contentHash: String?,
    @SerialName("position_index") val positionIndex: Int,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class PropDto(
    @SerialName("id") val id: String,
    @SerialName("scene_id") val sceneId: String,
    @SerialName("name") val name: String,
    @SerialName("anchor") val anchor: String,
    @SerialName("status") val status: String,
    @SerialName("category") val category: String,
    @SerialName("quantity") val quantity: Int? = 1, // Сделано nullable
    @SerialName("actor_id") val actorId: String?,
    @SerialName("is_cross_cutting") val isCrossCutting: Boolean? = false, // Сделано nullable
    @SerialName("group_id") val groupId: String?,
    @SerialName("note") val note: String?,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class KppFileDto(
    @SerialName("id") val id: String,
    @SerialName("project_id") val projectId: String,
    @SerialName("file_name") val fileName: String,
    @SerialName("file_path") val filePath: String,
    @SerialName("version") val version: Int,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class ShiftDto(
    @SerialName("id") val id: String,
    @SerialName("project_id") val projectId: String,
    @SerialName("shift_number") val shiftNumber: Int,
    @SerialName("date") val date: String,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class ShiftSceneDto(
    @SerialName("shift_id") val shiftId: String,
    @SerialName("scene_id") val sceneId: String,
    @SerialName("position") val position: Int
)

@Serializable
data class SceneActorDto(
    @SerialName("scene_id") val sceneId: String,
    @SerialName("actor_id") val actorId: String
)

class SyncManager(
    private val syncRepository: SyncRepository,
    private val queries: DatabaseQueries,
    private val supabase: SupabaseClient
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val LAST_SYNC_KEY = "last_sync_timestamp"
    // Настраиваем Json для "мягкого" парсинга
    private val json = Json { 
        ignoreUnknownKeys = true
        coerceInputValues = true // Авто-подстановка дефолтных значений при null в JSON
    }

    init {
        logSync("SyncManager: Initializing with complete table support...")
        scope.launch {
            try {
                // 1. Запускаем Realtime в фоне
                setupRealtime()
                
                // 2. Холодный прогрев: всегда тянем данные с сервера при старте
                pull()
                
                // 3. Отправляем локальные изменения, если они есть
                push()
            } catch (e: Exception) {
                logSync("SyncManager: Init sequence failed: ${e.message}")
            }
        }
    }

    private fun setupRealtime() {
        scope.launch {
            try {
                val channel = supabase.realtime.channel("db-changes")
                val tables = listOf("projects", "actors", "script_files", "scenes", "scene_versions", "props", "kpp_files", "shifts", "shift_scenes", "scene_actors")
                
                tables.forEach { tableName ->
                    channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                        table = tableName
                    }.onEach { action ->
                        withContext(Dispatchers.IO) { handleRealtimeAction(tableName, action) }
                    }.launchIn(scope)
                }
                channel.subscribe()
                logSync("SyncManager: Realtime subscribed")
            } catch (e: Exception) {
                logSync("SyncManager: Realtime setup failed: ${e.message}")
            }
        }
    }

    private suspend fun handleRealtimeAction(tableName: String, action: PostgresAction) {
        try {
            when (action) {
                is PostgresAction.Insert -> applyRealtimeData(tableName, action.record)
                is PostgresAction.Update -> applyRealtimeData(tableName, action.record)
                is PostgresAction.Delete -> {
                    withContext(Dispatchers.IO) {
                        if (tableName == "shift_scenes") {
                            val sId = action.oldRecord["shift_id"]?.jsonPrimitive?.content
                            val scId = action.oldRecord["scene_id"]?.jsonPrimitive?.content
                            if (sId != null && scId != null) { queries.deleteShiftScene(sId, scId) }
                        } else {
                            val id = action.oldRecord["id"]?.jsonPrimitive?.content
                            if (id != null) {
                                when (tableName) {
                                    "projects" -> queries.deleteProject(id)
                                    "actors" -> queries.deleteActor(id)
                                    "script_files" -> queries.deleteScriptFile(id)
                                    "scenes" -> queries.deleteSceneUserData(id)
                                    "scene_versions" -> queries.deleteSceneVersion(id)
                                    "props" -> queries.deleteProp(id)
                                }
                            }
                        }
                    }
                }
                else -> {}
            }
        } catch (e: Exception) {
            logSync("SyncManager: Error processing realtime event: ${e.message}")
        }
    }

    private fun applyRealtimeData(tableName: String, record: JsonObject) {
        queries.transaction {
            try {
                when (tableName) {
                    "projects" -> {
                        val dto = json.decodeFromJsonElement<ProjectDto>(record)
                        queries.upsertProject(dto.id, dto.name, dto.director, dto.updatedAt.toEpochMillis())
                    }
                    "actors" -> {
                        val dto = json.decodeFromJsonElement<ActorDto>(record)
                        queries.upsertActor(dto.id, dto.projectId, dto.name, dto.updatedAt.toEpochMillis())
                    }
                    "script_files" -> {
                        val dto = json.decodeFromJsonElement<ScriptFileDto>(record)
                        queries.upsertScriptFile(dto.id, dto.projectId, dto.seriesNumber.toLong(), dto.title, dto.filePath ?: "", dto.createdAt, null, "White", "Supabase", dto.updatedAt.toEpochMillis())
                    }
                    "scenes" -> {
                        val dto = json.decodeFromJsonElement<SceneDto>(record)
                        queries.upsertSceneUserData(dto.id, dto.projectId, dto.seriesNumber.toLong(), dto.sceneNumber, dto.location, if (dto.isInterior) 1L else 0L, dto.timeOfDay, dto.notes, (dto.needsReview ?: 0).toLong(), dto.updatedAt.toEpochMillis())
                    }
                    "scene_versions" -> {
                        val dto = json.decodeFromJsonElement<SceneVersionDto>(record)
                        queries.upsertSceneVersion(dto.id, dto.scriptFileId, dto.sceneId, dto.content, dto.contentHash ?: "", dto.positionIndex.toLong(), dto.updatedAt.toEpochMillis())
                    }
                    "props" -> {
                        val dto = json.decodeFromJsonElement<PropDto>(record)
                        queries.upsertProp(
                            id = dto.id, sceneUserDataId = dto.sceneId, name = dto.name, anchor = dto.anchor, status = dto.status,
                            category = dto.category, propType = "Обстановочный", note = dto.note, photoPath = null,
                            isCrossCutting = if (dto.isCrossCutting == true) 1L else 0L, quantity = (dto.quantity ?: 1).toLong(), actorId = dto.actorId,
                            startOffset = 0, endOffset = 0, orphaned = 0, groupId = dto.groupId, updatedAt = dto.updatedAt.toEpochMillis()
                        )
                    }
                    "kpp_files" -> {
                        val dto = json.decodeFromJsonElement<KppFileDto>(record)
                        queries.upsertKppFile(dto.id, dto.projectId, dto.fileName, dto.filePath, dto.version.toLong(), dto.updatedAt.toEpochMillis())
                    }
                    "shifts" -> {
                        val dto = json.decodeFromJsonElement<ShiftDto>(record)
                        queries.upsertShift(dto.id, dto.projectId, dto.shiftNumber.toLong(), dto.date, dto.updatedAt.toEpochMillis())
                    }
                    "shift_scenes" -> {
                        val dto = json.decodeFromJsonElement<ShiftSceneDto>(record)
                        queries.linkShiftToScene(dto.shiftId, dto.sceneId, dto.position.toLong())
                    }
                    "scene_actors" -> {
                        val dto = json.decodeFromJsonElement<SceneActorDto>(record)
                        queries.linkActorToScene(dto.sceneId, dto.actorId)
                    }
                }
            } catch (e: Exception) {
                logSync("SyncManager: Apply realtime failed for $tableName: ${e.message}")
            }
        }
    }

    private fun String.toEpochMillis(): Long = try {
        kotlinx.datetime.Instant.parse(this).toEpochMilliseconds()
    } catch (e: Exception) {
        0L
    }

    fun push() {
        scope.launch {
            try {
                delay(500)
                val pending = syncRepository.getPending().first()
                if (pending.isEmpty()) return@launch

                logSync("SyncManager: Starting push of ${pending.size} records")
                val grouped = pending.groupBy { it.tableName to it.operation }
                val tableOrder = listOf("Project", "Actor", "ScriptFile", "SceneUserData", "SceneVersion", "Prop", "KppFile", "Shift", "ShiftScene", "SceneActor")

                tableOrder.forEach { tableName ->
                    grouped[tableName to "INSERT"]?.let { records -> syncTable(tableName, records, false) }
                    grouped[tableName to "UPDATE"]?.let { records -> syncTable(tableName, records, false) }
                    grouped[tableName to "DELETE"]?.let { records -> syncTable(tableName, records, true) }
                }
            } catch (e: Exception) {
                logSync("SyncManager: Push failed: ${e.message}")
            }
        }
    }

    suspend fun pull() {
        withContext(Dispatchers.IO) {
            try {
                logSync("SyncManager: Starting pull...")
                val lastSyncStr = queries.getSetting(LAST_SYNC_KEY).executeAsOneOrNull() ?: "1970-01-01T00:00:00Z"
                
                val projects = supabase.postgrest["projects"].select { filter { ProjectDto::updatedAt gt lastSyncStr } }.decodeList<ProjectDto>()
                val actors = supabase.postgrest["actors"].select { filter { ActorDto::updatedAt gt lastSyncStr } }.decodeList<ActorDto>()
                val scripts = supabase.postgrest["script_files"].select { filter { ScriptFileDto::updatedAt gt lastSyncStr } }.decodeList<ScriptFileDto>()
                val scenes = supabase.postgrest["scenes"].select { filter { SceneDto::updatedAt gt lastSyncStr } }.decodeList<SceneDto>()
                val versions = supabase.postgrest["scene_versions"].select { filter { SceneVersionDto::updatedAt gt lastSyncStr } }.decodeList<SceneVersionDto>()
                val props = supabase.postgrest["props"].select { filter { PropDto::updatedAt gt lastSyncStr } }.decodeList<PropDto>()
                
                val kppFiles = supabase.postgrest["kpp_files"].select().decodeList<KppFileDto>()
                val shifts = supabase.postgrest["shifts"].select().decodeList<ShiftDto>()
                val shiftScenes = supabase.postgrest["shift_scenes"].select().decodeList<ShiftSceneDto>()
                val sceneActors = supabase.postgrest["scene_actors"].select().decodeList<SceneActorDto>()

                logSync("SyncManager: Pull downloaded ${projects.size} projects, ${scenes.size} scenes")

                queries.transaction {
                    projects.forEach { queries.upsertProject(it.id, it.name, it.director, it.updatedAt.toEpochMillis()) }
                    actors.forEach { queries.upsertActor(it.id, it.projectId, it.name, it.updatedAt.toEpochMillis()) }
                    scripts.forEach { queries.upsertScriptFile(it.id, it.projectId, it.seriesNumber.toLong(), it.title, it.filePath ?: "", it.createdAt, null, "White", "Supabase", it.updatedAt.toEpochMillis()) }
                    scenes.forEach { queries.upsertSceneUserData(it.id, it.projectId, it.seriesNumber.toLong(), it.sceneNumber, it.location, if (it.isInterior) 1L else 0L, it.timeOfDay, it.notes, (it.needsReview ?: 0).toLong(), it.updatedAt.toEpochMillis()) }
                    versions.forEach { queries.upsertSceneVersion(it.id, it.scriptFileId, it.sceneId, it.content, it.contentHash ?: "", it.positionIndex.toLong(), it.updatedAt.toEpochMillis()) }
                    props.forEach { dto ->
                        queries.upsertProp(
                            id = dto.id, sceneUserDataId = dto.sceneId, name = dto.name, anchor = dto.anchor, status = dto.status,
                            category = dto.category, propType = "Обстановочный", note = dto.note, photoPath = null,
                            isCrossCutting = if (dto.isCrossCutting == true) 1L else 0L, quantity = (dto.quantity ?: 1).toLong(), actorId = dto.actorId,
                            startOffset = 0, endOffset = 0, orphaned = 0, groupId = dto.groupId, updatedAt = dto.updatedAt.toEpochMillis()
                        )
                    }
                    kppFiles.forEach { queries.upsertKppFile(it.id, it.projectId, it.fileName, it.filePath, it.version.toLong(), it.updatedAt.toEpochMillis()) }
                    shifts.forEach { queries.upsertShift(it.id, it.projectId, it.shiftNumber.toLong(), it.date, it.updatedAt.toEpochMillis()) }
                    shiftScenes.forEach { queries.linkShiftToScene(it.shiftId, it.sceneId, it.position.toLong()) }
                    sceneActors.forEach { queries.linkActorToScene(it.sceneId, it.actorId) }
                    
                    val nowStr = kotlinx.datetime.Instant.fromEpochMilliseconds(currentTimestamp()).toString()
                    queries.upsertSetting(LAST_SYNC_KEY, nowStr)
                }
                logSync("SyncManager: Pull completed successfully")
            } catch (e: Exception) {
                logSync("SyncManager: Pull failed: ${e.message}")
            }
        }
    }

    private suspend fun syncTable(tableName: String, records: List<org.mosyagin.project.SyncQueue>, isDelete: Boolean) {
        val recordIds = records.map { it.recordId }
        val queueIds = records.map { it.id }
        val supabaseTable = when (tableName) {
            "Project" -> "projects"; "Actor" -> "actors"; "ScriptFile" -> "script_files"
            "SceneUserData" -> "scenes"; "SceneVersion" -> "scene_versions"; "Prop" -> "props"
            "KppFile" -> "kpp_files"; "Shift" -> "shifts"; "ShiftScene" -> "shift_scenes"; "SceneActor" -> "scene_actors"
            else -> return
        }

        withContext(Dispatchers.IO) {
            try {
                if (isDelete) {
                    recordIds.forEach { compositeId ->
                        if (compositeId.contains("|")) {
                            val parts = compositeId.split("|")
                            val (col1, col2) = if (tableName == "ShiftScene") "shift_id" to "scene_id" else "scene_id" to "actor_id"
                            supabase.postgrest[supabaseTable].delete { filter { eq(col1, parts[0]); eq(col2, parts[1]) } }
                        } else {
                            supabase.postgrest[supabaseTable].delete { filter { eq("id", compositeId) } }
                        }
                    }
                } else {
                    when (tableName) {
                        "Project" -> {
                            val data = queries.getProjectsByIds(recordIds).executeAsList().map { ProjectDto(it.id, it.name, it.director, kotlinx.datetime.Instant.fromEpochMilliseconds(it.updatedAt).toString()) }
                            if (data.isNotEmpty()) supabase.postgrest["projects"].upsert(data)
                        }
                        "Actor" -> {
                            val data = queries.getActorsByIds(recordIds).executeAsList().map { ActorDto(it.id, it.projectId, it.name, kotlinx.datetime.Instant.fromEpochMilliseconds(it.updatedAt).toString()) }
                            if (data.isNotEmpty()) supabase.postgrest["actors"].upsert(data)
                        }
                        "ScriptFile" -> {
                            val data = queries.getScriptFilesByIds(recordIds).executeAsList().map { ScriptFileDto(it.id, it.projectId, it.seriesNumber.toInt(), it.title, it.filePath, it.createdAt, kotlinx.datetime.Instant.fromEpochMilliseconds(it.updatedAt).toString()) }
                            if (data.isNotEmpty()) supabase.postgrest["script_files"].upsert(data)
                        }
                        "SceneUserData" -> {
                            val data = queries.getSceneUserDataByIds(recordIds).executeAsList().map { SceneDto(it.id, it.projectId, it.seriesNumber.toInt(), it.sceneNumber, it.location, it.isInterior == 1L, it.timeOfDay, it.notes, it.needsReview?.toInt(), kotlinx.datetime.Instant.fromEpochMilliseconds(it.updatedAt).toString()) }
                            if (data.isNotEmpty()) supabase.postgrest["scenes"].upsert(data)
                        }
                        "SceneVersion" -> {
                            val data = queries.getSceneVersionsByIds(recordIds).executeAsList().map { SceneVersionDto(it.id, it.scriptFileId, it.sceneUserDataId, it.content, it.contentHash, it.positionIndex.toInt(), kotlinx.datetime.Instant.fromEpochMilliseconds(it.updatedAt).toString()) }
                            if (data.isNotEmpty()) supabase.postgrest["scene_versions"].upsert(data)
                        }
                        "Prop" -> {
                            val data = queries.getPropsByIds(recordIds).executeAsList().map { PropDto(it.id, it.sceneUserDataId, it.name, it.anchor, it.status, it.category, it.quantity?.toInt(), it.actorId, it.isCrossCutting == 1L, it.groupId, it.note, kotlinx.datetime.Instant.fromEpochMilliseconds(it.updatedAt).toString()) }
                            if (data.isNotEmpty()) supabase.postgrest["props"].upsert(data)
                        }
                        "KppFile" -> {
                            val data = recordIds.mapNotNull { queries.getKppFileById(it).executeAsOneOrNull() }.map { KppFileDto(it.id, it.projectId, it.fileName, it.filePath, it.version.toInt(), kotlinx.datetime.Instant.fromEpochMilliseconds(it.updatedAt).toString()) }
                            if (data.isNotEmpty()) supabase.postgrest["kpp_files"].upsert(data)
                        }
                        "Shift" -> {
                            val data = recordIds.mapNotNull { queries.getShiftById(it).executeAsOneOrNull() }.map { ShiftDto(it.id, it.projectId, it.shiftNumber.toInt(), it.date, kotlinx.datetime.Instant.fromEpochMilliseconds(it.updatedAt).toString()) }
                            if (data.isNotEmpty()) supabase.postgrest["shifts"].upsert(data)
                        }
                        "ShiftScene" -> {
                            recordIds.forEach { compositeId ->
                                val parts = compositeId.split("|")
                                if (parts.size == 2) supabase.postgrest["shift_scenes"].upsert(ShiftSceneDto(parts[0], parts[1], 0))
                            }
                        }
                        "SceneActor" -> {
                            recordIds.forEach { compositeId ->
                                val parts = compositeId.split("|")
                                if (parts.size == 2) {
                                    logSync("SyncManager: Syncing SceneActor connection: ${parts[0]} -> ${parts[1]}")
                                    supabase.postgrest["scene_actors"].upsert(SceneActorDto(parts[0], parts[1]))
                                }
                            }
                        }
                    }
                }
                syncRepository.markSynced(queueIds)
            } catch (e: Exception) {
                logSync("SyncManager: Failed to sync table '$tableName' (delete=$isDelete): ${e.message}")
            }
        }
    }
}
