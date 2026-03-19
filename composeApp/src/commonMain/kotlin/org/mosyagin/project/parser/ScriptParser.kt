/**
 * Парсер сценария.
 */
package org.mosyagin.project.parser

class ScriptParser {
    
    /**
     * Группа 1: Номер сцены (8, 5А, 1.12)
     * Группа 2: Тип (ИНТ/НАТ)
     * Группа 3: Локация
     * Группа 4: Время суток
     */
    private val sceneHeaderRegex = Regex(
        """(?:([\d\wА-Яа-я.-]+)\.?\s+)?(ИНТ|НАТ|INT|EXT)\.?\s+(.+?)(?:\.|\s+--|(?:\s+(?=ДЕНЬ|НОЧЬ|УТРО|ВЕЧЕР|DAY|NIGHT)))\s*(ДЕНЬ|НОЧЬ|УТРО|ВЕЧЕР|DAY|NIGHT)""",
        RegexOption.IGNORE_CASE
    )

    fun parse(text: String, seriesNumber: Int): List<ParsedScene> {
        val scenes = mutableListOf<ParsedScene>()
        val cleanedText = text.replace("\r\n", "\n").replace("\r", "\n")
        
        val matches = sceneHeaderRegex.findAll(cleanedText).toList()
        
        for (i in matches.indices) {
            val currentMatch = matches[i]
            val nextMatchStart = if (i + 1 < matches.size) matches[i + 1].range.first else cleanedText.length
            val fullSceneText = cleanedText.substring(currentMatch.range.first, nextMatchStart)

            // Извлекаем "сырой" номер (например, "8" или "5А" или "1.10")
            val rawNum = currentMatch.groups[1]?.value?.trim()?.removeSuffix(".") ?: ""
            
            val sceneNumber = if (rawNum.isEmpty()) {
                (i + 1).toString()
            } else {
                // Если номер содержит точку или тире (префикс серии), берем только последнюю часть
                // Фильтруем пустые части, чтобы "8." не превращалось в ""
                val parts = rawNum.split(Regex("[-.]")).filter { it.isNotEmpty() }
                parts.lastOrNull() ?: (i + 1).toString()
            }

            val type = currentMatch.groupValues[2].trim().uppercase()
            val location = currentMatch.groupValues[3].trim().removeSuffix(".")
            val time = currentMatch.groupValues[4].trim().uppercase()

            val actors = extractActors(fullSceneText)

            scenes.add(ParsedScene(
                seriesNumber = seriesNumber,
                sceneNumber = sceneNumber,
                type = type,
                location = location,
                time = time,
                content = fullSceneText,
                actors = actors
            ))
        }
        return scenes
    }

    private fun extractActors(text: String): List<String> {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val possibleActorLines = lines.drop(1).take(3)

        for (line in possibleActorLines) {
            val cleanLine = line.replace(Regex("""\(\d{2}:\d{2}\)"""), "")
                .replace(Regex("""\(.*?\)"""), "")
                .trim()

            val isActorLine = cleanLine.isNotEmpty() &&
                    cleanLine.all { it.isUpperCase() || it.isWhitespace() || it == ',' || it == '.' || it == '-' } &&
                    !cleanLine.startsWith("ИНТ") && !cleanLine.startsWith("НАТ")

            if (isActorLine) {
                return cleanLine.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            }
        }
        return emptyList()
    }
}
