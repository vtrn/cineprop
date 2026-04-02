package org.mosyagin.project.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import org.koin.core.parameter.parametersOf
import org.mosyagin.project.models.versioning.PropStatus
import org.mosyagin.project.repository.PropWithScene
import org.mosyagin.project.ui.components.CineCard
import org.mosyagin.project.ui.components.ThreePaneLayout

private fun getCategoryColor(category: String): Color {
    return when (category.lowercase()) {
        "персонажный" -> Color(0xFFFF2D55)
        "транспорт" -> Color(0xFFFF9500)
        "типографика" -> Color(0xFF34C759)
        "графика" -> Color(0xFF5856D6)
        "исходящий" -> Color(0xFFFF3B30)
        "животные" -> Color(0xFFA2845E)
        "оружие" -> Color(0xFF8E8E93)
        "прочее" -> Color(0xFF5AC8FA)
        else -> Color(0xFF8E8E93)
    }
}

private fun getCategoryIcon(category: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (category.lowercase()) {
        "персонажный" -> Icons.Default.Person
        "транспорт" -> Icons.Default.DirectionsCar
        "типографика" -> Icons.Default.TextFields
        "графика" -> Icons.Default.Brush
        "исходящий" -> Icons.Default.Restaurant
        "животные" -> Icons.Default.Pets
        "оружие" -> Icons.Default.MilitaryTech
        else -> Icons.Default.Inventory2
    }
}

private fun getStatusColor(statusStr: String): Color {
    return when (PropStatus.fromString(statusStr)) {
        PropStatus.PLANNED -> Color(0xFFFF9500) 
        PropStatus.BOUGHT -> Color(0xFF5AC8FA)
        PropStatus.READY -> Color(0xFF34C759) 
        PropStatus.LOST -> Color(0xFFFF3B30)
    }
}

data class PropWorkspaceScreen(val projectId: Long) : Screen {

    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<PropWorkspaceViewModel> { parametersOf(projectId) }
        
        val propsByCategory by viewModel.propsByCategory.collectAsState()
        val filteredProps by viewModel.filteredProps.collectAsState()
        val selectedPropId by viewModel.selectedPropId.collectAsState()
        val selectedPropIds by viewModel.selectedPropIds.collectAsState()
        val selectedCategoryFilter by viewModel.selectedCategoryFilter.collectAsState()
        val sortColumn by viewModel.sortColumn.collectAsState()
        val isSortAscending by viewModel.isSortAscending.collectAsState()

        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            ThreePaneLayout(
                masterPane = {
                    PropMasterPane(
                        propsByCategory = propsByCategory,
                        selectedCategoryFilter = selectedCategoryFilter,
                        onCategoryFilterSelected = { viewModel.onCategoryFilterSelected(it) }
                    )
                },
                detailPane = {
                    PropDetailPane(
                        props = filteredProps,
                        selectedPropIds = selectedPropIds,
                        selectedPropId = selectedPropId,
                        viewModel = viewModel,
                        sortColumn = sortColumn,
                        isSortAscending = isSortAscending
                    )
                },
                inspectorPane = {
                    PropInspectorPane(
                        propId = selectedPropId,
                        props = filteredProps,
                        onNoteChange = { id, note -> viewModel.updatePropNote(id, note) },
                        onConfirm = { id -> viewModel.confirmProps(listOf(id)) }
                    )
                }
            )
        }
    }

    @Composable
    private fun PropMasterPane(
        propsByCategory: Map<String, List<PropWithScene>>,
        selectedCategoryFilter: String?,
        onCategoryFilterSelected: (String?) -> Unit
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Text(
                "Категории", 
                style = MaterialTheme.typography.headlineSmall, 
                fontWeight = FontWeight.Bold, 
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 24.dp, bottom = 16.dp)
            )

            CategoryItem(
                title = "Весь реквизит", 
                icon = Icons.Default.AllInclusive, 
                isSelected = selectedCategoryFilter == null,
                color = MaterialTheme.colorScheme.primary // ПЕРЕДАЛ ЦВЕТ
            ) {
                onCategoryFilterSelected(null)
            }

            Spacer(Modifier.height(16.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                propsByCategory.forEach { (category, props) ->
                    item {
                        CategoryItem(
                            title = category,
                            icon = getCategoryIcon(category),
                            isSelected = selectedCategoryFilter == category,
                            count = props.size,
                            color = getCategoryColor(category)
                        ) { onCategoryFilterSelected(category) }
                    }
                }
            }
        }
    }

    @Composable
    private fun CategoryItem(
        title: String,
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        isSelected: Boolean,
        count: Int? = null,
        color: Color,
        onClick: () -> Unit
    ) {
        val bgColor by animateColorAsState(if (isSelected) color.copy(alpha = 0.15f) else Color.Transparent)
        val contentColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        val border = if (isSelected) BorderStroke(1.dp, color.copy(alpha = 0.3f)) else null

        Surface(
            onClick = onClick,
            color = bgColor,
            shape = RoundedCornerShape(10.dp),
            border = border,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, null, modifier = Modifier.size(18.dp), tint = if (isSelected) color else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                Spacer(Modifier.width(12.dp))
                Text(
                    title.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }, 
                    modifier = Modifier.weight(1f), 
                    style = MaterialTheme.typography.bodyMedium, 
                    color = contentColor
                )
                if (count != null) {
                    Text(count.toString(), style = MaterialTheme.typography.labelSmall, color = if (isSelected) color else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            }
        }
    }

    @Composable
    private fun PropDetailPane(
        props: List<PropWithScene>,
        selectedPropIds: Set<Long>,
        selectedPropId: Long?,
        viewModel: PropWorkspaceViewModel,
        sortColumn: PropSortColumn,
        isSortAscending: Boolean
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Row(
                verticalAlignment = Alignment.Top, 
                modifier = Modifier.padding(top = 7.dp, bottom = 16.dp).fillMaxWidth()
            ) {
                Text(
                    "Объекты", 
                    style = MaterialTheme.typography.headlineSmall, 
                    fontWeight = FontWeight.Bold, 
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.weight(1f))
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PropSortColumn.entries.take(3).forEach { col ->
                        val isSelected = sortColumn == col
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.toggleSort(col) },
                            label = { Text(col.name.lowercase(), fontSize = 11.sp) },
                            trailingIcon = if (isSelected) {
                                { Icon(if (isSortAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward, null, modifier = Modifier.size(12.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                containerColor = Color.Transparent
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                enabled = true, selected = isSelected,
                                selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                        )
                    }
                }
            }

            BulkActionsToolbar(selectedIds = selectedPropIds, viewModel = viewModel)

            LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(props, key = { it.id }) { prop ->
                    PropListItem(
                        prop = prop,
                        isSelected = selectedPropIds.contains(prop.id),
                        isCurrent = selectedPropId == prop.id,
                        onSelect = { viewModel.togglePropSelection(prop.id) },
                        onClick = { viewModel.onPropSelected(prop.id) },
                        onStatusChange = { viewModel.updatePropStatus(prop.id, it) },
                        onCrossCuttingChange = { viewModel.updatePropCrossCutting(prop.id, it) },
                        onQuantityChange = { viewModel.updatePropQuantity(prop.id, it) }
                    )
                }
            }
        }
    }

    @Composable
    private fun PropListItem(
        prop: PropWithScene,
        isSelected: Boolean,
        isCurrent: Boolean,
        onSelect: () -> Unit,
        onClick: () -> Unit,
        onStatusChange: (PropStatus) -> Unit,
        onCrossCuttingChange: (Boolean) -> Unit,
        onQuantityChange: (Int) -> Unit
    ) {
        val categoryColor = getCategoryColor(prop.category)
        val statusColor = getStatusColor(prop.status)
        var statusMenuExpanded by remember { mutableStateOf(false) }
        
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
                Checkbox(
                    checked = isSelected, 
                    onCheckedChange = { onSelect() }, 
                    modifier = Modifier.size(20.dp),
                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                )
                
                Spacer(Modifier.width(12.dp))

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(categoryColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(getCategoryIcon(prop.category), null, modifier = Modifier.size(16.dp), tint = categoryColor)
                }

                Spacer(Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        prop.name, 
                        style = MaterialTheme.typography.bodyLarge, 
                        fontWeight = FontWeight.Medium, 
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    
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

                Icon(
                    Icons.Default.AllInclusive, 
                    null, 
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .clickable { onCrossCuttingChange(!prop.isCrossCutting) }
                        .padding(4.dp),
                    tint = if (prop.isCrossCutting) Color(0xFF5856D6) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                )

                Spacer(Modifier.width(12.dp))

                Box {
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
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
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
                                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(getStatusColor(status.displayName)))
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.width(12.dp))
                
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
                    
                    Text(
                        prop.quantity.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 2.dp)
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
            }
        }
    }

    @Composable
    private fun BulkActionsToolbar(selectedIds: Set<Long>, viewModel: PropWorkspaceViewModel) {
        AnimatedVisibility(visible = selectedIds.isNotEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Выбрано: ${selectedIds.size}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.width(20.dp))
                    IconButton(onClick = { viewModel.confirmProps(selectedIds.toList()) }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp)) }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { viewModel.clearSelection() }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp)) }
                }
            }
        }
    }

    @Composable
    private fun PropInspectorPane(propId: Long?, props: List<PropWithScene>, onNoteChange: (Long, String) -> Unit, onConfirm: (Long) -> Unit) {
        val prop = props.find { it.id == propId }
        
        Column(modifier = Modifier.fillMaxSize()) {
            if (prop == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { 
                    Text("Выберите объект", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) 
                }
            } else {
                Column(modifier = Modifier.padding(horizontal = 16.dp).fillMaxHeight()) {
                    Text(
                        prop.name, 
                        style = MaterialTheme.typography.headlineSmall, 
                        fontWeight = FontWeight.ExtraBold, 
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 24.dp, bottom = 16.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = getCategoryColor(prop.category).copy(alpha = 0.12f),
                            shape = CircleShape,
                            border = BorderStroke(0.5.dp, getCategoryColor(prop.category).copy(alpha = 0.25f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(getCategoryIcon(prop.category), null, modifier = Modifier.size(14.dp), tint = getCategoryColor(prop.category))
                                Spacer(Modifier.width(6.dp))
                                Text(prop.category, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
                            shape = CircleShape,
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                        ) {
                            Text(
                                "Сцена ${prop.seriesNumber}-${prop.sceneNumber}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    Text("Контекст из сценария", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), modifier = Modifier.padding(bottom = 8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)), RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            prop.anchor, 
                            style = MaterialTheme.typography.bodyMedium, 
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            lineHeight = 20.sp
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    Text("Заметки", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), modifier = Modifier.padding(bottom = 8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)), RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Filled.Notes, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Напишите что-нибудь...", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                            }
                            Spacer(Modifier.height(12.dp))
                            BasicTextField(
                                value = prop.note ?: "",
                                onValueChange = { onNoteChange(prop.id, it) },
                                modifier = Modifier.fillMaxSize(),
                                textStyle = TextStyle(fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface, lineHeight = 22.sp),
                                cursorBrush = Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary))
                            )
                        }
                    }
                    
                    Spacer(Modifier.height(24.dp))
                    
                    Button(
                        onClick = { onConfirm(prop.id) },
                        modifier = Modifier.fillMaxWidth().height(50.dp).padding(bottom = 8.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Подтвердить готовность", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}
