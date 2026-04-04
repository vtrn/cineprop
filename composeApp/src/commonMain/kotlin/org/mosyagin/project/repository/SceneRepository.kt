package org.mosyagin.project.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.mosyagin.project.Actor
import org.mosyagin.project.DatabaseQueries
import org.mosyagin.project.SceneUserData
import org.mosyagin.project.SceneVersion
import org.mosyagin.project.GetScenesByProject
import org.mosyagin.project.GetLatestScenesForProject
import org.mosyagin.project.GetSceneById
import org.mosyagin.project.GetScenesByActor
import org.mosyagin.project.Project
import org.mosyagin.project.models.versioning.Prop
import org.mosyagin.project.models.versioning.PropStatus

data class PropWithScene(
    val id: Long,
    val name: String,
    val anchor: String,
    val status: String,
    val category: String,
    val propType: String,
    val note: String?,
    val photoPath: String?,
    val isCrossCutting: Boolean,
    val quantity: Int,
    val actorId: Long?,
    val seriesNumber: Long,
    val sceneNumber: String,
    val isOrphaned: Boolean = false,
    val groupId: Long? = null,
    val allSceneNumbers: List<String> = emptyList() // ДОБАВЛЕНО: Список всех сцен в цепочке
)

interface SceneRepository {
    fun getSceneById(sceneId: Long, scriptFileId: Long): Flow<GetSceneById?>
    fun getSceneUserDataById(id: Long): Flow<SceneUserData?>
    fun getSceneVersionsForUserData(sceneUserDataId: Long): Flow<List<SceneVersion>>
    fun getScenesByProject(projectId: Long, scriptFileId: Long): Flow<List<GetScenesByProject>>
    fun getLatestScenesForProject(projectId: Long): Flow<List<GetLatestScenesForProject>>
    fun getActorsForScene(sceneUserDataId: Long): Flow<List<Actor>>
    fun getActorsByProject(projectId: Long): Flow<List<Actor>>
    fun getScenesByActor(actorId: Long, scriptFileId: Long): Flow<List<GetScenesByActor>>
    fun getLocationsByActor(actorId: Long): Flow<List<String>>
    fun getPropsForScene(sceneUserDataId: Long): Flow<List<Prop>>
    fun getPropsByProject(projectId: Long): Flow<List<PropWithScene>>
    suspend fun getSceneUserDataIdBySeriesAndNumber(projectId: Long, series: Long, sceneNumber: String): Long?
    
    suspend fun addPropFull(
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
    ): Long

    suspend fun addProp(sceneUserDataId: Long, name: String, anchor: String, status: String = "Найти", startOffset: Long = 0, endOffset: Long = 0): Long
    suspend fun updatePropStatus(propId: Long, newStatus: String)
    suspend fun updatePropOrphanedStatus(propId: Long, isOrphaned: Boolean)
    suspend fun deleteProp(propId: Long)
    suspend fun updateSceneUserDataReviewStatus(needsReview: Long, id: Long)
    suspend fun confirmAllProps(sceneUserDataId: Long)
    suspend fun markPropAsOrphaned(sceneUserDataId: Long, propName: String)
    
    suspend fun updatePropCategory(propId: Long, category: String)
    suspend fun updatePropNote(propId: Long, note: String?)
    suspend fun updatePropQuantity(propId: Long, quantity: Int)
    suspend fun updatePropCrossCutting(propId: Long, isCrossCutting: Boolean)
    suspend fun updatePropActor(propId: Long, actorId: Long?)
    suspend fun updatePropType(propId: Long, propType: String)
    suspend fun updatePropGroupId(propId: Long, groupId: Long?)
    suspend fun bulkUpdatePropStatus(propIds: List<Long>, status: String)
}

class SceneRepositoryImpl(val queries: DatabaseQueries) : SceneRepository,
    ProjectRepository {
    override fun getSceneById(sceneId: Long, scriptFileId: Long): Flow<GetSceneById?> =
        queries.getSceneById(sceneId, scriptFileId)
            .asFlow()
            .map { it.executeAsOneOrNull() }

    override fun getSceneUserDataById(id: Long): Flow<SceneUserData?> =
        queries.getSceneUserDataById(id)
            .asFlow()
            .map { it.executeAsOneOrNull() }

    override fun getSceneVersionsForUserData(sceneUserDataId: Long): Flow<List<SceneVersion>> =
        queries.getSceneVersionsForUserData(sceneUserDataId)
            .asFlow()
            .mapToList(Dispatchers.IO)

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
            .map { list -> 
                list.map { p ->
                    Prop(
                        id = p.id,
                        sceneUserDataId = p.sceneUserDataId,
                        name = p.name,
                        anchor = p.anchor,
                        status = PropStatus.fromString(p.status),
                        category = p.category,
                        propType = p.propType,
                        note = p.note,
                        photoPath = p.photoPath,
                        isCrossCutting = p.isCrossCutting == 1L,
                        quantity = p.quantity.toInt(),
                        actorId = p.actorId,
                        startOffset = p.startOffset,
                        endOffset = p.endOffset,
                        isOrphaned = p.orphaned == 1L,
                        groupId = p.groupId
                    )
                } 
            }

    override fun getPropsByProject(projectId: Long): Flow<List<PropWithScene>> =
        queries.getPropsByProject(projectId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list ->
                // Группируем сцены по groupId
                val groupScenes = list.filter { it.groupId != null }
                    .groupBy { it.groupId }
                    .mapValues { entry -> entry.value.map { "${it.seriesNumber}-${it.sceneNumber}" }.distinct() }

                list.map { prop ->
                    PropWithScene(
                        id = prop.id,
                        name = prop.name,
                        anchor = prop.anchor,
                        status = prop.status,
                        category = prop.category,
                        propType = prop.propType,
                        note = prop.note,
                        photoPath = prop.photoPath,
                        isCrossCutting = prop.isCrossCutting == 1L,
                        quantity = prop.quantity.toInt(),
                        actorId = prop.actorId,
                        seriesNumber = prop.seriesNumber,
                        sceneNumber = prop.sceneNumber,
                        isOrphaned = prop.orphaned == 1L,
                        groupId = prop.groupId,
                        allSceneNumbers = if (prop.groupId != null) groupScenes[prop.groupId] ?: emptyList() else emptyList()
                    )
                }
            }

    override suspend fun getSceneUserDataIdBySeriesAndNumber(projectId: Long, series: Long, sceneNumber: String): Long? {
        return queries.getSceneUserDataBySeriesAndNumber(projectId, series, sceneNumber).executeAsOneOrNull()?.id
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
    ): Long {
        queries.insertFullProp(
            sceneUserDataId = sceneUserDataId,
            name = name,
            anchor = anchor,
            status = status,
            category = category,
            propType = "Обстановочный",
            note = note,
            photoPath = null,
            isCrossCutting = if (isCrossCutting) 1L else 0L,
            quantity = quantity.toLong(),
            actorId = actorId,
            startOffset = 0,
            endOffset = 0,
            orphaned = 0,
            groupId = groupId
        )
        return queries.lastInsertRowId().executeAsOne()
    }

    override suspend fun addProp(sceneUserDataId: Long, name: String, anchor: String, status: String, startOffset: Long, endOffset: Long): Long {
        queries.insertFullProp(
            sceneUserDataId = sceneUserDataId,
            name = name,
            anchor = anchor,
            status = status,
            category = "Прочее",
            propType = "Обстановочный",
            note = null,
            photoPath = null,
            isCrossCutting = 0,
            quantity = 1,
            actorId = null,
            startOffset = startOffset,
            endOffset = endOffset,
            orphaned = 0,
            groupId = null
        )
        return queries.lastInsertRowId().executeAsOne()
    }

    override suspend fun updatePropStatus(propId: Long, newStatus: String) {
        queries.updatePropStatus(newStatus, propId)
    }

    override suspend fun updatePropOrphanedStatus(propId: Long, isOrphaned: Boolean) {
        queries.updatePropOrphanedStatus(if (isOrphaned) 1L else 0L, propId)
    }

    override suspend fun deleteProp(propId: Long) {
        queries.deleteProp(propId)
    }

    override suspend fun updateSceneUserDataReviewStatus(needsReview: Long, id: Long) {
        queries.updateSceneUserDataReviewStatus(needsReview, id)
    }

    override suspend fun confirmAllProps(sceneUserDataId: Long) {
        queries.transaction {
            val props = queries.getPropsForScene(sceneUserDataId).executeAsList()
            props.forEach { prop ->
                queries.updatePropOrphanedStatus(0L, prop.id)
            }
        }
    }

    override suspend fun markPropAsOrphaned(sceneUserDataId: Long, propName: String) {
        val props = queries.getPropsForScene(sceneUserDataId).executeAsList()
        props.find { it.name.equals(propName, ignoreCase = true) }?.let {
            queries.updatePropOrphanedStatus(1L, it.id)
        }
    }

    override suspend fun updatePropCategory(propId: Long, category: String) {
        queries.updatePropCategory(category, propId)
    }

    override suspend fun updatePropNote(propId: Long, note: String?) {
        queries.updatePropNote(note, propId)
    }

    override suspend fun updatePropQuantity(propId: Long, quantity: Int) {
        queries.updatePropQuantity(quantity.toLong(), propId)
    }

    override suspend fun updatePropCrossCutting(propId: Long, isCrossCutting: Boolean) {
        queries.updatePropCrossCutting(if (isCrossCutting) 1L else 0L, propId)
    }

    override suspend fun updatePropActor(propId: Long, actorId: Long?) {
        queries.updatePropActor(actorId, propId)
    }

    override suspend fun updatePropType(propId: Long, propType: String) {
        queries.updatePropType(propType, propId)
    }

    override suspend fun updatePropGroupId(propId: Long, groupId: Long?) {
        queries.updatePropGroupId(groupId, propId)
    }

    override suspend fun bulkUpdatePropStatus(propIds: List<Long>, status: String) {
        queries.transaction {
            propIds.forEach { id ->
                queries.updatePropStatus(status, id)
            }
        }
    }

    override fun getAllProjects(): Flow<List<Project>> =
        queries.getAllProjects()
            .asFlow()
            .mapToList(Dispatchers.IO)

    override fun getProjectById(id: Long): Flow<Project?> =
        queries.getProjectById(id)
            .asFlow()
            .map { it.executeAsOneOrNull() }

    override suspend fun addProject(name: String, director: String) {
        queries.insertProject(name, director)
    }

    override suspend fun deleteProject(id: Long) {
        queries.deleteProject(id)
    }
}
