package org.mosyagin.project.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.core.model.rememberScreenModel
import org.mosyagin.project.db.ProjectListScreenModel
import org.mosyagin.project.db.LocalDatabaseQueries

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add

class ProjectListScreen : Screen {

    @Composable
    override fun Content() {
        val queries = LocalDatabaseQueries.current
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { ProjectListScreenModel(queries) }
        val projects by screenModel.projects.collectAsState()

        // Scaffold — это скелет экрана. Он сам знает, где должен быть заголовок и кнопка FAB
        Scaffold(
            topBar = {
                // Можно добавить красивый заголовок
                Text(
                    "Проекты",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            },
            floatingActionButton = {
                // Вот он — настоящий FAB
                FloatingActionButton(
                    onClick = { navigator.push(CreateProjectScreen()) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.Black,
                    shape = CircleShape // Делаем его круглым или скругленным квадратом
                ) {
                    // Иконка плюсика (нужен импорт androidx.compose.material.icons.Icons)
                    Icon(Icons.Default.Add, contentDescription = "Создать проект")
                }
            },
            containerColor = MaterialTheme.colorScheme.background // Делаем фон всего экрана темным
        ) { paddingValues ->
            // Весь контент теперь внутри paddingValues
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (projects.isEmpty()) {
                    // Если проектов нет — показываем красивую заглушку
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Нет активных проектов", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp) // Отступы между карточками
                    ) {
                        items(projects) { project ->
                            ProjectCard(
                                project = project,
                                onClick = {
                                    navigator.push(ProjectDashboardScreen(project.id))
                                },
                                onDelete = {
                                    // УДАЛЕНИЕ
                                    screenModel.deleteProject(project.id)
                                }
                            )
                        }
                        }
                    }
                }
            }
        }
    }
