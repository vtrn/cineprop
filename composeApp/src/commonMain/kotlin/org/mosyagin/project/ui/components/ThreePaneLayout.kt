package org.mosyagin.project.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
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
    Row(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Левая панель (Master) - 25%
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(0.25f)
        ) {
            masterPane()
        }

        VerticalDivider(
            modifier = Modifier.fillMaxHeight(),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )

        // Центральная панель (Detail) - 50%
        // Оформлена как "парящая карточка"
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(0.50f)
                .padding(16.dp)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp)),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                shadowElevation = 4.dp
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    detailPane()
                }
            }
        }

        VerticalDivider(
            modifier = Modifier.fillMaxHeight(),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )

        // Правая панель (Inspector) - 25%
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(0.25f)
        ) {
            inspectorPane()
        }
    }
}
