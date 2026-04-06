package org.mosyagin.project.db

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

// Статическая переменная для хранения контекста (один раз при запуске)
lateinit var appContext: Context

actual fun createDriver(): SqlDriver {
    return AndroidSqliteDriver(
        schema = CinePropDatabase.Schema,
        context = appContext,
        name = "cineprop.db",
        callback = object : AndroidSqliteDriver.Callback(CinePropDatabase.Schema) {
            override fun onConfigure(db: SupportSQLiteDatabase) {
                super.onConfigure(db)
                // ВКЛЮЧАЕМ WAL РЕЖИМ
                // Это позволяет читать базу, пока идет запись в другом потоке.
                db.enableWriteAheadLogging()
                db.execSQL("PRAGMA synchronous=NORMAL;")
            }
        }
    )
}
