package org.mosyagin.project.parser.update

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FuzzyMatcherTest {

    @Test
    fun testLevenshteinSimilarity() {
        // Полное совпадение
        assertEquals(1.0, LevenshteinDistance.calculateSimilarity("ИНТ. КУХНЯ", "ИНТ. КУХНЯ"))
        
        // Одна буква разницы (9 из 10 букв совпали)
        val sim = LevenshteinDistance.calculateSimilarity("ИНТ. КУХНЯ", "ИНТ. БАНЯ")
        assertTrue(sim < 1.0 && sim > 0.5)

        // Полностью разные
        assertEquals(0.0, LevenshteinDistance.calculateSimilarity("АБВ", ""))
    }

    @Test
    fun testJaccardSimilarity() {
        val text1 = "Герой идет по улице и видит собаку"
        val text2 = "Герой видит собаку и идет по улице" // Перестановка слов
        
        // Нормализуем вручную для теста JaccardIndex
        val n1 = TextNormalizer.normalize(text1)
        val n2 = TextNormalizer.normalize(text2)
        
        // Жаккар должен дать 1.0, так как набор слов идентичен
        assertEquals(1.0, JaccardIndex.calculateSimilarity(n1, n2))
    }

    @Test
    fun testSceneFuzzyComparator() {
        val oldSlug = "ИНТ. КУХНЯ - ДЕНЬ"
        val oldContent = "Она наливает кофе."

        // Сцена почти такая же, но поправили заголовок и чуть-чуть текст
        val newSlug = "ИНТ. КУХНЯ - УТРО"
        val newContent = "Она наливает горячий кофе."

        val score = SceneFuzzyComparator.compare(oldSlug, newSlug, oldContent, newContent)

        // Ожидаем высокий скор (> 0.7), так как большая часть совпала
        assertTrue(score > 0.7, "Score should be high for similar scenes: $score")
        assertTrue(score < 1.0, "Score should not be 1.0 if there are changes: $score")
    }

    @Test
    fun testCompletelyDifferentScenes() {
        val score = SceneFuzzyComparator.compare(
            "ИНТ. КУХНЯ", "НАТ. ЛЕС",
            "Она пьет чай.", "Медведь рычит в лесу."
        )
        assertTrue(score < 0.3, "Score should be low for different scenes: $score")
    }
}
