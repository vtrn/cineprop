package org.mosyagin.project.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import org.koin.core.parameter.parametersOf
import org.mosyagin.project.Actor
import org.mosyagin.project.ui.components.ThreePaneLayout
import org.mosyagin.project.ui.components.CineCard

data class CharacterWorkspaceScreen(val projectId: Long) : Screen {

    @Composable
    override fun Content() {
        val screenModel = koinScreenModel<CharacterWorkspaceViewModel> { parametersOf(projectId) }
        
        val characters by screenModel.characters.collectAsState()
        val selectedId by screenModel.selectedActorId.collectAsState()
        val details by screenModel.selectedCharacterDetails.collectAsState()

        ThreePaneLayout(
            masterPane = {
                Column(modifier = Modifier.fillMaxSize()) {
                    Text(
                        "Библия персонажей",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(characters) { character ->
                            val isSelected = character.id == selectedId
                            CineCard(
                                onClick = { screenModel.onCharacterSelected(character.id) },
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer 
                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Person,
                                        null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        character.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            },
            detailPane = {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                    if (details != null) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Text(
                                text = characters.find { it.id == selectedId }?.name ?: "",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Spacer(Modifier.height(24.dp))
                            
                            Text("ЛОКАЦИИ", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(8.dp))
                            if (details!!.locations.isEmpty()) {
                                Text("Нет данных", style = MaterialTheme.typography.bodyMedium)
                            } else {
                                Text(details!!.locations.joinToString(", "), style = MaterialTheme.typography.bodyMedium)
                            }
                            
                            Spacer(Modifier.height(32.dp))
                            
                            Text("СЦЕНЫ", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(8.dp))
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(details!!.scenes) { scene ->
                                    CineCard {
                                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Movie, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.outline)
                                            Spacer(Modifier.width(8.dp))
                                            Text("Сцена ${scene.seriesNumber}-${scene.sceneNumber}: ${scene.location}", style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Выберите персонажа слева", color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            },
            inspectorPane = {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("Инспектор персонажа", color = MaterialTheme.colorScheme.outline)
                }
            }
        )
    }
}
