package org.mosyagin.project.data.repository

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.mosyagin.project.repository.ProjectRepository
import org.mosyagin.project.repository.ProjectRepositoryImpl
import org.mosyagin.project.DatabaseQueries
import org.mosyagin.project.db.CinePropDatabase
import org.mosyagin.project.db.createTestDriver
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class ProjectRepositoryTest {
    private lateinit var repository: ProjectRepository
    private lateinit var queries: DatabaseQueries

    @BeforeTest
    fun setup() {
        val driver = createTestDriver()
        val database = CinePropDatabase(driver)
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
