/**
 * Парсер Календарно-постановочного плана (КПП).
 * 
 * Отвечает за чтение CSV-файлов, извлечение из них информации о сменах
 * и создание связей между сменами и сценами в базе данных.
 */
package org.mosyagin.project.parser

import org.mosyagin.project.DatabaseQueries

class KppParser(private val queries: DatabaseQueries) {

    /**
     * Основной метод парсинга CSV текста.
     * 
     * Ожидает CSV, где:
     * - Номер серии в первой колонке.
     * - Номер сцены во второй колонке.
     * - Содержит строки "СМЕНА №X" и даты в формате "ДД.ММ.ГГГГ".
     * 
     * @param projectId ID текущего проекта.
     * @param csvText Сырой текст из CSV файла.
     */
    fun parseAndSaveKpp(projectId: Long, csvText: String) {
        val rows = csvText.lines()

        // Текущие значения, которые обновляются по мере прохода по строкам CSV
        var currentShiftNumber: Long = 1
        var currentDate: String = "Неизвестно"
        
        // Счетчик позиции внутри смены для сохранения порядка из КПП
        var currentPosition: Long = 0

        var totalRows = 0
        var linkedScenes = 0

        queries.transaction {
            rows.forEach { row ->
                val cells = row.split(";").map { it.trim() }
                if (cells.isEmpty()) return@forEach

                // 1. Поиск даты (ищем паттерн типа 15.03.2024)
                val dateRegex = Regex("""\d{2}\.\d{2}\.\d{4}""")
                val foundDate = cells.find { dateRegex.containsMatchIn(it) }
                if (foundDate != null) {
                    currentDate = foundDate
                }

                // 2. Поиск номера смены (строки типа "Смена №5")
                val shiftRegex = Regex("""СМЕНА\s*№?\s*(\d+)""", RegexOption.IGNORE_CASE)
                cells.forEach { cell ->
                    shiftRegex.find(cell)?.let { match ->
                        val newShiftNumber = match.groupValues[1].toLongOrNull() ?: currentShiftNumber
                        if (newShiftNumber != currentShiftNumber) {
                            currentShiftNumber = newShiftNumber
                            currentPosition = 0 // Сбрасываем позицию при начале новой смены
                        }
                    }
                }

                // 3. Извлечение Серии и Сцены
                val series = cells.getOrNull(0)
                val sceneNumber = cells.getOrNull(1)

                // Если строка похожа на данные сцены (есть номер серии и сцены)
                if (!series.isNullOrBlank() && !sceneNumber.isNullOrBlank() && series.any { it.isDigit() }) {
                    totalRows++

                    // Проверяем, создана ли уже такая смена в БД, если нет — создаем
                    var shiftId = queries.getShiftByNumber(projectId, currentShiftNumber).executeAsOneOrNull()?.id
                    if (shiftId == null) {
                        queries.insertShift(projectId, currentShiftNumber, currentDate)
                        shiftId = queries.lastInsertRowId().executeAsOne()
                    }

                    // Ищем ID сцены в базе (сцена должна быть предварительно загружена через PDF)
                    val sceneId = queries.getSceneIdBySeriesAndNumber(projectId, series, sceneNumber)
                        .executeAsOneOrNull()

                    // Если сцена найдена — создаем связь "Смена <-> Сцена" с указанием позиции
                    if (sceneId != null && shiftId != null) {
                        queries.linkShiftToScene(shiftId, sceneId, currentPosition)
                        currentPosition++ // Увеличиваем позицию для следующей сцены
                        linkedScenes++
                    }
                }
            }
        }

        // Логирование результатов в консоль для отладки
        println("CINE_DEBUG: Обработка завершена.")
        println("CINE_DEBUG: Найдено строк со сценами: $totalRows")
        println("CINE_DEBUG: Успешно привязано к сценам в базе: $linkedScenes")
    }
}
