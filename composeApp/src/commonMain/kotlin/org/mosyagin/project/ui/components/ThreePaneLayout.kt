package org.mosyagin.project.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun ThreePaneLayout(
    masterPane: @Composable BoxScope.() -> Unit,
    detailPane: @Composable BoxScope.() -> Unit,
    inspectorPane: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier
) {
    // Используем BoxWithConstraints для получения текущей ширины окна
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface) // Установили surface как основной фон для всего экрана
    ) {
        val totalWidth = maxWidth
        val scrollState = rememberScrollState()

        // РАССЧЕТ ПРОЦЕНТОВ (22% / 56% / 22%)
        val sideWidth = (totalWidth * 0.22f).coerceAtLeast(300.dp)
        val centerWidth = (totalWidth * 0.56f).coerceAtLeast(800.dp)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(scrollState)
        ) {
            Row(
                modifier = Modifier.fillMaxHeight().wrapContentWidth()
            ) {
                // ЛЕВАЯ ПАНЕЛЬ
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .requiredWidth(sideWidth)
                ) {
                    masterPane()
                }

                VerticalDivider(
                    modifier = Modifier.fillMaxHeight(),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                )

                // ЦЕНТРАЛЬНАЯ ПАНЕЛЬ
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .requiredWidth(centerWidth)
                        .padding(16.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp)),
                        color = MaterialTheme.colorScheme.surface, // Тот же цвет, что и у подложки
                        tonalElevation = 0.dp, // Убрали подкрашивание, чтобы цвета слились
                        shadowElevation = 2.dp // Оставили легкую тень для объема «листа»
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Box(modifier = Modifier.widthIn(max = 800.dp).fillMaxHeight()) {
                                detailPane()
                            }
                        }
                    }
                }

                VerticalDivider(
                    modifier = Modifier.fillMaxHeight(),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                )

                // ПРАВАЯ ПАНЕЛЬ
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .requiredWidth(sideWidth)
                ) {
                    inspectorPane()
                }
            }
        }
    }
}
