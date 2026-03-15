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
import org.mosyagin.project.DatabaseQueries
import org.mosyagin.project.Project
import cafe.adriel.voyager.core.model.rememberScreenModel
import org.mosyagin.project.db.ProjectListScreenModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class ProjectListScreen(private val queries: DatabaseQueries) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        // 1. Инициализируем нашу ScreenModel
        val screenModel = rememberScreenModel { ProjectListScreenModel(queries) }

        // 2. Подписываемся на StateFlow.
        // Теперь переменная projects будет САМА меняться, когда меняется БД!
        val projects by screenModel.projects.collectAsState()

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Список проектов", modifier = Modifier.padding(16.dp))

            // 3. Выводим список
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(projects) { project ->
                    ProjectCard(project = project, onClick = {
                        // Клик по карточке (сделаем позже)
                    })
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {
                // Идем на экран создания
                navigator.push(CreateProjectScreen(queries))
            }) {
                Text("Создать новый проект")
            }
        }
    }
}
