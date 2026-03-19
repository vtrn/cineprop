package org.mosyagin.project.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.mosyagin.project.db.LocalDatabaseQueries
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mosyagin.project.parser.KppParser
import org.mosyagin.project.util.rememberFilePickerLauncher

data class KppListScreen(val projectId: Long) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val queries = LocalDatabaseQueries.current
        val scope = rememberCoroutineScope()

        // 1. Подписка на список версий КПП из БД
        val kppFiles by remember(projectId, queries) {
            queries.getKppFilesByProject(projectId)
                .asFlow()
                .mapToList(Dispatchers.IO)
        }.collectAsState(initial = emptyList())

        val kppPicker = rememberFilePickerLauncher { platformFile ->
            platformFile?.let { file ->
                scope.launch {
                    val csvText = file.bytes?.decodeToString() ?: ""
                    if (csvText.isNotEmpty()) {
                        withContext(Dispatchers.IO) {
                            val parser = KppParser(queries)
                            // Парсим и сохраняем смены
                            parser.parseAndSaveKpp(projectId = projectId, csvText = csvText)
                            
                            // Определяем следующую версию
                            val nextVersion = (kppFiles.maxByOrNull { it.version }?.version ?: 0) + 1
                            
                            // Сохраняем информацию о самом файле КПП, чтобы он появился в списке
                            queries.insertKppFile(
                                projectId = projectId,
                                fileName = file.name,
                                filePath = file.uriString ?: "memory",
                                version = nextVersion
                            )
                        }
                    }
                }
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Версии КПП") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = {
                    kppPicker.launch()
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Загрузить КПП")
                }
            }
        ) { padding ->
            if (kppFiles.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("КПП еще не загружен", color = MaterialTheme.colorScheme.outline)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.padding(padding).fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(kppFiles) { file ->
                        ListItem(
                            headlineContent = { Text(file.fileName) },
                            supportingContent = { Text("Версия: ${file.version}") },
                            leadingContent = {
                                Icon(Icons.Default.Event, tint = MaterialTheme.colorScheme.primary, contentDescription = null)
                            },
                            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.clickable {
                                // Здесь можно будет открыть детальный просмотр смен этой версии
                            }
                        )
                    }
                }
            }
        }
    }
}
