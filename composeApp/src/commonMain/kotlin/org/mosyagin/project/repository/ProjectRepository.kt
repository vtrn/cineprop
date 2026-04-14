@file:OptIn(ExperimentalTime::class)

package org.mosyagin.project.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.mosyagin.project.DatabaseQueries
import org.mosyagin.project.Project
import org.mosyagin.project.crypto.CryptoManager
import org.mosyagin.project.crypto.KeyVault
import org.mosyagin.project.generateUUID
import org.mosyagin.project.util.currentTimestamp
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

interface ProjectRepository {
    fun getAllProjects(): Flow<List<Project>>
    fun getProjectById(id: String): Flow<Project?>
    suspend fun addProject(name: String, director: String)
    suspend fun deleteProject(id: String)
    suspend fun updateProject(id: String, name: String, director: String)
    suspend fun markProjectAsRemote(id: String)
}

@OptIn(ExperimentalEncodingApi::class)
class ProjectRepositoryImpl(
    private val queries: DatabaseQueries,
    private val syncRepository: SyncRepository,
    private val authRepository: AuthRepository,
    private val cryptoManager: CryptoManager,
    private val keyVault: KeyVault,
    private val supabase: SupabaseClient
) : ProjectRepository {
    override fun getAllProjects(): Flow<List<Project>> =
        queries.getAllProjects()
            .asFlow()
            .mapToList(Dispatchers.Default)

    override fun getProjectById(id: String): Flow<Project?> =
        queries.getProjectById(id)
            .asFlow()
            .map { it.executeAsOneOrNull() }

    override suspend fun addProject(name: String, director: String) {
        val now = Clock.System.now().toEpochMilliseconds()
        val id = generateUUID()
        val creatorEmail = authRepository.getCurrentUserSync()?.email
        
        println("DEBUG TEST 1: Generating master key for project: $name")
        val masterKey = cryptoManager.generateProjectMasterKey()
        
        println("DEBUG TEST 1: Saving project master key locally")
        keyVault.saveMasterKey(id, masterKey)
        
        queries.insertProject(
            id = id, 
            name = name, 
            director = director, 
            updatedAt = now, 
            isRemote = 0L, 
            created_by = creatorEmail
        )
    }

    override suspend fun markProjectAsRemote(id: String) {
        val user = authRepository.getCurrentUserSync() ?: return
        val masterKey = keyVault.loadMasterKey(id) ?: run {
            println("DEBUG TEST 1: ERROR - Master key not found locally for project $id")
            return
        }
        
        try {
            println("DEBUG TEST 1: Fetching public key for wrapping from user_public_keys (user: ${user.id})")
            val publicKeyResponse = supabase.postgrest["user_public_keys"]
                .select { filter { eq("user_id", user.id) } }
                .decodeSingleOrNull<Map<String, String>>()
            
            val publicKeyBase64 = publicKeyResponse?.get("public_key") ?: run {
                println("DEBUG TEST 1: ERROR - Public key not found in cloud")
                return
            }
            val publicKeyBytes = Base64.decode(publicKeyBase64)
            
            println("DEBUG TEST 1: Wrapping project key with user public key")
            val wrappedKey = cryptoManager.wrapKey(masterKey, publicKeyBytes)
            val wrappedKeyBase64 = Base64.encode(wrappedKey)
            
            queries.markProjectAsRemote(id)
            
            val memberId = "mem_${id}_${user.email.hashCode()}"
            println("DEBUG TEST 1: Publishing project member with wrapped_master_key")
            queries.upsertProjectMember(
                id = memberId,
                project_id = id,
                user_id = user.id,
                email = user.email ?: "",
                role = "owner",
                updatedAt = currentTimestamp(),
                wrapped_master_key = wrappedKeyBase64
            )
            
            syncRepository.enqueue("INSERT", "Project", id, id, null)
            syncRepository.enqueue("INSERT", "ProjectMember", memberId, id, null)
            
            println("DEBUG TEST 1: Project master key wrapped and published successfully")
            
        } catch (e: Exception) {
            println("DEBUG TEST 1: CRITICAL ERROR - ${e.message}")
        }
    }

    override suspend fun deleteProject(id: String) {
        val project = queries.getProjectById(id).executeAsOneOrNull()
        queries.deleteProject(id)
        keyVault.deleteProjectKey(id)
        
        if (project?.isRemote == 1L) {
            syncRepository.enqueue("DELETE", "Project", id, id, null)
        }
    }

    override suspend fun updateProject(id: String, name: String, director: String) {
        val now = Clock.System.now().toEpochMilliseconds()
        val project = queries.getProjectById(id).executeAsOneOrNull()
        
        queries.updateProject(name, director, now, id)
        
        if (project?.isRemote == 1L) {
            syncRepository.enqueue("UPDATE", "Project", id, id, null)
        }
    }
}
