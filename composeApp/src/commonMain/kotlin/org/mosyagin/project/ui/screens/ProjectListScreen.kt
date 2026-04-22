@file:OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)

package org.mosyagin.project.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.jsonPrimitive
import org.mosyagin.project.ActivityLog
import org.mosyagin.project.Project
import org.mosyagin.project.db.ProjectListScreenModel
import org.mosyagin.project.db.ProjectStats
import org.mosyagin.project.ui.components.*

class ProjectListScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = koinScreenModel<ProjectListScreenModel>()
        
        val recentProjects by screenModel.recentProjects.collectAsState()
        val cloudProjects by screenModel.cloudProjects.collectAsState()
        val localProjects by screenModel.localProjects.collectAsState()
        
        val selectedProjectId by screenModel.selectedProjectId.collectAsState()
        val projectStatuses by screenModel.projectStatuses.collectAsState()
        val currentUser by screenModel.currentUser.collectAsState()
        val layoutType = LocalAppLayoutType.current
        
        val isDesktop = layoutType == AppLayoutType.DESKTOP

        var localProjectToConnect by remember { mutableStateOf<Project?>(null) }
        var authExpiredProject by remember { mutableStateOf<Project?>(null) }

        // --- ДИАЛОГИ ---
        localProjectToConnect?.let { project ->
            AlertDialog(
                onDismissRequest = { localProjectToConnect = null },
                title = { Text("Проект хранится только локально") },
                text = { Text("Подключите его к облаку, чтобы работать вместе с командой.") },
                confirmButton = {
                    Button(onClick = {
                        localProjectToConnect = null
                        if (currentUser == null) navigator.push(AuthScreen(onSkipAuth = { navigator.pop() }))
                        else screenModel.connectProjectToCloud(project.id)
                    }) { Text("Подключить") }
                },
                dismissButton = { TextButton(onClick = { localProjectToConnect = null }) { Text("Отмена") } }
            )
        }

        authExpiredProject?.let { _ ->
            AlertDialog(
                onDismissRequest = { authExpiredProject = null },
                title = { Text("Сессия истекла") },
                text = { Text("Войдите снова, чтобы продолжить синхронизацию.") },
                confirmButton = {
                    Button(onClick = {
                        authExpiredProject = null
                        navigator.push(AuthScreen(onSkipAuth = { navigator.pop() }))
                    }) { Text("Войти") }
                }
            )
        }

        Row(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            // ЛЕВАЯ ПАНЕЛЬ
            Column(
                modifier = Modifier
                    .weight(if (isDesktop) 0.38f else 1f)
                    .fillMaxHeight()
                    .padding(horizontal = 20.dp)
            ) {
                ProjectListHeader(screenModel)
                
                Spacer(Modifier.height(16.dp))
                
                Button(
                    onClick = { navigator.push(CreateProjectScreen()) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Создать проект", fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(24.dp))
                ProjectSearchField(screenModel)
                Spacer(Modifier.height(24.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    item {
                        GlobalProjectsCard(
                            isSelected = selectedProjectId == null && isDesktop,
                            onClick = { screenModel.selectProject(null) }
                        )
                        Spacer(Modifier.height(16.dp))
                    }

                    if (recentProjects.isNotEmpty()) {
                        item { SectionHeader("ПОСЛЕДНИЕ") }
                        items(recentProjects) { project ->
                            ProjectCardItem(project, selectedProjectId, projectStatuses, isDesktop, screenModel, navigator) {
                                localProjectToConnect = it
                            }
                        }
                        item { Spacer(Modifier.height(16.dp)) }
                    }

                    if (cloudProjects.isNotEmpty()) {
                        item { SectionHeader("ОБЛАКО") }
                        items(cloudProjects) { project ->
                            ProjectCardItem(project, selectedProjectId, projectStatuses, isDesktop, screenModel, navigator) {
                                authExpiredProject = it
                            }
                        }
                        item { Spacer(Modifier.height(16.dp)) }
                    }

                    if (localProjects.isNotEmpty()) {
                        item { SectionHeader("ЛОКАЛЬНЫЕ") }
                        items(localProjects) { project ->
                            ProjectCardItem(project, selectedProjectId, projectStatuses, isDesktop, screenModel, navigator) {
                                localProjectToConnect = it
                            }
                        }
                    }
                }
            }

            // ПРАВАЯ ПАНЕЛЬ
            if (isDesktop) {
                VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                Box(modifier = Modifier.weight(0.62f).fillMaxHeight()) {
                    val allProjectsList = (recentProjects + cloudProjects + localProjects).distinctBy { it.id }
                    val selectedProject = allProjectsList.find { it.id == selectedProjectId }
                    
                    val stats by screenModel.selectedProjectStats.collectAsState()
                    val activities by screenModel.recentActivities.collectAsState()

                    if (selectedProjectId == null) {
                        ProjectPreviewPane(
                            project = null,
                            stats = stats,
                            recentActivities = activities,
                            onOpenProject = { }
                        )
                    } else if (selectedProject != null) {
                        ProjectPreviewPane(
                            project = selectedProject,
                            stats = stats,
                            recentActivities = activities,
                            onOpenProject = { navigator.push(ProjectDashboardScreen(selectedProject.id)) }
                        )
                    } else {
                        EmptyPreviewPlaceholder()
                    }
                }
            }
        }
    }

    @Composable
    private fun GlobalProjectsCard(isSelected: Boolean, onClick: () -> Unit) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .combinedClickable(
                    onClick = onClick,
                    onDoubleClick = { /* Можно добавить открытие общего журнала */ }
                ),
            shape = RoundedCornerShape(16.dp),
            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) 
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
            border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) else null
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Public, null, modifier = Modifier.size(24.dp).alpha(0.6f))
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Все проекты", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Глобальная лента событий", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
        }
    }

    @Composable
    private fun SectionHeader(title: String) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }

    @Composable
    private fun ProjectCardItem(
        project: Project,
        selectedProjectId: String?,
        statuses: Map<String, ProjectSyncStatus>,
        isDesktop: Boolean,
        screenModel: ProjectListScreenModel,
        navigator: cafe.adriel.voyager.navigator.Navigator,
        onAlert: (Project) -> Unit
    ) {
        val status = statuses[project.id] ?: ProjectSyncStatus.LOCAL
        ModernProjectCard(
            project = project,
            status = status,
            isSelected = project.id == selectedProjectId && isDesktop,
            onClick = {
                screenModel.selectProject(project.id)
                if (!isDesktop) navigator.push(ProjectDashboardScreen(project.id))
            },
            onDoubleClick = {
                if (isDesktop) {
                    screenModel.selectProject(project.id)
                    navigator.push(ProjectDashboardScreen(project.id))
                }
            },
            onStatusClick = {
                if (status == ProjectSyncStatus.LOCAL || status == ProjectSyncStatus.REQUIRES_AUTH) {
                    onAlert(project)
                }
            }
        )
    }

    @Composable
    private fun ProjectListHeader(screenModel: ProjectListScreenModel) {
        val currentUser by screenModel.currentUser.collectAsState()
        val isOnline by screenModel.isOnline.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        var showUserMenu by remember { mutableStateOf(false) }

        Row(modifier = Modifier.fillMaxWidth().padding(top = 24.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Проекты", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (currentUser == null) {
                    Button(onClick = { navigator.push(AuthScreen(onSkipAuth = { navigator.pop() })) }, shape = RoundedCornerShape(12.dp)) {
                        Text("Войти")
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Filled.Login, null, modifier = Modifier.size(18.dp))
                    }
                } else {
                    Box {
                        UserAvatar(email = currentUser?.email, isOnline = isOnline, onClick = { showUserMenu = true })
                        DropdownMenu(expanded = showUserMenu, onDismissRequest = { showUserMenu = false }) {
                            val fullName = currentUser?.userMetadata?.get("full_name")?.jsonPrimitive?.content ?: "Пользователь"
                            DropdownMenuItem(text = { Column { Text(fullName, fontWeight = FontWeight.Bold); Text(currentUser?.email ?: "", style = MaterialTheme.typography.bodySmall, color = Color.Gray) } }, onClick = {}, enabled = false)
                            HorizontalDivider()
                            DropdownMenuItem(text = { Text("Выйти") }, onClick = { showUserMenu = false; screenModel.signOut() }, leadingIcon = { Icon(Icons.AutoMirrored.Filled.Logout, null) })
                            DropdownMenuItem(text = { Text("Настройки") }, onClick = { showUserMenu = false; navigator.push(SettingsScreen()) }, leadingIcon = { Icon(Icons.Default.Settings, null) })
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun ProjectSearchField(screenModel: ProjectListScreenModel) {
        val searchQuery by screenModel.searchQuery.collectAsState()
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { screenModel.onSearchQueryChange(it) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Поиск проектов...") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )
    }

    @Composable
    private fun ModernProjectCard(
        project: Project, 
        status: ProjectSyncStatus, 
        isSelected: Boolean, 
        onClick: () -> Unit,
        onDoubleClick: () -> Unit,
        onStatusClick: () -> Unit
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .combinedClickable(
                    onClick = onClick,
                    onDoubleClick = onDoubleClick
                ),
            shape = RoundedCornerShape(16.dp),
            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
            border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) else null
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Description, null, modifier = Modifier.size(24.dp).alpha(0.6f))
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(project.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Изменён недавно", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
                StatusBadgeCompact(status, onStatusClick)
            }
        }
    }

    @Composable
    private fun StatusBadgeCompact(status: ProjectSyncStatus, onClick: () -> Unit) {
        val (text, color, icon) = when (status) {
            ProjectSyncStatus.SYNCED -> Triple("синхр.", Color(0xFF2E7D32), Icons.Default.Done)
            ProjectSyncStatus.DIRTY -> Triple("изменён", Color(0xFFE67E22), Icons.Default.Sync)
            ProjectSyncStatus.LOCAL -> Triple("локальный", Color(0xFF7F8C8D), Icons.Default.CloudOff)
            ProjectSyncStatus.REQUIRES_AUTH -> Triple("войдите", Color(0xFFC0392B), Icons.Default.Lock)
            ProjectSyncStatus.ERROR -> Triple("ошибка", Color(0xFFD32F2F), Icons.Default.Error)
        }
        Surface(color = color.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp), modifier = Modifier.clickable { onClick() }) {
            Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(icon, null, modifier = Modifier.size(12.dp), tint = color)
                Text(text, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
            }
        }
    }

    @Composable
    private fun ProjectPreviewPane(project: Project?, stats: ProjectStats, recentActivities: List<Any>, onOpenProject: () -> Unit) {
        Column(modifier = Modifier.fillMaxSize().padding(32.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(project?.name ?: "ОБЗОР ВСЕХ ПРОЕКТОВ", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(project?.director ?: "Общая активность студии", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
                }
                if (project != null) {
                    Button(onClick = onOpenProject, shape = RoundedCornerShape(12.dp)) { Text("Открыть проект") }
                }
            }
            Spacer(Modifier.height(32.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatCard("${stats.changeCount}", "ИЗМЕНЕНИЙ", Modifier.weight(1f))
                if (project != null) {
                    StatCard("${stats.memberCount}", "УЧАСТНИКА", Modifier.weight(1f))
                }
                StatCard(stats.status, "СТАТУС", Modifier.weight(1f), true)
            }
            Spacer(Modifier.height(40.dp))
            Text("ПОСЛЕДНИЕ ДЕЙСТВИЯ", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
            Spacer(Modifier.height(16.dp))
            
            recentActivities.forEach { log ->
                if (log is ActivityLog) {
                    Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).background(Color(0xFF3498DB), CircleShape))
                        Spacer(Modifier.width(12.dp))
                        Text("${log.userName} ${log.encryptedDescription ?: ""}", style = MaterialTheme.typography.bodyMedium)
                    }
                } else if (log is org.mosyagin.project.GetAllActivities) {
                    Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).background(Color(0xFF3498DB), CircleShape))
                        Spacer(Modifier.width(12.dp))
                        Text("${log.userName} ${log.encryptedDescription ?: ""} [${log.projectName ?: ""}]", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }

    @Composable
    private fun StatCard(value: String, label: String, modifier: Modifier, isStatus: Boolean = false) {
        Surface(modifier, shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)) {
            Column(Modifier.padding(20.dp)) {
                Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = if (isStatus) Color(0xFF2E7D32) else Color.Unspecified)
                Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
        }
    }

    @Composable
    private fun EmptyPreviewPlaceholder() {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Выберите проект", color = Color.Gray) }
    }

    private fun formatRelativeTime(timestamp: Long): String {
        return "недавно"
    }
}
