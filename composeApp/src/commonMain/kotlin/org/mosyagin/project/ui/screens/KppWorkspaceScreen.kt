package org.mosyagin.project.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
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
import org.mosyagin.project.util.rememberFilePickerLauncher

data class KppWorkspaceScreen(val projectId: String) : Screen {

    @Composable
    override fun Content() {
        val screenModel = koinScreenModel<KppWorkspaceViewModel> { parametersOf(projectId) }
        
        val kppFiles by screenModel.kppFiles.collectAsState()
        val selectedId by screenModel.selectedFileId.collectAsState()
        val isLoading by screenModel.isLoading.collectAsState()
        val selectedFile = remember(kppFiles, selectedId) { kppFiles.find { it.id == selectedId } }

        val filePicker = rememberFilePickerLauncher { platformFile ->
            platformFile?.let { file ->
                val content = file.bytes?.decodeToString() ?: ""
                screenModel.processCsv(file.name, content)
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            ThreePaneLayout(
                masterPane = {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Text(
                            "Файлы КПП",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(16.dp)
                        )
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(kppFiles) { file ->
                                val isSelected = file.id == selectedId
                                CineCard(
                                    onClick = { screenModel.onFileSelected(file.id) },
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer 
                                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Description,
                                            null,
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                file.fileName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                            Text(
                                                "Версия ${file.version}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                detailPane = {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (selectedFile != null) {
                            Text("Детали файла: ${selectedFile.fileName}", style = MaterialTheme.typography.headlineSmall)
                        } else {
                            Text("Выберите файл КПП слева", color = MaterialTheme.colorScheme.outline)
                        }
                    }
                },
                inspectorPane = {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (selectedFile != null) {
                            Text("Инспектор КПП", style = MaterialTheme.typography.titleMedium)
                        } else {
                            Text("Инспектор пуст", color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            )

            // Кнопка загрузки КПП
            FloatingActionButton(
                onClick = { filePicker.launch() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(32.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Загрузить КПП")
            }

            // Индикатор загрузки
            if (isLoading) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black.copy(alpha = 0.6f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(16.dp))
                            Text("Обработка КПП...", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
