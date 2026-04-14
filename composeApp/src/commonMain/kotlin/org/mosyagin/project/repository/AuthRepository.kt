package org.mosyagin.project.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.OTP
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.Apple
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.mosyagin.project.crypto.CryptoManager
import org.mosyagin.project.crypto.KeyVault
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.PBKDF2
import dev.whyoleg.cryptography.algorithms.SHA256
import dev.whyoleg.cryptography.BinarySize.Companion.bytes

interface AuthRepository {
    val currentUser: Flow<UserInfo?>
    val isEncryptionReady: StateFlow<Boolean> 
    
    suspend fun sendMagicLink(email: String)
    suspend fun signInWithPassword(email: String, password: String)
    suspend fun signUpWithPassword(email: String, password: String)
    suspend fun signInWithGoogle()
    suspend fun signInWithApple()
    suspend fun resetPassword(email: String)
    suspend fun signOut()
    fun getCurrentUserSync(): UserInfo?
    
    suspend fun createRecoveryBackup(pin: String): Boolean
    suspend fun restoreFromBackup(pin: String): Boolean
    suspend fun isBackupAvailable(): Boolean
    suspend fun generateNewKeys(pinForBackup: String? = null)
}

@OptIn(ExperimentalEncodingApi::class)
class AuthRepositoryImpl(
    private val supabase: SupabaseClient,
    private val cryptoManager: CryptoManager,
    private val keyVault: KeyVault,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) : AuthRepository {
    private val _currentUser = MutableStateFlow<UserInfo?>(null)
    override val currentUser: Flow<UserInfo?> = _currentUser.asStateFlow()

    private val _isEncryptionReady = MutableStateFlow(false)
    override val isEncryptionReady: StateFlow<Boolean> = _isEncryptionReady.asStateFlow()

    init {
        supabase.auth.sessionStatus.onEach { status ->
            when (status) {
                is SessionStatus.Authenticated -> {
                    val user = status.session.user ?: supabase.auth.currentUserOrNull()
                    if (user != null) {
                        _currentUser.value = user
                        checkLocalKeys(user.id)
                    }
                }
                else -> {
                    if (status !is SessionStatus.Initializing) {
                        _currentUser.value = null
                        _isEncryptionReady.value = false
                    }
                }
            }
        }.launchIn(scope)
    }

    private fun checkLocalKeys(userId: String) {
        scope.launch {
            val key = keyVault.loadPrivateKey(userId)
            println("DEBUG TEST 0: Checking local private key for user $userId. Found: ${key != null}")
            _isEncryptionReady.value = key != null
        }
    }

    private suspend fun deriveKeyFromPin(pin: String, email: String): ByteArray {
        val provider = CryptographyProvider.Default
        val pbkdf2 = provider.get(PBKDF2)
        val derivation = pbkdf2.secretDerivation(
            digest = SHA256,
            iterations = 100000,
            outputSize = 32.bytes,
            salt = email.encodeToByteArray()
        )
        return derivation.deriveSecretToByteArray(pin.encodeToByteArray())
    }

    override suspend fun generateNewKeys(pinForBackup: String?) {
        val user = getCurrentUserSync() ?: throw Exception("Пользователь не авторизован")
        val email = user.email ?: throw Exception("Email не найден")
        
        println("DEBUG TEST 0: Generating new RSA key pair for user ${user.id}")
        val (pub, priv) = cryptoManager.generateUserKeyPair()
        
        // ЗДЕСЬ ЗАПОЛНЯЕТСЯ ТАБЛИЦА ПУБЛИЧНЫХ КЛЮЧЕЙ (Добавлен email)
        supabase.postgrest["user_public_keys"].upsert(mapOf(
            "user_id" to user.id,
            "email" to email,
            "public_key" to Base64.encode(pub)
        ))
        
        keyVault.savePrivateKey(user.id, priv)

        if (pinForBackup != null) {
            withContext(NonCancellable) {
                createRecoveryBackup(pinForBackup)
            }
        }
        
        _isEncryptionReady.value = true
    }

    override suspend fun createRecoveryBackup(pin: String): Boolean = withContext(NonCancellable) {
        val user = getCurrentUserSync() ?: return@withContext false
        val email = user.email ?: return@withContext false
        val privateKey = keyVault.loadPrivateKey(user.id) ?: return@withContext false
        
        return@withContext try {
            val pinKey = deriveKeyFromPin(pin, email)
            val encryptedKey = cryptoManager.encryptText(Base64.encode(privateKey), pinKey)
            
            supabase.postgrest["user_key_backups"].upsert(mapOf(
                "user_id" to user.id,
                "encrypted_private_key" to encryptedKey
            ))
            println("DEBUG TEST 0: Recovery backup created successfully")
            true
        } catch (e: Exception) {
            println("DEBUG TEST 0: Failed to create recovery backup: ${e.message}")
            false
        }
    }

    override suspend fun restoreFromBackup(pin: String): Boolean {
        val user = getCurrentUserSync() ?: return false
        val email = user.email ?: return false
        
        return try {
            val response = supabase.postgrest["user_key_backups"]
                .select { filter { eq("user_id", user.id) } }
                .decodeSingleOrNull<Map<String, String>>() ?: return false
            
            val encryptedKey = response["encrypted_private_key"] ?: return false
            val pinKey = deriveKeyFromPin(pin, email)
            
            val decryptedBase64 = cryptoManager.decryptText(encryptedKey, pinKey)
            if (decryptedBase64 == "Ошибка расшифровки") return false
            
            val privateKeyBytes = Base64.decode(decryptedBase64)
            keyVault.savePrivateKey(user.id, privateKeyBytes)
            _isEncryptionReady.value = true
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun isBackupAvailable(): Boolean {
        val user = getCurrentUserSync() ?: return false
        return try {
            val result = supabase.postgrest["user_key_backups"]
                .select { filter { eq("user_id", user.id) } }
                .decodeSingleOrNull<Map<String, String>>()
            result != null
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun sendMagicLink(email: String) {
        supabase.auth.signInWith(OTP, redirectUrl = "cineprop://auth") { this.email = email }
    }
    override suspend fun signInWithPassword(email: String, password: String) {
        supabase.auth.signInWith(Email) { this.email = email; this.password = password }
        supabase.auth.currentUserOrNull()?.let { _currentUser.value = it }
    }
    override suspend fun signUpWithPassword(email: String, password: String) {
        supabase.auth.signUpWith(Email) { this.email = email; this.password = password }
    }
    override suspend fun signInWithGoogle() { supabase.auth.signInWith(Google, redirectUrl = "cineprop://auth") }
    override suspend fun signInWithApple() { supabase.auth.signInWith(Apple, redirectUrl = "cineprop://auth") }
    override suspend fun resetPassword(email: String) { supabase.auth.resetPasswordForEmail(email, redirectUrl = "cineprop://auth") }
    override suspend fun signOut() { 
        supabase.auth.signOut()
        _currentUser.value = null
        _isEncryptionReady.value = false 
    }
    override fun getCurrentUserSync(): UserInfo? = supabase.auth.currentUserOrNull()
}
