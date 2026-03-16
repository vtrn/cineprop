package org.mosyagin.project.db

import androidx.compose.runtime.staticCompositionLocalOf
import org.mosyagin.project.DatabaseQueries

val LocalDatabaseQueries = staticCompositionLocalOf<DatabaseQueries> {
    error("DatabaseQueries not provided")
}
