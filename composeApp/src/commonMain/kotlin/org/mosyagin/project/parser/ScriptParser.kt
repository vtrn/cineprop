package org.mosyagin.project.parser

class ScriptParser {

    private val sceneTypeRegex = Regex("""\b(ИНТ|НАТ|INT|EXT|инт|нат|int|ext)\b""")
    private val pageNumberRegex = Regex("""^\d+$""")

    fun parse(text: String, seriesNumber: Int): List<ParsedScene> {
        val scenes = mutableListOf<ParsedScene>()
        val cleanedText = text.replace("\r\n", "\n").replace("\r", "\n")
        
        // Предварительная очистка текста от мусора PDF (номера страниц на отдельных строках)
        val filteredLines = cleanedText.lines()
            .filter { line -> !line.trim().matches(pageNumberRegex) }
        
        var currentSceneContent = StringBuilder()
        var currentHeader: HeaderInfo? = null

        for (i in filteredLines.indices) {
            val line = filteredLines[i].trim()
            if (line.isEmpty()) {
                if (currentHeader != null) currentSceneContent.append("\n")
                continue
            }

            val typeMatch = sceneTypeRegex.find(line)
            
            // Если нашли ИНТ/НАТ и это похоже на заголовок
            if (typeMatch != null && isLikelyHeader(line, typeMatch)) {
                // Сохраняем предыдущую накопленную сцену
                currentHeader?.let { 
                    scenes.add(createParsedScene(it, currentSceneContent.toString(), seriesNumber)) 
                }
                
                // Извлекаем номер сцены (то, что перед типом)
                val rawNum = line.substring(0, typeMatch.range.first).trim().removeSuffix(".")
                
                // Если на этой строке номера нет, попробуем взять со строки выше
                var sceneNum = rawNum
                if (sceneNum.isEmpty() && i > 0) {
                    val prevLine = filteredLines[i-1].trim()
                    if (prevLine.length in 1..8 && prevLine.all { it.isDigit() || it.isLetter() || it == '-' || it == '.' }) {
                        sceneNum = prevLine.removeSuffix(".")
                    }
                }
                
                // Очистка номера от дублирующейся серии
                val cleanedSceneNum = sanitizeSceneNumber(sceneNum, seriesNumber)

                val finalSceneNum = if (cleanedSceneNum.isEmpty()) {
                    (scenes.size + 1).toString()
                } else {
                    cleanedSceneNum
                }

                val afterType = line.substring(typeMatch.range.last + 1).trim().removePrefix(".")
                val (location, time) = parseLocationAndTime(afterType)
                
                currentHeader = HeaderInfo(
                    number = finalSceneNum,
                    type = typeMatch.value.uppercase(),
                    location = location,
                    time = time
                )
                currentSceneContent = StringBuilder(line).append("\n")
            } else {
                // Это текст текущей сцены
                if (currentHeader != null) {
                    currentSceneContent.append(filteredLines[i]).append("\n")
                }
            }
        }
        
        // Не забываем добавить последнюю сцену
        currentHeader?.let { 
            scenes.add(createParsedScene(it, currentSceneContent.toString(), seriesNumber)) 
        }
        
        return scenes
    }

    private fun sanitizeSceneNumber(rawNum: String, currentSeries: Int): String {
        val seriesPrefixes = listOf("$currentSeries-", "$currentSeries.")
        var result = rawNum.trim()
        
        for (prefix in seriesPrefixes) {
            if (result.startsWith(prefix)) {
                result = result.substring(prefix.length).trim()
            }
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
        // При разбиении на блоки также игнорируем строки-числа (номера страниц)
        val lines = content.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.matches(pageNumberRegex) }
        
        for (i in lines.indices) {
            val line = lines[i]
            
            val type = when {
                i == 0 && sceneTypeRegex.containsMatchIn(line) -> BlockType.SLUGLINE
                line.all { it.isUpperCase() || it.isWhitespace() || it == '.' || it == ':' } && line.length in 2..40 -> BlockType.CHARACTER
                line.startsWith("(") && line.endsWith(")") -> BlockType.PARENTHETICAL
                i > 0 && (blocks.last().type == BlockType.CHARACTER || blocks.last().type == BlockType.PARENTHETICAL) -> BlockType.DIALOGUE
                line.endsWith(":") || line.contains("СКЛЕЙКА") || line.contains("ЗТМ") || line.contains("CUT TO") -> BlockType.TRANSITION
                else -> BlockType.ACTION
            }
            
            blocks.add(ScriptBlock(type, line))
        }
        return blocks
    }

    private data class HeaderInfo(val number: String, val type: String, val location: String, val time: String)

    private fun isLikelyHeader(line: String, match: MatchResult): Boolean {
        val before = line.substring(0, match.range.first).trim()
        if (before.isNotEmpty() && before.length > 12) return false 
        val after = line.substring(match.range.last + 1).trim().removePrefix(".")
        if (after.isEmpty()) return false
        val firstLetter = after.firstOrNull { it.isLetter() }
        if (firstLetter != null && !firstLetter.isUpperCase()) return false
        if (line.length > 150) return false 
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
            content = content.trim(),
            actors = extractActors(content)
        )
    }

    private fun extractActors(text: String): List<String> {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) return emptyList()
        val actors = mutableSetOf<String>()
        val possibleActorLines = lines.drop(1).take(20)
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
