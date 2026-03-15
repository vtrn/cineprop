package org.mosyagin.project.db

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

// Статическая переменная для хранения контекста (один раз при запуске)
lateinit var appContext: Context

actual fun createDriver(): SqlDriver {
    return AndroidSqliteDriver(CinePropDatabase.Schema, appContext, "cineprop.db")
}