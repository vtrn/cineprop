/**
 * Экран "Список версий КПП".
 * 
 * Позволяет загружать новые файлы КПП (в формате CSV) и просматривать историю версий.
 * При загрузке файла запускается процесс парсинга, который извлекает смены и связывает их со сценами.
 */
package org.mosyagin.project.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import org.mosyagin.project.parser.KppParser
import org.mosyagin.project.repository.KppRepository
import org.mosyagin.project.util.rememberFilePickerLauncher

data class KppListScreen(val projectId: Long) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val kppRepository = koinInject<KppRepository>()
        val kppParser = koinInject<KppParser>()
        val scope = rememberCoroutineScope()

        // Подписка на список загруженных файлов КПП через репозиторий
        val kppFiles by kppRepository.getKppFilesByProject(projectId).collectAsState(initial = emptyList())

        // Лончер для выбора файла в системе
        val kppPicker = rememberFilePickerLauncher { platformFile ->
            platformFile?.let { file ->
                scope.launch {
                    val csvText = file.bytes?.decodeToString() ?: ""
                    if (csvText.isNotEmpty()) {
                        withContext(Dispatchers.IO) {
                            // 1. Используем внедренный парсер
                            kppParser.parseAndSaveKpp(projectId = projectId, csvText = csvText)
                            
                            // 2. Вычисляем номер следующей версии
                            val nextVersion = (kppFiles.maxByOrNull { it.version }?.version ?: 0) + 1
                            
                            // 3. Сохраняем запись о файле через репозиторий
                            kppRepository.addKppFile(
                                projectId = projectId,
                                fileName = file.name,
                                filePath = file.uriString ?: "memory",
                                version = nextVersion
                            )
                        }
                    }
                }
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Версии КПП") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Назад"
                            )
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = {
                    kppPicker.launch()
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Загрузить КПП")
                }
            }
        ) { padding ->
            if (kppFiles.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("КПП еще не загружен", color = MaterialTheme.colorScheme.outline)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.padding(padding).fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(kppFiles) { file ->
                        ListItem(
                            headlineContent = { Text(file.fileName) },
                            supportingContent = { Text("Версия: ${file.version}") },
                            leadingContent = {
                                Icon(Icons.Default.Event, tint = MaterialTheme.colorScheme.primary, contentDescription = null)
                            },
                            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.clickable {
                                // Будущий функционал
                            }
                        )
                    }
                }
            }
        }
    }
}
