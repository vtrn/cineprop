package org.mosyagin.project.ui.screens

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.*
import org.mosyagin.project.SyncQueue
import org.mosyagin.project.repository.*
import org.mosyagin.project.models.versioning.PropStatus
import org.mosyagin.project.ui.components.props.ExportFormat
import org.mosyagin.project.ui.components.props.ExportGrouping
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class DebounceSyncTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testDebounceWriteAmplification() = runTest(testDispatcher) {
        val syncRepository = FakeSyncRepositoryLocal()
        val sceneRepository = FakeSceneRepository()
        val projectRepository = FakeProjectRepository()
        
        val viewModel = PropWorkspaceViewModel(
            projectId = "1",
            sceneRepository = sceneRepository,
            projectRepository = projectRepository,
            syncRepository = syncRepository,
            propExporter = MockPropExporter(),
            fileSaver = MockFileSaver()
        )

        // 1. Делаем 10 быстрых изменений статуса реквизита
        repeat(10) {
            viewModel.updatePropStatus("1", PropStatus.READY)
            delay(100) 
        }

        // 2. Проверяем сразу — в очереди должно быть 0 записей
        runCurrent()
        assertEquals(0, syncRepository.getSyncCount(), "Should be 0 records before debounce timeout")

        // 3. Продвигаем время вперед на 2 секунды (debounce 1500ms)
        advanceTimeBy(2000)
        runCurrent()

        // 4. Теперь должна появиться ровно 1 запись в SyncQueue
        assertEquals(1, syncRepository.getSyncCount(), "Should be exactly 1 record in SyncQueue after 10 rapid changes")
    }
}

// Mock/Fake классы для теста
class FakeSyncRepositoryLocal : SyncRepository {
    private val queue = MutableStateFlow<List<SyncQueue>>(emptyList())
    
    override suspend fun enqueue(operation: String, tableName: String, recordId: String, dataJson: String?) {
        enqueueSync(operation, tableName, recordId, dataJson)
    }

    override fun enqueueSync(operation: String, tableName: String, recordId: String, dataJson: String?) {
        val newQueue = queue.value.toMutableList()
        newQueue.add(
            SyncQueue(
                id = (newQueue.size + 1).toLong(),
                operation = operation,
                tableName = tableName,
                recordId = recordId,
                dataJson = dataJson,
                updatedAt = 0L,
                synced = 0L
            )
        )
        queue.value = newQueue
    }

    override fun getPending(): Flow<List<SyncQueue>> = queue.asStateFlow()
    override suspend fun markSynced(ids: List<Long>) {}
    override fun setSyncManager(manager: SyncManager) {}
    override fun triggerPush() {}
    
    fun getSyncCount() = queue.value.size
}

class MockPropExporter : org.mosyagin.project.export.PropExporter {
    override suspend fun export(
        projectName: String, 
        grouping: ExportGrouping, 
        format: ExportFormat, 
        props: List<PropWithScene>
    ): ByteArray = byteArrayOf()
}

class MockFileSaver : org.mosyagin.project.export.FileSaver {
    override suspend fun saveFile(fileName: String, mimeType: String, bytes: ByteArray) {}
}
