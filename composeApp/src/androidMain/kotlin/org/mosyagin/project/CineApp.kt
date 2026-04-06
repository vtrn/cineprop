package org.mosyagin.project

import android.app.Application
import android.util.Log
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import org.koin.android.ext.koin.androidContext
import org.mosyagin.project.db.appContext
import org.mosyagin.project.di.initKoin
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

class CineApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Инициализируем папку кэша для логов
        org.mosyagin.project.cacheDir = externalCacheDir ?: cacheDir

        // Настройка глобального перехватчика ошибок
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            saveCrashLog(throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }

        appContext = applicationContext
        try {
            PDFBoxResourceLoader.init(applicationContext)
            initKoin {
                androidContext(this@CineApp)
            }
        } catch (e: Exception) {
            saveCrashLog(e)
            Log.e("CineApp", "Initialization error", e)
            throw e
        }
    }

    private fun saveCrashLog(throwable: Throwable) {
        try {
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            val stackTrace = sw.toString()

            val logFile = File(externalCacheDir ?: cacheDir, "crash_log.txt")
            logFile.appendText("\n--- CRASH ---\nTime: ${java.util.Date()}\n$stackTrace\n")
            
            Log.e("CineApp", "CRASH SAVED TO ${logFile.absolutePath}")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
