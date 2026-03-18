package org.mosyagin.project.parser

class ScriptParser {
    // Теперь ищем ИНТ или НАТ как главный якорь начала сцены
    // (\d+[-.]\d+|\d+)? — номер сцены (опционально)
    // (ИНТ|НАТ|INT|EXT) — тип
    // (.+?) — локация
    // (ДЕНЬ|НОЧЬ|УТРО|ВЕЧЕР|DAY|NIGHT) — время
    private val sceneHeaderRegex = Regex(
        """(\d+[-.]\d+|\d+)?\.?\s+(ИНТ|НАТ|INT|EXT)\.?\s+(.+?)(?:\.|\s+--)\s+(ДЕНЬ|НОЧЬ|УТРО|ВЕЧЕР|DAY|NIGHT)""",
        RegexOption.IGNORE_CASE
    )

    fun parse(text: String, seriesNumber: Int): List<ParsedScene> {
        val scenes = mutableListOf<ParsedScene>()

        // Убираем лишние пробелы и приводим переносы к одному виду
        val cleanedText = text.replace("\r\n", "\n").replace("\r", "\n")
        val matches = sceneHeaderRegex.findAll(cleanedText).toList()

        for (i in matches.indices) {
            val currentMatch = matches[i]
            val nextMatchStart = if (i + 1 < matches.size) matches[i + 1].range.first else cleanedText.length
            val fullSceneText = cleanedText.substring(currentMatch.range.first, nextMatchStart)

            // Если номера нет, берем индекс сцены (i+1)
            val sceneNumRaw = currentMatch.groupValues[1]
            val sceneNumber = sceneNumRaw.split("[-.]").lastOrNull()?.toIntOrNull() ?: (i + 1)

            val type = currentMatch.groupValues[2].trim().uppercase()
            val location = currentMatch.groupValues[3].trim()
            val time = currentMatch.groupValues[4].trim()

            // Парсинг актеров
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
        val possibleActorLines = lines.drop(1).take(2)

        for (line in possibleActorLines) {
            val cleanLine = line.replace(Regex("""\(\d{2}:\d{2}\)"""), "") // Убираем (01:40)
                .replace(Regex("""\(.*?\)"""), "") // Убираем (ремарки)
                .trim()

            // Проверка: строка не пустая, состоит из КАПСА, запятых, пробелов или точек
            val isActorLine = cleanLine.isNotEmpty() &&
                    cleanLine.all { it.isUpperCase() || it.isWhitespace() || it == ',' || it == '.' } &&
                    !cleanLine.startsWith("ИНТ") && !cleanLine.startsWith("НАТ")

            if (isActorLine) {
                return cleanLine.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            }
        }
        return emptyList()
    }
}
