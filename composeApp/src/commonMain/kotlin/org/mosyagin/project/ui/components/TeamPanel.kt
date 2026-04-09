package org.mosyagin.project.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.mosyagin.project.ProjectMember
import org.mosyagin.project.repository.MemberRepository

@Composable
fun TeamPanel(
    isOpen: Boolean,
    projectId: String?,
    onClose: () -> Unit
) {
    val memberRepository = koinInject<MemberRepository>()
    val scope = rememberCoroutineScope()
    
    val members by if (projectId != null) {
        memberRepository.getMembersByProject(projectId).collectAsState(emptyList())
    } else {
        remember { mutableStateOf(emptyList()) }
    }

    var showInviteDialog by remember { mutableStateOf(false) }

    if (showInviteDialog && projectId != null) {
        InviteMemberDialog(
            onDismiss = { showInviteDialog = false },
            onInvite = { email, role ->
                scope.launch {
                    memberRepository.addMember(projectId, email, role)
                    showInviteDialog = false
                }
            }
        )
    }

    AnimatedVisibility(
        visible = isOpen,
        enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.CenterEnd
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.1f))
                    .clickable { onClose() }
            )

            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.25f)
                    .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable(enabled = false) {},
                tonalElevation = 8.dp,
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Команда проекта",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.Close, contentDescription = "Закрыть")
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    Text(
                        "УЧАСТНИКИ (${members.size})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(16.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(members) { member ->
                            TeamMemberItem(
                                name = member.email.split("@").first().replaceFirstChar { it.uppercase() },
                                email = member.email,
                                role = when(member.role) {
                                    "owner" -> "Владелец"
                                    "editor" -> "Редактор"
                                    else -> "Зритель"
                                },
                                isOwner = member.role == "owner",
                                onRemove = {
                                    scope.launch { memberRepository.removeMember(member.id) }
                                }
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = { showInviteDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Пригласить")
                    }
                }
            }
        }
    }
}

@Composable
private fun InviteMemberDialog(
    onDismiss: () -> Unit,
    onInvite: (String, String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("editor") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Пригласить участника") },
        text = {
            Column {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(16.dp))
                Text("Роль:", style = MaterialTheme.typography.labelMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = selectedRole == "editor", onClick = { selectedRole = "editor" })
                    Text("Редактор", modifier = Modifier.clickable { selectedRole = "editor" })
                    Spacer(Modifier.width(16.dp))
                    RadioButton(selected = selectedRole == "viewer", onClick = { selectedRole = "viewer" })
                    Text("Зритель", modifier = Modifier.clickable { selectedRole = "viewer" })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onInvite(email, selectedRole) },
                enabled = email.contains("@")
            ) {
                Text("Пригласить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

@Composable
private fun TeamMemberItem(
    name: String,
    email: String,
    role: String,
    isOwner: Boolean = false,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = if (isOwner) Color(0xFFE67E22) else MaterialTheme.colorScheme.secondaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    name.take(1).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            Text(email, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Surface(
            color = if (isOwner) Color(0xFFE67E22).copy(alpha = 0.1f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                role,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = if (isOwner) Color(0xFFE67E22) else MaterialTheme.colorScheme.primary
            )
        }

        if (!isOwner) {
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.DeleteOutline, 
                    contentDescription = "Удалить",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
