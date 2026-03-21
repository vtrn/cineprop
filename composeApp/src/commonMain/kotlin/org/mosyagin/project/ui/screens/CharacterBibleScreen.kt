/**
 * Экран "Библия персонажей".
 * 
 * Отображает список всех персонажей проекта с возможностью развернуть каждого
 * и увидеть список его сцен и локаций.
 * Реализует функционал для Milestone #23.
 */
package org.mosyagin.project.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.koin.compose.koinInject
import org.mosyagin.project.Actor
import org.mosyagin.project.Scene
import org.mosyagin.project.repository.SceneRepository
import org.mosyagin.project.ui.components.CineEmptyState

/**
 * Главный экран Библии персонажей.
 */
data class CharacterBibleScreen(val projectId: Long) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val repository = koinInject<SceneRepository>()

        val actors by repository.getActorsByProject(projectId).collectAsState(initial = emptyList())

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Библия персонажей") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                        }
                    }
                )
            }
        ) { padding ->
            if (actors.isEmpty()) {
                CineEmptyState(
                    modifier = Modifier.padding(padding),
                    title = "Библия пуста",
                    description = "Здесь появится список героев после загрузки и парсинга сценария.",
                    icon = Icons.Default.Person
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(actors) { actor ->
                        ExpandableCharacterCard(actor)
                    }
                }
            }
        }
    }
}

/**
 * Раскрывающаяся карточка персонажа.
 */
@Composable
fun ExpandableCharacterCard(actor: Actor) {
    val repository = koinInject<SceneRepository>()
    var expanded by remember { mutableStateOf(false) }

    // Загружаем сцены и локации только если карточка развернута
    val actorScenes by remember(actor.id, expanded) {
        if (expanded) repository.getScenesByActor(actor.id)
        else kotlinx.coroutines.flow.flowOf(emptyList())
    }.collectAsState(initial = emptyList())

    val actorLocations by remember(actor.id, expanded) {
        if (expanded) repository.getLocationsByActor(actor.id)
        else kotlinx.coroutines.flow.flowOf(emptyList())
    }.collectAsState(initial = emptyList())

    OutlinedCard(
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text(actor.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1F))
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                    HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))

                    Text("ЛОКАЦИИ:", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.height(4.dp))
                    if (actorLocations.isEmpty()) {
                        Text("Нет данных", style = MaterialTheme.typography.bodySmall)
                    } else {
                        Text(
                            text = actorLocations.joinToString(", "),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Text("СЦЕНЫ:", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.height(8.dp))
                    
                    if (actorScenes.isEmpty()) {
                        Text("Нет данных", style = MaterialTheme.typography.bodySmall)
                    } else {
                        actorScenes.forEach { scene ->
                            SceneItem(scene)
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SceneItem(scene: Scene) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Movie, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Сцена ${scene.seriesNumber}-${scene.sceneNumber}: ${scene.location}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
