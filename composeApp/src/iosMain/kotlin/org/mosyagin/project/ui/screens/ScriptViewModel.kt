package org.mosyagin.project.ui.screens

import cafe.adriel.voyager.core.model.ScreenModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.mosyagin.project.repository.ScriptRepository
import org.mosyagin.project.parser.update.UpdateResult

actual class ScriptViewModel actual constructor(actual val repository: ScriptRepository) : ScreenModel {
    private val _isLoading = MutableStateFlow(false)
    actual val isLoading: StateFlow<Boolean> = _isLoading

    private val _updateResult = MutableStateFlow<UpdateResult?>(null)
    actual val updateResult: StateFlow<UpdateResult?> = _updateResult

    actual suspend fun processPdfUri(projectId: Long, seriesNumber: Int, uriString: String) {}
    actual suspend fun processPdfUri(projectId: Long, uriString: String) {}
    
    actual suspend fun commitUpdate() {}

    actual fun deleteScriptFile(fileId: Long) {}
    actual fun updateScriptTitle(fileId: Long, newTitle: String) {}
    
    actual fun clearUpdateResult() {
        _updateResult.value = null
    }
}
