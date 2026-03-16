package org.mosyagin.project.ui.screens

import android.net.Uri
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mosyagin.project.DatabaseQueries
import org.mosyagin.project.db.appContext // Твой глобальный контекст
import org.mosyagin.project.parser.ScriptParser

actual class ScriptViewModel actual constructor(actual val queries: DatabaseQueries) {

    actual suspend fun processPdfUri(projectId: Long, uriString: String) {
        withContext(Dispatchers.IO) {
            try {
                val uri = android.net.Uri.parse(uriString)
                val inputStream = appContext.contentResolver.openInputStream(uri)
                val document = PDDocument.load(inputStream)
                val stripper = PDFTextStripper()
                val fullText = stripper.getText(document)
                document.close()

                val parser = ScriptParser()
                val parsedScenes = parser.parse(fullText)

                // ВАЖНО: Выносим создание записи о файле ИЗ условия parsedScenes.isNotEmpty()
                // Чтобы мы видели файл в списке, даже если внутри 0 сцен
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
                println("CINE_DEBUG: Загрузка завершена. Найдено сцен: ${parsedScenes.size}")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}