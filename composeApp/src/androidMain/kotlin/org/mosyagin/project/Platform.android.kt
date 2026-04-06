package org.mosyagin.project

import android.os.Build
import android.util.Log
import java.io.File
import java.util.UUID

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()

// Реализация для Android
actual fun generateUUID(): String = UUID.randomUUID().toString()

// Реализация логирования для Android
lateinit var cacheDir: File

actual fun logSync(message: String) {
    Log.d("SyncManager", message)
    try {
        if (::cacheDir.isInitialized) {
            val logFile = File(cacheDir, "crash_log.txt")
            logFile.appendText("${java.util.Date()}: $message\n")
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
