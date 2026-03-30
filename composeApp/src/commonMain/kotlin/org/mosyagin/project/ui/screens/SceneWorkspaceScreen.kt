package org.mosyagin.project.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import kotlinx.coroutines.launch
import org.koin.core.parameter.parametersOf
import org.mosyagin.project.GetLatestScenesForProject
import org.mosyagin.project.models.versioning.Prop
import org.mosyagin.project.ui.components.ThreePaneLayout
import org.mosyagin.project.ui.components.CineCard
import org.mosyagin.project.ui.components.CineTag
import org.mosyagin.project.parser.ScriptParser
import org.mosyagin.project.ui.components.AppLayoutType
import org.mosyagin.project.ui.components.LocalAppLayoutType

data class SceneWorkspaceScreen(val projectId: Long) : Screen {

    @Composable
    override fun Content() {
        val screenModel = koinScreenModel<SceneWorkspaceViewModel> { parametersOf(projectId) }
        val layoutType = LocalAppLayoutType.current
        val scope = rememberCoroutineScope()
        val listState = rememberLazyListState()
        
        val scenes by screenModel.scenesList.collectAsState()
        val selectedId by screenModel.selectedSceneId.collectAsState()
        val searchQuery by screenModel.searchQuery.collectAsState()
        val selectedFilter by screenModel.selectedFilter.collectAsState()
        
        val sceneDetails by screenModel.selectedSceneDetails.collectAsState()
        val inspectorData by screenModel.selectedSceneInspector.collectAsState()

        var showAddPropDialog by remember { mutableStateOf(false) }
        var showSelectionPopup by remember { mutableStateOf(false) }
        var selectedAnchor by remember { mutableStateOf("") }
        var propNameInput by remember { mutableStateOf("") }

        Box(modifier = Modifier.fillMaxSize()) {
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
                        selectedPropId = null,
                        onPropClick = { /* Логика выделения */ },
                        onTextSelected = { text ->
                            if (text.isNotBlank()) {
                                selectedAnchor = text
                                showSelectionPopup = true
                            }
                        },
                        listState = listState
                    )
                },
                inspectorPane = {
                    InspectorPane(
                        data = inspectorData,
                        layoutType = layoutType,
                        onDeleteProp = { screenModel.deleteProp(it) },
                        onMarkupClick = {
                            // Оставляем как запасной вариант (через буфер)
                        },
                        onPropSelected = { prop ->
                            val content = sceneDetails?.content
                            if (content != null) {
                                val blocks = ScriptParser().parseBlocks(content)
                                val index = blocks.indexOfFirst { it.text.lowercase().contains(prop.anchor.lowercase()) }
                                if (index != -1) {
                                    scope.launch { listState.animateScrollToItem(index) }
                                }
                            }
                        }
                    )
                }
            )

            // Попап "Добавить реквизит?" при выделении текста
            if (showSelectionPopup) {
                Popup(
                    alignment = Alignment.Center,
                    onDismissRequest = { showSelectionPopup = false }
                ) {
                    Card(
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Добавить реквизит?", style = MaterialTheme.typography.titleMedium)
                            Text("\"$selectedAnchor\"", style = MaterialTheme.typography.bodySmall, maxLines = 1)
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { showSelectionPopup = false }) {
                                    Text("Нет")
                                }
                                Button(onClick = {
                                    showSelectionPopup = false
                                    propNameInput = selectedAnchor
                                    showAddPropDialog = true
                                }) {
                                    Text("Да")
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showAddPropDialog) {
            AlertDialog(
                onDismissRequest = { 
                    showAddPropDialog = false
                    selectedAnchor = ""
                },
                title = { Text("Новый реквизит") },
                text = {
                    Column {
                        Text("Текст из сценария (anchor):", style = MaterialTheme.typography.labelSmall)
                        Text("\"$selectedAnchor\"", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(
                            value = propNameInput,
                            onValueChange = { propNameInput = it },
                            label = { Text("Название предмета") },
                            placeholder = { Text("Напр: Клетки") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = { 
                        screenModel.addProp(propNameInput.ifBlank { selectedAnchor }, anchor = selectedAnchor)
                        showAddPropDialog = false
                        selectedAnchor = ""
                        propNameInput = ""
                    }) { 
                        Text("Сохранить") 
                    }
                },
                dismissButton = {
                    TextButton(onClick = { 
                        showAddPropDialog = false
                        selectedAnchor = ""
                    }) { 
                        Text("Отмена") 
                    }
                }
            )
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun MasterPane(
        scenes: List<GetLatestScenesForProject>,
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
        props: List<Prop>,
        selectedPropId: Long?,
        onPropClick: (Long) -> Unit,
        onTextSelected: (String) -> Unit,
        listState: androidx.compose.foundation.lazy.LazyListState
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (content != null) {
                val blocks = remember(content) { ScriptParser().parseBlocks(content) }
                InteractiveScriptViewer(
                    blocks = blocks,
                    props = props,
                    selectedPropId = selectedPropId,
                    onPropClick = onPropClick,
                    onTextSelected = onTextSelected,
                    modifier = Modifier.fillMaxSize(),
                    listState = listState
                )
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
        layoutType: AppLayoutType,
        onDeleteProp: (Long) -> Unit,
        onMarkupClick: () -> Unit,
        onPropSelected: (Prop) -> Unit
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Инспектор",
                    style = MaterialTheme.typography.titleLarge
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (layoutType == AppLayoutType.DESKTOP) {
                        IconButton(onClick = onMarkupClick) {
                            Icon(
                                Icons.Default.AddBox,
                                contentDescription = "Разметить",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                "ЭТО ДЕСКТОП",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            }
            
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
                        supportingContent = if (prop.anchor != prop.name) { { Text(prop.anchor) } } else null,
                        leadingContent = { Icon(Icons.Default.Inventory, null, modifier = Modifier.size(18.dp)) },
                        trailingContent = {
                            IconButton(onClick = { onDeleteProp(prop.id) }) {
                                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable { onPropSelected(prop) }
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
