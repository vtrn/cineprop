package org.mosyagin.project.ui.screens

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.mosyagin.project.Actor
import org.mosyagin.project.GetScenesForShift
import org.mosyagin.project.Shift
import org.mosyagin.project.SceneVersion
import org.mosyagin.project.models.versioning.Prop
import org.mosyagin.project.repository.SceneRepository
import org.mosyagin.project.repository.ShiftRepository

@OptIn(ExperimentalCoroutinesApi::class)
class TrackerViewModel(
    private val projectId: Long,
    private val shiftRepository: ShiftRepository,
    private val sceneRepository: SceneRepository
) : ScreenModel {

    // Стейт для списка смен
    val shifts: StateFlow<List<Shift>> = shiftRepository.getShiftsByProject(projectId)
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Стейт для раскрытых смен (набор ID)
    private val _expandedShiftIds = MutableStateFlow<Set<Long>>(emptySet())
    val expandedShiftIds: StateFlow<Set<Long>> = _expandedShiftIds.asStateFlow()

    // Стейт для выбранной сцены
    private val _selectedSceneId = MutableStateFlow<Long?>(null)
    val selectedSceneId: StateFlow<Long?> = _selectedSceneId.asStateFlow()

    // Хранилище для сцен по сменам (кэшируем флоу)
    private val _scenesByShift = mutableMapOf<Long, StateFlow<List<GetScenesForShift>>>()

    fun getScenesForShift(shiftId: Long): StateFlow<List<GetScenesForShift>> {
        return _scenesByShift.getOrPut(shiftId) {
            shiftRepository.getScenesForShift(shiftId)
                .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        }
    }

    // Детали выбранной сцены (текст)
    val selectedSceneDetails: StateFlow<SceneVersion?> = _selectedSceneId.flatMapLatest { id ->
        if (id == null) flowOf(null)
        else {
            sceneRepository.getSceneVersionsForUserData(id).map { it.firstOrNull() }
        }
    }.stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Инспектор выбранной сцены
    val selectedSceneInspector = _selectedSceneId.flatMapLatest { id ->
        if (id == null) flowOf(null)
        else {
            combine(
                sceneRepository.getPropsForScene(id),
                sceneRepository.getActorsForScene(id)
            ) { props, actors ->
                SceneInspectorData(props, actors)
            }
        }
    }.stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun toggleShift(shiftId: Long) {
        val current = _expandedShiftIds.value
        _expandedShiftIds.value = if (current.contains(shiftId)) current - shiftId else current + shiftId
    }

    fun onSceneSelected(sceneUserDataId: Long) {
        _selectedSceneId.value = sceneUserDataId
    }

    fun addProp(name: String) {
        val sceneId = _selectedSceneId.value ?: return
        screenModelScope.launch {
            sceneRepository.addProp(sceneUserDataId = sceneId, name = name)
        }
    }

    fun deleteProp(propId: Long) {
        screenModelScope.launch {
            sceneRepository.deleteProp(propId)
        }
    }
}
