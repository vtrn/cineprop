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

    val shifts: StateFlow<List<Shift>> = shiftRepository.getShiftsByProject(projectId)
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _expandedShiftIds = MutableStateFlow<Set<Long>>(emptySet())
    val expandedShiftIds: StateFlow<Set<Long>> = _expandedShiftIds.asStateFlow()

    private val _selectedSceneId = MutableStateFlow<Long?>(null)
    val selectedSceneId: StateFlow<Long?> = _selectedSceneId.asStateFlow()

    private val _scenesByShift = mutableMapOf<Long, StateFlow<List<GetScenesForShift>>>()

    fun getScenesForShift(shiftId: Long): StateFlow<List<GetScenesForShift>> {
        return _scenesByShift.getOrPut(shiftId) {
            shiftRepository.getScenesForShift(shiftId)
                .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        }
    }

    val selectedSceneDetails: StateFlow<SceneVersion?> = _selectedSceneId.flatMapLatest { id ->
        if (id == null) flowOf(null)
        else {
            sceneRepository.getSceneVersionsForUserData(id).map { it.firstOrNull() }
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

    fun toggleShift(shiftId: Long) {
        val current = _expandedShiftIds.value
        _expandedShiftIds.value = if (current.contains(shiftId)) current - shiftId else current + shiftId
    }

    fun onSceneSelected(sceneUserDataId: Long) {
        _selectedSceneId.value = sceneUserDataId
    }

    fun addProp(name: String, anchor: String) {
        val sceneId = _selectedSceneId.value ?: return
        screenModelScope.launch {
            sceneRepository.addProp(sceneUserDataId = sceneId, name = name, anchor = anchor)
        }
    }

    fun deleteProp(propId: Long) {
        screenModelScope.launch {
            sceneRepository.deleteProp(propId)
        }
    }
}
