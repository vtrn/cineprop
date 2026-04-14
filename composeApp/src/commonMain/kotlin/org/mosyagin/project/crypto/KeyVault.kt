package org.mosyagin.project.crypto

/**
 * KeyVault — кроссплатформенное хранилище для секретных ключей.
 * Хранит:
 * 1. Приватный RSA ключ пользователя (защищен системными механизмами).
 * 2. Мастер-ключи проектов (AES).
 */
expect class KeyVault {
    /**
     * Сохраняет приватный ключ пользователя.
     */
    suspend fun savePrivateKey(userId: String, privateKeyBytes: ByteArray)

    /**
     * Загружает приватный ключ пользователя.
     */
    suspend fun loadPrivateKey(userId: String): ByteArray?

    /**
     * Сохраняет мастер-ключ проекта.
     */
    suspend fun saveMasterKey(projectId: String, masterKeyBytes: ByteArray)

    /**
     * Загружает мастер-ключ проекта.
     */
    suspend fun loadMasterKey(projectId: String): ByteArray?

    /**
     * Удаляет ключ проекта (например, при выходе из него).
     */
    suspend fun deleteProjectKey(projectId: String)

    /**
     * Очищает все ключи пользователя.
     */
    suspend fun clearAll()
}
