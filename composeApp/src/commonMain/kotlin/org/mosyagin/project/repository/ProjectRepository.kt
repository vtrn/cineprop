@file:OptIn(ExperimentalTime::class)

package org.mosyagin.project.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

import org.mosyagin.project.DatabaseQueries
import org.mosyagin.project.Project
import org.mosyagin.project.generateUUID
import kotlin.time.Clock
import kotlin.time.ExperimentalTime


interface ProjectRepository {
    fun getAllProjects(): Flow<List<Project>>
    fun getProjectById(id: String): Flow<Project?>
    suspend fun addProject(name: String, director: String)
    suspend fun deleteProject(id: String)
    suspend fun updateProject(id: String, name: String, director: String)
}

class ProjectRepositoryImpl(
    private val queries: DatabaseQueries,
    private val syncRepository: SyncRepository
) : ProjectRepository {
    override fun getAllProjects(): Flow<List<Project>> =
        queries.getAllProjects()
            .asFlow()
            .mapToList(Dispatchers.Default)

    override fun getProjectById(id: String): Flow<Project?> =
        queries.getProjectById(id)
            .asFlow()
            .map { it.executeAsOneOrNull() }

    override suspend fun addProject(name: String, director: String) {
        val now = Clock.System.now().toEpochMilliseconds()
        val id = generateUUID() // Генерируем уникальный ID на клиенте
        
        queries.insertProject(id, name, director, now)
        syncRepository.enqueue("INSERT", "Project", id, null)
    }

    override suspend fun deleteProject(id: String) {
        queries.deleteProject(id)
        syncRepository.enqueue("DELETE", "Project", id, null)
    }

    override suspend fun updateProject(id: String, name: String, director: String) {
        val now = Clock.System.now().toEpochMilliseconds()
        queries.updateProject(name, director, now, id)
        syncRepository.enqueue("UPDATE", "Project", id, null)
    }
}
