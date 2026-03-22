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
import org.mosyagin.project.ui.components.CineCard
import org.mosyagin.project.ui.components.CineTag

enum class SceneFilter { ALL, MODIFIED, NEW }

data class SceneListScreen(val projectId: Long, val projectName: String) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val sceneRepository = koinInject<SceneRepository>()

        // Состояния поиска и фильтрации
        var searchQuery by remember { mutableStateOf("") }
        var selectedFilter by remember { mutableStateOf(SceneFilter.ALL) }

        // Получаем список актуальных сцен (последних версий каждой серии) для проекта
        val scenes by sceneRepository.getLatestScenesForProject(projectId).collectAsState(initial = emptyList())

        // Логика фильтрации
        val filteredScenes = remember(scenes, searchQuery, selectedFilter) {
            scenes.filter { scene ->
                val matchesSearch = scene.sceneNumber.contains(searchQuery, ignoreCase = true) || 
                                  scene.location.contains(searchQuery, ignoreCase = true)
                val matchesFilter = when (selectedFilter) {
                    SceneFilter.ALL -> true
                    SceneFilter.MODIFIED -> scene.needsReview == 1L
                    SceneFilter.NEW -> {
                        val nowMs: Long = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
                        val scriptTimeMs: Long = scene.scriptCreatedAt
                        val diff: Long = nowMs - scriptTimeMs
                        diff < 86400000L
                    }
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
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                // Поиск
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

                // Фильтры (FilterChip)
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
                            },
                            leadingIcon = if (selectedFilter == filter) {
                                { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                            } else null
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SceneCardItem(
    scene: GetLatestScenesForProject,
    onClick: () -> Unit,
    onDiffClick: () -> Unit
) {
    val isNewScene = remember(scene.scriptCreatedAt) {
        val nowMs: Long = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        val scriptTimeMs: Long = scene.scriptCreatedAt
        val diff: Long = nowMs - scriptTimeMs
        diff < 86400000L
    }

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
                // Номер сцены и индикатор изменений
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Сцена ${scene.seriesNumber}-${scene.sceneNumber}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    if (scene.needsReview == 1L) {
                        Spacer(Modifier.width(8.dp))
                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
                            tooltip = { PlainTooltip { Text("Изменена — требует проверки") } },
                            state = rememberTooltipState()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Review needed",
                                tint = Color(0xFFFFB300),
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { onDiffClick() }
                            )
                        }
                    }
                }

                // Индикаторы: Реквизит и NEW
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (scene.hasOrphanedProps == true) {
                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
                            tooltip = { PlainTooltip { Text("Есть осиротевший реквизит") } },
                            state = rememberTooltipState()
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Inventory2,
                                    contentDescription = "Orphaned props",
                                    tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                                    modifier = Modifier.size(20.dp)
                                )
                                Icon(
                                    imageVector = Icons.Default.QuestionMark,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.size(10.dp).align(Alignment.BottomEnd)
                                )
                            }
                        }
                    }
                    
                    if (isNewScene) {
                        CineTag(
                            text = "NEW",
                            containerColor = Color(0xFFE8F5E9),
                            contentColor = Color(0xFF2E7D32)
                        )
                    }
                }
            }
            
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
