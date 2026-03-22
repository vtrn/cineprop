package org.mosyagin.project.parser

/**
 * Интерфейс для парсинга содержимого сценария на смысловые блоки.
 */
interface ScriptContentParser {
    fun parse(content: String): List<ScriptBlock>
}

/**
 * Базовый парсер, использующий правила Fountain для обычного текста.
 */
class DefaultScriptContentParser : ScriptContentParser {

    override fun parse(content: String): List<ScriptBlock> {
        val lines = content.lines()
        val blocks = mutableListOf<ScriptBlock>()

        var lastType: BlockType? = null

        for (i in lines.indices) {
            val rawLine = lines[i]
            val line = rawLine.trim()

            if (line.isEmpty()) continue

            val type = when {
                // 1. Slugline (Заголовок сцены) - приоритет #1
                isSlugline(line) -> BlockType.SLUGLINE

                // 2. Character (Имя персонажа) - CAPS, без знаков препинания в конце
                isCharacter(line) -> BlockType.CHARACTER

                // 3. Parenthetical (Ремарка) - в скобках
                line.startsWith("(") && line.endsWith(")") -> BlockType.PARENTHETICAL

                // 4. Dialogue (Диалог) - идет после Character, Parenthetical или другого Dialogue
                // Если это не заголовок и не новый персонаж, то продолжаем диалог
                lastType == BlockType.CHARACTER || lastType == BlockType.PARENTHETICAL || lastType == BlockType.DIALOGUE -> BlockType.DIALOGUE

                // 5. Transition (Переход)
                line.endsWith("ЗТМ", ignoreCase = true) || line.endsWith("CUT TO:", ignoreCase = true) -> BlockType.TRANSITION

                // 6. Action - все остальное
                else -> BlockType.ACTION
            }

            blocks.add(ScriptBlock(type, rawLine))
            lastType = type
        }

        return blocks
    }

    private fun isSlugline(line: String): Boolean {
        return line.startsWith("ИНТ.", ignoreCase = true) ||
                line.startsWith("НАТ.", ignoreCase = true) ||
                line.startsWith("INT.", ignoreCase = true) ||
                line.startsWith("EXT.", ignoreCase = true)
    }

    private fun isCharacter(line: String): Boolean {
        if (line.length < 2) return false
        // Имя персонажа обычно CAPS, но не должно заканчиваться пунктуацией (чтобы не путать с Action "ВЗРЫВ!")
        val isCaps = line.all { it.isUpperCase() || !it.isLetter() } && line.any { it.isLetter() }
        val endsWithPunctuation = line.endsWith("!") || line.endsWith("?") || line.endsWith(".")

        return isCaps && !endsWithPunctuation && !isSlugline(line)
    }
}

/**
 * Заглушка для парсинга .FDX файлов (Final Draft XML).
 */
class FdxScriptContentParser : ScriptContentParser {
    override fun parse(content: String): List<ScriptBlock> {
        // TODO: Реализовать XML парсинг для формата Final Draft
        return emptyList()
    }
}

/**
 * Основной класс для структурного разбора текста сцены.
 */
class ScriptStructureParser(private val contentParser: ScriptContentParser = DefaultScriptContentParser()) {
    fun parse(sceneText: String): List<ScriptBlock> {
        return contentParser.parse(sceneText)
    }
}
