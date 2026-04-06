package org.mosyagin.project.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
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
import kotlinx.coroutines.launch
import org.koin.core.parameter.parametersOf
import org.mosyagin.project.models.versioning.ScriptFile
import org.mosyagin.project.parser.update.UpdateResult
import org.mosyagin.project.ui.components.CineCard
import org.mosyagin.project.ui.components.CineTag
import org.mosyagin.project.ui.components.UpdateReportDialog
import org.mosyagin.project.util.rememberFilePickerLauncher

data class ScriptVersionScreen(val projectId: String, val seriesNumber: Int) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        
        // Модель для списка версий
        val viewModel = koinScreenModel<ScriptVersionViewModel> { parametersOf(projectId, seriesNumber) }
        val uiState by viewModel.uiState.collectAsState()

        // Общая модель для обработки PDF
        val scriptViewModel = koinScreenModel<ScriptViewModel>()
        val isParsing by scriptViewModel.isLoading.collectAsState()
        val updateResult by scriptViewModel.updateResult.collectAsState()

        // Инициализация выбора файла
        val filePicker = rememberFilePickerLauncher { platformFile ->
            platformFile?.uriString?.let { uri ->
                scope.launch {
                    scriptViewModel.processPdfUri(projectId, seriesNumber, uri)
                }
            }
        }

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("История версий: Серия $seriesNumber") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { filePicker.launch() }) {
                    Icon(Icons.Default.Add, contentDescription = "Новая ревизия")
                }
            }
        ) { paddingValues ->
            Box(Modifier.fillMaxSize()) {
                when (val state = uiState) {
                    is ScriptVersionUiState.Loading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is ScriptVersionUiState.Success -> {
                        if (state.versions.isEmpty()) {
                            EmptyState(paddingValues)
                        } else {
                            VersionList(state.versions, state.activeVersionId, viewModel, paddingValues)
                        }
                    }
                    is ScriptVersionUiState.Error -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(state.message, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                // Индикатор парсинга нового файла
                if (isParsing) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = Color.Black.copy(alpha = 0.4f)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(color = Color.White)
                            Spacer(Modifier.height(16.dp))
                            Text("Обработка данных...", color = Color.White)
                        }
                    }
                }
            }
        }

        // Показ диалога с результатами обновления
        updateResult?.let { result ->
            when (result) {
                is UpdateResult.Success -> {
                    UpdateReportDialog(
                        stats = result.stats,
                        matches = result.matches,
                        onConfirm = { 
                            scope.launch {
                                scriptViewModel.commitUpdate()
                                viewModel.loadVersions() // Обновляем список после сохранения
                            }
                        },
                        onCancel = { scriptViewModel.clearUpdateResult() },
                        onViewDiff = { match ->
                            // TODO: Переход к Diff Viewer
                        }
                    )
                }
                is UpdateResult.Error -> {
                    AlertDialog(
                        onDismissRequest = { scriptViewModel.clearUpdateResult() },
                        title = { Text("Ошибка") },
                        text = { Text(result.message) },
                        confirmButton = {
                            TextButton(onClick = { scriptViewModel.clearUpdateResult() }) { Text("OK") }
                        }
                    )
                }
                UpdateResult.NoChanges -> {
                    LaunchedEffect(Unit) {
                        // Можно показать Snackbar
                        scriptViewModel.clearUpdateResult()
                    }
                }
            }
        }
    }

    @Composable
    private fun VersionList(
        versions: List<ScriptFile>,
        activeId: String?,
        viewModel: ScriptVersionViewModel,
        padding: PaddingValues
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(versions) { version ->
                VersionCard(version, isActive = version.id == activeId, onDelete = { viewModel.deleteVersion(version.id) })
            }
        }
    }

    @Composable
    private fun VersionCard(version: ScriptFile, isActive: Boolean, onDelete: () -> Unit) {
        val navigator = LocalNavigator.currentOrThrow
        CineCard(
            onClick = {
                // При клике открываем список сцен этой версии
                navigator.push(SceneListScreen(version.projectId, "Серия ${version.seriesNumber}"))
            },
            containerColor = if (isActive) 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) 
            else 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(color = Color(parseColor(version.revisionColor.hexCode)), shape = CircleShape)
                )
                
                Spacer(Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = version.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Загружено: ${formatDate(version.createdAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isActive) {
                    CineTag(text = "Активная", containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                }

                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                }
            }
        }
    }

    @Composable
    private fun EmptyState(paddingValues: PaddingValues) {
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.History, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.height(16.dp))
            Text("Нет загруженных версий", style = MaterialTheme.typography.titleMedium)
        }
    }

    private fun formatDate(timestamp: Long): String {
        return (timestamp / 1000 / 3600 / 24 % 31).toString() + " числа"
    }

    private fun parseColor(hex: String): Long {
        return try {
            hex.removePrefix("#").toLong(16) or 0xFF000000
        } catch (e: Exception) {
            0xFFFFFFFF
        }
    }
}
