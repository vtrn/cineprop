package org.mosyagin.project.ui.screens

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.mosyagin.project.Actor
import org.mosyagin.project.DatabaseQueries
import org.mosyagin.project.Prop
import org.mosyagin.project.Scene

class SceneDetailScreenModel(
    private val queries: DatabaseQueries,
    private val sceneId: Long
) : ScreenModel {

    // Достаем саму сцену
    val scene: StateFlow<Scene?> = flow {
        emit(queries.getSceneById(sceneId).executeAsOneOrNull())
    }.stateIn(screenModelScope, SharingStarted.Eagerly, null)

    // Достаем актеров этой сцены
    val actors: StateFlow<List<Actor>> = queries.getActorsForScene(sceneId)
        .asFlow()
        .mapToList(kotlinx.coroutines.Dispatchers.Default)
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- НОВОЕ: Поток реквизита для этой сцены ---
    val props: StateFlow<List<Prop>> = queries.getPropsForScene(sceneId)
        .asFlow()
        .mapToList(kotlinx.coroutines.Dispatchers.Default)
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Метод добавления реквизита
    fun addProp(name: String) {
        screenModelScope.launch {
            queries.insertProp(sceneId = sceneId, name = name, status = "Найти")
        }
    }

    // Метод удаления реквизита
    fun deleteProp(propId: Long) {
        screenModelScope.launch {
            queries.deleteProp(propId)
        }
    }
}