@file:OptIn(ExperimentalMaterial3Api::class)

package org.mosyagin.project.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
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
import androidx.compose.ui.window.Popup
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.launch
import org.koin.core.parameter.parametersOf
import org.mosyagin.project.Actor
import org.mosyagin.project.GetLatestScenesForProject
import org.mosyagin.project.models.versioning.Prop
import org.mosyagin.project.models.versioning.PropStatus
import org.mosyagin.project.repository.PropWithScene
import org.mosyagin.project.ui.components.ThreePaneLayout
import org.mosyagin.project.ui.components.SceneMobileLayout
import org.mosyagin.project.ui.components.AppLayoutType
import org.mosyagin.project.ui.components.LocalAppLayoutType
import org.mosyagin.project.ui.components.CineCard
import org.mosyagin.project.ui.components.CineTag
import org.mosyagin.project.parser.ScriptParser

data class SceneWorkspaceScreen(val projectId: String) : Screen {

    @Composable
    override fun Content() {
        val screenModel = koinScreenModel<SceneWorkspaceViewModel> { parametersOf(projectId) }
        val navigator = LocalNavigator.currentOrThrow
        val layoutType = LocalAppLayoutType.current
        val scope = rememberCoroutineScope()
        val listState = rememberLazyListState()
        
        val scenes by screenModel.scenesList.collectAsState()
        val selectedId by screenModel.selectedSceneId.collectAsState()
        val searchQuery by screenModel.searchQuery.collectAsState()
        val selectedFilter by screenModel.selectedFilter.collectAsState()
        
        val sceneDetails by screenModel.selectedSceneDetails.collectAsState()
        val inspectorData by screenModel.selectedSceneInspector.collectAsState()

        var showAddPropDialog by remember { mutableStateOf(false) }
        var showSelectionPopup by remember { mutableStateOf(false) }
        var selectedAnchor by remember { mutableStateOf("") }
        
        // Мобильные состояния
        var isLeftExpanded by remember { mutableStateOf(false) }
        var isInspectorVisible by remember { mutableStateOf(false) }

        // Левая панель (Список сцен)
        val masterPane: @Composable (Boolean) -> Unit = { _ ->
            MasterPane(
                scenes = scenes,
                selectedId = selectedId,
                searchQuery = searchQuery,
                selectedFilter = selectedFilter,
                onSceneClick = { id -> 
                    screenModel.onSceneSelected(id)
                    if (layoutType == AppLayoutType.MOBILE) isLeftExpanded = false
                },
                onSearchChange = { screenModel.onSearchQueryChange(it) },
                onFilterClick = { screenModel.onFilterSelected(it) }
            )
        }

        // Центральная панель (Текст)
        val detailPane = @Composable {
            DetailPane(
                content = sceneDetails?.content,
                props = inspectorData?.props ?: emptyList(),
                selectedPropId = null,
                onPropClick = { /* Логика выделения */ },
                onTextSelected = { text ->
                    if (text.isNotBlank()) {
                        selectedAnchor = text.trim()
                        showSelectionPopup = true
                    }
                },
                listState = listState,
                onOpenInspector = { isInspectorVisible = true },
                onOpenMaster = { isLeftExpanded = true },
                layoutType = layoutType
            )
        }

        // Правая панель (Инспектор)
        val inspectorPane = @Composable {
            InspectorPane(
                data = inspectorData,
                onDeleteProp = { screenModel.deleteProp(it) },
                onPropSelected = { prop ->
                    val content = sceneDetails?.content
                    if (content != null) {
                        val blocks = ScriptParser().parseBlocks(content)
                        val index = blocks.indexOfFirst { it.text.lowercase().contains(prop.anchor.lowercase()) }
                        if (index != -1) {
                            scope.launch { 
                                listState.animateScrollToItem(index)
                                if (layoutType == AppLayoutType.MOBILE) isInspectorVisible = false
                            }
                        }
                    }
                },
                onShowDiff = { id -> navigator.push(SceneDiffScreen(id)) },
                onAcceptChanges = { screenModel.updateSceneReviewStatus(false) },
                onClose = if (layoutType == AppLayoutType.MOBILE) { { isInspectorVisible = false } } else null
            )
        }

        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            if (layoutType == AppLayoutType.DESKTOP) {
                ThreePaneLayout(
                    masterPane = { masterPane(false) },
                    detailPane = { detailPane() },
                    inspectorPane = { inspectorPane() }
                )
            } else {
                SceneMobileLayout(
                    masterPane = masterPane,
                    detailPane = detailPane,
                    inspectorPane = inspectorPane,
                    isLeftExpanded = isLeftExpanded,
                    onToggleLeft = { isLeftExpanded = it },
                    isInspectorVisible = isInspectorVisible,
                    onCloseInspector = { isInspectorVisible = false }
                )
            }

            if (showSelectionPopup) {
                Popup(alignment = Alignment.Center, onDismissRequest = { showSelectionPopup = false }) {
                    Card(
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Добавить реквизит?", style = MaterialTheme.typography.titleMedium)
                            Text("\"${selectedAnchor.take(30)}${if(selectedAnchor.length > 30) "..." else ""}\"", style = MaterialTheme.typography.bodySmall, maxLines = 1)
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { showSelectionPopup = false }) { Text("Нет") }
                                Button(onClick = {
                                    showSelectionPopup = false
                                    showAddPropDialog = true
                                }) { Text("Да") }
                            }
                        }
                    }
                }
            }
        }

        if (showAddPropDialog) {
            AddPropDialog(
                anchor = selectedAnchor,
                seriesNumber = inspectorData?.seriesNumber ?: 0,
                sceneNumber = inspectorData?.sceneNumber ?: "",
                actors = screenModel.projectActors.collectAsState().value,
                existingProps = screenModel.allProjectProps.collectAsState().value,
                onDismiss = { showAddPropDialog = false },
                onConfirm = { name, category, status, quantity, actorId, note, existingId ->
                    screenModel.addPropExtended(
                        name = name,
                        anchor = selectedAnchor,
                        category = category,
                        status = status,
                        quantity = quantity,
                        actorId = actorId,
                        note = note,
                        existingPropId = existingId
                    )
                    showAddPropDialog = false
                }
            )
        }
    }

    @Composable
    private fun AddPropDialog(
        anchor: String,
        seriesNumber: Long,
        sceneNumber: String,
        actors: List<Actor>,
        existingProps: List<PropWithScene>,
        onDismiss: () -> Unit,
        onConfirm: (String, String, String, Int, String?, String?, String?) -> Unit
    ) {
        var name by remember { mutableStateOf(anchor) }
        var quantity by remember { mutableStateOf("1") }
        var selectedCategory by remember { mutableStateOf("Прочее") }
        var selectedStatus by remember { mutableStateOf(PropStatus.PLANNED.displayName) }
        var selectedActorId by remember { mutableStateOf<String?>(null) }
        var notes by remember { mutableStateOf("") }
        var selectedExistingPropId by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Column {
                    Text("Добавление реквизита", style = MaterialTheme.typography.headlineSmall)
                    Text("Якорь: \"$anchor\" • Сцена $seriesNumber-$sceneNumber", 
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            },
            text = {
                Column(modifier = Modifier.widthIn(max = 450.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Название") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = quantity,
                            onValueChange = { if (it.all { c -> c.isDigit() }) quantity = it },
                            label = { Text("Кол-во") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        
                        ExposedDropdown(
                            label = "Категория",
                            options = listOf("Персонажный", "Транспорт", "Типографика", "Графика", "Исходящий", "Животные", "Оружие", "Прочее"),
                            selected = selectedCategory,
                            onSelect = { selectedCategory = it },
                            modifier = Modifier.weight(2f)
                        )
                    }

                    ExposedDropdown(
                        label = "Статус",
                        options = PropStatus.entries.map { it.displayName },
                        selected = selectedStatus,
                        onSelect = { selectedStatus = it },
                        modifier = Modifier.fillMaxWidth()
                    )

                    SearchableExposedDropdown(
                        label = "Принадлежит персонажу",
                        options = listOf("Нет") + actors.map { it.name },
                        selected = actors.find { it.id == selectedActorId }?.name ?: "Нет",
                        onSelect = { selectedName -> 
                            selectedActorId = actors.find { it.name == selectedName }?.id
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    SearchableExposedDropdown(
                        label = "Связать со сквозным (если есть)",
                        options = listOf("Новый предмет") + existingProps.map { it.name }.distinct(),
                        selected = existingProps.find { it.id == selectedExistingPropId }?.name ?: "Новый предмет",
                        onSelect = { propName ->
                            selectedExistingPropId = existingProps.find { it.name == propName }?.id
                            if (propName != "Новый предмет") name = propName
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Заметки") },
                        placeholder = { Text("Напр: цвет, материал, особенности...") },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        maxLines = 4
                    )
                }
            },
            confirmButton = {
                Button(onClick = { 
                    onConfirm(name, selectedCategory, selectedStatus, quantity.toIntOrNull() ?: 1, selectedActorId, notes.ifBlank { null }, selectedExistingPropId) 
                }) { Text("Добавить") }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Отменить") }
            }
        )
    }

    @Composable
    private fun SearchableExposedDropdown(
        label: String,
        options: List<String>,
        selected: String,
        onSelect: (String) -> Unit,
        modifier: Modifier = Modifier
    ) {
        var expanded by remember { mutableStateOf(false) }
        var query by remember { mutableStateOf("") }
        
        val filteredOptions = remember(query, options) {
            options.filter { it.contains(query, ignoreCase = true) }
        }

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = modifier
        ) {
            OutlinedTextField(
                value = if (expanded) query else selected,
                onValueChange = { 
                    query = it
                    expanded = true
                },
                label = { Text(label) },
                trailingIcon = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (expanded && query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp))
                            }
                        }
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    }
                },
                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true).fillMaxWidth(),
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                singleLine = true,
                placeholder = { if (expanded) Text("Поиск...") }
            )
            
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { 
                    expanded = false
                    query = ""
                }
            ) {
                if (filteredOptions.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("Ничего не найдено", color = Color.Gray, fontSize = 14.sp) },
                        onClick = { },
                        enabled = false
                    )
                } else {
                    filteredOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                onSelect(option)
                                expanded = false
                                query = ""
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun ExposedDropdown(
        label: String,
        options: List<String>,
        selected: String,
        onSelect: (String) -> Unit,
        modifier: Modifier = Modifier
    ) {
        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = modifier
        ) {
            OutlinedTextField(
                value = selected,
                onValueChange = {},
                readOnly = true,
                label = { Text(label) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true).fillMaxWidth(),
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
            }
        }
    }

    @Composable
    private fun MasterPane(
        scenes: List<GetLatestScenesForProject>,
        selectedId: String?,
        searchQuery: String,
        selectedFilter: SceneFilter,
        onSceneClick: (String) -> Unit,
        onSearchChange: (String) -> Unit,
        onFilterClick: (SceneFilter) -> Unit
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text("Сцены", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(16.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Поиск...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                shape = CircleShape,
                singleLine = true
            )

            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SceneFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { onFilterClick(filter) },
                        label = { Text(when (filter) { SceneFilter.ALL -> "Все"; SceneFilter.MODIFIED -> "Измененные"; SceneFilter.NEW -> "Новые" }) }
                    )
                }
            }

            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(scenes) { scene ->
                    val isSelected = scene.id == selectedId
                    val isBrandNew = scene.versionCount == 1L
                    val needsReview = scene.needsReview == 1L

                    CineCard(
                        onClick = { onSceneClick(scene.id) },
                        isSelected = isSelected,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Movie, null, modifier = Modifier.size(18.dp), tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Сцена ${scene.seriesNumber}-${scene.sceneNumber}", style = MaterialTheme.typography.labelMedium, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                    Row {
                                        if (isBrandNew) CineTag(text = "NEW", containerColor = Color(0xFFE8F5E9), contentColor = Color(0xFF2E7D32))
                                        if (needsReview) { Spacer(Modifier.width(4.dp)); CineTag(text = "UPD", containerColor = Color(0xFFFFF3E0), contentColor = Color(0xFFE65100)) }
                                    }
                                }
                                Text(scene.location, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun DetailPane(
        content: String?, 
        props: List<Prop>, 
        selectedPropId: String?, 
        onPropClick: (String) -> Unit, 
        onTextSelected: (String) -> Unit, 
        listState: androidx.compose.foundation.lazy.LazyListState,
        onOpenInspector: () -> Unit,
        onOpenMaster: () -> Unit,
        layoutType: AppLayoutType
    ) {
        val isMobile = layoutType == AppLayoutType.MOBILE
        Column(modifier = Modifier.fillMaxSize()) {
            if (isMobile && content != null) {
                TopAppBar(
                    title = { Text("Текст сценария", style = MaterialTheme.typography.titleMedium) },
                    navigationIcon = {
                        IconButton(onClick = onOpenMaster) {
                            Icon(Icons.Default.Menu, contentDescription = "Список сцен")
                        }
                    },
                    actions = {
                        IconButton(onClick = onOpenInspector) {
                            Icon(Icons.Default.Info, contentDescription = "Инфо")
                        }
                    }
                )
            }
            
            Box(modifier = Modifier.fillMaxSize()) {
                if (content != null) {
                    InteractiveScriptViewer(
                        blocks = ScriptParser().parseBlocks(content), 
                        props = props, 
                        selectedPropId = selectedPropId, 
                        onPropClick = onPropClick, 
                        onTextSelected = onTextSelected, 
                        modifier = Modifier.fillMaxSize(), 
                        listState = listState,
                        layoutType = layoutType
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { 
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Выберите сцену", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            if (isMobile) {
                                Spacer(Modifier.height(16.dp))
                                Button(onClick = onOpenMaster) {
                                    Icon(Icons.AutoMirrored.Filled.List, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Открыть список сцен")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun InspectorPane(
        data: SceneInspectorData?, 
        onDeleteProp: (String) -> Unit, 
        onPropSelected: (Prop) -> Unit, 
        onShowDiff: (String) -> Unit,
        onAcceptChanges: () -> Unit,
        onClose: (() -> Unit)? = null
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(if (onClose != null) 16.dp else 24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Инспектор", style = MaterialTheme.typography.titleLarge)
                if (onClose != null) {
                    IconButton(onClick = onClose) { Icon(Icons.Default.Close, null) }
                }
            }
            
            Spacer(Modifier.height(24.dp))
            
            if (data != null) {
                if (data.needsReview) {
                    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                        Button(
                            onClick = { onShowDiff(data.sceneId) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Difference, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Показать изменения", fontWeight = FontWeight.Bold)
                        }
                        
                        Spacer(Modifier.height(8.dp))
                        
                        OutlinedButton(
                            onClick = onAcceptChanges,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Check, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Принять все правки")
                        }
                    }
                }

                Text("Персонажи", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                data.actors.forEach { actor ->
                    ListItem(headlineContent = { Text(actor.name) }, leadingContent = { Icon(Icons.Default.Person, null, modifier = Modifier.size(18.dp)) }, colors = ListItemDefaults.colors(containerColor = Color.Transparent))
                }
                
                Spacer(Modifier.height(32.dp))
                
                Text("Реквизит", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                data.props.forEach { prop ->
                    ListItem(
                        headlineContent = { Text(prop.name) },
                        leadingContent = { Icon(Icons.Default.Inventory, null, modifier = Modifier.size(18.dp)) },
                        trailingContent = { IconButton(onClick = { onDeleteProp(prop.id) }) { Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp)) } },
                        modifier = Modifier.clickable { onPropSelected(prop) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            } else {
                Text("Выберите сцену", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            }
        }
    }
}
