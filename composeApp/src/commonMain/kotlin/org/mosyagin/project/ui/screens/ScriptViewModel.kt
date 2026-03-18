package org.mosyagin.project.ui.screens

import kotlinx.coroutines.flow.StateFlow
import org.mosyagin.project.DatabaseQueries

expect class ScriptViewModel(queries: DatabaseQueries) {
    val queries: DatabaseQueries
    // Добавляем поток состояния загрузки
    val isLoading: StateFlow<Boolean>

    suspend fun processPdfUri(projectId: Long, seriesNumber: Int, uriString: String)
    suspend fun processPdfUri(projectId: Long, uriString: String)
}