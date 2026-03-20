/**
 * Экран "Список всех сцен проекта".
 * 
 * Отображает полный перечень сцен, извлеченных из сценария.
 * Позволяет быстро просмотреть локацию, тип (ИНТ/НАТ) и время суток для каждой сцены.
 */
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
import org.koin.compose.koinInject
import org.mosyagin.project.repository.SceneRepository

data class SceneListScreen(val projectId: Long, val projectName: String) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val repository = koinInject<SceneRepository>()

        // Получаем поток (Flow) сцен для данного проекта через репозиторий.
        val scenes by repository.getScenesByProject(projectId).collectAsState(initial = emptyList())

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Сцены: $projectName") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
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
                        headlineContent = {
                            Text("Сцена ${scene.seriesNumber}-${scene.sceneNumber}: ${scene.location}")
                        },
                        supportingContent = {
                            Text(if (scene.isInterior == 1L) "ИНТ. | ${scene.timeOfDay}" else "НАТ. | ${scene.timeOfDay}")
                        },
                        modifier = Modifier.clickable {
                            navigator.push(SceneDetailScreen(sceneId = scene.id, projectId = projectId))
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                }
            }
        }
    }
}
