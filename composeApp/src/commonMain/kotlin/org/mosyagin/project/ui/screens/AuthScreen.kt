package org.mosyagin.project.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.mosyagin.project.repository.AuthRepository

class AuthScreenModel(private val authRepository: AuthRepository) : ScreenModel {
    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun onEmailChange(value: String) {
        _email.value = value
    }

    fun sendMagicLink() {
        screenModelScope.launch {
            _isLoading.value = true
            _message.value = null
            try {
                authRepository.sendMagicLink(_email.value)
                _message.value = "Ссылка для входа отправлена на почту!"
            } catch (e: Exception) {
                _message.value = "Ошибка: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}

class AuthScreen(private val onSkipAuth: () -> Unit) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = getScreenModel<AuthScreenModel>()
        val email by screenModel.email.collectAsState()
        val isLoading by screenModel.isLoading.collectAsState()
        val message by screenModel.message.collectAsState()

        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Вход в CineProp", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(32.dp))
                
                OutlinedTextField(
                    value = email,
                    onValueChange = { screenModel.onEmailChange(it) },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isLoading
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = { screenModel.sendMagicLink() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = email.isNotEmpty() && !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Отправить Magic Link")
                    }
                }
                
                message?.let {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(it, color = if (it.startsWith("Ошибка")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                }

                Spacer(modifier = Modifier.height(32.dp))
                
                // Исправлено: теперь вызывает переданный коллбек
                TextButton(onClick = { onSkipAuth() }) {
                    Text("Продолжить локально")
                }
            }

            // Версия внизу экрана
            Text(
                text = "version-0.0.1",
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                fontSize = 10.sp
            )
        }
    }
}
