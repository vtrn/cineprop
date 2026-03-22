package org.mosyagin.project.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.mosyagin.project.parser.update.SceneMatch
import org.mosyagin.project.parser.update.UpdateStats

/**
 * Диалог отчета об изменениях в сценарии.
 * Показывает статистику и список затронутых сцен перед применением обновления.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateReportDialog(
    stats: UpdateStats,
    matches: List<SceneMatch>,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onViewDiff: (SceneMatch.Fuzzy) -> Unit
) {
    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.75f) // Ограничиваем высоту до 75% экрана
                .clip(RoundedCornerShape(28.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // Header
                Text(
                    text = "Сценарий обновлен",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(Modifier.height(16.dp))

                // Summary Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SummaryItem(
                        modifier = Modifier.weight(1f),
                        count = stats.newCount,
                        label = "Новых",
                        color = Color(0xFF4CAF50), // Green
                        icon = Icons.Default.AddCircle
                    )
                    SummaryItem(
                        modifier = Modifier.weight(1f),
                        count = stats.fuzzyCount,
                        label = "Изменено",
                        color = Color(0xFFFFC107), // Yellow
                        icon = Icons.Default.Edit
                    )
                    SummaryItem(
                        modifier = Modifier.weight(1f),
                        count = stats.deletedCount,
                        label = "Удалено",
                        color = Color(0xFFF44336), // Red
                        icon = Icons.Default.RemoveCircle
                    )
                    SummaryItem(
                        modifier = Modifier.weight(1f),
                        count = stats.exactCount,
                        label = "Тот же",
                        color = MaterialTheme.colorScheme.outline,
                        icon = Icons.Default.CheckCircle
                    )
                }

                Spacer(Modifier.height(24.dp))

                Text(
                    text = "Список изменений",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(8.dp))

                // Changes List
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val interestingMatches = matches.filter { it !is SceneMatch.Exact }
                    
                    if (interestingMatches.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("Текст сцен не изменился", color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    } else {
                        items(interestingMatches) { match ->
                            SceneMatchRow(match, onViewDiff)
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onCancel) {
                        Text("Отмена")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = onConfirm,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Применить изменения")
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryItem(
    modifier: Modifier = Modifier,
    count: Int,
    label: String,
    color: Color,
    icon: ImageVector
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun SceneMatchRow(
    match: SceneMatch,
    onViewDiff: (SceneMatch.Fuzzy) -> Unit
) {
    val (sceneNumber, sceneTitle, badgeText, badgeColor) = when (match) {
        is SceneMatch.New -> Quad(match.scene.sceneNumber, match.scene.location, "NEW", Color(0xFF4CAF50))
        is SceneMatch.Fuzzy -> Quad(match.scene.sceneNumber, match.scene.location, "CHANGED", Color(0xFFFFC107))
        is SceneMatch.Deleted -> Quad(match.oldSceneNumber, match.oldSceneTitle, "DELETED", Color(0xFFF44336))
        else -> Quad("", "", "", Color.Transparent)
    }

    CineCard(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Сцена $sceneNumber",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = sceneTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            SuggestionChip(
                onClick = { },
                label = { Text(badgeText, style = MaterialTheme.typography.labelSmall) },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    labelColor = badgeColor,
                    containerColor = badgeColor.copy(alpha = 0.1f)
                ),
                border = null,
                modifier = Modifier.height(24.dp)
            )

            if (match is SceneMatch.Fuzzy) {
                IconButton(onClick = { onViewDiff(match) }) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = "Просмотр изменений",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
