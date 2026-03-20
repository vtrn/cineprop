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
import org.mosyagin.project.DatabaseQueries
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers

data class SceneListScreen(val projectId: Long, val projectName: String) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val queries = koinInject<DatabaseQueries>()

        // Получаем поток (Flow) сцен для данного проекта из базы данных.
        // При добавлении новых сцен в базу (через парсер), этот список обновится автоматически.
        val scenes by remember(projectId, queries) {
            queries.getScenesByProject(projectId)
                .asFlow()
                .mapToList(Dispatchers.Default)
        }.collectAsState(initial = emptyList())

        // Загружаем информацию о проекте для навигации
        val project = remember(projectId, queries) {
            queries.getProjectById(projectId).executeAsOne()
        }

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
            // Список сцен в формате ListTile (название, подзаголовок, иконка)
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                items(scenes) { scene ->
                    ListItem(
                        headlineContent = {
                            // Формат: Сцена 1-5: ЛОКАЦИЯ
                            Text("Сцена ${scene.seriesNumber}-${scene.sceneNumber}: ${scene.location}")
                        },
                        supportingContent = {
                            // Формат: ИНТ. | ДЕНЬ
                            Text(if (scene.isInterior == 1L) "ИНТ. | ${scene.timeOfDay}" else "НАТ. | ${scene.timeOfDay}")
                        },
                        modifier = Modifier.clickable {
                            // Переход к детальному просмотру текста конкретной сцены
                            navigator.push(SceneDetailScreen(sceneId = scene.id, projectId = project.id))
                        }
                    )
                    // Разделительная линия между элементами списка
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                }
            }
        }
    }
}
