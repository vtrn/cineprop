package org.mosyagin.project.ui.screens

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.*
import org.mosyagin.project.db.ProjectListScreenModel
import org.mosyagin.project.repository.FakeProjectRepository
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectListScreenModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: FakeProjectRepository
    private lateinit var screenModel: ProjectListScreenModel

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeProjectRepository()
        screenModel = ProjectListScreenModel(repository)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialStateIsEmpty() = runTest {
        // Подписываемся на поток, чтобы stateIn начал работать
        val job = screenModel.projects.onEach { }.launchIn(backgroundScope)
        
        assertEquals(0, screenModel.projects.value.size)
        job.cancel()
    }

    @Test
    fun testAddProjectUpdatesState() = runTest {
        val job = screenModel.projects.onEach { }.launchIn(backgroundScope)
        
        screenModel.addProject("Новый проект", "Режиссер")
        runCurrent() // Прогоняем текущие задачи в диспатчере
        
        assertEquals(1, screenModel.projects.value.size)
        assertEquals("Новый проект", screenModel.projects.value[0].name)
        job.cancel()
    }

    @Test
    fun testDeleteProjectUpdatesState() = runTest {
        val job = screenModel.projects.onEach { }.launchIn(backgroundScope)
        
        screenModel.addProject("Проект для удаления", "Реж")
        runCurrent()
        
        val id = screenModel.projects.value[0].id
        screenModel.deleteProject(id)
        runCurrent()
        
        assertEquals(0, screenModel.projects.value.size)
        job.cancel()
    }
}
