package org.mosyagin.project.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import org.koin.core.parameter.parametersOf
import org.mosyagin.project.models.versioning.PropStatus
import org.mosyagin.project.repository.PropWithScene

data class PropAssetManagerScreen(val projectId: String) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<PropAssetManagerViewModel> { parametersOf(projectId) }
        val state by viewModel.uiState.collectAsState()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Asset Manager (Реквизит)") },
                    actions = {
                        SearchAndFilters(state, viewModel)
                    }
                )
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                BulkActionsPanel(state, viewModel)
                
                Box(modifier = Modifier.fillMaxSize()) {
                    val scrollState = rememberLazyListState()
                    
                    LazyColumn(
                        state = scrollState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Header
                        item {
                            PropTableHeader(
                                isAllSelected = state.selectedPropIds.size == state.filteredProps.size && state.filteredProps.isNotEmpty(),
                                onSelectAll = { viewModel.selectAllFiltered() },
                                onUnselectAll = { viewModel.clearSelection() }
                            )
                        }

                        items(state.filteredProps, key = { it.id }) { prop ->
                            PropTableRow(
                                prop = prop,
                                isSelected = state.selectedPropIds.contains(prop.id),
                                onSelect = { viewModel.togglePropSelection(prop.id) },
                                onStatusChange = { viewModel.updatePropStatus(prop.id, it) },
                                onCategoryChange = { viewModel.updatePropCategory(prop.id, it) },
                                onQuantityChange = { viewModel.updatePropQuantity(prop.id, it) },
                                onCrossCuttingChange = { viewModel.updatePropCrossCutting(prop.id, it) },
                                onNoteChange = { viewModel.updatePropNote(prop.id, it) }
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun SearchAndFilters(state: PropAssetManagerUiState, viewModel: PropAssetManagerViewModel) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 16.dp)) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = { Text("Поиск...") },
                modifier = Modifier.width(200.dp).padding(end = 8.dp),
                leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp)) },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium
            )

            // Фильтр категорий
            var expanded by remember { mutableStateOf(false) }
            Box {
                FilterChip(
                    selected = state.selectedCategory != null,
                    onClick = { expanded = true },
                    label = { Text(state.selectedCategory ?: "Все категории") },
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) }
                )
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Все категории") },
                        onClick = { viewModel.updateCategoryFilter(null); expanded = false }
                    )
                    state.categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = { viewModel.updateCategoryFilter(cat); expanded = false }
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun BulkActionsPanel(state: PropAssetManagerUiState, viewModel: PropAssetManagerViewModel) {
        AnimatedVisibility(visible = state.selectedPropIds.isNotEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Выбрано: ${state.selectedPropIds.size}", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(24.dp))
                    
                    PropStatus.entries.forEach { status ->
                        Button(
                            onClick = { /* TODO: viewModel.bulkUpdateStatus(status) */ },
                            modifier = Modifier.padding(horizontal = 4.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(status.displayName, fontSize = 12.sp)
                        }
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = { viewModel.clearSelection() }) {
                        Text("Отмена")
                    }
                }
            }
        }
    }

    @Composable
    private fun PropTableHeader(
        isAllSelected: Boolean,
        onSelectAll: () -> Unit,
        onUnselectAll: () -> Unit
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = isAllSelected, onCheckedChange = { if (it) onSelectAll() else onUnselectAll() })
            Text("Фото", modifier = Modifier.width(60.dp), fontWeight = FontWeight.Bold)
            Text("Название", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold)
            Text("Категория", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold)
            Text("Сцены", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
            Text("Кол-во", modifier = Modifier.width(80.dp), fontWeight = FontWeight.Bold)
            Text("Сквозной", modifier = Modifier.width(100.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text("Статус", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun PropTableRow(
        prop: PropWithScene,
        isSelected: Boolean,
        onSelect: () -> Unit,
        onStatusChange: (PropStatus) -> Unit,
        onCategoryChange: (String) -> Unit,
        onQuantityChange: (Int) -> Unit,
        onCrossCuttingChange: (Boolean) -> Unit,
        onNoteChange: (String?) -> Unit
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = isSelected, onCheckedChange = { onSelect() })
            
            // Фото (Placeholder)
            Box(modifier = Modifier.width(60.dp).height(40.dp).background(Color.LightGray, MaterialTheme.shapes.small), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Image, null, tint = Color.Gray)
            }

            // Название
            Column(modifier = Modifier.weight(2f).padding(horizontal = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(prop.name, fontWeight = FontWeight.Medium)
                    if (prop.isOrphaned) {
                        Icon(Icons.Default.Warning, "Orphaned", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp).padding(start = 4.dp))
                    }
                }
                Text(prop.anchor, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, maxLines = 1)
            }

            // Категория
            CategoryCell(prop.category, onCategoryChange, modifier = Modifier.weight(1.5f))

            // Сцены (Чип)
            Box(modifier = Modifier.weight(1f)) {
                SuggestionChip(
                    onClick = { /* Перейти к сцене */ },
                    label = { Text("${prop.seriesNumber}-${prop.sceneNumber}", fontSize = 11.sp) }
                )
            }

            // Кол-во
            var qtyText by remember(prop.quantity) { mutableStateOf(prop.quantity.toString()) }
            OutlinedTextField(
                value = qtyText,
                onValueChange = { 
                    qtyText = it
                    it.toIntOrNull()?.let { n -> onQuantityChange(n) }
                },
                modifier = Modifier.width(70.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall
            )

            // Сквозной
            Checkbox(checked = prop.isCrossCutting, onCheckedChange = onCrossCuttingChange, modifier = Modifier.width(100.dp))

            // Статус
            StatusDropdown(prop.status, onStatusChange, modifier = Modifier.weight(1.5f))
        }
    }

    @Composable
    private fun CategoryCell(currentCategory: String, onCategoryChange: (String) -> Unit, modifier: Modifier) {
        var expanded by remember { mutableStateOf(false) }
        val commonCategories = listOf("Мебель", "Текстиль", "Посуда", "Техника", "Прочее")

        Box(modifier = modifier) {
            TextButton(onClick = { expanded = true }) {
                Text(currentCategory, color = MaterialTheme.colorScheme.onSurface)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                commonCategories.forEach { cat ->
                    DropdownMenuItem(
                        text = { Text(cat) },
                        onClick = { onCategoryChange(cat); expanded = false }
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun StatusDropdown(currentStatus: String, onStatusChange: (PropStatus) -> Unit, modifier: Modifier) {
        var expanded by remember { mutableStateOf(false) }
        val status = PropStatus.fromString(currentStatus)
        
        Box(modifier = modifier) {
            AssistChip(
                onClick = { expanded = true },
                label = { Text(status.displayName) },
                colors = AssistChipDefaults.assistChipColors(
                    labelColor = when(status) {
                        PropStatus.READY -> Color(0xFF2E7D32)
                        PropStatus.BOUGHT -> Color(0xFF1976D2)
                        PropStatus.LOST -> Color(0xFFD32F2F)
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            )
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                PropStatus.entries.forEach { s ->
                    DropdownMenuItem(
                        text = { Text(s.displayName) },
                        onClick = {
                            onStatusChange(s)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
