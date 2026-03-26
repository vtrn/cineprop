package org.mosyagin.project

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.mosyagin.project.di.initKoin

fun main() {
    // Инициализируем Koin перед запуском приложения
    initKoin()
    
    application {
        val windowState = rememberWindowState()
        
        Window(
            onCloseRequest = ::exitApplication,
            state = windowState,
            title = "CineApp",
        ) {
            // Основной компонент приложения из commonMain
            App()
        }
    }
}
