package org.mosyagin.project.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.mosyagin.project.Actor
import org.mosyagin.project.Prop
import org.mosyagin.project.Scene
import org.mosyagin.project.Shift

class FakeSceneRepository : SceneRepository {
    private val scenes = mutableListOf<Scene>()
    
    fun addFakeScene(scene: Scene) {
        scenes.add(scene)
    }

    override fun getSceneById(sceneId: Long): Flow<Scene?> = flowOf(scenes.find { it.id == sceneId })
    override fun getScenesByProject(projectId: Long): Flow<List<Scene>> = flowOf(scenes.filter { it.projectId == projectId })
    override fun getActorsForScene(sceneId: Long): Flow<List<Actor>> = flowOf(emptyList())
    override fun getActorsByProject(projectId: Long): Flow<List<Actor>> = flowOf(emptyList())
    override fun getScenesByActor(actorId: Long): Flow<List<Scene>> = flowOf(emptyList())
    override fun getLocationsByActor(actorId: Long): Flow<List<String>> = flowOf(emptyList())
    override fun getPropsForScene(sceneId: Long): Flow<List<Prop>> = flowOf(emptyList())
    override fun getPropsByProject(projectId: Long): Flow<List<PropWithScene>> = flowOf(emptyList())

    override suspend fun getSceneIdBySeriesAndNumber(projectId: Long, series: String, sceneNumber: String): Long? {
        return scenes.find { it.projectId == projectId && it.seriesNumber == series && it.sceneNumber == sceneNumber }?.id
    }

    override suspend fun addProp(sceneId: Long, name: String, status: String, startOffset: Long, endOffset: Long) {}
    override suspend fun updatePropStatus(propId: Long, newStatus: String) {}
    override suspend fun deleteProp(propId: Long) {}
}

class FakeShiftRepository : ShiftRepository {
    private val shifts = mutableListOf<Shift>()
    private val links = mutableListOf<Triple<Long, Long, Long>>() // shiftId, sceneId, position

    override fun getShiftsByProject(projectId: Long): Flow<List<Shift>> = flowOf(shifts.filter { it.projectId == projectId })
    override fun getShiftById(shiftId: Long): Flow<Shift?> = flowOf(shifts.find { it.id == shiftId })
    override fun getScenesForShift(shiftId: Long): Flow<List<Scene>> = flowOf(emptyList())

    override suspend fun addShift(projectId: Long, shiftNumber: Long, date: String): Long {
        val id = (shifts.size + 1).toLong()
        shifts.add(Shift(id, projectId, shiftNumber, date))
        return id
    }

    override suspend fun linkSceneToShift(shiftId: Long, sceneId: Long, position: Long) {
        links.add(Triple(shiftId, sceneId, position))
    }

    override suspend fun getShiftByNumber(projectId: Long, shiftNumber: Long): Shift? {
        return shifts.find { it.projectId == projectId && it.shiftNumber == shiftNumber }
    }
    
    fun getLinksCount(): Int = links.size
}
