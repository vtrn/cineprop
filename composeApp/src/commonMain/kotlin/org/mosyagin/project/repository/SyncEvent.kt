package org.mosyagin.project.repository

/**
 * Событие для очереди синхронизации.
 * Используется в ViewModel для реализации Debounce.
 */
data class SyncEvent(
    val operation: String,
    val tableName: String,
    val recordId: Long,
    val dataJson: String? = null
)
