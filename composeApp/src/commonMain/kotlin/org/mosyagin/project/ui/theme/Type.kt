package org.mosyagin.project.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.Font
import cineapp.composeapp.generated.resources.Res
import cineapp.composeapp.generated.resources.Inter_Italic

@Composable
fun getInterFontFamily(): FontFamily {
    return FontFamily(
        Font(resource = Res.font.Inter_Italic, weight = FontWeight.Normal),
    )
}
