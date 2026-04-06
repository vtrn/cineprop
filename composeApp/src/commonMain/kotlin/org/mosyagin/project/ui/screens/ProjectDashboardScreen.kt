package org.mosyagin.project.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.koin.compose.koinInject
import org.mosyagin.project.repository.ProjectRepository
import org.mosyagin.project.ui.components.AppLayoutType
import org.mosyagin.project.ui.components.CineCard
import org.mosyagin.project.ui.components.LocalAppLayoutType

data class ProjectDashboardScreen(val projectId: String) : Screen { // Изменено на String

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val repository = koinInject<ProjectRepository>()
        val project by repository.getProjectById(projectId).collectAsState(initial = null)
        val layoutType = LocalAppLayoutType.current

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            project?.let { currentProject ->
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header
                    item(span = { GridItemSpan(2) }) {
                        Column(modifier = Modifier.padding(top = 24.dp, bottom = 12.dp)) {
                            if (layoutType == AppLayoutType.MOBILE) {
                                IconButton(
                                    onClick = { navigator.pop() },
                                    modifier = Modifier.offset(x = (-12).dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                                }
                            }
                            
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

                    item(span = { GridItemSpan(2) }) {
                        CineCard(
                            onClick = { navigator.push(KppCalendarScreen(projectId)) },
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        modifier = Modifier.size(52.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        color = Color(0xFFB4E6B2).copy(alpha = 0.2f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarMonth,
                                            contentDescription = null,
                                            modifier = Modifier.padding(12.dp),
                                            tint = Color(0xFFB4E6B2)
                                        )
                                    }
                                    Spacer(Modifier.width(16.dp))
                                    Column {
                                        Text(
                                            text = "Календарь смен",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Нажмите, чтобы увидеть расписание КПП",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    item(span = { GridItemSpan(2) }) {
                        Text(
                            "Разделы",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                        )
                    }

                    item {
                        DashboardActionTile(
                            "Сценарий", 
                            Icons.Default.Description,
                            onClick = { 
                                if (layoutType == AppLayoutType.DESKTOP) {
                                    navigator.push(ScriptWorkspaceScreen(projectId))
                                } else {
                                    navigator.push(ScriptListScreen(projectId))
                                }
                            }
                        )
                    }
                    item {
                        DashboardActionTile(
                            "Сцены",
                            Icons.AutoMirrored.Filled.List,
                            onClick = { 
                                if (layoutType == AppLayoutType.DESKTOP) {
                                    navigator.push(SceneWorkspaceScreen(projectId))
                                } else {
                                    navigator.push(SceneListScreen(currentProject.id, currentProject.name))
                                }
                            }
                        )
                    }
                    item {
                        DashboardActionTile(
                            "КПП", 
                            Icons.Default.UploadFile,
                            onClick = { navigator.push(KppListScreen(projectId = currentProject.id)) }
                        )
                    }
                    item {
                        DashboardActionTile(
                            "Трекер", 
                            Icons.Default.AddAPhoto,
                            onClick = { navigator.push(TrackerScreen(projectId = currentProject.id)) }
                        )
                    }
                    item {
                        DashboardActionTile(
                            "Реквизит", 
                            Icons.Default.Inventory,
                            onClick = { 
                                if (layoutType == AppLayoutType.DESKTOP) {
                                    navigator.push(PropWorkspaceScreen(projectId))
                                } else {
                                    navigator.push(PropListScreen(projectId))
                                }
                            }
                        )
                    }
                    item {
                        DashboardActionTile(
                            "Библия", 
                            Icons.Default.AutoStories,
                            onClick = { navigator.push(CharacterBibleScreen(projectId)) }
                        )
                    }
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
                modifier = Modifier.fillMaxWidth().height(80.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
