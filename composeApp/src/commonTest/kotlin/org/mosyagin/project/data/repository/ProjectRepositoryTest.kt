package org.mosyagin.project.data.repository

import app.cash.sqldelight.db.SqlDriver
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.koin.core.context.stopKoin
import org.mosyagin.project.repository.ProjectRepository
import org.mosyagin.project.DatabaseQueries
import org.mosyagin.project.db.CinePropDatabase
import org.mosyagin.project.db.createTestDriver
import org.mosyagin.project.repository.FakeSyncRepository
import org.mosyagin.project.repository.SceneRepositoryImpl
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProjectRepositoryTest {
    private lateinit var repository: ProjectRepository
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
        repository = SceneRepositoryImpl(queries, FakeSyncRepository())

        // СОЗДАЕМ ОБЯЗАТЕЛЬНУЮ ИЕРАРХИЮ
        // 1. Проект (добавлен updatedAt = 0)
        queries.insertProject("Test Project", "Director", 0L)
        val projectId = queries.lastInsertRowId().executeAsOne()

        // 2. Файл сценария (добавлен updatedAt = 0)
        queries.insertScriptFile(
            projectId = projectId,
            seriesNumber = 1L,
            title = "Version 1",
            filePath = "/mock/path",
            createdAt = 123456789L,
            previousVersionId = null,
            revisionColor = "White",
            uploadedBy = "Tester",
            updatedAt = 0L
        )
        val scriptId = queries.lastInsertRowId().executeAsOne()

        // 3. Якорь сцены (добавлен updatedAt = 0)
        queries.insertSceneUserData(
            projectId = projectId,
            seriesNumber = 1L,
            sceneNumber = "1",
            location = "Lobby",
            isInterior = 1L,
            timeOfDay = "Day",
            notes = null,
            needsReview = 0L,
            updatedAt = 0L
        )
        val userDataId = queries.lastInsertRowId().executeAsOne()

        // Сохраняем ID для использования в тестах
        this.testProjectId = projectId
        this.testScriptId = scriptId
        this.testUserDataId = userDataId
    }

    @AfterTest
    fun tearDown() {
        driver.close()
        stopKoin()
    }

    @Test
    fun testAddAndGetAllProjects() = runTest {
        repository.addProject("Тестовый проект", "Режиссер")
        
        val projects = repository.getAllProjects().first()
        
        // В БД уже есть 1 проект из setup()
        assertEquals(2, projects.size)
        assertTrue(projects.any { it.name == "Тестовый проект" })
    }

    @Test
    fun testDeleteProject() = runTest {
        repository.addProject("Проект 1", "Реж")
        val projectsBefore = repository.getAllProjects().first()
        assertTrue(projectsBefore.any { it.name == "Проект 1" }, "Project should be added")
        val id = projectsBefore.find { it.name == "Проект 1" }!!.id
        
        repository.deleteProject(id)
        
        val projectsAfter = repository.getAllProjects().first()
        assertTrue(projectsAfter.none { it.id == id }, "Project should be deleted")
    }
}
