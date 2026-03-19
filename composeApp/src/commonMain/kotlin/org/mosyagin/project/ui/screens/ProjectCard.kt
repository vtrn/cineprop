/**
 * Компонент карточки проекта.
 * 
 * Используется в списке проектов [ProjectListScreen].
 * Отображает краткую информацию (название, режиссер) и предоставляет кнопку удаления.
 */
package org.mosyagin.project.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.mosyagin.project.Project
import org.mosyagin.project.ui.theme.DarkSurface
import org.mosyagin.project.ui.theme.TextPrimary
import org.mosyagin.project.ui.theme.TextSecondary
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color

@Composable
fun ProjectCard(
    project: Project,
    onClick: () -> Unit, // Действие при клике на карточку (переход в Dashboard)
    onDelete: () -> Unit // Действие при нажатии на иконку корзины
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Название проекта (выделено жирным/крупным)
                Text(
                    text = project.name, 
                    style = MaterialTheme.typography.titleLarge, 
                    color = TextPrimary
                )
                // Имя режиссера
                Text(
                    text = "Режиссер: ${project.director}", 
                    color = TextSecondary
                )
            }

            // Кнопка удаления (иконка корзины)
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Удалить",
                    tint = Color.Red.copy(alpha = 0.7f) // Полупрозрачный красный цвет для акцента на удалении
                )
            }
        }
    }
}
