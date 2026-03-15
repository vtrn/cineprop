package org.mosyagin.project.db

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn


import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.mosyagin.project.DatabaseQueries
import org.mosyagin.project.Project

class ProjectListScreenModel(private val queries: DatabaseQueries) : ScreenModel {

    private val _projects = MutableStateFlow<List<Project>>(emptyList())
    val projects: StateFlow<List<Project>> = queries.getAllProjects()
        .asFlow()
        .mapToList(Dispatchers.Default) // Default стабильнее для мультиплатформы
        .stateIn(
            scope = screenModelScope, // Привязываем к жизненному циклу экрана
            started = SharingStarted.WhileSubscribed(5000), // Пауза при сворачивании
            initialValue = emptyList() // Значение до первой загрузки
        )

    init {
        loadProjects()
    }

    private fun loadProjects() {
        screenModelScope.launch {
            _projects.value = queries.getAllProjects().executeAsList()
        }
    }

    fun addProject(name: String, director: String) {
        screenModelScope.launch {
            queries.insertProject(name, director)
            loadProjects()
        }
    }
}
