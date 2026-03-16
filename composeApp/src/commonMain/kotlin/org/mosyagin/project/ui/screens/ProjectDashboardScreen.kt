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
data class ProjectDashboardScreen(val projectId: Long) : Screen { // Передаем только ID

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val queries = LocalDatabaseQueries.current

        // Достаем данные проекта из базы по ID
        // remember гарантирует, что мы не будем мучить базу при каждом мигании экрана
        val project = remember(projectId) {
            queries.getProjectById(projectId).executeAsOne()
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(project.name) }, // Теперь берем имя отсюда
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null)
                        }
                    }
                )
            }
        ) { padding ->
            LazyVerticalGrid(
                columns = GridCells.Fixed(2), // 2 колонки как в плитке шоколада
                modifier = Modifier.fillMaxSize().padding(padding).padding(8.dp)
            ) {
                item {
                    DashboardTile("Сценарий", Icons.Default.Description) {
                        // ПЕРЕДАЕМ ТОЛЬКО ПРОЕКТ
                        navigator.push(ScriptListScreen(projectId))
                    }
                }
                item {
                    DashboardTile("Сцены", Icons.Default.List) {
                        navigator.push(SceneListScreen(project.id, project.name))
                    }
                }
                item { DashboardTile("КПП", Icons.Default.Event) { /* Скоро */ } }
                item { DashboardTile("Трекер", Icons.Default.AddAPhoto) { /* Скоро */ } }
                item { DashboardTile("Реквизит", Icons.Default.Inventory) { /* Скоро */ } }
                item { DashboardTile("Библия", Icons.Default.AutoStories) { /* Скоро */ } }
            }
        }
    }
}