package org.mosyagin.project.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import androidx.test.core.app.ApplicationProvider
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// В Android Unit-тестах нам нужен Robolectric для эмуляции Context
actual fun createTestDriver(): SqlDriver {
    return AndroidSqliteDriver(
        schema = CinePropDatabase.Schema,
        context = ApplicationProvider.getApplicationContext(),
        name = null
    )
}
