/**
 * Парсер сценария.
 * 
 * Отвечает за поиск сцен в сыром тексте, извлеченном из PDF.
 * Использует регулярные выражения (Regex) для определения заголовков сцен,
 * локаций, времени суток и имен персонажей.
 */
package org.mosyagin.project.parser

class ScriptParser {
    
    /**
     * Регулярное выражение для поиска заголовка сцены.
     * Ищет комбинации типа: "125. ИНТ. КВАРТИРА -- ДЕНЬ" или "1. НАТ. ДВОР. НОЧЬ"
     */
    private val sceneHeaderRegex = Regex(
        """(\d+[-.]\d+|\d+)?\.?\s+(ИНТ|НАТ|INT|EXT)\.?\s+(.+?)(?:\.|\s+--)\s+(ДЕНЬ|НОЧЬ|УТРО|ВЕЧЕР|DAY|NIGHT)""",
        RegexOption.IGNORE_CASE
    )

    /**
     * Разбирает весь текст сценария на список объектов [ParsedScene].
     * 
     * @param text Полный текст сценария из PDF.
     * @param seriesNumber Номер серии, к которой относится этот текст.
     */
    fun parse(text: String, seriesNumber: Int): List<ParsedScene> {
        val scenes = mutableListOf<ParsedScene>()

        // Приводим переносы строк к единому формату
        val cleanedText = text.replace("\r\n", "\n").replace("\r", "\n")
        
        // Находим все заголовки сцен в тексте
        val matches = sceneHeaderRegex.findAll(cleanedText).toList()

        for (i in matches.indices) {
            val currentMatch = matches[i]
            // Текст текущей сцены — это всё от текущего заголовка до начала следующего
            val nextMatchStart = if (i + 1 < matches.size) matches[i + 1].range.first else cleanedText.length
            val fullSceneText = cleanedText.substring(currentMatch.range.first, nextMatchStart)

            // Извлекаем номер сцены (если его нет в тексте, используем порядковый номер)
            val sceneNumRaw = currentMatch.groupValues[1]
            val sceneNumber = sceneNumRaw.split("[-.]").lastOrNull()?.toIntOrNull() ?: (i + 1)

            val type = currentMatch.groupValues[2].trim().uppercase() // ИНТ или НАТ
            val location = currentMatch.groupValues[3].trim()         // Например, "КАБИНЕТ"
            val time = currentMatch.groupValues[4].trim()             // ДЕНЬ / НОЧЬ

            // Пробуем найти персонажей, участвующих в сцене
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

    /**
     * Извлекает имена актеров из текста сцены.
     * Обычно в сценариях имена пишутся капсом в первых строках после заголовка.
     */
    private fun extractActors(text: String): List<String> {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        // Берем первые две строки после заголовка — там чаще всего список героев
        val possibleActorLines = lines.drop(1).take(2)

        for (line in possibleActorLines) {
            // Очищаем строку от таймкодов (01:20) и ремарок (в скобках)
            val cleanLine = line.replace(Regex("""\(\d{2}:\d{2}\)"""), "")
                .replace(Regex("""\(.*?\)"""), "")
                .trim()

            // Если строка написана ЗАГЛАВНЫМИ БУКВАМИ, считаем, что это список актеров
            val isActorLine = cleanLine.isNotEmpty() &&
                    cleanLine.all { it.isUpperCase() || it.isWhitespace() || it == ',' || it == '.' } &&
                    !cleanLine.startsWith("ИНТ") && !cleanLine.startsWith("НАТ")

            if (isActorLine) {
                // Разбиваем строку по запятым, если там несколько имен (например: "ИВАН, МАРИЯ")
                return cleanLine.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            }
        }
        return emptyList()
    }
}
