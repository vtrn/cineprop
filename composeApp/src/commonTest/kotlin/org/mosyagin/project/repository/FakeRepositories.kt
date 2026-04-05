package org.mosyagin.project.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
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

class FakeSyncRepository : SyncRepository {
    private val queue = MutableStateFlow<List<SyncQueue>>(emptyList())
    
    override fun setSyncManager(manager: SyncManager) {
        // No-op для тестов
    }

    override suspend fun enqueue(operation: String, tableName: String, recordId: Long, dataJson: String?) {
        enqueueSync(operation, tableName, recordId, dataJson)
    }

    override fun enqueueSync(operation: String, tableName: String, recordId: Long, dataJson: String?) {
        val newQueue = queue.value.toMutableList()
        newQueue.add(
            SyncQueue(
                id = (newQueue.size + 1).toLong(),
                operation = operation,
                tableName = tableName,
                recordId = recordId,
                dataJson = dataJson,
                updatedAt = 0L,
                synced = 0L
            )
        )
        queue.value = newQueue
    }

    override fun getPending(): Flow<List<SyncQueue>> = queue.asStateFlow()
    override suspend fun markSynced(ids: List<Long>) {
        queue.value = queue.value.map { 
            if (ids.contains(it.id)) it.copy(synced = 1L) else it 
        }
    }
    
    fun getSyncCount() = queue.value.size
}

class FakeProjectRepository : ProjectRepository {
    private val projects = MutableStateFlow<List<Project>>(emptyList())

    override fun getAllProjects(): Flow<List<Project>> = projects.asStateFlow()

    override fun getProjectById(id: Long): Flow<Project?> = flowOf(projects.value.find { it.id == id })

    override suspend fun addProject(name: String, director: String) {
        val newId = (projects.value.size + 1).toLong()
        projects.value += Project(newId, name, director, 0L)
    }

    override suspend fun deleteProject(id: Long) {
        projects.value = projects.value.filter { it.id != id }
    }

    override suspend fun updateProject(id: Long, name: String, director: String) {
        projects.value = projects.value.map {
            if (it.id == id) it.copy(name = name, director = director) else it
        }
    }
}

class FakeScriptRepository : ScriptRepository {
    private val scripts = MutableStateFlow<List<ScriptFile>>(emptyList())
    var saveParsedScriptCalled = false
    var lastSavedText = ""

    override fun getScriptsForProject(projectId: Long): Flow<List<ScriptFile>> = 
        flowOf(scripts.value.filter { it.projectId == projectId })

    override fun getScriptFileById(id: Long): Flow<ScriptFile?> {
        return flowOf(scripts.value.find { it.id == id })
    }

    override suspend fun saveParsedScript(
        projectId: Long,
        seriesNumber: Int,
        filePath: String,
        fullText: String,
        createdAt: Long
    ) {
        saveParsedScriptCalled = true
        lastSavedText = fullText
        val newId = (scripts.value.size + 1).toLong()
        scripts.value += ScriptFile(
            id = newId,
            projectId = projectId,
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

    override suspend fun deleteScriptFile(fileId: Long) {
        scripts.value = scripts.value.filter { it.id != fileId }
    }

    override suspend fun updateScriptTitle(fileId: Long, newTitle: String) {
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

    override fun getSceneById(sceneId: Long, scriptFileId: Long): Flow<GetSceneById?> = 
       flowOf(null)

    override fun getSceneUserDataById(id: Long): Flow<SceneUserData?> = flowOf(null)

    override fun getSceneVersionsForUserData(sceneUserDataId: Long): Flow<List<SceneVersion>> = flowOf(emptyList())

    override fun getScenesByProject(projectId: Long, scriptFileId: Long): Flow<List<GetScenesByProject>> = 
        flowOf(scenes.filter { it.projectId == projectId })

    override fun getLatestScenesForProject(projectId: Long): Flow<List<GetLatestScenesForProject>> =
        flowOf(emptyList())

    override fun getActorsForScene(sceneUserDataId: Long): Flow<List<Actor>> = flowOf(emptyList())
    override fun getActorsByProject(projectId: Long): Flow<List<Actor>> = flowOf(emptyList())
    override fun getScenesByActor(actorId: Long, scriptFileId: Long): Flow<List<GetScenesByActor>> = flowOf(emptyList())
    override fun getLocationsByActor(actorId: Long): Flow<List<String>> = flowOf(emptyList())
    override fun getPropsForScene(sceneUserDataId: Long): Flow<List<Prop>> = flowOf(emptyList())
    override fun getPropsByProject(projectId: Long): Flow<List<PropWithScene>> = flowOf(emptyList())
    override fun getShiftsByProject(projectId: Long): Flow<List<Shift>> = flowOf(emptyList())
    override fun getPropsWithShiftByProject(projectId: Long): Flow<List<PropWithScene>> = flowOf(emptyList())

    override suspend fun getSceneUserDataIdBySeriesAndNumber(projectId: Long, series: Long, sceneNumber: String): Long? {
        return scenes.find { it.projectId == projectId && it.seriesNumber == series && it.sceneNumber == sceneNumber }?.id
    }

    override suspend fun addPropFull(
        sceneUserDataId: Long,
        name: String,
        anchor: String,
        status: String,
        category: String,
        quantity: Int,
        actorId: Long?,
        note: String?,
        isCrossCutting: Boolean,
        groupId: Long?
    ): Long = 0

    override suspend fun addProp(sceneUserDataId: Long, name: String, anchor: String, status: String, startOffset: Long, endOffset: Long): Long = 0
    override suspend fun updatePropStatus(propId: Long, newStatus: String) {}
    override suspend fun updatePropOrphanedStatus(propId: Long, isOrphaned: Boolean) {}

    override suspend fun deleteProp(propId: Long) {}
    override suspend fun updateSceneUserDataReviewStatus(needsReview: Long, id: Long) {}
    override suspend fun confirmAllProps(sceneUserDataId: Long) {}
    override suspend fun markPropAsOrphaned(sceneUserDataId: Long, propName: String) {}
    
    override suspend fun updatePropCategory(propId: Long, category: String) {}
    override suspend fun updatePropNote(propId: Long, note: String?) {}
    override suspend fun updatePropQuantity(propId: Long, quantity: Int) {}
    override suspend fun updatePropCrossCutting(propId: Long, isCrossCutting: Boolean) {}
    override suspend fun updatePropActor(propId: Long, actorId: Long?) {}
    override suspend fun updatePropType(propId: Long, propType: String) {}
    override suspend fun updatePropGroupId(propId: Long, groupId: Long?) {}
    override suspend fun bulkUpdatePropStatus(propIds: List<Long>, status: String) {}

    // Делегируем методы ProjectRepository
    override fun getAllProjects(): Flow<List<Project>> = projectsRepo.getAllProjects()
    override fun getProjectById(id: Long): Flow<Project?> = projectsRepo.getProjectById(id)
    override suspend fun addProject(name: String, director: String) = projectsRepo.addProject(name, director)
    override suspend fun deleteProject(id: Long) = projectsRepo.deleteProject(id)
    override suspend fun updateProject(id: Long, name: String, director: String) = projectsRepo.updateProject(id, name, director)
}

class FakeShiftRepository : ShiftRepository {
    private val shifts = mutableListOf<Shift>()
    private val links = mutableListOf<Triple<Long, Long, Long>>() // shiftId, sceneUserDataId, position

    override fun getShiftsByProject(projectId: Long): Flow<List<Shift>> = flowOf(shifts.filter { it.projectId == projectId })
    override fun getShiftById(shiftId: Long): Flow<Shift?> = flowOf(shifts.find { it.id == shiftId })
    override fun getScenesForShift(shiftId: Long): Flow<List<GetScenesForShift>> = flowOf(emptyList())

    override suspend fun addShift(projectId: Long, shiftNumber: Long, date: String): Long {
        val id = (shifts.size + 1).toLong()
        shifts.add(Shift(id, projectId, shiftNumber, date, 0L))
        return id
    }

    override suspend fun linkSceneToShift(shiftId: Long, sceneUserDataId: Long, position: Long) {
        links.add(Triple(shiftId, sceneUserDataId, position))
    }

    override suspend fun getShiftByNumber(projectId: Long, shiftNumber: Long): Shift? {
        return shifts.find { it.projectId == projectId && it.shiftNumber == shiftNumber }
    }
    
    fun getLinksCount(): Int = links.size
}
