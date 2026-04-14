@file:OptIn(ExperimentalTime::class, FlowPreview::class)

package org.mosyagin.project.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonPrimitive
import org.mosyagin.project.DatabaseQueries
import org.mosyagin.project.crypto.CryptoManager
import org.mosyagin.project.crypto.KeyVault
import org.mosyagin.project.logSync
import org.mosyagin.project.util.NetworkObserver
import org.mosyagin.project.util.currentTimestamp
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.ExperimentalTime

// ─────────────────────────────────────────────
// DTOs
// ─────────────────────────────────────────────

@Serializable
data class ProjectDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("director") val director: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("created_by") val createdBy: String? = null
)

@Serializable
data class ProjectMemberDto(
    @SerialName("id") val id: String,
    @SerialName("project_id") val projectId: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("email") val email: String,
    @SerialName("role") val role: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("wrapped_master_key") val wrappedMasterKey: String? = null
)

@Serializable
data class ActorDto(
    @SerialName("id") val id: String,
    @SerialName("project_id") val projectId: String,
    @SerialName("name") val name: String,
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
data class SceneDto(
    @SerialName("id") val id: String,
    @SerialName("project_id") val projectId: String,
    @SerialName("series_number") val seriesNumber: Int,
    @SerialName("scene_number") val sceneNumber: String,
    @SerialName("location") val location: String,
    @SerialName("is_interior") val isInterior: Boolean,
    @SerialName("time_of_day") val timeOfDay: String,
    @SerialName("notes") val notes: String?,
    @SerialName("needs_review") val needsReview: Int? = 0,
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
    @SerialName("quantity") val quantity: Int? = 1,
    @SerialName("actor_id") val actorId: String?,
    @SerialName("is_cross_cutting") val isCrossCutting: Boolean? = false,
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

// ─────────────────────────────────────────────
// SyncManager
// ─────────────────────────────────────────────

class SyncManager(
    private val syncRepository: SyncRepository,
    private val queries: DatabaseQueries,
    private val supabase: SupabaseClient,
    private val networkObserver: NetworkObserver,
    private val authRepository: AuthRepository,
    private val cryptoManager: CryptoManager,
    private val keyVault: KeyVault,
    private val keyManager: KeyManager
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    private val LAST_SYNC_KEY = "last_sync_timestamp"
    private val PROJECT_SYNC_PREFIX = "project_sync_"

    private val syncMutex = Mutex()
    private val pushMutex = Mutex()

    private val pushTrigger = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    init {
        logSync("SyncManager: Initializing architecture fixes...")

        pushTrigger
            .debounce(500)
            .onEach { doPush() }
            .launchIn(scope)

        networkObserver.isOnline
            .drop(1)
            .filter { it }
            .onEach {
                if (authRepository.getCurrentUserSync() != null) {
                    logSync("SyncManager: Network restored. Syncing...")
                    pull()
                    push()
                }
            }
            .launchIn(scope)

        authRepository.currentUser
            .filterNotNull()
            .distinctUntilChangedBy { it.id }
            .onEach { user ->
                logSync("SyncManager: User authenticated. Starting sync.")
                pull()
                push()
                setupRealtime()
            }
            .launchIn(scope)
    }

    fun push() {
        pushTrigger.tryEmit(Unit)
    }

    suspend fun pull() {
        syncMutex.withLock {
            if (!networkObserver.isOnline.value) return@withLock
            if (authRepository.getCurrentUserSync() == null) return@withLock
            doPull()
        }
    }

    private suspend fun setupRealtime() {
        try {
            supabase.realtime.removeAllChannels()
            val channel = supabase.realtime.channel("db-changes")
            val tables = listOf(
                "projects", "project_members", "actors", "script_files",
                "scenes", "scene_versions", "props", "kpp_files",
                "shifts", "shift_scenes", "scene_actors"
            )

            tables.forEach { tableName ->
                channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = tableName
                }.onEach { action ->
                    handleRealtimeAction(tableName, action)
                }.launchIn(scope)
            }

            channel.subscribe()
        } catch (e: Exception) {
            logSync("SyncManager: Realtime setup failed: ${e.message}")
        }
    }

    private suspend fun handleRealtimeAction(tableName: String, action: PostgresAction) {
        syncMutex.withLock {
            try {
                when (action) {
                    is PostgresAction.Insert -> processRealtimeRecord(tableName, action.record)
                    is PostgresAction.Update -> processRealtimeRecord(tableName, action.record)
                    is PostgresAction.Delete -> processRealtimeDelete(tableName, action.oldRecord)
                    else -> Unit
                }
            } catch (e: Exception) {
                logSync("SyncManager: Realtime error: ${e.message}")
            }
        }
    }

    private suspend fun processRealtimeRecord(tableName: String, record: JsonObject) {
        when (tableName) {
            "projects" -> {
                val dto = json.decodeFromJsonElement<ProjectDto>(record)
                queries.upsertProject(dto.id, dto.name, dto.director, dto.updatedAt.toEpochMillis(), 1L, dto.createdBy)
            }
            "project_members" -> {
                val dto = json.decodeFromJsonElement<ProjectMemberDto>(record)
                queries.upsertProjectMember(dto.id, dto.projectId, dto.userId, dto.email, dto.role, dto.updatedAt.toEpochMillis(), dto.wrappedMasterKey)
                keyManager.unwrapAvailableKeys()
            }
            "actors" -> {
                val dto = json.decodeFromJsonElement<ActorDto>(record)
                queries.upsertActor(dto.id, dto.projectId, dto.name, dto.updatedAt.toEpochMillis())
            }
            "script_files" -> {
                val dto = json.decodeFromJsonElement<ScriptFileDto>(record)
                queries.upsertScriptFile(dto.id, dto.projectId, dto.seriesNumber.toLong(), dto.title, dto.filePath ?: "", dto.createdAt, null, "White", "User", dto.updatedAt.toEpochMillis())
            }
            "scenes" -> {
                val dto = json.decodeFromJsonElement<SceneDto>(record)
                val key = keyVault.loadMasterKey(dto.projectId)
                val isDecrypted = key != null
                val notes = if (isDecrypted && dto.notes != null) cryptoManager.decryptText(dto.notes, key!!) else dto.notes
                queries.upsertSceneUserData(dto.id, dto.projectId, dto.seriesNumber.toLong(), dto.sceneNumber, dto.location, if (dto.isInterior) 1L else 0L, dto.timeOfDay, notes, (dto.needsReview ?: 0).toLong(), dto.updatedAt.toEpochMillis(), if (isDecrypted) 1L else 0L)
            }
            "scene_versions" -> {
                val dto = json.decodeFromJsonElement<SceneVersionDto>(record)
                val scene = queries.getSceneUserDataById(dto.sceneId).executeAsOneOrNull()
                val key = scene?.let { keyVault.loadMasterKey(it.project_id) }
                val isDecrypted = key != null
                val content = if (isDecrypted) cryptoManager.decryptText(dto.content, key!!) else dto.content
                queries.upsertSceneVersion(dto.id, dto.scriptFileId, dto.sceneId, content, dto.contentHash ?: "", dto.positionIndex.toLong(), dto.updatedAt.toEpochMillis(), if (isDecrypted) 1L else 0L)
            }
            "props" -> {
                val dto = json.decodeFromJsonElement<PropDto>(record)
                val scene = queries.getSceneUserDataById(dto.sceneId).executeAsOneOrNull()
                val key = scene?.let { keyVault.loadMasterKey(it.project_id) }
                val isDecrypted = key != null
                val note = if (isDecrypted && dto.note != null) cryptoManager.decryptText(dto.note, key!!) else dto.note
                queries.upsertProp(dto.id, dto.sceneId, dto.name, anchor = dto.anchor, status = dto.status, category = dto.category, propType = "Обстановочный", note = note, photoPath = null, isCrossCutting = if (dto.isCrossCutting == true) 1L else 0L, quantity = (dto.quantity ?: 1).toLong(), actorId = dto.actorId, startOffset = 0, endOffset = 0, orphaned = 0, groupId = dto.groupId, updatedAt = dto.updatedAt.toEpochMillis(), isDecrypted = if (isDecrypted) 1L else 0L)
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
                queries.upsertShiftScene(dto.shiftId, dto.sceneId, dto.position.toLong())
            }
            "scene_actors" -> {
                val dto = json.decodeFromJsonElement<SceneActorDto>(record)
                queries.upsertSceneActor(dto.sceneId, dto.actorId)
            }
        }
    }

    private fun processRealtimeDelete(tableName: String, oldRecord: JsonObject) {
        val id = oldRecord["id"]?.jsonPrimitive?.content ?: return
        when (tableName) {
            "projects" -> queries.deleteProject(id)
            "project_members" -> queries.deleteProjectMember(id)
            "actors" -> queries.deleteActor(id)
            "script_files" -> queries.deleteScriptFile(id)
            "scenes" -> queries.deleteSceneUserData(id)
            "scene_versions" -> queries.deleteSceneVersion(id)
            "props" -> queries.deleteProp(id)
            "kpp_files" -> queries.deleteKppFile(id)
            "shifts" -> queries.deleteShift(id)
            "shift_scenes" -> {
                val sid = oldRecord["shift_id"]?.jsonPrimitive?.content ?: return
                val scid = oldRecord["scene_id"]?.jsonPrimitive?.content ?: return
                queries.deleteShiftScene(sid, scid)
            }
            "scene_actors" -> {
                val scid = oldRecord["scene_id"]?.jsonPrimitive?.content ?: return
                val aid = oldRecord["actor_id"]?.jsonPrimitive?.content ?: return
                queries.deleteSceneActor(scid, aid)
            }
        }
    }

    private suspend fun doPush() {
        if (!networkObserver.isOnline.value || authRepository.getCurrentUserSync() == null) return
        if (!pushMutex.tryLock()) return
        try {
            val pending = syncRepository.getPending().first()
            if (pending.isEmpty()) return

            logSync("SyncManager: Pushing ${pending.size} records...")
            val grouped = pending.groupBy { it.tableName to it.operation }
            
            // ВАЖНО: Порядок строго определен родительскими связями
            val tableOrder = listOf(
                "Project", "Actor", "ScriptFile", "SceneUserData", 
                "ProjectMember", "SceneVersion", "Prop", "KppFile", 
                "Shift", "ShiftScene", "SceneActor"
            )

            for (tableName in tableOrder) {
                // Сначала INSERT, потом UPDATE, затем DELETE для каждой таблицы
                // Это гарантирует, что ScriptFile улетит раньше SceneVersion
                grouped[tableName to "INSERT"]?.let { syncTable(tableName, it, isDelete = false) }
                grouped[tableName to "UPDATE"]?.let { syncTable(tableName, it, isDelete = false) }
                grouped[tableName to "DELETE"]?.let { syncTable(tableName, it, isDelete = true) }
            }
        } finally {
            pushMutex.unlock()
        }
    }

    private suspend fun syncTable(tableName: String, records: List<org.mosyagin.project.SyncQueue>, isDelete: Boolean) {
        val recordIds = records.map { it.recordId }; val queueIds = records.map { it.id }
        try {
            if (isDelete) { syncRepository.markSynced(queueIds); return }
            when (tableName) {
                "Project" -> {
                    val dtos = queries.getProjectsByIds(recordIds).executeAsList().map { ProjectDto(it.id, it.name, it.director, it.updatedAt.toIso(), it.created_by) }
                    if (dtos.isNotEmpty()) supabase.postgrest["projects"].upsert(dtos)
                }
                "ProjectMember" -> {
                    val dtos = queries.getProjectMembersByIds(recordIds).executeAsList().map { ProjectMemberDto(it.id, it.project_id, it.user_id, it.email, it.role, it.updatedAt.toIso(), it.wrapped_master_key) }
                    if (dtos.isNotEmpty()) supabase.postgrest["project_members"].upsert(dtos) { onConflict = "project_id,email" }
                }
                "Actor" -> {
                    val dtos = queries.getActorsByIds(recordIds).executeAsList().map { ActorDto(it.id, it.project_id, it.name, it.updatedAt.toIso()) }
                    if (dtos.isNotEmpty()) supabase.postgrest["actors"].upsert(dtos)
                }
                "ScriptFile" -> {
                    val dtos = queries.getScriptFilesByIds(recordIds).executeAsList().map { ScriptFileDto(it.id, it.project_id, it.seriesNumber.toInt(), it.title, it.filePath, it.createdAt, it.updatedAt.toIso()) }
                    if (dtos.isNotEmpty()) supabase.postgrest["script_files"].upsert(dtos)
                }
                "SceneUserData" -> {
                    val dtos = mutableListOf<SceneDto>()
                    for (ud in queries.getSceneUserDataByIds(recordIds).executeAsList()) {
                        val key = keyVault.loadMasterKey(ud.project_id)
                        val enc = if (key != null && ud.notes != null) cryptoManager.encryptText(ud.notes, key) else ud.notes
                        dtos.add(SceneDto(ud.id, ud.project_id, ud.seriesNumber.toInt(), ud.sceneNumber, ud.location, ud.isInterior == 1L, ud.timeOfDay, enc, ud.needsReview.toInt(), ud.updatedAt.toIso()))
                    }
                    if (dtos.isNotEmpty()) supabase.postgrest["scenes"].upsert(dtos)
                }
                "SceneVersion" -> {
                    val dtos = mutableListOf<SceneVersionDto>()
                    for (v in queries.getSceneVersionsByIds(recordIds).executeAsList()) {
                        val scene = queries.getSceneUserDataById(v.sceneUserDataId).executeAsOneOrNull()
                        val key = scene?.let { keyVault.loadMasterKey(it.project_id) }
                        val enc = if (key != null) cryptoManager.encryptText(v.content, key) else v.content
                        dtos.add(SceneVersionDto(v.id, v.scriptFileId, v.sceneUserDataId, enc, v.contentHash, v.positionIndex.toInt(), v.updatedAt.toIso()))
                    }
                    if (dtos.isNotEmpty()) supabase.postgrest["scene_versions"].upsert(dtos)
                }
                "Prop" -> {
                    val dtos = mutableListOf<PropDto>()
                    for (p in queries.getPropsByIds(recordIds).executeAsList()) {
                        val scene = queries.getSceneUserDataById(p.sceneUserDataId).executeAsOneOrNull()
                        val key = scene?.let { keyVault.loadMasterKey(it.project_id) }
                        val enc = if (key != null && p.note != null) cryptoManager.encryptText(p.note, key) else p.note
                        dtos.add(PropDto(p.id, p.sceneUserDataId, p.name, p.anchor, p.status, p.category, p.quantity.toInt(), p.actorId, p.isCrossCutting == 1L, p.groupId, enc, p.updatedAt.toIso()))
                    }
                    if (dtos.isNotEmpty()) supabase.postgrest["props"].upsert(dtos)
                }
                "KppFile" -> {
                    val dtos = recordIds.mapNotNull { queries.getKppFileById(it).executeAsOneOrNull() }.map { KppFileDto(it.id, it.project_id, it.fileName, it.filePath, it.version.toInt(), it.updatedAt.toIso()) }
                    if (dtos.isNotEmpty()) supabase.postgrest["kpp_files"].upsert(dtos)
                }
                "Shift" -> {
                    val dtos = recordIds.mapNotNull { queries.getShiftById(it).executeAsOneOrNull() }.map { ShiftDto(it.id, it.project_id, it.shiftNumber.toInt(), it.date, it.updatedAt.toIso()) }
                    if (dtos.isNotEmpty()) supabase.postgrest["shifts"].upsert(dtos)
                }
                "ShiftScene" -> {
                    val dtos = recordIds.mapNotNull { id -> val parts = id.split("|"); if (parts.size == 2) ShiftSceneDto(parts[0], parts[1], 0) else null }
                    if (dtos.isNotEmpty()) supabase.postgrest["shift_scenes"].upsert(dtos)
                }
                "SceneActor" -> {
                    val dtos = recordIds.mapNotNull { id -> val parts = id.split("|"); if (parts.size == 2) SceneActorDto(parts[0], parts[1]) else null }
                    if (dtos.isNotEmpty()) supabase.postgrest["scene_actors"].upsert(dtos)
                }
            }
            syncRepository.markSynced(queueIds)
        } catch (e: Exception) { 
            logSync("SyncLog: Push failed for $tableName: ${e.message}")
            // БРОСАЕМ исключение дальше, чтобы остановить doPush и не пытаться вставить дочерние записи
            throw e 
        }
    }

    private suspend fun doPull() {
        try {
            val projects = supabase.postgrest["projects"].select().decodeList<ProjectDto>()
            val members = supabase.postgrest["project_members"].select().decodeList<ProjectMemberDto>()
            projects.forEach { queries.upsertProject(it.id, it.name, it.director, it.updatedAt.toEpochMillis(), 1L, it.createdBy) }
            members.forEach { m -> queries.upsertProjectMember(m.id, m.projectId, m.userId, m.email, m.role, m.updatedAt.toEpochMillis(), m.wrappedMasterKey) }
            keyManager.unwrapAvailableKeys()
            for (p in projects) syncProjectData(p.id)
            queries.upsertSetting(LAST_SYNC_KEY, currentIso())
        } catch (e: Exception) { logSync("Pull failed: ${e.message}") }
    }

    private suspend fun syncProjectData(projectId: String, forceFull: Boolean = false) {
        val lastSync = if (forceFull) "1970-01-01T00:00:00Z" else getProjectSyncTimestamp(projectId)
        val key = keyVault.loadMasterKey(projectId)
        try {
            val actors = supabase.postgrest["actors"].select { filter { eq("project_id", projectId); gt("updated_at", lastSync) } }.decodeList<ActorDto>()
            val scripts = supabase.postgrest["script_files"].select { filter { eq("project_id", projectId); gt("updated_at", lastSync) } }.decodeList<ScriptFileDto>()
            val scenes = supabase.postgrest["scenes"].select { filter { eq("project_id", projectId); gt("updated_at", lastSync) } }.decodeList<SceneDto>()
            val scids = scenes.map { it.id }
            val versions = if (scids.isNotEmpty()) supabase.postgrest["scene_versions"].select { filter { isIn("scene_id", scids) } }.decodeList<SceneVersionDto>() else emptyList()
            val props = if (scids.isNotEmpty()) supabase.postgrest["props"].select { filter { isIn("scene_id", scids) } }.decodeList<PropDto>() else emptyList()
            val isDecrypted = key != null
            val decScenes = scenes.map { s -> s.copy(notes = if (isDecrypted && s.notes != null) cryptoManager.decryptText(s.notes, key!!) else s.notes) }
            val decVersions = versions.map { v -> v.copy(content = if (isDecrypted) cryptoManager.decryptText(v.content, key!!) else v.content) }
            val decProps = props.map { p -> p.copy(note = if (isDecrypted && p.note != null) cryptoManager.decryptText(p.note, key!!) else p.note) }
            queries.transaction {
                actors.forEach { queries.upsertActor(it.id, it.projectId, it.name, it.updatedAt.toEpochMillis()) }
                scripts.forEach { queries.upsertScriptFile(it.id, it.projectId, it.seriesNumber.toLong(), it.title, it.filePath ?: "", it.createdAt, null, "White", "User", it.updatedAt.toEpochMillis()) }
                decScenes.forEach { s -> queries.upsertSceneUserData(s.id, s.projectId, s.seriesNumber.toLong(), s.sceneNumber, s.location, if (s.isInterior) 1L else 0L, s.timeOfDay, s.notes, (s.needsReview ?: 0).toLong(), s.updatedAt.toEpochMillis(), if (isDecrypted) 1L else 0L) }
                decVersions.forEach { v -> queries.upsertSceneVersion(v.id, v.scriptFileId, v.sceneId, v.content, v.contentHash ?: "", v.positionIndex.toLong(), v.updatedAt.toEpochMillis(), if (isDecrypted) 1L else 0L) }
                decProps.forEach { p -> queries.upsertProp(p.id, p.sceneId, p.name, p.anchor, p.status, p.category, "Обстановочный", p.note, null, if (p.isCrossCutting == true) 1L else 0L, (p.quantity ?: 1).toLong(), p.actorId, 0, 0, 0, p.groupId, p.updatedAt.toEpochMillis(), if (isDecrypted) 1L else 0L) }
            }
            updateProjectSyncTimestamp(projectId)
        } catch (e: Exception) { logSync("Project sync failed: ${e.message}") }
    }

    private fun getProjectSyncTimestamp(id: String): String = queries.getSetting("$PROJECT_SYNC_PREFIX$id").executeAsOneOrNull() ?: "1970-01-01T00:00:00Z"
    private fun updateProjectSyncTimestamp(id: String) = queries.upsertSetting("$PROJECT_SYNC_PREFIX$id", currentIso())
    private fun Long.toIso(): String = kotlinx.datetime.Instant.fromEpochMilliseconds(this).toString()
    private fun String.toEpochMillis(): Long = try { kotlinx.datetime.Instant.parse(this).toEpochMilliseconds() } catch (e: Exception) { 0L }
    private fun currentIso(): String = kotlinx.datetime.Instant.fromEpochMilliseconds(currentTimestamp()).toString()
}
