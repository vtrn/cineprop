package org.mosyagin.project.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.mosyagin.project.DatabaseQueries // Твой сгенерированный класс
import org.mosyagin.project.Project // Твой сгенерированный дата-класс

// Передаем queries в конструктор экрана
class ProjectListScreen(private val queries: DatabaseQueries) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        // Получаем список проектов из БД. Используем remember, чтобы не перечитывать на каждом рекомпозе
        // .executeAsList() — это метод, который генерирует SQLDelight
        val projects by remember { mutableStateOf(queries.getAllProjects().executeAsList()) }

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Список проектов", modifier = Modifier.padding(16.dp))

            // Список наших проектов из БД

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(projects) { project ->
                    ProjectCard(project = project, onClick = {
                        // Тут будет навигация в детали проекта, когда создашь экран
                    })
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {
                // Пример добавления тестового проекта (чтобы проверить БД)
                queries.insertProject("Новенькая", "Сергей Мосягин")
                // В идеале тут должен быть рефреш списка,
                // но для простоты начни с этого.
                navigator.push(CreateProjectScreen(queries))
            }) {
                Text("Создать новый проект")
            }
        }
    }
}