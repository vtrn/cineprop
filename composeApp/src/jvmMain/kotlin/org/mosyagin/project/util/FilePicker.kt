package org.mosyagin.project.util

import androidx.compose.runtime.Composable

@Composable
actual fun rememberFilePickerLauncher(onFilePicked: (PlatformFile?) -> Unit): FilePickerLauncher {
    return object : FilePickerLauncher {
        override fun launch() {
            // Реализация для Desktop (например, через AWT FileDialog или JFileChooser)
        }
    }
}
