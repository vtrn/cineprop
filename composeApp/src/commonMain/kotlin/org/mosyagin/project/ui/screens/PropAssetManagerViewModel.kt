package org.mosyagin.project.ui.screens

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.mosyagin.project.models.versioning.PropStatus
import org.mosyagin.project.repository.PropWithScene
import org.mosyagin.project.repository.SceneRepository
import org.mosyagin.project.repository.SyncEvent
import org.mosyagin.project.repository.SyncRepository

data class PropAssetManagerUiState(
    val isLoading: Boolean = true,
    val props: List<PropWithScene> = emptyList(),
    val filteredProps: List<PropWithScene> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: String? = null,
    val selectedStatus: PropStatus? = null,
    val selectedPropIds: Set<String> = emptySet(), 
    val categories: List<String> = emptyList(),
    val error: String? = null
)

@OptIn(FlowPreview::class)
class PropAssetManagerViewModel(
    private val repository: SceneRepository,
    private val syncRepository: SyncRepository,
    private val projectId: String 
) : ScreenModel {

    private val _uiState = MutableStateFlow(PropAssetManagerUiState())
    val uiState: StateFlow<PropAssetManagerUiState> = _uiState.asStateFlow()

    private val syncEvents = MutableSharedFlow<SyncEvent>()

    init {
        syncEvents
            .debounce(1500L)
            .onEach { event ->
                syncRepository.enqueue(event.operation, event.tableName, event.recordId, projectId, event.dataJson)
            }
            .launchIn(screenModelScope)
            
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

    fun togglePropSelection(propId: String) { 
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

    fun updatePropStatus(propId: String, status: PropStatus) { 
        screenModelScope.launch {
            repository.updatePropStatus(propId, status.displayName)
            syncEvents.emit(SyncEvent("UPDATE", "Prop", propId))
        }
    }

    fun updatePropCategory(propId: String, category: String) { 
        screenModelScope.launch {
            repository.updatePropCategory(propId, category)
            syncEvents.emit(SyncEvent("UPDATE", "Prop", propId))
        }
    }

    fun updatePropQuantity(propId: String, quantity: Int) { 
        screenModelScope.launch {
            repository.updatePropQuantity(propId, quantity)
            syncEvents.emit(SyncEvent("UPDATE", "Prop", propId))
        }
    }

    fun updatePropCrossCutting(propId: String, isCrossCutting: Boolean) { 
        screenModelScope.launch {
            repository.updatePropCrossCutting(propId, isCrossCutting)
            syncEvents.emit(SyncEvent("UPDATE", "Prop", propId))
        }
    }

    fun updatePropNote(propId: String, note: String?) {
        screenModelScope.launch {
            repository.updatePropNote(propId, note)
            syncEvents.emit(SyncEvent("UPDATE", "Prop", propId))
        }
    }
}
