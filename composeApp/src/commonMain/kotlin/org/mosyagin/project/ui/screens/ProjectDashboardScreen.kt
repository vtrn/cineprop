/**
 * Экран "Дашборд проекта" (Панель управления).
 * 
 * Это центральный узел конкретного кинопроекта. Отображается в виде сетки (плиток).
 * Позволяет перейти к сценариям, списку сцен, КПП и Трекеру.
 */
package org.mosyagin.project.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.mosyagin.project.Project
import org.mosyagin.project.db.LocalDatabaseQueries
import org.mosyagin.project.ui.components.DashboardTile

@OptIn(ExperimentalMaterial3Api::class)
data class ProjectDashboardScreen(val projectId: Long) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val queries = LocalDatabaseQueries.current

        // Загружаем данные проекта из базы по его ID.
        val project = remember(projectId) {
            queries.getProjectById(projectId).executeAsOne()
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(project.name) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                        }
                    }
                )
            }
        ) { padding ->
            // Сетка из плиток (2 колонки)
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize().padding(padding).padding(8.dp)
            ) {
                item {
                    DashboardTile("Сценарий", Icons.Default.Description) {
                        navigator.push(ScriptListScreen(projectId))
                    }
                }
                item {
                    DashboardTile("Сцены", Icons.Default.List) {
                        navigator.push(SceneListScreen(project.id, project.name))
                    }
                }

                item {
                    DashboardTile("КПП", Icons.Default.EventNote) {
                        navigator.push(KppListScreen(projectId = project.id))
                    }
                }

                item { 
                    DashboardTile("Трекер", Icons.Default.AddAPhoto) {
                        navigator.push(TrackerScreen(projectId = project.id))
                    } 
                }
                
                item { 
                    DashboardTile("Реквизит", Icons.Default.Inventory) { 
                        navigator.push(PropListScreen(projectId))
                    } 
                }
                
                item { 
                    DashboardTile("Библия", Icons.Default.AutoStories) { 
                        navigator.push(CharacterBibleScreen(projectId))
                    } 
                }
            }
        }
    }
}
