package org.mosyagin.project.ui.components.props

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

/**
 * Панель управления категориями (Левая панель).
 * Адаптивна: умеет сворачиваться до одних иконок.
 */
@Composable
fun PropMasterPane(
    categories: List<String>,
    selectedCategoryFilter: String?,
    isCollapsed: Boolean = false,
    onCategoryFilterSelected: (String?) -> Unit,
    onToggleExpand: () -> Unit = {},
    onExportClick: () -> Unit = {}
) {
    // Ширина видимой зоны в свернутом состоянии (должна совпадать с PropMobileLayout)
    val collapsedVisibleWidth = 56.dp
    
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(if (isCollapsed) 260.dp else 260.dp) // Сама панель всегда широкая для анимации
    ) {
        if (!isCollapsed) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 24.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Категории", 
                    style = MaterialTheme.typography.headlineSmall, 
                    fontWeight = FontWeight.Bold
                )
                
                IconButton(onClick = onExportClick) {
                    Icon(Icons.Default.FileUpload, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        } else {
            Spacer(Modifier.height(24.dp))
        }

        // Список категорий
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = if (isCollapsed) 0.dp else 16.dp)
        ) {
            item {
                CategoryItem(
                    title = "Все", 
                    icon = Icons.Default.AllInclusive, 
                    isSelected = selectedCategoryFilter == null,
                    color = MaterialTheme.colorScheme.primary,
                    isCollapsed = isCollapsed,
                    collapsedWidth = collapsedVisibleWidth,
                    onDoubleClick = onToggleExpand,
                    onClick = { onCategoryFilterSelected(null) }
                )
            }

            items(categories) { category ->
                CategoryItem(
                    title = category,
                    icon = PropUiUtils.getCategoryIcon(category),
                    isSelected = selectedCategoryFilter == category,
                    color = PropUiUtils.getCategoryColor(category),
                    isCollapsed = isCollapsed,
                    collapsedWidth = collapsedVisibleWidth,
                    onDoubleClick = onToggleExpand,
                    onClick = { onCategoryFilterSelected(category) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CategoryItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
            .combinedClickable(
                onClick = onClick,
                onDoubleClick = onDoubleClick
            )
            .background(bgColor, RoundedCornerShape(12.dp))
            .then(if (isSelected) Modifier.border(BorderStroke(1.dp, color.copy(alpha = 0.3f)), RoundedCornerShape(12.dp)) else Modifier),
        contentAlignment = Alignment.CenterStart // Выравниваем по левому краю!
    ) {
        Row(
            modifier = Modifier.fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Контейнер для иконки - всегда фиксированной ширины, чтобы быть видимым в узкой полосе
            Box(
                modifier = Modifier.width(collapsedWidth),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon, 
                    contentDescription = null, 
                    modifier = Modifier.size(22.dp), 
                    tint = if (isSelected) color else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            
            // Название категории (уходит под правую панель при сворачивании)
            if (!isCollapsed) {
                Text(
                    text = title.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }, 
                    style = MaterialTheme.typography.bodyMedium, 
                    maxLines = 1,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(end = 12.dp)
                )
            }
        }
    }
}
