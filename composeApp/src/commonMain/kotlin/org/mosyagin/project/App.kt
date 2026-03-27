package org.mosyagin.project

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import org.koin.compose.KoinContext
import org.mosyagin.project.ui.components.AdaptiveScaffold
import org.mosyagin.project.ui.components.AppLayoutType
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
                AdaptiveScaffold { layoutType ->
                    if (layoutType == AppLayoutType.DESKTOP) {
                        Row(Modifier.fillMaxSize()) {
                            // Временная заглушка для Sidebar (Navigation Rail)
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(80.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Rail", style = MaterialTheme.typography.labelSmall)
                            }

                            Box(modifier = Modifier.weight(1f)) {
                                NavigatorContent()
                            }
                        }
                    } else {
                        NavigatorContent()
                    }
                }
            }
        }
    }
}

@Composable
private fun NavigatorContent() {
    Navigator(ProjectListScreen()) { navigator ->
        SlideTransition(navigator)
    }
}
