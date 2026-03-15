package org.mosyagin.project.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

@Composable
fun CinePropTheme(content: @Composable () -> Unit) {
    val colors = darkColorScheme(
        primary = CinematicOrange,
        background = DarkBackground,
        surface = DarkSurface,
        onPrimary = Color.Black
    )

    MaterialTheme(
        colorScheme = colors,
        typography = Typography(
            bodyMedium = TextStyle(fontFamily = getInterFontFamily()),
            titleLarge = TextStyle(fontFamily = getInterFontFamily(), fontWeight = FontWeight.Bold)
        ),
        content = content
    )
}
