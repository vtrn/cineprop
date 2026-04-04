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
import androidx.compose.material.icons.filled.Delete
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

/**
 * Правая панель рабочего пространства: детальный просмотр и редактирование объекта.
 * Позволяет управлять заметками, подтверждать готовность и видеть контекст из сценария.
 *
 * @param propId ID выбранного объекта.
 * @param props Список всех доступных объектов (для поиска текущего).
 * @param actors Список актеров проекта для поиска владельца реквизита.
 * @param onNoteChange Обработчик сохранения заметки.
 * @param onConfirm Обработчик кнопки готовности.
 * @param onDelete Обработчик удаления из инспектора.
 * @param onActorClick Обработчик нажатия на имя персонажа (переход в библию).
 */
@Composable
fun PropInspectorPane(
    propId: Long?, 
    props: List<PropWithScene>, 
    actors: List<Actor> = emptyList(),
    onNoteChange: (Long, String) -> Unit, 
    onConfirm: (Long) -> Unit, 
    onDelete: (Long) -> Unit,
    onActorClick: (Long) -> Unit = {}
) {
    val prop = props.find { it.id == propId }
    val ownerActor = remember(prop, actors) { 
        actors.find { it.id == prop?.actorId } 
    }
    
    // Локальное состояние поля ввода заметок, обновляется при смене выбранного объекта
    var noteValue by remember(propId) { 
        mutableStateOf(TextFieldValue(prop?.note ?: "")) 
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (prop == null) {
            // Состояние "Ничего не выбрано"
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { 
                Text("Выберите объект", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) 
            }
        } else {
            Column(modifier = Modifier.padding(horizontal = 16.dp).fillMaxHeight()) {
                // Заголовок объекта
                Text(
                    prop.name, 
                    style = MaterialTheme.typography.headlineSmall, 
                    fontWeight = FontWeight.ExtraBold, 
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 24.dp, bottom = 16.dp)
                )
                
                // Бейджи категории, персонажа и сцены
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    maxItemsInEachRow = 3
                ) {
                    // Категория
                    Surface(
                        color = PropUiUtils.getCategoryColor(prop.category).copy(alpha = 0.12f),
                        shape = CircleShape,
                        border = BorderStroke(0.5.dp, PropUiUtils.getCategoryColor(prop.category).copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                PropUiUtils.getCategoryIcon(prop.category), 
                                contentDescription = null, 
                                modifier = Modifier.size(14.dp), 
                                tint = PropUiUtils.getCategoryColor(prop.category)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(prop.category, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    // ПЕРСОНАЖ (Если реквизит персонажный)
                    if (ownerActor != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                            shape = CircleShape,
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)),
                            modifier = Modifier.clickable { onActorClick(ownerActor.id) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Person, 
                                    contentDescription = null, 
                                    modifier = Modifier.size(14.dp), 
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(ownerActor.name, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }
                    }

                    // Сцена
                    Surface(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
                        shape = CircleShape,
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    ) {
                        Text(
                            "Сцена ${prop.seriesNumber}-${prop.sceneNumber}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Секция: Контекст из сценария (Anchor)
                Text(
                    "Контекст из сценария", 
                    style = MaterialTheme.typography.labelSmall, 
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), 
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        prop.anchor, 
                        style = MaterialTheme.typography.bodyMedium, 
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        lineHeight = 20.sp
                    )
                }

                Spacer(Modifier.height(24.dp))

                // Секция: Заметки
                Text(
                    "Заметки", 
                    style = MaterialTheme.typography.labelSmall, 
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), 
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.AutoMirrored.Filled.Notes, 
                                contentDescription = null, 
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), 
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Напишите что-нибудь...", 
                                style = MaterialTheme.typography.labelSmall, 
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        BasicTextField(
                            value = noteValue,
                            onValueChange = { 
                                noteValue = it
                                onNoteChange(prop.id, it.text) 
                            },
                            modifier = Modifier.fillMaxSize(),
                            textStyle = TextStyle(fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface, lineHeight = 22.sp),
                            cursorBrush = Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary))
                        )
                    }
                }
                
                Spacer(Modifier.height(24.dp))
                
                // Кнопки управления готовностью и удалением
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { onConfirm(prop.id) },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Готовность", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    Button(
                        onClick = { onDelete(prop.id) },
                        modifier = Modifier.height(50.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f), 
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Удалить")
                    }
                }
            }
        }
    }
}
