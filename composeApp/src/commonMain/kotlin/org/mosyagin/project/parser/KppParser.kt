package org.mosyagin.project.parser

import org.mosyagin.project.DatabaseQueries

class KppParser(private val queries: DatabaseQueries) {

    fun parseAndSaveKpp(projectId: Long, csvText: String) {
        val rows = csvText.lines()

        // Переменные для хранения текущего контекста
        var currentShiftNumber: Long = 1
        var currentDate: String = "Неизвестно"

        var totalRows = 0
        var linkedScenes = 0

        queries.transaction {
            rows.forEach { row ->
                val cells = row.split(";").map { it.trim() }
                if (cells.isEmpty()) return@forEach

                // 1. Ищем дату (формат ДД.ММ.ГГГГ)
                val dateRegex = Regex("""\d{2}\.\d{2}\.\d{4}""")
                val foundDate = cells.find { dateRegex.containsMatchIn(it) }
                if (foundDate != null) {
                    currentDate = foundDate
                }

                // 2. Ищем номер смены
                val shiftRegex = Regex("""СМЕНА\s*№?\s*(\d+)""", RegexOption.IGNORE_CASE)
                cells.forEach { cell ->
                    shiftRegex.find(cell)?.let { match ->
                        currentShiftNumber = match.groupValues[1].toLongOrNull() ?: currentShiftNumber
                    }
                }

                // 3. Достаем Серию и Сцену (теперь как TEXT)
                val series = cells.getOrNull(0)
                val sceneNumber = cells.getOrNull(1)

                // Если оба поля не пустые и серия/сцена похожи на данные (не заголовки)
                if (!series.isNullOrBlank() && !sceneNumber.isNullOrBlank() && series.any { it.isDigit() }) {
                    totalRows++

                    // Обеспечиваем наличие смены
                    var shiftId = queries.getShiftByNumber(projectId, currentShiftNumber).executeAsOneOrNull()?.id
                    if (shiftId == null) {
                        queries.insertShift(projectId, currentShiftNumber, currentDate)
                        shiftId = queries.lastInsertRowId().executeAsOne()
                    }

                    // Ищем ID сцены (сравнение строк теперь точное, включая "36-А")
                    val sceneId = queries.getSceneIdBySeriesAndNumber(projectId, series, sceneNumber)
                        .executeAsOneOrNull()

                    if (sceneId != null && shiftId != null) {
                        queries.linkShiftToScene(shiftId, sceneId)
                        linkedScenes++
                    }
                }
            }
        }

        println("CINE_DEBUG: Обработка завершена.")
        println("CINE_DEBUG: Найдено строк со сценами: $totalRows")
        println("CINE_DEBUG: Успешно привязано к сценам в базе: $linkedScenes")
    }
}
