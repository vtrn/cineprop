/**
 * Главный экран приложения — Список проектов.
 * 
 * Здесь отображаются все созданные пользователем кинопроекты.
 * Отсюда можно создать новый проект или перейти в панель управления существующим.
 */
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
import cafe.adriel.voyager.koin.getScreenModel
import org.mosyagin.project.db.ProjectListScreenModel

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add

class ProjectListScreen : Screen {

    @Composable
    override fun Content() {
        // Получаем навигатор
        val navigator = LocalNavigator.currentOrThrow
        
        // Используем Koin для получения ScreenModel
        val screenModel = getScreenModel<ProjectListScreenModel>()
        
        // Подписываемся на список проектов (UI обновится сам при изменении списка)
        val projects by screenModel.projects.collectAsState()

        Scaffold(
            topBar = {
                Text(
                    "Проекты",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            },
            floatingActionButton = {
                // Кнопка "+" для перехода на экран создания проекта
                FloatingActionButton(
                    onClick = { navigator.push(CreateProjectScreen()) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.Black,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Создать проект")
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (projects.isEmpty()) {
                    // Заглушка, если список пуст
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Нет активных проектов", color = Color.Gray)
                    }
                } else {
                    // Список проектов с использованием LazyColumn (аналог RecyclerView)
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(projects) { project ->
                            ProjectCard(
                                project = project,
                                onClick = {
                                    // Переход в Dashboard проекта
                                    navigator.push(ProjectDashboardScreen(project.id))
                                },
                                onDelete = {
                                    // Вызов удаления через ScreenModel
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
