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
    suspend fun markProjectAsRemote(id: String)
}

class ProjectRepositoryImpl(
    private val queries: DatabaseQueries,
    private val syncRepository: SyncRepository,
    private val authRepository: AuthRepository
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
        val id = generateUUID()
        val creatorEmail = authRepository.getCurrentUserSync()?.email
        
        // Новые проекты по умолчанию локальные (isRemote = 0)
        queries.insertProject(
            id = id, 
            name = name, 
            director = director, 
            updatedAt = now, 
            isRemote = 0L, 
            created_by = creatorEmail
        )
    }

    override suspend fun markProjectAsRemote(id: String) {
        queries.markProjectAsRemote(id)
    }

    override suspend fun deleteProject(id: String) {
        val project = queries.getProjectById(id).executeAsOneOrNull()
        queries.deleteProject(id)
        
        if (project?.isRemote == 1L) {
            syncRepository.enqueue("DELETE", "Project", id, id, null)
        }
    }

    override suspend fun updateProject(id: String, name: String, director: String) {
        val now = Clock.System.now().toEpochMilliseconds()
        val project = queries.getProjectById(id).executeAsOneOrNull()
        
        queries.updateProject(name, director, now, id)
        
        if (project?.isRemote == 1L) {
            syncRepository.enqueue("UPDATE", "Project", id, id, null)
        }
    }
}
