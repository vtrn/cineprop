package org.mosyagin.project.ui.screens

import cafe.adriel.voyager.core.model.ScreenModel
import kotlinx.coroutines.flow.StateFlow
import org.mosyagin.project.repository.ScriptRepository

expect class ScriptViewModel(repository: ScriptRepository) : ScreenModel {
    val repository: ScriptRepository
    val isLoading: StateFlow<Boolean>

    suspend fun processPdfUri(projectId: Long, seriesNumber: Int, uriString: String)
    suspend fun processPdfUri(projectId: Long, uriString: String)
    
    // CRUD методы
    fun deleteScriptFile(fileId: Long)
    fun updateScriptTitle(fileId: Long, newTitle: String)
}
