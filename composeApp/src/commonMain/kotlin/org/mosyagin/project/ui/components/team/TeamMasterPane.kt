package org.mosyagin.project.ui.components.team

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun TeamMasterPane(
    roles: List<String>,
    selectedRole: String?,
    isCollapsed: Boolean = false,
    onRoleSelected: (String?) -> Unit,
    onToggleExpand: () -> Unit = {}
) {
    val collapsedVisibleWidth = 56.dp
    
    Column(modifier = Modifier.fillMaxHeight().width(260.dp)) {
        if (!isCollapsed) {
            Text(
                "Команда", 
                style = MaterialTheme.typography.headlineSmall, 
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp).padding(top = 8.dp)
            )
        } else {
            Spacer(Modifier.height(24.dp))
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = if (isCollapsed) 0.dp else 16.dp)
        ) {
            item {
                RoleItem(
                    title = "Все",
                    icon = Icons.Default.Groups,
                    isSelected = selectedRole == null,
                    color = MaterialTheme.colorScheme.primary,
                    isCollapsed = isCollapsed,
                    collapsedWidth = collapsedVisibleWidth,
                    onDoubleClick = onToggleExpand,
                    onClick = { onRoleSelected(null) }
                )
            }

            items(roles) { role ->
                RoleItem(
                    title = role.replaceFirstChar { it.uppercase() },
                    icon = getRoleIcon(role),
                    isSelected = selectedRole == role,
                    color = getRoleColor(role),
                    isCollapsed = isCollapsed,
                    collapsedWidth = collapsedVisibleWidth,
                    onDoubleClick = onToggleExpand,
                    onClick = { onRoleSelected(role) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RoleItem(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    color: Color,
    isCollapsed: Boolean,
    collapsedWidth: androidx.compose.ui.unit.Dp,
    onDoubleClick: () -> Unit,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(if (isSelected) color.copy(alpha = 0.15f) else Color.Transparent)
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isCollapsed) 56.dp else 48.dp)
            .combinedClickable(onClick = onClick, onDoubleClick = onDoubleClick)
            .background(bgColor, RoundedCornerShape(12.dp))
            .then(if (isSelected) Modifier.border(BorderStroke(1.dp, color.copy(alpha = 0.3f)), RoundedCornerShape(12.dp)) else Modifier),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(collapsedWidth), contentAlignment = Alignment.Center) {
                Icon(icon, null, modifier = Modifier.size(22.dp), tint = if (isSelected) color else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
            if (!isCollapsed) {
                Text(title, style = MaterialTheme.typography.bodyMedium, color = if (isSelected) color else MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

private fun getRoleIcon(role: String): ImageVector = when(role.lowercase()) {
    "owner" -> Icons.Default.AdminPanelSettings
    "editor" -> Icons.Default.Edit
    else -> Icons.Default.Visibility
}

private fun getRoleColor(role: String): Color = when(role.lowercase()) {
    "owner" -> Color(0xFFFF9500) // Gold
    "editor" -> Color(0xFF5856D6) // Indigo
    else -> Color(0xFF8E8E93) // Gray
}
