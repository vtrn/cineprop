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
        // 1. Создаем проект, файл сценария и сцену
        queries.insertProject("Проект", "Реж")
        val projectId = queries.lastInsertRowId().executeAsOne()

        queries.insertScriptFile(
            projectId = projectId,
            seriesNumber = 1L,
            title = "Сценарий",
            filePath = "path/to/file",
            createdAt = 123456789L,
            previousVersionId = null,
            revisionColor = "White",
            uploadedBy = "User"
        )
        val scriptFileId = queries.lastInsertRowId().executeAsOne()
        
        queries.insertSceneUserData(projectId, "1", "1", "ЛОКАЦИЯ", 1L, "ДЕНЬ", null, 0L)
        val sceneUserDataId = queries.lastInsertRowId().executeAsOne()

        queries.insertSceneVersion(scriptFileId, sceneUserDataId, "Текст сцены", "hash", 0)

        // 2. Добавляем реквизит (привязывается к SceneUserData)
        repository.addProp(sceneUserDataId, "Меч", "Найти", 0, 10)

        // 3. Проверяем
        val props = repository.getPropsForScene(sceneUserDataId).first()
        assertEquals(1, props.size)
        assertEquals("Меч", props[0].name)
    }

    @Test
    fun testUpdatePropStatus() = runTest {
        queries.insertProject("Проект", "Реж")
        val projectId = queries.lastInsertRowId().executeAsOne()

        queries.insertSceneUserData(projectId, "1", "1", "ЛОКАЦИЯ", 1L, "ДЕНЬ", null, 0L)
        val sceneUserDataId = queries.lastInsertRowId().executeAsOne()

        repository.addProp(sceneUserDataId, "Ваза", "Найти")
        val props = repository.getPropsByProject(projectId).first()
        val propId = props[0].id

        repository.updatePropStatus(propId, "Готово")

        val updatedProp = repository.getPropsByProject(projectId).first()[0]
        assertEquals("Готово", updatedProp.status)
    }
}
