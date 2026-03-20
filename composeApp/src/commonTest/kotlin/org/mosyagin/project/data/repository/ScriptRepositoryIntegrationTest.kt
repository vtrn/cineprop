package org.mosyagin.project.data.repository

import app.cash.sqldelight.db.SqlDriver
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.mosyagin.project.DatabaseQueries
import org.mosyagin.project.db.CinePropDatabase
import org.mosyagin.project.db.createTestDriver
import org.mosyagin.project.parser.ScriptParser
import org.mosyagin.project.repository.ScriptRepository
import org.mosyagin.project.repository.ScriptRepositoryImpl
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScriptRepositoryIntegrationTest {
    private lateinit var repository: ScriptRepository
    private lateinit var queries: DatabaseQueries
    private val parser = ScriptParser()

    @BeforeTest
    fun setup() {
        val driver = createTestDriver()
        CinePropDatabase.Schema.create(driver)
        val database = CinePropDatabase(driver)
        queries = database.databaseQueries
        repository = ScriptRepositoryImpl(queries, parser)
    }

    @Test
    fun testParseAndSaveIntegration() = runTest {
        // 1. Создаем проект
        queries.insertProject("Интеграционный тест", "Режиссер")
        val projectId = queries.getAllProjects().executeAsList()[0].id

        // 2. Тестовый текст сценария
        val scriptText = """
            1. ИНТ. ОФИС - ДЕНЬ
            АЛЕКСЕЙ сидит за столом.
            
            2. НАТ. ПАРК - ВЕЧЕР
            МАРИНА гуляет.
        """.trimIndent()

        // 3. Сохраняем через репозиторий
        repository.saveParsedScript(
            projectId = projectId,
            seriesNumber = 1,
            filePath = "test.pdf",
            fullText = scriptText,
            createdAt = 123456789L
        )

        // 4. Проверяем БД напрямую через queries
        val scenes = queries.getScenesByProject(projectId).executeAsList()
        assertEquals(2, scenes.size)
        
        assertEquals("1", scenes[0].sceneNumber)
        assertEquals("ОФИС", scenes[0].location)
        assertEquals(1L, scenes[0].isInterior) // ИНТ

        assertEquals("2", scenes[1].sceneNumber)
        assertEquals("ПАРК", scenes[1].location)
        assertEquals(0L, scenes[1].isInterior) // НАТ
    }

    @Test
    fun testActorsAreLinkedDuringParsing() = runTest {
        queries.insertProject("Актеры", "Режиссер")
        val projectId = queries.lastInsertRowId().executeAsOne()

        val scriptText = """
            1. ИНТ. КУХНЯ - ДЕНЬ
            ГЕРОЙ, МАМА
            Они пьют чай.
        """.trimIndent()

        repository.saveParsedScript(projectId, 1, "path", scriptText, 0L)

        val scenes = queries.getScenesByProject(projectId).executeAsList()
        val sceneId = scenes[0].id

        val actors = queries.getActorsForScene(sceneId).executeAsList()
        assertEquals(2, actors.size)
        assertTrue(actors.any { it.name == "ГЕРОЙ" })
        assertTrue(actors.any { it.name == "МАМА" })
    }
}
