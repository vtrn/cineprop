package org.mosyagin.project.parser.update

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FuzzyMatcherTest {

    @Test
    fun testLevenshteinSimilarity() {
        // Полное совпадение
        assertEquals(1.0, LevenshteinDistance.calculateSimilarity("ИНТ. КУХНЯ", "ИНТ. КУХНЯ"))
        
        // "ИНТКУХНЯ" vs "ИНТБАНЯ" (после нормализации \p{L})
        // ИНТКУХНЯ (8 букв), ИНТБАНЯ (7 букв)
        // Различия: КУХНЯ vs БАНЯ
        val sim = LevenshteinDistance.calculateSimilarity("ИНТ. КУХНЯ", "ИНТ. БАНЯ")
        assertTrue(sim > 0.3, "Similarity should be moderate: $sim")

        // Полностью разные
        assertEquals(0.0, LevenshteinDistance.calculateSimilarity("АБВ", ""))
    }

    @Test
    fun testJaccardSimilarity() {
        // Тестируем напрямую JaccardIndex.calculateSimilarity, 
        // который внутри сам вызывает TextNormalizer.normalizeForFuzzy
        val text1 = "Герой идет по улице и видит собаку"
        val text2 = "Герой видит собаку и идет по улице" // Перестановка слов
        
        // Жаккар должен дать 1.0, так как набор слов идентичен после нормализации
        val similarity = JaccardIndex.calculateSimilarity(text1, text2)
        assertEquals(1.0, similarity, "Jaccard similarity should be 1.0 for same words set")
    }

    @Test
    fun testSceneFuzzyComparator() {
        val oldSlug = "ИНТ. КУХНЯ - ДЕНЬ"
        val oldContent = "Она наливает кофе."

        // Сцена почти такая же, но поправили заголовок и чуть-чуть текст
        val newSlug = "ИНТ. КУХНЯ - УТРО"
        val newContent = "Она наливает горячий кофе."

        val score = SceneFuzzyComparator.compare(oldSlug, newSlug, oldContent, newContent)

        // Ожидаем высокий скор (> 0.6), так как большая часть совпала
        assertTrue(score > 0.6, "Score should be high for similar scenes: $score")
        assertTrue(score <= 1.0, "Score should be valid: $score")
    }

    @Test
    fun testCompletelyDifferentScenes() {
        val score = SceneFuzzyComparator.compare(
            "ИНТ. КУХНЯ", "НАТ. ЛЕС",
            "Она пьет чай.", "Медведь рычит в лесу."
        )
        assertTrue(score < 0.4, "Score should be low for different scenes: $score")
    }
}
