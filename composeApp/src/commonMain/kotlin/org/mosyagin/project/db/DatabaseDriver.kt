package org.mosyagin.project.db

import app.cash.sqldelight.db.SqlDriver

// Это "объявление" (expect), которое говорит:
// "Платформы, каждая из вас должна написать свою реализацию этой функции"
expect fun createDriver(): SqlDriver