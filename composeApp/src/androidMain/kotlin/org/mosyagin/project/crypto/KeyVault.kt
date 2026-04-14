package org.mosyagin.project.crypto

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
actual class KeyVault(private val context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPrefs = EncryptedSharedPreferences.create(
        context,
        "secure_keys_v1",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    actual suspend fun savePrivateKey(userId: String, privateKeyBytes: ByteArray) {
        sharedPrefs.edit().putString("priv_key_$userId", Base64.encode(privateKeyBytes)).apply()
    }

    actual suspend fun loadPrivateKey(userId: String): ByteArray? {
        val encoded = sharedPrefs.getString("priv_key_$userId", null) ?: return null
        return Base64.decode(encoded)
    }

    actual suspend fun saveMasterKey(projectId: String, masterKeyBytes: ByteArray) {
        sharedPrefs.edit().putString("proj_key_$projectId", Base64.encode(masterKeyBytes)).apply()
    }

    actual suspend fun loadMasterKey(projectId: String): ByteArray? {
        val encoded = sharedPrefs.getString("proj_key_$projectId", null) ?: return null
        return Base64.decode(encoded)
    }

    actual suspend fun deleteProjectKey(projectId: String) {
        sharedPrefs.edit().remove("proj_key_$projectId").apply()
    }

    actual suspend fun clearAll() {
        sharedPrefs.edit().clear().apply()
    }
}
