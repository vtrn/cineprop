package org.mosyagin.project.ui.screens

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.mosyagin.project.Actor
import org.mosyagin.project.models.versioning.Prop
import org.mosyagin.project.models.versioning.PropStatus
import org.mosyagin.project.repository.SceneRepository

class SceneDetailScreenModel(
    private val repository: SceneRepository,
    private val sceneUserDataId: String, // Изменено на String
    private val scriptFileId: String // Изменено на String
) : ScreenModel {

    val sceneData = repository.getSceneById(sceneUserDataId, scriptFileId)
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), null)

    val props = repository.getPropsForScene(sceneUserDataId)
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val actors = repository.getActorsForScene(sceneUserDataId)
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updatePropStatus(propId: String, status: PropStatus) { // Изменено на String
        screenModelScope.launch {
            repository.updatePropStatus(propId, status.toDbString())
        }
    }

    private fun PropStatus.toDbString(): String = when(this) {
        PropStatus.PLANNED -> "Найти"
        PropStatus.BOUGHT -> "Куплено"
        PropStatus.READY -> "Готово"
        PropStatus.LOST -> "Утеряно"
    }

    fun updateNeedsReview(needsReview: Boolean) {
        screenModelScope.launch {
            repository.updateSceneUserDataReviewStatus(if (needsReview) 1L else 0L, sceneUserDataId)
        }
    }

    fun deleteProp(propId: String) { // Изменено на String
        screenModelScope.launch {
            repository.deleteProp(propId)
        }
    }
}
