package org.mosyagin.project.data.repository

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.mosyagin.project.repository.SceneRepository
import org.mosyagin.project.repository.SceneRepositoryImpl
import org.mosyagin.project.DatabaseQueries
import org.mosyagin.project.db.CinePropDatabase
import org.mosyagin.project.db.createTestDriver
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SceneRepositoryTest {
    private lateinit var repository: SceneRepository
    private lateinit var queries: DatabaseQueries

    @BeforeTest
    fun setup() {
        val driver = createTestDriver()
        CinePropDatabase.Schema.create(driver)
        val database = CinePropDatabase(driver)
        queries = database.databaseQueries
        repository = SceneRepositoryImpl(queries)
    }

    @Test
    fun testAddAndGetProp() = runTest {
        // 1. Создаем проект и сцену (нужны для внешних ключей)
        queries.insertProject("Проект", "Реж")
        val projectId = queries.getAllProjects().executeAsList()[0].id
        
        queries.insertScene(projectId, null, "1", "1", "ЛОКАЦИЯ", 1, "ДЕНЬ", "Текст")
        val sceneId = queries.getScenesByProject(projectId).executeAsList()[0].id

        // 2. Добавляем реквизит
        repository.addProp(sceneId, "Меч", "Найти", 0, 10)

        // 3. Проверяем
        val props = repository.getPropsForScene(sceneId).first()
        assertEquals(1, props.size)
        assertEquals("Меч", props[0].name)
    }

    @Test
    fun testUpdatePropStatus() = runTest {
        queries.insertProject("Проект", "Реж")
        val projectId = queries.lastInsertRowId().executeAsOne()
        queries.insertScene(projectId, null, "1", "1", "ЛОКАЦИЯ", 1, "ДЕНЬ", "Текст")
        val sceneId = queries.lastInsertRowId().executeAsOne()

        repository.addProp(sceneId, "Ваза", "Найти")
        val propId = repository.getPropsByProject(projectId).first()[0].id

        repository.updatePropStatus(propId, "Готово")

        val updatedProp = repository.getPropsByProject(projectId).first()[0]
        assertEquals("Готово", updatedProp.status)
    }
}
