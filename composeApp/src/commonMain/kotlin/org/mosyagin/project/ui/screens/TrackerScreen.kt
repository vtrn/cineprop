package org.mosyagin.project.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.flow.StateFlow
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import org.mosyagin.project.ui.components.AppLayoutType
import org.mosyagin.project.ui.components.CineCard
import org.mosyagin.project.ui.components.LocalAppLayoutType
import org.mosyagin.project.ui.components.ThreePaneLayout
import org.mosyagin.project.ui.components.InteractiveScriptText
import org.mosyagin.project.repository.ShiftRepository

data class TrackerScreen(val projectId: Long) : Screen {

    @Composable
    override fun Content() {
        val layoutType = LocalAppLayoutType.current
        
        if (layoutType == AppLayoutType.DESKTOP) {
            TrackerDesktopContent(projectId)
        } else {
            TrackerMobileContent(projectId)
        }
    }

    @Composable
    private fun TrackerDesktopContent(projectId: Long) {
        val screenModel = koinScreenModel<TrackerViewModel> { parametersOf(projectId) }
        
        val shifts by screenModel.shifts.collectAsState()
        val expandedShiftIds by screenModel.expandedShiftIds.collectAsState()
        val selectedSceneId by screenModel.selectedSceneId.collectAsState()
        val sceneDetails by screenModel.selectedSceneDetails.collectAsState()
        val inspectorData by screenModel.selectedSceneInspector.collectAsState()

        var showAddPropDialog by remember { mutableStateOf(false) }
        var selectedWord by remember { mutableStateOf("") }

        ThreePaneLayout(
            masterPane = {
                Column(modifier = Modifier.fillMaxSize()) {
                    Text(
                        "Смены и сцены",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(shifts) { shift ->
                            val isExpanded = expandedShiftIds.contains(shift.id)
                            ShiftExpandableItem(
                                shift = shift,
                                isExpanded = isExpanded,
                                selectedSceneId = selectedSceneId,
                                onShiftClick = { screenModel.toggleShift(shift.id) },
                                onSceneClick = { screenModel.onSceneSelected(it) },
                                getScenesFlow = { screenModel.getScenesForShift(shift.id) }
                            )
                        }
                    }
                }
            },
            detailPane = {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopStart) {
                    if (sceneDetails != null) {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            item {
                                InteractiveScriptText(
                                    fullText = sceneDetails!!.content,
                                    props = inspectorData?.props ?: emptyList(),
                                    onWordLongClick = {
                                        selectedWord = it
                                        showAddPropDialog = true
                                    }
                                )
                            }
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Выберите сцену в списке слева", color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            },
            inspectorPane = {
                Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                    Text("Инспектор", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(24.dp))
                    
                    if (inspectorData != null) {
                        Text("Персонажи", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        inspectorData!!.actors.forEach { actor ->
                            ListItem(
                                headlineContent = { Text(actor.name) },
                                leadingContent = { Icon(Icons.Default.Person, null, modifier = Modifier.size(18.dp)) },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            )
                        }
                        
                        Spacer(Modifier.height(32.dp))
                        
                        Text("Реквизит", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        inspectorData!!.props.forEach { prop ->
                            ListItem(
                                headlineContent = { Text(prop.name) },
                                leadingContent = { Icon(Icons.Default.Inventory, null, modifier = Modifier.size(18.dp)) },
                                trailingContent = {
                                    IconButton(onClick = { screenModel.deleteProp(prop.id) }) {
                                        Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                                    }
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            )
                        }
                    } else {
                        Text("Данные не выбраны", color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        )

        if (showAddPropDialog) {
            AlertDialog(
                onDismissRequest = { showAddPropDialog = false },
                title = { Text("Добавить реквизит?") },
                text = { Text("Добавить \"$selectedWord\" в список этой сцены?") },
                confirmButton = {
                    Button(onClick = {
                        screenModel.addProp(selectedWord)
                        showAddPropDialog = false
                    }) { Text("Да") }
                },
                dismissButton = {
                    TextButton(onClick = { showAddPropDialog = false }) { Text("Нет") }
                }
            )
        }
    }

    @Composable
    private fun ShiftExpandableItem(
        shift: org.mosyagin.project.Shift,
        isExpanded: Boolean,
        selectedSceneId: Long?,
        onShiftClick: () -> Unit,
        onSceneClick: (Long) -> Unit,
        getScenesFlow: () -> StateFlow<List<org.mosyagin.project.GetScenesForShift>>
    ) {
        Column {
            CineCard(
                onClick = onShiftClick,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CalendarToday, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Смена №${shift.shiftNumber}", fontWeight = FontWeight.Bold)
                        Text(shift.date, style = MaterialTheme.typography.bodySmall)
                    }
                    Icon(if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
                }
            }
            
            AnimatedVisibility(visible = isExpanded) {
                val scenes by getScenesFlow().collectAsState()
                Column(modifier = Modifier.padding(start = 16.dp, top = 4.dp)) {
                    scenes.forEach { scene ->
                        val isSelected = scene.id == selectedSceneId
                        ListItem(
                            headlineContent = { Text("Сцена ${scene.seriesNumber}-${scene.sceneNumber}") },
                            supportingContent = { Text(scene.location, maxLines = 1) },
                            leadingContent = { Icon(Icons.Default.Movie, null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.clickable { onSceneClick(scene.id) },
                            colors = ListItemDefaults.colors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) 
                                                else Color.Transparent
                            )
                        )
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun TrackerMobileContent(projectId: Long) {
        val navigator = LocalNavigator.currentOrThrow
        val repository = koinInject<ShiftRepository>()
        val shifts by repository.getShiftsByProject(projectId).collectAsState(initial = emptyList())

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(title = { Text("Трекер смен") })
            }
        ) { padding ->
            LazyColumn(modifier = Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
                items(shifts) { shift ->
                    ListItem(
                        headlineContent = { Text("Смена №${shift.shiftNumber}") },
                        supportingContent = { Text(shift.date) },
                        modifier = Modifier.clickable { navigator.push(ShiftDetailScreen(shift.id)) }
                    )
                }
            }
        }
    }
}
