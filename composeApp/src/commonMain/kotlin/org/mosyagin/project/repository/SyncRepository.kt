@file:OptIn(ExperimentalTime::class)

package org.mosyagin.project.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import org.mosyagin.project.DatabaseQueries
import org.mosyagin.project.SyncQueue
import kotlin.time.Clock
import kotlin.time.ExperimentalTime


interface SyncRepository {
    suspend fun enqueue(operation: String, tableName: String, recordId: Long, dataJson: String?)
    // Синхронная версия для использования в ViewModel с Debounce
    fun enqueueSync(operation: String, tableName: String, recordId: Long, dataJson: String?)
    fun getPending(): Flow<List<SyncQueue>>
    suspend fun markSynced(ids: List<Long>)
    fun setSyncManager(manager: SyncManager)
}

class SyncRepositoryImpl(private val queries: DatabaseQueries) : SyncRepository {
    private var syncManager: SyncManager? = null

    override fun setSyncManager(manager: SyncManager) {
        this.syncManager = manager
    }

    override suspend fun enqueue(operation: String, tableName: String, recordId: Long, dataJson: String?) {
        enqueueSync(operation, tableName, recordId, dataJson)
    }

    override fun enqueueSync(operation: String, tableName: String, recordId: Long, dataJson: String?) {
        val now = Clock.System.now().toEpochMilliseconds()
        queries.enqueue(
            operation = operation,
            tableName = tableName,
            recordId = recordId,
            dataJson = dataJson,
            updatedAt = now
        )
        // Сразу инициируем пуш в Supabase
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
