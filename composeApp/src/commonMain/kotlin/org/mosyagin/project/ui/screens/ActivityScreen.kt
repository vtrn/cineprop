@file:OptIn(ExperimentalMaterial3Api::class)

package org.mosyagin.project.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.core.parameter.parametersOf
import org.mosyagin.project.ActivityLog
import org.mosyagin.project.ui.components.CineTag

data class ActivityScreen(val projectId: String) : Screen {

    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<ActivityViewModel> { parametersOf(projectId) }
        val activities by viewModel.activities.collectAsState()
        val selectedFilter by viewModel.selectedFilter.collectAsState()
        val searchQuery by viewModel.searchQuery.collectAsState()

        Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            // Header
            Text(
                "Журнал изменений",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(16.dp)
            )

            // Filters
            ScrollableTabRow(
                selectedTabIndex = ActivityFilter.entries.indexOf(selectedFilter),
                edgePadding = 16.dp,
                containerColor = Color.Transparent,
                divider = {}
            ) {
                ActivityFilter.entries.forEach { filter ->
                    Tab(
                        selected = selectedFilter == filter,
                        onClick = { viewModel.onFilterSelected(filter) },
                        text = {
                            Text(
                                when (filter) {
                                    ActivityFilter.ALL -> "Все"
                                    ActivityFilter.SCENARIO -> "Сценарий"
                                    ActivityFilter.KPP -> "КПП"
                                    ActivityFilter.PROP -> "Реквизит"
                                    ActivityFilter.PHOTO -> "Фото"
                                }
                            )
                        }
                    )
                }
            }

            // Search
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                placeholder = { Text("Поиск...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                shape = CircleShape,
                singleLine = true
            )

            // Content
            if (activities.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Записей пока нет", color = MaterialTheme.colorScheme.outline)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Группировка по датам (упрощенно)
                    items(activities) { activity ->
                        ActivityItem(activity)
                    }
                }
            }
        }
    }

    @Composable
    private fun ActivityItem(activity: ActivityLog) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    activity.userName.take(1).uppercase(),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        activity.userName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(8.dp))
                    
                    val tagData = when(activity.type) {
                        "SCENARIO" -> "Сценарий" to Color(0xFFE3F2FD)
                        "PROP" -> "Реквизит" to Color(0xFFF1F8E9)
                        "KPP" -> "КПП" to Color(0xFFFFF3E0)
                        else -> "Фото" to Color(0xFFF3E5F5)
                    }
                    
                    CineTag(
                        text = tagData.first,
                        containerColor = tagData.second,
                        contentColor = Color.DarkGray
                    )
                    
                    Spacer(Modifier.weight(1f))
                    
                    Text(
                        formatTime(activity.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Text(
                    text = activity.encryptedDescription ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (!activity.encryptedEntityName.isNullOrBlank()) {
                    Text(
                        text = activity.encryptedEntityName!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

    private fun formatTime(timestamp: Long): String {
        val dt = Instant.fromEpochMilliseconds(timestamp).toLocalDateTime(TimeZone.currentSystemDefault())
        return "${dt.hour.toString().padStart(2, '0')}:${dt.minute.toString().padStart(2, '0')}"
    }
}
