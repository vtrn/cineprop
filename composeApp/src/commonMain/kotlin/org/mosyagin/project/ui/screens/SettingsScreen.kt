package org.mosyagin.project.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel

class SettingsScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<SettingsViewModel>()
        val themeMode by viewModel.themeMode.collectAsState()
        val isEncryptionEnabled by viewModel.isEncryptionEnabled.collectAsState()
        val isCloudKeySyncEnabled by viewModel.isCloudKeySyncEnabled.collectAsState()
        val isRecoveryPinEnabled by viewModel.isRecoveryPinEnabled.collectAsState()

        Scaffold(
            topBar = {
                TopAppBar(title = { Text("Настройки") })
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Блок: Внешний вид
                item {
                    Text("Внешний вид", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            ThemeOption(
                                title = "Системная",
                                icon = Icons.Default.SettingsBrightness,
                                isSelected = themeMode == "system",
                                onClick = { viewModel.setThemeMode("system") }
                            )
                            ThemeOption(
                                title = "Светлая",
                                icon = Icons.Default.LightMode,
                                isSelected = themeMode == "light",
                                onClick = { viewModel.setThemeMode("light") }
                            )
                            ThemeOption(
                                title = "Темная",
                                icon = Icons.Default.DarkMode,
                                isSelected = themeMode == "dark",
                                onClick = { viewModel.setThemeMode("dark") }
                            )
                        }
                    }
                }

                // Блок: Безопасность
                item {
                    Text("Безопасность", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // 1. Основной переключатель E2EE
                            SecurityToggle(
                                title = "Сквозное шифрование (E2EE)",
                                subtitle = "Шифрует заметки и сценарии перед отправкой в облако.",
                                icon = Icons.Default.Lock,
                                checked = isEncryptionEnabled,
                                onCheckedChange = { viewModel.setEncryptionEnabled(it) }
                            )

                            if (isEncryptionEnabled) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    thickness = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                                )
                                
                                Text("Методы восстановления доступа", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)

                                // 2. Облачная синхронизация ключей
                                SecurityToggle(
                                    title = "Облачная синхронизация",
                                    subtitle = "Безопасное хранение ключей в iCloud / Google Backup (Рекомендуется).",
                                    icon = Icons.Default.CloudSync,
                                    checked = isCloudKeySyncEnabled,
                                    onCheckedChange = { viewModel.setCloudKeySyncEnabled(it) }
                                )

                                // 3. Recovery PIN
                                SecurityToggle(
                                    title = "Использовать Recovery PIN",
                                    subtitle = "6-значный код для восстановления доступа без старого устройства.",
                                    icon = Icons.Default.Dialpad,
                                    checked = isRecoveryPinEnabled,
                                    onCheckedChange = { viewModel.setRecoveryPinEnabled(it) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun SecurityToggle(
        title: String,
        subtitle: String,
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(icon, null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(
                    subtitle, 
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }

    @Composable
    private fun ThemeOption(
        title: String,
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        isSelected: Boolean,
        onClick: () -> Unit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Text(title, modifier = Modifier.weight(1f))
            RadioButton(selected = isSelected, onClick = onClick)
        }
    }
}
