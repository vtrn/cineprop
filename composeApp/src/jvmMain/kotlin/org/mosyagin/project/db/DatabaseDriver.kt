package org.mosyagin.project.db

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File

actual fun createDriver(): SqlDriver {
    val databasePath = File(System.getProperty("user.home"), ".cineapp/cineapp.db")
    
    // Создаем директорию, если она не существует
    if (!databasePath.parentFile.exists()) {
        databasePath.parentFile.mkdirs()
    }

    // Функция для создания драйвера и проверки/создания схемы
    fun initDriver(): SqlDriver {
        val driver = JdbcSqliteDriver("jdbc:sqlite:${databasePath.absolutePath}")
        
        // Получаем текущую версию схемы из БД (PRAGMA user_version)
        val currentTableVersion = driver.executeQuery(
            identifier = null,
            sql = "PRAGMA user_version;",
            mapper = { cursor ->
                QueryResult.Value(if (cursor.next().value) cursor.getLong(0) else 0L)
            },
            parameters = 0
        ).value ?: 0L

        val requiredVersion = CinePropDatabase.Schema.version

        if (currentTableVersion == 0L) {
            // База пустая или новая — создаем структуру
            CinePropDatabase.Schema.create(driver)
            driver.execute(null, "PRAGMA user_version = $requiredVersion;", 0)
        } else if (currentTableVersion < requiredVersion) {
            // Схема устарела!
            driver.close()
            databasePath.delete()
            // Рекурсивно вызываем инициализацию для создания нового файла
            return initDriver()
        }
        
        return driver
    }

    return try {
        initDriver()
    } catch (e: Exception) {
        // На случай любых других критических ошибок со схемой в dev-режиме
        if (databasePath.exists()) {
            databasePath.delete()
        }
        val driver = JdbcSqliteDriver("jdbc:sqlite:${databasePath.absolutePath}")
        CinePropDatabase.Schema.create(driver)
        driver.execute(null, "PRAGMA user_version = ${CinePropDatabase.Schema.version};", 0)
        driver
    }
}
