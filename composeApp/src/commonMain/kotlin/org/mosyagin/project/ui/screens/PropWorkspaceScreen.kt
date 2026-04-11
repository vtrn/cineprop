@file:OptIn(ExperimentalCoroutinesApi::class)

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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.koin.core.parameter.parametersOf
import org.mosyagin.project.ui.components.AppLayoutType
import org.mosyagin.project.ui.components.LocalAppLayoutType
import org.mosyagin.project.ui.components.ThreePaneLayout
import org.mosyagin.project.ui.components.props.*

/**
 * Основной экран рабочего пространства реквизита.
 * Адаптивен:
 * - Desktop: Стандартный ThreePaneLayout
 * - Mobile: Специальный PropMobileLayout с системой наслоения
 */
data class PropWorkspaceScreen(val projectId: String) : Screen {

    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<PropWorkspaceViewModel> { parametersOf(projectId) }
        val navigator = LocalNavigator.currentOrThrow
        val layoutType = LocalAppLayoutType.current
        
        val categories by viewModel.categories.collectAsState()
        val filteredProps by viewModel.filteredProps.collectAsState()
        val projectActors by viewModel.projectActors.collectAsState()
        val selectedPropId by viewModel.selectedPropId.collectAsState()
        val selectedPropIds by viewModel.selectedPropIds.collectAsState()
        val selectedCategoryFilter by viewModel.selectedCategoryFilter.collectAsState()
        val sortColumn by viewModel.sortColumn.collectAsState()
        val isSortAscending by viewModel.isSortAscending.collectAsState()
        val searchQuery by viewModel.searchQuery.collectAsState()
        val isKppMode by viewModel.isKppMode.collectAsState()
        val propsByShift by viewModel.propsByShift.collectAsState()

        var showExportDialog by remember { mutableStateOf(false) }
        
        // Состояния для мобильной навигации
        var isLeftPanelExpanded by remember { mutableStateOf(false) }
        val isRightPanelVisible = selectedPropId != null

        // Левая панель
        val masterPane: @Composable (Boolean) -> Unit = { isCollapsed ->
            PropMasterPane(
                categories = categories,
                selectedCategoryFilter = selectedCategoryFilter,
                isCollapsed = isCollapsed,
                onCategoryFilterSelected = { 
                    viewModel.onCategoryFilterSelected(it)
                    if (layoutType == AppLayoutType.MOBILE) isLeftPanelExpanded = false
                },
                onToggleExpand = { isLeftPanelExpanded = !isLeftPanelExpanded },
                onExportClick = { showExportDialog = true }
            )
        }

        val detailPane = @Composable {
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
                onToggleKppMode = { viewModel.toggleKppMode() },
                onMenuClick = null // Теперь используем свайпы
            )
        }

        val inspectorPane = @Composable {
            PropInspectorPane(
                propId = selectedPropId,
                props = filteredProps,
                actors = projectActors,
                onNoteChange = { id, note -> viewModel.updatePropNote(id, note) },
                onConfirm = { id -> /* viewModel.confirmProps(listOf(id)) */ },
                onDelete = { id -> 
                    viewModel.deleteProp(id)
                    if (layoutType == AppLayoutType.MOBILE) viewModel.onPropSelected(null)
                },
                onActorClick = { actorId ->
                    navigator.push(CharacterWorkspaceScreen(projectId)) 
                },
                onClose = if (layoutType == AppLayoutType.MOBILE) {
                    { viewModel.onPropSelected(null) }
                } else null
            )
        }

        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            if (layoutType == AppLayoutType.DESKTOP) {
                ThreePaneLayout(
                    masterPane = { masterPane(false) },
                    detailPane = { detailPane() },
                    inspectorPane = { inspectorPane() }
                )
            } else {
                PropMobileLayout(
                    leftPane = masterPane,
                    centerPane = detailPane,
                    rightPane = inspectorPane,
                    isLeftExpanded = isLeftPanelExpanded,
                    onToggleLeft = { isLeftPanelExpanded = it },
                    isRightVisible = isRightPanelVisible,
                    onCloseRight = { viewModel.onPropSelected(null) }
                )
            }

            if (showExportDialog) {
                PropExportDialog(
                    onDismiss = { showExportDialog = false },
                    onExport = { format, grouping ->
                        viewModel.performExport(format, grouping)
                        showExportDialog = false
                    }
                )
            }
        }
    }
}
