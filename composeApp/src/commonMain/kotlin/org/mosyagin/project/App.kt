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
import org.mosyagin.project.ui.components.LocalAppLayoutType
import org.mosyagin.project.ui.screens.*
import org.mosyagin.project.ui.theme.CinePropTheme

@Composable
fun App() {
    KoinContext {
        val settingsRepository: SettingsRepository = koinInject()
        val authRepository: AuthRepository = koinInject()
        
        val themeMode by settingsRepository.getThemeMode().collectAsState("system")
        val currentUser by authRepository.currentUser.collectAsState(null)
        val isEncryptionReady by authRepository.isEncryptionReady.collectAsState()
        
        var skipAuth by remember { mutableStateOf(false) }
        
        LaunchedEffect(currentUser) {
            if (currentUser != null) {
                skipAuth = false
            }
        }
        
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
                if (currentUser == null && !skipAuth) {
                    Navigator(AuthScreen(onSkipAuth = { skipAuth = true })) { navigator ->
                        SlideTransition(navigator)
                    }
                } else if (!isEncryptionReady && !skipAuth) {
                    Navigator(CryptoSetupScreen()) { navigator ->
                        SlideTransition(navigator)
                    }
                } else {
                    var isInProject by remember { mutableStateOf(false) }
                    var currentSection by remember { mutableStateOf("projects") }
                    var activeProjectId by remember { mutableStateOf<String?>(null) }

                    Navigator(ProjectListScreen()) { navigator ->
                        LaunchedEffect(navigator.lastItem) {
                            val item = navigator.lastItem
                            
                            when (item) {
                                is SettingsScreen -> { currentSection = "settings" }
                                is ProjectDashboardScreen -> {
                                    isInProject = true
                                    activeProjectId = item.projectId
                                    currentSection = "dashboard"
                                }
                                is ScriptWorkspaceScreen -> {
                                    isInProject = true
                                    activeProjectId = item.projectId
                                    currentSection = "script"
                                }
                                is SceneWorkspaceScreen -> {
                                    isInProject = true
                                    activeProjectId = item.projectId
                                    currentSection = "scenes"
                                }
                                is KppWorkspaceScreen -> {
                                    isInProject = true
                                    activeProjectId = item.projectId
                                    currentSection = "schedule"
                                }
                                is TrackerScreen -> {
                                    isInProject = true
                                    activeProjectId = item.projectId
                                    currentSection = "tracker"
                                }
                                is PropWorkspaceScreen -> {
                                    isInProject = true
                                    activeProjectId = item.projectId
                                    currentSection = "inventory"
                                }
                                is CharacterWorkspaceScreen -> {
                                    isInProject = true
                                    activeProjectId = item.projectId
                                    currentSection = "bible"
                                }
                                is TeamScreen -> {
                                    isInProject = true
                                    activeProjectId = item.projectId
                                    currentSection = "team"
                                }
                                is ActivityScreen -> {
                                    isInProject = true
                                    activeProjectId = item.projectId
                                    currentSection = "activity"
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
                            projectId = activeProjectId,
                            onBackToProjects = {
                                navigator.popUntilRoot()
                            },
                            onSectionSelect = { section ->
                                if (currentSection == section) return@AdaptiveScaffold

                                when (section) {
                                    "projects" -> navigator.popUntilRoot()
                                    "settings" -> navigator.push(SettingsScreen())
                                    "dashboard" -> activeProjectId?.let { navigator.replace(ProjectDashboardScreen(it)) }
                                    "script" -> activeProjectId?.let { navigator.replace(ScriptWorkspaceScreen(it)) }
                                    "scenes" -> activeProjectId?.let { navigator.replace(SceneWorkspaceScreen(it)) }
                                    "schedule" -> activeProjectId?.let { navigator.replace(KppWorkspaceScreen(it)) }
                                    "tracker" -> activeProjectId?.let { navigator.replace(TrackerScreen(it)) }
                                    "inventory" -> activeProjectId?.let { navigator.replace(PropWorkspaceScreen(it)) }
                                    "bible" -> activeProjectId?.let { navigator.replace(CharacterWorkspaceScreen(it)) }
                                    "team" -> activeProjectId?.let { navigator.replace(TeamScreen(it)) }
                                    "activity" -> activeProjectId?.let { navigator.replace(ActivityScreen(it)) }
                                }
                            }
                        ) { _ ->
                            SlideTransition(navigator)
                        }
                    }
                }
            }
        }
    }
}
