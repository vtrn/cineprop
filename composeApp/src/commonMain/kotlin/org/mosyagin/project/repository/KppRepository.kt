package org.mosyagin.project.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

import org.mosyagin.project.DatabaseQueries
import org.mosyagin.project.KppFile
import org.mosyagin.project.generateUUID
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

interface KppRepository {
    fun getKppFilesByProject(projectId: String): Flow<List<KppFile>>
    suspend fun addKppFile(projectId: String, fileName: String, filePath: String, version: Long)
}

class KppRepositoryImpl(
    private val queries: DatabaseQueries,
    private val syncRepository: SyncRepository
) : KppRepository {
    override fun getKppFilesByProject(projectId: String): Flow<List<KppFile>> =
        queries.getKppFilesByProject(project_id = projectId)
            .asFlow()
            .mapToList(Dispatchers.Default)

    @OptIn(ExperimentalTime::class)
    override suspend fun addKppFile(projectId: String, fileName: String, filePath: String, version: Long) {
        val id = "kpp_${projectId}_$version"
        val now = Clock.System.now().toEpochMilliseconds()
        
        val existing = queries.getKppFileById(id).executeAsOneOrNull()
        if (existing == null) {
            queries.upsertKppFile(id = id, project_id = projectId, fileName = fileName, filePath = filePath, version = version, updatedAt = now)
            syncRepository.enqueue("INSERT", "KppFile", id, projectId, null)
        }
    }
}
