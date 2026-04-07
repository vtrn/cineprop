package org.mosyagin.project.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.OTP
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.Apple
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

interface AuthRepository {
    val currentUser: Flow<UserInfo?>
    suspend fun sendMagicLink(email: String)
    suspend fun signInWithPassword(email: String, password: String)
    suspend fun signUpWithPassword(email: String, password: String)
    suspend fun signInWithGoogle()
    suspend fun signInWithApple()
    suspend fun resetPassword(email: String)
    suspend fun signOut()
    fun getCurrentUserSync(): UserInfo?
}

class AuthRepositoryImpl(
    private val supabase: SupabaseClient,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) : AuthRepository {
    private val _currentUser = MutableStateFlow<UserInfo?>(null)
    override val currentUser: Flow<UserInfo?> = _currentUser.asStateFlow()

    init {
        supabase.auth.sessionStatus.onEach { status ->
            when (status) {
                is SessionStatus.Authenticated -> {
                    val user = status.session.user ?: supabase.auth.currentUserOrNull()
                    if (user != null) {
                        _currentUser.value = user
                    } else {
                        scope.launch {
                            try {
                                val fetchedUser = supabase.auth.retrieveUserForCurrentSession()
                                _currentUser.value = fetchedUser
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
                else -> {
                    _currentUser.value = null
                }
            }
        }.launchIn(scope)
    }

    override suspend fun sendMagicLink(email: String) {
        supabase.auth.signInWith(OTP, redirectUrl = "cineprop://auth") {
            this.email = email
        }
    }

    override suspend fun signInWithPassword(email: String, password: String) {
        supabase.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    override suspend fun signUpWithPassword(email: String, password: String) {
        supabase.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
    }

    override suspend fun signInWithGoogle() {
        supabase.auth.signInWith(Google, redirectUrl = "cineprop://auth")
    }

    override suspend fun signInWithApple() {
        supabase.auth.signInWith(Apple, redirectUrl = "cineprop://auth")
    }

    override suspend fun resetPassword(email: String) {
        supabase.auth.resetPasswordForEmail(email, redirectUrl = "cineprop://auth")
    }

    override suspend fun signOut() {
        supabase.auth.signOut()
    }

    override fun getCurrentUserSync(): UserInfo? = supabase.auth.currentUserOrNull()
}
