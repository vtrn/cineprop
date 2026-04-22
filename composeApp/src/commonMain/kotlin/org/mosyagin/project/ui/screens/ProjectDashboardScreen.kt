@file:OptIn(ExperimentalMaterial3Api::class)

package org.mosyagin.project.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import org.mosyagin.project.ActivityLog
import org.mosyagin.project.repository.ProjectRepository
import org.mosyagin.project.ui.components.AppLayoutType
import org.mosyagin.project.ui.components.CineCard
import org.mosyagin.project.ui.components.LocalAppLayoutType

data class ProjectDashboardScreen(val projectId: String) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val repository = koinInject<ProjectRepository>()
        val project by repository.getProjectById(projectId).collectAsState(initial = null)
        val layoutType = LocalAppLayoutType.current
        
        // Получаем активность через ViewModel для мини-ленты
        val activityViewModel = koinScreenModel<ActivityViewModel> { parametersOf(projectId) }
        val recentActivities by activityViewModel.activities.collectAsState()

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                if (layoutType == AppLayoutType.MOBILE) {
                    TopAppBar(
                        title = { Text(project?.name ?: "Проект", style = MaterialTheme.typography.titleMedium) },
                        navigationIcon = {
                            IconButton(onClick = { navigator.pop() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                            }
                        }
                    )
                }
            }
        ) { paddingValues ->
            project?.let { currentProject ->
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header (только для Desktop, на Mobile он в TopAppBar)
                    if (layoutType == AppLayoutType.DESKTOP) {
                        item(span = { GridItemSpan(2) }) {
                            Column(modifier = Modifier.padding(top = 24.dp, bottom = 12.dp)) {
                                Text(
                                    text = currentProject.name,
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = (-0.5).sp
                                    )
                                )
                                Text(
                                    text = "Панель управления проектом",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Календарь
                    item(span = { GridItemSpan(2) }) {
                        CineCard(
                            onClick = { navigator.push(KppCalendarScreen(projectId)) },
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        modifier = Modifier.size(48.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        color = Color(0xFFB4E6B2).copy(alpha = 0.2f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarMonth,
                                            contentDescription = null,
                                            modifier = Modifier.padding(12.dp),
                                            tint = Color(0xFF2E7D32)
                                        )
                                    }
                                    Spacer(Modifier.width(16.dp))
                                    Column {
                                        Text(
                                            text = "Календарь смен",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Расписание КПП",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    item(span = { GridItemSpan(2) }) {
                        Text("РАЗДЕЛЫ", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.outline)
                    }

                    // Плитки разделов
                    item { DashboardActionTile("Сценарий", Icons.Default.Description) { navigator.push(ScriptWorkspaceScreen(projectId)) } }
                    item { DashboardActionTile("Сцены", Icons.AutoMirrored.Filled.List) { navigator.push(SceneWorkspaceScreen(projectId)) } }
                    item { DashboardActionTile("КПП", Icons.Default.Event) { navigator.push(KppListScreen(projectId)) } }
                    item { DashboardActionTile("Трекер", Icons.Default.AddAPhoto) { navigator.push(TrackerScreen(projectId)) } }
                    item { DashboardActionTile("Реквизит", Icons.Default.Inventory2) { navigator.push(PropWorkspaceScreen(projectId)) } }
                    item { DashboardActionTile("Библия", Icons.Default.AutoStories) { navigator.push(CharacterWorkspaceScreen(projectId)) } }
                    item { DashboardActionTile("Команда", Icons.Default.Group) { navigator.push(TeamScreen(projectId)) } }
                    item { DashboardActionTile("Журнал", Icons.Default.History) { navigator.push(ActivityScreen(projectId)) } }

                    // МИНИ-ЛЕНТА ЖУРНАЛА
                    item(span = { GridItemSpan(2) }) {
                        Column(modifier = Modifier.padding(top = 16.dp, bottom = 32.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("ПОСЛЕДНИЕ ИЗМЕНЕНИЯ", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.outline)
                                TextButton(onClick = { navigator.push(ActivityScreen(projectId)) }) {
                                    Text("Все", style = MaterialTheme.typography.labelLarge)
                                }
                            }
                            
                            Spacer(Modifier.height(8.dp))
                            
                            CineCard(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    if (recentActivities.isEmpty()) {
                                        Text("Действий пока нет", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    } else {
                                        recentActivities.take(3).forEach { log ->
                                            MiniLogItem(log)
                                            if (log != recentActivities.take(3).last()) {
                                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun MiniLogItem(log: ActivityLog) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(6.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = "${log.userName} ${log.encryptedDescription ?: ""}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                if (!log.encryptedEntityName.isNullOrBlank()) {
                    Text(log.encryptedEntityName!!, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }

    @Composable
    private fun DashboardActionTile(title: String, icon: ImageVector, onClick: () -> Unit) {
        CineCard(
            onClick = onClick,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().height(70.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
                Spacer(Modifier.height(4.dp))
                Text(text = title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
