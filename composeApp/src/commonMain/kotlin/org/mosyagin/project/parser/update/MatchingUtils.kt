package org.mosyagin.project.parser.update

/**
 * Утилиты для нормализации текста перед сравнением.
 */
object TextNormalizer {
    /**
     * Мягкая нормализация: удаляет лишние пробелы и знаки препинания, 
     * но сохраняет структуру слов для Fuzzy поиска.
     */
    fun normalizeForFuzzy(text: String): String {
        return text.lowercase()
            .replace(Regex("[^a-zа-яё\\s]"), "") // Оставляем только буквы и пробелы
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /**
     * Максимально жесткая нормализация: оставляет только буквы.
     * Игнорирует цифры, пробелы, знаки препинания и переносы строк.
     * Используется для определения факта изменения сцены.
     */
    fun normalize(text: String): String {
        return text.lowercase()
            .replace(Regex("[^a-zа-яё]"), "")
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
     * Возвращает true, если нормализованные тексты (только буквы) полностью идентичны.
     */
    fun compareExact(text1: String, text2: String): Boolean {
        if (text1 == text2) return true
        
        val norm1 = TextNormalizer.normalize(text1)
        val norm2 = TextNormalizer.normalize(text2)
        
        if (norm1 == norm2) return true

        val hash1 = calculateSha256(norm1)
        val hash2 = calculateSha256(norm2)

        return hash1 != "" && hash1 == hash2
    }
}
