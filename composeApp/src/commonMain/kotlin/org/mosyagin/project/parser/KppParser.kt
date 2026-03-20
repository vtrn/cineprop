/**
 * Парсер Календарно-постановочного плана (КПП).
 */
package org.mosyagin.project.parser

import kotlinx.coroutines.runBlocking
import org.mosyagin.project.repository.SceneRepository
import org.mosyagin.project.repository.ShiftRepository

class KppParser(
    private val sceneRepository: SceneRepository,
    private val shiftRepository: ShiftRepository
) {

    /**
     * Основной метод парсинга CSV текста.
     */
    fun parseAndSaveKpp(projectId: Long, csvText: String) {
        val rows = csvText.lines()

        var currentShiftNumber: Long = 1
        var currentDate: String = "Неизвестно"
        var currentPosition: Long = 0

        // Используем runBlocking для упрощения миграции, 
        // так как репозитории используют приостанавливаемые функции
        runBlocking {
            rows.forEach { row ->
                val cells = row.split(";").map { it.trim() }
                if (cells.isEmpty()) return@forEach

                // 1. Поиск даты
                val dateRegex = Regex("""\d{2}\.\d{2}\.\d{4}""")
                val foundDate = cells.find { dateRegex.containsMatchIn(it) }
                if (foundDate != null) {
                    currentDate = foundDate
                }

                // 2. Поиск номера смены
                val shiftRegex = Regex("""СМЕНА\s*№?\s*(\d+)""", RegexOption.IGNORE_CASE)
                cells.forEach { cell ->
                    shiftRegex.find(cell)?.let { match ->
                        val newShiftNumber = match.groupValues[1].toLongOrNull() ?: currentShiftNumber
                        if (newShiftNumber != currentShiftNumber) {
                            currentShiftNumber = newShiftNumber
                            currentPosition = 0
                        }
                    }
                }

                // 3. Извлечение Серии и Сцены
                val series = cells.getOrNull(0)
                val sceneNumber = cells.getOrNull(1)

                if (!series.isNullOrBlank() && !sceneNumber.isNullOrBlank() && series.any { it.isDigit() }) {
                    
                    val shift = shiftRepository.getShiftByNumber(projectId, currentShiftNumber)
                    val shiftId = if (shift == null) {
                        shiftRepository.addShift(projectId, currentShiftNumber, currentDate)
                    } else {
                        shift.id
                    }

                    // Теперь sceneRepository — это репозиторий, и метод вернет Long?
                    val sceneId = sceneRepository.getSceneIdBySeriesAndNumber(projectId, series, sceneNumber)

                    if (sceneId != null) {
                        shiftRepository.linkSceneToShift(shiftId, sceneId, currentPosition)
                        currentPosition++
                    }
                }
            }
        }
    }
}
