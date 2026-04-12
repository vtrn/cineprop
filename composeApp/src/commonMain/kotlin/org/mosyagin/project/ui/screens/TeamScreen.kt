package org.mosyagin.project.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import org.koin.core.parameter.parametersOf
import org.mosyagin.project.ui.components.AppLayoutType
import org.mosyagin.project.ui.components.LocalAppLayoutType
import org.mosyagin.project.ui.components.ThreePaneLayout
import org.mosyagin.project.ui.components.props.PropMobileLayout
import org.mosyagin.project.ui.components.team.*

data class TeamScreen(val projectId: String) : Screen {

    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<TeamViewModel> { parametersOf(projectId) }
        val layoutType = LocalAppLayoutType.current
        
        val members by viewModel.filteredMembers.collectAsState()
        val roles by viewModel.roles.collectAsState()
        val selectedRole by viewModel.roleFilter.collectAsState()
        val selectedMemberId by viewModel.selectedMemberId.collectAsState()

        var isLeftPanelExpanded by remember { mutableStateOf(false) }
        val isRightPanelVisible = selectedMemberId != null
        
        // Состояние диалога
        var showAddMemberDialog by remember { mutableStateOf(false) }

        // ЛЕВАЯ ПАНЕЛЬ: Фильтр по ролям
        val masterPane: @Composable (Boolean) -> Unit = { isCollapsed ->
            TeamMasterPane(
                roles = roles,
                selectedRole = selectedRole,
                isCollapsed = isCollapsed,
                onRoleSelected = { 
                    viewModel.setRoleFilter(it)
                    if (layoutType == AppLayoutType.MOBILE) isLeftPanelExpanded = false
                },
                onToggleExpand = { isLeftPanelExpanded = !isLeftPanelExpanded }
            )
        }

        // ЦЕНТРАЛЬНАЯ ПАНЕЛЬ: Список участников
        val detailPane = @Composable {
            TeamDetailPane(
                members = members,
                selectedMemberId = selectedMemberId,
                onMemberClick = { viewModel.selectMember(it) },
                onAddMemberClick = { showAddMemberDialog = true } // Открываем диалог
            )
        }

        // ПРАВАЯ ПАНЕЛЬ: Детали участника
        val inspectorPane = @Composable {
            TeamInspectorPane(
                member = members.find { it.id == selectedMemberId },
                onRemoveMember = { viewModel.removeMember(it) },
                onClose = { viewModel.selectMember(null) }
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
                    onCloseRight = { viewModel.selectMember(null) }
                )
            }

            // Отображение диалога
            if (showAddMemberDialog) {
                AddMemberDialog(
                    onDismiss = { showAddMemberDialog = false },
                    onConfirm = { email, role ->
                        viewModel.addMember(email, role)
                        showAddMemberDialog = false
                    }
                )
            }
        }
    }
}
