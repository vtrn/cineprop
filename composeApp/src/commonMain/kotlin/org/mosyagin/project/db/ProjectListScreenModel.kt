package org.mosyagin.project.db

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.mosyagin.project.Project
import org.mosyagin.project.repository.ProjectRepository
import org.mosyagin.project.repository.SyncEvent
import org.mosyagin.project.repository.SyncManager
import org.mosyagin.project.repository.SyncRepository

@OptIn(FlowPreview::class)
class ProjectListScreenModel(
    private val repository: ProjectRepository,
    private val syncRepository: SyncRepository,
    private val syncManager: SyncManager
) : ScreenModel {

    private val syncEvents = MutableSharedFlow<SyncEvent>()

    init {
        syncEvents
            .debounce(1500L)
            .onEach { event ->
                syncRepository.enqueue(event.operation, event.tableName, event.recordId, null)
            }
            .launchIn(screenModelScope)

        syncManager.push()
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
        }
    }

    fun deleteProject(id: String) { // Изменено на String
        screenModelScope.launch {
            repository.deleteProject(id)
        }
    }

    fun updateProject(id: String, name: String, director: String) { // Изменено на String
        screenModelScope.launch {
            repository.updateProject(id, name, director)
            syncEvents.emit(SyncEvent("UPDATE", "Project", id))
        }
    }
}
