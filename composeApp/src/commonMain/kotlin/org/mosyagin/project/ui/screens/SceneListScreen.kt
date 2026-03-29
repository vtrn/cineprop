package org.mosyagin.project.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.koin.compose.koinInject
import org.mosyagin.project.GetLatestScenesForProject
import org.mosyagin.project.repository.SceneRepository
import org.mosyagin.project.ui.components.AppLayoutType
import org.mosyagin.project.ui.components.CineCard
import org.mosyagin.project.ui.components.CineTag
import org.mosyagin.project.ui.components.LocalAppLayoutType

enum class SceneFilter { ALL, MODIFIED, NEW }

data class SceneListScreen(val projectId: Long, val projectName: String) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val sceneRepository = koinInject<SceneRepository>()
        val layoutType = LocalAppLayoutType.current

        var searchQuery by remember { mutableStateOf("") }
        var selectedFilter by remember { mutableStateOf(SceneFilter.ALL) }

        val scenes by sceneRepository.getLatestScenesForProject(projectId).collectAsState(initial = emptyList())

        val filteredScenes = remember(scenes, searchQuery, selectedFilter) {
            scenes.filter { scene ->
                val matchesSearch = scene.sceneNumber.contains(searchQuery, ignoreCase = true) || 
                                  scene.location.contains(searchQuery, ignoreCase = true)
                val matchesFilter = when (selectedFilter) {
                    SceneFilter.ALL -> true
                    SceneFilter.MODIFIED -> scene.needsReview == 1L
                    SceneFilter.NEW -> scene.versionCount == 1L
                }
                matchesSearch && matchesFilter
            }
        }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Сцены: $projectName", style = MaterialTheme.typography.titleLarge) },
                    navigationIcon = {
                        if (layoutType == AppLayoutType.MOBILE) {
                            IconButton(onClick = { navigator.pop() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { paddingValues ->
            Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Поиск сцены или локации...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        { IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Close, null) } }
                    } else null,
                    shape = CircleShape,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SceneFilter.entries.forEach { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = {
                                Text(
                                    when (filter) {
                                        SceneFilter.ALL -> "Все"
                                        SceneFilter.MODIFIED -> "Измененные"
                                        SceneFilter.NEW -> "Новые"
                                    }
                                )
                            }
                        )
                    }
                }

                if (filteredScenes.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (scenes.isEmpty()) "Загрузите сценарий" else "Ничего не найдено",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredScenes) { scene ->
                            SceneCardItem(
                                scene = scene,
                                onClick = {
                                    navigator.push(SceneDetailScreen(
                                        sceneUserDataId = scene.id, 
                                        projectId = projectId,
                                        scriptFileId = scene.scriptFileId
                                    ))
                                },
                                onDiffClick = {
                                    navigator.push(SceneDiffScreen(scene.id))
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun SceneCardItem(
        scene: GetLatestScenesForProject,
        onClick: () -> Unit,
        onDiffClick: () -> Unit
    ) {
        val isBrandNew = scene.versionCount == 1L

        CineCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = onClick
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Сцена ${scene.seriesNumber}-${scene.sceneNumber}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (isBrandNew) {
                        CineTag(
                            text = "NEW",
                            containerColor = Color(0xFFE8F5E9),
                            contentColor = Color(0xFF2E7D32)
                        )
                    }
                }
                
                Spacer(Modifier.height(4.dp))
                Text(
                    text = scene.location,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
