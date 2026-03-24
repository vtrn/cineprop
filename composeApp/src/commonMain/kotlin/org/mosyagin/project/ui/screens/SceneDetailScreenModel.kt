package org.mosyagin.project.ui.screens

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.mosyagin.project.Actor
import org.mosyagin.project.models.versioning.Prop
import org.mosyagin.project.GetSceneById
import org.mosyagin.project.repository.SceneRepository

class SceneDetailScreenModel(
    private val repository: SceneRepository,
    private val sceneUserDataId: Long,
    private val scriptFileId: Long
) : ScreenModel {

    val scene: StateFlow<GetSceneById?> = repository.getSceneById(sceneUserDataId, scriptFileId)
        .stateIn(screenModelScope, SharingStarted.Eagerly, null)

    val actors: StateFlow<List<Actor>> = repository.getActorsForScene(sceneUserDataId)
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val props: StateFlow<List<Prop>> = repository.getPropsForScene(sceneUserDataId)
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addProp(name: String, startOffset: Long = 0, endOffset: Long = 0) {
        screenModelScope.launch {
            repository.addProp(
                sceneUserDataId = sceneUserDataId,
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

    fun confirmAllProps() {
        screenModelScope.launch {
            repository.confirmAllProps(sceneUserDataId)
        }
    }
}
