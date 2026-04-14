package org.mosyagin.project.ui.screens

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import org.mosyagin.project.repository.AuthRepository

sealed class CryptoSetupState {
    object Checking : CryptoSetupState()
    object CreatePin : CryptoSetupState()
    object RestorePin : CryptoSetupState()
    object Success : CryptoSetupState()
    data class Error(val message: String) : CryptoSetupState()
}

class CryptoSetupViewModel(
    private val authRepository: AuthRepository
) : ScreenModel {

    private val _state = MutableStateFlow<CryptoSetupState>(CryptoSetupState.Checking)
    val state: StateFlow<CryptoSetupState> = _state.asStateFlow()

    init {
        checkBackupStatus()
    }

    private fun checkBackupStatus() {
        screenModelScope.launch {
            _state.value = CryptoSetupState.Checking
            try {
                if (authRepository.isBackupAvailable()) {
                    _state.value = CryptoSetupState.RestorePin
                } else {
                    _state.value = CryptoSetupState.CreatePin
                }
            } catch (e: Exception) {
                _state.value = CryptoSetupState.Error("Ошибка проверки облачного бэкапа. Проверьте интернет или сессию.")
            }
        }
    }

    fun handlePinAction(pin: String) {
        screenModelScope.launch {
            val currentState = _state.value
            _state.value = CryptoSetupState.Checking
            
            try {
                val success = if (currentState is CryptoSetupState.CreatePin) {
                    // Генерируем ключи (может упасть, если сессия битая)
                    authRepository.generateNewKeys()
                    // Создаем бэкап
                    authRepository.createRecoveryBackup(pin)
                } else {
                    authRepository.restoreFromBackup(pin)
                }

                if (success) {
                    _state.value = CryptoSetupState.Success
                } else {
                    _state.value = CryptoSetupState.Error("Не удалось выполнить операцию. Проверьте PIN.")
                    delay(2000)
                    checkBackupStatus()
                }
            } catch (e: Exception) {
                // Если сессия недействительна (Foreign Key Error), показываем критическую ошибку
                _state.value = CryptoSetupState.Error("Ошибка сессии: ${e.message}. Попробуйте перезайти в аккаунт.")
            }
        }
    }

    fun signOut() {
        screenModelScope.launch {
            authRepository.signOut()
        }
    }
}
