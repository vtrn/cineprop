package org.mosyagin.project.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class AppLayoutType {
    MOBILE, DESKTOP
}

// Цвета для "Pro" темы
val DeepBackgroundStart = Color(0xFF23272E)
val DeepBackgroundEnd = Color(0xFF16181D)
val SurfaceCard = Color(0xFF121418)
val BorderLight = Color.White.copy(alpha = 0.05f)

@Composable
private fun NavIcon(
    imageVector: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean = false
) {
    // Цвет акцента (можно заменить на твой основной фиолетовый)
    val activeColor = Color(0xFFBB86FC)
    val inactiveColor = Color.White.copy(alpha = 0.4f)

    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { /* Навигация будет тут */ },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = label,
            tint = if (isSelected) activeColor else inactiveColor,
            modifier = Modifier.size(26.dp)
        )

        // Маленький индикатор активного раздела слева
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(3.dp)
                    .height(3.dp)
                    .background(activeColor, RoundedCornerShape(2.dp))
            )
        }
    }
}

@Composable
fun AdaptiveScaffold(
    content: @Composable (AppLayoutType) -> Unit
) {
    BoxWithConstraints {
        val isDesktop = maxWidth > 800.dp
        val layoutType = if (isDesktop) AppLayoutType.DESKTOP else AppLayoutType.MOBILE

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(DeepBackgroundStart, DeepBackgroundEnd)
                    )
                )
        ) {
            if (isDesktop) {
                Row(modifier = Modifier.fillMaxSize()) {
                    // 1. Узкий Сайдбар (Navigation Rail)
                    Column(
                        modifier = Modifier
                            .width(48.dp)
                            .fillMaxHeight()
                            .padding(top = 56.dp, bottom = 24.dp), // 56.dp — чтобы не мешать кнопкам macOS
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // ВЕРХНЯЯ ЧАСТЬ: Основной рабочий раздел
                        NavIcon(
                            imageVector = Icons.Default.Folder,
                            label = "Проекты",
                            isSelected = true
                        )

                        Spacer(modifier = Modifier.weight(1f)) // "Пружина" — толкает всё остальное вниз

                        // НИЖНЯЯ ЧАСТЬ: Аккаунт и Настройки
                        NavIcon(
                            imageVector = Icons.Default.AccountCircle,
                            label = "Профиль"
                        )

                        Spacer(Modifier.height(8.dp))

                        NavIcon(
                            imageVector = Icons.Default.Settings,
                            label = "Настройки"
                        )
                    }

                    // 2. Основная рабочая область (Парящая карточка)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 0.dp, end = 16.dp, top = 24.dp, bottom = 16.dp)
                            .shadow(
                                elevation = 12.dp,
                                shape = RoundedCornerShape(12.dp),
                                clip = false,
                                ambientColor = Color.Black,
                                spotColor = Color.Black
                            )
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceCard)
                            .border(0.5.dp, BorderLight, RoundedCornerShape(12.dp))
                    ) {
                        content(layoutType)
                    }
                }
            } else {
                // Мобильная версия (на весь экран)
                Box(modifier = Modifier.fillMaxSize()) {
                    content(layoutType)
                }
            }
        }
    }
}
