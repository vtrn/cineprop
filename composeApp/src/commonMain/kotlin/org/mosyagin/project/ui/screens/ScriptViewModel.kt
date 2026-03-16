package org.mosyagin.project.ui.screens

import org.mosyagin.project.DatabaseQueries

// Мы объявляем expect class - это значит реализация будет в androidMain и iosMain
expect class ScriptViewModel(queries: DatabaseQueries) {
    val queries: DatabaseQueries
    suspend fun processPdfUri(projectId: Long, uriString: String)
}