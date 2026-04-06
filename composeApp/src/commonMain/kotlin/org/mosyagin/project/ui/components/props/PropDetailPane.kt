@file:OptIn(ExperimentalFoundationApi::class)

package org.mosyagin.project.ui.components.props

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.mosyagin.project.repository.PropWithScene
import org.mosyagin.project.ui.screens.PropSortColumn
import org.mosyagin.project.ui.screens.PropWorkspaceViewModel

/**
 * Центральная панель рабочего пространства: детализированный список реквизита.
 * Поддерживает поиск, сортировку, группировку по КПП и древовидную группировку сквозного реквизита.
 *
 * @param props Полный список объектов реквизита.
 * @param selectedPropIds Множество ID выбранных элементов.
 * @param selectedPropId ID текущего выбранного объекта.
 * @param viewModel Ссылка на вью-модель.
 * @param sortColumn Текущая колонка сортировки.
 * @param isSortAscending Направление сортировки.
 * @param searchQuery Текущий поисковый запрос.
 * @param isKppMode Флаг включения режима отображения по КПП.
 * @param propsByShift Данные реквизита, сгруппированные по сменам (для режима КПП).
 * @param onToggleKppMode Переключатель режима КПП.
 */
@Composable
fun PropDetailPane(
    props: List<PropWithScene>,
    selectedPropIds: Set<String>,
    selectedPropId: String?,
    viewModel: PropWorkspaceViewModel,
    sortColumn: PropSortColumn,
    isSortAscending: Boolean,
    searchQuery: String,
    isKppMode: Boolean = false,
    propsByShift: Map<Long, List<PropWithScene>> = emptyMap(),
    onToggleKppMode: () -> Unit = {}
) {
    // Состояние развернутых групп сквозного реквизита (по имени)
    var expandedGroups by remember { mutableStateOf(setOf<String>()) }
    
    // Группировка: выделяем уникальные "головы" для сквозного реквизита
    val heads = remember(props) {
        val seenNames = mutableSetOf<String>()
        props.filter { 
            if (!it.isCrossCutting) true
            else if (seenNames.contains(it.name)) false
            else {
                seenNames.add(it.name)
                true
            }
        }
    }
    
    val groupedByPropName = remember(props) { props.groupBy { it.name } }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        // Шапка панели: Название и Чипсы сортировки
        Row(
            verticalAlignment = Alignment.CenterVertically, 
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp).fillMaxWidth()
        ) {
            Text(
                "Объекты", 
                style = MaterialTheme.typography.headlineSmall, 
                fontWeight = FontWeight.Bold, 
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.weight(1f))
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Кнопка переключения режима КПП
                FilterChip(
                    selected = isKppMode,
                    onClick = onToggleKppMode,
                    label = { Text("По КПП", fontSize = 11.sp) },
                    leadingIcon = { Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(12.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )

                PropSortColumn.entries.take(3).forEach { col ->
                    val isSelected = sortColumn == col && !isKppMode
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.onSortColumnChange(col) },
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

        // Поиск по списку
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.onSearchQueryChange(it) },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            placeholder = { Text("Поиск по названию или сцене...", fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(20.dp)) },
            trailingIcon = if (searchQuery.isNotEmpty()) {
                { IconButton(onClick = { viewModel.onSearchQueryChange("") }) { Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp)) } }
            } else null,
            shape = CircleShape,
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
            )
        )

        // Панель массовых действий
        BulkActionsToolbar(selectedIds = selectedPropIds, viewModel = viewModel)

        // Основной список
        LazyColumn(
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (isKppMode) {
                // РЕЖИМ КПП: Группировка по сменам со Sticky Headers
                propsByShift.forEach { (shiftNum, shiftProps) ->
                    stickyHeader {
                        ShiftHeader(shiftNum = shiftNum, date = shiftProps.firstOrNull()?.shiftDate ?: "")
                    }
                    items(shiftProps, key = { "shift_${shiftNum}_${it.id}" }) { prop ->
                        PropListItem(
                            prop = prop,
                            isSelected = selectedPropIds.contains(prop.id),
                            isCurrent = selectedPropId == prop.id,
                            onSelect = { viewModel.togglePropSelection(prop.id) },
                            onClick = { viewModel.onPropSelected(prop.id) },
                            onStatusChange = { viewModel.updatePropStatus(prop.id, it) },
                            onCrossCuttingChange = { viewModel.updatePropCrossCutting(prop.id, it) },
                            onQuantityChange = { viewModel.updatePropQuantity(prop.id, it) },
                            onCategoryChange = { viewModel.updatePropCategory(prop.id, it) },
                            onDelete = { viewModel.deleteProp(prop.id) }
                        )
                    }
                }
            } else {
                // ОБЫЧНЫЙ РЕЖИМ: Плоский список с иерархией сквозных
                heads.forEach { headProp ->
                    item(key = headProp.id) {
                        PropListItem(
                            prop = headProp,
                            isSelected = selectedPropIds.contains(headProp.id),
                            isCurrent = selectedPropId == headProp.id,
                            onSelect = { viewModel.togglePropSelection(headProp.id) },
                            onClick = { viewModel.onPropSelected(headProp.id) },
                            onStatusChange = { viewModel.updatePropStatus(headProp.id, it) },
                            onCrossCuttingChange = { viewModel.updatePropCrossCutting(headProp.id, it) },
                            onQuantityChange = { viewModel.updatePropQuantity(headProp.id, it) },
                            onCategoryChange = { viewModel.updatePropCategory(headProp.id, it) },
                            onDelete = { viewModel.deleteProp(headProp.id) },
                            onStackClick = {
                                if (headProp.isCrossCutting) {
                                    expandedGroups = if (expandedGroups.contains(headProp.name)) {
                                        expandedGroups - headProp.name
                                    } else {
                                        expandedGroups + headProp.name
                                    }
                                }
                            }
                        )
                    }
                    
                    if (headProp.isCrossCutting && expandedGroups.contains(headProp.name)) {
                        val children = groupedByPropName[headProp.name]?.filter { it.id != headProp.id } ?: emptyList()
                        items(children, key = { it.id }) { childProp ->
                            Box(modifier = Modifier.padding(start = 28.dp)) {
                                PropListItem(
                                    prop = childProp,
                                    isSelected = selectedPropIds.contains(childProp.id),
                                    isCurrent = selectedPropId == childProp.id,
                                    onSelect = { viewModel.togglePropSelection(childProp.id) },
                                    onClick = { viewModel.onPropSelected(childProp.id) },
                                    onStatusChange = { viewModel.updatePropStatus(childProp.id, it) },
                                    onCrossCuttingChange = { viewModel.updatePropCrossCutting(childProp.id, it) },
                                    onQuantityChange = { viewModel.updatePropQuantity(childProp.id, it) },
                                    onCategoryChange = { viewModel.updatePropCategory(childProp.id, it) },
                                    onDelete = { viewModel.deleteProp(childProp.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Заголовок смены для режима КПП
 */
@Composable
private fun ShiftHeader(shiftNum: Long, date: String) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Movie, 
                null, 
                modifier = Modifier.size(16.dp), 
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Смена №$shiftNum",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (date.isNotEmpty()) {
                Text(
                    text = " — $date",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Панель массовых действий
 */
@Composable
fun BulkActionsToolbar(selectedIds: Set<String>, viewModel: PropWorkspaceViewModel) {
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
                IconButton(onClick = { /* TODO: viewModel.confirmProps(selectedIds.toList()) */ }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp)) }
                Spacer(Modifier.width(12.dp))
                IconButton(onClick = { /* TODO: viewModel.deleteSelectedProps() */ }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp)) }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { viewModel.clearSelection() }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp)) }
            }
        }
    }
}
