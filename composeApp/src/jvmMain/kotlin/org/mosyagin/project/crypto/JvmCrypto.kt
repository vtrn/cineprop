package org.mosyagin.project.crypto

import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.security.SecureRandom

actual interface KeyProvider {
    actual fun getEncryptionKey(): ByteArray
}

class JvmKeyProvider : KeyProvider {
    private val keyFile = java.io.File(System.getProperty("user.home"), ".cineapp/master.key")
    
    override fun getEncryptionKey(): ByteArray {
        if (!keyFile.exists()) {
            val key = ByteArray(32) // 256 bit
            SecureRandom().nextBytes(key)
            keyFile.parentFile.mkdirs()
            keyFile.writeBytes(key)
        }
        return keyFile.readBytes()
    }
}

actual fun platformEncrypt(data: String, key: ByteArray): String {
    val secretKey = SecretKeySpec(key, "AES")
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    
    val iv = ByteArray(12)
    SecureRandom().nextBytes(iv)
    
    val spec = GCMParameterSpec(128, iv)
    cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)
    
    val encryptedBytes = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
    
    val combined = iv + encryptedBytes
    return Base64.getEncoder().encodeToString(combined)
}

actual fun platformDecrypt(cipherText: String, key: ByteArray): String {
    val secretKey = SecretKeySpec(key, "AES")
    val combined = Base64.getDecoder().decode(cipherText)
    
    val iv = combined.sliceArray(0 until 12)
    val encryptedBytes = combined.sliceArray(12 until combined.size)
    
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    val spec = GCMParameterSpec(128, iv)
    cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
    
    val decryptedBytes = cipher.doFinal(encryptedBytes)
    return String(decryptedBytes, Charsets.UTF_8)
}
