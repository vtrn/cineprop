package org.mosyagin.project.ui.components.props

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.mosyagin.project.Actor
import org.mosyagin.project.repository.PropWithScene

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PropInspectorPane(
    propId: String?,
    props: List<PropWithScene>,
    actors: List<Actor> = emptyList(),
    onNoteChange: (String, String) -> Unit,
    onConfirm: (String) -> Unit,
    onDelete: (String) -> Unit,
    onActorClick: (String) -> Unit = {},
    onClose: (() -> Unit)? = null
) {
    val prop = props.find { it.id == propId }
    val ownerActor = remember(prop, actors) { 
        actors.find { it.id == prop?.actorId } 
    }
    
    var noteValue by remember(propId) { 
        mutableStateOf(TextFieldValue(prop?.note ?: "")) 
    }

    // Проверка на шифрование
    val isEncrypted = prop?.note == "Ошибка расшифровки"

    Column(modifier = Modifier.fillMaxSize()) {
        if (prop == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { 
                Text("Выберите объект", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) 
            }
        } else {
            Column(modifier = Modifier.padding(horizontal = 16.dp).fillMaxHeight()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Детали", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    if (onClose != null) {
                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.Close, contentDescription = "Закрыть")
                        }
                    }
                }

                Text(
                    prop.name, 
                    style = MaterialTheme.typography.headlineSmall, 
                    fontWeight = FontWeight.ExtraBold, 
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
                )
                
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = PropUiUtils.getCategoryColor(prop.category).copy(alpha = 0.12f),
                        shape = CircleShape,
                        border = BorderStroke(0.5.dp, PropUiUtils.getCategoryColor(prop.category).copy(alpha = 0.25f))
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(PropUiUtils.getCategoryIcon(prop.category), null, modifier = Modifier.size(14.dp), tint = PropUiUtils.getCategoryColor(prop.category))
                            Spacer(Modifier.width(6.dp))
                            Text(prop.category, style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    if (ownerActor != null) {
                        Surface(color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f), shape = CircleShape, modifier = Modifier.clickable { onActorClick(ownerActor.id) }) {
                            Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)) {
                                Icon(Icons.Default.Person, null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(ownerActor.name, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                Text("Контекст", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                Box(modifier = Modifier.fillMaxWidth().padding(top = 4.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)).padding(12.dp)) {
                    Text(prop.anchor, style = MaterialTheme.typography.bodyMedium)
                }

                Spacer(Modifier.height(16.dp))

                Text("Заметки", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                
                if (isEncrypted) {
                    // Плашка для зашифрованного контента
                    Box(
                        modifier = Modifier.fillMaxWidth().height(120.dp).padding(top = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f))
                            .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                            Text("Текст зашифрован", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxWidth().height(120.dp).padding(top = 4.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)).padding(12.dp)) {
                        BasicTextField(
                            value = noteValue,
                            onValueChange = { noteValue = it; onNoteChange(prop.id, it.text) },
                            modifier = Modifier.fillMaxSize(),
                            textStyle = TextStyle(fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                        )
                    }
                }
                
                Spacer(Modifier.weight(1f))
                
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { onConfirm(prop.id) }, modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(12.dp)) {
                        Text("Готовность")
                    }
                    IconButton(onClick = { onDelete(prop.id) }, modifier = Modifier.height(50.dp).width(50.dp).background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(12.dp))) {
                        Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        }
    }
}
