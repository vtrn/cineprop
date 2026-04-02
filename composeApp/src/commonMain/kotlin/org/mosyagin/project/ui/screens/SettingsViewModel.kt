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

    fun setThemeMode(mode: String) {
        screenModelScope.launch {
            repository.setThemeMode(mode)
        }
    }
}
