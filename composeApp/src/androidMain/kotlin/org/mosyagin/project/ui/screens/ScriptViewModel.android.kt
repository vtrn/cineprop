/**
 * ViewModel для обработки файлов сценария (PDF) на платформе Android.
 * 
 * Основная задача: извлечь текст из PDF, распарсить его на сцены
 * и сохранить в локальную базу данных SQLDelight.
 */
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

    // Состояние загрузки для отображения индикатора на UI
    private val _isLoading = MutableStateFlow(false)
    actual val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * Основной метод обработки PDF.
     * 
     * @param projectId ID проекта, к которому относится сценарий.
     * @param seriesNumber Номер серии (важно для связки с КПП).
     * @param uriString Путь к файлу в системе Android.
     */
    actual suspend fun processPdfUri(projectId: Long, seriesNumber: Int, uriString: String) {
        _isLoading.value = true

        withContext(Dispatchers.IO) {
            try {
                // 1. Извлекаем текст из PDF с помощью библиотеки PDFBox
                val uri = android.net.Uri.parse(uriString)
                val inputStream = appContext.contentResolver.openInputStream(uri)
                val document = PDDocument.load(inputStream)
                val stripper = PDFTextStripper()
                val fullText = stripper.getText(document)
                document.close()

                // 2. Парсим текст (ищем заголовки сцен, время, локации)
                val parser = ScriptParser()
                val parsedScenes = parser.parse(fullText, seriesNumber)

                // 3. Сохраняем все данные в БД в рамках одной транзакции (для скорости и надежности)
                queries.transaction {
                    // Записываем информацию о самом файле
                    queries.insertScriptFile(
                        projectId = projectId,
                        title = "Серия $seriesNumber",
                        filePath = uriString,
                        createdAt = System.currentTimeMillis()
                    )

                    // Сохраняем каждую найденную сцену
                    parsedScenes.forEach { scene ->
                        try {
                            queries.insertScene(
                                projectId = projectId,
                                seriesNumber = seriesNumber.toString(),
                                sceneNumber = scene.sceneNumber.toString(),
                                location = scene.location,
                                isInterior = if (scene.type == "ИНТ") 1L else 0L,
                                timeOfDay = scene.time,
                                content = scene.content
                            )

                            // Получаем ID только что вставленной сцены для связки с актерами
                            val sceneId = queries.lastInsertRowId().executeAsOne()

                            // Сохраняем актеров и привязываем их к сцене
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

    // Заглушка для совместимости с общим интерфейсом
    actual suspend fun processPdfUri(projectId: Long, uriString: String) {
    }
}
