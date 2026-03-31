package org.mosyagin.project.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.launch
import org.koin.core.parameter.parametersOf
import org.mosyagin.project.Actor
import org.mosyagin.project.models.versioning.Prop
import org.mosyagin.project.ui.components.AppLayoutType
import org.mosyagin.project.ui.components.LocalAppLayoutType

data class SceneDetailScreen(
    val sceneUserDataId: Long,
    val projectId: Long,
    val scriptFileId: Long?
) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = koinScreenModel<SceneDetailScreenModel> {
            parametersOf(sceneUserDataId, scriptFileId ?: 0L) 
        }
        val layoutType = LocalAppLayoutType.current
        val clipboardManager = LocalClipboardManager.current
        val scope = rememberCoroutineScope()
        val listState = rememberLazyListState()

        val scene by screenModel.scene.collectAsState()
        val actors by screenModel.actors.collectAsState()
        val props by screenModel.props.collectAsState()
        val scriptBlocks by screenModel.scriptBlocks.collectAsState()
        val selectedPropId by screenModel.selectedPropId.collectAsState()

        var showAddPropDialog by remember { mutableStateOf(false) }
        var showSelectionPopup by remember { mutableStateOf(false) }
        var selectedAnchor by remember { mutableStateOf("") }
        var propNameInput by remember { mutableStateOf("") }

        // Состояние сворачивания инспектора для мобилок
        val isCollapsed by remember {
            derivedStateOf {
                listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 50
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(scene?.let { "Сцена ${it.seriesNumber}-${it.sceneNumber}" } ?: "Загрузка...") },
                        navigationIcon = {
                            if (layoutType == AppLayoutType.MOBILE) {
                                IconButton(onClick = { navigator.pop() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                                }
                            }
                        },
                        actions = {
                            if (scene?.needsReview == 1L) {
                                TextButton(
                                    onClick = { navigator.push(SceneDiffScreen(sceneUserDataId)) },
                                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF9800))
                                ) {
                                    Icon(Icons.Default.Difference, null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Изменения")
                                }
                            }
                        }
                    )
                }
            ) { paddingValues ->
                if (layoutType == AppLayoutType.MOBILE) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        // Динамический инспектор
                        Surface(
                            shadowElevation = if (isCollapsed) 4.dp else 0.dp,
                            tonalElevation = if (isCollapsed) 2.dp else 0.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            AnimatedContent(
                                targetState = isCollapsed,
                                transitionSpec = {
                                    fadeIn() + expandVertically() togetherWith fadeOut() + shrinkVertically()
                                }
                            ) { collapsed ->
                                if (collapsed) {
                                    // СВЕРНУТЫЙ ВИД (одна строка)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Info, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = "${scene?.location ?: ""} • ${actors.size} акт. • ${props.size} рекв.",
                                            style = MaterialTheme.typography.labelMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(onClick = { scope.launch { listState.animateScrollToItem(0) } }) {
                                            Icon(Icons.Default.ExpandMore, null, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                } else {
                                    // ПОЛНЫЙ ВИД
                                    InspectorContent(
                                        scene = scene,
                                        actors = actors,
                                        props = props,
                                        selectedPropId = selectedPropId,
                                        onPropClick = { prop ->
                                            screenModel.setSelectedProp(prop.id)
                                            val blockIndex = scriptBlocks.indexOfFirst { it.text.lowercase().contains(prop.anchor.lowercase()) }
                                            if (blockIndex != -1) {
                                                scope.launch { listState.animateScrollToItem(blockIndex) }
                                            }
                                        },
                                        onDeleteProp = { screenModel.deleteProp(it) },
                                        modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()).fillMaxHeight(0.4f)
                                    )
                                }
                            }
                        }

                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                        
                        // Текст сценария занимает всё оставшееся место
                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            InteractiveScriptViewer(
                                blocks = scriptBlocks,
                                props = props,
                                selectedPropId = selectedPropId,
                                onPropClick = { id -> screenModel.setSelectedProp(id) },
                                onTextSelected = { text ->
                                    if (text.isNotBlank()) {
                                        selectedAnchor = text
                                        showSelectionPopup = true
                                    }
                                },
                                modifier = Modifier.fillMaxSize(),
                                listState = listState,
                                layoutType = layoutType
                            )
                        }
                    }
                } else {
                    // ДЕСКТОПНАЯ ВЕРСИЯ (без изменений)
                    Row(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                        Column(
                            modifier = Modifier.weight(0.7f).fillMaxHeight().padding(16.dp)
                        ) {
                            Card(
                                modifier = Modifier.fillMaxSize(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column {
                                    Text("ТЕКСТ СЦЕНАРИЯ", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        InteractiveScriptViewer(
                                            blocks = scriptBlocks,
                                            props = props,
                                            selectedPropId = selectedPropId,
                                            onPropClick = { id -> screenModel.setSelectedProp(id) },
                                            onTextSelected = { text ->
                                                if (text.isNotBlank()) {
                                                    selectedAnchor = text
                                                    showSelectionPopup = true
                                                }
                                            },
                                            modifier = Modifier.fillMaxSize(),
                                            listState = listState,
                                            layoutType = layoutType
                                        )
                                    }
                                }
                            }
                        }

                        Column(
                            modifier = Modifier.weight(0.3f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            InspectorContent(
                                scene = scene,
                                actors = actors,
                                props = props,
                                selectedPropId = selectedPropId,
                                onPropClick = { prop ->
                                    screenModel.setSelectedProp(prop.id)
                                    val blockIndex = scriptBlocks.indexOfFirst { it.text.lowercase().contains(prop.anchor.lowercase()) }
                                    if (blockIndex != -1) {
                                        scope.launch { listState.animateScrollToItem(blockIndex) }
                                    }
                                },
                                onDeleteProp = { screenModel.deleteProp(it) }
                            )
                        }
                    }
                }
            }

            // Попапы
            if (showSelectionPopup) {
                Popup(alignment = Alignment.Center, onDismissRequest = { showSelectionPopup = false }) {
                    Card(
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Добавить реквизит?", style = MaterialTheme.typography.titleMedium)
                            Text("\"$selectedAnchor\"", style = MaterialTheme.typography.bodySmall, maxLines = 1)
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { showSelectionPopup = false }) { Text("Нет") }
                                Button(onClick = {
                                    showSelectionPopup = false
                                    propNameInput = selectedAnchor
                                    showAddPropDialog = true
                                }) { Text("Да") }
                            }
                        }
                    }
                }
            }
        }

        if (showAddPropDialog) {
            AlertDialog(
                onDismissRequest = { showAddPropDialog = false },
                title = { Text("Новый реквизит") },
                text = {
                    Column {
                        Text("Текст в сценарии:", style = MaterialTheme.typography.labelSmall)
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
                    Button(
                        onClick = { 
                            screenModel.addProp(propNameInput.ifBlank { selectedAnchor }, anchor = selectedAnchor)
                            showAddPropDialog = false
                            propNameInput = ""
                        },
                        enabled = propNameInput.isNotBlank() || selectedAnchor.isNotBlank()
                    ) { Text("Сохранить") }
                },
                dismissButton = {
                    TextButton(onClick = { showAddPropDialog = false }) { Text("Отмена") }
                }
            )
        }
    }

    @Composable
    private fun InspectorContent(
        scene: org.mosyagin.project.GetSceneById?,
        actors: List<Actor>,
        props: List<Prop>,
        selectedPropId: Long?,
        onPropClick: (Prop) -> Unit,
        onDeleteProp: (Long) -> Unit,
        modifier: Modifier = Modifier
    ) {
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("ИНСПЕКТОР", style = MaterialTheme.typography.titleLarge)

            scene?.let {
                Text(
                    text = "${if (it.isInterior == 1L) "ИНТ." else "НАТ."} ${it.location} — ${it.timeOfDay}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("ПЕРСОНАЖИ", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    actors.forEach { actor ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                            Icon(Icons.Default.Person, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(actor.name, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            Text("РЕКВИЗИТ", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            props.forEach { prop ->
                val isSelected = prop.id == selectedPropId
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPropClick(prop) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(prop.name, style = MaterialTheme.typography.bodyMedium, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                            Text(prop.anchor, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }
                        IconButton(onClick = { onDeleteProp(prop.id) }) {
                            Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}
