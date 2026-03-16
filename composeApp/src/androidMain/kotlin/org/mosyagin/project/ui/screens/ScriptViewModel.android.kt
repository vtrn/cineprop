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
        _isLoading.value = true

        withContext(Dispatchers.IO) {
            try {
                val uri = android.net.Uri.parse(uriString)
                val inputStream = appContext.contentResolver.openInputStream(uri)
                val document = com.tom_roush.pdfbox.pdmodel.PDDocument.load(inputStream)
                val stripper = com.tom_roush.pdfbox.text.PDFTextStripper()
                val fullText = stripper.getText(document)
                document.close()

                val parser = ScriptParser()
                val parsedScenes = parser.parse(fullText)

                queries.transaction {
                    // 1. Сохраняем информацию о файле
                    queries.insertScriptFile(
                        projectId = projectId,
                        title = "Сценарий (${parsedScenes.size} сцен)",
                        filePath = uriString,
                        createdAt = System.currentTimeMillis()
                    )

                    parsedScenes.forEach { scene ->
                        try {
                            // 2. Сохраняем сцену
                            queries.insertScene(
                                projectId = projectId,
                                sceneNumber = scene.number,
                                location = scene.location,
                                isInterior = if (scene.type == "ИНТ") 1L else 0L,
                                timeOfDay = scene.time,
                                content = scene.content
                            )

                            // Получаем ID только что созданной сцены
                            val sceneId = queries.lastInsertRowId().executeAsOne()

                            // 3. Сохраняем актеров
                            scene.actors.forEach { actorName ->
                                val cleanName = actorName.trim()
                                if (cleanName.isNotEmpty()) {
                                    // Пытаемся вставить актера (если он уже есть, сработает IGNORE из SQL)
                                    queries.insertActor(projectId, cleanName)

                                    // Достаем его ID
                                    val actor = queries.getActorByName(projectId, cleanName)
                                        .executeAsOneOrNull()

                                    // Связываем сцену и актера
                                    if (actor != null) {
                                        queries.linkActorToScene(sceneId, actor.id)
                                    } else {
                                        println("CINE_ERROR: Не удалось найти/создать актера: $cleanName")
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            println("CINE_ERROR: Ошибка при сохранении сцены ${scene.number}: ${e.message}")
                        }
                    }
                }
                println("CINE_DEBUG: Все данные успешно записаны в БД")
            } catch (e: Exception) {
                println("CINE_ERROR: Критическая ошибка парсинга: ${e.message}")
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
}