package org.mosyagin.project.data.repository

import org.mosyagin.project.crypto.KeyVault

actual fun createTestKeyVault(): KeyVault = KeyVault()
