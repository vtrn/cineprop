package org.mosyagin.project.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
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
import org.mosyagin.project.ScriptFile
import org.mosyagin.project.repository.ScriptRepository
import org.mosyagin.project.util.AppResult

data class ScriptListScreen(val projectId: Long) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val repository = koinInject<ScriptRepository>()
        val screenModel = getScreenModel<ScriptViewModel>()

        val isLoading by screenModel.isLoading.collectAsState()
        val parseResult by screenModel.parseResult.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()

        val scripts by repository.getScriptsForProject(projectId).collectAsState(initial = emptyList())

        // Состояния для диалогов
        var showAddDialog by remember { mutableStateOf(false) }
        var seriesNumberInput by remember { mutableStateOf("1") }

        var scriptToEdit by remember { mutableStateOf<ScriptFile?>(null) }
        var editTitleInput by remember { mutableStateOf("") }

        var scriptToDelete by remember { mutableStateOf<ScriptFile?>(null) }

        val filePicker = rememberFilePickerLauncher { platformFile ->
            platformFile?.uriString?.let { uri ->
                scope.launch {
                    val seriesNum = seriesNumberInput.toIntOrNull() ?: 1
                    screenModel.processPdfUri(projectId, seriesNum, uri)
                }
            }
        }

        // Обработка результатов парсинга через Snackbar
        LaunchedEffect(parseResult) {
            parseResult?.let { result ->
                if (result is AppResult.Error) {
                    snackbarHostState.showSnackbar(
                        message = result.message,
                        duration = SnackbarDuration.Long
                    )
                } else if (result is AppResult.Success) {
                    snackbarHostState.showSnackbar("Сценарий успешно загружен")
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    TopAppBar(
                        title = { Text("Сценарии") },
                        navigationIcon = {
                            IconButton(onClick = { navigator.pop() }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
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
                if (scripts.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                        Text("Нет загруженных сценариев", color = MaterialTheme.colorScheme.outline)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(scripts) { script ->
                            ListItem(
                                headlineContent = { Text(script.title) },
                                supportingContent = { Text("Создан: ${formatDate(script.createdAt)}") },
                                leadingContent = { Icon(Icons.Default.Description, tint = MaterialTheme.colorScheme.primary, contentDescription = null) },
                                trailingContent = {
                                    Row {
                                        IconButton(onClick = {
                                            scriptToEdit = script
                                            editTitleInput = script.title
                                        }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Изменить")
                                        }
                                        IconButton(onClick = { scriptToDelete = script }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                },
                                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier.clickable { /* Можно открывать список сцен этого файла */ }
                            )
                        }
                    }
                }

                // Диалог добавления
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

                // Диалог редактирования названия
                scriptToEdit?.let { script ->
                    AlertDialog(
                        onDismissRequest = { scriptToEdit = null },
                        title = { Text("Изменить название") },
                        text = {
                            OutlinedTextField(
                                value = editTitleInput,
                                onValueChange = { editTitleInput = it },
                                label = { Text("Название") },
                                singleLine = true
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                screenModel.updateScriptTitle(script.id, editTitleInput)
                                scriptToEdit = null
                            }) { Text("Сохранить") }
                        },
                        dismissButton = {
                            TextButton(onClick = { scriptToEdit = null }) { Text("Отмена") }
                        }
                    )
                }

                // Диалог удаления
                scriptToDelete?.let { script ->
                    AlertDialog(
                        onDismissRequest = { scriptToDelete = null },
                        title = { Text("Удалить сценарий?") },
                        text = { Text("Это удалит файл '${script.title}' и ВСЕ связанные с ним сцены. Это действие нельзя отменить.") },
                        confirmButton = {
                            Button(
                                onClick = {
                                    screenModel.deleteScriptFile(script.id)
                                    scriptToDelete = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) { Text("Удалить") }
                        },
                        dismissButton = {
                            TextButton(onClick = { scriptToDelete = null }) { Text("Отмена") }
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
        return "от " + (timestamp / 1000 / 3600 / 24 % 31).toString()
    }
}
