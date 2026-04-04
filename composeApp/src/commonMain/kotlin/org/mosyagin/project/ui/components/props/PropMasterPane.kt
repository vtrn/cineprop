package org.mosyagin.project.ui.components.props

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.mosyagin.project.repository.PropWithScene

/**
 * Панель управления категориями (Левая панель).
 * Позволяет фильтровать список реквизита по его типу.
 *
 * @param propsByCategory Словарь, где ключ - название категории, а значение - список объектов в ней.
 * @param selectedCategoryFilter Текущая выбранная категория для фильтрации.
 * @param onCategoryFilterSelected Обработчик выбора категории.
 * @param onExportClick Обработчик нажатия на кнопку экспорта.
 */
@Composable
fun PropMasterPane(
    propsByCategory: Map<String, List<PropWithScene>>,
    selectedCategoryFilter: String?,
    onCategoryFilterSelected: (String?) -> Unit,
    onExportClick: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Категории", 
                style = MaterialTheme.typography.headlineSmall, 
                fontWeight = FontWeight.Bold, 
                color = MaterialTheme.colorScheme.onSurface
            )
            
            IconButton(onClick = onExportClick) {
                Icon(
                    imageVector = Icons.Default.FileUpload, 
                    contentDescription = "Экспорт реквизита",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Кнопка сброса фильтра (показать всё)
        CategoryItem(
            title = "Весь реквизит", 
            icon = Icons.Default.AllInclusive, 
            isSelected = selectedCategoryFilter == null,
            color = MaterialTheme.colorScheme.primary
        ) {
            onCategoryFilterSelected(null)
        }

        Spacer(Modifier.height(16.dp))

        // Список доступных категорий
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            propsByCategory.forEach { (category, props) ->
                item {
                    CategoryItem(
                        title = category,
                        icon = PropUiUtils.getCategoryIcon(category),
                        isSelected = selectedCategoryFilter == category,
                        count = props.size,
                        color = PropUiUtils.getCategoryColor(category)
                    ) { onCategoryFilterSelected(category) }
                }
            }
        }
    }
}

/**
 * Индивидуальный элемент списка категорий.
 */
@Composable
private fun CategoryItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    count: Int? = null,
    color: Color,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(if (isSelected) color.copy(alpha = 0.15f) else Color.Transparent)
    val contentColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    val border = if (isSelected) BorderStroke(1.dp, color.copy(alpha = 0.3f)) else null

    Surface(
        onClick = onClick,
        color = bgColor,
        shape = RoundedCornerShape(10.dp),
        border = border,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon, 
                contentDescription = null, 
                modifier = Modifier.size(18.dp), 
                tint = if (isSelected) color else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = title.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }, 
                modifier = Modifier.weight(1f), 
                style = MaterialTheme.typography.bodyMedium, 
                color = contentColor
            )
            if (count != null) {
                Text(
                    text = count.toString(), 
                    style = MaterialTheme.typography.labelSmall, 
                    color = if (isSelected) color else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}
