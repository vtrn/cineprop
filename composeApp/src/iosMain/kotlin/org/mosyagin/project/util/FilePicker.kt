package org.mosyagin.project.util

import androidx.compose.runtime.Composable

@Composable
actual fun rememberFilePickerLauncher(onResult: (PlatformFile?) -> Unit): FilePickerLauncher {
    return object : FilePickerLauncher {
        override fun launch() {
            // Реализация для iOS (Native)
        }
    }
}
