@file:OptIn(ExperimentalTime::class)

package org.mosyagin.project.ui.screens

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.mosyagin.project.ScriptFile
import org.mosyagin.project.parser.update.ScriptUpdateManager
import org.mosyagin.project.parser.update.UpdateResult
import org.mosyagin.project.repository.ScriptRepository
import org.mosyagin.project.util.extractTextFromPdf
import kotlin.time.Clock

import kotlin.time.ExperimentalTime

@OptIn(ExperimentalCoroutinesApi::class)
class ScriptWorkspaceViewModel(
    private val projectId: String,
    private val scriptRepository: ScriptRepository
) : ScreenModel, KoinComponent {

    private val updateManager: ScriptUpdateManager by inject()

    val scriptTree: StateFlow<Map<Long, List<ScriptFile>>> = scriptRepository
        .getScriptsForProject(projectId)
        .map { scripts -> scripts.groupBy { it.seriesNumber } }
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val _expandedSeries = MutableStateFlow<Set<Long>>(emptySet())
    val expandedSeries: StateFlow<Set<Long>> = _expandedSeries.asStateFlow()

    private val _selectedScriptFileId = MutableStateFlow<String?>(null)
    val selectedScriptFileId: StateFlow<String?> = _selectedScriptFileId.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _updateResult = MutableStateFlow<UpdateResult?>(null)
    val updateResult: StateFlow<UpdateResult?> = _updateResult.asStateFlow()

    val selectedScriptDetails = _selectedScriptFileId.flatMapLatest { id ->
        if (id == null) flowOf(null)
        else scriptRepository.getScriptFileById(id)
    }.stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun toggleSeries(seriesNumber: Long) {
        val current = _expandedSeries.value
        _expandedSeries.value = if (current.contains(seriesNumber)) current - seriesNumber else current + seriesNumber
    }

    fun onScriptFileSelected(id: String) {
        _selectedScriptFileId.value = id
    }

    fun clearUpdateResult() { _updateResult.value = null }

    fun commitUpdate() {
        val currentResult = _updateResult.value
        if (currentResult is UpdateResult.Success) {
            _isLoading.value = true
            screenModelScope.launch {
                try {
                    updateManager.executeUpdate(currentResult.previewData)
                    _updateResult.value = null
                } finally {
                    _isLoading.value = false
                }
            }
        }
    }

    fun processPdfFile(seriesNumber: Int, uri: String) {
        _isLoading.value = true
        screenModelScope.launch {
            try {
                // Корректно извлекаем текст из PDF используя платформенную реализацию
                val fullText = extractTextFromPdf(uri)
                
                if (fullText.isBlank()) {
                    _updateResult.value = UpdateResult.Error("Не удалось извлечь текст из PDF. Возможно, файл пуст или защищен.")
                    return@launch
                }

                val result = updateManager.prepareUpdate(
                    projectId = projectId,
                    seriesNumber = seriesNumber,
                    filePath = uri,
                    fullText = fullText,
                    createdAt = Clock.System.now().toEpochMilliseconds()
                )
                _updateResult.value = result
            } catch (e: Exception) {
                _updateResult.value = UpdateResult.Error(e.message ?: "Ошибка при обработке PDF")
            } finally {
                _isLoading.value = false
            }
        }
    }
}
