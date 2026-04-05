package org.mosyagin.project.db

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.mosyagin.project.Project
import org.mosyagin.project.repository.ProjectRepository
import org.mosyagin.project.repository.SyncRepository

/**
 * Событие для очереди синхронизации (копия из PropWorkspaceViewModel для консистентности)
 */
data class SyncEvent(
    val operation: String,
    val tableName: String,
    val recordId: Long
)

@OptIn(FlowPreview::class)
class ProjectListScreenModel(
    private val repository: ProjectRepository,
    private val syncRepository: SyncRepository
) : ScreenModel {

    // Поток событий для синхронизации с debounce (Issue #3)
    private val syncEvents = MutableSharedFlow<SyncEvent>()

    init {
        syncEvents
            .debounce(1500L)
            .onEach { event ->
                syncRepository.enqueue(event.operation, event.tableName, event.recordId, null)
            }
            .launchIn(screenModelScope)
    }

    val projects: StateFlow<List<Project>> = repository.getAllProjects()
        .stateIn(
            scope = screenModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addProject(name: String, director: String) {
        screenModelScope.launch {
            repository.addProject(name, director)
            // При добавлении нового проекта мы сразу пишем в очередь (через репозиторий), 
            // так как это не "печатаемое" поле, которое требует дебаунса. 
            // Репозиторий уже вызывает enqueue для INSERT.
        }
    }

    fun deleteProject(id: Long) {
        screenModelScope.launch {
            repository.deleteProject(id)
            // Репозиторий вызывает enqueue для DELETE.
        }
    }

    // Если появится метод updateProjectName (который пользователь может быстро менять):
    fun updateProject(id: Long, name: String, director: String) {
        screenModelScope.launch {
            repository.updateProject(id, name, director)
            syncEvents.emit(SyncEvent("UPDATE", "Project", id))
        }
    }
}
