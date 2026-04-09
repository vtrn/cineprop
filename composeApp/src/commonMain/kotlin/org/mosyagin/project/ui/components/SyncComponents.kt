package org.mosyagin.project.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class ProjectSyncStatus {
    SYNCED, DIRTY, LOCAL, REQUIRES_AUTH, ERROR
}

@Composable
fun StatusBadge(
    status: ProjectSyncStatus,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val (text, icon, color) = when (status) {
        ProjectSyncStatus.SYNCED -> Triple("Синхронизирован", Icons.Default.Done, Color(0xFF4CAF50))
        ProjectSyncStatus.DIRTY -> Triple("Есть изменения", Icons.Default.Sync, Color(0xFFFFA000))
        ProjectSyncStatus.LOCAL -> Triple("Только локально", Icons.Default.CloudOff, Color(0xFF9E9E9E))
        ProjectSyncStatus.REQUIRES_AUTH -> Triple("Требует входа", Icons.Default.Lock, Color(0xFFE67E22))
        ProjectSyncStatus.ERROR -> Triple("Ошибка синхр.", Icons.Default.ErrorOutline, Color(0xFFE53935))
    }

    val isClickable = status == ProjectSyncStatus.LOCAL || status == ProjectSyncStatus.REQUIRES_AUTH

    Surface(
        modifier = modifier.then(
            if (isClickable && onClick != null) Modifier.clickable { onClick() } else Modifier
        ),
        color = color.copy(alpha = 0.15f),
        shape = CircleShape,
        border = null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = color
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Medium
            )
            if (isClickable) {
                Icon(
                    imageVector = Icons.Default.ArrowOutward,
                    contentDescription = null,
                    modifier = Modifier.size(10.dp),
                    tint = color.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun UserAvatar(
    email: String?,
    isOnline: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val initials = email?.split("@")?.firstOrNull()?.take(2)?.uppercase() ?: "??"
    
    Box(modifier = modifier.clickable { onClick() }) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 2.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = initials,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Surface(
            modifier = Modifier
                .size(12.dp)
                .align(Alignment.BottomEnd),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            border = null
        ) {
            Box(
                modifier = Modifier
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(if (isOnline) Color(0xFF4CAF50) else Color(0xFFFFA000))
            )
        }
    }
}

@Composable
fun SyncBanner(
    message: String,
    isVisible: Boolean,
    icon: ImageVector = Icons.Default.WifiOff,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = expandVertically(),
        exit = shrinkVertically()
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFFFFA000).copy(alpha = 0.9f),
            contentColor = Color.Black
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                if (actionText != null && onAction != null) {
                    Text(
                        text = actionText,
                        modifier = Modifier
                            .clickable { onAction() }
                            .padding(8.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
