package org.mosyagin.project.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.launch
import org.koin.core.parameter.parametersOf
import org.mosyagin.project.ui.components.AppLayoutType
import org.mosyagin.project.ui.components.LocalAppLayoutType

data class SceneDetailScreen(
    val sceneUserDataId: Long,
    val projectId: Long,
    val scriptFileId: Long?
) : Screen {

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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

        val hasOrphanedProps = props.any { it.isOrphaned }

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
                            if (layoutType == AppLayoutType.DESKTOP) {
                                IconButton(onClick = {
                                    val text = clipboardManager.getText()?.text ?: ""
                                    if (text.isNotBlank()) {
                                        selectedAnchor = text
                                        propNameInput = ""
                                        showAddPropDialog = true
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.AddBox,
                                        contentDescription = "Разметить реквизит",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Surface(
                                    color = MaterialTheme.colorScheme.tertiaryContainer,
                                    shape = MaterialTheme.shapes.small,
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Text(
                                        "ЭТО ДЕСКТОП",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
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
                Row(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                    // Левая/Центральная панель: Текст сценария
                    Column(
                        modifier = Modifier.weight(0.7f).fillMaxHeight().padding(16.dp)
                    ) {
                        Card(
                            modifier = Modifier.fillMaxSize(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("ТЕКСТ СЦЕНАРИЯ", style = MaterialTheme.typography.titleMedium)
                                    if (layoutType == AppLayoutType.DESKTOP) {
                                        Text("Выделите текст, чтобы разметить реквизит", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    }
                                }
                                
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
                                        listState = listState
                                    )
                                }
                            }
                        }
                    }

                    // Правая панель: Инспектор (weight 0.3f)
                    Column(
                        modifier = Modifier.weight(0.3f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("ИНСПЕКТОР", style = MaterialTheme.typography.titleLarge)

                        // Метаданные сцены
                        scene?.let {
                            Text(
                                text = "${if (it.isInterior == 1L) "ИНТ." else "НАТ."} ${it.location} — ${it.timeOfDay}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Персонажи
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

                        // Реквизит
                        Text("РЕКВИЗИТ", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        props.forEach { prop ->
                            val isSelected = prop.id == selectedPropId
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { 
                                        screenModel.setSelectedProp(prop.id)
                                        // Скролл к блоку с этим реквизитом
                                        val blockIndex = scriptBlocks.indexOfFirst { it.text.lowercase().contains(prop.anchor.lowercase()) }
                                        if (blockIndex != -1) {
                                            scope.launch { listState.animateScrollToItem(blockIndex) }
                                        }
                                    },
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
                                        Text(
                                            prop.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                        Text(prop.anchor, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    }
                                    IconButton(onClick = { screenModel.deleteProp(prop.id) }) {
                                        Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Попап "Добавить реквизит?"
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
}
