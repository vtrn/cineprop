package org.mosyagin.project.ui.screens

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.mosyagin.project.repository.SettingsRepository

class SettingsViewModel(
    private val repository: SettingsRepository
) : ScreenModel {
    
    val themeMode: StateFlow<String> = repository.getThemeMode()
        .stateIn(screenModelScope, SharingStarted.Eagerly, "system")

    val isEncryptionEnabled: StateFlow<Boolean> = repository.isEncryptionEnabled()
        .stateIn(screenModelScope, SharingStarted.Eagerly, false)

    val isCloudKeySyncEnabled: StateFlow<Boolean> = repository.isCloudKeySyncEnabled()
        .stateIn(screenModelScope, SharingStarted.Eagerly, true)

    val isRecoveryPinEnabled: StateFlow<Boolean> = repository.isRecoveryPinEnabled()
        .stateIn(screenModelScope, SharingStarted.Eagerly, false)

    fun setThemeMode(mode: String) {
        screenModelScope.launch {
            repository.setThemeMode(mode)
        }
    }

    fun setEncryptionEnabled(enabled: Boolean) {
        screenModelScope.launch {
            repository.setEncryptionEnabled(enabled)
        }
    }

    fun setCloudKeySyncEnabled(enabled: Boolean) {
        screenModelScope.launch {
            repository.setCloudKeySyncEnabled(enabled)
        }
    }

    fun setRecoveryPinEnabled(enabled: Boolean) {
        screenModelScope.launch {
            repository.setRecoveryPinEnabled(enabled)
        }
    }
}
