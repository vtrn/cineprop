package org.mosyagin.project.parser

/**
 * ScriptParser — высокоточный парсер сценариев.
 * Реализует рекомендации по группировке строк и контекстному анализу.
 */
class ScriptParser {

    private val sceneTypeRegex = Regex("""\b(ИНТ|НАТ|INT|EXT|ИНТ\.|НАТ\.|INT\.|EXT\.)\b""", RegexOption.IGNORE_CASE)
    private val pageNumberRegex = Regex("""^\s*(?:Page\s+)?\d+\s*$""", RegexOption.IGNORE_CASE)
    private val gluedCharacterRegex = Regex("""^([A-ZА-ЯЁ]{2,}[A-ZА-ЯЁ\s\d\.\-]{0,40})([a-zа-яё].*)$""")
    private val sceneNumberWithSuffixRegex = Regex("""^[\d\.\-a-zа-яёA-ZА-ЯЁ]+$""")

    fun parse(text: String, seriesNumber: Int): List<ParsedScene> {
        if (text.isBlank()) return emptyList()
        return runCatching {
            val scenes = mutableListOf<ParsedScene>()
            val lines = text.replace("\r\n", "\n").replace("\r", "\n").lines()
            var currentSceneContent = StringBuilder()
            var currentHeader: HeaderInfo? = null

            for (i in lines.indices) {
                val line = lines[i]
                val trimmed = line.trim()
                if (trimmed.isEmpty() && currentHeader == null) continue
                if (trimmed.matches(pageNumberRegex)) continue

                val typeMatch = sceneTypeRegex.find(trimmed)
                if (typeMatch != null && isLikelyHeader(trimmed)) {
                    currentHeader?.let { scenes.add(createParsedScene(it, currentSceneContent.toString(), seriesNumber)) }
                    currentHeader = parseSceneHeader(trimmed, typeMatch, i, lines, scenes.size, seriesNumber)
                    currentSceneContent = StringBuilder(line).append("\n")
                } else if (currentHeader != null) {
                    currentSceneContent.append(line).append("\n")
                }
            }
            currentHeader?.let { scenes.add(createParsedScene(it, currentSceneContent.toString(), seriesNumber)) }
            scenes
        }.getOrElse { emptyList() }
    }

    /**
     * Реализует рекомендации по группировке строк до следующего CHARACTER
     * и снижению чувствительности семантического анализа.
     */
    fun parseBlocks(content: String): List<ScriptBlock> {
        val resultBlocks = mutableListOf<ScriptBlock>()
        val rawLines = content.lines().filter { it.isNotBlank() }
        if (rawLines.isEmpty()) return emptyList()

        // 1. Нормализация (табы и спец-пробелы) перед любой обработкой
        val normalizedLines = rawLines.map { line ->
            line.replace("\u00A0", " ").replace("\t", "    ")
        }

        // 2. Динамическая калибровка по очищенным строкам
        val indents = normalizedLines.map { line -> 
            line.replace("[B]", "").takeWhile { it == ' ' }.length 
        }
        val minIndent = indents.minOrNull() ?: 0
        val maxIndent = indents.maxOrNull() ?: 0
        val midPoint = (minIndent + maxIndent) / 2

        var inDialogueMode = false

        for (line in normalizedLines) {
            val isBold = line.startsWith("[B]")
            val cleanLine = line.replace("[B]", "")
            val trimmed = cleanLine.trim()
            
            // Восстановление склеек и разделение Имя (Ремарка)
            val initialLines = if (!inDialogueMode) restoreGluedText(trimmed) else listOf(trimmed)
            val linesToProcess = initialLines.flatMap { l ->
                val bracketIdx = l.indexOf('(')
                if (bracketIdx > 0 && !sceneTypeRegex.containsMatchIn(l)) {
                    val namePart = l.substring(0, bracketIdx).trim()
                    val parenPart = l.substring(bracketIdx).trim()
                    if (isAllCaps(namePart) && namePart.length in 2..60) {
                        listOf(namePart, parenPart)
                    } else listOf(l)
                } else listOf(l)
            }
            
            for (idx in linesToProcess.indices) {
                val subLine = linesToProcess[idx]
                val realLineIndent = cleanLine.takeWhile { it == ' ' }.length
                val indent = if (subLine == trimmed || linesToProcess.size > 1) realLineIndent else midPoint
                
                // Fallback для десктопа: если indent 0, но мы в режиме диалога, притворяемся, что отступ есть.
                val effectiveIndent = if (indent == 0 && inDialogueMode) minIndent + 6 else indent

                val type = when {
                    // 1. Заголовок сцены
                    (isBold && sceneTypeRegex.containsMatchIn(subLine)) || (indent <= minIndent + 2 && sceneTypeRegex.containsMatchIn(subLine)) -> {
                        inDialogueMode = false
                        BlockType.SLUGLINE
                    }
                    
                    // 2. Имя персонажа (новое начало диалога)
                    isAllCaps(subLine) && subLine.first().isLetter() && subLine.last().isLetter() &&
                    (indent >= midPoint || isBold || (indent == 0 && subLine.length in 2..60)) && 
                    subLine.length in 2..60 && !subLine.endsWith(":") -> {
                        inDialogueMode = true
                        BlockType.CHARACTER
                    }
                    
                    // 3. Ремарка
                    (subLine.startsWith("(") || subLine.endsWith(")")) && inDialogueMode -> {
                        inDialogueMode = true
                        BlockType.PARENTHETICAL
                    }
                    
                    // 4. Диалоговый режим
                    inDialogueMode && effectiveIndent >= minIndent + 6 -> {
                        BlockType.DIALOGUE
                    }
                    
                    // 5. Переходы
                    isAllCaps(subLine) && (subLine.endsWith(":") || subLine.contains("СКЛЕЙКА")) -> {
                        inDialogueMode = false
                        BlockType.TRANSITION
                    }
                    
                    // 6. Всё остальное — это ACTION
                    else -> {
                        inDialogueMode = false
                        BlockType.ACTION
                    }
                }

                // Склеиваем блоки (Action, Dialogue, Parenthetical)
                val lastBlock = resultBlocks.lastOrNull()
                if (lastBlock != null && lastBlock.type == type && (type == BlockType.DIALOGUE || type == BlockType.ACTION || type == BlockType.PARENTHETICAL)) {
                    resultBlocks[resultBlocks.size - 1] = ScriptBlock(type, lastBlock.text + "\n" + subLine)
                } else {
                    resultBlocks.add(ScriptBlock(type, subLine))
                }
            }
        }
        return resultBlocks
    }

    private fun isAllCaps(text: String): Boolean {
        if (text.isEmpty() || text.startsWith("...")) return false
        if (text.contains('(') || text.contains(')')) return false
        return text.all { it.isUpperCase() || it.isDigit() || it.isWhitespace() || it in ".:-'#&/" }
    }

    private fun restoreGluedText(text: String): List<String> {
        if (text.isEmpty() || !text[0].isLetter()) return listOf(text)
        val match = gluedCharacterRegex.find(text)
        return if (match != null && !sceneTypeRegex.containsMatchIn(text)) {
            listOf(match.groupValues[1].trim(), match.groupValues[2].trim())
        } else listOf(text)
    }

    private fun isLikelyHeader(line: String): Boolean = line.length <= 150 && !line.startsWith("(")

    private fun parseSceneHeader(line: String, match: MatchResult, idx: Int, all: List<String>, count: Int, series: Int): HeaderInfo {
        var sceneNumber = line.substring(0, match.range.first).replace("[B]", "").trim().removeSuffix(".")
        if (sceneNumber.isEmpty() && idx > 0) {
            val prev = all[idx-1].trim()
            if (prev.length in 1..10 && sceneNumberWithSuffixRegex.matches(prev)) sceneNumber = prev
        }
        val (loc, time) = parseLocationAndTime(line.substring(match.range.last + 1).trim())
        return HeaderInfo(if (sceneNumber.isNotEmpty()) sanitizeSceneNumber(sceneNumber, series) else (count + 1).toString(), match.value.uppercase().replace(".", ""), loc, time)
    }

    private fun sanitizeSceneNumber(raw: String, series: Int): String {
        val prefix = "$series-"; val dotPrefix = "$series."
        var res = raw.trim()
        if (res.startsWith(prefix)) res = res.substring(prefix.length)
        if (res.startsWith(dotPrefix)) res = res.substring(dotPrefix.length)
        return res.trim()
    }

    private fun parseLocationAndTime(text: String): Pair<String, String> {
        val timeRegex = Regex("""\b(ДЕНЬ|НОЧЬ|УТРО|ВЕЧЕР|DAY|NIGHT)\b""", RegexOption.IGNORE_CASE)
        val m = timeRegex.find(text)
        return if (m != null) text.substring(0, m.range.first).trim('-', ' ', '.') to m.value.uppercase() 
               else text.trim('-', ' ', '.') to "НЕ УКАЗАНО"
    }

    private fun createParsedScene(header: HeaderInfo, content: String, series: Int) = ParsedScene(series.toString(), header.number, header.type, header.location, header.time, content, extractActors(content, header.number))

    private fun extractActors(text: String, sceneNum: String): List<String> {
        return text.lines().map { it.trim() }.filter { it.isNotEmpty() }
            .filter { isAllCaps(it) && it.length in 2..60 && !sceneTypeRegex.containsMatchIn(it) }
            .map { it.replace(Regex("""\(.*?\)"""), "").trim() }.distinct()
    }

    private data class HeaderInfo(val number: String, val type: String, val location: String, val time: String)
}
