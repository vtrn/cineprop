package org.mosyagin.project.data

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.test.runTest
import org.mosyagin.project.BuildKonfig
import kotlin.test.Test
import kotlin.test.assertTrue

class SupabaseConnectionTest {

    @Test
    fun testSupabaseConnectionAndSelect() = runTest {
        // 1. Проверяем, что ключи не пусты (подтянулись из local.properties)
        assertTrue(BuildKonfig.SUPABASE_URL.isNotEmpty(), "SUPABASE_URL is empty. Check local.properties")
        assertTrue(BuildKonfig.SUPABASE_KEY.isNotEmpty(), "SUPABASE_KEY is empty. Check local.properties")

        // 2. Инициализируем клиент
        val client = createSupabaseClient(
            supabaseUrl = BuildKonfig.SUPABASE_URL,
            supabaseKey = BuildKonfig.SUPABASE_KEY
        ) {
            install(Postgrest)
        }

        try {
            // 3. Пытаемся сделать запрос к таблице projects
            // Используем .select().data, так как нам достаточно убедиться, что запрос ушел и пришел ответ
            val response = client.postgrest["projects"].select()

            // Если мы дошли до сюда без исключений, значит соединение установлено
            println("Successfully connected to Supabase.")
            
            // Проверяем, что данные пришли (даже если это пустой массив [])
            assertTrue(response.data.isNotEmpty(), "Response data should not be empty (should at least be '[]')")
            
        } catch (e: Exception) {
            println("Failed to connect to Supabase: ${e.message}")
            e.printStackTrace()
            throw e
        } finally {
            client.close()
        }
    }
}
