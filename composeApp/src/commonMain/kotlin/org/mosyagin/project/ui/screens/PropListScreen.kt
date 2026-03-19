/**
 * Экран "Реквизит".
 * 
 * Отображает список всего реквизита проекта с указанием сцен и статуса.
 * Реализует функционал для Milestone #24.
 */
package org.mosyagin.project.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
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
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import org.mosyagin.project.GetPropsByProject

/**
 * Экран со списком реквизита.
 */
data class PropListScreen(val projectId: Long) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val queries = LocalDatabaseQueries.current
        val scope = rememberCoroutineScope()

        var searchQuery by remember { mutableStateOf("") }

        // Получаем весь реквизит проекта
        val props by remember(projectId, queries) {
            queries.getPropsByProject(projectId)
                .asFlow()
                .mapToList(Dispatchers.IO)
        }.collectAsState(initial = emptyList())

        // Фильтрация по поиску
        val filteredProps = props.filter { it.name.contains(searchQuery, ignoreCase = true) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Реквизит") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                        }
                    }
                )
            }
        ) { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                // Поле поиска
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    placeholder = { Text("Поиск реквизита...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )

                if (props.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Реквизит не найден. Выделите его в тексте сцен.", color = MaterialTheme.colorScheme.outline)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp, start = 16.dp, end = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredProps) { prop ->
                            PropItem(prop) { newStatus ->
                                scope.launch(Dispatchers.IO) {
                                    queries.updatePropStatus(newStatus, prop.id)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PropItem(prop: GetPropsByProject, onStatusChange: (String) -> Unit) {
    val isReady = prop.status == "Готово"

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (isReady) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) 
                            else MaterialTheme.colorScheme.surface
        )
    ) {
        ListItem(
            headlineContent = { 
                Text(
                    text = prop.name, 
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isReady) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                ) 
            },
            supportingContent = { 
                Text("Сцена ${prop.seriesNumber}-${prop.sceneNumber}") 
            },
            leadingContent = {
                Icon(
                    imageVector = Icons.Default.Inventory,
                    contentDescription = null,
                    tint = if (isReady) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary
                )
            },
            trailingContent = {
                IconButton(onClick = { 
                    onStatusChange(if (isReady) "Найти" else "Готово") 
                }) {
                    Icon(
                        imageVector = if (isReady) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = "Статус",
                        tint = if (isReady) Color(0xFF4CAF50) else MaterialTheme.colorScheme.outline
                    )
                }
            }
        )
    }
}
