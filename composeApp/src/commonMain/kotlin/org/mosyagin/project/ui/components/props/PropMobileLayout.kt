package org.mosyagin.project.ui.components.props

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

@Composable
fun PropMobileLayout(
    leftPane: @Composable (isCollapsed: Boolean) -> Unit,
    centerPane: @Composable () -> Unit,
    rightPane: @Composable () -> Unit,
    isLeftExpanded: Boolean,
    onToggleLeft: (Boolean) -> Unit,
    isRightVisible: Boolean,
    onCloseRight: () -> Unit
) {
    val leftExpandedWidth = 260.dp
    val leftCollapsedWidth = 56.dp
    
    val currentLeftOffset by animateDpAsState(
        targetValue = if (isLeftExpanded) leftExpandedWidth else leftCollapsedWidth
    )

    val rightPanelOffset by animateDpAsState(
        targetValue = if (isRightVisible) 0.dp else 450.dp
    )

    val draggableState = rememberDraggableState { delta ->
        if (delta > 20 && !isLeftExpanded) onToggleLeft(true)
        if (delta < -20 && isLeftExpanded) onToggleLeft(false)
    }

    BoxWithConstraints(modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.surface)
        .statusBarsPadding() 
    ) {
        val screenWidth = maxWidth
        
        // ВАЖНО: Фиксированная ширина контента = Экран минус узкая полоска иконок
        // Это гарантирует, что в обычном виде всё влезет на 100%
        val baseContentWidth = screenWidth - leftCollapsedWidth

        // 1. ЛЕВАЯ ПАНЕЛЬ (Нижний слой)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(leftExpandedWidth)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            leftPane(!isLeftExpanded)
        }

        // 2. ЦЕНТРАЛЬНАЯ ПАНЕЛЬ (Средний слой)
        Surface(
            modifier = Modifier
                .fillMaxHeight()
                .width(baseContentWidth) // Установили "фиксированную" ширину
                .offset(x = currentLeftOffset) // Сдвигаем
                .zIndex(1f)
                .draggable(
                    state = draggableState,
                    orientation = Orientation.Horizontal,
                ),
            shadowElevation = if (isLeftExpanded) 12.dp else 0.dp,
            color = MaterialTheme.colorScheme.background,
            border = if (!isLeftExpanded) 
                androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.05f)) 
                else null
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                centerPane()
                
                if (isLeftExpanded) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onToggleLeft(false) }
                    )
                }
            }
        }

        // 3. ПРАВАЯ ПАНЕЛЬ (Верхний слой)
        if (isRightVisible || rightPanelOffset < 450.dp) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(2f)
                    .background(Color.Black.copy(alpha = 0.4f * (1f - rightPanelOffset.value / 450f)))
                    .clickable { onCloseRight() }
            )

            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.88f) 
                    .align(Alignment.CenterEnd)
                    .offset(x = rightPanelOffset)
                    .zIndex(3f),
                shadowElevation = 24.dp,
                color = MaterialTheme.colorScheme.surface,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp)
            ) {
                rightPane()
            }
        }
    }
}
