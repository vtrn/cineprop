package org.mosyagin.project.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.serialization.json.jsonPrimitive
import org.mosyagin.project.Project
import org.mosyagin.project.db.ProjectListScreenModel
import org.mosyagin.project.ui.components.*

class ProjectListScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = koinScreenModel<ProjectListScreenModel>()
        val projects by screenModel.projects.collectAsState()
        val currentUser by screenModel.currentUser.collectAsState()
        val isOnline by screenModel.isOnline.collectAsState()
        val projectStatuses by screenModel.projectStatuses.collectAsState()
        
        var searchQuery by remember { mutableStateOf("") }
        var showUserMenu by remember { mutableStateOf(false) }
        
        // Состояния для попапов
        var localProjectToConnect by remember { mutableStateOf<Project?>(null) }
        var authExpiredProject by remember { mutableStateOf<Project?>(null) }

        // 1. Попап "Только локально"
        localProjectToConnect?.let { project ->
            AlertDialog(
                onDismissRequest = { localProjectToConnect = null },
                modifier = Modifier.widthIn(max = 400.dp),
                icon = { Icon(Icons.Default.DesktopWindows, contentDescription = null, modifier = Modifier.size(48.dp)) },
                title = { Text("Проект хранится только на этом устройстве", textAlign = TextAlign.Center) },
                text = { 
                    Text(
                        "Чтобы работать с командой и синхронизировать данные между устройствами — подключите проект к облаку. Для этого нужно войти или создать аккаунт.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium
                    ) 
                },
                confirmButton = {
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Button(
                            onClick = { 
                                val pid = project.id
                                localProjectToConnect = null
                                if (currentUser == null) {
                                    navigator.push(AuthScreen(onSkipAuth = { navigator.pop() })) 
                                } else {
                                    screenModel.connectProjectToCloud(pid)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (currentUser == null) "Войти и подключить к облаку" else "Подключить к облаку")
                        }
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = { localProjectToConnect = null }) {
                            Text("Оставить локально", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Данные останутся на устройстве до тех пор, пока вы не подключите облако",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            )
        }

        // 2. Попап "Сессия истекла"
        authExpiredProject?.let { _ ->
            AlertDialog(
                onDismissRequest = { authExpiredProject = null },
                modifier = Modifier.widthIn(max = 450.dp),
                icon = { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color(0xFFE67E22)) },
                title = { Text("Сессия истекла", textAlign = TextAlign.Center) },
                text = { 
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Проект загружен локально и доступен для работы. Чтобы синхронизировать изменения с командой — войдите снова.",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(16.dp))
                        Surface(
                            color = Color(0xFFFFF3E0),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFE67E22))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Ваши изменения сохранены локально и не потеряются. После входа они синхронизируются автоматически.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFE67E22)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Button(
                            onClick = { 
                                authExpiredProject = null
                                navigator.push(AuthScreen(onSkipAuth = { navigator.pop() })) 
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Войти снова")
                        }
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = { authExpiredProject = null }) {
                            Text("Продолжить оффлайн", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            )
        }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { navigator.push(CreateProjectScreen()) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                SyncBanner(
                    message = if (currentUser == null) "Войдите, чтобы синхронизировать проекты" else "Нет интернета — изменения сохранены локально",
                    isVisible = currentUser == null || !isOnline,
                    icon = if (isOnline) Icons.Default.Info else Icons.Default.CloudOff
                )

                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Spacer(Modifier.height(24.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Мои проекты",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp
                            )
                        )
                        
                        Box {
                            if (currentUser == null) {
                                Button(
                                    onClick = { navigator.push(AuthScreen(onSkipAuth = { navigator.pop() })) },
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Войти")
                                    Spacer(Modifier.width(8.dp))
                                    Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                            } else {
                                UserAvatar(
                                    email = currentUser?.email,
                                    isOnline = isOnline,
                                    onClick = { showUserMenu = true }
                                )
                                
                                DropdownMenu(
                                    expanded = showUserMenu,
                                    onDismissRequest = { showUserMenu = false },
                                    modifier = Modifier
                                        .width(280.dp)
                                        .background(MaterialTheme.colorScheme.surface)
                                        .padding(vertical = 8.dp),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    val initials = currentUser?.email?.take(1)?.uppercase() ?: "U"
                                    val fullName = currentUser?.userMetadata?.get("full_name")?.jsonPrimitive?.content
                                        ?: currentUser?.email?.split("@")?.firstOrNull()?.replaceFirstChar { it.uppercase() }
                                        ?: "Пользователь"

                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            modifier = Modifier.size(48.dp),
                                            shape = CircleShape,
                                            color = Color(0xFFE67E22)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(initials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                            }
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Column {
                                            Text(fullName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                            Text(currentUser?.email ?: "", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                    
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                    
                                    DropdownMenuItem(
                                        text = { Text("Sign Out") },
                                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(20.dp)) },
                                        onClick = {
                                            showUserMenu = false
                                            screenModel.signOut()
                                        }
                                    )
                                    
                                    DropdownMenuItem(
                                        text = { Text("Settings") },
                                        leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(20.dp)) },
                                        onClick = { 
                                            showUserMenu = false
                                            navigator.push(SettingsScreen())
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        placeholder = { 
                            Text("Поиск проектов...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) 
                        },
                        leadingIcon = { 
                            Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) 
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                        )
                    )

                    Spacer(Modifier.height(24.dp))

                    if (projects.isEmpty()) {
                        EmptyProjectsView(onCreateClick = { navigator.push(CreateProjectScreen()) })
                    } else {
                        Text(
                            text = "ПОСЛЕДНИЕ",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            items(projects.filter { it.name.contains(searchQuery, ignoreCase = true) }) { project ->
                                val status = projectStatuses[project.id] ?: ProjectSyncStatus.LOCAL
                                ModernProjectCard(
                                    project = project,
                                    status = status,
                                    onClick = { navigator.push(ProjectDashboardScreen(project.id)) },
                                    onStatusClick = {
                                        when (status) {
                                            ProjectSyncStatus.LOCAL -> localProjectToConnect = project
                                            ProjectSyncStatus.REQUIRES_AUTH -> authExpiredProject = project
                                            else -> {}
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun ModernProjectCard(
        project: Project,
        status: ProjectSyncStatus,
        onClick: () -> Unit,
        onStatusClick: () -> Unit
    ) {
        CineCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = onClick,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                ) {
                    Icon(
                        Icons.Default.BusinessCenter, 
                        contentDescription = null, 
                        modifier = Modifier.padding(10.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                Spacer(Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = project.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = project.director.ifEmpty { "Режиссер не указан" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                StatusBadge(status = status, onClick = onStatusClick)
            }
        }
    }

    @Composable
    private fun EmptyProjectsView(onCreateClick: () -> Unit) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.CloudQueue, 
                contentDescription = null, 
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            )
            Spacer(Modifier.height(16.dp))
            Text("Пока ничего нет", style = MaterialTheme.typography.titleMedium)
            CineButton(
                text = "Создать", 
                onClick = onCreateClick, 
                modifier = Modifier.padding(top = 16.dp).width(150.dp)
            )
        }
    }
}
