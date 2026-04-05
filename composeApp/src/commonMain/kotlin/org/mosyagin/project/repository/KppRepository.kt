package org.mosyagin.project.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

import org.mosyagin.project.DatabaseQueries
import org.mosyagin.project.KppFile
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

interface KppRepository {
    fun getKppFilesByProject(projectId: Long): Flow<List<KppFile>>
    suspend fun addKppFile(projectId: Long, fileName: String, filePath: String, version: Long)
}

class KppRepositoryImpl(
    private val queries: DatabaseQueries,
    private val syncRepository: SyncRepository
) : KppRepository {
    override fun getKppFilesByProject(projectId: Long): Flow<List<KppFile>> =
        queries.getKppFilesByProject(projectId)
            .asFlow()
            .mapToList(Dispatchers.Default)

    @OptIn(ExperimentalTime::class)
    override suspend fun addKppFile(projectId: Long, fileName: String, filePath: String, version: Long) {
        val now = Clock.System.now().toEpochMilliseconds()
        queries.insertKppFile(projectId, fileName, filePath, version, now)
        val id = queries.lastInsertRowId().executeAsOne()
        syncRepository.enqueue("INSERT", "KppFile", id, null)
    }
}
