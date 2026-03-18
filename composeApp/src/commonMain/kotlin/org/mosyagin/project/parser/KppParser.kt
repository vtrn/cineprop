package org.mosyagin.project.parser

import org.mosyagin.project.DatabaseQueries

class KppParser(private val queries: DatabaseQueries) {

    fun parseAndSaveKpp(projectId: Long, csvText: String) {
        val rows = csvText.lines().filter { it.contains(";") }

        queries.transaction {
            rows.forEach { row ->
                val cells = row.split(";")
                // Пропускаем строки, где нет данных
                if (cells.size >= 2) {
                    val series = cells[0].toIntOrNull()
                    val sceneNumber = cells[1].toIntOrNull()

                    if (series != null && sceneNumber != null) {
                        // Ищем ID сцены в таблице Scene по серии и номеру
                        val sceneId = queries.getSceneIdBySeriesAndNumber(projectId, series.toLong(), sceneNumber.toLong())
                            .executeAsOneOrNull()

                        // Если нашли, создаем запись в Shift (если её нет) и связываем
                        if (sceneId != null) {
                            // Тут логика создания Смены (Shift)
                            // Для теста: можно просто вывести в лог
                            println("CINE_DEBUG: Нашел сцену $series-$sceneNumber, ID: $sceneId")
                        }
                    }
                }
            }
        }
    }
}