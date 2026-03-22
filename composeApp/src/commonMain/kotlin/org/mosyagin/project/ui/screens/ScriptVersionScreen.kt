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
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.koin.core.parameter.parametersOf
import org.mosyagin.project.models.versioning.RevisionColor
import org.mosyagin.project.models.versioning.ScriptFile
import org.mosyagin.project.ui.components.CineCard
import org.mosyagin.project.ui.components.CineTag

data class ScriptVersionScreen(val projectId: Long, val seriesNumber: Int) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = getScreenModel<ScriptVersionViewModel> { parametersOf(projectId, seriesNumber) }
        val uiState by viewModel.uiState.collectAsState()

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
                FloatingActionButton(onClick = { /* Вызов выбора файла будет здесь */ }) {
                    Icon(Icons.Default.Add, contentDescription = "Новая ревизия")
                }
            }
        ) { padding ->
            when (val state = uiState) {
                is ScriptVersionUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is ScriptVersionUiState.Success -> {
                    if (state.versions.isEmpty()) {
                        EmptyState(padding)
                    } else {
                        VersionList(state.versions, state.activeVersionId, viewModel, padding)
                    }
                }
                is ScriptVersionUiState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    @Composable
    private fun VersionList(
        versions: List<ScriptFile>,
        activeId: Long?,
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
        CineCard(
            containerColor = if (isActive) 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) 
            else 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Цвет ревизии
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
                    if (version.uploadedBy != null) {
                        Text(
                            text = "Автор: ${version.uploadedBy}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
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
    private fun EmptyState(padding: PaddingValues) {
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.History, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.height(16.dp))
            Text("Нет загруженных версий", style = MaterialTheme.typography.titleMedium)
        }
    }

    private fun formatDate(timestamp: Long): String {
        // Упрощенное форматирование для KMP
        return "от " + (timestamp / 1000 / 3600 / 24 % 31).toString() + " числа"
    }

    private fun parseColor(hex: String): Long {
        return hex.removePrefix("#").toLong(16) or 0xFF000000
    }
}
