package org.mosyagin.project.ui.screens

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.mosyagin.project.DatabaseQueries
import org.mosyagin.project.db.appContext
import org.mosyagin.project.parser.ParsedScene
import org.mosyagin.project.parser.ScriptParser

actual class ScriptViewModel actual constructor(actual val queries: DatabaseQueries) {

    private val _isLoading = MutableStateFlow(false)
    actual val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // 1. Добавили seriesNumber в аргументы функции
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

                // 2. Передаем seriesNumber в парсер
                val parser = ScriptParser()
                val parsedScenes = parser.parse(fullText, seriesNumber)

                queries.transaction {
                    queries.insertScriptFile(
                        projectId = projectId,
                        title = "Серия $seriesNumber", // Красивое название
                        filePath = uriString,
                        createdAt = System.currentTimeMillis()
                    )

                    parsedScenes.forEach { scene ->
                        try {
                            // 3. Используем переданный seriesNumber для БД
                            queries.insertScene(
                                projectId = projectId,
                                seriesNumber = seriesNumber.toString(),
                                sceneNumber = scene.sceneNumber.toString(),
                                location = scene.location,
                                isInterior = if (scene.type == "ИНТ") 1L else 0L,
                                timeOfDay = scene.time,
                                content = scene.content
                            )

                            val sceneId = queries.lastInsertRowId().executeAsOne()

                            scene.actors.forEach { actorName ->
                                val cleanName = actorName.trim()
                                if (cleanName.isNotEmpty()) {
                                    queries.insertActor(projectId, cleanName)
                                    val actor = queries.getActorByName(projectId, cleanName).executeAsOneOrNull()
                                    if (actor != null) {
                                        queries.linkActorToScene(sceneId, actor.id)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            println("CINE_ERROR: Ошибка сцены: ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    actual suspend fun processPdfUri(projectId: Long, uriString: String) {
    }
}
