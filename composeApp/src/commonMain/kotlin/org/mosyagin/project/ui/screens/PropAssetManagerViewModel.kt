package org.mosyagin.project.ui.screens

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.mosyagin.project.models.versioning.PropStatus
import org.mosyagin.project.repository.PropWithScene
import org.mosyagin.project.repository.SceneRepository

data class PropAssetManagerUiState(
    val isLoading: Boolean = true,
    val props: List<PropWithScene> = emptyList(),
    val filteredProps: List<PropWithScene> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: String? = null,
    val selectedStatus: PropStatus? = null,
    val selectedPropIds: Set<Long> = emptySet(),
    val categories: List<String> = emptyList(),
    val error: String? = null
)

class PropAssetManagerViewModel(
    private val repository: SceneRepository,
    private val projectId: Long
) : ScreenModel {

    private val _uiState = MutableStateFlow(PropAssetManagerUiState())
    val uiState: StateFlow<PropAssetManagerUiState> = _uiState.asStateFlow()

    init {
        loadProps()
    }

    private fun loadProps() {
        screenModelScope.launch {
            repository.getPropsByProject(projectId)
                .onStart { _uiState.update { it.copy(isLoading = true) } }
                .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
                .collect { allProps ->
                    val categories = allProps.map { it.category }.distinct().sorted()
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            props = allProps,
                            categories = categories,
                            filteredProps = filterProps(allProps, state.searchQuery, state.selectedCategory, state.selectedStatus)
                        )
                    }
                }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { state ->
            state.copy(
                searchQuery = query,
                filteredProps = filterProps(state.props, query, state.selectedCategory, state.selectedStatus)
            )
        }
    }

    fun updateCategoryFilter(category: String?) {
        _uiState.update { state ->
            state.copy(
                selectedCategory = category,
                filteredProps = filterProps(state.props, state.searchQuery, category, state.selectedStatus)
            )
        }
    }

    fun updateStatusFilter(status: PropStatus?) {
        _uiState.update { state ->
            state.copy(
                selectedStatus = status,
                filteredProps = filterProps(state.props, state.searchQuery, state.selectedCategory, status)
            )
        }
    }

    private fun filterProps(
        props: List<PropWithScene>,
        query: String,
        category: String?,
        status: PropStatus?
    ): List<PropWithScene> {
        return props.filter { prop ->
            val matchesQuery = prop.name.contains(query, ignoreCase = true) || 
                             prop.sceneNumber.contains(query, ignoreCase = true)
            val matchesCategory = category == null || prop.category == category
            val matchesStatus = status == null || prop.status == status.displayName
            matchesQuery && matchesCategory && matchesStatus
        }
    }

    fun togglePropSelection(propId: Long) {
        _uiState.update { state ->
            val newSelection = if (state.selectedPropIds.contains(propId)) {
                state.selectedPropIds - propId
            } else {
                state.selectedPropIds + propId
            }
            state.copy(selectedPropIds = newSelection)
        }
    }

    fun selectAllFiltered() {
        _uiState.update { state ->
            state.copy(selectedPropIds = state.filteredProps.map { it.id }.toSet())
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedPropIds = emptySet()) }
    }

    // Update operations
    fun updatePropStatus(propId: Long, status: PropStatus) {
        screenModelScope.launch {
            repository.updatePropStatus(propId, status.displayName)
        }
    }

    fun updatePropCategory(propId: Long, category: String) {
        screenModelScope.launch {
            repository.updatePropCategory(propId, category)
        }
    }

    fun updatePropQuantity(propId: Long, quantity: Int) {
        screenModelScope.launch {
            repository.updatePropQuantity(propId, quantity)
        }
    }

    fun updatePropCrossCutting(propId: Long, isCrossCutting: Boolean) {
        screenModelScope.launch {
            repository.updatePropCrossCutting(propId, isCrossCutting)
        }
    }

    fun updatePropNote(propId: Long, note: String?) {
        screenModelScope.launch {
            repository.updatePropNote(propId, note)
        }
    }

    // Bulk actions
    fun bulkUpdateStatus(status: PropStatus) {
        val selectedIds = _uiState.value.selectedPropIds.toList()
        if (selectedIds.isNotEmpty()) {
            screenModelScope.launch {
                repository.bulkUpdatePropStatus(selectedIds, status.displayName)
                clearSelection()
            }
        }
    }
}
