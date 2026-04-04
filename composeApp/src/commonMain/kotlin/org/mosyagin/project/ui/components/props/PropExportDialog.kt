package org.mosyagin.project.ui.components.props

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Варианты группировки данных при экспорте
 */
enum class ExportGrouping {
    BY_KPP, BY_SCRIPT
}

/**
 * Поддерживаемые форматы файлов
 */
enum class ExportFormat {
    PDF, EXCEL
}

/**
 * Диалоговое окно выбора параметров экспорта реквизита.
 * 
 * @param onDismiss Закрыть диалог без действий.
 * @param onExport Начать экспорт с выбранными параметрами.
 */
@Composable
fun PropExportDialog(
    onDismiss: () -> Unit,
    onExport: (grouping: ExportGrouping, format: ExportFormat) -> Unit
) {
    var selectedGrouping by remember { mutableStateOf(ExportGrouping.BY_KPP) }
    var selectedFormat by remember { mutableStateOf(ExportFormat.PDF) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Экспорт реквизита", style = MaterialTheme.typography.headlineSmall)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Секция выбора группировки
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Тип группировки",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { selectedGrouping = ExportGrouping.BY_KPP }
                    ) {
                        RadioButton(
                            selected = selectedGrouping == ExportGrouping.BY_KPP,
                            onClick = { selectedGrouping = ExportGrouping.BY_KPP }
                        )
                        Text("По КПП (по сменам)")
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { selectedGrouping = ExportGrouping.BY_SCRIPT }
                    ) {
                        RadioButton(
                            selected = selectedGrouping == ExportGrouping.BY_SCRIPT,
                            onClick = { selectedGrouping = ExportGrouping.BY_SCRIPT }
                        )
                        Text("По Сценарию (по порядку сцен)")
                    }
                }

                // Секция выбора формата
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Формат файла",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Кнопка PDF
                        FilterChip(
                            selected = selectedFormat == ExportFormat.PDF,
                            onClick = { selectedFormat = ExportFormat.PDF },
                            label = { Text("PDF") },
                            leadingIcon = { Icon(Icons.Default.PictureAsPdf, null, modifier = Modifier.size(18.dp)) },
                            modifier = Modifier.weight(1f)
                        )
                        
                        // Кнопка Excel
                        FilterChip(
                            selected = selectedFormat == ExportFormat.EXCEL,
                            onClick = { selectedFormat = ExportFormat.EXCEL },
                            label = { Text("Excel") },
                            leadingIcon = { Icon(Icons.Default.TableChart, null, modifier = Modifier.size(18.dp)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onExport(selectedGrouping, selectedFormat) }
            ) {
                Text("Экспортировать")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
