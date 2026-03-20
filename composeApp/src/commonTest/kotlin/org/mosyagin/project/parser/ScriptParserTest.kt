package org.mosyagin.project.parser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScriptParserTest {

    private val parser = ScriptParser()

    @Test
    fun testBasicSceneParsing() {
        val scriptText = """
            1. ИНТ. КУХНЯ - ДЕНЬ
            ГЕРОЙ заходит на кухню.
            
            2. НАТ. ДВОР - НОЧЬ
            СОСЕД копает яму.
        """.trimIndent()

        val scenes = parser.parse(scriptText, 1)

        assertEquals(2, scenes.size)
        
        assertEquals("1", scenes[0].sceneNumber)
        assertEquals("ИНТ", scenes[0].type)
        assertEquals("КУХНЯ", scenes[0].location)
        assertEquals("ДЕНЬ", scenes[0].time)

        assertEquals("2", scenes[1].sceneNumber)
        assertEquals("НАТ", scenes[1].type)
        assertEquals("ДВОР", scenes[1].location)
        assertEquals("НОЧЬ", scenes[1].time)
    }

    @Test
    fun testSceneWithLetters() {
        val scriptText = """
            5А. ИНТ. ОФИС - УТРО
            В офисе пусто.
        """.trimIndent()

        val scenes = parser.parse(scriptText, 1)
        assertEquals(1, scenes.size)
        assertEquals("5А", scenes[0].sceneNumber)
    }

    @Test
    fun testActorExtraction() {
        val scriptText = """
            10. ИНТ. ГОСТИНАЯ - ВЕЧЕР
            
            ИВАН, МАРИЯ
            
            Они сидят за столом.
        """.trimIndent()

        val scenes = parser.parse(scriptText, 1)
        assertEquals(1, scenes.size)
        
        val actors = scenes[0].actors
        assertTrue(actors.contains("ИВАН"))
        assertTrue(actors.contains("МАРИЯ"))
        assertEquals(2, actors.size)
    }

    @Test
    fun testMultipleActorsOnOneLine() {
        val scriptText = """
            15. НАТ. ЛЕС - ДЕНЬ
            
            ОХОТНИК, ВОЛК, ЛЕСНИК
            
            Погоня продолжается.
        """.trimIndent()

        val scenes = parser.parse(scriptText, 1)
        val actors = scenes[0].actors
        assertEquals(3, actors.size)
        assertTrue(actors.contains("ОХОТНИК"))
        assertTrue(actors.contains("ВОЛК"))
        assertTrue(actors.contains("ЛЕСНИК"))
    }
}
