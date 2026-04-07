package org.mosyagin.project.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.OTP
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
        // Подписываемся на изменения состояния сессии
        supabase.auth.sessionStatus.onEach { status ->
            println("AuthRepository: Session status changed to $status")
            
            when (status) {
                is SessionStatus.Authenticated -> {
                    // Пытаемся взять пользователя из сессии
                    val user = status.session.user ?: supabase.auth.currentUserOrNull()
                    
                    if (user != null) {
                        println("AuthRepository: User found: ${user.email}")
                        _currentUser.value = user
                    } else {
                        // Если статус Authenticated, но пользователя нет - запрашиваем его
                        println("AuthRepository: Authenticated but no user info. Fetching...")
                        scope.launch {
                            try {
                                val fetchedUser = supabase.auth.retrieveUserForCurrentSession()
                                println("AuthRepository: User fetched successfully: ${fetchedUser.email}")
                                _currentUser.value = fetchedUser
                            } catch (e: Exception) {
                                println("AuthRepository: Failed to fetch user: ${e.message}")
                            }
                        }
                    }
                }
                else -> {
                    println("AuthRepository: Not authenticated. Setting user to null")
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

    override suspend fun signOut() {
        supabase.auth.signOut()
    }

    override fun getCurrentUserSync(): UserInfo? = supabase.auth.currentUserOrNull()
}
