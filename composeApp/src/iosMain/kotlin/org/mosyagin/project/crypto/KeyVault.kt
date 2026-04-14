@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.mosyagin.project.crypto

import platform.Foundation.*
import platform.Security.*
import kotlinx.cinterop.*
import platform.CoreFoundation.*

actual class KeyVault {

    actual suspend fun savePrivateKey(userId: String, privateKeyBytes: ByteArray) {
        saveToKeychain("priv_key_$userId", privateKeyBytes)
    }

    actual suspend fun loadPrivateKey(userId: String): ByteArray? {
        return loadFromKeychain("priv_key_$userId")
    }

    actual suspend fun saveMasterKey(projectId: String, masterKeyBytes: ByteArray) {
        saveToKeychain("proj_key_$projectId", masterKeyBytes)
    }

    actual suspend fun loadMasterKey(projectId: String): ByteArray? {
        return loadFromKeychain("proj_key_$projectId")
    }

    actual suspend fun deleteProjectKey(projectId: String) {
        deleteFromKeychain("proj_key_$projectId")
    }

    actual suspend fun clearAll() {
        val query = CFDictionaryCreateMutable(null, 1, null, null)
        CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
        SecItemDelete(query)
    }

    private fun saveToKeychain(key: String, data: ByteArray) {
        val nsData = data.toNSData()
        deleteFromKeychain(key)

        val query = CFDictionaryCreateMutable(null, 4, null, null)
        CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
        CFDictionaryAddValue(query, kSecAttrAccount, CFBridgingRetain(key))
        CFDictionaryAddValue(query, kSecValueData, CFBridgingRetain(nsData))
        CFDictionaryAddValue(query, kSecAttrSynchronizable, kCFBooleanTrue)

        val status = SecItemAdd(query, null)
        if (status != errSecSuccess) {
            println("KeyVault iOS: Error saving key $key, status: $status")
        }
    }

    private fun loadFromKeychain(key: String): ByteArray? {
        val query = CFDictionaryCreateMutable(null, 5, null, null)
        CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
        CFDictionaryAddValue(query, kSecAttrAccount, CFBridgingRetain(key))
        CFDictionaryAddValue(query, kSecReturnData, kCFBooleanTrue)
        CFDictionaryAddValue(query, kSecMatchLimit, kSecMatchLimitOne)
        CFDictionaryAddValue(query, kSecAttrSynchronizable, kCFBooleanTrue)

        return memScoped {
            val resultPtr = alloc<CFTypeRefVar>()
            val status = SecItemCopyMatching(query, resultPtr.ptr)
            if (status == errSecSuccess) {
                val nsData = CFBridgingRelease(resultPtr.value) as? NSData
                nsData?.toByteArray()
            } else {
                null
            }
        }
    }

    private fun deleteFromKeychain(key: String) {
        val query = CFDictionaryCreateMutable(null, 3, null, null)
        CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
        CFDictionaryAddValue(query, kSecAttrAccount, CFBridgingRetain(key))
        CFDictionaryAddValue(query, kSecAttrSynchronizable, kCFBooleanTrue)
        SecItemDelete(query)
    }

    private fun ByteArray.toNSData(): NSData = if (isEmpty()) NSData() else {
        val pinned = pin()
        NSData.dataWithBytes(pinned.addressOf(0), size.toULong()).also { pinned.unpin() }
    }

    private fun NSData.toByteArray(): ByteArray {
        val length = this.length.toInt()
        val bytes = this.bytes ?: return byteArrayOf()
        val byteArray = ByteArray(length)
        byteArray.usePinned { pinned ->
            platform.posix.memcpy(pinned.addressOf(0), bytes, this.length)
        }
        return byteArray
    }
}
