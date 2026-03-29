package org.mosyagin.project.ui.screens

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.*
import org.mosyagin.project.Actor
import org.mosyagin.project.GetScenesByActor
import org.mosyagin.project.repository.SceneRepository

class CharacterWorkspaceViewModel(
    private val projectId: Long,
    private val sceneRepository: SceneRepository
) : ScreenModel {

    // Список всех персонажей
    val characters: StateFlow<List<Actor>> = sceneRepository.getActorsByProject(projectId)
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedActorId = MutableStateFlow<Long?>(null)
    val selectedActorId: StateFlow<Long?> = _selectedActorId.asStateFlow()

    // Детали персонажа (сцены и локации)
    val selectedCharacterDetails = _selectedActorId.flatMapLatest { id ->
        if (id == null) flowOf(null)
        else {
            combine(
                // Передаем 0 как заглушку для scriptFileId, если нужно получить все сцены
                sceneRepository.getScenesByActor(id, 0L), 
                sceneRepository.getLocationsByActor(id)
            ) { scenes, locations ->
                CharacterDetails(scenes, locations)
            }
        }
    }.stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun onCharacterSelected(id: Long) {
        _selectedActorId.value = id
    }
}

data class CharacterDetails(
    val scenes: List<GetScenesByActor>,
    val locations: List<String>
)
