package org.mosyagin.project.ui.components

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
fun SceneMobileLayout(
    masterPane: @Composable (isCollapsed: Boolean) -> Unit,
    detailPane: @Composable () -> Unit,
    inspectorPane: @Composable () -> Unit,
    isLeftExpanded: Boolean,
    onToggleLeft: (Boolean) -> Unit,
    isInspectorVisible: Boolean,
    onCloseInspector: () -> Unit
) {
    val leftExpandedWidth = 280.dp
    val leftCollapsedWidth = 0.dp // На мобилках для сцен лучше скрывать совсем или оставить узкую полоску
    
    val currentLeftOffset by animateDpAsState(
        targetValue = if (isLeftExpanded) leftExpandedWidth else leftCollapsedWidth
    )

    val rightPanelOffset by animateDpAsState(
        targetValue = if (isInspectorVisible) 0.dp else 500.dp
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
        // 1. ЛЕВАЯ ПАНЕЛЬ (Список сцен - нижний слой)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(leftExpandedWidth)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            masterPane(!isLeftExpanded)
        }

        // 2. ЦЕНТРАЛЬНАЯ ПАНЕЛЬ (Текст сценария - средний слой)
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .offset(x = currentLeftOffset)
                .zIndex(1f)
                .draggable(
                    state = draggableState,
                    orientation = Orientation.Horizontal,
                ),
            shadowElevation = if (isLeftExpanded) 12.dp else 0.dp,
            color = MaterialTheme.colorScheme.background
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                detailPane()
                
                // Тап по тексту закрывает левое меню, если оно открыто
                if (isLeftExpanded) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.1f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onToggleLeft(false) }
                    )
                }
            }
        }

        // 3. ИНСПЕКТОР (Правая панель - верхний слой)
        if (isInspectorVisible || rightPanelOffset < 500.dp) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(2f)
                    .background(Color.Black.copy(alpha = 0.4f * (1f - rightPanelOffset.value / 500f)))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onCloseInspector() }
            )

            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.9f) 
                    .align(Alignment.CenterEnd)
                    .offset(x = rightPanelOffset)
                    .zIndex(3f),
                shadowElevation = 24.dp,
                color = MaterialTheme.colorScheme.surface,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp)
            ) {
                inspectorPane()
            }
        }
    }
}
