package org.mosyagin.project.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class AppLayoutType {
    MOBILE, DESKTOP
}

val LocalAppLayoutType = staticCompositionLocalOf { AppLayoutType.MOBILE }

val DeepBackgroundStart = Color(0xFF23272E)
val DeepBackgroundEnd = Color(0xFF16181D)
val SurfaceCard = Color(0xFF121418)
val BorderLight = Color.White.copy(alpha = 0.05f)

@Composable
private fun NavIcon(
    imageVector: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean = false,
    onClick: () -> Unit = {}
) {
    val activeColor = Color(0xFFBB86FC)
    val inactiveColor = Color.White.copy(alpha = 0.4f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = imageVector,
                contentDescription = label,
                tint = if (isSelected) activeColor else inactiveColor,
                modifier = Modifier.size(24.dp)
            )

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = (-26).dp)
                        .width(3.dp)
                        .height(16.dp)
                        .background(activeColor, RoundedCornerShape(2.dp))
                )
            }
        }
        
        Spacer(Modifier.height(4.dp))
        
        Text(
            text = label,
            color = if (isSelected) activeColor else inactiveColor,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1
        )
    }
}

@Composable
fun AdaptiveScaffold(
    isInProject: Boolean = false,
    currentSection: String = "projects",
    onBackToProjects: () -> Unit = {},
    onSectionSelect: (String) -> Unit = {},
    content: @Composable (AppLayoutType) -> Unit
) {
    BoxWithConstraints {
        val isDesktop = maxWidth > 800.dp
        val layoutType = if (isDesktop) AppLayoutType.DESKTOP else AppLayoutType.MOBILE

        CompositionLocalProvider(LocalAppLayoutType provides layoutType) {
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
                        Column(
                            modifier = Modifier
                                .width(84.dp)
                                .fillMaxHeight()
                                .padding(top = 40.dp, bottom = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (!isInProject) {
                                NavIcon(
                                    imageVector = Icons.Default.Folder,
                                    label = "Проекты",
                                    isSelected = currentSection == "projects",
                                    onClick = { onSectionSelect("projects") }
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                NavIcon(
                                    imageVector = Icons.Default.AccountCircle,
                                    label = "Профиль",
                                    isSelected = currentSection == "profile",
                                    onClick = { onSectionSelect("profile") }
                                )
                                NavIcon(
                                    imageVector = Icons.Default.Settings,
                                    label = "Настройки",
                                    isSelected = currentSection == "settings",
                                    onClick = { onSectionSelect("settings") }
                                )
                            } else {
                                // ЗАМЕНЕНО: Иконка папки для возврата к проектам
                                NavIcon(
                                    imageVector = Icons.Default.Folder,
                                    label = "Проекты",
                                    onClick = onBackToProjects
                                )

                                Spacer(Modifier.height(24.dp))

                                NavIcon(
                                    imageVector = Icons.Default.Dashboard,
                                    label = "Обзор",
                                    isSelected = currentSection == "dashboard",
                                    onClick = { onSectionSelect("dashboard") }
                                )
                                NavIcon(
                                    imageVector = Icons.Default.Description,
                                    label = "Сценарий",
                                    isSelected = currentSection == "script",
                                    onClick = { onSectionSelect("script") }
                                )
                                NavIcon(
                                    imageVector = Icons.AutoMirrored.Filled.FormatListBulleted,
                                    label = "Сцены",
                                    isSelected = currentSection == "scenes",
                                    onClick = { onSectionSelect("scenes") }
                                )
                                NavIcon(
                                    imageVector = Icons.Default.Event,
                                    label = "КПП",
                                    isSelected = currentSection == "schedule",
                                    onClick = { onSectionSelect("schedule") }
                                )
                                NavIcon(
                                    imageVector = Icons.Default.AddAPhoto,
                                    label = "Трекер",
                                    isSelected = currentSection == "tracker",
                                    onClick = { onSectionSelect("tracker") }
                                )
                                NavIcon(
                                    imageVector = Icons.Default.Inventory2,
                                    label = "Реквизит",
                                    isSelected = currentSection == "inventory",
                                    onClick = { onSectionSelect("inventory") }
                                )
                                NavIcon(
                                    imageVector = Icons.Default.AutoStories,
                                    label = "Библия",
                                    isSelected = currentSection == "bible",
                                    onClick = { onSectionSelect("bible") }
                                )

                                Spacer(modifier = Modifier.weight(1f))

                                NavIcon(
                                    imageVector = Icons.Default.Settings,
                                    label = "Настройки",
                                    isSelected = currentSection == "settings",
                                    onClick = { onSectionSelect("settings") }
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(start = 0.dp, end = 16.dp, top = 20.dp, bottom = 16.dp)
                                .shadow(
                                    elevation = 12.dp,
                                    shape = RoundedCornerShape(16.dp),
                                    clip = false,
                                    ambientColor = Color.Black,
                                    spotColor = Color.Black
                                )
                                .clip(RoundedCornerShape(16.dp))
                                .background(SurfaceCard)
                                .border(0.5.dp, BorderLight, RoundedCornerShape(16.dp))
                        ) {
                            content(layoutType)
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        content(layoutType)
                    }
                }
            }
        }
    }
}
