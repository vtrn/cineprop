package org.mosyagin.project

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import org.koin.compose.KoinContext
import org.mosyagin.project.ui.components.AdaptiveScaffold
import org.mosyagin.project.ui.components.AppLayoutType
import org.mosyagin.project.ui.screens.*
import org.mosyagin.project.ui.theme.CinePropTheme

@Composable
fun App() {
    CinePropTheme {
        KoinContext {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                var isInProject by remember { mutableStateOf(false) }
                var currentSection by remember { mutableStateOf("projects") }
                var activeProjectId by remember { mutableStateOf<Long?>(null) }
                var lastLayoutType by remember { mutableStateOf(AppLayoutType.MOBILE) }

                Navigator(ProjectListScreen()) { navigator ->
                    // Синхронизация состояния Сайдбара с текущим экраном
                    LaunchedEffect(navigator.lastItem) {
                        val item = navigator.lastItem
                        
                        when (item) {
                            is ProjectDashboardScreen -> {
                                isInProject = true
                                activeProjectId = item.projectId
                                currentSection = "dashboard"
                            }
                            is ScriptListScreen -> {
                                isInProject = true
                                activeProjectId = item.projectId
                                currentSection = "script"
                            }
                            is SceneListScreen, is SceneWorkspaceScreen -> {
                                isInProject = true
                                activeProjectId = if (item is SceneListScreen) item.projectId else (item as SceneWorkspaceScreen).projectId
                                currentSection = "scenes"
                            }
                            is KppListScreen -> {
                                isInProject = true
                                activeProjectId = item.projectId
                                currentSection = "schedule"
                            }
                            is TrackerScreen -> {
                                isInProject = true
                                activeProjectId = item.projectId
                                currentSection = "tracker"
                            }
                            is PropListScreen, is PropWorkspaceScreen -> {
                                isInProject = true
                                activeProjectId = if (item is PropListScreen) item.projectId else (item as PropWorkspaceScreen).projectId
                                currentSection = "inventory"
                            }
                            is CharacterBibleScreen -> {
                                isInProject = true
                                activeProjectId = item.projectId
                                currentSection = "bible"
                            }
                            is ProjectListScreen -> {
                                isInProject = false
                                activeProjectId = null
                                currentSection = "projects"
                            }
                        }
                    }

                    AdaptiveScaffold(
                        isInProject = isInProject,
                        currentSection = currentSection,
                        onBackToProjects = {
                            navigator.popUntilRoot()
                        },
                        onSectionSelect = { section ->
                            val id = activeProjectId ?: return@AdaptiveScaffold
                            
                            // Предотвращаем повторное открытие того же экрана
                            if (currentSection == section) return@AdaptiveScaffold

                            when (section) {
                                "dashboard" -> navigator.replace(ProjectDashboardScreen(id))
                                "script" -> navigator.replace(ScriptListScreen(id))
                                "scenes" -> {
                                    if (lastLayoutType == AppLayoutType.DESKTOP) {
                                        navigator.replace(SceneWorkspaceScreen(id))
                                    } else {
                                        navigator.replace(SceneListScreen(id, "Проект"))
                                    }
                                }
                                "schedule" -> navigator.replace(KppListScreen(id))
                                "tracker" -> navigator.replace(TrackerScreen(id))
                                "inventory" -> {
                                    if (lastLayoutType == AppLayoutType.DESKTOP) {
                                        navigator.replace(PropWorkspaceScreen(id))
                                    } else {
                                        navigator.replace(PropListScreen(id))
                                    }
                                }
                                "bible" -> navigator.replace(CharacterBibleScreen(id))
                            }
                        }
                    ) { layoutType ->
                        lastLayoutType = layoutType
                        SlideTransition(navigator)
                    }
                }
            }
        }
    }
}
