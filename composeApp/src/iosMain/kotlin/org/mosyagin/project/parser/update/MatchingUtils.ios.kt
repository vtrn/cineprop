package org.mosyagin.project.parser.update

import kotlinx.cinterop.*
import platform.CoreCrypto.*

/**
 * Реализация SHA-256 для iOS с использованием CoreCrypto.
 */
@OptIn(ExperimentalForeignApi::class)
actual fun calculateSha256(input: String): String {
    val data = input.encodeToByteArray()
    val hash = ByteArray(CC_SHA256_DIGEST_LENGTH)

    data.usePinned { dataPinned ->
        hash.usePinned { hashPinned ->
            CC_SHA256(dataPinned.addressOf(0), data.size.toUInt(), hashPinned.addressOf(0).reinterpret())
        }
    }

    return hash.joinToString("") {
        (it.toInt() and 0xFF).toString(16).padStart(2, '0')
    }
}
