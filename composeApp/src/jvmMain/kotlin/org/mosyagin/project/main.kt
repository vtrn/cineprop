@file:OptIn(ExperimentalTime::class)

package org.mosyagin.project

import androidx.compose.material3.Text
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.mosyagin.project.di.initKoin
import java.awt.Desktop
import java.awt.Dimension
import kotlin.time.ExperimentalTime

/**
 * Вспомогательный объект для доступа к Koin-компонентам вне контекста Compose.
 */
object KoinHelper : KoinComponent {
    val supabase: SupabaseClient by inject()
}

@OptIn(DelicateCoroutinesApi::class)
fun main() {
    println("--- STARTING CINEAPP JVM ---")
    
    var initError: String? = null
    
    try {
        initKoin()
        println("Init: Koin Success")
    } catch (e: Throwable) {
        initError = "Koin Init Failed: ${e.message}"
        e.printStackTrace()
    }
    
    if (initError == null) {
        try {
            setupDeepLinking()
        } catch (e: Throwable) {
            println("Deep Link Warning: ${e.message}")
        }
    }

    application {
        val windowState = rememberWindowState()
        
        Window(
            onCloseRequest = ::exitApplication,
            state = windowState,
            title = "CineProp",
        ) {
            window.minimumSize = Dimension(960, 700)
            
            try {
                window.rootPane.putClientProperty("apple.awt.fullWindowContent", true)
                window.rootPane.putClientProperty("apple.awt.transparentTitleBar", true)
                window.rootPane.putClientProperty("apple.awt.windowTitleVisible", false)
            } catch (ignore: Exception) {}

            if (initError != null) {
                Text("Error during launch: $initError")
            } else {
                App()
            }
        }
    }
}

@OptIn(DelicateCoroutinesApi::class)
private fun setupDeepLinking() {
    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.APP_OPEN_URI)) {
        Desktop.getDesktop().setOpenURIHandler { event ->
            val uri = event.uri
            println("Deep Link Received: $uri")
            
            GlobalScope.launch {
                try {
                    val params = mutableMapOf<String, String>()
                    
                    // Разбираем query (?key=value)
                    uri.query?.split("&")?.forEach { pair ->
                        val parts = pair.split("=", limit = 2)
                        if (parts.size == 2) params[parts[0]] = parts[1]
                    }
                    
                    // Разбираем fragment (#key=value)
                    uri.fragment?.split("&")?.forEach { pair ->
                        val parts = pair.split("=", limit = 2)
                        if (parts.size == 2) params[parts[0]] = parts[1]
                    }

                    val accessToken = params["access_token"]
                    val refreshToken = params["refresh_token"]
                    val expiresInSeconds = params["expires_in"]?.toLongOrNull() ?: 3600L
                    val code = params["code"]

                    when {
                        // 1. Implicit Flow (токены в URL - основной вариант для Magic Link на Desktop)
                        accessToken != null -> {
                            println("Deep Link: Implicit Flow detected. Importing session...")
                            val session = UserSession(
                                accessToken = accessToken,
                                refreshToken = refreshToken ?: "",
                                expiresIn = expiresInSeconds,
                                tokenType = params["token_type"] ?: "bearer",
                                user = null
                            )
                            KoinHelper.supabase.auth.importSession(session)
                            
                            // Пытаемся подтянуть данные пользователя, чтобы UI обновился
                            try {
                                KoinHelper.supabase.auth.retrieveUserForCurrentSession()
                                println("Deep Link: User info retrieved successfully")
                            } catch (e: Exception) {
                                println("Deep Link: Could not fetch user info automatically: ${e.message}")
                            }
                            println("Deep Link: Auth Success (Implicit)!")
                        }
                        
                        // 2. PKCE Flow (код в URL - требует verifier в памяти)
                        code != null -> {
                            println("Deep Link: Attempting PKCE exchange...")
                            try {
                                KoinHelper.supabase.auth.exchangeCodeForSession(code)
                                println("Deep Link: Auth Success (PKCE)!")
                            } catch (e: IllegalArgumentException) {
                                if (e.message?.contains("No code verifier") == true) {
                                    println("Deep Link Error: PKCE verifier missing (No code verifier stored).")
                                    println("Hint: PKCE login must be completed in the same app session. For testing, use a link with #access_token=...")
                                } else throw e
                            }
                        }
                    }
                } catch (e: Exception) {
                    println("Deep Link Error: ${e.message}")
                    e.printStackTrace()
                }
            }
        }
        println("Deep Link: Handler Registered")
    }
}
