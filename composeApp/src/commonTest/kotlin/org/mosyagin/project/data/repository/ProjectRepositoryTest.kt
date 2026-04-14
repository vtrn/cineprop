package org.mosyagin.project.data.repository

import app.cash.sqldelight.db.SqlDriver
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.koin.core.context.stopKoin
import org.mosyagin.project.repository.ProjectRepository
import org.mosyagin.project.DatabaseQueries
import org.mosyagin.project.db.CinePropDatabase
import org.mosyagin.project.db.createTestDriver
import org.mosyagin.project.repository.FakeSyncRepository
import org.mosyagin.project.repository.ProjectRepositoryImpl
import org.mosyagin.project.repository.FakeAuthRepository
import org.mosyagin.project.crypto.CryptoManager
import org.mosyagin.project.crypto.KeyVault
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

    @BeforeTest
    fun setup() {
        driver = createTestDriver()
        driver.execute(null, "PRAGMA foreign_keys=ON;", 0)
        
        try {
            CinePropDatabase.Schema.create(driver)
        } catch (e: Exception) { }

        val database = CinePropDatabase(driver)
        queries = database.databaseQueries
        
        val authRepo = FakeAuthRepository()
        val cryptoManager = CryptoManager()
        
        // ВАЖНО: В тестах commonTest KeyVault() вызывается без параметров, 
        // так как он компилируется против JVM реализации (или заглушки).
        // Если компилятор Android жалуется на отсутствие context, нам нужно 
        // предоставить KeyVault через фабрику или использовать мок.
        val keyVault = createTestKeyVault() 
        
        val supabase = createSupabaseClient("https://mock.supabase.co", "key") {
            httpEngine = MockEngine { 
                respond(
                    content = "{}",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }
            install(Postgrest)
        }

        repository = ProjectRepositoryImpl(
            queries = queries, 
            syncRepository = FakeSyncRepository(), 
            authRepository = authRepo,
            cryptoManager = cryptoManager,
            keyVault = keyVault,
            supabase = supabase
        )

        val projectId = "test-project-id"
        queries.insertProject(projectId, "Test Project", "Director", 0L, 0L, "test@example.com")
        this.testProjectId = projectId
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
        assertEquals(2, projects.size)
        assertTrue(projects.any { it.name == "Тестовый проект" })
    }

    @Test
    fun testDeleteProject() = runTest {
        repository.addProject("Проект 1", "Реж")
        val projectsBefore = repository.getAllProjects().first()
        val id = projectsBefore.find { it.name == "Проект 1" }!!.id
        
        repository.deleteProject(id)
        val projectsAfter = repository.getAllProjects().first()
        assertTrue(projectsAfter.none { it.id == id })
    }
}

// Заглушка для тестов, так как expect/actual может требовать параметров на Android
expect fun createTestKeyVault(): KeyVault
