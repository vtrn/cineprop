package org.mosyagin.project.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.koin.core.parameter.parametersOf
import org.mosyagin.project.ui.components.ThreePaneLayout
import org.mosyagin.project.ui.components.props.*

/**
 * Основной экран рабочего пространства реквизита.
 * Использует трехпанельную верстку: 
 * 1. Категории (Master)
 * 2. Список объектов с поиском и группировкой (Detail)
 * 3. Детальная информация и заметки (Inspector)
 */
data class PropWorkspaceScreen(val projectId: Long) : Screen {

    @Composable
    override fun Content() {
        // Инициализация ViewModel через Koin с передачей ID проекта
        val viewModel = koinScreenModel<PropWorkspaceViewModel> { parametersOf(projectId) }
        val navigator = LocalNavigator.currentOrThrow
        
        // Подписка на состояния из ViewModel для реактивного обновления UI
        val propsByCategory by viewModel.propsByCategory.collectAsState()
        val filteredProps by viewModel.filteredProps.collectAsState()
        val projectActors by viewModel.projectActors.collectAsState()
        val selectedPropId by viewModel.selectedPropId.collectAsState()
        val selectedPropIds by viewModel.selectedPropIds.collectAsState()
        val selectedCategoryFilter by viewModel.selectedCategoryFilter.collectAsState()
        val sortColumn by viewModel.sortColumn.collectAsState()
        val isSortAscending by viewModel.isSortAscending.collectAsState()
        val searchQuery by viewModel.searchQuery.collectAsState()
        
        // Состояния для режима КПП
        val isKppMode by viewModel.isKppMode.collectAsState()
        val propsByShift by viewModel.propsByShift.collectAsState()

        // Состояние диалога экспорта
        var showExportDialog by remember { mutableStateOf(false) }

        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            // Использование стандартного трехпанельного макета CineApp
            ThreePaneLayout(
                // Левая панель: Выбор категории реквизита
                masterPane = {
                    PropMasterPane(
                        propsByCategory = propsByCategory,
                        selectedCategoryFilter = selectedCategoryFilter,
                        onCategoryFilterSelected = { viewModel.onCategoryFilterSelected(it) },
                        onExportClick = { showExportDialog = true }
                    )
                },
                // Центральная панель: Интерактивный список объектов
                detailPane = {
                    PropDetailPane(
                        props = filteredProps,
                        selectedPropIds = selectedPropIds,
                        selectedPropId = selectedPropId,
                        viewModel = viewModel,
                        sortColumn = sortColumn,
                        isSortAscending = isSortAscending,
                        searchQuery = searchQuery,
                        isKppMode = isKppMode,
                        propsByShift = propsByShift,
                        onToggleKppMode = { viewModel.toggleKppMode() }
                    )
                },
                // Правая панель: Инспектор выбранного объекта
                inspectorPane = {
                    PropInspectorPane(
                        propId = selectedPropId,
                        props = filteredProps,
                        actors = projectActors,
                        onNoteChange = { id, note -> viewModel.updatePropNote(id, note) },
                        onConfirm = { id -> viewModel.confirmProps(listOf(id)) },
                        onDelete = { id -> viewModel.deleteProp(id) },
                        onActorClick = { actorId ->
                            // Навигация к персонажу в "Библии персонажей"
                            navigator.push(CharacterWorkspaceScreen(projectId)) 
                        }
                    )
                }
            )

            // Диалоговое окно экспорта
            if (showExportDialog) {
                PropExportDialog(
                    onDismiss = { showExportDialog = false },
                    onExport = { grouping, format ->
                        // ИСПРАВЛЕНО: Теперь реально вызываем экспорт
                        viewModel.performExport(grouping, format)
                        showExportDialog = false
                    }
                )
            }
        }
    }
}
