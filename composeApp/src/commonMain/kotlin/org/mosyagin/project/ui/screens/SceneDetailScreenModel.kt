package org.mosyagin.project.ui.screens

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.mosyagin.project.Actor
import org.mosyagin.project.models.versioning.Prop
import org.mosyagin.project.GetSceneById
import org.mosyagin.project.parser.ScriptBlock
import org.mosyagin.project.parser.ScriptParser
import org.mosyagin.project.parser.update.ScriptUpdateManager
import org.mosyagin.project.repository.SceneRepository

class SceneDetailScreenModel(
    private val repository: SceneRepository,
    private val scriptUpdateManager: ScriptUpdateManager,
    private val sceneUserDataId: Long,
    private val scriptFileId: Long
) : ScreenModel {

    val scene: StateFlow<GetSceneById?> = repository.getSceneById(sceneUserDataId, scriptFileId)
        .stateIn(screenModelScope, SharingStarted.Eagerly, null)

    val actors: StateFlow<List<Actor>> = repository.getActorsForScene(sceneUserDataId)
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val props: StateFlow<List<Prop>> = repository.getPropsForScene(sceneUserDataId)
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedPropId = MutableStateFlow<Long?>(null)
    val selectedPropId: StateFlow<Long?> = _selectedPropId.asStateFlow()

    val scriptBlocks: StateFlow<List<ScriptBlock>> = scene
        .filterNotNull()
        .map { ScriptParser().parseBlocks(it.content) }
        .stateIn(screenModelScope, SharingStarted.Lazily, emptyList())

    fun setSelectedProp(id: Long?) {
        _selectedPropId.value = id
    }

    fun addProp(name: String, anchor: String, startOffset: Long = 0, endOffset: Long = 0) {
        screenModelScope.launch {
            repository.addProp(
                sceneUserDataId = sceneUserDataId,
                name = name,
                anchor = anchor,
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

    fun updateScript(projectId: Long, seriesNumber: Int, filePath: String, fullText: String) {
        screenModelScope.launch {
            // В реальной жизни тут нужен вызов prepareUpdate и показ диалога, 
            // но для D&D в контексте ОДНОЙ сцены мы можем упростить или обновить файл
            // (Пока оставим как заглушку для вызова логики обновления из ТЗ)
        }
    }
}
