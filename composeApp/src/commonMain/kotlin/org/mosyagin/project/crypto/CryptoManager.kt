package org.mosyagin.project.crypto

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.BinarySize.Companion.bits
import dev.whyoleg.cryptography.algorithms.AES
import dev.whyoleg.cryptography.algorithms.RSA
import dev.whyoleg.cryptography.algorithms.SHA256
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * CryptoManager — центральный класс для выполнения криптографических операций.
 * Версия 0.6.0 библиотеки dev.whyoleg.cryptography.
 */
@OptIn(ExperimentalEncodingApi::class)
class CryptoManager {
    private val provider = CryptographyProvider.Default
    
    // Алгоритмы
    private val aesGcm = provider.get(AES.GCM)
    private val rsaOaep = provider.get(RSA.OAEP)

    /**
     * Генерирует новый симметричный ключ AES-256 для проекта.
     */
    suspend fun generateProjectMasterKey(): ByteArray {
        val keyGenerator = aesGcm.keyGenerator(256.bits)
        val key = keyGenerator.generateKey()
        return key.encodeToByteArray(AES.Key.Format.RAW)
    }

    /**
     * Генерирует новую пару RSA-2048 ключей для пользователя.
     * @return Pair(Public Key Bytes, Private Key Bytes)
     */
    suspend fun generateUserKeyPair(): Pair<ByteArray, ByteArray> {
        // RSA OAEP требует указания digest (SHA256) при генерации
        val keyPairGenerator = rsaOaep.keyPairGenerator(2048.bits, SHA256)
        val keyPair = keyPairGenerator.generateKey()
        val publicKey = keyPair.publicKey.encodeToByteArray(RSA.PublicKey.Format.DER)
        val privateKey = keyPair.privateKey.encodeToByteArray(RSA.PrivateKey.Format.DER)
        return Pair(publicKey, privateKey)
    }

    /**
     * "Заворачивает" симметричный ключ проекта публичным ключом участника.
     */
    suspend fun wrapKey(masterKeyBytes: ByteArray, publicKeyBytes: ByteArray): ByteArray {
        // Декодируем публичный ключ
        val publicKey = rsaOaep.publicKeyDecoder(SHA256).decodeFromByteArray(RSA.PublicKey.Format.DER, publicKeyBytes)
        // В 0.6.0 для OAEP используем метод encryptor() у ключа
        val encryptor = publicKey.encryptor()
        return encryptor.encrypt(masterKeyBytes)
    }

    /**
     * "Разворачивает" симметричный ключ проекта приватным ключом пользователя.
     */
    suspend fun unwrapKey(wrappedKey: ByteArray, privateKeyBytes: ByteArray): ByteArray {
        // Декодируем приватный ключ
        val privateKey = rsaOaep.privateKeyDecoder(SHA256).decodeFromByteArray(RSA.PrivateKey.Format.DER, privateKeyBytes)
        // В 0.6.0 для OAEP используем метод decryptor() у ключа
        val decryptor = privateKey.decryptor()
        return decryptor.decrypt(wrappedKey)
    }

    /**
     * Шифрует текст с использованием мастер-ключа проекта.
     */
    suspend fun encryptText(text: String, masterKeyBytes: ByteArray): String {
        if (text.isEmpty()) return ""
        // Декодируем AES ключ
        val key = aesGcm.keyDecoder().decodeFromByteArray(AES.Key.Format.RAW, masterKeyBytes)
        // В 0.6.0 для AES GCM используем метод cipher() у ключа
        val cipher = key.cipher()
        val ciphertext = cipher.encrypt(text.encodeToByteArray())
        return Base64.encode(ciphertext)
    }

    /**
     * Расшифровывает Base64 строку мастер-ключом проекта.
     */
    suspend fun decryptText(base64Ciphertext: String, masterKeyBytes: ByteArray): String {
        if (base64Ciphertext.isEmpty()) return ""
        return try {
            val key = aesGcm.keyDecoder().decodeFromByteArray(AES.Key.Format.RAW, masterKeyBytes)
            val cipher = key.cipher()
            val ciphertext = Base64.decode(base64Ciphertext)
            val plaintext = cipher.decrypt(ciphertext)
            plaintext.decodeToString()
        } catch (e: Exception) {
            "Ошибка расшифровки"
        }
    }
}
