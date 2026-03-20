package org.mosyagin.project.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.koin.compose.koinInject
import org.mosyagin.project.DatabaseQueries
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

data class ShiftDetailScreen(val shiftId: Long) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val queries = koinInject<DatabaseQueries>()
        
        val shift = remember(shiftId, queries) {
            queries.getShiftById(shiftId).executeAsOne()
        }

        val scenes by remember(shiftId, queries) {
            queries.getScenesForShift(shiftId)
                .asFlow()
                .mapToList(Dispatchers.IO)
        }.collectAsState(initial = emptyList())

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Column {
                            Text("Смена №${shift.shiftNumber}", style = MaterialTheme.typography.titleMedium)
                            Text(shift.date, style = MaterialTheme.typography.bodySmall)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                        }
                    }
                )
            }
        ) { padding ->
            if (scenes.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "В этой смене нет привязанных сцен.\nПроверьте, загружен ли сценарий и КПП.",
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.padding(padding).fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text("План смены:", style = MaterialTheme.typography.headlineSmall)
                    }
                    items(scenes) { scene ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable {
                                navigator.push(SceneDetailScreen(scene.id, scene.projectId))
                            },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Сцена ${scene.seriesNumber} / ${scene.sceneNumber}", style = MaterialTheme.typography.titleMedium)
                                Text(scene.location, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "${scene.timeOfDay} | ${if (scene.isInterior == 1L) "ИНТ" else "НАТ"}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
