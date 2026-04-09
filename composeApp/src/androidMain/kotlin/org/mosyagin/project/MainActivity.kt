package org.mosyagin.project

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.handleDeeplinks
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    
    private val supabase: SupabaseClient by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Обрабатываем Deep Link при холодном запуске
        supabase.handleDeeplinks(intent)

        enableEdgeToEdge()
        setContent {
            App()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Обрабатываем Deep Link, если приложение уже открыто
        supabase.handleDeeplinks(intent)
    }
}
