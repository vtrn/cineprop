package org.mosyagin.project.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.mosyagin.project.DatabaseQueries
import org.mosyagin.project.Project
import org.mosyagin.project.ui.components.DashboardTile

@OptIn(ExperimentalMaterial3Api::class)
data class ProjectDashboardScreen(
    val queries: DatabaseQueries,
    val project: Project
) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Column {
                        Text(project.name, style = MaterialTheme.typography.titleMedium)
                        Text("Дашборд проекта", style = MaterialTheme.typography.bodySmall)
                    }},
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
                item { DashboardTile("Сценарий", Icons.Default.Description) { /* Скоро */ } }
                item { DashboardTile("Сцены", Icons.Default.List) { /* Скоро */ } }
                item { DashboardTile("КПП", Icons.Default.Event) { /* Скоро */ } }
                item { DashboardTile("Трекер", Icons.Default.AddAPhoto) { /* Скоро */ } }
                item { DashboardTile("Реквизит", Icons.Default.Inventory) { /* Скоро */ } }
                item { DashboardTile("Библия", Icons.Default.AutoStories) { /* Скоро */ } }
            }
        }
    }
}