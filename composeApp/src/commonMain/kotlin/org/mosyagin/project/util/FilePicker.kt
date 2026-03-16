package org.mosyagin.project.util

import androidx.compose.runtime.Composable

// Описываем, что мы хотим получить "пускатель" для выбора файла
@Composable
expect fun rememberFilePickerLauncher(onResult: (PlatformFile?) -> Unit): FilePickerLauncher

interface FilePickerLauncher {
    fun launch()
}

// Простая обертка над файлом, чтобы commonMain понимал, что мы выбрали
data class PlatformFile(
    val name: String,
    val bytes: ByteArray? = null,
    val uriString: String? = null // Для Android будем хранить URI
)