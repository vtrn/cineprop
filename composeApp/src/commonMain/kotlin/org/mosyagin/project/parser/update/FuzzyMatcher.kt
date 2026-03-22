package org.mosyagin.project.parser.update

import kotlin.math.max
import kotlin.math.min

/**
 * Вычисление расстояния Левенштейна для сравнения коротких строк (заголовков).
 */
object LevenshteinDistance {

    /**
     * Возвращает коэффициент схожести от 0.0 до 1.0.
     */
    fun calculateSimilarity(s1: String, s2: String): Double {
        if (s1 == s2) return 1.0
        val n1 = TextNormalizer.normalize(s1)
        val n2 = TextNormalizer.normalize(s2)
        if (n1 == n2) return 1.0
        if (n1.isEmpty() || n2.isEmpty()) return 0.0

        val distance = computeDistance(n1, n2)
        val maxLength = max(n1.length, n2.length)
        
        return 1.0 - (distance.toDouble() / maxLength.toDouble())
    }

    private fun computeDistance(s1: String, s2: String): Int {
        val n = s1.length
        val m = s2.length
        var prev = IntArray(m + 1) { it }
        var curr = IntArray(m + 1)
        for (i in 1..n) {
            curr[0] = i
            for (j in 1..m) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                curr[j] = min(min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost)
            }
            val temp = prev
            prev = curr
            curr = temp
        }
        return prev[m]
    }
}

/**
 * Вычисление коэффициента Жаккара (мешок слов) для длинных текстов.
 */
object JaccardIndex {
    fun calculateSimilarity(text1: String, text2: String): Double {
        // Используем специальную нормализацию для Fuzzy, которая сохраняет пробелы
        val words1 = TextNormalizer.normalizeForFuzzy(text1).split(" ").filter { it.isNotBlank() }.toSet()
        val words2 = TextNormalizer.normalizeForFuzzy(text2).split(" ").filter { it.isNotBlank() }.toSet()

        if (words1.isEmpty() && words2.isEmpty()) return 1.0
        if (words1.isEmpty() || words2.isEmpty()) return 0.0
        
        val intersection = words1.intersect(words2).size
        val union = words1.union(words2).size
        
        return intersection.toDouble() / union.toDouble()
    }
}

/**
 * Фасад для нечеткого сравнения сцен.
 */
object SceneFuzzyComparator {
    private const val SLUGLINE_WEIGHT = 0.3
    private const val CONTENT_WEIGHT = 0.7

    fun compare(
        oldSlugline: String, 
        newSlugline: String, 
        oldContent: String, 
        newContent: String
    ): Double {
        // Сравнение заголовков
        val slugSimilarity = LevenshteinDistance.calculateSimilarity(oldSlugline, newSlugline)
        // Сравнение контента
        val contentSimilarity = JaccardIndex.calculateSimilarity(oldContent, newContent)

        return (slugSimilarity * SLUGLINE_WEIGHT) + (contentSimilarity * CONTENT_WEIGHT)
    }
}
