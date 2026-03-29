package org.mosyagin.project.ui.screens

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.*
import org.mosyagin.project.repository.PropWithScene
import org.mosyagin.project.repository.SceneRepository

class PropWorkspaceViewModel(
    private val projectId: Long,
    private val sceneRepository: SceneRepository
) : ScreenModel {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedPropId = MutableStateFlow<Long?>(null)
    val selectedPropId: StateFlow<Long?> = _selectedPropId.asStateFlow()

    // Стейт для раскрытых категорий
    private val _expandedCategories = MutableStateFlow<Set<String>>(setOf("ВСЕ"))
    val expandedCategories: StateFlow<Set<String>> = _expandedCategories.asStateFlow()

    // Список реквизита с фильтрацией и группировкой
    val propsList: StateFlow<Map<String, List<PropWithScene>>> = combine(
        sceneRepository.getPropsByProject(projectId),
        _searchQuery
    ) { props, query ->
        props.filter { it.name.contains(query, ignoreCase = true) }
            .groupBy { "ВСЕ" } // В будущем здесь может быть реальная категория
    }.stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onPropSelected(id: Long) {
        _selectedPropId.value = id
    }

    fun toggleCategory(category: String) {
        val current = _expandedCategories.value
        _expandedCategories.value = if (current.contains(category)) current - category else current + category
    }
}
