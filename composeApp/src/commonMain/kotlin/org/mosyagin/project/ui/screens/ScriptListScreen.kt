package org.mosyagin.project.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.koin.compose.koinInject
import org.mosyagin.project.util.rememberFilePickerLauncher
import kotlinx.coroutines.launch
import org.mosyagin.project.repository.ScriptRepository
import org.mosyagin.project.ui.components.CineCard

data class ScriptListScreen(val projectId: Long) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val repository = koinInject<ScriptRepository>()
        val screenModel = getScreenModel<ScriptViewModel>()

        val isLoading by screenModel.isLoading.collectAsState()
        val scope = rememberCoroutineScope()

        val scripts by repository.getScriptsForProject(projectId).collectAsState(initial = emptyList())

        // Группируем скрипты по номеру серии
        val groupedScripts = remember(scripts) {
            scripts.groupBy { it.seriesNumber }
        }

        var showAddDialog by remember { mutableStateOf(false) }
        var seriesNumberInput by remember { mutableStateOf("1") }

        val filePicker = rememberFilePickerLauncher { platformFile ->
            platformFile?.uriString?.let { uri ->
                scope.launch {
                    val seriesNum = seriesNumberInput.toIntOrNull() ?: 1
                    screenModel.processPdfUri(projectId, seriesNum, uri)
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Сценарии") },
                        navigationIcon = {
                            IconButton(onClick = { navigator.pop() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                            }
                        }
                    )
                },
                floatingActionButton = {
                    FloatingActionButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Добавить")
                    }
                }
            ) { padding ->
                if (groupedScripts.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                        Text("Нет загруженных сценариев", color = MaterialTheme.colorScheme.outline)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(groupedScripts.keys.toList().sorted()) { seriesNum ->
                            val versions = groupedScripts[seriesNum] ?: emptyList()
                            val latestVersion = versions.maxByOrNull { it.createdAt }

                            CineCard(
                                onClick = {
                                    // Переходим к истории версий этой серии
                                    navigator.push(ScriptVersionScreen(projectId, seriesNum.toInt()))
                                }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Description, tint = MaterialTheme.colorScheme.primary, contentDescription = null)
                                        Spacer(Modifier.width(16.dp))
                                        Column {
                                            Text(
                                                text = "Серия $seriesNum",
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            Text(
                                                text = "${versions.size} версий • Посл. от ${formatDate(latestVersion?.createdAt ?: 0)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.outline)
                                }
                            }
                        }
                    }
                }

                if (showAddDialog) {
                    AlertDialog(
                        onDismissRequest = { showAddDialog = false },
                        title = { Text("Новая серия") },
                        text = {
                            OutlinedTextField(
                                value = seriesNumberInput,
                                onValueChange = { seriesNumberInput = it.filter { c -> c.isDigit() } },
                                label = { Text("Номер серии") },
                                singleLine = true
                            )
                        },
                        confirmButton = {
                            Button(onClick = {
                                showAddDialog = false
                                filePicker.launch()
                            }) { Text("Выбрать PDF") }
                        }
                    )
                }
            }

            if (isLoading) {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Black.copy(alpha = 0.6f)) {
                    Column(verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(16.dp))
                        Text("Обработка сценария...", color = Color.White)
                    }
                }
            }
        }
    }

    private fun formatDate(timestamp: Long): String {
        return (timestamp / 1000 / 3600 / 24 % 31).toString() + " числа"
    }
}
