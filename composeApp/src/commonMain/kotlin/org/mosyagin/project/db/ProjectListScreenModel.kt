package org.mosyagin.project.db

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.mosyagin.project.Project
import org.mosyagin.project.ActivityLog
import org.mosyagin.project.GetAllActivities
import org.mosyagin.project.repository.*
import org.mosyagin.project.ui.components.ProjectSyncStatus
import org.mosyagin.project.util.NetworkObserver
import org.mosyagin.project.util.currentTimestamp

data class ProjectStats(
    val changeCount: Int = 0,
    val memberCount: Int = 0,
    val status: String = "актив."
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class ProjectListScreenModel(
    private val repository: ProjectRepository,
    private val syncRepository: SyncRepository,
    private val syncManager: SyncManager,
    private val authRepository: AuthRepository,
    private val memberRepository: MemberRepository,
    private val activityRepository: ActivityRepository,
    private val sceneRepository: SceneRepository,
    private val networkObserver: NetworkObserver
) : ScreenModel {

    private val syncEvents = MutableSharedFlow<SyncEvent>()

    val currentUser = authRepository.currentUser
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), authRepository.getCurrentUserSync())

    val isOnline: StateFlow<Boolean> = networkObserver.isOnline

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    // Базовый список проектов
    private val allProjects = repository.getAllProjects()

    val recentProjects: StateFlow<List<Project>> = allProjects
        .combine(_searchQuery) { list, query ->
            val twelveHoursAgo = currentTimestamp() - (12 * 60 * 60 * 1000)
            list.filter { it.updatedAt > twelveHoursAgo && it.name.contains(query, ignoreCase = true) }
        }
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cloudProjects: StateFlow<List<Project>> = allProjects
        .combine(_searchQuery) { list, query ->
            list.filter { it.isRemote == 1L && it.name.contains(query, ignoreCase = true) }
        }
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val localProjects: StateFlow<List<Project>> = allProjects
        .combine(_searchQuery) { list, query ->
            list.filter { it.isRemote == 0L && it.name.contains(query, ignoreCase = true) }
        }
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedProjectId = MutableStateFlow<String?>(null)
    val selectedProjectId = _selectedProjectId.asStateFlow()

    // Глобальная статистика по всем проектам
    val globalStats = activityRepository.getAllRecentActivities().map { logs ->
        ProjectStats(
            changeCount = logs.size,
            memberCount = -1, // Не считаем для глобала
            status = "обзор"
        )
    }.stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), ProjectStats())

    // Детальная статистика по выбранному проекту
    val selectedProjectStats = _selectedProjectId.flatMapLatest { id ->
        if (id == null) globalStats
        else combine(
            activityRepository.getActivities(id),
            memberRepository.getMembersByProject(id)
        ) { logs, members ->
            ProjectStats(
                changeCount = logs.size,
                memberCount = members.size,
                status = if (logs.isNotEmpty()) "актив." else "черновик"
            )
        }
    }.stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), ProjectStats())

    // Последние действия (для превью - либо по проекту, либо глобально)
    val recentActivities = _selectedProjectId.flatMapLatest { id ->
        if (id == null) activityRepository.getAllRecentActivities().map { it.take(10) }
        else activityRepository.getActivities(id).map { it.take(10) }
    }.stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val projectStatuses: StateFlow<Map<String, ProjectSyncStatus>> = combine(
        currentUser,
        allProjects,
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
        
        // ВАЖНО: Убираем авто-выбор первого проекта, чтобы по умолчанию видеть глобальный обзор
        _selectedProjectId.value = null
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun selectProject(id: String?) {
        _selectedProjectId.value = id
        if (id != null) {
            screenModelScope.launch {
                activityRepository.decryptActivities(id)
            }
        }
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
                memberRepository.addOwnerLocally(projectId, user.email!!)
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
            val all = allProjects.first()
            val project = all.find { it.id == id }
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
