package org.mosyagin.project

import android.app.Application
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import org.koin.android.ext.koin.androidContext
import org.mosyagin.project.db.appContext
import org.mosyagin.project.di.initKoin

class CineApp : Application() {
    override fun onCreate() {
        super.onCreate()

        appContext = applicationContext
        PDFBoxResourceLoader.init(applicationContext)

        initKoin {
            androidContext(this@CineApp)
        }
    }
}
