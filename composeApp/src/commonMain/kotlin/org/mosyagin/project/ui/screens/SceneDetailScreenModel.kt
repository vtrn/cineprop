package org.mosyagin.project.ui.screens

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.mosyagin.project.Actor
import org.mosyagin.project.Prop
import org.mosyagin.project.Scene
import org.mosyagin.project.repository.SceneRepository

class SceneDetailScreenModel(
    private val repository: SceneRepository,
    private val sceneId: Long
) : ScreenModel {

    val scene: StateFlow<Scene?> = repository.getSceneById(sceneId)
        .stateIn(screenModelScope, SharingStarted.Eagerly, null)

    val actors: StateFlow<List<Actor>> = repository.getActorsForScene(sceneId)
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val props: StateFlow<List<Prop>> = repository.getPropsForScene(sceneId)
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addProp(name: String, startOffset: Long = 0, endOffset: Long = 0) {
        screenModelScope.launch {
            // Используем именованные аргументы, чтобы пропустить 'status'
            repository.addProp(
                sceneId = sceneId,
                name = name,
                startOffset = startOffset,
                endOffset = endOffset
            )
        }
    }

    fun deleteProp(propId: Long) {
        screenModelScope.launch {
            repository.deleteProp(propId)
        }
    }
}
