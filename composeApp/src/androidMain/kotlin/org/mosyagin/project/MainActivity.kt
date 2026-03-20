package org.mosyagin.project

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import org.mosyagin.project.db.appContext
import org.mosyagin.project.di.initKoin
import org.koin.android.ext.koin.androidContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appContext = applicationContext
        PDFBoxResourceLoader.init(applicationContext)

        // Инициализация Koin для Android
        initKoin {
            androidContext(this@MainActivity)
        }

        enableEdgeToEdge()
        setContent {
            App()
        }
    }
}
