package org.mosyagin.project.ui.screens

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.mosyagin.project.parser.ScriptBlock
import org.mosyagin.project.parser.ScriptParser
import org.mosyagin.project.parser.update.*
import org.mosyagin.project.repository.SceneRepository

/**
 * Описывает одну строку в Side-by-Side Diff.
 */
data class SideBySideDiffRow(
    val oldBlock: ScriptBlock? = null,
    val newBlock: ScriptBlock? = null,
    val type: DiffType
)

class SceneDiffViewModel(
    private val sceneUserDataId: Long,
    private val repository: SceneRepository,
    private val parser: ScriptParser
) : ScreenModel {

    private val _state = MutableStateFlow<DiffState>(DiffState.Loading)
    val state: StateFlow<DiffState> = _state.asStateFlow()

    init {
        loadDiff()
    }

    private fun loadDiff() {
        repository.getSceneVersionsForUserData(sceneUserDataId)
            .take(1)
            .onEach { versions ->
                if (versions.isEmpty()) {
                    _state.value = DiffState.Error("Текст не найден")
                    return@onEach
                }

                val currentVersion = versions.first()
                val previousVersion = versions.getOrNull(1)

                val currentBlocks = parser.parseBlocks(currentVersion.content)
                val oldBlocks = previousVersion?.let { parser.parseBlocks(it.content) } ?: emptyList()

                val report = MyersDiffEngine.compare(oldBlocks, currentBlocks)
                val sbsRows = transformToSideBySide(report.diffs)

                val sceneTitle = repository.getSceneUserDataById(sceneUserDataId).firstOrNull()?.let { 
                    "${it.seriesNumber}-${it.sceneNumber}"
                } ?: ""

                _state.value = DiffState.Success(
                    rows = sbsRows,
                    sceneNumber = sceneTitle,
                    addedCount = report.addedCount,
                    deletedCount = report.deletedCount
                )
            }
            .launchIn(screenModelScope)
    }

    /**
     * Превращает плоский список DiffBlock в пары для колонок.
     * Применяет агрессивное сопоставление (Smart Alignment), чтобы склеивать 
     * группы изменений в одну строку Side-by-Side.
     */
    private fun transformToSideBySide(diffs: List<DiffBlock>): List<SideBySideDiffRow> {
        val rows = mutableListOf<SideBySideDiffRow>()
        var i = 0
        while (i < diffs.size) {
            val current = diffs[i]
            
            if (current.type == DiffType.UNCHANGED) {
                rows.add(SideBySideDiffRow(current.block, current.block, DiffType.UNCHANGED))
                i++
                continue
            }

            // 1. Собираем всю группу идущих подряд удалений
            val deletedGroup = mutableListOf<ScriptBlock>()
            var j = i
            while (j < diffs.size && diffs[j].type == DiffType.DELETED) {
                deletedGroup.add(diffs[j].block)
                j++
            }

            // 2. Собираем всю группу идущих подряд добавлений, которые идут СРАЗУ за удалениями
            val addedGroup = mutableListOf<ScriptBlock>()
            var k = j
            while (k < diffs.size && diffs[k].type == DiffType.ADDED) {
                addedGroup.add(diffs[k].block)
                k++
            }

            if (deletedGroup.isNotEmpty() && addedGroup.isNotEmpty()) {
                // АГРЕССИВНОЕ СОПОСТАВЛЕНИЕ:
                // Ставим группу "Было" напротив группы "Стало", даже если они не равны.
                // Это заставит UI отобразить их в одной строке Side-by-Side и запустить WordDiff.
                rows.add(SideBySideDiffRow(
                    oldBlock = ScriptBlock(deletedGroup[0].type, deletedGroup.joinToString("\n") { it.text }),
                    newBlock = ScriptBlock(addedGroup[0].type, addedGroup.joinToString("\n") { it.text }),
                    type = DiffType.UNCHANGED // UI сам подсветит разницу, так как тексты будут отличаться
                ))
                i = k
            } else {
                // Если есть только одна группа (либо только удалили, либо только добавили)
                if (deletedGroup.isNotEmpty()) {
                    deletedGroup.forEach { rows.add(SideBySideDiffRow(oldBlock = it, type = DiffType.DELETED)) }
                    i = j
                } else if (addedGroup.isNotEmpty()) {
                    addedGroup.forEach { rows.add(SideBySideDiffRow(newBlock = it, type = DiffType.ADDED)) }
                    i = k
                }
            }
        }
        return rows
    }

    fun markAsReviewed() {
        screenModelScope.launch {
            repository.updateSceneUserDataReviewStatus(0L, sceneUserDataId)
        }
    }

    sealed class DiffState {
        object Loading : DiffState()
        data class Success(
            val rows: List<SideBySideDiffRow>, 
            val sceneNumber: String,
            val addedCount: Int,
            val deletedCount: Int
        ) : DiffState()
        data class Error(val message: String) : DiffState()
    }
}
