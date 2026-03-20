package org.mosyagin.project.ui.screens

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
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
        assertEquals(0, screenModel.projects.value.size)
    }

    @Test
    fun testAddProjectUpdatesState() = runTest {
        screenModel.addProject("Новый проект", "Режиссер")
        
        // В UnconfinedTestDispatcher изменения должны примениться сразу
        assertEquals(1, screenModel.projects.value.size)
        assertEquals("Новый проект", screenModel.projects.value[0].name)
    }

    @Test
    fun testDeleteProjectUpdatesState() = runTest {
        screenModel.addProject("Проект для удаления", "Реж")
        val id = screenModel.projects.value[0].id
        
        screenModel.deleteProject(id)
        
        assertEquals(0, screenModel.projects.value.size)
    }
}
