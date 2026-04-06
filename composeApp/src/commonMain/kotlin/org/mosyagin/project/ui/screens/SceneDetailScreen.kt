package org.mosyagin.project.ui.screens

import androidx.compose.animation.*
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
import androidx.compose.ui.text.font.FontWeight
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
    val sceneUserDataId: String, // UUID
    val projectId: String,
    val scriptFileId: String?
) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = koinScreenModel<SceneDetailScreenModel> {
            parametersOf(sceneUserDataId, scriptFileId ?: "") 
        }
        val layoutType = LocalAppLayoutType.current
        val scope = rememberCoroutineScope()
        val listState = rememberLazyListState()

        val sceneData by screenModel.sceneData.collectAsState()
        val actors by screenModel.actors.collectAsState()
        val props by screenModel.props.collectAsState()
        
        // Получаем блоки сценария из модели (если они там есть) или парсим контент
        // Для восстановления функционала предположим, что модель предоставляет контент
        val scriptContent = sceneData?.content ?: ""
        
        var selectedPropId by remember { mutableStateOf<String?>(null) }
        var showAddPropDialog by remember { mutableStateOf(false) }
        var showSelectionPopup by remember { mutableStateOf(false) }
        var selectedAnchor by remember { mutableStateOf("") }
        var propNameInput by remember { mutableStateOf("") }

        val isCollapsed by remember {
            derivedStateOf {
                listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 50
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(sceneData?.let { "Сцена ${it.seriesNumber}-${it.sceneNumber}" } ?: "Загрузка...") },
                        navigationIcon = {
                            if (layoutType == AppLayoutType.MOBILE) {
                                IconButton(onClick = { navigator.pop() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                                }
                            }
                        },
                        actions = {
                            if (sceneData?.needsReview == 1L) {
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
                        Surface(
                            shadowElevation = if (isCollapsed) 4.dp else 0.dp,
                            tonalElevation = if (isCollapsed) 2.dp else 0.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            AnimatedContent(
                                targetState = isCollapsed
                            ) { collapsed ->
                                if (collapsed) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp, 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${sceneData?.location ?: ""} • ${actors.size} акт. • ${props.size} рекв.",
                                            style = MaterialTheme.typography.labelMedium,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1
                                        )
                                        IconButton(onClick = { scope.launch { listState.animateScrollToItem(0) } }) {
                                            Icon(Icons.Default.ExpandMore, null)
                                        }
                                    }
                                } else {
                                    InspectorContent(
                                        scene = sceneData,
                                        actors = actors,
                                        props = props,
                                        selectedPropId = selectedPropId,
                                        onPropClick = { prop ->
                                            selectedPropId = prop.id
                                            // Скроллинг к якорю можно реализовать, если есть блоки
                                        },
                                        onDeleteProp = { screenModel.deleteProp(it) },
                                        modifier = Modifier.padding(16.dp).fillMaxHeight(0.4f).verticalScroll(rememberScrollState())
                                    )
                                }
                            }
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            // Здесь должен быть InteractiveScriptViewer. 
                            // Если у вас есть доступ к блокам, передайте их. 
                            // Пока используем заглушку текста для компиляции.
                            Text(scriptContent, modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()))
                        }
                    }
                } else {
                    Row(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                        Box(modifier = Modifier.weight(0.7f).fillMaxHeight().padding(16.dp)) {
                            Card(modifier = Modifier.fillMaxSize()) {
                                Column {
                                    Text("ТЕКСТ СЦЕНАРИЯ", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
                                    Box(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
                                        Text(scriptContent)
                                    }
                                }
                            }
                        }

                        Column(
                            modifier = Modifier.weight(0.3f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            InspectorContent(
                                scene = sceneData,
                                actors = actors,
                                props = props,
                                selectedPropId = selectedPropId,
                                onPropClick = { selectedPropId = it.id },
                                onDeleteProp = { screenModel.deleteProp(it) }
                            )
                        }
                    }
                }
            }

            if (showSelectionPopup) {
                Popup(alignment = Alignment.Center, onDismissRequest = { showSelectionPopup = false }) {
                    Card(elevation = CardDefaults.cardElevation(8.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Добавить реквизит?", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(12.dp))
                            Row {
                                TextButton(onClick = { showSelectionPopup = false }) { Text("Нет") }
                                Button(onClick = {
                                    showSelectionPopup = false
                                    propNameInput = ""
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
                    OutlinedTextField(
                        value = propNameInput,
                        onValueChange = { propNameInput = it },
                        label = { Text("Название предмета") },
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        // Метод addProp в ScreenModel должен принимать String
                        // screenModel.addProp(propNameInput.ifBlank { selectedAnchor }, selectedAnchor)
                        showAddPropDialog = false
                    }) { Text("Сохранить") }
                }
            )
        }
    }

    @Composable
    private fun InspectorContent(
        scene: org.mosyagin.project.GetSceneById?,
        actors: List<Actor>,
        props: List<Prop>,
        selectedPropId: String?,
        onPropClick: (Prop) -> Unit,
        onDeleteProp: (String) -> Unit,
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
                    actors.forEach { actor ->
                        ListItem(
                            headlineContent = { Text(actor.name) },
                            leadingContent = { Icon(Icons.Default.Person, null, modifier = Modifier.size(16.dp)) }
                        )
                    }
                }
            }

            Text("РЕКВИЗИТ", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            props.forEach { prop ->
                val isSelected = prop.id == selectedPropId
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onPropClick(prop) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(prop.name, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
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
