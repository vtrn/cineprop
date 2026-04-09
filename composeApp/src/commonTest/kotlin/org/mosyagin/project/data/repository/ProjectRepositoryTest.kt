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
import org.mosyagin.project.repository.ProjectRepositoryImpl
import org.mosyagin.project.repository.AuthRepository
import org.mosyagin.project.repository.FakeAuthRepository
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProjectRepositoryTest {
    private lateinit var repository: ProjectRepository
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
        val authRepo = FakeAuthRepository()
        repository = ProjectRepositoryImpl(queries, FakeSyncRepository(), authRepo)

        // СОЗДАЕМ ОБЯЗАТЕЛЬНУЮ ИЕРАРХИЮ
        val projectId = "test-project-id"
        // Добавлен 6-й параметр created_by
        queries.insertProject(projectId, "Test Project", "Director", 0L, 0L, "test@example.com")

        val scriptId = "test-script-id"
        queries.insertScriptFile(
            id = scriptId,
            project_id = projectId,
            seriesNumber = 1L,
            title = "Version 1",
            filePath = "/mock/path",
            createdAt = 123456789L,
            previousVersionId = null,
            revisionColor = "White",
            uploadedBy = "Tester",
            updatedAt = 0L
        )

        val userDataId = "test-user-data-id"
        queries.insertSceneUserData(
            id = userDataId,
            project_id = projectId,
            seriesNumber = 1L,
            sceneNumber = "1",
            location = "Lobby",
            isInterior = 1L,
            timeOfDay = "Day",
            notes = null,
            needsReview = 0L,
            updatedAt = 0L
        )

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
