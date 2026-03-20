package org.mosyagin.project.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File

actual fun createDriver(): SqlDriver {
    val databasePath = File(System.getProperty("user.home"), ".cineapp/cineapp.db")
    if (!databasePath.parentFile.exists()) {
        databasePath.parentFile.mkdirs()
    }
    val driver = JdbcSqliteDriver("jdbc:sqlite:${databasePath.absolutePath}")
    CinePropDatabase.Schema.create(driver)
    return driver
}
