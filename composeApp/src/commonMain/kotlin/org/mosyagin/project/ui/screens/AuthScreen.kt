package org.mosyagin.project.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
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

enum class AuthMode {
    LOGIN, SIGN_UP
}

class AuthScreenModel(private val authRepository: AuthRepository) : ScreenModel {
    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _authMode = MutableStateFlow(AuthMode.LOGIN)
    val authMode: StateFlow<AuthMode> = _authMode.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun onEmailChange(value: String) { _email.value = value }
    fun onPasswordChange(value: String) { _password.value = value }
    fun setAuthMode(mode: AuthMode) {
        _authMode.value = mode
        _message.value = null
    }

    fun handleAuth() {
        screenModelScope.launch {
            _isLoading.value = true
            _message.value = null
            try {
                if (_authMode.value == AuthMode.LOGIN) {
                    if (_password.value.isEmpty()) {
                        authRepository.sendMagicLink(_email.value)
                        _message.value = "Magic Link отправлен на почту!"
                    } else {
                        authRepository.signInWithPassword(_email.value, _password.value)
                    }
                } else {
                    authRepository.signUpWithPassword(_email.value, _password.value)
                    _message.value = "Регистрация успешна! Проверьте почту для подтверждения."
                }
            } catch (e: Exception) {
                _message.value = "Ошибка: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun forgotPassword() {
        screenModelScope.launch {
            if (_email.value.isEmpty()) {
                _message.value = "Введите email для сброса пароля"
                return@launch
            }
            _isLoading.value = true
            _message.value = null
            try {
                authRepository.resetPassword(_email.value)
                _message.value = "Ссылка для сброса отправлена на почту!"
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
        val screenModel = getScreenModel<AuthScreenModel>()
        val email by screenModel.email.collectAsState()
        val password by screenModel.password.collectAsState()
        val authMode by screenModel.authMode.collectAsState()
        val isLoading by screenModel.isLoading.collectAsState()
        val message by screenModel.message.collectAsState()
        var passwordVisible by remember { mutableStateOf(false) }

        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Column(
                modifier = Modifier
                    .widthIn(max = 450.dp)
                    .align(Alignment.Center)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Вкладки Login / Sign Up
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(4.dp)
                ) {
                    AuthTab(
                        text = "Login",
                        selected = authMode == AuthMode.LOGIN,
                        onClick = { screenModel.setAuthMode(AuthMode.LOGIN) },
                        modifier = Modifier.weight(1f)
                    )
                    AuthTab(
                        text = "Sign Up",
                        selected = authMode == AuthMode.SIGN_UP,
                        onClick = { screenModel.setAuthMode(AuthMode.SIGN_UP) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = if (authMode == AuthMode.LOGIN) "Вход в CineProp" else "Создать аккаунт",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { screenModel.onEmailChange(it) },
                    label = { Text("Email address") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    enabled = !isLoading
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Контейнер для пароля с заголовком и кнопкой "Forgot"
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Password",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (authMode == AuthMode.LOGIN) {
                            Text(
                                text = "Forgot password?",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable(enabled = !isLoading) { screenModel.forgotPassword() }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { screenModel.onPasswordChange(it) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        enabled = !isLoading,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(image, contentDescription = null)
                            }
                        }
                    )
                }

                if (authMode == AuthMode.LOGIN && password.isEmpty()) {
                    Text(
                        text = "Оставьте пароль пустым для входа по Magic Link",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 8.dp).align(Alignment.Start)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { screenModel.handleAuth() },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = email.isNotEmpty() && !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    } else {
                        Text(if (authMode == AuthMode.LOGIN) "Log In" else "Create an account")
                    }
                }

                message?.let {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = it,
                        color = if (it.contains("Ошибка")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                    Text(" OR ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(horizontal = 8.dp))
                    HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                }

                Spacer(modifier = Modifier.height(24.dp))

                SocialButton(text = "Continue with Google", onClick = { /* TODO */ })
                Spacer(modifier = Modifier.height(8.dp))
                SocialButton(text = "Continue with Apple", onClick = { /* TODO */ })

                Spacer(modifier = Modifier.height(24.dp))

                TextButton(onClick = { onSkipAuth() }) {
                    Text("Продолжить локально", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Text(
                text = "version-0.0.1",
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                fontSize = 10.sp
            )
        }
    }

    @Composable
    private fun AuthTab(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
        val backgroundColor = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent
        val textColor = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(8.dp))
                .background(backgroundColor)
                .clickable { onClick() }
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = text, color = textColor, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, fontSize = 14.sp)
        }
    }

    @Composable
    private fun SocialButton(text: String, onClick: () -> Unit) {
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().height(44.dp),
            shape = RoundedCornerShape(12.dp),
            border = ButtonDefaults.outlinedButtonBorder(enabled = true)
        ) {
            Text(text, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
        }
    }
}
