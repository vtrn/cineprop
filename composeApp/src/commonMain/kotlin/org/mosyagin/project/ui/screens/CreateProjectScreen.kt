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

class CreateProjectScreen(private val queries: DatabaseQueries) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        // Переменные для хранения того, что ввел юзер
        var name by remember { mutableStateOf("") }
        var director by remember { mutableStateOf("") }

        Column(modifier = Modifier.padding(16.dp)) {
            // Поле для названия
            TextField(value = name, onValueChange = { name = it }, label = { Text("Название проекта") })

            Spacer(modifier = Modifier.height(8.dp))

            // Поле для режиссера
            TextField(value = director, onValueChange = { director = it }, label = { Text("Режиссер") })

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {
                if (name.isNotBlank() && director.isNotBlank()) {
                    queries.insertProject(name, director)
                    navigator.pop() // Возвращаемся в список
                }
            }) {
                Text("Сохранить проект")
            }
        }
    }
}
