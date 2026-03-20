package org.mosyagin.project.data.repository

import app.cash.sqldelight.db.SqlDriver
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.mosyagin.project.repository.ProjectRepository
import org.mosyagin.project.repository.ProjectRepositoryImpl
import org.mosyagin.project.DatabaseQueries
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Мы используем expect/actual для драйвера, чтобы тесты работали на всех платформах
expect fun createTestDriver(): SqlDriver

class ProjectRepositoryTest {
    private lateinit var repository: ProjectRepository
    private lateinit var queries: DatabaseQueries

    @BeforeTest
    fun setup() {
        val driver = createTestDriver()
        // Инициализируем схему базы данных (в реальном приложении это делает SqlDelight)
        // Но в тестах нам нужно создать таблицы вручную или через сгенерированный Schema
        org.mosyagin.project.db.CinePropDatabase.Schema.create(driver)
        val database = org.mosyagin.project.db.CinePropDatabase(driver)
        queries = database.databaseQueries
        repository = ProjectRepositoryImpl(queries)
    }

    @Test
    fun testAddAndGetAllProjects() = runTest {
        repository.addProject("Тестовый проект", "Режиссер")
        
        val projects = repository.getAllProjects().first()
        
        assertEquals(1, projects.size)
        assertEquals("Тестовый проект", projects[0].name)
    }

    @Test
    fun testDeleteProject() = runTest {
        repository.addProject("Проект 1", "Реж")
        val id = repository.getAllProjects().first()[0].id
        
        repository.deleteProject(id)
        
        val projects = repository.getAllProjects().first()
        assertTrue(projects.isEmpty())
    }
}
