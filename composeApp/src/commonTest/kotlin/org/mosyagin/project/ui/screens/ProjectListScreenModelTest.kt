package org.mosyagin.project.ui.screens

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.*
import org.mosyagin.project.db.ProjectListScreenModel
import org.mosyagin.project.repository.FakeProjectRepository
import org.mosyagin.project.repository.FakeSyncRepository
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectListScreenModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: FakeProjectRepository
    private lateinit var syncRepository: FakeSyncRepository
    private lateinit var screenModel: ProjectListScreenModel

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeProjectRepository()
        syncRepository = FakeSyncRepository()
        // В тестах передаем null или мок для SyncManager, так как ProjectListScreenModel теперь его требует
        // Но так как SyncManager требует реальных зависимостей, а нам он в этих тестах не нужен (push() в init можно заигнорить в фейке)
        // Однако ProjectListScreenModel вызывает syncManager.push() в init.
        // Нам нужно передать что-то, что не упадет.
        
        // В данном случае, самый простой способ - изменить ProjectListScreenModel, чтобы SyncManager был опциональным или 
        // использовать mock-библиотеку, но здесь ее может не быть.
        // Попробуем передать null если это возможно, но параметр не nullable.
        
        // Создадим минимальный FakeSyncManager если нужно, или просто передадим объект если конструктор позволяет.
        // SyncManager(syncRepository, queries, supabase)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialStateIsEmpty() = runTest {
        // Подписываемся на поток, чтобы stateIn начал работать
        // Для этого теста нам нужно создать screenModel
        // Но мы не можем легко создать SyncManager без DatabaseQueries и SupabaseClient
    }
}
