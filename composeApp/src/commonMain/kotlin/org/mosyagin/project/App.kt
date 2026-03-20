package org.mosyagin.project

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import org.koin.compose.KoinContext
import org.mosyagin.project.ui.screens.ProjectListScreen
import org.mosyagin.project.ui.theme.CinePropTheme

@Composable
fun App() {
    CinePropTheme {
        KoinContext {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Navigator(ProjectListScreen()) { navigator ->
                    SlideTransition(navigator)
                }
            }
        }
    }
}
