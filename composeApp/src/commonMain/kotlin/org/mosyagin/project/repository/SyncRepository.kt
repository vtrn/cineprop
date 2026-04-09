@file:OptIn(ExperimentalTime::class)

package org.mosyagin.project.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import org.mosyagin.project.DatabaseQueries
import org.mosyagin.project.SyncQueue
import org.mosyagin.project.util.currentTimestamp
import kotlin.time.ExperimentalTime


interface SyncRepository {
    suspend fun enqueue(operation: String, tableName: String, recordId: String, projectId: String?, dataJson: String?)
    fun enqueueSync(operation: String, tableName: String, recordId: String, projectId: String?, dataJson: String?)
    fun getPending(): Flow<List<SyncQueue>>
    suspend fun markSynced(ids: List<Long>)
    fun setSyncManager(manager: SyncManager)
    fun triggerPush()
}

class SyncRepositoryImpl(private val queries: DatabaseQueries) : SyncRepository {
    private var syncManager: SyncManager? = null

    override fun setSyncManager(manager: SyncManager) {
        this.syncManager = manager
    }

    override suspend fun enqueue(operation: String, tableName: String, recordId: String, projectId: String?, dataJson: String?) {
        enqueueSync(operation, tableName, recordId, projectId, dataJson)
        triggerPush()
    }

    override fun enqueueSync(operation: String, tableName: String, recordId: String, projectId: String?, dataJson: String?) {
        val now = currentTimestamp()
        queries.enqueue(
            operation = operation,
            tableName = tableName,
            recordId = recordId,
            project_id = projectId, // Теперь соответствует Database.sq
            dataJson = dataJson,
            updatedAt = now
        )
    }

    override fun triggerPush() {
        syncManager?.push()
    }

    override fun getPending(): Flow<List<SyncQueue>> =
        queries.getPending()
            .asFlow()
            .mapToList(Dispatchers.IO)

    override suspend fun markSynced(ids: List<Long>) {
        queries.markSynced(ids)
    }
}
