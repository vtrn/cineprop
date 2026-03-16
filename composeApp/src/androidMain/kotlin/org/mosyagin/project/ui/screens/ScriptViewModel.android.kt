package org.mosyagin.project.ui.screens

import android.net.Uri
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.mosyagin.project.DatabaseQueries
import org.mosyagin.project.db.appContext
import org.mosyagin.project.parser.ScriptParser

actual class ScriptViewModel actual constructor(actual val queries: DatabaseQueries) {

    // Внутренняя переменная (меняем здесь)
    private val _isLoading = MutableStateFlow(false)
    // Публичная переменная (экран только читает)
    actual val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    actual suspend fun processPdfUri(projectId: Long, uriString: String) {
        _isLoading.value = true // ВКЛЮЧАЕМ ЛОАДЕР

        withContext(Dispatchers.IO) {
            try {
                val uri = Uri.parse(uriString)
                val inputStream = appContext.contentResolver.openInputStream(uri)
                val document = PDDocument.load(inputStream)
                val stripper = PDFTextStripper()
                val fullText = stripper.getText(document)
                document.close()

                val parser = ScriptParser()
                val parsedScenes = parser.parse(fullText)

                queries.transaction {
                    queries.insertScriptFile(
                        projectId = projectId,
                        title = "Сценарий (${parsedScenes.size} сцен)",
                        filePath = uriString,
                        createdAt = System.currentTimeMillis()
                    )
                    parsedScenes.forEach { scene ->
                        queries.insertScene(
                            projectId = projectId,
                            sceneNumber = scene.number,
                            location = scene.location,
                            isInterior = if (scene.type == "ИНТ") 1L else 0L,
                            timeOfDay = scene.time,
                            content = scene.content
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false // ВЫКЛЮЧАЕМ ЛОАДЕР (в любом случае, даже при ошибке)
            }
        }
    }
}