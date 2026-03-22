package org.mosyagin.project.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.mosyagin.project.Actor
import org.mosyagin.project.DatabaseQueries
import org.mosyagin.project.Prop
import org.mosyagin.project.GetScenesByProject
import org.mosyagin.project.GetLatestScenesForProject
import org.mosyagin.project.GetScenesBySeries
import org.mosyagin.project.GetSceneById
import org.mosyagin.project.GetScenesByActor
import org.mosyagin.project.GetScenesForShift

data class PropWithScene(
    val id: Long,
    val name: String,
    val status: String,
    val seriesNumber: String,
    val sceneNumber: String
)

interface SceneRepository {
    fun getSceneById(sceneId: Long, scriptFileId: Long): Flow<GetSceneById?>
    fun getScenesByProject(projectId: Long, scriptFileId: Long): Flow<List<GetScenesByProject>>
    fun getLatestScenesForProject(projectId: Long): Flow<List<GetLatestScenesForProject>>
    fun getActorsForScene(sceneUserDataId: Long): Flow<List<Actor>>
    fun getActorsByProject(projectId: Long): Flow<List<Actor>>
    fun getScenesByActor(actorId: Long, scriptFileId: Long): Flow<List<GetScenesByActor>>
    fun getLocationsByActor(actorId: Long): Flow<List<String>>
    fun getPropsForScene(sceneUserDataId: Long): Flow<List<Prop>>
    fun getPropsByProject(projectId: Long): Flow<List<PropWithScene>>
    suspend fun getSceneUserDataIdBySeriesAndNumber(projectId: Long, series: String, sceneNumber: String): Long?
    suspend fun addProp(sceneUserDataId: Long, name: String, status: String = "Найти", startOffset: Long = 0, endOffset: Long = 0)
    suspend fun updatePropStatus(propId: Long, newStatus: String)
    suspend fun deleteProp(propId: Long)
}

class SceneRepositoryImpl(private val queries: DatabaseQueries) : SceneRepository {
    override fun getSceneById(sceneId: Long, scriptFileId: Long): Flow<GetSceneById?> =
        queries.getSceneById(sceneId, scriptFileId)
            .asFlow()
            .map { it.executeAsOneOrNull() }

    override fun getScenesByProject(projectId: Long, scriptFileId: Long): Flow<List<GetScenesByProject>> =
        queries.getScenesByProject(projectId, scriptFileId)
            .asFlow()
            .mapToList(Dispatchers.IO)

    override fun getLatestScenesForProject(projectId: Long): Flow<List<GetLatestScenesForProject>> =
        queries.getLatestScenesForProject(projectId)
            .asFlow()
            .mapToList(Dispatchers.IO)

    override fun getActorsForScene(sceneUserDataId: Long): Flow<List<Actor>> =
        queries.getActorsForScene(sceneUserDataId)
            .asFlow()
            .mapToList(Dispatchers.IO)

    override fun getActorsByProject(projectId: Long): Flow<List<Actor>> =
        queries.getActorsByProject(projectId)
            .asFlow()
            .mapToList(Dispatchers.IO)

    override fun getScenesByActor(actorId: Long, scriptFileId: Long): Flow<List<GetScenesByActor>> =
        queries.getScenesByActor(actorId, scriptFileId)
            .asFlow()
            .mapToList(Dispatchers.IO)

    override fun getLocationsByActor(actorId: Long): Flow<List<String>> =
        queries.getLocationsByActor(actorId)
            .asFlow()
            .mapToList(Dispatchers.IO)

    override fun getPropsForScene(sceneUserDataId: Long): Flow<List<Prop>> =
        queries.getPropsForScene(sceneUserDataId)
            .asFlow()
            .mapToList(Dispatchers.IO)

    override fun getPropsByProject(projectId: Long): Flow<List<PropWithScene>> =
        queries.getPropsByProject(projectId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list ->
                list.map { prop ->
                    PropWithScene(
                        id = prop.id,
                        name = prop.name,
                        status = prop.status,
                        seriesNumber = prop.seriesNumber,
                        sceneNumber = prop.sceneNumber
                    )
                }
            }

    override suspend fun getSceneUserDataIdBySeriesAndNumber(projectId: Long, series: String, sceneNumber: String): Long? {
        return queries.getSceneUserDataBySeriesAndNumber(projectId, series, sceneNumber).executeAsOneOrNull()?.id
    }

    override suspend fun addProp(sceneUserDataId: Long, name: String, status: String, startOffset: Long, endOffset: Long) {
        queries.insertProp(
            sceneUserDataId = sceneUserDataId,
            name = name,
            status = status,
            startOffset = startOffset,
            endOffset = endOffset,
            orphaned = 0
        )
    }

    override suspend fun updatePropStatus(propId: Long, newStatus: String) {
        queries.updatePropStatus(newStatus, propId)
    }

    override suspend fun deleteProp(propId: Long) {
        queries.deleteProp(propId)
    }
}
