package org.mosyagin.project.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.Font
import cineapp.composeapp.generated.resources.Res
import cineapp.composeapp.generated.resources.*

@Composable
fun getIbmPlexMonoFontFamily(): FontFamily {
    return FontFamily(
        // Используем прямое обращение через Res.font
        Font(resource = Res.font.ibm_plex_mono_regular, weight = FontWeight.Normal),
        Font(resource = Res.font.ibm_plex_mono_bold, weight = FontWeight.Bold)
    )
}

// Базовая типографика
val baseline = Typography()

@Composable
fun getAppTypography(): Typography {
    val ibmPlexMono = getIbmPlexMonoFontFamily()
    
    return Typography(
        displayLarge = baseline.displayLarge.copy(fontFamily = ibmPlexMono),
        displayMedium = baseline.displayMedium.copy(fontFamily = ibmPlexMono),
        displaySmall = baseline.displaySmall.copy(fontFamily = ibmPlexMono),
        headlineLarge = baseline.headlineLarge.copy(fontFamily = ibmPlexMono),
        headlineMedium = baseline.headlineMedium.copy(fontFamily = ibmPlexMono),
        headlineSmall = baseline.headlineSmall.copy(fontFamily = ibmPlexMono),
        titleLarge = baseline.titleLarge.copy(fontFamily = ibmPlexMono),
        titleMedium = baseline.titleMedium.copy(fontFamily = ibmPlexMono),
        titleSmall = baseline.titleSmall.copy(fontFamily = ibmPlexMono),
        bodyLarge = baseline.bodyLarge.copy(fontFamily = ibmPlexMono),
        bodyMedium = baseline.bodyMedium.copy(fontFamily = ibmPlexMono),
        bodySmall = baseline.bodySmall.copy(fontFamily = ibmPlexMono),
        labelLarge = baseline.labelLarge.copy(fontFamily = ibmPlexMono),
        labelMedium = baseline.labelMedium.copy(fontFamily = ibmPlexMono),
        labelSmall = baseline.labelSmall.copy(fontFamily = ibmPlexMono),
    )
}
