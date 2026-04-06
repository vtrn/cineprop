@file:OptIn(FlowPreview::class)

package org.mosyagin.project.ui.screens

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.mosyagin.project.export.FileSaver
import org.mosyagin.project.export.PropExporter
import org.mosyagin.project.models.versioning.PropStatus
import org.mosyagin.project.repository.ProjectRepository
import org.mosyagin.project.repository.PropWithScene
import org.mosyagin.project.repository.SceneRepository
import org.mosyagin.project.repository.SyncRepository
import org.mosyagin.project.ui.components.props.ExportFormat
import org.mosyagin.project.ui.components.props.ExportGrouping

enum class PropSortColumn {
    NAME, CATEGORY, SCENE, QUANTITY, STATUS
}

data class SyncEvent(
    val operation: String,
    val tableName: String,
    val recordId: String 
)

class PropWorkspaceViewModel(
    private val projectId: String,
    private val sceneRepository: SceneRepository,
    private val projectRepository: ProjectRepository,
    private val syncRepository: SyncRepository,
    private val propExporter: PropExporter,
    private val fileSaver: FileSaver
) : ScreenModel {

    companion object {
        val DEFAULT_CATEGORIES = listOf(
            "персонажный",
            "транспорт",
            "типографика",
            "графика",
            "исходящий",
            "животные",
            "оружие",
            "прочее"
        )
    }

    private val syncEvents = MutableSharedFlow<SyncEvent>()

    init {
        syncEvents
            .debounce(1500L)
            .onEach { event ->
                syncRepository.enqueue(event.operation, event.tableName, event.recordId, null)
            }
            .launchIn(screenModelScope)
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedPropId = MutableStateFlow<String?>(null)
    val selectedPropId: StateFlow<String?> = _selectedPropId.asStateFlow()

    private val _selectedPropIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedPropIds: StateFlow<Set<String>> = _selectedPropIds.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow<String?>(null)
    val selectedCategoryFilter: StateFlow<String?> = _selectedCategoryFilter.asStateFlow()

    private val _sortColumn = MutableStateFlow(PropSortColumn.NAME)
    val sortColumn: StateFlow<PropSortColumn> = _sortColumn.asStateFlow()

    private val _isSortAscending = MutableStateFlow(true)
    val isSortAscending: StateFlow<Boolean> = _isSortAscending.asStateFlow()

    private val _isKppMode = MutableStateFlow(false)
    val isKppMode: StateFlow<Boolean> = _isKppMode.asStateFlow()

    private val allProps = sceneRepository.getPropsByProject(projectId)
        .stateIn(screenModelScope, SharingStarted.Eagerly, emptyList())

    val projectActors = sceneRepository.getActorsByProject(projectId)
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val propsByShift: StateFlow<Map<Long, List<PropWithScene>>> = combine(
        allProps,
        _searchQuery
    ) { props, query ->
        props.filter { it.name.contains(query, ignoreCase = true) }
             .groupBy { it.shiftNumber ?: 0L }
    }.stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val categories: StateFlow<List<String>> = allProps.map { list ->
        val fromDb = list.map { it.category.lowercase() }.distinct()
        (DEFAULT_CATEGORIES + fromDb).distinct().sorted()
    }.stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), DEFAULT_CATEGORIES)

    val filteredProps: StateFlow<List<PropWithScene>> = combine(
        allProps, _searchQuery, _selectedCategoryFilter, _sortColumn, _isSortAscending
    ) { props, query, category, sortCol, ascending ->
        val filtered = props.filter { prop ->
            val matchesQuery = prop.name.contains(query, ignoreCase = true) || 
                             prop.sceneNumber.contains(query, ignoreCase = true) ||
                             prop.anchor.contains(query, ignoreCase = true)
            
            val matchesCategory = category == null || prop.category.lowercase() == category.lowercase()
            matchesQuery && matchesCategory
        }

        // Создаем карту для группировки. Ключ - effectiveGroupId, значение - имя "родителя" (первого в группе)
        val groupNames = props.groupBy { it.groupId ?: it.id }.mapValues { it.value.first().name }

        val sorted = when (sortCol) {
            PropSortColumn.NAME -> filtered.sortedWith(
                compareBy<PropWithScene> { groupNames[it.groupId ?: it.id] ?: it.name }
                .thenBy { it.name }
                .thenBy { it.seriesNumber }
                .thenBy { it.sceneNumber }
            )
            PropSortColumn.CATEGORY -> filtered.sortedBy { it.category }
            PropSortColumn.SCENE -> filtered.sortedWith(compareBy({ it.seriesNumber }, { it.sceneNumber }))
            PropSortColumn.QUANTITY -> filtered.sortedBy { it.quantity }
            PropSortColumn.STATUS -> filtered.sortedBy { it.status }
        }

        if (ascending) sorted else sorted.reversed()
    }.stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onPropSelected(id: String?) {
        _selectedPropId.value = id
    }

    fun togglePropSelection(propId: String) {
        _selectedPropIds.update { current ->
            if (current.contains(propId)) current - propId else current + propId
        }
    }

    fun selectAllFiltered() {
        _selectedPropIds.value = filteredProps.value.map { it.id }.toSet()
    }

    fun clearSelection() {
        _selectedPropIds.value = emptySet()
    }

    fun onSortColumnChange(column: PropSortColumn) {
        if (_sortColumn.value == column) {
            _isSortAscending.value = !_isSortAscending.value
        } else {
            _sortColumn.value = column
            _isSortAscending.value = true
        }
    }

    fun toggleKppMode() {
        _isKppMode.value = !_isKppMode.value
    }

    fun deleteProp(propId: String) {
        screenModelScope.launch {
            sceneRepository.deleteProp(propId)
            if (_selectedPropId.value == propId) {
                _selectedPropId.value = null
            }
            _selectedPropIds.update { it - propId }
        }
    }

    fun deleteSelectedProps() {
        val selected = _selectedPropIds.value
        screenModelScope.launch {
            selected.forEach { id ->
                sceneRepository.deleteProp(id)
            }
            _selectedPropIds.value = emptySet()
        }
    }

    fun confirmProps(propIds: List<String>) {
        screenModelScope.launch {
            propIds.forEach { id ->
                updatePropStatus(id, PropStatus.READY)
            }
        }
    }

    fun updatePropStatus(propId: String, status: PropStatus) {
        screenModelScope.launch {
            sceneRepository.updatePropStatus(propId, status.displayName)
            syncEvents.emit(SyncEvent("UPDATE", "Prop", propId))
        }
    }

    fun updatePropCategory(propId: String, category: String) {
        screenModelScope.launch {
            sceneRepository.updatePropCategory(propId, category.lowercase())
            syncEvents.emit(SyncEvent("UPDATE", "Prop", propId))
        }
    }

    fun updatePropQuantity(propId: String, quantity: Int) {
        screenModelScope.launch {
            sceneRepository.updatePropQuantity(propId, quantity)
            syncEvents.emit(SyncEvent("UPDATE", "Prop", propId))
        }
    }

    fun updatePropCrossCutting(propId: String, isCrossCutting: Boolean) {
        screenModelScope.launch {
            sceneRepository.updatePropCrossCutting(propId, isCrossCutting)
            syncEvents.emit(SyncEvent("UPDATE", "Prop", propId))
        }
    }

    fun updatePropNote(propId: String, note: String?) {
        screenModelScope.launch {
            sceneRepository.updatePropNote(propId, note)
            syncEvents.emit(SyncEvent("UPDATE", "Prop", propId))
        }
    }

    fun onCategoryFilterSelected(category: String?) {
        _selectedCategoryFilter.value = category
    }

    fun performExport(format: ExportGrouping, grouping: ExportFormat) {
        // Implementation for export
    }
}
