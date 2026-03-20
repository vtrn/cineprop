package org.mosyagin.project.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import co.touchlab.sqliter.DatabaseConfiguration

actual fun createTestDriver(): SqlDriver {
    return NativeSqliteDriver(
        CinePropDatabase.Schema,
        "test.db",
        onConfiguration = { it.copy(inMemory = true) }
    )
}
