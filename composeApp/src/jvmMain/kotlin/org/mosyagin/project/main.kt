package org.mosyagin.project

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.mosyagin.project.di.initKoin

fun main() {
    // Инициализируем Koin перед запуском приложения
    initKoin()
    
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "cineapp",
        ) {
            App()
        }
    }
}
