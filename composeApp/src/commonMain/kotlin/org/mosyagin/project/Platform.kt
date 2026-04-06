package org.mosyagin.project

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

// Добавляем кросс-платформенную генерацию UUID
expect fun generateUUID(): String

// Глобальная функция для логирования синхронизации
expect fun logSync(message: String)
