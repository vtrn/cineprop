package org.mosyagin.project.ui.screens

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.mosyagin.project.KppFile
import org.mosyagin.project.parser.KppParser
import org.mosyagin.project.repository.KppRepository

class KppWorkspaceViewModel(
    private val projectId: Long,
    private val kppRepository: KppRepository
) : ScreenModel, KoinComponent {

    private val kppParser: KppParser by inject()

    // Список файлов КПП
    val kppFiles: StateFlow<List<KppFile>> = kppRepository.getKppFilesByProject(projectId)
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedFileId = MutableStateFlow<Long?>(null)
    val selectedFileId: StateFlow<Long?> = _selectedFileId.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun onFileSelected(id: Long) {
        _selectedFileId.value = id
    }

    fun processCsv(fileName: String, content: String) {
        _isLoading.value = true
        screenModelScope.launch {
            try {
                // Вызываем правильный метод парсера
                kppParser.parseAndSaveKpp(projectId, content)
                
                // Добавляем запись в таблицу файлов КПП, чтобы она появилась в левой панели
                val version = (kppFiles.value.maxOfOrNull { it.version } ?: 0L) + 1
                kppRepository.addKppFile(projectId, fileName, "local", version)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
