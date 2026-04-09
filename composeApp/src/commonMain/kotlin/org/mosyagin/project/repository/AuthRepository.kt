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
            println("AuthRepository: Session status changed to $status")
            when (status) {
                is SessionStatus.Authenticated -> {
                    val user = status.session.user ?: supabase.auth.currentUserOrNull()
                    if (user != null) {
                        println("AuthRepository: User found in status: ${user.email}")
                        _currentUser.value = user
                    } else {
                        println("AuthRepository: Authenticated but no user info. Fetching manually...")
                        scope.launch {
                            try {
                                val fetchedUser = supabase.auth.retrieveUserForCurrentSession()
                                println("AuthRepository: User fetched manually: ${fetchedUser.email}")
                                _currentUser.value = fetchedUser
                            } catch (e: Exception) {
                                println("AuthRepository: Manual fetch failed: ${e.message}")
                            }
                        }
                    }
                }
                else -> {
                    if (status !is SessionStatus.Initializing) {
                        println("AuthRepository: Status is $status, setting user to null")
                        _currentUser.value = null
                    }
                }
            }
        }.launchIn(scope)
    }

    override suspend fun sendMagicLink(email: String) {
        println("AuthRepository: Sending magic link to $email")
        supabase.auth.signInWith(OTP, redirectUrl = "cineprop://auth") {
            this.email = email
        }
    }

    override suspend fun signInWithPassword(email: String, password: String) {
        println("AuthRepository: Attempting sign in with password for $email")
        supabase.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
        // Принудительно проверяем пользователя сразу после успешного вызова
        val user = supabase.auth.currentUserOrNull()
        println("AuthRepository: Post-login user check: ${user?.email}")
        if (user != null) _currentUser.value = user
    }

    override suspend fun signUpWithPassword(email: String, password: String) {
        println("AuthRepository: Attempting sign up for $email")
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
        println("AuthRepository: Signing out...")
        supabase.auth.signOut()
        _currentUser.value = null
    }

    override fun getCurrentUserSync(): UserInfo? = supabase.auth.currentUserOrNull()
}
