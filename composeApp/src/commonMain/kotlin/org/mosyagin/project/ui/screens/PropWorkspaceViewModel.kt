package org.mosyagin.project.ui.screens

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.mosyagin.project.Actor
import org.mosyagin.project.models.versioning.PropStatus
import org.mosyagin.project.repository.PropWithScene
import org.mosyagin.project.repository.SceneRepository

enum class PropSortColumn {
    NAME, CATEGORY, SCENE, QUANTITY, STATUS
}

class PropWorkspaceViewModel(
    private val projectId: Long,
    private val sceneRepository: SceneRepository
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

    private val _expandedCategories = MutableStateFlow<Set<String>>(setOf("Все"))
    val expandedCategories: StateFlow<Set<String>> = _expandedCategories.asStateFlow()

    private val allProps = sceneRepository.getPropsByProject(projectId)
        .stateIn(screenModelScope, SharingStarted.Eagerly, emptyList())

    // ДОБАВЛЕНО: Список актеров проекта для отображения в инспекторе
    val projectActors = sceneRepository.getActorsByProject(projectId)
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    fun toggleSort(column: PropSortColumn) {
        if (_sortColumn.value == column) {
            _isSortAscending.value = !_isSortAscending.value
        } else {
            _sortColumn.value = column
            _isSortAscending.value = true
        }
    }

    fun toggleCategory(category: String) {
        val catLower = category.lowercase()
        val current = _expandedCategories.value
        _expandedCategories.value = if (current.contains(catLower)) current - catLower else current + catLower
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

    // Удаление реквизита
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

    // Update operations
    fun updatePropStatus(propId: Long, status: PropStatus) {
        screenModelScope.launch {
            sceneRepository.updatePropStatus(propId, status.displayName)
        }
    }

    fun updatePropCategory(propId: Long, category: String) {
        screenModelScope.launch {
            sceneRepository.updatePropCategory(propId, category.lowercase())
        }
    }

    fun updatePropQuantity(propId: Long, quantity: Int) {
        screenModelScope.launch {
            sceneRepository.updatePropQuantity(propId, quantity)
        }
    }

    fun updatePropCrossCutting(propId: Long, isCrossCutting: Boolean) {
        screenModelScope.launch {
            sceneRepository.updatePropCrossCutting(propId, isCrossCutting)
        }
    }

    fun updatePropNote(propId: Long, note: String?) {
        screenModelScope.launch {
            sceneRepository.updatePropNote(propId, note)
        }
    }

    fun bulkUpdateStatus(status: PropStatus) {
        val ids = _selectedPropIds.value.toList()
        if (ids.isNotEmpty()) {
            screenModelScope.launch {
                sceneRepository.bulkUpdatePropStatus(ids, status.displayName)
                clearSelection()
            }
        }
    }

    fun confirmProps(propIds: List<Long>) {
        screenModelScope.launch {
            propIds.forEach { id ->
                sceneRepository.updatePropOrphanedStatus(id, false)
            }
        }
    }
}
