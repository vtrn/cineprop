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
import org.mosyagin.project.DatabaseQueries
import org.mosyagin.project.db.appContext
import org.mosyagin.project.parser.ScriptParser

actual class ScriptViewModel actual constructor(actual val queries: DatabaseQueries) : ScreenModel {

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

                val parser = ScriptParser()
                val parsedScenes = parser.parse(fullText, seriesNumber)

                queries.transaction {
                    // 1. Создаем запись о файле
                    queries.insertScriptFile(
                        projectId = projectId,
                        title = "Серия $seriesNumber",
                        filePath = uriString,
                        createdAt = System.currentTimeMillis()
                    )
                    
                    val scriptFileId = queries.lastInsertRowId().executeAsOne()

                    // 2. Сохраняем сцены с привязкой к файлу
                    parsedScenes.forEach { scene ->
                        try {
                            queries.insertScene(
                                projectId = projectId,
                                scriptFileId = scriptFileId, // Передаем ID файла
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
                            println("CINE_ERROR: Ошибка обработки сцены: ${e.message}")
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
        // Опционально: автоматическое определение серии из названия файла
    }

    actual fun deleteScriptFile(fileId: Long) {
        screenModelScope.launch(Dispatchers.IO) {
            queries.transaction {
                // Благодаря ON DELETE CASCADE в БД, сцены удалятся автоматически
                // Но мы можем удалить их и явно, если CASCADE не сработает (хотя должен)
                queries.deleteScenesByScriptFile(fileId)
                queries.deleteScriptFile(fileId)
            }
        }
    }

    actual fun updateScriptTitle(fileId: Long, newTitle: String) {
        screenModelScope.launch(Dispatchers.IO) {
            queries.updateScriptFileTitle(newTitle, fileId)
        }
    }
}
