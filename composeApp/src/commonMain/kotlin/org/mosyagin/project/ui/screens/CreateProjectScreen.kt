package org.mosyagin.project.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.mosyagin.project.repository.ProjectRepository

class CreateProjectScreen(
    private val projectIdToEdit: String? = null,
    private val initialName: String? = null,
    private val initialDirector: String? = null
) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val repository = koinInject<ProjectRepository>() // Используем репозиторий вместо queries
        val scope = rememberCoroutineScope()

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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
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

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {
                        if (name.isNotBlank() && director.isNotBlank()) {
                            scope.launch {
                                if (projectIdToEdit == null) {
                                    repository.addProject(name, director)
                                } else {
                                    repository.updateProject(
                                        id = projectIdToEdit,
                                        name = name,
                                        director = director
                                    )
                                }
                                navigator.pop()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = name.isNotBlank() && director.isNotBlank()
                ) {
                    Text(if (projectIdToEdit == null) "Создать проект" else "Сохранить изменения")
                }
            }
        }
    }
}
