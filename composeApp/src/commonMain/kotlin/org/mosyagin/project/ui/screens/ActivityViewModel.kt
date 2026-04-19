package org.mosyagin.project.ui.screens

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.mosyagin.project.ActivityLog
import org.mosyagin.project.repository.ActivityRepository

enum class ActivityFilter { ALL, SCENARIO, KPP, PROP, PHOTO }

class ActivityViewModel(
    private val projectId: String,
    private val activityRepository: ActivityRepository
) : ScreenModel {

    private val _selectedFilter = MutableStateFlow(ActivityFilter.ALL)
    val selectedFilter: StateFlow<ActivityFilter> = _selectedFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val activities: StateFlow<List<ActivityLog>> = combine(
        activityRepository.getActivities(projectId),
        _selectedFilter,
        _searchQuery
    ) { logs, filter, query ->
        logs.filter { log ->
            val matchesFilter = when (filter) {
                ActivityFilter.ALL -> true
                ActivityFilter.SCENARIO -> log.type == "SCENARIO"
                ActivityFilter.KPP -> log.type == "KPP"
                ActivityFilter.PROP -> log.type == "PROP"
                ActivityFilter.PHOTO -> log.type == "PHOTO"
            }
            
            // Простой поиск по расшифрованным полям
            val matchesQuery = if (query.isBlank()) true else {
                (log.encryptedEntityName?.contains(query, ignoreCase = true) == true) ||
                (log.encryptedDescription?.contains(query, ignoreCase = true) == true) ||
                (log.userName.contains(query, ignoreCase = true))
            }
            
            matchesFilter && matchesQuery
        }
    }.stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // При запуске пробуем расшифровать всё, что еще не расшифровано
        screenModelScope.launch {
            activityRepository.decryptActivities(projectId)
        }
    }

    fun onFilterSelected(filter: ActivityFilter) {
        _selectedFilter.value = filter
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }
    
    fun refresh() {
        screenModelScope.launch {
            activityRepository.decryptActivities(projectId)
        }
    }
}
