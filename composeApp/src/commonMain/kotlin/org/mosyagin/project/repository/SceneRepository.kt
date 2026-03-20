package org.mosyagin.project.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.mosyagin.project.Actor
import org.mosyagin.project.DatabaseQueries
import org.mosyagin.project.Prop
import org.mosyagin.project.Scene

data class PropWithScene(
    val id: Long,
    val name: String,
    val status: String,
    val seriesNumber: String,
    val sceneNumber: String
)

interface SceneRepository {
    fun getSceneById(sceneId: Long): Flow<Scene?>
    fun getScenesByProject(projectId: Long): Flow<List<Scene>>
    fun getActorsForScene(sceneId: Long): Flow<List<Actor>>
    fun getActorsByProject(projectId: Long): Flow<List<Actor>>
    fun getScenesByActor(actorId: Long): Flow<List<Scene>>
    fun getLocationsByActor(actorId: Long): Flow<List<String>>
    fun getPropsForScene(sceneId: Long): Flow<List<Prop>>
    fun getPropsByProject(projectId: Long): Flow<List<PropWithScene>>
    suspend fun getSceneIdBySeriesAndNumber(projectId: Long, series: String, sceneNumber: String): Long?
    suspend fun addProp(sceneId: Long, name: String, status: String = "Найти", startOffset: Long = 0, endOffset: Long = 0)
    suspend fun updatePropStatus(propId: Long, newStatus: String)
    suspend fun deleteProp(propId: Long)
}

class SceneRepositoryImpl(private val queries: DatabaseQueries) : SceneRepository {
    override fun getSceneById(sceneId: Long): Flow<Scene?> =
        queries.getSceneById(sceneId)
            .asFlow()
            .map { it.executeAsOneOrNull() }

    override fun getScenesByProject(projectId: Long): Flow<List<Scene>> =
        queries.getScenesByProject(projectId)
            .asFlow()
            .mapToList(Dispatchers.Default)

    override fun getActorsForScene(sceneId: Long): Flow<List<Actor>> =
        queries.getActorsForScene(sceneId)
            .asFlow()
            .mapToList(Dispatchers.Default)

    override fun getActorsByProject(projectId: Long): Flow<List<Actor>> =
        queries.getActorsByProject(projectId)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list ->
                list.map { actor ->
                    Actor(actor.id, actor.projectId, actor.name)
                }
            }

    override fun getScenesByActor(actorId: Long): Flow<List<Scene>> =
        queries.getScenesByActor(actorId)
            .asFlow()
            .mapToList(Dispatchers.Default)

    override fun getLocationsByActor(actorId: Long): Flow<List<String>> =
        queries.getLocationsByActor(actorId)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list -> list.map { it } } // SqlDelight обычно возвращает String для одной колонки в SELECT DISTINCT

    override fun getPropsForScene(sceneId: Long): Flow<List<Prop>> =
        queries.getPropsForScene(sceneId)
            .asFlow()
            .mapToList(Dispatchers.Default)

    override fun getPropsByProject(projectId: Long): Flow<List<PropWithScene>> =
        queries.getPropsByProject(projectId)
            .asFlow()
            .mapToList(Dispatchers.Default)
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

    override suspend fun getSceneIdBySeriesAndNumber(projectId: Long, series: String, sceneNumber: String): Long? {
        val result = queries.getSceneIdBySeriesAndNumber(projectId, series, sceneNumber).executeAsOneOrNull()
        // В SqlDelight если SELECT id, то возвращается объект с полем id или просто Long
        // Если это объект, то result.id. Если Long, то result.
        return result
    }

    override suspend fun addProp(sceneId: Long, name: String, status: String, startOffset: Long, endOffset: Long) {
        queries.insertProp(
            sceneId = sceneId,
            name = name,
            status = status,
            startOffset = startOffset,
            endOffset = endOffset
        )
    }

    override suspend fun updatePropStatus(propId: Long, newStatus: String) {
        queries.updatePropStatus(newStatus, propId)
    }

    override suspend fun deleteProp(propId: Long) {
        queries.deleteProp(propId)
    }
}
