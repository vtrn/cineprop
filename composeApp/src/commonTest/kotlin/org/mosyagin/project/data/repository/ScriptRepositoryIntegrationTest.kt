package org.mosyagin.project.data.repository

import app.cash.sqldelight.db.SqlDriver
import kotlinx.coroutines.test.runTest
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject
import org.mosyagin.project.DatabaseQueries
import org.mosyagin.project.db.CinePropDatabase
import org.mosyagin.project.db.createTestDriver
import org.mosyagin.project.parser.ScriptParser
import org.mosyagin.project.parser.update.ScriptUpdateManager
import org.mosyagin.project.repository.FakeSyncRepository
import org.mosyagin.project.repository.ScriptRepository
import org.mosyagin.project.repository.ScriptRepositoryImpl
import org.mosyagin.project.repository.SyncRepository
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScriptRepositoryIntegrationTest : KoinTest {
    private val repository: ScriptRepository by inject()
    private val queries: DatabaseQueries by inject()
    private val updateManager: ScriptUpdateManager by inject()
    private lateinit var driver: SqlDriver

    @BeforeTest
    fun setup() {
        driver = createTestDriver()
        driver.execute(null, "PRAGMA foreign_keys=ON;", 0)
        
        try {
            CinePropDatabase.Schema.create(driver)
        } catch (e: Exception) {
            // Схема уже может быть создана внутри createTestDriver() на некоторых платформах
        }

        val database = CinePropDatabase(driver)
        val dbQueries = database.databaseQueries

        startKoin {
            modules(module {
                single { dbQueries }
                single { ScriptParser() }
                single<SyncRepository> { FakeSyncRepository() }
                // Передаем по 3 параметра get(), так как конструкторы обновились
                single { ScriptUpdateManager(get(), get(), get()) }
                single<ScriptRepository> { ScriptRepositoryImpl(get(), get(), get()) }
            })
        }
    }

    @AfterTest
    fun tearDown() {
        if (::driver.isInitialized) {
            driver.close()
        }
        stopKoin()
    }

    @Test
    fun testParseAndSaveIntegration() = runTest {
        // Добавлен updatedAt = 0
        queries.insertProject("Интеграционный тест", "Режиссер", 0L)
        val projectId = queries.lastInsertRowId().executeAsOne()

        val scriptText = """
            1. ИНТ. ОФИС - ДЕНЬ
            АЛЕКСЕЙ
            Сидит за столом.
            
            2. НАТ. ПАРК - ВЕЧЕР
            МАРИНА
            Гуляет.
        """.trimIndent()

        repository.saveParsedScript(
            projectId = projectId,
            seriesNumber = 1,
            filePath = "test.pdf",
            fullText = scriptText,
            createdAt = 123456789L
        )

        val scriptFiles = queries.getScriptsForProject(projectId).executeAsList()
        assertTrue(scriptFiles.isNotEmpty(), "Script files should not be empty")
        val scriptFileId = scriptFiles.first().id
        
        val scenes = queries.getScenesByProject(projectId, scriptFileId).executeAsList()
        
        assertEquals(2, scenes.size)
        assertEquals("1", scenes[0].sceneNumber)
        assertEquals("ОФИС", scenes[0].location)
    }

    @Test
    fun testActorsAreLinkedDuringParsing() = runTest {
        // Добавлен updatedAt = 0
        queries.insertProject("Актеры", "Режиссер", 0L)
        val projectId = queries.lastInsertRowId().executeAsOne()

        val scriptText = """
            1. ИНТ. КУХНЯ - ДЕНЬ
            ГЕРОЙ
            МАМА
            Они пьют чай.
        """.trimIndent()

        val result = updateManager.prepareUpdate(
            projectId = projectId,
            seriesNumber = 1,
            filePath = "path",
            fullText = scriptText,
            createdAt = 1710500000000L
        )
        
        assertTrue(result is org.mosyagin.project.parser.update.UpdateResult.Success, "Preparation should be successful")
        updateManager.executeUpdate(result.previewData)

        val scriptFiles = queries.getScriptsForProject(projectId).executeAsList()
        val scriptFileId = scriptFiles.first().id
        val scenes = queries.getScenesByProject(projectId, scriptFileId).executeAsList()
        
        assertTrue(scenes.isNotEmpty(), "Scenes should not be empty")
        val sceneUserDataId = scenes[0].id

        val actors = queries.getActorsForScene(sceneUserDataId).executeAsList()
        
        assertEquals(2, actors.size, "Should find 2 actors: ГЕРОЙ and МАМА")
        assertTrue(actors.any { it.name == "ГЕРОЙ" }, "Should contain ГЕРОЙ")
        assertTrue(actors.any { it.name == "МАМА" }, "Should contain МАМА")
    }
}
