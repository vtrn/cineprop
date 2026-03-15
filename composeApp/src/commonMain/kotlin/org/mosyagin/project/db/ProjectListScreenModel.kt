package org.mosyagin.project.db

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProjectListScreenModel(private val queries: DatabaseQueries) : ScreenModel {

    // Состояние: список наших проектов
    private val _projects = MutableStateFlow<List<Project>>(emptyList())
    val projects: StateFlow<List<Project>> = _projects

    init {
        loadProjects()
    }

    private fun loadProjects() {
        screenModelScope.launch {
            // Берем данные из сгенерированных SQLDelight-ом запросов
            _projects.value = queries.getAllProjects().executeAsList()
        }
    }

    fun addProject(name: String, director: String) {
        screenModelScope.launch {
            queries.insertProject(name, director)
            loadProjects() // Обновляем список после вставки
        }
    }
}
