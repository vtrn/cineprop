package org.mosyagin.project.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import org.koin.core.parameter.parametersOf
import org.mosyagin.project.ui.components.ThreePaneLayout
import org.mosyagin.project.ui.components.CineCard
import org.mosyagin.project.ui.components.CineTag
import org.mosyagin.project.ui.components.InteractiveScriptText

data class SceneWorkspaceScreen(val projectId: Long) : Screen {

    @Composable
    override fun Content() {
        val screenModel = koinScreenModel<SceneWorkspaceViewModel> { parametersOf(projectId) }
        
        val scenes by screenModel.scenesList.collectAsState()
        val selectedId by screenModel.selectedSceneId.collectAsState()
        val searchQuery by screenModel.searchQuery.collectAsState()
        val selectedFilter by screenModel.selectedFilter.collectAsState()
        
        val sceneDetails by screenModel.selectedSceneDetails.collectAsState()
        val inspectorData by screenModel.selectedSceneInspector.collectAsState()

        var showAddPropDialog by remember { mutableStateOf(false) }
        var selectedWord by remember { mutableStateOf("") }

        ThreePaneLayout(
            masterPane = {
                MasterPane(
                    scenes = scenes,
                    selectedId = selectedId,
                    searchQuery = searchQuery,
                    selectedFilter = selectedFilter,
                    onSceneClick = { screenModel.onSceneSelected(it) },
                    onSearchChange = { screenModel.onSearchQueryChange(it) },
                    onFilterClick = { screenModel.onFilterSelected(it) }
                )
            },
            detailPane = {
                DetailPane(
                    content = sceneDetails?.content,
                    props = inspectorData?.props ?: emptyList(),
                    onWordLongClick = { 
                        selectedWord = it
                        showAddPropDialog = true
                    }
                )
            },
            inspectorPane = {
                InspectorPane(
                    data = inspectorData,
                    onDeleteProp = { screenModel.deleteProp(it) }
                )
            }
        )

        if (showAddPropDialog) {
            AlertDialog(
                onDismissRequest = { 
                    showAddPropDialog = false
                    selectedWord = ""
                },
                title = { Text("Добавить реквизит?") },
                text = { Text("Добавить \"$selectedWord\" в список реквизита для этой сцены?") },
                confirmButton = {
                    Button(onClick = { 
                        screenModel.addProp(selectedWord, anchor = selectedWord)
                        showAddPropDialog = false
                        selectedWord = ""
                    }) { 
                        Text("Да") 
                    }
                },
                dismissButton = {
                    TextButton(onClick = { 
                        showAddPropDialog = false
                        selectedWord = ""
                    }) { 
                        Text("Нет") 
                    }
                }
            )
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun MasterPane(
        scenes: List<org.mosyagin.project.GetLatestScenesForProject>,
        selectedId: Long?,
        searchQuery: String,
        selectedFilter: SceneFilter,
        onSceneClick: (Long) -> Unit,
        onSearchChange: (String) -> Unit,
        onFilterClick: (SceneFilter) -> Unit
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                "Сцены",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp)
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Поиск сцены или локации...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    { IconButton(onClick = { onSearchChange("") }) { Icon(Icons.Default.Close, null) } }
                } else null,
                shape = CircleShape,
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SceneFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { onFilterClick(filter) },
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

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (scenes.isEmpty()) {
                    item {
                        Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Ничего не найдено", color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }

                items(scenes) { scene ->
                    val isSelected = scene.id == selectedId
                    val isBrandNew = scene.versionCount == 1L
                    val needsReview = scene.needsReview == 1L

                    CineCard(
                        onClick = { onSceneClick(scene.id) },
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer 
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Movie,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Сцена ${scene.seriesNumber}-${scene.sceneNumber}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                    
                                    Row {
                                        if (isBrandNew) {
                                            CineTag(
                                                text = "NEW",
                                                containerColor = Color(0xFFE8F5E9),
                                                contentColor = Color(0xFF2E7D32)
                                            )
                                        }
                                        if (needsReview) {
                                            Spacer(Modifier.width(4.dp))
                                            CineTag(
                                                text = "UPD",
                                                containerColor = Color(0xFFFFF3E0),
                                                contentColor = Color(0xFFE65100)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    scene.location,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer 
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun DetailPane(
        content: String?,
        props: List<org.mosyagin.project.models.versioning.Prop>,
        onWordLongClick: (String) -> Unit
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopStart) {
            if (content != null) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        InteractiveScriptText(
                            fullText = content,
                            props = props,
                            onWordLongClick = onWordLongClick
                        )
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Выберите сцену в левой панели",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }

    @Composable
    private fun InspectorPane(
        data: SceneInspectorData?,
        onDeleteProp: (Long) -> Unit
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            Text(
                "Инспектор",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            if (data != null) {
                Text("Персонажи", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                data.actors.forEach { actor ->
                    ListItem(
                        headlineContent = { Text(actor.name) },
                        leadingContent = { Icon(Icons.Default.Person, null, modifier = Modifier.size(18.dp)) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
                
                Spacer(Modifier.height(32.dp))
                
                Text("Реквизит", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                data.props.forEach { prop ->
                    ListItem(
                        headlineContent = { Text(prop.name) },
                        leadingContent = { Icon(Icons.Default.Inventory, null, modifier = Modifier.size(18.dp)) },
                        trailingContent = {
                            IconButton(onClick = { onDeleteProp(prop.id) }) {
                                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            } else {
                Text(
                    "Нет данных для инспектора",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}
