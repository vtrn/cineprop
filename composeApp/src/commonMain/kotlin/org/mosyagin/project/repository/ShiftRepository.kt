package org.mosyagin.project.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.mosyagin.project.DatabaseQueries
import org.mosyagin.project.Scene
import org.mosyagin.project.Shift

interface ShiftRepository {
    fun getShiftsByProject(projectId: Long): Flow<List<Shift>>
    fun getShiftById(shiftId: Long): Flow<Shift?>
    fun getScenesForShift(shiftId: Long): Flow<List<Scene>>
    suspend fun addShift(projectId: Long, shiftNumber: Long, date: String): Long
    suspend fun linkSceneToShift(shiftId: Long, sceneId: Long, position: Long)
    suspend fun getShiftByNumber(projectId: Long, shiftNumber: Long): Shift?
}

class ShiftRepositoryImpl(private val queries: DatabaseQueries) : ShiftRepository {
    override fun getShiftsByProject(projectId: Long): Flow<List<Shift>> =
        queries.getShiftsByProject(projectId)
            .asFlow()
            .mapToList(Dispatchers.Default)

    override fun getShiftById(shiftId: Long): Flow<Shift?> =
        queries.getShiftById(shiftId)
            .asFlow()
            .map { it.executeAsOneOrNull() }

    override fun getScenesForShift(shiftId: Long): Flow<List<Scene>> =
        queries.getScenesForShift(shiftId)
            .asFlow()
            .mapToList(Dispatchers.Default)

    override suspend fun addShift(projectId: Long, shiftNumber: Long, date: String): Long {
        queries.insertShift(projectId, shiftNumber, date)
        return queries.lastInsertRowId().executeAsOne()
    }

    override suspend fun linkSceneToShift(shiftId: Long, sceneId: Long, position: Long) {
        queries.linkShiftToScene(shiftId, sceneId, position)
    }

    override suspend fun getShiftByNumber(projectId: Long, shiftNumber: Long): Shift? =
        queries.getShiftByNumber(projectId, shiftNumber).executeAsOneOrNull()
}
