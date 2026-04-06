@file:OptIn(ExperimentalTime::class)

package org.mosyagin.project.ui.screens

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
import org.koin.core.parameter.parametersOf
import org.mosyagin.project.ScriptFile
import org.mosyagin.project.ui.components.ThreePaneLayout
import org.mosyagin.project.ui.components.CineCard
import org.mosyagin.project.ui.components.UpdateReportDialog
import org.mosyagin.project.parser.update.UpdateResult
import org.mosyagin.project.util.rememberFilePickerLauncher
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime

data class ScriptWorkspaceScreen(val projectId: String) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val screenModel = koinScreenModel<ScriptWorkspaceViewModel> { parametersOf(projectId) }
        
        val scriptTree by screenModel.scriptTree.collectAsState()
        val expandedSeries by screenModel.expandedSeries.collectAsState()
        val selectedId by screenModel.selectedScriptFileId.collectAsState()
        val selectedFileDetails by screenModel.selectedScriptDetails.collectAsState()
        val isLoading by screenModel.isLoading.collectAsState()
        val updateResult by screenModel.updateResult.collectAsState()

        var showAddDialog by remember { mutableStateOf(false) }
        var seriesNumberInput by remember { mutableStateOf("1") }

        val filePicker = rememberFilePickerLauncher { platformFile ->
            platformFile?.let { file ->
                val seriesNum = seriesNumberInput.toIntOrNull() ?: 1
                screenModel.processPdfFile(seriesNum, file.uriString ?: "")
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            ThreePaneLayout(
                masterPane = {
                    ScriptMasterPane(
                        scriptTree = scriptTree,
                        expandedSeries = expandedSeries,
                        selectedId = selectedId,
                        onSeriesClick = { screenModel.toggleSeries(it) },
                        onVersionClick = { screenModel.onScriptFileSelected(it) },
                        onUploadNewVersion = { series ->
                            seriesNumberInput = series.toString()
                            filePicker.launch()
                        }
                    )
                },
                detailPane = {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopStart) {
                        if (selectedFileDetails != null) {
                            Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                                Text(
                                    text = selectedFileDetails!!.title,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "Версия от ${formatDate(selectedFileDetails!!.createdAt)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 24.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f)
                                )
                                
                                Text(
                                    "Локальный путь:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    selectedFileDetails!!.filePath,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                
                                Spacer(Modifier.height(32.dp))
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("Текст сценария и список сцен этой версии будут доступны в следующем обновлении", 
                                         color = MaterialTheme.colorScheme.outline,
                                         textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                }
                            }
                        } else {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Выберите версию сценария в списке слева", color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                },
                inspectorPane = {
                    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                        Text("Инспектор файла", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(32.dp))
                        
                        if (selectedFileDetails != null) {
                            InfoItem("ID Версии", selectedFileDetails!!.id)
                            InfoItem("Номер серии", selectedFileDetails!!.seriesNumber.toString())
                            InfoItem("Дата загрузки", formatDate(selectedFileDetails!!.createdAt))
                            InfoItem("Цвет ревизии", selectedFileDetails!!.revisionColor ?: "White")
                            InfoItem("Пользователь", selectedFileDetails!!.uploadedBy ?: "Система")
                        } else {
                            Text("Выберите файл для просмотра метаданных", color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            )

            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.align(Alignment.BottomEnd).padding(32.dp)
            ) {
                Icon(Icons.Default.Add, "Добавить серию")
            }

            if (showAddDialog) {
                AlertDialog(
                    onDismissRequest = { showAddDialog = false },
                    title = { Text("Новая серия") },
                    text = {
                        OutlinedTextField(
                            value = seriesNumberInput,
                            onValueChange = { seriesNumberInput = it.filter { c -> c.isDigit() } },
                            label = { Text("Номер серии") },
                            singleLine = true
                        )
                    },
                    confirmButton = {
                        Button(onClick = {
                            showAddDialog = false
                            filePicker.launch()
                        }) { Text("Выбрать PDF") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddDialog = false }) { Text("Отмена") }
                    }
                )
            }

            if (isLoading) {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Black.copy(alpha = 0.6f)) {
                    Column(verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(16.dp))
                        Text("Обработка сценария...", color = Color.White)
                    }
                }
            }

            updateResult?.let { result ->
                when (result) {
                    is UpdateResult.Success -> {
                        UpdateReportDialog(
                            stats = result.stats,
                            matches = result.matches,
                            onConfirm = { screenModel.commitUpdate() },
                            onCancel = { screenModel.clearUpdateResult() },
                            onViewDiff = {}
                        )
                    }
                    is UpdateResult.Error -> {
                        AlertDialog(
                            onDismissRequest = { screenModel.clearUpdateResult() },
                            title = { Text("Ошибка") },
                            text = { Text(result.message) },
                            confirmButton = {
                                TextButton(onClick = { screenModel.clearUpdateResult() }) { Text("OK") }
                            }
                        )
                    }
                    else -> {}
                }
            }
        }
    }

    @Composable
    private fun ScriptMasterPane(
        scriptTree: Map<Long, List<ScriptFile>>,
        expandedSeries: Set<Long>,
        selectedId: String?,
        onSeriesClick: (Long) -> Unit,
        onVersionClick: (String) -> Unit,
        onUploadNewVersion: (Long) -> Unit
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                "Сценарии",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                scriptTree.keys.sorted().forEach { seriesNum ->
                    val isExpanded = expandedSeries.contains(seriesNum)
                    val versions = scriptTree[seriesNum] ?: emptyList()

                    item {
                        CineCard(
                            onClick = { onSeriesClick(seriesNum) },
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Серия $seriesNum", fontWeight = FontWeight.Bold)
                                    Text("${versions.size} версий", style = MaterialTheme.typography.bodySmall)
                                }
                                IconButton(onClick = { onUploadNewVersion(seriesNum) }) {
                                    Icon(Icons.Default.Upload, "Загрузить новую версию", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }

                    if (isExpanded) {
                        items(versions.sortedByDescending { it.createdAt }) { file ->
                            val isSelected = file.id == selectedId
                            CineCard(
                                onClick = { onVersionClick(file.id) },
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer 
                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                modifier = Modifier.fillMaxWidth().padding(start = 24.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Description,
                                        null,
                                        modifier = Modifier.size(18.dp),
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            file.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                        Text(
                                            formatDate(file.createdAt),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun InfoItem(label: String, value: String) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Text(value, style = MaterialTheme.typography.bodyMedium)
        }
    }

    private fun formatDate(timestamp: Long): String {
        return try {
            val instant = Instant.fromEpochMilliseconds(timestamp)
            val dt = instant.toLocalDateTime(TimeZone.currentSystemDefault())
            "${dt.dayOfMonth}.${dt.monthNumber}.${dt.year} ${dt.hour}:${dt.minute.toString().padStart(2, '0')}"
        } catch (e: Exception) {
            "Неизвестно"
        }
    }
}
