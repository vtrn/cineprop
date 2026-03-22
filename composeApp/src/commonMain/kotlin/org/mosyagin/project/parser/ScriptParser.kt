package org.mosyagin.project.parser

class ScriptParser {

    private val sceneTypeRegex = Regex("""\b(ИНТ|НАТ|INT|EXT|инт|нат|int|ext)\b""")

    fun parse(text: String, seriesNumber: Int): List<ParsedScene> {
        val scenes = mutableListOf<ParsedScene>()
        val cleanedText = text.replace("\r\n", "\n").replace("\r", "\n")
        val lines = cleanedText.lines()
        
        var currentSceneContent = StringBuilder()
        var currentHeader: HeaderInfo? = null

        for (i in lines.indices) {
            val line = lines[i].trim()
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
                
                // Если на этой строке номера нет, попробуем взять со строки выше (бывает в PDF)
                var sceneNum = rawNum
                if (sceneNum.isEmpty() && i > 0) {
                    val prevLine = lines[i-1].trim()
                    // Номер обычно короткий: "15" или "10-А"
                    if (prevLine.length in 1..8 && prevLine.all { it.isDigit() || it.isLetter() || it == '-' || it == '.' }) {
                        sceneNum = prevLine.removeSuffix(".")
                    }
                }
                
                // Если всё равно пусто, даем порядковый номер
                if (sceneNum.isEmpty()) {
                    sceneNum = (scenes.size + 1).toString()
                }

                // Извлекаем локацию и время (то, что после типа)
                val afterType = line.substring(typeMatch.range.last + 1).trim().removePrefix(".")
                val (location, time) = parseLocationAndTime(afterType)
                
                currentHeader = HeaderInfo(
                    number = sceneNum,
                    type = typeMatch.value.uppercase(),
                    location = location,
                    time = time
                )
                currentSceneContent = StringBuilder(line).append("\n")
            } else {
                // Это текст текущей сцены
                if (currentHeader != null) {
                    currentSceneContent.append(lines[i]).append("\n")
                }
            }
        }
        
        // Не забываем добавить последнюю сцену
        currentHeader?.let { 
            scenes.add(createParsedScene(it, currentSceneContent.toString(), seriesNumber)) 
        }
        
        return scenes
    }

    private data class HeaderInfo(val number: String, val type: String, val location: String, val time: String)

    private fun isLikelyHeader(line: String, match: MatchResult): Boolean {
        // Проверка: ИНТ/НАТ должен быть в начале строки (или после номера)
        val before = line.substring(0, match.range.first).trim()
        if (before.isNotEmpty() && before.length > 8) return false // Если перед ИНТ много текста - это диалог
        
        // После ИНТ/НАТ должна быть локация
        val after = line.substring(match.range.last + 1).trim().removePrefix(".")
        if (after.isEmpty()) return false
        
        // Локация в сценариях почти всегда начинается с заглавной буквы
        val firstLetter = after.firstOrNull { it.isLetter() }
        if (firstLetter != null && !firstLetter.isUpperCase()) return false
        
        // Заголовок не бывает слишком длинным
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
            // Персонажи обычно пишутся капсом в центре строки
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
