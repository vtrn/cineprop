package org.mosyagin.project.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import androidx.test.core.app.ApplicationProvider

actual fun createTestDriver(): SqlDriver {
    return AndroidSqliteDriver(
        schema = CinePropDatabase.Schema,
        context = ApplicationProvider.getApplicationContext(),
        name = null // null создаст базу в памяти (in-memory)
    )
}
