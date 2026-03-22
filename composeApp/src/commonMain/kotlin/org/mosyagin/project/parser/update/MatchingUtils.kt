package org.mosyagin.project.parser.update

/**
 * Утилиты для нормализации текста перед сравнением.
 */
object TextNormalizer {
    /**
     * Приводит текст к нижнему регистру, удаляет пунктуацию и нормализует пробелы.
     * Превращает текст в сплошную строку слов, разделенных одним пробелом.
     */
    fun normalize(text: String): String {
        return text.lowercase()
            // 1. Удаляем все переносы строк
            .replace("\n", " ")
            .replace("\r", " ")
            // 2. Удаляем пунктуацию (оставляем буквы, цифры и пробелы)
            .replace(Regex("[^a-zа-я0-9ё\\s]"), "")
            // 3. Заменяем множественные пробелы на один
            .replace("\\s+".toRegex(), " ")
            .trim()
    }
}

/**
 * Ожидаемая функция хеширования для разных платформ.
 *
 * TODO: Реализация для androidMain / jvmMain:
 * import java.security.MessageDigest
 * val bytes = input.toByteArray()
 * val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
 * return digest.fold("") { str, it -> str + "%02x".format(it) }
 *
 * TODO: Реализация для iosMain:
 * Используйте platform.CoreCrypto (CC_SHA256) или аналогичные библиотеки.
 */
expect fun calculateSha256(input: String): String

/**
 * Быстрое сравнение текстов на основе хешей.
 */
object QuickComparator {
    /**
     * Возвращает true, если нормализованные тексты полностью идентичны.
     * Использует SHA-256 хеширование для сравнения.
     */
    fun compareExact(text1: String, text2: String): Boolean {
        if (text1 == text2) return true

        val hash1 = calculateSha256(TextNormalizer.normalize(text1))
        val hash2 = calculateSha256(TextNormalizer.normalize(text2))

        return hash1 == hash2
    }
}
