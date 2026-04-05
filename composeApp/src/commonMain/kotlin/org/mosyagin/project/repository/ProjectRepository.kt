package org.mosyagin.project.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

import org.mosyagin.project.DatabaseQueries
import org.mosyagin.project.Project
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

interface ProjectRepository {
    fun getAllProjects(): Flow<List<Project>>
    fun getProjectById(id: Long): Flow<Project?>
    suspend fun addProject(name: String, director: String)
    suspend fun deleteProject(id: Long)
    suspend fun updateProject(id: Long, name: String, director: String)
}

class ProjectRepositoryImpl(
    private val queries: DatabaseQueries,
    private val syncRepository: SyncRepository
) : ProjectRepository {
    override fun getAllProjects(): Flow<List<Project>> =
        queries.getAllProjects()
            .asFlow()
            .mapToList(Dispatchers.Default)

    override fun getProjectById(id: Long): Flow<Project?> =
        queries.getProjectById(id)
            .asFlow()
            .map { it.executeAsOneOrNull() }

    @OptIn(ExperimentalTime::class)
    override suspend fun addProject(name: String, director: String) {
        val now = Clock.System.now().toEpochMilliseconds()
        queries.insertProject(name, director, now)
        val id = queries.lastInsertRowId().executeAsOne()
        syncRepository.enqueue("INSERT", "Project", id, null)
    }

    override suspend fun deleteProject(id: Long) {
        queries.deleteProject(id)
        syncRepository.enqueue("DELETE", "Project", id, null)
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun updateProject(id: Long, name: String, director: String) {
        val now = Clock.System.now().toEpochMilliseconds()
        queries.updateProject(name, director, now, id)
        // Обновление проекта (Project) обычно не требует дебаунса во ViewModel, 
        // но если вы планируете печатать имя проекта, можно вызывать enqueue из ViewModel.
        // Для надежности добавим сюда прямое попадание в очередь для не-текстовых полей.
        syncRepository.enqueue("UPDATE", "Project", id, null)
    }
}
