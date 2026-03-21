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
        var currentDate = "Неизвестно"
        var currentPosition: Long = 0

        runBlocking {
            rows.forEach { row ->
                val cells = row.split(";").map { it.trim() }
                if (cells.isEmpty()) return@forEach

                val dateRegex = Regex("""\d{2}\.\d{2}\.\d{4}""")
                val foundDate = cells.find { dateRegex.containsMatchIn(it) }
                if (foundDate != null) {
                    currentDate = foundDate
                }

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

                val series = cells.getOrNull(0)
                val sceneNumber = cells.getOrNull(1)

                if (!series.isNullOrBlank() && !sceneNumber.isNullOrBlank() && series.any { it.isDigit() }) {
                    
                    val shift = shiftRepository.getShiftByNumber(projectId, currentShiftNumber)
                    val shiftId = shift?.id ?: shiftRepository.addShift(projectId, currentShiftNumber, currentDate)

                    val sceneUserDataId = sceneRepository.getSceneUserDataIdBySeriesAndNumber(projectId, series, sceneNumber)

                    if (sceneUserDataId != null) {
                        shiftRepository.linkSceneToShift(
                            shiftId = shiftId, 
                            sceneUserDataId = sceneUserDataId, 
                            position = currentPosition
                        )
                        currentPosition++
                    }
                }
            }
        }
    }
}
