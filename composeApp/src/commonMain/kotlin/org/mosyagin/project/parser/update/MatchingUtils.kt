package org.mosyagin.project.parser.update

/**
 * Утилиты для нормализации текста перед сравнением.
 */
object TextNormalizer {

    /**
     * Агрессивная нормализация: удаляет пробелы, знаки препинания и переносы строк.
     * Оставляет только буквы и цифры в нижнем регистре.
     */
    fun normalize(text: String): String {
        return text.lowercase()
            .replace(Regex("[^a-zа-яё0-9]"), "")
    }

    /**
     * Очистка текста от технического мусора PDF (номера страниц).
     * Оставляем этот метод для обратной совместимости, если нужно.
     */
    fun cleanPdfGarbage(text: String): String {
        return text.lines()
            .filter { line -> !line.trim().matches(Regex("^\\d+$")) }
            .joinToString("\n")
            .trim()
    }

    /**
     * Максимально жесткая очистка для хеширования: только буквы.
     * Игнорирует регистр, цифры, знаки препинания, пробелы и технические маркеры.
     */
    fun sanitizeForHashing(text: String): String {
        return text
            .replace("[B]", "") // Удаляем маркер жирного шрифта, чтобы он не ломал сравнение
            .lowercase()
            .replace(Regex("[^a-zа-яё]"), "")
    }

    /**
     * Мягкая нормализация для Fuzzy поиска.
     * Сохраняет пробелы для разбивки на слова.
     */
    fun normalizeForFuzzy(text: String): String {
        return cleanPdfGarbage(text)
            .lowercase()
            .replace(Regex("[^a-zа-яё\\s]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}

/**
 * Ожидаемая функция хеширования для разных платформ.
 */
expect fun calculateSha256(input: String): String

/**
 * Быстрое сравнение текстов на основе хешей.
 */
object QuickComparator {
    /**
     * Возвращает true, если нормализованные тексты (без мусора и небуквенных знаков) идентичны.
     */
    fun compareExact(text1: String, text2: String): Boolean {
        if (text1 == text2) return true
        
        val norm1 = TextNormalizer.sanitizeForHashing(text1)
        val norm2 = TextNormalizer.sanitizeForHashing(text2)
        
        if (norm1 == norm2) return true

        val hash1 = calculateSha256(norm1)
        val hash2 = calculateSha256(norm2)

        return hash1 != "" && hash1 == hash2
    }
}
