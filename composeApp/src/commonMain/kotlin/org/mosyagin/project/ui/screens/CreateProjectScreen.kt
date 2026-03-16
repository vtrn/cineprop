package org.mosyagin.project.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow

import org.mosyagin.project.db.LocalDatabaseQueries

class CreateProjectScreen(
    private val projectIdToEdit: Long? = null,
    private val initialName: String? = null,
    private val initialDirector: String? = null
) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val queries = LocalDatabaseQueries.current
        val navigator = LocalNavigator.currentOrThrow

        // Инициализируем поля значениями из проекта, если мы в режиме редактирования
        var name by remember { mutableStateOf(initialName ?: "") }
        var director by remember { mutableStateOf(initialDirector ?: "") }

        Scaffold(
            topBar = {
                TopAppBar(title = { Text(if (projectIdToEdit == null) "Новый проект" else "Редактировать проект") })
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp) // Авто-отступы между полями
            ) {
                OutlinedTextField( // Используем Outlined для более современного вида
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название фильма") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = director,
                    onValueChange = { director = it },
                    label = { Text("Режиссер") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.weight(1f)) // Прижимает кнопку к низу

                Button(
                    onClick = {
                        if (name.isNotBlank() && director.isNotBlank()) {
                            if (projectIdToEdit == null) {
                                // 1. Если проекта НЕТ — создаем новый
                                queries.insertProject(name, director)
                            } else {
                                // 2. Если проект ЕСТЬ — обновляем существующий по ID
                                queries.updateProject(
                                    name = name,
                                    director = director,
                                    id = projectIdToEdit // Тот самый ID, который пришел из списка
                                )
                            }
                            navigator.pop() // Возвращаемся в список
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = name.isNotBlank() && director.isNotBlank()
                ) {
                    // Меняем текст на кнопке для красоты
                    Text(if (projectIdToEdit == null) "Создать проект" else "Сохранить изменения")
                }
            }
        }
    }
}
