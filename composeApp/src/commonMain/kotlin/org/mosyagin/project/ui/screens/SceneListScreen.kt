package org.mosyagin.project.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.mosyagin.project.db.LocalDatabaseQueries
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers

data class SceneListScreen(val projectId: Long, val projectName: String) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val queries = LocalDatabaseQueries.current

        // Получаем все сцены этого проекта из БД
        val scenes by remember(projectId) {
            queries.getScenesByProject(projectId)
                .asFlow()
                .mapToList(Dispatchers.Default)
        }.collectAsState(initial = emptyList())

        // 1. Достаем проект из базы по ID, который пришел в конструктор
        val project = remember(projectId) {
            queries.getProjectById(projectId).executeAsOne()
        }


        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Сцены: $projectName") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null)
                        }
                    }
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                items(scenes) { scene ->
                    ListItem(
                        headlineContent = { Text("Сцена ${scene.sceneNumber}: ${scene.location}") },
                        supportingContent = {
                            Text(if (scene.isInterior == 1L) "ИНТ. | ${scene.timeOfDay}" else "НАТ. | ${scene.timeOfDay}")
                        },
                        modifier = Modifier.clickable {
                            // ПЕРЕХОД: передаем ID сцены и ID проекта
                            navigator.push(SceneDetailScreen(sceneId = scene.id, projectId = project.id))
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                }
            }
        }
    }
}