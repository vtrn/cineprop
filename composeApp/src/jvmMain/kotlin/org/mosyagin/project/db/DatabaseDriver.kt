package org.mosyagin.project.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File

actual fun createDriver(): SqlDriver {
    val databasePath = File(System.getProperty("user.home"), ".cineapp/cineapp.db")
    
    // Создаем директорию, если она не существует
    if (!databasePath.parentFile.exists()) {
        databasePath.parentFile.mkdirs()
    }

    val databaseExists = databasePath.exists()
    val driver = JdbcSqliteDriver("jdbc:sqlite:${databasePath.absolutePath}")
    
    // Инициализируем схему только при первом создании файла базы данных
    if (!databaseExists) {
        CinePropDatabase.Schema.create(driver)
    }

    return driver
}
