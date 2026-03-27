package org.mosyagin.project.ui.screens

import cafe.adriel.voyager.core.model.ScreenModel
import kotlinx.coroutines.flow.StateFlow
import org.mosyagin.project.repository.ScriptRepository
import org.mosyagin.project.parser.update.UpdateResult

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect class ScriptViewModel(repository: ScriptRepository) : ScreenModel {
    val repository: ScriptRepository
    val isLoading: StateFlow<Boolean>
    val updateResult: StateFlow<UpdateResult?>

    suspend fun processPdfUri(projectId: Long, seriesNumber: Int, uriString: String)
    suspend fun processPdfUri(projectId: Long, uriString: String)
    suspend fun commitUpdate()
    
    // CRUD методы
    fun deleteScriptFile(fileId: Long)
    fun updateScriptTitle(fileId: Long, newTitle: String)
    
    fun clearUpdateResult()
}
