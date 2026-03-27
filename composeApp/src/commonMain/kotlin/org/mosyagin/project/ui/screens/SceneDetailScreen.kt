package org.mosyagin.project.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Difference
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.RunningWithErrors
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.koin.core.parameter.parametersOf

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

        val scene by screenModel.scene.collectAsState()
        val actors by screenModel.actors.collectAsState()
        val props by screenModel.props.collectAsState()

        var showProps by remember { mutableStateOf(true) }
        var showActors by remember { mutableStateOf(true) }
        var showScript by remember { mutableStateOf(true) }
        var showAddPropDialog by remember { mutableStateOf(false) }
        var selectedWord by remember { mutableStateOf("") }
        var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

        val hasOrphanedProps = props.any { it.isOrphaned }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(scene?.let { "Сцена ${it.seriesNumber}-${it.sceneNumber}" } ?: "Загрузка...") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
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
            scene?.let { currentScene ->
                Column(
                    modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "${if (currentScene.isInterior == 1L) "ИНТ." else "НАТ."} ${currentScene.location} — ${currentScene.timeOfDay}",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Баннер о сиротских реквизитах
                    if (hasOrphanedProps) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF9800))
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    "Некоторые предметы больше не упоминаются в новой версии сценария. Проверьте их актуальность.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF6D4C41)
                                )
                            }
                        }
                    }

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
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { showProps = !showProps }.padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("РЕКВИЗИТ (${props.size})", style = MaterialTheme.typography.titleMedium)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (hasOrphanedProps) {
                                        TextButton(onClick = { screenModel.confirmAllProps() }) {
                                            Text("Оставить всё", style = MaterialTheme.typography.labelLarge)
                                        }
                                    }
                                    Icon(if (showProps) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null)
                                }
                            }
                            AnimatedVisibility(visible = showProps) {
                                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                    props.forEach { prop ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                if (prop.isOrphaned) {
                                                    Icon(
                                                        Icons.Default.RunningWithErrors,
                                                        contentDescription = null,
                                                        tint = Color(0xFFFF9800),
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Spacer(Modifier.width(8.dp))
                                                }
                                                
                                                Column {
                                                    Text(
                                                        prop.name,
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        color = if (prop.isOrphaned) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) 
                                                                else MaterialTheme.colorScheme.onSurface
                                                    )
                                                    if (prop.isOrphaned) {
                                                        Text(
                                                            "Не найден в тексте",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = Color(0xFFFF9800)
                                                        )
                                                    }
                                                }
                                            }
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
                                            addStyle(
                                                SpanStyle(
                                                    color = if (prop.isOrphaned) Color.Gray else Color(0xFFFF9800),
                                                    fontWeight = FontWeight.Bold,
                                                    textDecoration = if (prop.isOrphaned) TextDecoration.LineThrough else TextDecoration.None
                                                ),
                                                index,
                                                index + prop.name.length
                                            )
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
                onDismissRequest = { },
                title = { Text("Добавить реквизит?") },
                text = { Text("Добавить \"$selectedWord\" в список?") },
                confirmButton = {
                    Button(onClick = { screenModel.addProp(selectedWord); }) { Text("Да") }
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
