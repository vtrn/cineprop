package org.mosyagin.project.parser

class ScriptParser {

    private val sceneTypeRegex = Regex("""\b(ИНТ|НАТ|INT|EXT|инт|нат|int|ext)\b""")
    private val pageNumberRegex = Regex("""^\d+$""")

    fun parse(text: String, seriesNumber: Int): List<ParsedScene> {
        val scenes = mutableListOf<ParsedScene>()
        val cleanedText = text.replace("\r\n", "\n").replace("\r", "\n")
        
        val lines = cleanedText.lines()
        
        var currentSceneContent = StringBuilder()
        var currentHeader: HeaderInfo? = null

        for (i in lines.indices) {
            val line = lines[i]
            val trimmed = line.trim()
            
            if (trimmed.isEmpty() && currentHeader == null) continue
            if (trimmed.matches(pageNumberRegex)) continue

            val typeMatch = sceneTypeRegex.find(trimmed)
            
            if (typeMatch != null && isLikelyHeader(trimmed, typeMatch)) {
                currentHeader?.let { 
                    scenes.add(createParsedScene(it, currentSceneContent.toString(), seriesNumber)) 
                }
                
                val rawNum = trimmed.substring(0, typeMatch.range.first).trim().removeSuffix(".")
                var sceneNum = rawNum
                if (sceneNum.isEmpty() && i > 0) {
                    val prevLine = lines[i-1].trim()
                    if (prevLine.length in 1..8 && prevLine.all { it.isDigit() || it.isLetter() || it == '-' || it == '.' }) {
                        sceneNum = prevLine.removeSuffix(".")
                    }
                }
                
                val cleanedSceneNum = sanitizeSceneNumber(sceneNum, seriesNumber)
                val finalSceneNum = if (cleanedSceneNum.isEmpty()) (scenes.size + 1).toString() else cleanedSceneNum
                val afterType = trimmed.substring(typeMatch.range.last + 1).trim().removePrefix(".")
                val (location, time) = parseLocationAndTime(afterType)
                
                currentHeader = HeaderInfo(
                    number = finalSceneNum,
                    type = typeMatch.value.uppercase(),
                    location = location,
                    time = time
                )
                currentSceneContent = StringBuilder(line).append("\n")
            } else {
                if (currentHeader != null) {
                    currentSceneContent.append(line).append("\n")
                }
            }
        }
        
        currentHeader?.let { 
            scenes.add(createParsedScene(it, currentSceneContent.toString(), seriesNumber)) 
        }
        
        return scenes
    }

    private fun sanitizeSceneNumber(rawNum: String, currentSeries: Int): String {
        val seriesPrefixes = listOf("$currentSeries-", "$currentSeries.")
        var result = rawNum.trim()
        for (prefix in seriesPrefixes) {
            if (result.startsWith(prefix)) result = result.substring(prefix.length).trim()
        }
        if (result.contains("-") || result.contains(".")) {
             val parts = result.split(Regex("[-.]"))
             if (parts.firstOrNull() == currentSeries.toString()) {
                 return parts.drop(1).joinToString("-")
             }
        }
        return result
    }

    fun parseBlocks(content: String): List<ScriptBlock> {
        val blocks = mutableListOf<ScriptBlock>()
        val lines = content.lines()
        
        var inDialogueMode = false
        
        println("--- START PARSING BLOCKS ---") // ДЛЯ ПРОВЕРКИ

        for (i in lines.indices) {
            val originalLine = lines[i]
            val trimmed = originalLine.trim()
            
            if (trimmed.isEmpty()) {
                inDialogueMode = false
                continue
            }
            if (trimmed.matches(pageNumberRegex)) continue

            val indent = originalLine.takeWhile { it == ' ' }.length
            
            // ВЫВОДИМ В КОНСОЛЬ КАЖДУЮ СТРОКУ И ЕЁ ОТСТУП
            println("DEBUG: [indent=$indent] text='$trimmed'")

            val type = when {
                indent < 10 && sceneTypeRegex.containsMatchIn(trimmed) -> {
                    inDialogueMode = false
                    BlockType.SLUGLINE
                }
                indent > 15 && isCharacterName(trimmed) -> {
                    inDialogueMode = true
                    BlockType.CHARACTER
                }
                indent > 8 && trimmed.startsWith("(") && trimmed.endsWith(")") -> {
                    inDialogueMode = true
                    BlockType.PARENTHETICAL
                }
                inDialogueMode && indent > 6 -> {
                    BlockType.DIALOGUE
                }
                indent > 25 && (trimmed.endsWith(":") || trimmed.contains("СКЛЕЙКА") || trimmed.contains("ЗТМ")) -> {
                    inDialogueMode = false
                    BlockType.TRANSITION
                }
                else -> {
                    inDialogueMode = false
                    BlockType.ACTION
                }
            }
            
            if (blocks.isNotEmpty() && blocks.last().type == type && (type == BlockType.DIALOGUE || type == BlockType.ACTION)) {
                val lastBlock = blocks.last()
                blocks[blocks.size - 1] = ScriptBlock(type, lastBlock.text + "\n" + trimmed)
            } else {
                blocks.add(ScriptBlock(type, trimmed))
            }
        }
        println("--- END PARSING BLOCKS ---")
        return blocks
    }

    private fun isCharacterName(text: String): Boolean {
        if (text.length < 2 || text.length > 50) return false
        return text.all { 
            it.isUpperCase() || it.isDigit() || it.isWhitespace() || 
            it == '.' || it == ':' || it == '(' || it == ')' || it == '-' || it == '#' || it == '\''
        }
    }

    private data class HeaderInfo(val number: String, val type: String, val location: String, val time: String)

    private fun isLikelyHeader(line: String, match: MatchResult): Boolean {
        if (line.isEmpty() || line.length > 150) return false
        val after = line.substring(match.range.last + 1).trim().removePrefix(".")
        if (after.isEmpty()) return false
        return true
    }

    private fun parseLocationAndTime(text: String): Pair<String, String> {
        val timeRegex = Regex("""\b(ДЕНЬ|НОЧЬ|УТРО|ВЕЧЕР|DAY|NIGHT|день|ночь|утро|вечер|day|night)\b""", RegexOption.IGNORE_CASE)
        val timeMatch = timeRegex.find(text)
        
        return if (timeMatch != null) {
            val location = text.substring(0, timeMatch.range.first).trim().removeSuffix(".").trim('-', ' ', '.')
            location to timeMatch.value.uppercase()
        } else {
            text.trim().removeSuffix(".").trim('-', ' ', '.') to "НЕ УКАЗАНО"
        }
    }

    private fun createParsedScene(header: HeaderInfo, content: String, seriesNum: Int): ParsedScene {
        return ParsedScene(
            seriesNumber = seriesNum.toString(),
            sceneNumber = header.number,
            type = header.type,
            location = header.location,
            time = header.time,
            content = content, 
            actors = extractActors(content)
        )
    }

    private fun extractActors(text: String): List<String> {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) return emptyList()
        val actors = mutableSetOf<String>()
        val possibleActorLines = lines.drop(1).take(30)
        for (line in possibleActorLines) {
            val cleanLine = line.replace(Regex("""\(.*?\)"""), "").trim()
            if (cleanLine.length in 2..30 && 
                cleanLine.all { it.isUpperCase() || it.isWhitespace() || it == '.' } &&
                !cleanLine.contains(Regex("ИНТ|НАТ|INT|EXT|СЦЕНА|СЕРИЯ"))
            ) {
                actors.add(cleanLine)
            }
        }
        return actors.toList()
    }
}
