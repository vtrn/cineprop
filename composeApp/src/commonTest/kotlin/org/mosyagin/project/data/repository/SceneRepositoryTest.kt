package org.mosyagin.project.data.repository

import app.cash.sqldelight.db.SqlDriver
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.koin.core.context.stopKoin
import org.mosyagin.project.repository.SceneRepository
import org.mosyagin.project.repository.SceneRepositoryImpl
import org.mosyagin.project.DatabaseQueries
import org.mosyagin.project.db.CinePropDatabase
import org.mosyagin.project.db.createTestDriver
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SceneRepositoryTest {
    private lateinit var repository: SceneRepository
    private lateinit var queries: DatabaseQueries
    private lateinit var driver: SqlDriver
    private var testProjectId: Long = 0
    private var testScriptId: Long = 0
    private var testUserDataId: Long = 0

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
        queries = database.databaseQueries
        repository = SceneRepositoryImpl(queries)

        // Создаем базовую структуру для тестов
        queries.insertProject("Проект", "Реж")
        testProjectId = queries.lastInsertRowId().executeAsOne()

        queries.insertScriptFile(
            projectId = testProjectId,
            seriesNumber = 1L,
            title = "Сценарий",
            filePath = "path/to/file",
            createdAt = 123456789L,
            previousVersionId = null,
            revisionColor = "White",
            uploadedBy = "User"
        )
        testScriptId = queries.lastInsertRowId().executeAsOne()

        queries.insertSceneUserData(
            projectId = testProjectId,
            seriesNumber = 1L,
            sceneNumber = "1",
            location = "ЛОКАЦИЯ",
            isInterior = 1L,
            timeOfDay = "ДЕНЬ",
            notes = null,
            needsReview = 0L
        )
        testUserDataId = queries.lastInsertRowId().executeAsOne()
    }

    @AfterTest
    fun tearDown() {
        driver.close()
        stopKoin()
    }

    @Test
    fun testAddAndGetProp() = runTest {
        queries.insertSceneVersion(testScriptId, testUserDataId, "Текст сцены", "hash", 0)

        repository.addProp(testUserDataId, "Меч", "в руках меч", "Найти", 0, 10)

        val props = repository.getPropsForScene(testUserDataId).first()
        assertEquals(1, props.size)
        assertEquals("Меч", props[0].name)
        assertEquals("в руках меч", props[0].anchor)
    }

    @Test
    fun testUpdatePropStatus() = runTest {
        repository.addProp(testUserDataId, "Ваза", "красивая ваза")
        val props = repository.getPropsByProject(testProjectId).first()
        assertTrue(props.isNotEmpty(), "Props should be added")
        val propId = props[0].id

        repository.updatePropStatus(propId, "Готово")

        val updatedProps = repository.getPropsByProject(testProjectId).first()
        assertTrue(updatedProps.isNotEmpty(), "Props should exist")
        assertEquals("Готово", updatedProps[0].status)
    }
}
