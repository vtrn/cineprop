package org.mosyagin.project.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel

class CryptoSetupScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<CryptoSetupViewModel>()
        val state by viewModel.state.collectAsState()
        val focusRequester = remember { FocusRequester() }
        
        var pin by remember { mutableStateOf("") }

        LaunchedEffect(state) {
            if (state is CryptoSetupState.CreatePin || state is CryptoSetupState.RestorePin) {
                focusRequester.requestFocus()
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .clickable { focusRequester.requestFocus() },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        modifier = Modifier.padding(20.dp).size(40.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(Modifier.height(32.dp))

                val (title, subtitle) = when (state) {
                    is CryptoSetupState.Checking -> "Проверка защиты..." to "Пожалуйста, подождите"
                    is CryptoSetupState.CreatePin -> "Создайте Recovery PIN" to "Этот 6-значный код позволит вам восстановить доступ к сценариям, если вы потеряете устройство."
                    is CryptoSetupState.RestorePin -> "Введите Recovery PIN" to "Мы нашли ваш зашифрованный ключ в облаке. Введите ваш PIN-код, чтобы разблокировать данные."
                    is CryptoSetupState.Success -> "Защита настроена!" to "Ваши данные теперь в безопасности. Переходим к проектам..."
                    is CryptoSetupState.Error -> "Ошибка защиты" to (state as CryptoSetupState.Error).message
                }

                Text(text = title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, lineHeight = 20.sp)

                Spacer(Modifier.height(40.dp))

                if (state is CryptoSetupState.CreatePin || state is CryptoSetupState.RestorePin || state is CryptoSetupState.Error) {
                    PinInputField(
                        pin = pin,
                        focusRequester = focusRequester,
                        onPinChange = {
                            if (it.length <= 6 && it.all { c -> c.isDigit() }) {
                                pin = it
                                if (it.length == 6) {
                                    viewModel.handlePinAction(it)
                                    pin = "" 
                                }
                            }
                        }
                    )
                }

                if (state is CryptoSetupState.Checking) {
                    CircularProgressIndicator(modifier = Modifier.size(48.dp))
                }

                // КНОПКА ВЫХОДА (на случай проблем с сессией)
                if (state is CryptoSetupState.Error || state is CryptoSetupState.CreatePin || state is CryptoSetupState.RestorePin) {
                    Spacer(Modifier.height(48.dp))
                    TextButton(onClick = { viewModel.signOut() }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Выйти из аккаунта")
                    }
                }
            }
        }
    }

    @Composable
    private fun PinInputField(pin: String, focusRequester: FocusRequester, onPinChange: (String) -> Unit) {
        Box(contentAlignment = Alignment.Center) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                repeat(6) { index ->
                    val char = pin.getOrNull(index)
                    val isFilled = char != null
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isFilled) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .border(1.dp, if (isFilled) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isFilled) {
                            Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                        }
                    }
                }
            }
            TextField(
                value = pin,
                onValueChange = onPinChange,
                modifier = Modifier.matchParentSize().alpha(0f).focusRequester(focusRequester),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
            )
        }
        Spacer(Modifier.height(24.dp))
        Text("Введите 6 цифр", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
    }
}
