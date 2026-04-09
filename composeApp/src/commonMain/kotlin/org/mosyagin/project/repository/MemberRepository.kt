package org.mosyagin.project.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import org.mosyagin.project.DatabaseQueries
import org.mosyagin.project.ProjectMember
import org.mosyagin.project.util.currentTimestamp

interface MemberRepository {
    fun getMembersByProject(projectId: String): Flow<List<ProjectMember>>
    suspend fun addMember(projectId: String, email: String, role: String)
    suspend fun removeMember(memberId: String)
    suspend fun addOwnerLocally(projectId: String, email: String)
}

class MemberRepositoryImpl(
    private val queries: DatabaseQueries,
    private val syncRepository: SyncRepository
) : MemberRepository {

    override fun getMembersByProject(projectId: String): Flow<List<ProjectMember>> =
        queries.getMembersByProject(project_id = projectId)
            .asFlow()
            .mapToList(Dispatchers.IO)

    override suspend fun addMember(projectId: String, email: String, role: String) {
        val id = "mem_${projectId}_${email.hashCode()}"
        val now = currentTimestamp()
        queries.upsertProjectMember(id = id, project_id = projectId, email = email, role = role, updatedAt = now)
        syncRepository.enqueue("INSERT", "ProjectMember", id, projectId, null)
    }

    override suspend fun addOwnerLocally(projectId: String, email: String) {
        val id = "mem_${projectId}_${email.hashCode()}"
        val now = currentTimestamp()
        queries.upsertProjectMember(id = id, project_id = projectId, email = email, role = "owner", updatedAt = now)
        // Мы НЕ ставим это в очередь синхронизации, так как триггер на бэкенде 
        // сам создаст запись owner при вставке проекта. 
        // Но локально нам это нужно для UI и логики.
    }

    override suspend fun removeMember(memberId: String) {
        val member = queries.getProjectMemberById(memberId).executeAsOneOrNull() ?: return
        queries.deleteProjectMember(memberId)
        syncRepository.enqueue("DELETE", "ProjectMember", memberId, member.project_id, null)
    }
}
