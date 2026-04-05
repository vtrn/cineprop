package org.mosyagin.project.ui.screens

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.mosyagin.project.Actor
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

/**
 * Событие для очереди синхронизации
 */
data class SyncEvent(
    val operation: String,
    val tableName: String,
    val recordId: Long
)

@OptIn(FlowPreview::class)
class PropWorkspaceViewModel(
    private val projectId: Long,
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

    // Поток событий для синхронизации с debounce
    private val syncEvents = MutableSharedFlow<SyncEvent>()

    init {
        // Issue #3: Защита от write amplification через debounce
        syncEvents
            .debounce(1500L)
            .onEach { event ->
                syncRepository.enqueue(event.operation, event.tableName, event.recordId, null)
            }
            .launchIn(screenModelScope)
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedPropId = MutableStateFlow<Long?>(null)
    val selectedPropId: StateFlow<Long?> = _selectedPropId.asStateFlow()

    private val _selectedPropIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedPropIds: StateFlow<Set<Long>> = _selectedPropIds.asStateFlow()

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

    /**
     * Группировка реквизита по сменам КПП.
     */
    val propsByShift: StateFlow<Map<Long, List<PropWithScene>>> = combine(
        sceneRepository.getPropsWithShiftByProject(projectId), 
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

        val sorted = when (sortCol) {
            PropSortColumn.NAME -> filtered.sortedBy { it.name }
            PropSortColumn.CATEGORY -> filtered.sortedBy { it.category }
            PropSortColumn.SCENE -> filtered.sortedWith(compareBy({ it.seriesNumber }, { it.sceneNumber }))
            PropSortColumn.QUANTITY -> filtered.sortedBy { it.quantity }
            PropSortColumn.STATUS -> filtered.sortedBy { it.status }
        }

        if (ascending) sorted else sorted.reversed()
    }.stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val propsByCategory: StateFlow<Map<String, List<PropWithScene>>> = allProps.map { props ->
        val grouped = props.groupBy { it.category.lowercase() }
        val result = mutableMapOf<String, List<PropWithScene>>()
        
        DEFAULT_CATEGORIES.forEach { cat ->
            result[cat] = grouped[cat] ?: emptyList()
        }
        
        grouped.forEach { (cat, list) ->
            if (!result.containsKey(cat)) {
                result[cat] = list
            }
        }
        result
    }.stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onPropSelected(id: Long?) {
        _selectedPropId.value = id
    }

    fun onCategoryFilterSelected(category: String?) {
        _selectedCategoryFilter.value = category?.lowercase()
    }

    fun toggleKppMode() {
        _isKppMode.value = !_isKppMode.value
    }

    fun toggleSort(column: PropSortColumn) {
        if (_sortColumn.value == column) {
            _isSortAscending.value = !_isSortAscending.value
        } else {
            _sortColumn.value = column
            _isSortAscending.value = true
        }
    }

    fun togglePropSelection(propId: Long) {
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

    /**
     * Основная логика экспорта реквизита.
     */
    fun performExport(grouping: ExportGrouping, format: ExportFormat) {
        screenModelScope.launch {
            // 1. Получаем данные проекта для заголовка
            val project = projectRepository.getProjectById(projectId).firstOrNull() ?: return@launch
            
            // 2. Получаем актуальный список реквизита
            val props = if (grouping == ExportGrouping.BY_KPP) {
                sceneRepository.getPropsWithShiftByProject(projectId).first()
            } else {
                sceneRepository.getPropsByProject(projectId).first()
            }

            // 3. Генерируем байты файла
            val bytes = propExporter.export(project.name, grouping, format, props)
            
            if (bytes.isNotEmpty()) {
                // 4. Сохраняем файл на диск
                val fileName = "PropList_${project.name}_${if(grouping == ExportGrouping.BY_KPP) "KPP" else "Script"}.${if(format == ExportFormat.EXCEL) "xlsx" else "pdf"}"
                val mimeType = if (format == ExportFormat.EXCEL) {
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                } else {
                    "application/pdf"
                }
                
                fileSaver.saveFile(fileName, mimeType, bytes)
            }
        }
    }

    // Удаление реквизита (без debounce, так как действие разовое)
    fun deleteProp(propId: Long) {
        screenModelScope.launch {
            sceneRepository.deleteProp(propId)
            if (_selectedPropId.value == propId) {
                _selectedPropId.value = null
            }
            _selectedPropIds.update { it - propId }
        }
    }

    fun deleteSelectedProps() {
        val ids = _selectedPropIds.value.toList()
        screenModelScope.launch {
            ids.forEach { sceneRepository.deleteProp(it) }
            clearSelection()
            if (ids.contains(_selectedPropId.value)) {
                _selectedPropId.value = null
            }
        }
    }

    // Операции обновления с защитой Debounce
    fun updatePropStatus(propId: Long, status: PropStatus) {
        screenModelScope.launch {
            // 1. Сразу в локальную БД
            sceneRepository.updatePropStatus(propId, status.displayName)
            // 2. В очередь синхронизации с задержкой
            syncEvents.emit(SyncEvent("UPDATE", "Prop", propId))
        }
    }

    fun updatePropCategory(propId: Long, category: String) {
        screenModelScope.launch {
            sceneRepository.updatePropCategory(propId, category.lowercase())
            syncEvents.emit(SyncEvent("UPDATE", "Prop", propId))
        }
    }

    fun updatePropQuantity(propId: Long, quantity: Int) {
        screenModelScope.launch {
            sceneRepository.updatePropQuantity(propId, quantity)
            syncEvents.emit(SyncEvent("UPDATE", "Prop", propId))
        }
    }

    fun updatePropCrossCutting(propId: Long, isCrossCutting: Boolean) {
        screenModelScope.launch {
            sceneRepository.updatePropCrossCutting(propId, isCrossCutting)
            syncEvents.emit(SyncEvent("UPDATE", "Prop", propId))
        }
    }

    fun updatePropNote(propId: Long, note: String?) {
        screenModelScope.launch {
            sceneRepository.updatePropNote(propId, note)
            syncEvents.emit(SyncEvent("UPDATE", "Prop", propId))
        }
    }

    fun bulkUpdateStatus(status: PropStatus) {
        val ids = _selectedPropIds.value.toList()
        if (ids.isNotEmpty()) {
            screenModelScope.launch {
                sceneRepository.bulkUpdatePropStatus(ids, status.displayName)
                ids.forEach { syncEvents.emit(SyncEvent("UPDATE", "Prop", it)) }
                clearSelection()
            }
        }
    }

    fun confirmProps(propIds: List<Long>) {
        screenModelScope.launch {
            propIds.forEach { id ->
                sceneRepository.updatePropOrphanedStatus(id, false)
                syncEvents.emit(SyncEvent("UPDATE", "Prop", id))
            }
        }
    }
}
