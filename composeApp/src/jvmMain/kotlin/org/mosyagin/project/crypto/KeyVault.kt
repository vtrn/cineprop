package org.mosyagin.project.crypto

import java.io.File
import java.util.Properties
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import javax.crypto.spec.GCMParameterSpec
import java.security.SecureRandom
import java.security.MessageDigest
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
actual class KeyVault {
    private val vaultDir = File(System.getProperty("user.home"), ".cineapp")
    private val vaultFile = File(vaultDir, "vault.dat")
    private val properties = Properties()

    init {
        if (!vaultDir.exists()) vaultDir.mkdirs()
        if (vaultFile.exists()) {
            loadVault()
        }
    }

    /**
     * Загружает и расшифровывает файл хранилища.
     */
    private fun loadVault() {
        try {
            val encryptedBytes = vaultFile.readBytes()
            if (encryptedBytes.isEmpty()) return
            
            val decryptedBytes = decryptData(encryptedBytes)
            properties.load(decryptedBytes.inputStream())
        } catch (e: Exception) {
            println("KeyVault JVM: Ошибка загрузки хранилища (возможно, изменился HWID): ${e.message}")
        }
    }

    /**
     * Зашифровывает и сохраняет файл хранилища.
     */
    private fun saveVault() {
        try {
            val output = java.io.ByteArrayOutputStream()
            properties.store(output, "CineProp Secure Vault")
            val encryptedBytes = encryptData(output.toByteArray())
            vaultFile.writeBytes(encryptedBytes)
        } catch (e: Exception) {
            println("KeyVault JVM: Ошибка сохранения хранилища: ${e.message}")
        }
    }

    /**
     * Генерирует ключ на основе системных данных (Fingerprint).
     */
    private fun getSystemKey(): SecretKeySpec {
        val fingerprint = System.getProperty("os.name") + 
                          System.getProperty("user.name") + 
                          System.getProperty("os.arch")
        val hash = MessageDigest.getInstance("SHA-256").digest(fingerprint.toByteArray())
        return SecretKeySpec(hash, "AES")
    }

    private fun encryptData(data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = ByteArray(12).apply { SecureRandom().nextBytes(this) }
        cipher.init(Cipher.ENCRYPT_MODE, getSystemKey(), GCMParameterSpec(128, iv))
        val encrypted = cipher.doFinal(data)
        return iv + encrypted // Сохраняем IV вместе с данными
    }

    private fun decryptData(data: ByteArray): ByteArray {
        val iv = data.sliceArray(0 until 12)
        val encrypted = data.sliceArray(12 until data.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, getSystemKey(), GCMParameterSpec(128, iv))
        return cipher.doFinal(encrypted)
    }

    // --- Реализация интерфейса KeyVault ---

    actual suspend fun savePrivateKey(userId: String, privateKeyBytes: ByteArray) {
        properties.setProperty("priv_key_$userId", Base64.encode(privateKeyBytes))
        saveVault()
    }

    actual suspend fun loadPrivateKey(userId: String): ByteArray? {
        val encoded = properties.getProperty("priv_key_$userId") ?: return null
        return try { Base64.decode(encoded) } catch (e: Exception) { null }
    }

    actual suspend fun saveMasterKey(projectId: String, masterKeyBytes: ByteArray) {
        properties.setProperty("proj_key_$projectId", Base64.encode(masterKeyBytes))
        saveVault()
    }

    actual suspend fun loadMasterKey(projectId: String): ByteArray? {
        val encoded = properties.getProperty("proj_key_$projectId") ?: return null
        return try { Base64.decode(encoded) } catch (e: Exception) { null }
    }

    actual suspend fun deleteProjectKey(projectId: String) {
        properties.remove("proj_key_$projectId")
        saveVault()
    }

    actual suspend fun clearAll() {
        properties.clear()
        if (vaultFile.exists()) vaultFile.delete()
    }
}
