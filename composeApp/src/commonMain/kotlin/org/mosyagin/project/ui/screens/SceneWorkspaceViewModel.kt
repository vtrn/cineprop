package org.mosyagin.project.ui.screens

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.mosyagin.project.Actor
import org.mosyagin.project.GetLatestScenesForProject
import org.mosyagin.project.SceneVersion
import org.mosyagin.project.models.versioning.Prop
import org.mosyagin.project.repository.PropWithScene
import org.mosyagin.project.repository.SceneRepository
import org.mosyagin.project.repository.SyncEvent
import org.mosyagin.project.repository.SyncRepository

// Модель данных инспектора
data class SceneInspectorData(
    val sceneId: String, 
    val props: List<Prop>,
    val actors: List<Actor>,
    val needsReview: Boolean,
    val seriesNumber: Long,
    val sceneNumber: String
)

enum class SceneFilter { ALL, MODIFIED, NEW }

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class SceneWorkspaceViewModel(
    private val projectId: String, 
    private val sceneRepository: SceneRepository,
    private val syncRepository: SyncRepository
) : ScreenModel {

    private val syncEvents = MutableSharedFlow<SyncEvent>()

    init {
        syncEvents
            .debounce(1500L)
            .onEach { event ->
                syncRepository.enqueue(event.operation, event.tableName, event.recordId, projectId, event.dataJson)
            }
            .launchIn(screenModelScope)
    }

    private val _selectedSceneId = MutableStateFlow<String?>(null) 
    val selectedSceneId: StateFlow<String?> = _selectedSceneId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(SceneFilter.ALL)
    val selectedFilter: StateFlow<SceneFilter> = _selectedFilter.asStateFlow()

    val scenesList: StateFlow<List<GetLatestScenesForProject>> = combine(
        sceneRepository.getLatestScenesForProject(projectId),
        _searchQuery,
        _selectedFilter
    ) { scenes, query, filter ->
        scenes.filter { scene ->
            val matchesSearch = scene.sceneNumber.contains(query, ignoreCase = true) ||
                    scene.location.contains(query, ignoreCase = true)
            val matchesFilter = when (filter) {
                SceneFilter.ALL -> true
                SceneFilter.MODIFIED -> scene.needsReview == 1L
                SceneFilter.NEW -> scene.versionCount == 1L
            }
            matchesSearch && matchesFilter
        }
    }.stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val projectActors: StateFlow<List<Actor>> = sceneRepository.getActorsByProject(projectId)
        .stateIn(screenModelScope, SharingStarted.Eagerly, emptyList())

    val allProjectProps: StateFlow<List<PropWithScene>> = sceneRepository.getPropsByProject(projectId)
        .stateIn(screenModelScope, SharingStarted.Eagerly, emptyList())

    val selectedSceneDetails: StateFlow<SceneVersion?> = _selectedSceneId.flatMapLatest { id ->
        if (id == null) flowOf(null)
        else {
            sceneRepository.getSceneVersionsForUserData(id).map { versions ->
                versions.firstOrNull()
            }
        }
    }.stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedSceneInspector: StateFlow<SceneInspectorData?> = _selectedSceneId.flatMapLatest { id ->
        if (id == null) flowOf(null)
        else {
            combine(
                sceneRepository.getPropsForScene(id),
                sceneRepository.getActorsForScene(id),
                sceneRepository.getSceneUserDataById(id)
            ) { props, actors, userData ->
                SceneInspectorData(
                    sceneId = id,
                    props = props, 
                    actors = actors,
                    needsReview = userData?.needsReview == 1L,
                    seriesNumber = userData?.seriesNumber ?: 0L,
                    sceneNumber = userData?.sceneNumber ?: ""
                )
            }
        }
    }.stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun onSceneSelected(id: String) { 
        _selectedSceneId.value = id
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onFilterSelected(filter: SceneFilter) {
        _selectedFilter.value = filter
    }

    fun addPropExtended(
        name: String,
        anchor: String,
        category: String,
        status: String,
        quantity: Int,
        actorId: String?, 
        note: String?,
        existingPropId: String? = null 
    ) {
        val sceneId = _selectedSceneId.value ?: return
        screenModelScope.launch {
            var targetGroupId: String? = null 
            if (existingPropId != null) {
                val existingProp = allProjectProps.value.find { it.id == existingPropId }
                targetGroupId = existingProp?.groupId ?: existingPropId
            }

            val id = sceneRepository.addPropFull(
                sceneUserDataId = sceneId,
                name = name,
                anchor = anchor,
                status = status,
                category = category,
                quantity = quantity,
                actorId = actorId,
                note = note,
                isCrossCutting = targetGroupId != null,
                groupId = targetGroupId
            )
            
            if (existingPropId != null) {
                val existingProp = allProjectProps.value.find { it.id == existingPropId }
                if (existingProp?.groupId == null) {
                    sceneRepository.updatePropGroupId(existingPropId, targetGroupId)
                    sceneRepository.updatePropCrossCutting(existingPropId, true)
                    syncEvents.emit(SyncEvent("UPDATE", "Prop", existingPropId))
                }
            }
        }
    }

    fun addProp(name: String, anchor: String) {
        val sceneId = _selectedSceneId.value ?: return
        screenModelScope.launch {
            sceneRepository.addProp(sceneId, name, anchor)
        }
    }

    fun deleteProp(propId: String) {
        screenModelScope.launch {
            sceneRepository.deleteProp(propId)
        }
    }

    fun updateSceneReviewStatus(needsReview: Boolean) {
        val sceneId = _selectedSceneId.value ?: return
        screenModelScope.launch {
            sceneRepository.updateSceneUserDataReviewStatus(if (needsReview) 1L else 0L, sceneId)
            syncEvents.emit(SyncEvent("UPDATE", "SceneUserData", sceneId))
        }
    }
}
