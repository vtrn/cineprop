package org.mosyagin.project.crypto

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

actual interface KeyProvider {
    actual fun getEncryptionKey(): ByteArray
}

class AndroidKeyProvider(private val context: Context) : KeyProvider {
    private val KEY_ALIAS = "cineapp_master_key"
    private val ANDROID_KEYSTORE = "AndroidKeyStore"

    override fun getEncryptionKey(): ByteArray {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
            
            keyGenerator.init(spec)
            keyGenerator.generateKey()
        }
        
        // В AndroidKeyStore мы не можем получить сырые байты AES ключа напрямую (это безопасно)
        // Но для нашего AesEncrypter в commonMain нужны байты. 
        // Поэтому на Android мы будем использовать Cipher внутри actual функций, 
        // а KeyProvider будет просто меткой или хранить контекст.
        return ByteArray(0) 
    }
}

actual fun platformEncrypt(data: String, key: ByteArray): String {
    val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    val secretKey = keyStore.getKey("cineapp_master_key", null) as SecretKey
    
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE, secretKey)
    
    val iv = cipher.iv
    val encryptedBytes = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
    
    // Склеиваем IV и зашифрованные данные
    val combined = iv + encryptedBytes
    return Base64.encodeToString(combined, Base64.NO_WRAP)
}

actual fun platformDecrypt(cipherText: String, key: ByteArray): String {
    val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    val secretKey = keyStore.getKey("cineapp_master_key", null) as SecretKey
    
    val combined = Base64.decode(cipherText, Base64.NO_WRAP)
    val iv = combined.sliceArray(0 until 12)
    val encryptedBytes = combined.sliceArray(12 until combined.size)
    
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    val spec = GCMParameterSpec(128, iv)
    cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
    
    val decryptedBytes = cipher.doFinal(encryptedBytes)
    return String(decryptedBytes, Charsets.UTF_8)
}
