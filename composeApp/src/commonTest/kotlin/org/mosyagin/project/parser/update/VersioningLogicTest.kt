package org.mosyagin.project.parser.update

import org.mosyagin.project.parser.ParsedScene
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VersioningLogicTest {

    @Test
    fun testTextNormalizer() {
        val rawText = """
            СЦЕНА 1. ИНТ. ОФИС - ДЕНЬ
            12
            Я вас любил: любовь еще, быть может...
            
            В моей душе угасла не совсем;
            13
        """.trimIndent()

        // 1. Проверка удаления номеров страниц и лишних пробелов
        val normalized = TextNormalizer.normalize(rawText)
        assertTrue(!normalized.contains("\n12\n"))
        assertTrue(!normalized.contains("\n13"))
        
        // 2. Проверка очистки для хеширования (только буквы, нижний регистр)
        val sanitized = TextNormalizer.sanitizeForHashing(rawText)
        assertEquals("сценаинтофисденьяваслюбиллюбовьещебытьможетвмоейдушеугасланесовсем", sanitized)

        // 3. Проверка нормализации для Fuzzy (сохранение пробелов)
        val fuzzyNorm = TextNormalizer.normalizeForFuzzy("Привет,   Мир! 123")
        assertEquals("привет мир", fuzzyNorm)
    }

    @Test
    fun testDiffUtilsWordByWord() {
        val oldText = "Я вас любил"
        val newText = "Он вас любил"

        val diff = DiffUtils.diffWords(oldText, newText)
        
        // Ожидаем: [DELETED("Я"), UNCHANGED(" "), UNCHANGED("вас"), UNCHANGED(" "), UNCHANGED("любил"), ADDED("Он")]
        // Примечание: DiffUtils.tokenize выделяет пробелы как отдельные токены
        
        val deleted = diff.find { it.type == DiffType.DELETED }?.text
        val added = diff.find { it.type == DiffType.ADDED }?.text
        val unchanged = diff.filter { it.type == DiffType.UNCHANGED }.map { it.text }.joinToString("")

        assertEquals("Я", deleted)
        assertEquals("Он", added)
        assertTrue(unchanged.contains("вас любил"))
    }

    @Test
    fun testDiffIdenticalStrings() {
        val text = "Текст без изменений"
        val diff = DiffUtils.diffWords(text, text)
        
        assertTrue(diff.all { it.type == DiffType.UNCHANGED })
        assertEquals(text, diff.joinToString("") { it.text })
    }

    @Test
    fun testSceneMatchingLogic() {
        // Эмулируем работу ScriptUpdateManager.matchScenes
        
        // 1. Старые сцены (из БД)
        val oldScenes = listOf(
            mockDbScene(1, "1", "ИНТ. ОФИС", "Старый текст офиса"),
            mockDbScene(2, "2", "НАТ. ПАРК", "Текст который изменится на десять процентов"),
            mockDbScene(3, "10", "ИНТ. КУХНЯ", "Текст для смены номера")
        )

        // 2. Новые сцены (из парсера)
        val newScenes = listOf(
            ParsedScene("1", "ИНТ. ОФИС", "ДЕНЬ", "Старый текст офиса", emptyList()), // Exact Match
            ParsedScene("2", "НАТ. ПАРК", "ДЕНЬ", "Текст который изменится на 10 процентов!!!", emptyList()), // Fuzzy Match (номер совпал)
            ParsedScene("10А", "ИНТ. КУХНЯ", "НОЧЬ", "Текст для смены номера", emptyList()), // Fuzzy Match (текст совпал, номер нет)
            ParsedScene("11", "НАТ. УЛИЦА", "ДЕНЬ", "Совершенно новая сцена", emptyList()) // New
        )

        // Используем внутреннюю логику матчинга (упрощенно для теста)
        val matches = manualMatch(oldScenes, newScenes)

        // Сцена 1: Exact Match
        assertTrue(matches[0] is SceneMatch.Exact)
        assertEquals("1", (matches[0] as SceneMatch.Exact).scene.sceneNumber)

        // Сцена 2: Fuzzy Match (по номеру)
        assertTrue(matches[1] is SceneMatch.Fuzzy)
        assertEquals("2", (matches[1] as SceneMatch.Fuzzy).scene.sceneNumber)

        // Сцена 3: Fuzzy Match (по тексту, номер 10 -> 10А)
        assertTrue(matches[2] is SceneMatch.Fuzzy)
        assertEquals("10А", (matches[2] as SceneMatch.Fuzzy).scene.sceneNumber)
        
        // Сцена 4: New
        assertTrue(matches[3] is SceneMatch.New)
        assertEquals("11", (matches[3] as SceneMatch.New).scene.sceneNumber)
    }

    // Вспомогательные методы для тестов
    
    private fun mockDbScene(id: Long, num: String, loc: String, content: String) = 
        object : org.mosyagin.project.GetScenesBySeries {
            override val id: Long = id
            override val projectId: Long = 1
            override val seriesNumber: String = "1"
            override val sceneNumber: String = num
            override val location: String = loc
            override val isInterior: Long = 1
            override val timeOfDay: String = "ДЕНЬ"
            override val notes: String? = null
            override val needsReview: Long = 0
            override val content: String = content
            override val contentHash: String = ""
        }

    private fun manualMatch(old: List<org.mosyagin.project.GetScenesBySeries>, new: List<ParsedScene>): List<SceneMatch> {
        val results = mutableListOf<SceneMatch>()
        val remainingOld = old.toMutableList()

        for (n in new) {
            // 1. Точный поиск по номеру
            val byNum = remainingOld.find { it.sceneNumber == n.sceneNumber }
            if (byNum != null) {
                if (byNum.content == n.content) {
                    results.add(SceneMatch.Exact(byNum.id, n))
                } else {
                    results.add(SceneMatch.Fuzzy(byNum.id, n, 0.9))
                }
                remainingOld.remove(byNum)
                continue
            }

            // 2. Поиск по тексту (если номер не совпал)
            val byText = remainingOld.find { it.content == n.content }
            if (byText != null) {
                results.add(SceneMatch.Fuzzy(byText.id, n, 0.95))
                remainingOld.remove(byText)
                continue
            }

            results.add(SceneMatch.New(n))
        }
        return results
    }
}
