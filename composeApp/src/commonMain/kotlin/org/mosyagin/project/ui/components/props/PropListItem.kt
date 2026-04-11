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
import org.mosyagin.project.ui.components.AppLayoutType
import org.mosyagin.project.ui.components.LocalAppLayoutType
import org.mosyagin.project.ui.screens.PropWorkspaceViewModel

/**
 * Индивидуальный элемент списка реквизита.
 * Адаптивен для десктопа и мобильных устройств.
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
    val layoutType = LocalAppLayoutType.current
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
        if (layoutType == AppLayoutType.DESKTOP) {
            // ДЕСКТОПНАЯ ВЕРСИЯ (Одна строка)
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CheckboxCell(isSelected, onSelect)
                Spacer(Modifier.width(12.dp))
                CategoryIconCell(prop.category, categoryColor) { categoryMenuExpanded = true }
                Spacer(Modifier.width(12.dp))
                
                // Название и сцены
                Column(modifier = Modifier.weight(1f)) {
                    Text(prop.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    SceneTags(prop, onStackClick)
                }

                // Управление
                CrossCuttingIcon(prop.isCrossCutting) { onCrossCuttingChange(!prop.isCrossCutting) }
                Spacer(Modifier.width(12.dp))
                StatusCell(prop.status, statusColor) { statusMenuExpanded = true }
                Spacer(Modifier.width(12.dp))
                QuantityCell(quantityInput, prop.quantity, { quantityInput = it }, onQuantityChange)
                Spacer(Modifier.width(8.dp))
                DeleteButton(onDelete)
            }
        } else {
            // МОБИЛЬНАЯ ВЕРСИЯ (Более компактная и двухстрочная)
            Row(
                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CheckboxCell(isSelected, onSelect)
                Spacer(Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CategoryIconCell(prop.category, categoryColor, size = 24.dp) { categoryMenuExpanded = true }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            prop.name, 
                            style = MaterialTheme.typography.titleMedium, 
                            fontWeight = FontWeight.Bold,
                            maxLines = 1, 
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusIndicator(statusColor, prop.status)
                        Spacer(Modifier.width(12.dp))
                        SceneTags(prop, onStackClick)
                    }
                }

                QuantityCell(quantityInput, prop.quantity, { quantityInput = it }, onQuantityChange, compact = true)
            }
        }

        // Выпадающие меню (общие для обоих режимов)
        DropdownMenu(
            expanded = categoryMenuExpanded,
            onDismissRequest = { categoryMenuExpanded = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
        ) {
            PropWorkspaceViewModel.DEFAULT_CATEGORIES.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.replaceFirstChar { it.uppercase() }, fontSize = 14.sp) },
                    onClick = { onCategoryChange(category); categoryMenuExpanded = false },
                    leadingIcon = { Icon(PropUiUtils.getCategoryIcon(category), null, modifier = Modifier.size(18.dp), tint = PropUiUtils.getCategoryColor(category)) }
                )
            }
        }

        DropdownMenu(
            expanded = statusMenuExpanded,
            onDismissRequest = { statusMenuExpanded = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
        ) {
            PropStatus.entries.forEach { status ->
                DropdownMenuItem(
                    text = { Text(status.displayName, fontSize = 14.sp) },
                    onClick = { onStatusChange(status); statusMenuExpanded = false },
                    leadingIcon = { Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(PropUiUtils.getStatusColor(status.displayName))) }
                )
            }
        }
    }
}

@Composable
private fun CheckboxCell(isSelected: Boolean, onSelect: () -> Unit) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
            .clickable { onSelect() },
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onPrimary)
    }
}

@Composable
private fun CategoryIconCell(category: String, color: Color, size: androidx.compose.ui.unit.Dp = 32.dp, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(size).clip(CircleShape).background(color.copy(alpha = 0.1f)).clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(PropUiUtils.getCategoryIcon(category), null, modifier = Modifier.size(size * 0.5f), tint = color)
    }
}

@Composable
private fun SceneTags(prop: PropWithScene, onStackClick: () -> Unit) {
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

@Composable
private fun StatusIndicator(color: Color, status: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(
            PropStatus.fromString(status).displayName, 
            style = MaterialTheme.typography.labelSmall, 
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun StatusCell(status: String, color: Color, onClick: () -> Unit) {
    Box(modifier = Modifier.width(100.dp).clickable { onClick() }) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)) {
            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(6.dp))
            Text(PropStatus.fromString(status).displayName, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), maxLines = 1)
        }
    }
}

@Composable
private fun QuantityCell(input: String, value: Int, onInputChange: (String) -> Unit, onValueChange: (Int) -> Unit, compact: Boolean = false) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
    ) {
        if (!compact) {
            IconButton(onClick = { if (value > 1) onValueChange(value - 1) }, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Remove, null, modifier = Modifier.size(12.dp))
            }
        }
        
        BasicTextField(
            value = input,
            onValueChange = { newValue ->
                if (newValue.all { it.isDigit() } && newValue.length <= 3) {
                    onInputChange(newValue)
                    newValue.toIntOrNull()?.let { if (it > 0) onValueChange(it) }
                }
            },
            modifier = Modifier.width(if (compact) 40.dp else 32.dp),
            textStyle = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true
        )

        if (!compact) {
            IconButton(onClick = { onValueChange(value + 1) }, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(12.dp))
            }
        }
    }
}

@Composable
private fun CrossCuttingIcon(isCrossCutting: Boolean, onClick: () -> Unit) {
    Icon(
        Icons.Default.Link, null, 
        modifier = Modifier.size(22.dp).clip(CircleShape).clickable { onClick() }.padding(4.dp),
        tint = if (isCrossCutting) Color(0xFF5856D6) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
    )
}

@Composable
private fun DeleteButton(onDelete: () -> Unit) {
    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
        Icon(Icons.Default.DeleteOutline, null, tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun StackedSceneTags(currentScene: String, otherScenes: List<String>) {
    val currentSeries = currentScene.split("-").firstOrNull() ?: ""
    Box(modifier = Modifier.height(24.dp).wrapContentWidth(), contentAlignment = Alignment.CenterStart) {
        if (otherScenes.size >= 2) {
            TagLayer(text = otherScenes[1].substringAfter("-"), xOffset = 38.dp, zIndex = 1f, color = MaterialTheme.colorScheme.surfaceVariant, textColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), paddingStart = 10.dp)
        }
        if (otherScenes.isNotEmpty()) {
            TagLayer(text = "-${otherScenes[0].substringAfter("-")}", xOffset = 20.dp, zIndex = 2f, color = MaterialTheme.colorScheme.surfaceVariant, textColor = MaterialTheme.colorScheme.onSurfaceVariant, paddingStart = 14.dp)
        }
        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)), modifier = Modifier.zIndex(10f)) {
            Text(text = currentScene, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(horizontal = 8.dp, vertical = 1.dp))
        }
    }
}

@Composable
private fun TagLayer(text: String, xOffset: androidx.compose.ui.unit.Dp, zIndex: Float, color: Color, textColor: Color, paddingStart: androidx.compose.ui.unit.Dp) {
    Surface(color = color, shape = CircleShape, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)), modifier = Modifier.offset(x = xOffset).zIndex(zIndex)) {
        Text(text = text, style = MaterialTheme.typography.labelSmall, color = textColor, modifier = Modifier.padding(start = paddingStart, end = 6.dp, top = 1.dp, bottom = 1.dp))
    }
}
