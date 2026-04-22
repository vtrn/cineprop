package org.mosyagin.project.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import org.mosyagin.project.ActivityLog
import org.mosyagin.project.DatabaseQueries
import org.mosyagin.project.GetAllActivities
import org.mosyagin.project.crypto.CryptoManager
import org.mosyagin.project.crypto.KeyVault
import org.mosyagin.project.generateUUID
import org.mosyagin.project.util.currentTimestamp
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@Serializable
data class ActivityDto(
    val id: String,
    val project_id: String,
    val user_id: String,
    val user_name: String,
    val type: String,
    val action: String,
    val entity_id: String?,
    val encrypted_entity_name: String?,
    val encrypted_description: String?,
    val encrypted_metadata: String?,
    val created_at: String
)

interface ActivityRepository {
    fun getActivities(projectId: String): Flow<List<ActivityLog>>
    fun getAllRecentActivities(): Flow<List<GetAllActivities>>
    suspend fun logActivity(
        projectId: String,
        type: String,
        action: String,
        entityId: String? = null,
        entityName: String? = null,
        description: String? = null,
        metadata: String? = null
    )
    suspend fun decryptActivities(projectId: String)
}

@OptIn(ExperimentalEncodingApi::class)
class ActivityRepositoryImpl(
    private val queries: DatabaseQueries,
    private val syncRepository: SyncRepository,
    private val authRepository: AuthRepository,
    private val cryptoManager: CryptoManager,
    private val keyVault: KeyVault,
    private val supabase: SupabaseClient
) : ActivityRepository {

    override fun getActivities(projectId: String): Flow<List<ActivityLog>> =
        queries.getActivitiesByProject(projectId).asFlow().mapToList(Dispatchers.IO)

    override fun getAllRecentActivities(): Flow<List<GetAllActivities>> =
        queries.getAllActivities().asFlow().mapToList(Dispatchers.IO)

    override suspend fun logActivity(
        projectId: String,
        type: String,
        action: String,
        entityId: String?,
        entityName: String?,
        description: String?,
        metadata: String?
    ) {
        val user = authRepository.getCurrentUserSync() ?: return
        val userName = user.userMetadata?.get("full_name")?.toString() ?: user.email ?: "Unknown"
        val id = generateUUID()
        val now = currentTimestamp()

        // Локально храним ПЛОСКИЙ текст, SyncManager зашифрует при отправке
        queries.insertActivity(
            id = id,
            projectId = projectId,
            userId = user.id,
            userName = userName,
            type = type,
            action = action,
            entityId = entityId,
            encryptedEntityName = entityName,
            encryptedDescription = description,
            encryptedMetadata = metadata,
            createdAt = now,
            isDecrypted = 1L
        )

        val project = queries.getProjectById(projectId).executeAsOneOrNull()
        if (project?.isRemote == 1L) {
            syncRepository.enqueue("INSERT", "ActivityLog", id, projectId, null)
        }
    }

    override suspend fun decryptActivities(projectId: String) {
        val masterKey = keyVault.loadMasterKey(projectId) ?: return
        val activities = queries.getActivitiesByProject(projectId).executeAsList().filter { it.isDecrypted == 0L }
        
        activities.forEach { activity ->
            val decName = activity.encryptedEntityName?.let { cryptoManager.decryptText(it, masterKey) } ?: activity.encryptedEntityName
            val decDesc = activity.encryptedDescription?.let { cryptoManager.decryptText(it, masterKey) } ?: activity.encryptedDescription
            
            queries.updateActivityDecrypted(
                encryptedEntityName = decName,
                encryptedDescription = decDesc,
                id = activity.id
            )
        }
    }
}
