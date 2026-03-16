package org.mosyagin.project.util

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.mosyagin.project.db.appContext

@Composable
actual fun rememberFilePickerLauncher(onResult: (PlatformFile?) -> Unit): FilePickerLauncher {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            // Пытаемся получить имя файла
            val name = "script.pdf" // Упростим для MVP
            onResult(PlatformFile(name = name, uriString = uri.toString()))
        } else {
            onResult(null)
        }
    }

    return remember {
        object : FilePickerLauncher {
            override fun launch() {
                launcher.launch("application/pdf")
            }
        }
    }
}