package org.mosyagin.project.parser

class ScriptParser {
    // Ищем заголовок сцены
    private val sceneHeaderRegex = Regex(
        """(\d+[-.]\d+|\d+)\.?\s+(ИНТ|НАТ|INT|EXT)\.?\s+(.+?)(?:\.|\s+--)\s+(ДЕНЬ|НОЧЬ|УТРО|ВЕЧЕР|DAY|NIGHT)""",
        RegexOption.IGNORE_CASE
    )

    fun parse(text: String): List<ParsedScene> {
        val scenes = mutableListOf<ParsedScene>()
        val cleanedText = text.replace("\r\n", "\n").replace("\r", "\n")
        val matches = sceneHeaderRegex.findAll(cleanedText).toList()

        for (i in matches.indices) {
            val currentMatch = matches[i]
            val nextMatchStart = if (i + 1 < matches.size) matches[i + 1].range.first else cleanedText.length

            val fullSceneText = cleanedText.substring(currentMatch.range.first, nextMatchStart)

            val number = currentMatch.groupValues[1].trim()
            val type = currentMatch.groupValues[2].trim().uppercase()
            val location = currentMatch.groupValues[3].trim()
            val time = currentMatch.groupValues[4].trim()

            val actors = mutableListOf<String>()

            // Разбиваем текст сцены на строки и убираем пустые
            val lines = fullSceneText.lines().map { it.trim() }.filter { it.isNotEmpty() }

            // Актеры обычно идут на 2-й или 3-й строчке (после заголовка)
            val possibleActorLines = lines.drop(1).take(2)

            for (line in possibleActorLines) {
                // 1. Убираем тайминг типа (01:40)
                var cleanLine = line.replace(Regex("""\(\d{2}:\d{2}\)"""), "").trim()

                // 2. Убираем ремарки типа (МАЛЕНЬКАЯ) или (СОСЕДКА)
                cleanLine = cleanLine.replace(Regex("""\(.*?\)"""), "").trim()

                // 3. Проверяем, есть ли в строке буквы, и написана ли она КАПСОМ
                val hasLetters = cleanLine.any { it.isLetter() }
                val isAllCaps = cleanLine == cleanLine.uppercase()

                // Если строка капсом и это не случайный кусок заголовка
                if (hasLetters && isAllCaps && !cleanLine.startsWith("ИНТ") && !cleanLine.startsWith("НАТ")) {
                    val extractedActors = cleanLine.split(",")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }

                    actors.addAll(extractedActors)
                    println("CINE_DEBUG: Сцена $number -> Найдены актеры: $actors")
                    break // Нашли актеров, дальше не ищем
                }
            }

            scenes.add(ParsedScene(number, type, location, time, fullSceneText, actors))
        }
        return scenes
    }
}

data class ParsedScene(
    val number: String,
    val type: String,
    val location: String,
    val time: String,
    val content: String,
    val actors: List<String>
)