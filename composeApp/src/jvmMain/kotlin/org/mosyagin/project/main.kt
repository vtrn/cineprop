package org.mosyagin.project

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.mosyagin.project.di.initKoin
import java.awt.Dimension

fun main() {
    // Инициализируем Koin перед запуском приложения
    initKoin()
    
    application {
        val windowState = rememberWindowState()
        
        Window(
            onCloseRequest = ::exitApplication,
            state = windowState,
            title = "", // Скрываем текст заголовка для macOS стиля
        ) {
            // macOS-specific "Pro" appearance: buttons over content, transparent title bar
            window.rootPane.putClientProperty("apple.awt.fullWindowContent", true)
            window.rootPane.putClientProperty("apple.awt.transparentTitleBar", true)
            window.rootPane.putClientProperty("apple.awt.windowTitleVisible", false)
            
            // Минимальный размер для корректного отображения десктопного лейаута
            window.minimumSize = Dimension(960, 700)

            // Основной компонент приложения из commonMain
            App()
        }
    }
}
