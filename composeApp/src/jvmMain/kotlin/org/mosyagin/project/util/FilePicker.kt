package org.mosyagin.project.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

@Composable
actual fun rememberFilePickerLauncher(onResult: (PlatformFile?) -> Unit): FilePickerLauncher {
    return remember {
        object : FilePickerLauncher {
            override fun launch() {
                // Используем AWT FileDialog для выбора файла
                val dialog = FileDialog(null as Frame?, "Выберите файл", FileDialog.LOAD)
                dialog.isVisible = true
                
                if (dialog.file != null) {
                    val file = File(dialog.directory, dialog.file)
                    val bytes = try { file.readBytes() } catch (e: Exception) { null }
                    
                    onResult(
                        PlatformFile(
                            name = file.name,
                            bytes = bytes,
                            uriString = file.absolutePath
                        )
                    )
                } else {
                    onResult(null)
                }
            }
        }
    }
}
