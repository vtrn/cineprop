package org.mosyagin.project.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.mosyagin.project.DatabaseQueries
import org.mosyagin.project.Project

interface ProjectRepository {
    fun getAllProjects(): Flow<List<Project>>
    fun getProjectById(id: Long): Flow<Project?>
    suspend fun addProject(name: String, director: String)
    suspend fun deleteProject(id: Long)
}

class ProjectRepositoryImpl(private val queries: DatabaseQueries) : ProjectRepository {
    override fun getAllProjects(): Flow<List<Project>> =
        queries.getAllProjects()
            .asFlow()
            .mapToList(Dispatchers.Default)

    override fun getProjectById(id: Long): Flow<Project?> =
        queries.getProjectById(id)
            .asFlow()
            .map { it.executeAsOneOrNull() }

    override suspend fun addProject(name: String, director: String) {
        queries.insertProject(name, director)
    }

    override suspend fun deleteProject(id: Long) {
        queries.deleteProject(id)
    }
}
