package org.mosyagin.project.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.mosyagin.project.Actor
import org.mosyagin.project.models.versioning.Prop
import org.mosyagin.project.Shift
import org.mosyagin.project.Project
import org.mosyagin.project.ScriptFile
import org.mosyagin.project.GetScenesByProject
import org.mosyagin.project.GetLatestScenesForProject
import org.mosyagin.project.GetSceneById
import org.mosyagin.project.GetScenesByActor
import org.mosyagin.project.GetScenesForShift
import org.mosyagin.project.SceneUserData
import org.mosyagin.project.SceneVersion
import org.mosyagin.project.SyncQueue
import org.mosyagin.project.ProjectMember
import org.mosyagin.project.generateUUID
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.coroutines.flow.StateFlow

class FakeSyncRepository : SyncRepository {
    private val queue = MutableStateFlow<List<SyncQueue>>(emptyList())
    
    override fun setSyncManager(manager: SyncManager) {
        // No-op for tests
    }

    override suspend fun enqueue(operation: String, tableName: String, recordId: String, projectId: String?, dataJson: String?) {
        enqueueSync(operation, tableName, recordId, projectId, dataJson)
    }

    override fun enqueueSync(operation: String, tableName: String, recordId: String, projectId: String?, dataJson: String?) {
        val newQueue = queue.value.toMutableList()
        newQueue.add(
            SyncQueue(
                id = (newQueue.size + 1).toLong(),
                operation = operation,
                tableName = tableName,
                recordId = recordId,
                project_id = projectId,
                dataJson = dataJson,
                updatedAt = 0L,
                synced = 0L
            )
        )
        queue.value = newQueue
    }

    override fun triggerPush() {
        // No-op for tests
    }

    override fun getPending(): Flow<List<SyncQueue>> = queue.asStateFlow()
    override suspend fun markSynced(ids: List<Long>) {
        queue.value = queue.value.map { 
            if (ids.contains(it.id)) it.copy(synced = 1L) else it 
        }
    }
    
    fun getSyncCount() = queue.value.size
}

class FakeAuthRepository : AuthRepository {
    private val _currentUser = MutableStateFlow<UserInfo?>(null)
    override val currentUser: StateFlow<UserInfo?> = _currentUser.asStateFlow()
    
    override suspend fun sendMagicLink(email: String) {}
    override suspend fun signInWithPassword(email: String, password: String) {}
    override suspend fun signUpWithPassword(email: String, password: String) {}
    override suspend fun signInWithGoogle() {}
    override suspend fun signInWithApple() {}
    override suspend fun resetPassword(email: String) {}
    override suspend fun signOut() {
        _currentUser.value = null
    }
    override fun getCurrentUserSync(): UserInfo? = _currentUser.value
    
    fun setUser(user: UserInfo?) {
        _currentUser.value = user
    }
}

class FakeProjectRepository : ProjectRepository {
    private val projects = MutableStateFlow<List<Project>>(emptyList())

    override fun getAllProjects(): Flow<List<Project>> = projects.asStateFlow()

    override fun getProjectById(id: String): Flow<Project?> = flowOf(projects.value.find { it.id == id })

    override suspend fun addProject(name: String, director: String) {
        val newId = generateUUID()
        projects.value += Project(newId, name, director, 0L, isRemote = 0L, created_by = "test@example.com")
    }

    override suspend fun deleteProject(id: String) {
        projects.value = projects.value.filter { it.id != id }
    }

    override suspend fun updateProject(id: String, name: String, director: String) {
        projects.value = projects.value.map {
            if (it.id == id) it.copy(name = name, director = director) else it
        }
    }

    override suspend fun markProjectAsRemote(id: String) {
        projects.value = projects.value.map {
            if (it.id == id) it.copy(isRemote = 1L) else it
        }
    }
}

class FakeScriptRepository : ScriptRepository {
    private val scripts = MutableStateFlow<List<ScriptFile>>(emptyList())
    var saveParsedScriptCalled = false
    var lastSavedText = ""

    override fun getScriptsForProject(projectId: String): Flow<List<ScriptFile>> = 
        flowOf(scripts.value.filter { it.project_id == projectId })

    override fun getScriptFileById(id: String): Flow<ScriptFile?> {
        return flowOf(scripts.value.find { it.id == id })
    }

    override suspend fun saveParsedScript(
        projectId: String,
        seriesNumber: Int,
        filePath: String,
        fullText: String,
        createdAt: Long
    ) {
        saveParsedScriptCalled = true
        lastSavedText = fullText
        val newId = generateUUID()
        scripts.value += ScriptFile(
            id = newId,
            project_id = projectId,
            seriesNumber = seriesNumber.toLong(),
            title = "Серия $seriesNumber",
            filePath = filePath,
            createdAt = createdAt,
            previousVersionId = null,
            revisionColor = "White",
            uploadedBy = null,
            updatedAt = 0L
        )
    }

    override suspend fun deleteScriptFile(fileId: String) {
        scripts.value = scripts.value.filter { it.id != fileId }
    }

    override suspend fun updateScriptTitle(fileId: String, newTitle: String) {
        scripts.value = scripts.value.map { 
            if (it.id == fileId) it.copy(title = newTitle) else it 
        }
    }
}

class FakeSceneRepository : SceneRepository, ProjectRepository {
    private val projectsRepo = FakeProjectRepository()
    private val scenes = mutableListOf<GetScenesByProject>()
    
    fun addFakeScene(scene: GetScenesByProject) {
        scenes.add(scene)
    }

    override fun getSceneById(sceneId: String, scriptFileId: String): Flow<GetSceneById?> = 
       flowOf(null)

    override fun getSceneUserDataById(id: String): Flow<SceneUserData?> = flowOf(null)

    override fun getSceneVersionsForUserData(sceneUserDataId: String): Flow<List<SceneVersion>> = flowOf(emptyList())

    override fun getScenesByProject(projectId: String, scriptFileId: String): Flow<List<GetScenesByProject>> = 
        flowOf(scenes.filter { it.project_id == projectId })

    override fun getLatestScenesForProject(projectId: String): Flow<List<GetLatestScenesForProject>> =
        flowOf(emptyList())

    override fun getActorsForScene(sceneUserDataId: String): Flow<List<Actor>> = flowOf(emptyList())
    override fun getActorsByProject(projectId: String): Flow<List<Actor>> = flowOf(emptyList())
    override fun getScenesByActor(actorId: String, scriptFileId: String): Flow<List<GetScenesByActor>> = flowOf(emptyList())
    override fun getLocationsByActor(actorId: String): Flow<List<String>> = flowOf(emptyList())
    override fun getPropsForScene(sceneUserDataId: String): Flow<List<Prop>> = flowOf(emptyList())
    override fun getPropsByProject(projectId: String): Flow<List<PropWithScene>> = flowOf(emptyList())
    override fun getShiftsByProject(projectId: String): Flow<List<Shift>> = flowOf(emptyList())

    override suspend fun getSceneUserDataIdBySeriesAndNumber(projectId: String, series: Long, sceneNumber: String): String? {
        return scenes.find { it.project_id == projectId && it.seriesNumber == series && it.sceneNumber == sceneNumber }?.id
    }

    override suspend fun addPropFull(
        sceneUserDataId: String,
        name: String,
        anchor: String,
        status: String,
        category: String,
        quantity: Int,
        actorId: String?,
        note: String?,
        isCrossCutting: Boolean,
        groupId: String?
    ): String = ""

    override suspend fun addProp(sceneUserDataId: String, name: String, anchor: String, status: String, startOffset: Long, endOffset: Long): String = ""
    override suspend fun updatePropStatus(propId: String, newStatus: String) {}

    override suspend fun deleteProp(propId: String) {}
    override suspend fun updateSceneUserDataReviewStatus(needsReview: Long, id: String) {}
    
    override suspend fun updatePropCategory(propId: String, category: String) {}
    override suspend fun updatePropNote(propId: String, note: String?) {}
    override suspend fun updatePropQuantity(propId: String, quantity: Int) {}
    override suspend fun updatePropCrossCutting(propId: String, isCrossCutting: Boolean) {}
    override suspend fun updatePropActor(propId: String, actorId: String?) {}
    override suspend fun updatePropType(propId: String, propType: String) {}
    override suspend fun updatePropGroupId(propId: String, groupId: String?) {}

    // Реализация ProjectRepository (делегирование)
    override fun getAllProjects(): Flow<List<Project>> = projectsRepo.getAllProjects()
    override fun getProjectById(id: String): Flow<Project?> = projectsRepo.getProjectById(id)
    override suspend fun addProject(name: String, director: String) = projectsRepo.addProject(name, director)
    override suspend fun deleteProject(id: String) = projectsRepo.deleteProject(id)
    override suspend fun updateProject(id: String, name: String, director: String) = projectsRepo.updateProject(id, name, director)
    override suspend fun markProjectAsRemote(id: String) = projectsRepo.markProjectAsRemote(id)
}

class FakeShiftRepository : ShiftRepository {
    private val shifts = mutableListOf<Shift>()
    private val links = mutableListOf<Triple<String, String, Long>>() // shiftId, sceneUserDataId, position

    override fun getShiftsByProject(projectId: String): Flow<List<Shift>> = flowOf(shifts.filter { it.project_id == projectId })
    override fun getShiftById(shiftId: String): Flow<Shift?> = flowOf(shifts.find { it.id == shiftId })
    override fun getScenesForShift(shiftId: String): Flow<List<GetScenesForShift>> = flowOf(emptyList())

    override suspend fun addShift(projectId: String, shiftNumber: Long, date: String): String {
        val id = generateUUID()
        shifts.add(Shift(id, projectId, shiftNumber, date, 0L))
        return id
    }

    override suspend fun linkSceneToShift(shiftId: String, sceneUserDataId: String, position: Long) {
        links.add(Triple(shiftId, sceneUserDataId, position))
    }

    override suspend fun getShiftByNumber(projectId: String, shiftNumber: Long): Shift? {
        return shifts.find { it.project_id == projectId && it.shiftNumber == shiftNumber }
    }
    
    fun getLinksCount(): Int = links.size
}

class FakeMemberRepository : MemberRepository {
    private val members = MutableStateFlow<List<ProjectMember>>(emptyList())

    override fun getMembersByProject(projectId: String): Flow<List<ProjectMember>> =
        members.map { list -> list.filter { it.project_id == projectId } }

    override suspend fun addMember(projectId: String, email: String, role: String) {
        val id = "mem_${projectId}_${email.hashCode()}"
        members.value += ProjectMember(id, projectId, email, role, 0L)
    }

    override suspend fun removeMember(memberId: String) {
        members.value = members.value.filter { it.id != memberId }
    }

    override suspend fun addOwnerLocally(projectId: String, email: String) {
        val id = "mem_${projectId}_${email.hashCode()}"
        members.value += ProjectMember(id, projectId, email, "owner", 0L)
    }
}
