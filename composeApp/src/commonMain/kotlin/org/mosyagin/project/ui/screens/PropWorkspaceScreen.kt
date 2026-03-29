package org.mosyagin.project.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import org.koin.core.parameter.parametersOf
import org.mosyagin.project.ui.components.ThreePaneLayout
import org.mosyagin.project.ui.components.CineCard
import org.mosyagin.project.repository.PropWithScene

data class PropWorkspaceScreen(val projectId: Long) : Screen {

    @Composable
    override fun Content() {
        val screenModel = koinScreenModel<PropWorkspaceViewModel> { parametersOf(projectId) }
        
        val propsByCategory by screenModel.propsList.collectAsState()
        val searchQuery by screenModel.searchQuery.collectAsState()
        val selectedPropId by screenModel.selectedPropId.collectAsState()
        val expandedCategories by screenModel.expandedCategories.collectAsState()

        ThreePaneLayout(
            masterPane = {
                PropMasterPane(
                    propsByCategory = propsByCategory,
                    searchQuery = searchQuery,
                    selectedPropId = selectedPropId,
                    expandedCategories = expandedCategories,
                    onSearchChange = { screenModel.onSearchQueryChange(it) },
                    onPropClick = { screenModel.onPropSelected(it) },
                    onToggleCategory = { screenModel.toggleCategory(it) }
                )
            },
            detailPane = {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Детали реквизита появятся здесь", color = MaterialTheme.colorScheme.outline)
                }
            },
            inspectorPane = {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Инспектор реквизита появится здесь", color = MaterialTheme.colorScheme.outline)
                }
            }
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun PropMasterPane(
        propsByCategory: Map<String, List<PropWithScene>>,
        searchQuery: String,
        selectedPropId: Long?,
        expandedCategories: Set<String>,
        onSearchChange: (String) -> Unit,
        onPropClick: (Long) -> Unit,
        onToggleCategory: (String) -> Unit
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                "Реквизит",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp)
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Поиск реквизита...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                shape = CircleShape,
                singleLine = true
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                propsByCategory.forEach { (category, props) ->
                    val isExpanded = expandedCategories.contains(category)
                    
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleCategory(category) }
                                .padding(vertical = 4.dp),
                            color = Color.Transparent
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = category,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "(${props.size})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }

                    if (isExpanded) {
                        items(props) { prop ->
                            val isSelected = prop.id == selectedPropId
                            CineCard(
                                onClick = { onPropClick(prop.id) },
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer 
                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                modifier = Modifier.fillMaxWidth().padding(start = 16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Inventory,
                                        null,
                                        modifier = Modifier.size(18.dp),
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            prop.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                        Text(
                                            "Сцена ${prop.seriesNumber}-${prop.sceneNumber}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
