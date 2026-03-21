package org.mosyagin.project.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.mosyagin.project.db.ProjectListScreenModel
import org.mosyagin.project.ui.components.CineButton
import org.mosyagin.project.ui.components.CineCard

class ProjectListScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = getScreenModel<ProjectListScreenModel>()
        val projects by screenModel.projects.collectAsState()
        var searchQuery by remember { mutableStateOf("") }

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
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(Modifier.height(24.dp))
                
                // Заголовок в стиле макета
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Привет!",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Мои проекты",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp
                            )
                        )
                    }
                    // Имитация аватарки
                    Surface(
                        modifier = Modifier.size(44.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Icon(
                            Icons.Default.Person, 
                            contentDescription = null, 
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Поисковая строка как на макете
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
                    trailingIcon = {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        text = "Последние",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(projects.filter { it.name.contains(searchQuery, ignoreCase = true) }) { project ->
                            ModernProjectCard(
                                project = org.mosyagin.project.Project(project.id, project.name, project.director),
                                onClick = { navigator.push(ProjectDashboardScreen(project.id)) }
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun ModernProjectCard(project: org.mosyagin.project.Project, onClick: () -> Unit) {
        CineCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = onClick,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ) {
                        Icon(
                            Icons.Default.Movie, 
                            contentDescription = null, 
                            modifier = Modifier.padding(8.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
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
                    Spacer(Modifier.weight(1f))
                    // Имитация "Match Score" или статуса
                    Text(
                        text = "Active",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFB4E6B2), // Тот самый зеленый
                        modifier = Modifier
                            .background(Color(0xFFB4E6B2).copy(alpha = 0.1f), CircleShape)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
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
