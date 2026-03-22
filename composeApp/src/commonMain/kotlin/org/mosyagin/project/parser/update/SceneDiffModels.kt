package org.mosyagin.project.parser.update

import org.mosyagin.project.parser.ScriptBlock

/**
 * Тип изменения блока в сценарии.
 */
enum class DiffType {
    ADDED, DELETED, UNCHANGED
}

/**
 * Блок сценария с информацией о его изменении.
 */
data class DiffBlock(
    val type: DiffType,
    val block: ScriptBlock
)

/**
 * Итоговый отчет о разнице между двумя версиями сцены.
 */
data class SceneDiffReport(
    val diffs: List<DiffBlock>,
    val addedCount: Int,
    val deletedCount: Int
)
