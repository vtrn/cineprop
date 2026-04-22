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
import org.mosyagin.project.repository.FakeSyncRepository
import org.mosyagin.project.repository.FakeActivityRepository
import org.mosyagin.project.crypto.PlainDataEncrypter
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SceneRepositoryTest {
    private lateinit var repository: SceneRepository
    private lateinit var queries: DatabaseQueries
    private lateinit var driver: SqlDriver
    private var testProjectId: String = ""
    private var testScriptId: String = ""
    private var testUserDataId: String = ""

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
        repository = SceneRepositoryImpl(queries, FakeSyncRepository(), PlainDataEncrypter(), FakeActivityRepository())

        // Создаем базовую структуру для тестов
        val projectId = "test-project-id"
        // Добавлен 6-й параметр created_by
        queries.insertProject(
            id = projectId, 
            name = "Проект", 
            director = "Реж", 
            updatedAt = 0L, 
            isRemote = 0L, 
            created_by = "test@example.com"
        )
        testProjectId = projectId

        val scriptId = "test-script-id"
        queries.insertScriptFile(
            id = scriptId,
            project_id = testProjectId, // Изменено с projectId на project_id
            seriesNumber = 1L,
            title = "Сценарий",
            filePath = "path/to/file",
            createdAt = 123456789L,
            previousVersionId = null,
            revisionColor = "White",
            uploadedBy = "User",
            updatedAt = 0L
        )
        testScriptId = scriptId

        val userDataId = "test-user-data-id"
        queries.insertSceneUserData(
            id = userDataId,
            project_id = testProjectId, // Изменено с projectId на project_id
            seriesNumber = 1L,
            sceneNumber = "1",
            location = "ЛОКАЦИЯ",
            isInterior = 1L,
            timeOfDay = "ДЕНЬ",
            notes = null,
            needsReview = 0L,
            updatedAt = 0L,
            isDecrypted = 0L
        )
        testUserDataId = userDataId
    }

    @AfterTest
    fun tearDown() {
        driver.close()
        stopKoin()
    }

    @Test
    fun testAddAndGetProp() = runTest {
        queries.insertSceneVersion(
            id = "version-id",
            scriptFileId = testScriptId,
            sceneUserDataId = testUserDataId,
            content = "Текст сцены",
            contentHash = "hash",
            positionIndex = 0,
            updatedAt = 0L,
            isDecrypted = 0L
        )

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
        assertEquals("Готово", updatedProps[0].status.toString()) // Преобразуем статус в строку для сравнения
    }
}
