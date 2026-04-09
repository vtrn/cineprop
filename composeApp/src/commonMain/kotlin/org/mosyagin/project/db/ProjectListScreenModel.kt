package org.mosyagin.project.db

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.mosyagin.project.Project
import org.mosyagin.project.repository.AuthRepository
import org.mosyagin.project.repository.ProjectRepository
import org.mosyagin.project.repository.SyncEvent
import org.mosyagin.project.repository.SyncManager
import org.mosyagin.project.repository.SyncRepository
import org.mosyagin.project.repository.MemberRepository
import org.mosyagin.project.ui.components.ProjectSyncStatus
import org.mosyagin.project.util.NetworkObserver

@OptIn(FlowPreview::class)
class ProjectListScreenModel(
    private val repository: ProjectRepository,
    private val syncRepository: SyncRepository,
    private val syncManager: SyncManager,
    private val authRepository: AuthRepository,
    private val memberRepository: MemberRepository,
    private val networkObserver: NetworkObserver
) : ScreenModel {

    private val syncEvents = MutableSharedFlow<SyncEvent>()

    val currentUser = authRepository.currentUser
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), authRepository.getCurrentUserSync())

    val isOnline: StateFlow<Boolean> = networkObserver.isOnline

    val projects: StateFlow<List<Project>> = repository.getAllProjects()
        .stateIn(
            scope = screenModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val projectStatuses: StateFlow<Map<String, ProjectSyncStatus>> = combine(
        currentUser,
        projects,
        syncRepository.getPending()
    ) { user, currentProjects, pendingChanges ->
        val pendingProjectIds = pendingChanges.mapNotNull { it.project_id }.toSet()
        
        currentProjects.associate { project ->
            val status = when {
                project.isRemote == 0L -> ProjectSyncStatus.LOCAL
                user == null -> ProjectSyncStatus.REQUIRES_AUTH
                pendingProjectIds.contains(project.id) -> ProjectSyncStatus.DIRTY
                else -> ProjectSyncStatus.SYNCED
            }
            project.id to status
        }
    }.stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    init {
        syncEvents
            .debounce(1500L)
            .onEach { event ->
                syncRepository.enqueue(event.operation, event.tableName, event.recordId, event.recordId, null)
            }
            .launchIn(screenModelScope)

        syncManager.push()
    }

    fun addProject(name: String, director: String) {
        screenModelScope.launch {
            repository.addProject(name, director)
        }
    }

    fun connectProjectToCloud(projectId: String) {
        screenModelScope.launch {
            val user = authRepository.getCurrentUserSync()
            if (user?.email != null) {
                // Сначала добавляем владельца локально
                memberRepository.addOwnerLocally(projectId, user.email!!)
                
                // Затем помечаем проект как удаленный и ставим в очередь
                repository.markProjectAsRemote(projectId)
                syncRepository.enqueue("INSERT", "Project", projectId, projectId, null)
                
                syncManager.push()
            }
        }
    }

    fun deleteProject(id: String) {
        screenModelScope.launch {
            repository.deleteProject(id)
        }
    }

    fun updateProject(id: String, name: String, director: String) {
        screenModelScope.launch {
            repository.updateProject(id, name, director)
            val project = projects.value.find { it.id == id }
            if (project?.isRemote == 1L) {
                syncEvents.emit(SyncEvent("UPDATE", "Project", id))
            }
        }
    }

    fun signOut() {
        screenModelScope.launch {
            authRepository.signOut()
        }
    }
}
