package org.mosyagin.project.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.koin.core.parameter.parametersOf

data class SceneDetailScreen(
    val sceneUserDataId: Long, 
    val projectId: Long,
    val scriptFileId: Long
) : Screen {

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = getScreenModel<SceneDetailScreenModel> { 
            parametersOf(sceneUserDataId, scriptFileId) 
        }

        val scene by screenModel.scene.collectAsState()
        val actors by screenModel.actors.collectAsState()
        val props by screenModel.props.collectAsState()

        var showProps by remember { mutableStateOf(true) }
        var showActors by remember { mutableStateOf(true) }
        var showScript by remember { mutableStateOf(true) }
        var showAddPropDialog by remember { mutableStateOf(false) }
        var selectedWord by remember { mutableStateOf("") }
        var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(scene?.let { "Сцена ${it.seriesNumber}-${it.sceneNumber}" } ?: "Загрузка...") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                        }
                    },
                    actions = {
                        // Если сцена требует проверки, показываем кнопку сравнения
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
        ) { padding ->
            scene?.let { currentScene ->
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "${if (currentScene.isInterior == 1L) "ИНТ." else "НАТ."} ${currentScene.location} — ${currentScene.timeOfDay}",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Вкладка Персонажи
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column {
                            Row(modifier = Modifier.fillMaxWidth().clickable { showActors = !showActors }.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("ПЕРСОНАЖИ (${actors.size})", style = MaterialTheme.typography.titleMedium)
                                Icon(if (showActors) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null)
                            }
                            AnimatedVisibility(visible = showActors) {
                                FlowRow(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    actors.forEach { SuggestionChip(onClick = {}, label = { Text(it.name) }) }
                                }
                            }
                        }
                    }

                    // Вкладка РЕКВИЗИТ
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column {
                            Row(modifier = Modifier.fillMaxWidth().clickable { showProps = !showProps }.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("РЕКВИЗИТ (${props.size})", style = MaterialTheme.typography.titleMedium)
                                Icon(if (showProps) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null)
                            }
                            AnimatedVisibility(visible = showProps) {
                                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    props.forEach { prop ->
                                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text(prop.name, style = MaterialTheme.typography.bodyLarge)
                                            IconButton(onClick = { screenModel.deleteProp(prop.id) }) {
                                                Icon(Icons.Default.Delete, null, tint = Color.Red.copy(alpha = 0.7f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Вкладка Текст сценария
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column {
                            Row(modifier = Modifier.fillMaxWidth().clickable { showScript = !showScript }.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("ТЕКСТ СЦЕНАРИЯ", style = MaterialTheme.typography.titleMedium)
                                Icon(if (showScript) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null)
                            }
                            AnimatedVisibility(visible = showScript) {
                                val annotatedString = buildAnnotatedString {
                                    append(currentScene.content)
                                    props.forEach { prop ->
                                        val index = currentScene.content.indexOf(prop.name, ignoreCase = true)
                                        if (index != -1) {
                                            addStyle(SpanStyle(color = Color(0xFFFF9800), fontWeight = FontWeight.Bold), index, index + prop.name.length)
                                        }
                                    }
                                }
                                BasicText(
                                    text = annotatedString,
                                    onTextLayout = { textLayoutResult = it },
                                    modifier = Modifier.padding(16.dp).pointerInput(Unit) {
                                        detectTapGestures(onLongPress = { offset ->
                                            textLayoutResult?.let { layout ->
                                                val position = layout.getOffsetForPosition(offset)
                                                val word = getWordAtPosition(currentScene.content, position)
                                                if (word.isNotBlank()) {
                                                    selectedWord = word
                                                    showAddPropDialog = true
                                                }
                                            }
                                        })
                                    },
                                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showAddPropDialog) {
            AlertDialog(
                onDismissRequest = { showAddPropDialog = false },
                title = { Text("Добавить реквизит?") },
                text = { Text("Добавить \"$selectedWord\" в список?") },
                confirmButton = {
                    Button(onClick = { screenModel.addProp(selectedWord); showAddPropDialog = false }) { Text("Да") }
                }
            )
        }
    }
}

fun getWordAtPosition(text: String, position: Int): String {
    val start = text.lastIndexOfAny(charArrayOf(' ', '\n', '.', ','), position - 1).let { if (it == -1) 0 else it + 1 }
    val end = text.indexOfAny(charArrayOf(' ', '\n', '.', ',', '!'), position).let { if (it == -1) text.length else it }
    return text.substring(start, end).trim { !it.isLetterOrDigit() }
}
