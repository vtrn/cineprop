package org.mosyagin.project.parser

/**
 * Типы смысловых блоков сценария.
 */
enum class BlockType {
    SLUGLINE,      // Заголовок сцены (ИНТ. КУХНЯ - ДЕНЬ)
    ACTION,        // Описание действия
    CHARACTER,     // Имя персонажа перед диалогом
    DIALOGUE,      // Реплика персонажа
    PARENTHETICAL, // Ремарка в скобках
    TRANSITION     // Переход (СКЛЕЙКА, ЗТМ)
}

/**
 * Один смысловой блок текста сценария.
 */
data class ScriptBlock(
    val type: BlockType,
    val text: String
)
