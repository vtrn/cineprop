package org.mosyagin.project

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
// ВНИМАНИЕ: БЕЗ ДЕФИСА В СЛОВЕ tomroush
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import org.mosyagin.project.db.appContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appContext = applicationContext

        // Если библиотека подключена, эта строка станет цветной (не красной)
        PDFBoxResourceLoader.init(applicationContext)

        enableEdgeToEdge()
        setContent {
            App()
        }
    }
}