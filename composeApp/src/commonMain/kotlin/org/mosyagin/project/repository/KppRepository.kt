package org.mosyagin.project.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import org.mosyagin.project.DatabaseQueries
import org.mosyagin.project.KppFile
import org.mosyagin.project.generateUUID
import org.mosyagin.project.util.currentTimestamp

interface KppRepository {
    fun getKppFilesByProject(projectId: String): Flow<List<KppFile>>
    suspend fun addKppFile(projectId: String, fileName: String, filePath: String, version: Long)
}

class KppRepositoryImpl(
    private val queries: DatabaseQueries,
    private val syncRepository: SyncRepository,
    private val activityRepository: ActivityRepository
) : KppRepository {

    override fun getKppFilesByProject(projectId: String): Flow<List<KppFile>> =
        queries.getKppFilesByProject(projectId).asFlow().mapToList(Dispatchers.IO)

    override suspend fun addKppFile(projectId: String, fileName: String, filePath: String, version: Long) {
        val id = generateUUID()
        val now = currentTimestamp()
        queries.insertKppFile(id, projectId, fileName, filePath, version, now)
        
        syncRepository.enqueue("INSERT", "KppFile", id, projectId, null)
        
        activityRepository.logActivity(
            projectId = projectId,
            type = "KPP",
            action = "UPLOADED",
            entityId = id,
            entityName = fileName,
            description = "загрузил новый файл КПП (версия $version)"
        )
    }
}
