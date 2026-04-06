package org.mosyagin.project

import java.util.UUID

class JVMPlatform : Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
}

actual fun getPlatform(): Platform = JVMPlatform()

// Реализация для Desktop (JVM)
actual fun generateUUID(): String = UUID.randomUUID().toString()

// Реализация логирования для Desktop
actual fun logSync(message: String) {
    println("SyncLog: $message")
}
