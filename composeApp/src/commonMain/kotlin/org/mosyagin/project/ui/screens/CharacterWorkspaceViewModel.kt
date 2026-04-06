@file:OptIn(ExperimentalCoroutinesApi::class)

package org.mosyagin.project.ui.screens

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import org.mosyagin.project.Actor
import org.mosyagin.project.GetScenesByActor
import org.mosyagin.project.repository.SceneRepository

class CharacterWorkspaceViewModel(
    private val projectId: String,
    private val sceneRepository: SceneRepository
) : ScreenModel {

    // Список всех персонажей
    val characters: StateFlow<List<Actor>> = sceneRepository.getActorsByProject(projectId)
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedActorId = MutableStateFlow<String?>(null)
    val selectedActorId: StateFlow<String?> = _selectedActorId.asStateFlow()

    // Детали персонажа (сцены и локации)
    val selectedCharacterDetails = _selectedActorId.flatMapLatest { id ->
        if (id == null) flowOf(null)
        else {
            combine(
                // Передаем пустую строку как заглушку для scriptFileId, если нужно получить все сцены
                // Или нужно изменить репозиторий, чтобы он принимал String? и обрабатывал это
                sceneRepository.getScenesByActor(id, ""), 
                sceneRepository.getLocationsByActor(id)
            ) { scenes, locations ->
                CharacterDetails(scenes, locations)
            }
        }
    }.stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun onCharacterSelected(id: String) {
        _selectedActorId.value = id
    }
}

data class CharacterDetails(
    val scenes: List<GetScenesByActor>,
    val locations: List<String>
)
