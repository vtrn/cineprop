package org.mosyagin.project.parser

class ScriptParser {
    // Это "фильтры" (Regex), которые ищут заголовки
    private val sceneHeaderRegex = Regex("""^(\d+[-.]\d+|\d+)\.\s+(ИНТ|НАТ)\.\s+(.+)\.\s+(ДЕНЬ|НОЧЬ|УТРО|ВЕЧЕР)""", RegexOption.MULTILINE)

    fun parse(text: String): List<ParsedScene> {
        val scenes = mutableListOf<ParsedScene>()

        // 1. Ищем все заголовки сцен в тексте
        val matches = sceneHeaderRegex.findAll(text).toList()

        for (i in matches.indices) {
            val currentMatch = matches[i]
            val nextMatchStart = if (i + 1 < matches.size) matches[i + 1].range.first else text.length

            // Вырезаем текст ОДНОЙ сцены (от текущего заголовка до следующего)
            val fullSceneText = text.substring(currentMatch.range.first, nextMatchStart)

            // Разбираем заголовок на части
            val number = currentMatch.groupValues[1]
            val type = currentMatch.groupValues[2] // ИНТ или НАТ
            val location = currentMatch.groupValues[3]
            val time = currentMatch.groupValues[4]

            // Ищем персонажей (это вторая строка после заголовка, обычно капсом)
            val lines = fullSceneText.lines().filter { it.isNotBlank() }
            val charactersLine = if (lines.size > 1) lines[1] else ""
            val actors = charactersLine.split(",").map { it.trim() }.filter { it.all { char -> char.isUpperCase() || char == ' ' } }

            scenes.add(ParsedScene(number, type, location, time, fullSceneText, actors))
        }
        return scenes
    }
}

// Простая моделька для хранения данных в памяти во время парсинга
data class ParsedScene(
    val number: String,
    val type: String,
    val location: String,
    val time: String,
    val content: String,
    val actors: List<String>
)