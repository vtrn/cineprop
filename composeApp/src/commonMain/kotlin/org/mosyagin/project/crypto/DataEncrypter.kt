package org.mosyagin.project.crypto

import org.mosyagin.project.repository.SettingsRepository
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking

interface DataEncrypter {
    fun encrypt(data: String?): String?
    fun decrypt(data: String?): String?
}

/**
 * Провайдер ключей для шифрования. 
 * Должен сохранять ключ в безопасном хранилище платформы.
 */
expect interface KeyProvider {
    fun getEncryptionKey(): ByteArray
}

/**
 * Базовая реализация AES шифрования с поддержкой версионности и префиксов.
 */
class AesEncrypter(
    private val keyProvider: KeyProvider,
    private val settingsRepository: SettingsRepository
) : DataEncrypter {
    private val PREFIX = "enc:v1:"

    private fun isEncryptionEnabled(): Boolean {
        // Проверяем флаг в реальном времени. 
        // Использование runBlocking здесь допустимо, так как чтение из БД настроек обычно быстрое, 
        // а интерфейс DataEncrypter в текущей реализации синхронный.
        return runBlocking { settingsRepository.isEncryptionEnabled().firstOrNull() ?: false }
    }

    override fun encrypt(data: String?): String? {
        if (data == null || data.isEmpty() || !isEncryptionEnabled()) return data
        
        // Если данные уже зашифрованы, не шифруем повторно
        if (data.startsWith(PREFIX)) return data

        return try {
            val encrypted = platformEncrypt(data, keyProvider.getEncryptionKey())
            "$PREFIX$encrypted"
        } catch (e: Exception) {
            println("Encryption failed: ${e.message}")
            data
        }
    }

    override fun decrypt(data: String?): String? {
        if (data == null || data.isEmpty()) return data
        
        // Если нет префикса, значит это открытые данные
        if (!data.startsWith(PREFIX)) return data
        
        return try {
            val cipherText = data.removePrefix(PREFIX)
            platformDecrypt(cipherText, keyProvider.getEncryptionKey())
        } catch (e: Exception) {
            println("Decryption failed: ${e.message}")
            // Возвращаем как есть (хоть это и зашифрованная строка), 
            // чтобы не потерять данные безвозвратно при ошибке ключа
            data
        }
    }
}

class PlainDataEncrypter : DataEncrypter {
    override fun encrypt(data: String?): String? = data
    override fun decrypt(data: String?): String? = data
}

/**
 * Платформенно-зависимые функции шифрования
 */
expect fun platformEncrypt(data: String, key: ByteArray): String
expect fun platformDecrypt(cipherText: String, key: ByteArray): String
