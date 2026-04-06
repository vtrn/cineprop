package org.mosyagin.project.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SettingsBrightness
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

                item {
                    Text("Безопасность", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Lock, null, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Сквозное шифрование (E2EE)", style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        "Шифрует заметки и сценарии перед отправкой в облако.", 
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = isEncryptionEnabled,
                                    onCheckedChange = { viewModel.setEncryptionEnabled(it) }
                                )
                            }
                        }
                    }
                }
            }
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
                .height(48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Text(title, modifier = Modifier.weight(1f))
            RadioButton(selected = isSelected, onClick = onClick)
        }
    }
}
