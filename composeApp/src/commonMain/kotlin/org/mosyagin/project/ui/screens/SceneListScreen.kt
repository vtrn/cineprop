package org.mosyagin.project.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.koin.compose.koinInject
import org.mosyagin.project.repository.SceneRepository
import org.mosyagin.project.repository.ScriptRepository
import org.mosyagin.project.ui.components.CineCard
import org.mosyagin.project.ui.components.CineTag

data class SceneListScreen(val projectId: Long, val projectName: String) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val sceneRepository = koinInject<SceneRepository>()
        val scriptRepository = koinInject<ScriptRepository>()

        // Получаем список файлов сценария, чтобы выбрать последний/актуальный
        val scripts by scriptRepository.getScriptsForProject(projectId).collectAsState(initial = emptyList())
        
        // По умолчанию берем последний загруженный файл
        val selectedScriptFileId = scripts.lastOrNull()?.id

        // Получаем список сцен для выбранного файла
        val scenes by if (selectedScriptFileId != null) {
            sceneRepository.getScenesByProject(projectId, selectedScriptFileId).collectAsState(initial = emptyList())
        } else {
            remember { mutableStateOf(emptyList()) }
        }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Сцены: $projectName", style = MaterialTheme.typography.titleLarge) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                )
            }
        ) { padding ->
            if (selectedScriptFileId == null) {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text("Загрузите сценарий, чтобы увидеть список сцен")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(scenes) { scene ->
                        CineCard(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                navigator.push(SceneDetailScreen(
                                    sceneUserDataId = scene.id, 
                                    projectId = projectId,
                                    scriptFileId = selectedScriptFileId
                                ))
                            }
                        ) {
                            Column {
                                Text(
                                    text = "Сцена ${scene.seriesNumber}-${scene.sceneNumber}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = scene.location,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(12.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    CineTag(
                                        text = if (scene.isInterior == 1L) "ИНТ" else "НАТ",
                                        containerColor = if (scene.isInterior == 1L) 
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f) 
                                        else 
                                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                                    )
                                    CineTag(
                                        text = scene.timeOfDay.uppercase(),
                                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
