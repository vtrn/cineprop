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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
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
        val searchQuery by viewModel.searchQuery.collectAsState()

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
                        isSortAscending = isSortAscending,
                        searchQuery = searchQuery
                    )
                },
                inspectorPane = {
                    PropInspectorPane(
                        propId = selectedPropId,
                        props = filteredProps,
                        onNoteChange = { id, note -> viewModel.updatePropNote(id, note) },
                        onConfirm = { id -> viewModel.confirmProps(listOf(id)) },
                        onDelete = { id -> viewModel.deleteProp(id) }
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
                color = MaterialTheme.colorScheme.primary
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
        isSortAscending: Boolean,
        searchQuery: String
    ) {
        var expandedGroups by remember { mutableStateOf(setOf<String>()) }
        
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

            BulkActionsToolbar(selectedIds = selectedPropIds, viewModel = viewModel)

            LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
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

    @Composable
    private fun PropListItem(
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
        val categoryColor = getCategoryColor(prop.category)
        val statusColor = getStatusColor(prop.status)
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

                Box {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(categoryColor.copy(alpha = 0.1f))
                            .clickable { categoryMenuExpanded = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(getCategoryIcon(prop.category), null, modifier = Modifier.size(16.dp), tint = categoryColor)
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
                                    Icon(getCategoryIcon(category), null, modifier = Modifier.size(18.dp), tint = getCategoryColor(category))
                                }
                            )
                        }
                    }
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

    @Composable
    private fun StackedSceneTags(currentScene: String, otherScenes: List<String>) {
        val currentSeries = currentScene.split("-").firstOrNull() ?: ""
        
        Box(modifier = Modifier.height(24.dp).wrapContentWidth(), contentAlignment = Alignment.CenterStart) {
            // Background tags - используем сплошные цвета, чтобы слои не просвечивали
            
            // Third tag (самый нижний слой)
            if (otherScenes.size >= 2) {
                val scene = otherScenes[1]
                val displayScene = if (scene.startsWith("$currentSeries-")) scene.substringAfter("-") else scene
                TagLayer(
                    text = "$displayScene",
                    xOffset = 48.dp, // Смещение выбрано так, чтобы 2-й тег перекрывал его наполовину
                    zIndex = 1f,
                    color = Color(0xFFF5F5F5), // Сплошной светло-серый
                    textColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    paddingStart = 10.dp
                )
            }

            // Second tag (средний слой)
            if (otherScenes.isNotEmpty()) {
                val scene = otherScenes[0]
                val displayScene = if (scene.startsWith("$currentSeries-")) "-${scene.substringAfter("-")}" else "-$scene"
                TagLayer(
                    text = "$displayScene",
                    xOffset = 20.dp, // Смещение 1/2 от первого тега
                    zIndex = 2f,     // Выше третьего, перекрывает его непрозрачным фоном
                    color = Color(0xFFEBEBEB), // Сплошной серый
                    textColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    paddingStart = 14.dp
                )
            }
            
            // Main front tag
            Surface(
                color = Color(0xFFE1F5FE),
                shape = CircleShape,
                border = BorderStroke(1.dp, Color.White),
                modifier = Modifier.zIndex(10f)
            ) {
                Text(
                    text = currentScene,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 1.dp)
                )
            }
        }
    }

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
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
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
                    Spacer(Modifier.width(12.dp))
                    IconButton(onClick = { viewModel.deleteSelectedProps() }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp)) }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { viewModel.clearSelection() }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp)) }
                }
            }
        }
    }

    @Composable
    private fun PropInspectorPane(propId: Long?, props: List<PropWithScene>, onNoteChange: (Long, String) -> Unit, onConfirm: (Long) -> Unit, onDelete: (Long) -> Unit) {
        val prop = props.find { it.id == propId }
        
        var noteValue by remember(propId) { 
            mutableStateOf(TextFieldValue(prop?.note ?: "")) 
        }

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
                                value = noteValue,
                                onValueChange = { 
                                    noteValue = it
                                    onNoteChange(prop.id, it.text) 
                                },
                                modifier = Modifier.fillMaxSize(),
                                textStyle = TextStyle(fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface, lineHeight = 22.sp),
                                cursorBrush = Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary))
                            )
                        }
                    }
                    
                    Spacer(Modifier.height(24.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { onConfirm(prop.id) },
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Готовность", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        Button(
                            onClick = { onDelete(prop.id) },
                            modifier = Modifier.height(50.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f), contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Delete, null)
                        }
                    }
                }
            }
        }
    }
}
