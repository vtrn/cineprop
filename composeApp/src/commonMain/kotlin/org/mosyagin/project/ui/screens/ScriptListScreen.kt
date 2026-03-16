package org.mosyagin.project.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.mosyagin.project.db.LocalDatabaseQueries
import org.mosyagin.project.util.rememberFilePickerLauncher
import kotlinx.coroutines.launch
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import org.mosyagin.project.Project

// 1. Изменяем конструктор: принимаем Long вместо объекта Project
data class ScriptListScreen(val projectId: Long) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val queries = LocalDatabaseQueries.current
        val viewModel = remember(queries) { ScriptViewModel(queries) }

        // 1. Подписываемся на состояние загрузки из ViewModel
        val isLoading by viewModel.isLoading.collectAsState()

        val scope = rememberCoroutineScope()

        val scripts by remember(queries, projectId) {
            queries.getScriptsForProject(projectId).asFlow().mapToList(Dispatchers.Default)
        }.collectAsState(initial = emptyList())

        val filePicker = rememberFilePickerLauncher { platformFile ->
            platformFile?.uriString?.let { uri ->
                scope.launch { viewModel.processPdfUri(projectId, uri) }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                topBar = { /* твой TopAppBar */ },
                floatingActionButton = {
                    // Прячем кнопку или выключаем её, пока идет загрузка
                    if (!isLoading) {
                        FloatingActionButton(onClick = { filePicker.launch() }) {
                            Icon(Icons.Default.Add, contentDescription = null)
                        }
                    }
                }
            ) { padding ->
                if (scripts.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Description,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "Нет загруженных сценариев",
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
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
                                supportingContent = { Text("Файл: ...${script.filePath.takeLast(20)}") },
                                leadingContent = {
                                    Icon(
                                        Icons.Default.Description,
                                        tint = MaterialTheme.colorScheme.primary,
                                        contentDescription = null
                                    )
                                },
                                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            )
                        }
                    }
                }
            }

            // 2. Оверлей загрузки (показывается поверх всего)
            if (isLoading) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black.copy(alpha = 0.6f) // Затемнение экрана
                ) {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(16.dp))
                        Text("Анализирую сценарий...", color = Color.White)
                        Text(
                            "Это может занять пару секунд",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}
