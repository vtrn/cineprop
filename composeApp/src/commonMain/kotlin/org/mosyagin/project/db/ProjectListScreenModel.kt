package org.mosyagin.project.db

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.mosyagin.project.Project
import org.mosyagin.project.repository.ProjectRepository

class ProjectListScreenModel(private val repository: ProjectRepository) : ScreenModel {

    val projects: StateFlow<List<Project>> = repository.getAllProjects()
        .stateIn(
            scope = screenModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addProject(name: String, director: String) {
        screenModelScope.launch {
            repository.addProject(name, director)
        }
    }

    fun deleteProject(id: Long) {
        screenModelScope.launch {
            repository.deleteProject(id)
        }
    }
}
