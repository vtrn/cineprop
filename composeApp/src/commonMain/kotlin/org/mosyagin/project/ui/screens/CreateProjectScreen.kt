package org.mosyagin.project.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow

import org.mosyagin.project.DatabaseQueries
import org.mosyagin.project.Project

class CreateProjectScreen(
    private val queries: DatabaseQueries,
    private val projectToEdit: Project? = null // Если null — создаем, если нет — редактируем
) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        // Инициализируем поля значениями из проекта, если мы в режиме редактирования
        var name by remember { mutableStateOf(projectToEdit?.name ?: "") }
        var director by remember { mutableStateOf(projectToEdit?.director ?: "") }

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(if (projectToEdit == null) "Новый проект" else "Редактировать")

            TextField(value = name, onValueChange = { name = it }, label = { Text("Название") })
            TextField(value = director, onValueChange = { director = it }, label = { Text("Режиссер") })

            Button(onClick = {
                if (projectToEdit == null) {
                    queries.insertProject(name, director)
                } else {
                    // Используем функцию обновления
                    queries.updateProject(name, director, projectToEdit.id)
                }
                navigator.pop()
            }) {
                Text("Сохранить")
            }
        }
    }
}
