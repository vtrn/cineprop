/**
 * ViewModel для обработки файлов сценария (PDF) на платформе Android.
 */
package org.mosyagin.project.ui.screens

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.mosyagin.project.db.appContext
import org.mosyagin.project.repository.ScriptRepository

actual class ScriptViewModel actual constructor(actual val repository: ScriptRepository) : ScreenModel {

    private val _isLoading = MutableStateFlow(false)
    actual val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    actual suspend fun processPdfUri(projectId: Long, seriesNumber: Int, uriString: String) {
        _isLoading.value = true

        withContext(Dispatchers.IO) {
            try {
                val uri = android.net.Uri.parse(uriString)
                val inputStream = appContext.contentResolver.openInputStream(uri)
                val document = PDDocument.load(inputStream)
                val stripper = PDFTextStripper()
                val fullText = stripper.getText(document)
                document.close()

                repository.saveParsedScript(
                    projectId = projectId,
                    seriesNumber = seriesNumber,
                    filePath = uriString,
                    fullText = fullText,
                    createdAt = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    actual suspend fun processPdfUri(projectId: Long, uriString: String) {
        // Опционально: автоматическое определение серии из названия файла
    }

    actual fun deleteScriptFile(fileId: Long) {
        screenModelScope.launch(Dispatchers.IO) {
            repository.deleteScriptFile(fileId)
        }
    }

    actual fun updateScriptTitle(fileId: Long, newTitle: String) {
        screenModelScope.launch(Dispatchers.IO) {
            repository.updateScriptTitle(fileId, newTitle)
        }
    }
}
