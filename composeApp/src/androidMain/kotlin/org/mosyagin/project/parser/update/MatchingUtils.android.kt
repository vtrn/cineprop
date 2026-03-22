package org.mosyagin.project.parser.update

import java.security.MessageDigest

/**
 * Реализация SHA-256 для Android.
 */
actual fun calculateSha256(input: String): String {
    val bytes = input.toByteArray()
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(bytes)
    return digest.fold("") { str, it -> str + "%02x".format(it) }
}
