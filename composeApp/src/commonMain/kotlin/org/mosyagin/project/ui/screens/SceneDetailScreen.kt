package org.mosyagin.project.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.mosyagin.project.db.LocalDatabaseQueries

data class SceneDetailScreen(val sceneId: Long, val projectId: Long) : Screen {

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val queries = LocalDatabaseQueries.current

        val screenModel = rememberScreenModel { SceneDetailScreenModel(queries, sceneId) }
        val scene by screenModel.scene.collectAsState()
        val actors by screenModel.actors.collectAsState()

        // Состояния для раскрывающихся списков (по умолчанию текст открыт, персонажи открыты)
        var showActors by remember { mutableStateOf(false) }
        var showScript by remember { mutableStateOf(false) }


        // Добавь эти переменные:
        val props by screenModel.props.collectAsState()
        var showProps by remember { mutableStateOf(true) }

        // Переменные для диалогового окна добавления реквизита
        var showAddPropDialog by remember { mutableStateOf(false) }
        var newPropName by remember { mutableStateOf("") }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(scene?.let { "Сцена ${it.sceneNumber}" } ?: "Загрузка...") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                        }
                    }
                )
            }
        ) { padding ->
            scene?.let { currentScene ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Заголовок сцены (Slugline)
                    Text(
                        text = "${if (currentScene.isInterior == 1L) "ИНТ." else "НАТ."} ${currentScene.location} — ${currentScene.timeOfDay}",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // 2. Вкладка: ПЕРСОНАЖИ
                    Card(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column {
                            // Кликабельная шапка вкладки
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showActors = !showActors }
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("ПЕРСОНАЖИ (${actors.size})", style = MaterialTheme.typography.titleMedium)
                                Icon(
                                    imageVector = if (showActors) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Свернуть/Развернуть"
                                )
                            }

                            // Анимированное содержимое
                            AnimatedVisibility(
                                visible = showActors,
                                enter = expandVertically(animationSpec = tween(300)),
                                exit = shrinkVertically(animationSpec = tween(300))
                            ) {
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (actors.isEmpty()) {
                                        Text("Нет персонажей", color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))
                                    } else {
                                        actors.forEach { actor ->
                                            SuggestionChip(
                                                onClick = { },
                                                label = { Text(actor.name) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. Вкладка: ТЕКСТ СЦЕНЫ
                    Card(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column {
                            // Кликабельная шапка вкладки
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showScript = !showScript }
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("ТЕКСТ СЦЕНАРИЯ", style = MaterialTheme.typography.titleMedium)
                                Icon(
                                    imageVector = if (showScript) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Свернуть/Развернуть"
                                )
                            }

                            // Анимированное содержимое
                            AnimatedVisibility(
                                visible = showScript,
                                enter = expandVertically(animationSpec = tween(300)),
                                exit = shrinkVertically(animationSpec = tween(300))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Black.copy(alpha = 0.2f)) // Чуть темнее фон для самого текста
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = currentScene.content,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontFamily = FontFamily.Monospace, // Киношный шрифт
                                            lineHeight = 24.sp,
                                            fontSize = 15.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // 3. Вкладка: РЕКВИЗИТ
                    Card(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showProps = !showProps }
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("РЕКВИЗИТ (${props.size})", style = MaterialTheme.typography.titleMedium)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Кнопка добавления реквизита прямо в шапке вкладки
                                    IconButton(
                                        onClick = { showAddPropDialog = true },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Добавить", tint = MaterialTheme.colorScheme.primary)
                                    }
                                    Icon(
                                        imageVector = if (showProps) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Свернуть/Развернуть"
                                    )
                                }
                            }

                            AnimatedVisibility(
                                visible = showProps,
                                enter = expandVertically(animationSpec = tween(300)),
                                exit = shrinkVertically(animationSpec = tween(300))
                            ) {
                                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                                    if (props.isEmpty()) {
                                        Text("Реквизит пока не добавлен", color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))
                                    } else {
                                        props.forEach { prop ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(prop.name, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                                                    Text("Статус: ${prop.status}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                                }
                                                IconButton(onClick = { screenModel.deleteProp(prop.id) }) {
                                                    Icon(androidx.compose.material.icons.Icons.Default.Delete, contentDescription = "Удалить", tint = Color.Red.copy(alpha = 0.7f))
                                                }
                                            }
                                            HorizontalDivider(thickness = 0.5.dp, color = Color.Gray.copy(alpha = 0.2f))
                                        }
                                        Spacer(Modifier.height(8.dp))
                                    }
                                }
                            }
                        }
                    }
                    // Запас места снизу
                    Spacer(Modifier.height(32.dp))
                }
            } ?: run {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            // ДИАЛОГ ДОБАВЛЕНИЯ РЕКВИЗИТА
            if (showAddPropDialog) {
                AlertDialog(
                    onDismissRequest = {
                        showAddPropDialog = false
                        newPropName = "" // очищаем поле при закрытии
                    },
                    title = { Text("Новый реквизит") },
                    text = {
                        OutlinedTextField(
                            value = newPropName,
                            onValueChange = { newPropName = it },
                            label = { Text("Название (напр. Азалия, Шприц)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (newPropName.isNotBlank()) {
                                    screenModel.addProp(newPropName.trim())
                                    showAddPropDialog = false
                                    newPropName = "" // очищаем после сохранения
                                }
                            }
                        ) {
                            Text("Добавить")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showAddPropDialog = false
                            newPropName = ""
                        }) {
                            Text("Отмена", color = Color.Gray)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}