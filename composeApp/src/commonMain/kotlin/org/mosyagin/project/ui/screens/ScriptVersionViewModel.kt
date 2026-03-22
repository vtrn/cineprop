package org.mosyagin.project.ui.screens

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.mosyagin.project.repository.ScriptRepository
import org.mosyagin.project.models.versioning.ScriptFile
import org.mosyagin.project.models.versioning.toDomain

sealed class ScriptVersionUiState {
    object Loading : ScriptVersionUiState()
    data class Success(val versions: List<ScriptFile>, val activeVersionId: Long?) : ScriptVersionUiState()
    data class Error(val message: String) : ScriptVersionUiState()
}

class ScriptVersionViewModel(
    private val repository: ScriptRepository,
    private val projectId: Long,
    private val seriesNumber: Int
) : ScreenModel {

    private val _uiState = MutableStateFlow<ScriptVersionUiState>(ScriptVersionUiState.Loading)
    val uiState: StateFlow<ScriptVersionUiState> = _uiState.asStateFlow()

    init {
        loadVersions()
    }

    fun loadVersions() {
        screenModelScope.launch {
            repository.getScriptsForProject(projectId)
                .map { list ->
                    list.filter { it.seriesNumber == seriesNumber.toLong() }
                        .map { it.toDomain() }
                        .sortedByDescending { it.createdAt }
                }
                .collect { versions ->
                    _uiState.value = ScriptVersionUiState.Success(
                        versions = versions,
                        activeVersionId = versions.firstOrNull()?.id // По умолчанию последняя загруженная
                    )
                }
        }
    }

    fun deleteVersion(id: Long) {
        screenModelScope.launch {
            repository.deleteScriptFile(id)
            // Список обновится автоматически через Flow в repository
        }
    }
}
