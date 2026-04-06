@file:OptIn(ExperimentalTime::class)

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
import org.mosyagin.project.Shift
import org.mosyagin.project.crypto.DataEncrypter
import org.mosyagin.project.generateUUID
import org.mosyagin.project.models.versioning.Prop
import org.mosyagin.project.models.versioning.PropStatus
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

data class PropWithScene(
    val id: String,
    val name: String,
    val anchor: String,
    val status: String,
    val category: String,
    val propType: String,
    val note: String?,
    val photoPath: String?,
    val isCrossCutting: Boolean,
    val quantity: Int,
    val actorId: String?,
    val seriesNumber: Long,
    val sceneNumber: String,
    val isOrphaned: Boolean = false,
    val groupId: String? = null,
    val allSceneNumbers: List<String> = emptyList(),
    val shiftNumber: Long? = null,
    val shiftDate: String? = null
)

interface SceneRepository {
    fun getSceneById(sceneId: String, scriptFileId: String): Flow<GetSceneById?>
    fun getSceneUserDataById(id: String): Flow<SceneUserData?>
    fun getSceneVersionsForUserData(sceneUserDataId: String): Flow<List<SceneVersion>>
    fun getScenesByProject(projectId: String, scriptFileId: String): Flow<List<GetScenesByProject>>
    fun getLatestScenesForProject(projectId: String): Flow<List<GetLatestScenesForProject>>
    fun getActorsForScene(sceneUserDataId: String): Flow<List<Actor>>
    fun getActorsByProject(projectId: String): Flow<List<Actor>>
    fun getScenesByActor(actorId: String, scriptFileId: String): Flow<List<GetScenesByActor>>
    fun getLocationsByActor(actorId: String): Flow<List<String>>
    fun getPropsForScene(sceneUserDataId: String): Flow<List<Prop>>
    fun getPropsByProject(projectId: String): Flow<List<PropWithScene>>
    fun getShiftsByProject(projectId: String): Flow<List<Shift>>
    
    suspend fun getSceneUserDataIdBySeriesAndNumber(projectId: String, series: Long, sceneNumber: String): String?
    
    suspend fun addPropFull(
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
    ): String

    suspend fun addProp(sceneUserDataId: String, name: String, anchor: String, status: String = "Найти", startOffset: Long = 0, endOffset: Long = 0): String
    suspend fun updatePropStatus(propId: String, newStatus: String)
    suspend fun deleteProp(propId: String)
    suspend fun updateSceneUserDataReviewStatus(needsReview: Long, id: String)
    suspend fun updatePropCategory(propId: String, category: String)
    suspend fun updatePropNote(propId: String, note: String?)
    suspend fun updatePropQuantity(propId: String, quantity: Int)
    suspend fun updatePropCrossCutting(propId: String, isCrossCutting: Boolean)
    suspend fun updatePropActor(propId: String, actorId: String?)
    suspend fun updatePropType(propId: String, propType: String)
    suspend fun updatePropGroupId(propId: String, groupId: String?)
}

class SceneRepositoryImpl(
    val queries: DatabaseQueries,
    private val syncRepository: SyncRepository,
    private val encrypter: DataEncrypter
) : SceneRepository {
    override fun getSceneById(sceneId: String, scriptFileId: String): Flow<GetSceneById?> =
        queries.getSceneById(sceneId, scriptFileId)
            .asFlow()
            .map { it.executeAsOneOrNull() }
            .map { it?.copy(content = encrypter.decrypt(it.content) ?: "", notes = encrypter.decrypt(it.notes)) }

    override fun getSceneUserDataById(id: String): Flow<SceneUserData?> =
        queries.getSceneUserDataById(id)
            .asFlow()
            .map { it.executeAsOneOrNull() }
            .map { it?.copy(notes = encrypter.decrypt(it.notes)) }

    override fun getSceneVersionsForUserData(sceneUserDataId: String): Flow<List<SceneVersion>> =
        queries.getSceneVersionsForUserData(sceneUserDataId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.map { it.copy(content = encrypter.decrypt(it.content) ?: "") } }

    override fun getScenesByProject(projectId: String, scriptFileId: String): Flow<List<GetScenesByProject>> =
        queries.getScenesByProject(projectId, scriptFileId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.map { it.copy(content = encrypter.decrypt(it.content) ?: "", notes = encrypter.decrypt(it.notes)) } }

    override fun getLatestScenesForProject(projectId: String): Flow<List<GetLatestScenesForProject>> =
        queries.getLatestScenesForProject(projectId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.map { it.copy(content = encrypter.decrypt(it.content) ?: "", notes = encrypter.decrypt(it.notes)) } }

    override fun getActorsForScene(sceneUserDataId: String): Flow<List<Actor>> =
        queries.getActorsForScene(sceneUserDataId)
            .asFlow()
            .mapToList(Dispatchers.IO)

    override fun getActorsByProject(projectId: String): Flow<List<Actor>> =
        queries.getActorsByProject(projectId)
            .asFlow()
            .mapToList(Dispatchers.IO)

    override fun getScenesByActor(actorId: String, scriptFileId: String): Flow<List<GetScenesByActor>> =
        queries.getScenesByActor(actorId, scriptFileId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.map { it.copy(content = encrypter.decrypt(it.content) ?: "", notes = encrypter.decrypt(it.notes)) } }

    override fun getLocationsByActor(actorId: String): Flow<List<String>> =
        queries.getLocationsByActor(actorId)
            .asFlow()
            .mapToList(Dispatchers.IO)

    override fun getPropsForScene(sceneUserDataId: String): Flow<List<Prop>> =
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
                        note = encrypter.decrypt(p.note),
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

    override fun getPropsByProject(projectId: String): Flow<List<PropWithScene>> =
        queries.getPropsByProject(projectId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list ->
                // Группируем по groupId, а если его нет (это родитель) - по его собственному id
                val groupScenes = list.groupBy { it.groupId ?: it.id }
                    .mapValues { entry -> 
                        entry.value.map { "${it.seriesNumber}-${it.sceneNumber}" }.distinct() 
                    }

                list.map { prop ->
                    val effectiveGroupId = prop.groupId ?: prop.id
                    val allScenes = groupScenes[effectiveGroupId] ?: emptyList()

                    PropWithScene(
                        id = prop.id,
                        name = prop.name,
                        anchor = prop.anchor,
                        status = prop.status,
                        category = prop.category,
                        propType = prop.propType,
                        note = encrypter.decrypt(prop.note),
                        photoPath = prop.photoPath,
                        isCrossCutting = prop.isCrossCutting == 1L,
                        quantity = prop.quantity.toInt(),
                        actorId = prop.actorId,
                        seriesNumber = prop.seriesNumber,
                        sceneNumber = prop.sceneNumber,
                        isOrphaned = prop.orphaned == 1L,
                        groupId = prop.groupId,
                        // Стек показываем только если сцен в группе больше одной
                        allSceneNumbers = if (allScenes.size > 1) allScenes else emptyList()
                    )
                }
            }

    override fun getShiftsByProject(projectId: String): Flow<List<Shift>> =
        queries.getShiftsByProject(projectId)
            .asFlow()
            .mapToList(Dispatchers.IO)

    override suspend fun getSceneUserDataIdBySeriesAndNumber(projectId: String, series: Long, sceneNumber: String): String? {
        return queries.getSceneUserDataBySeriesAndNumber(projectId, series, sceneNumber).executeAsOneOrNull()?.id
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
    ): String {
        val id = generateUUID()
        val now = Clock.System.now().toEpochMilliseconds()
        val encryptedNote = encrypter.encrypt(note)
        queries.insertFullProp(
            id, sceneUserDataId, name, anchor, status, category, "Обстановочный",
            encryptedNote, null, if (isCrossCutting) 1L else 0L, quantity.toLong(), actorId,
            0, 0, 0, groupId, now
        )
        syncRepository.enqueue("INSERT", "Prop", id, null)
        return id
    }

    override suspend fun addProp(sceneUserDataId: String, name: String, anchor: String, status: String, startOffset: Long, endOffset: Long): String {
        val id = generateUUID()
        val now = Clock.System.now().toEpochMilliseconds()
        queries.insertFullProp(
            id, sceneUserDataId, name, anchor, status, "Прочее", "Обстановочный",
            null, null, 0, 1, null, startOffset, endOffset, 0, null, now
        )
        syncRepository.enqueue("INSERT", "Prop", id, null)
        return id
    }

    override suspend fun updatePropStatus(propId: String, newStatus: String) {
        val now = Clock.System.now().toEpochMilliseconds()
        queries.updatePropStatus(newStatus, now, propId)
        syncRepository.enqueue("UPDATE", "Prop", propId, null)
    }

    override suspend fun deleteProp(propId: String) {
        queries.deleteProp(propId)
        syncRepository.enqueue("DELETE", "Prop", propId, null)
    }

    override suspend fun updateSceneUserDataReviewStatus(needsReview: Long, id: String) {
        val now = Clock.System.now().toEpochMilliseconds()
        queries.updateSceneUserDataReviewStatus(needsReview, now, id)
        syncRepository.enqueue("UPDATE", "SceneUserData", id, null)
    }

    override suspend fun updatePropCategory(propId: String, category: String) {
        val now = Clock.System.now().toEpochMilliseconds()
        queries.updatePropCategory(category, now, propId)
        syncRepository.enqueue("UPDATE", "Prop", propId, null)
    }

    override suspend fun updatePropNote(propId: String, note: String?) {
        val now = Clock.System.now().toEpochMilliseconds()
        val encryptedNote = encrypter.encrypt(note)
        queries.updatePropNote(encryptedNote, now, propId)
        syncRepository.enqueue("UPDATE", "Prop", propId, null)
    }

    override suspend fun updatePropQuantity(propId: String, quantity: Int) {
        val now = Clock.System.now().toEpochMilliseconds()
        queries.updatePropQuantity(quantity.toLong(), now, propId)
        syncRepository.enqueue("UPDATE", "Prop", propId, null)
    }

    override suspend fun updatePropCrossCutting(propId: String, isCrossCutting: Boolean) {
        val now = Clock.System.now().toEpochMilliseconds()
        queries.updatePropCrossCutting(if (isCrossCutting) 1L else 0L, now, propId)
        syncRepository.enqueue("UPDATE", "Prop", propId, null)
    }

    override suspend fun updatePropActor(propId: String, actorId: String?) {
        val now = Clock.System.now().toEpochMilliseconds()
        queries.updatePropActor(actorId, now, propId)
        syncRepository.enqueue("UPDATE", "Prop", propId, null)
    }

    override suspend fun updatePropType(propId: String, propType: String) {
        val now = Clock.System.now().toEpochMilliseconds()
        queries.updatePropType(propType, now, propId)
        syncRepository.enqueue("UPDATE", "Prop", propId, null)
    }

    override suspend fun updatePropGroupId(propId: String, groupId: String?) {
        val now = Clock.System.now().toEpochMilliseconds()
        queries.updatePropGroupId(groupId, now, propId)
        syncRepository.enqueue("UPDATE", "Prop", propId, null)
    }
}
