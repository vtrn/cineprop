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
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.mosyagin.project.db.LocalDatabaseQueries
import org.mosyagin.project.util.rememberFilePickerLauncher // Импорт нашего моста
import kotlinx.coroutines.launch
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers

data class ScriptListScreen(
    val projectId: Long,
    val projectName: String
) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val queries = LocalDatabaseQueries.current
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()

        // 1. Инициализируем ViewModel (она теперь умеет парсить PDF)
        val viewModel = remember { ScriptViewModel(queries) }

        // Внутри функции Content()

        // Подписываемся на поток данных из БД
        // Каждый раз, когда в таблицу ScriptFile добавится запись, список обновится автоматически
        // Внутри Content()

        val scripts by remember(queries, projectId) {
            queries.getScriptsForProject(projectId)
                .asFlow()
                .mapToList(Dispatchers.Default)
        }.collectAsState(initial = emptyList())

        // Добавим состояние загрузки, чтобы понимать, что процесс идет
        var isParsing by remember { mutableStateOf(false) }

        val filePicker = rememberFilePickerLauncher { platformFile ->
            platformFile?.uriString?.let { uri ->
                scope.launch {
                    isParsing = true // Показываем лоадер
                    viewModel.processPdfUri(projectId, uri)
                    isParsing = false // Скрываем лоадер
                }
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Сценарии", style = MaterialTheme.typography.titleMedium)
                            Text(projectName, style = MaterialTheme.typography.bodySmall)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                        }
                    }
                )
            },
            floatingActionButton = {
                // Кнопка добавления файла
                FloatingActionButton(
                    onClick = { filePicker.launch() },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Загрузить PDF")
                }
            }
        ) { padding ->
            // 4. Отображение списка
            if (scripts.isEmpty()) {
                // Если файлов еще нет
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
                // Если файлы есть — выводим список серий
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
                                Icon(Icons.Default.Description, tint = MaterialTheme.colorScheme.primary, contentDescription = null)
                            },
                            colors = ListItemDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }
                }
            }
        }
    }
}
