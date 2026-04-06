/**
 * ViewModel для обработки файлов сценария (PDF) на платформе Android.
 */
package org.mosyagin.project.ui.screens

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.mosyagin.project.repository.ScriptRepository
import org.mosyagin.project.parser.update.ScriptUpdateManager
import org.mosyagin.project.parser.update.UpdateResult
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.mosyagin.project.util.extractTextFromPdf
import org.mosyagin.project.util.currentTimestamp

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class ScriptViewModel actual constructor(actual val repository: ScriptRepository) : ScreenModel, KoinComponent {

    private val _isLoading = MutableStateFlow(false)
    actual val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val updateManager: ScriptUpdateManager by inject()

    private val _updateResult = MutableStateFlow<UpdateResult?>(null)
    actual val updateResult: StateFlow<UpdateResult?> = _updateResult.asStateFlow()

    actual suspend fun processPdfUri(projectId: String, seriesNumber: Int, uriString: String) {
        _isLoading.value = true
        _updateResult.value = null

        withContext(Dispatchers.IO) {
            try {
                // ИСПОЛЬЗУЕМ НАШ КАСТОМНЫЙ ПАРСЕР С ОТСТУПАМИ
                val fullText = extractTextFromPdf(uriString)

                if (fullText.trim().isEmpty()) {
                    _updateResult.value = UpdateResult.Error("PDF файл пуст или не содержит текстового слоя")
                    return@withContext
                }

                // Этап 1: Только анализ и подготовка данных
                val result = updateManager.prepareUpdate(
                    projectId = projectId,
                    seriesNumber = seriesNumber,
                    filePath = uriString,
                    fullText = fullText,
                    createdAt = currentTimestamp()
                )
                
                _updateResult.value = result
            } catch (e: Exception) {
                e.printStackTrace()
                _updateResult.value = UpdateResult.Error(e.message ?: "Ошибка парсинга")
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Заглушка для старого метода, если он где-то остался
    actual suspend fun processPdfUri(projectId: Long, uriString: String) { }
    
    /**
     * Этап 2: Фиксация изменений в БД после того, как пользователь нажал "Применить"
     */
    actual suspend fun commitUpdate() {
        val currentResult = _updateResult.value
        if (currentResult is UpdateResult.Success) {
            _isLoading.value = true
            withContext(Dispatchers.IO) {
                try {
                    updateManager.executeUpdate(currentResult.previewData)
                    _updateResult.value = null // Успешно сохранено, закрываем отчет
                } catch (e: Exception) {
                    e.printStackTrace()
                    _updateResult.value = UpdateResult.Error(e.message ?: "Ошибка сохранения")
                } finally {
                    _isLoading.value = false
                }
            }
        }
    }

    actual fun deleteScriptFile(fileId: Long) {
        screenModelScope.launch(Dispatchers.IO) {
            repository.deleteScriptFile(fileId.toString())
        }
    }

    actual fun updateScriptTitle(fileId: Long, newTitle: String) {
        screenModelScope.launch(Dispatchers.IO) {
            repository.updateScriptTitle(fileId.toString(), newTitle)
        }
    }
    
    actual fun clearUpdateResult() {
        _updateResult.value = null
    }
}
