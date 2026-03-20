package org.mosyagin.project.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import org.mosyagin.project.DatabaseQueries
import org.mosyagin.project.KppFile

interface KppRepository {
    fun getKppFilesByProject(projectId: Long): Flow<List<KppFile>>
    suspend fun addKppFile(projectId: Long, fileName: String, filePath: String, version: Long)
}

class KppRepositoryImpl(private val queries: DatabaseQueries) : KppRepository {
    override fun getKppFilesByProject(projectId: Long): Flow<List<KppFile>> =
        queries.getKppFilesByProject(projectId)
            .asFlow()
            .mapToList(Dispatchers.Default)

    override suspend fun addKppFile(projectId: Long, fileName: String, filePath: String, version: Long) {
        queries.insertKppFile(projectId, fileName, filePath, version)
    }
}
