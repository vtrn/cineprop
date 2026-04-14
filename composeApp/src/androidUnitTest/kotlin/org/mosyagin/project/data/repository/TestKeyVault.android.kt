package org.mosyagin.project.data.repository

import org.mosyagin.project.crypto.KeyVault
import androidx.test.core.app.ApplicationProvider

actual fun createTestKeyVault(): KeyVault = KeyVault(ApplicationProvider.getApplicationContext())
