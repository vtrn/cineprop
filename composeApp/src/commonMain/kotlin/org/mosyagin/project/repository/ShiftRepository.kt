package org.mosyagin.project.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.mosyagin.project.DatabaseQueries
import org.mosyagin.project.GetScenesForShift
import org.mosyagin.project.Shift
import org.mosyagin.project.generateUUID
import kotlin.time.Clock

interface ShiftRepository {
    fun getShiftsByProject(projectId: String): Flow<List<Shift>>
    fun getShiftById(shiftId: String): Flow<Shift?>
    fun getScenesForShift(shiftId: String): Flow<List<GetScenesForShift>>
    suspend fun addShift(projectId: String, shiftNumber: Long, date: String): String
    suspend fun linkSceneToShift(shiftId: String, sceneUserDataId: String, position: Long)
    suspend fun getShiftByNumber(projectId: String, shiftNumber: Long): Shift?
}

class ShiftRepositoryImpl(
    private val queries: DatabaseQueries,
    private val syncRepository: SyncRepository
) : ShiftRepository {
    override fun getShiftsByProject(projectId: String): Flow<List<Shift>> =
        queries.getShiftsByProject(projectId)
            .asFlow()
            .mapToList(Dispatchers.Default)

    override fun getShiftById(shiftId: String): Flow<Shift?> =
        queries.getShiftById(shiftId)
            .asFlow()
            .map { it.executeAsOneOrNull() }

    override fun getScenesForShift(shiftId: String): Flow<List<GetScenesForShift>> =
        queries.getScenesForShift(shiftId)
            .asFlow()
            .mapToList(Dispatchers.Default)

    @OptIn(kotlin.time.ExperimentalTime::class)
    override suspend fun addShift(projectId: String, shiftNumber: Long, date: String): String {
        val id = "shift_${projectId}_$shiftNumber"
        val now = Clock.System.now().toEpochMilliseconds()
        
        val existing = queries.getShiftById(id).executeAsOneOrNull()
        if (existing == null) {
            queries.insertShift(id, projectId, shiftNumber, date, now)
            syncRepository.enqueue("INSERT", "Shift", id, null)
        }
        return id
    }

    override suspend fun linkSceneToShift(shiftId: String, sceneUserDataId: String, position: Long) {
        queries.linkShiftToScene(shiftId, sceneUserDataId, position)
        // Синхронизируем связь. Используем составной ID для очереди
        syncRepository.enqueue("INSERT", "ShiftScene", "${shiftId}|${sceneUserDataId}", null)
    }

    override suspend fun getShiftByNumber(projectId: String, shiftNumber: Long): Shift? =
        queries.getShiftByNumber(projectId, shiftNumber).executeAsOneOrNull()
}
