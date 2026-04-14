package org.mosyagin.project.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import org.mosyagin.project.DatabaseQueries
import org.mosyagin.project.ProjectMember
import org.mosyagin.project.crypto.CryptoManager
import org.mosyagin.project.crypto.KeyVault
import org.mosyagin.project.util.currentTimestamp
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

interface MemberRepository {
    fun getMembersByProject(projectId: String): Flow<List<ProjectMember>>
    suspend fun addMember(projectId: String, email: String, role: String)
    suspend fun removeMember(memberId: String)
    suspend fun addOwnerLocally(projectId: String, email: String)
}

@OptIn(ExperimentalEncodingApi::class)
class MemberRepositoryImpl(
    private val queries: DatabaseQueries,
    private val syncRepository: SyncRepository,
    private val cryptoManager: CryptoManager,
    private val keyVault: KeyVault,
    private val supabase: SupabaseClient,
    private val authRepository: AuthRepository
) : MemberRepository {

    override fun getMembersByProject(projectId: String): Flow<List<ProjectMember>> =
        queries.getMembersByProject(project_id = projectId)
            .asFlow()
            .mapToList(Dispatchers.IO)

    override suspend fun addMember(projectId: String, email: String, role: String) {
        try {
            val publicKeyResponse = supabase.postgrest["user_public_keys"]
                .select { filter { eq("email", email) } }
                .decodeSingleOrNull<Map<String, String>>()

            val userId = publicKeyResponse?.get("user_id")
            val publicKeyBase64 = publicKeyResponse?.get("public_key")
            val masterKey = keyVault.loadMasterKey(projectId)

            val wrappedKey = if (publicKeyBase64 != null && masterKey != null) {
                val publicKeyBytes = Base64.decode(publicKeyBase64)
                val encrypted = cryptoManager.wrapKey(masterKey, publicKeyBytes)
                Base64.encode(encrypted)
            } else {
                null
            }

            val id = "mem_${projectId}_${email.hashCode()}"
            val now = currentTimestamp()
            
            queries.upsertProjectMember(
                id = id, 
                project_id = projectId, 
                user_id = userId,
                email = email, 
                role = role, 
                updatedAt = now,
                wrapped_master_key = wrappedKey
            )
            
            syncRepository.enqueue("INSERT", "ProjectMember", id, projectId, null)
            
        } catch (e: Exception) {
            println("MemberRepository: Error adding member: ${e.message}")
        }
    }

    override suspend fun addOwnerLocally(projectId: String, email: String) {
        val user = authRepository.getCurrentUserSync()
        val id = "mem_${projectId}_${email.hashCode()}"
        val now = currentTimestamp()
        queries.upsertProjectMember(
            id = id, 
            project_id = projectId, 
            user_id = user?.id,
            email = email, 
            role = "owner", 
            updatedAt = now,
            wrapped_master_key = null
        )
    }

    override suspend fun removeMember(memberId: String) {
        val member = queries.getProjectMemberById(memberId).executeAsOneOrNull() ?: return
        queries.deleteProjectMember(memberId)
        syncRepository.enqueue("DELETE", "ProjectMember", memberId, member.project_id, null)
    }
}
