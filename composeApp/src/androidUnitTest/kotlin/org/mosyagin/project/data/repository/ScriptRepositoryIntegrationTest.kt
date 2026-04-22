package org.mosyagin.project.data.repository

import kotlinx.coroutines.test.runTest
import org.mosyagin.project.DatabaseQueries
import org.mosyagin.project.db.CinePropDatabase
import org.mosyagin.project.db.createTestDriver
import org.mosyagin.project.parser.ScriptParser
import org.mosyagin.project.repository.FakeSyncRepository
import org.mosyagin.project.repository.ScriptRepository
import org.mosyagin.project.repository.ScriptRepositoryImpl
import org.mosyagin.project.repository.FakeActivityRepository
import org.mosyagin.project.crypto.PlainDataEncrypter
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class ScriptRepositoryAndroidIntegrationTest {
    private lateinit var repository: ScriptRepository
    private lateinit var queries: DatabaseQueries
    private val parser = ScriptParser()

    @BeforeTest
    fun setup() {
        val driver = createTestDriver()
        val database = CinePropDatabase(driver)
        queries = database.databaseQueries
        repository = ScriptRepositoryImpl(queries, parser, FakeSyncRepository(), PlainDataEncrypter(), FakeActivityRepository())
    }

    @Test
    fun testParseAndSaveIntegration() = runTest {
        val projectId = "test-project-id"
        // Обновлено: добавлен 6-й параметр created_by
        queries.insertProject(projectId, "Интеграционный тест", "Режиссер", 0L, 0L, "test@example.com")

        val scriptText = """
            1. ИНТ. ОФИС - ДЕНЬ
            АЛЕКСЕЙ сидит за столом.
            
            2. НАТ. ПАРК - ВЕЧЕР
            МАРИНА гуляет.
        """.trimIndent()

        repository.saveParsedScript(
            projectId = projectId,
            seriesNumber = 1,
            filePath = "test.pdf",
            fullText = scriptText,
            createdAt = 123456789L
        )

        val scriptFiles = queries.getScriptsForProject(projectId).executeAsList()
        val scriptFileId = scriptFiles.last().id

        val scenes = queries.getScenesByProject(projectId, scriptFileId).executeAsList()
        assertEquals(2, scenes.size)
        
        assertEquals("1", scenes[0].sceneNumber)
        assertEquals("ОФИС", scenes[0].location)
        assertEquals(1L, scenes[0].isInterior) 

        assertEquals("2", scenes[1].sceneNumber)
        assertEquals("ПАРК", scenes[1].location)
        assertEquals(0L, scenes[1].isInterior)
    }

    @Test
    fun testActorsAreLinkedDuringParsing() = runTest {
        val projectId = "actors-test-project-id"
        // Обновлено: добавлен 6-й параметр created_by
        queries.insertProject(projectId, "Актеры", "Режиссер", 0L, 0L, "test@example.com")

        val scriptText = """
            1. ИНТ. КУХНЯ - ДЕНЬ
            ГЕРОЙ, МАМА
            Они пьют чай.
        """.trimIndent()

        repository.saveParsedScript(projectId, 1, "path", scriptText, 0L)

        val scriptFiles = queries.getScriptsForProject(projectId).executeAsList()
        val scriptFileId = scriptFiles.last().id
        val scenes = queries.getScenesByProject(projectId, scriptFileId).executeAsList()
        val sceneUserDataId = scenes[0].id

        val actors = queries.getActorsForScene(sceneUserDataId).executeAsList()
        assertTrue(actors.isNotEmpty())
        assertTrue(actors.any { it.name == "ГЕРОЙ" })
        assertTrue(actors.any { it.name == "МАМА" })
    }
}
