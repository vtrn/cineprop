package org.mosyagin.project

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import org.koin.compose.KoinContext
import org.koin.compose.koinInject
import org.mosyagin.project.repository.AuthRepository
import org.mosyagin.project.repository.SettingsRepository
import org.mosyagin.project.ui.components.AdaptiveScaffold
import org.mosyagin.project.ui.components.AppLayoutType
import org.mosyagin.project.ui.screens.*
import org.mosyagin.project.ui.theme.CinePropTheme

@Composable
fun App() {
    KoinContext {
        val settingsRepository: SettingsRepository = koinInject()
        val authRepository: AuthRepository = koinInject()
        
        val themeMode by settingsRepository.getThemeMode().collectAsState("system")
        val currentUser by authRepository.currentUser.collectAsState(null)
        
        // Состояние для пропуска авторизации (локальный режим)
        var skipAuth by remember { mutableStateOf(false) }
        
        val isDarkTheme = when (themeMode) {
            "light" -> false
            "dark" -> true
            else -> isSystemInDarkTheme()
        }

        CinePropTheme(darkTheme = isDarkTheme) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                // Если пользователь не залогинен и не нажал "работать локально"
                if (currentUser == null && !skipAuth) {
                    Navigator(AuthScreen(onSkipAuth = { skipAuth = true })) { navigator ->
                        SlideTransition(navigator)
                    }
                } else {
                    var isInProject by remember { mutableStateOf(false) }
                    var currentSection by remember { mutableStateOf("projects") }
                    var activeProjectId by remember { mutableStateOf<String?>(null) }
                    var lastLayoutType by remember { mutableStateOf(AppLayoutType.MOBILE) }

                    Navigator(ProjectListScreen()) { navigator ->
                        LaunchedEffect(navigator.lastItem) {
                            val item = navigator.lastItem
                            
                            when (item) {
                                is SettingsScreen -> {
                                    currentSection = "settings"
                                }
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
                                is ScriptWorkspaceScreen -> {
                                    isInProject = true
                                    activeProjectId = item.projectId.toString()
                                    currentSection = "script"
                                }
                                is SceneListScreen -> {
                                    isInProject = true
                                    activeProjectId = item.projectId
                                    currentSection = "scenes"
                                }
                                is SceneWorkspaceScreen -> {
                                    isInProject = true
                                    activeProjectId = item.projectId.toString()
                                    currentSection = "scenes"
                                }
                                is KppListScreen -> {
                                    isInProject = true
                                    activeProjectId = item.projectId.toString()
                                    currentSection = "schedule"
                                }
                                is KppWorkspaceScreen -> {
                                    isInProject = true
                                    activeProjectId = item.projectId.toString()
                                    currentSection = "schedule"
                                }
                                is TrackerScreen -> {
                                    isInProject = true
                                    activeProjectId = item.projectId
                                    currentSection = "tracker"
                                }
                                is PropListScreen -> {
                                    isInProject = true
                                    activeProjectId = item.projectId.toString()
                                    currentSection = "inventory"
                                }
                                is PropWorkspaceScreen -> {
                                    isInProject = true
                                    activeProjectId = item.projectId.toString()
                                    currentSection = "inventory"
                                }
                                is CharacterBibleScreen -> {
                                    isInProject = true
                                    activeProjectId = item.projectId.toString()
                                    currentSection = "bible"
                                }
                                is CharacterWorkspaceScreen -> {
                                    isInProject = true
                                    activeProjectId = item.projectId.toString()
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
                                if (currentSection == section) return@AdaptiveScaffold

                                when (section) {
                                    "projects" -> navigator.popUntilRoot()
                                    "settings" -> navigator.push(SettingsScreen())
                                    "dashboard" -> activeProjectId?.let { navigator.replace(ProjectDashboardScreen(it)) }
                                    "script" -> activeProjectId?.let { id ->
                                        if (lastLayoutType == AppLayoutType.DESKTOP) {
                                            navigator.replace(ScriptWorkspaceScreen(id))
                                        } else {
                                            navigator.replace(ScriptListScreen(id))
                                        }
                                    }
                                    "scenes" -> activeProjectId?.let { id ->
                                        if (lastLayoutType == AppLayoutType.DESKTOP) {
                                            navigator.replace(SceneWorkspaceScreen(id))
                                        } else {
                                            navigator.replace(SceneListScreen(id, "Проект"))
                                        }
                                    }
                                    "schedule" -> activeProjectId?.let { id ->
                                        if (lastLayoutType == AppLayoutType.DESKTOP) {
                                            navigator.replace(KppWorkspaceScreen(id))
                                        } else {
                                            navigator.replace(KppListScreen(id))
                                        }
                                    }
                                    "tracker" -> activeProjectId?.let { navigator.replace(TrackerScreen(it)) }
                                    "inventory" -> activeProjectId?.let { id ->
                                        if (lastLayoutType == AppLayoutType.DESKTOP) {
                                            navigator.replace(PropWorkspaceScreen(id))
                                        } else {
                                            navigator.replace(PropListScreen(id))
                                        }
                                    }
                                    "bible" -> activeProjectId?.let { id ->
                                        if (lastLayoutType == AppLayoutType.DESKTOP) {
                                            navigator.replace(CharacterWorkspaceScreen(id))
                                        } else {
                                            navigator.replace(CharacterBibleScreen(id))
                                        }
                                    }
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
}
