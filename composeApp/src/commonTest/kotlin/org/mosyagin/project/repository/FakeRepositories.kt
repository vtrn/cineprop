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

class FakeProjectRepository : ProjectRepository {
    private val projects = MutableStateFlow<List<Project>>(emptyList())

    override fun getAllProjects(): Flow<List<Project>> = projects.asStateFlow()

    override fun getProjectById(id: Long): Flow<Project?> = flowOf(projects.value.find { it.id == id })

    override suspend fun addProject(name: String, director: String) {
        val newId = (projects.value.size + 1).toLong()
        projects.value += Project(newId, name, director)
    }

    override suspend fun deleteProject(id: Long) {
        projects.value = projects.value.filter { it.id != id }
    }
}

class FakeScriptRepository : ScriptRepository {
    private val scripts = MutableStateFlow<List<ScriptFile>>(emptyList())
    var saveParsedScriptCalled = false
    var lastSavedText = ""

    override fun getScriptsForProject(projectId: Long): Flow<List<ScriptFile>> = 
        flowOf(scripts.value.filter { it.projectId == projectId })

    override fun getScriptFileById(id: Long): Flow<ScriptFile?> {
        TODO("Not yet implemented")
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
            uploadedBy = null
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

class FakeSceneRepository : SceneRepository {
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

    override suspend fun getSceneUserDataIdBySeriesAndNumber(projectId: Long, series: Long, sceneNumber: String): Long? {
        return scenes.find { it.projectId == projectId && it.seriesNumber == series && it.sceneNumber == sceneNumber }?.id
    }

    override suspend fun addProp(sceneUserDataId: Long, name: String, anchor: String, status: String, startOffset: Long, endOffset: Long) {}
    override suspend fun updatePropStatus(propId: Long, newStatus: String) {}
    override suspend fun deleteProp(propId: Long) {}
    override suspend fun updateSceneUserDataReviewStatus(needsReview: Long, id: Long) {}
    override suspend fun confirmAllProps(sceneUserDataId: Long) {}
}

class FakeShiftRepository : ShiftRepository {
    private val shifts = mutableListOf<Shift>()
    private val links = mutableListOf<Triple<Long, Long, Long>>() // shiftId, sceneUserDataId, position

    override fun getShiftsByProject(projectId: Long): Flow<List<Shift>> = flowOf(shifts.filter { it.projectId == projectId })
    override fun getShiftById(shiftId: Long): Flow<Shift?> = flowOf(shifts.find { it.id == shiftId })
    override fun getScenesForShift(shiftId: Long): Flow<List<GetScenesForShift>> = flowOf(emptyList())

    override suspend fun addShift(projectId: Long, shiftNumber: Long, date: String): Long {
        val id = (shifts.size + 1).toLong()
        shifts.add(Shift(id, projectId, shiftNumber, date))
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
