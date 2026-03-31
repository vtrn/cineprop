package org.mosyagin.project.ui.screens

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.mosyagin.project.Actor
import org.mosyagin.project.GetLatestScenesForProject
import org.mosyagin.project.SceneVersion
import org.mosyagin.project.models.versioning.Prop
import org.mosyagin.project.repository.SceneRepository

// Переносим enum сюда для общего использования
enum class SceneFilter { ALL, MODIFIED, NEW }

@OptIn(ExperimentalCoroutinesApi::class)
class SceneWorkspaceViewModel(
    private val projectId: Long,
    private val sceneRepository: SceneRepository
) : ScreenModel {

    private val _selectedSceneId = MutableStateFlow<Long?>(null)
    val selectedSceneId: StateFlow<Long?> = _selectedSceneId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(SceneFilter.ALL)
    val selectedFilter: StateFlow<SceneFilter> = _selectedFilter.asStateFlow()

    // Список всех сцен с учетом фильтрации и поиска
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

    // Детали выбранной сцены (текст из последней версии)
    val selectedSceneDetails: StateFlow<SceneVersion?> = _selectedSceneId.flatMapLatest { id ->
        if (id == null) flowOf(null)
        else {
            sceneRepository.getSceneVersionsForUserData(id).map { versions ->
                versions.firstOrNull()
            }
        }
    }.stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Инспектор выбранной сцены (Реквизит и Актеры)
    val selectedSceneInspector = _selectedSceneId.flatMapLatest { id ->
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
                    needsReview = userData?.needsReview == 1L
                )
            }
        }
    }.stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun onSceneSelected(id: Long) {
        _selectedSceneId.value = id
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onFilterSelected(filter: SceneFilter) {
        _selectedFilter.value = filter
    }

    // Добавление реквизита (аналогично мобильной версии)
    fun addProp(name: String, anchor: String) {
        val sceneId = _selectedSceneId.value ?: return
        screenModelScope.launch {
            sceneRepository.addProp(
                sceneUserDataId = sceneId,
                name = name,
                anchor = anchor
            )
        }
    }

    fun deleteProp(propId: Long) {
        screenModelScope.launch {
            sceneRepository.deleteProp(propId)
        }
    }
}

data class SceneInspectorData(
    val sceneId: Long,
    val props: List<Prop>,
    val actors: List<Actor>,
    val needsReview: Boolean
)
