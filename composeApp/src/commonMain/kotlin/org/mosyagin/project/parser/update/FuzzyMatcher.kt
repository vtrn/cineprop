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
        if (s1.isEmpty() || s2.isEmpty()) return 0.0

        val distance = computeDistance(s1, s2)
        val maxLength = max(s1.length, s2.length)
        
        return 1.0 - (distance.toDouble() / maxLength.toDouble())
    }

    /**
     * Итеративное вычисление расстояния Левенштейна (O(N*M) по времени, O(M) по памяти).
     */
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

    /**
     * Возвращает коэффициент схожести на основе пересечения множеств слов.
     */
    fun calculateSimilarity(text1: String, text2: String): Double {
        val words1 = text1.split(" ").filter { it.isNotBlank() }.toSet()
        val words2 = text2.split(" ").filter { it.isNotBlank() }.toSet()

        if (words1.isEmpty() && words2.isEmpty()) return 1.0
        
        val intersection = words1.intersect(words2).size
        val union = words1.union(words2).size
        
        return intersection.toDouble() / union.toDouble()
    }
}

/**
 * Фасад для нечеткого сравнения сцен, объединяющий разные алгоритмы.
 */
object SceneFuzzyComparator {
    private const val SLUGLINE_WEIGHT = 0.2
    private const val CONTENT_WEIGHT = 0.8

    /**
     * Сравнивает две сцены по заголовку (Слэглайну) и основному тексту.
     * Возвращает взвешенный коэффициент схожести (0.0 - 1.0).
     */
    fun compare(
        oldSlugline: String, 
        newSlugline: String, 
        oldContent: String, 
        newContent: String
    ): Double {
        // 1. Нормализация всех входных данных
        val nOldSlug = TextNormalizer.normalize(oldSlugline)
        val nNewSlug = TextNormalizer.normalize(newSlugline)
        val nOldContent = TextNormalizer.normalize(oldContent)
        val nNewContent = TextNormalizer.normalize(newContent)

        // 2. Сравнение заголовков через Левенштейна (чувствителен к порядку букв)
        val slugSimilarity = LevenshteinDistance.calculateSimilarity(nOldSlug, nNewSlug)

        // 3. Сравнение контента через Жаккара (устойчив к перестановкам слов)
        val contentSimilarity = JaccardIndex.calculateSimilarity(nOldContent, nNewContent)

        // 4. Возвращаем взвешенный результат
        return (slugSimilarity * SLUGLINE_WEIGHT) + (contentSimilarity * CONTENT_WEIGHT)
    }
}
