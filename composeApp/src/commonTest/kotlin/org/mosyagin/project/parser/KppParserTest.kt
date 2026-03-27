package org.mosyagin.project.parser

import kotlin.test.Test
import kotlin.test.assertEquals
import org.mosyagin.project.GetScenesByProject
import org.mosyagin.project.repository.FakeSceneRepository
import org.mosyagin.project.repository.FakeShiftRepository

class KppParserTest {

    private val sceneRepo = FakeSceneRepository()
    private val shiftRepo = FakeShiftRepository()
    private val parser = KppParser(sceneRepo, shiftRepo)

    @Test
    fun testKppParsingAndLinking() {
        val projectId = 1L
        
        // 1. Предварительно добавляем сцены в "базу"
        sceneRepo.addFakeScene(GetScenesByProject(101, projectId, 1L, "1", "КУХНЯ", 1, "ДЕНЬ", null, 0, "Текст", "hash"))
        sceneRepo.addFakeScene(GetScenesByProject(102, projectId, 1L, "2", "ДВОР", 0, "НОЧЬ", null, 0, "Текст", "hash"))

        // 2. CSV текст КПП (Серия; Сцена; ...)
        val csvText = """
            15.03.2024;;;;;
            СМЕНА №1;;;;;
            1; 1; КУХНЯ; ИНТ; ДЕНЬ
            1; 2; ДВОР; НАТ; НОЧЬ
        """.trimIndent()

        // 3. Запускаем парсинг
        parser.parseAndSaveKpp(projectId, csvText)

        // 4. Проверяем результаты
        // Должно быть создано 2 связи (ShiftScene)
        assertEquals(2, shiftRepo.getLinksCount())
    }

    @Test
    fun testMultipleShiftsInOneCsv() {
        val projectId = 1L
        
        sceneRepo.addFakeScene(GetScenesByProject(101, projectId, 1L, "1", "КУХНЯ", 1, "ДЕНЬ", null, 0, "Текст", "hash"))
        sceneRepo.addFakeScene(GetScenesByProject(102, projectId, 1L, "2", "ДВОР", 0, "НОЧЬ", null, 0, "Текст", "hash"))

        val csvText = """
            15.03.2024;;;;;
            СМЕНА №1;;;;;
            1; 1; КУХНЯ; ИНТ; ДЕНЬ
            16.03.2024;;;;;
            СМЕНА №2;;;;;
            1; 2; ДВОР; НАТ; НОЧЬ
        """.trimIndent()

        parser.parseAndSaveKpp(projectId, csvText)

        assertEquals(2, shiftRepo.getLinksCount())
    }
}
