package org.mosyagin.project.ui.components.props

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import org.mosyagin.project.models.versioning.PropStatus
import org.mosyagin.project.repository.PropWithScene
import org.mosyagin.project.ui.screens.PropWorkspaceViewModel

/**
 * Индивидуальный элемент списка реквизита.
 * Реализует карточку с названием, выбором категории, статусом и количеством.
 * 
 * @param prop Данные объекта.
 * @param isSelected Флаг выбора (для массовых действий).
 * @param isCurrent Флаг активного объекта (подсвечен в инспекторе).
 * @param onSelect Обработчик выбора.
 * @param onClick Обработчик клика по карточке.
 * @param onStatusChange Изменение статуса.
 * @param onCrossCuttingChange Изменение признака "сквозной".
 * @param onQuantityChange Изменение количества.
 * @param onCategoryChange Изменение категории.
 * @param onDelete Удаление объекта.
 * @param onStackClick Клик по стеку сцен (для раскрытия группы).
 */
@Composable
fun PropListItem(
    prop: PropWithScene,
    isSelected: Boolean,
    isCurrent: Boolean,
    onSelect: () -> Unit,
    onClick: () -> Unit,
    onStatusChange: (PropStatus) -> Unit,
    onCrossCuttingChange: (Boolean) -> Unit,
    onQuantityChange: (Int) -> Unit,
    onCategoryChange: (String) -> Unit,
    onDelete: () -> Unit,
    onStackClick: () -> Unit = {}
) {
    val categoryColor = PropUiUtils.getCategoryColor(prop.category)
    val statusColor = PropUiUtils.getStatusColor(prop.status)
    var statusMenuExpanded by remember { mutableStateOf(false) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    
    var quantityInput by remember(prop.quantity) { mutableStateOf(prop.quantity.toString()) }

    val background = if (isCurrent) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    }

    val borderColor = if (isCurrent) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Чекбокс
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .border(
                        width = 1.dp, 
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .clickable { onSelect() },
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
            
            Spacer(Modifier.width(12.dp))

            // Иконка категории с выпадающим меню
            Box {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(categoryColor.copy(alpha = 0.1f))
                        .clickable { categoryMenuExpanded = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(PropUiUtils.getCategoryIcon(prop.category), null, modifier = Modifier.size(16.dp), tint = categoryColor)
                }

                DropdownMenu(
                    expanded = categoryMenuExpanded,
                    onDismissRequest = { categoryMenuExpanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                ) {
                    PropWorkspaceViewModel.DEFAULT_CATEGORIES.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.replaceFirstChar { it.uppercase() }, fontSize = 14.sp) },
                            onClick = {
                                onCategoryChange(category)
                                categoryMenuExpanded = false
                            },
                            leadingIcon = {
                                Icon(PropUiUtils.getCategoryIcon(category), null, modifier = Modifier.size(18.dp), tint = PropUiUtils.getCategoryColor(category))
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.width(12.dp))
            
            // Название и теги сцен
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    prop.name, 
                    style = MaterialTheme.typography.bodyLarge, 
                    fontWeight = FontWeight.Medium, 
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                
                if (prop.isCrossCutting && prop.allSceneNumbers.size > 1) {
                    Box(modifier = Modifier.clickable { onStackClick() }) {
                        StackedSceneTags(
                            currentScene = "${prop.seriesNumber}-${prop.sceneNumber}",
                            otherScenes = prop.allSceneNumbers.filter { it != "${prop.seriesNumber}-${prop.sceneNumber}" }
                        )
                    }
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                        shape = CircleShape,
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    ) {
                        Text(
                            text = "${prop.seriesNumber}-${prop.sceneNumber}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            // Индикатор сквозного реквизита
            Icon(
                Icons.Default.Link, 
                null, 
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .clickable { onCrossCuttingChange(!prop.isCrossCutting) }
                    .padding(4.dp),
                tint = if (prop.isCrossCutting) Color(0xFF5856D6) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
            )

            Spacer(Modifier.width(12.dp))

            // Статус
            Box(modifier = Modifier.width(100.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { statusMenuExpanded = true }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(statusColor))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        PropStatus.fromString(prop.status).displayName, 
                        style = MaterialTheme.typography.labelMedium, 
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        maxLines = 1
                    )
                }
                
                DropdownMenu(
                    expanded = statusMenuExpanded,
                    onDismissRequest = { statusMenuExpanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                ) {
                    PropStatus.entries.forEach { status ->
                        DropdownMenuItem(
                            text = { Text(status.displayName, fontSize = 14.sp) },
                            onClick = {
                                onStatusChange(status)
                                statusMenuExpanded = false
                            },
                            leadingIcon = {
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(PropUiUtils.getStatusColor(status.displayName)))
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.width(12.dp))
            
            // Количество
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { if (prop.quantity > 1) onQuantityChange(prop.quantity - 1) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Remove, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
                
                BasicTextField(
                    value = quantityInput,
                    onValueChange = { newValue ->
                        if (newValue.all { it.isDigit() } && newValue.length <= 4) {
                            quantityInput = newValue
                            newValue.toIntOrNull()?.let { if (it > 0) onQuantityChange(it) }
                        }
                    },
                    modifier = Modifier.width(32.dp),
                    textStyle = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onQuantityChange(prop.quantity + 1) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }

            Spacer(Modifier.width(8.dp))

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Default.DeleteOutline, 
                    contentDescription = "Удалить",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Визуальный стек сцен для сквозного реквизита.
 */
@Composable
private fun StackedSceneTags(currentScene: String, otherScenes: List<String>) {
    val currentSeries = currentScene.split("-").firstOrNull() ?: ""
    
    Box(modifier = Modifier.height(24.dp).wrapContentWidth(), contentAlignment = Alignment.CenterStart) {
        // Третий слой
        if (otherScenes.size >= 2) {
            val scene = otherScenes[1]
            val displayScene = if (scene.startsWith("$currentSeries-")) scene.substringAfter("-") else scene
            TagLayer(
                text = displayScene,
                xOffset = 38.dp, 
                zIndex = 1f,
                color = MaterialTheme.colorScheme.surfaceVariant,
                textColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                paddingStart = 10.dp
            )
        }

        // Второй слой
        if (otherScenes.isNotEmpty()) {
            val scene = otherScenes[0]
            val displayScene = if (scene.startsWith("$currentSeries-")) "-${scene.substringAfter("-")}" else "-$scene"
            TagLayer(
                text = displayScene,
                xOffset = 20.dp, 
                zIndex = 2f,
                color = MaterialTheme.colorScheme.surfaceVariant,
                textColor = MaterialTheme.colorScheme.onSurfaceVariant,
                paddingStart = 14.dp
            )
        }
        
        // Основной тег
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = CircleShape,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
            modifier = Modifier.zIndex(10f)
        ) {
            Text(
                text = currentScene,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 1.dp)
            )
        }
    }
}

/**
 * Вспомогательная функция для отрисовки слоя в стеке.
 */
@Composable
private fun TagLayer(
    text: String, 
    xOffset: androidx.compose.ui.unit.Dp, 
    zIndex: Float, 
    color: Color, 
    textColor: Color = Color.Transparent,
    paddingStart: androidx.compose.ui.unit.Dp = 14.dp
) {
    Surface(
        color = color,
        shape = CircleShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
        modifier = Modifier
            .offset(x = xOffset)
            .zIndex(zIndex)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            modifier = Modifier.padding(start = paddingStart, end = 6.dp, top = 1.dp, bottom = 1.dp)
        )
    }
}
