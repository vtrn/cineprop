package org.mosyagin.project.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.mosyagin.project.DatabaseQueries


interface SettingsRepository {
    fun getThemeMode(): Flow<String>
    suspend fun setThemeMode(mode: String)
    
    fun isEncryptionEnabled(): Flow<Boolean>
    suspend fun setEncryptionEnabled(enabled: Boolean)

    fun isCloudKeySyncEnabled(): Flow<Boolean>
    suspend fun setCloudKeySyncEnabled(enabled: Boolean)

    fun isRecoveryPinEnabled(): Flow<Boolean>
    suspend fun setRecoveryPinEnabled(enabled: Boolean)
}

class SettingsRepositoryImpl(private val queries: DatabaseQueries) : SettingsRepository {
    override fun getThemeMode(): Flow<String> {
        return queries.getSetting("theme_mode")
            .asFlow()
            .mapToOneOrNull<String>(Dispatchers.IO)
            .map { it ?: "system" }
    }

    override suspend fun setThemeMode(mode: String) {
        queries.upsertSetting("theme_mode", mode)
    }

    override fun isEncryptionEnabled(): Flow<Boolean> {
        return queries.getSetting("encryption_enabled")
            .asFlow()
            .mapToOneOrNull<String>(Dispatchers.IO)
            .map { it == "true" }
    }

    override suspend fun setEncryptionEnabled(enabled: Boolean) {
        queries.upsertSetting("encryption_enabled", enabled.toString())
    }

    override fun isCloudKeySyncEnabled(): Flow<Boolean> {
        return queries.getSetting("cloud_key_sync_enabled")
            .asFlow()
            .mapToOneOrNull<String>(Dispatchers.IO)
            .map { it == "true" }
    }

    override suspend fun setCloudKeySyncEnabled(enabled: Boolean) {
        queries.upsertSetting("cloud_key_sync_enabled", enabled.toString())
    }

    override fun isRecoveryPinEnabled(): Flow<Boolean> {
        return queries.getSetting("recovery_pin_enabled")
            .asFlow()
            .mapToOneOrNull<String>(Dispatchers.IO)
            .map { it == "true" }
    }

    override suspend fun setRecoveryPinEnabled(enabled: Boolean) {
        queries.upsertSetting("recovery_pin_enabled", enabled.toString())
    }
}
