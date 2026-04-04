package org.mosyagin.project.ui.components.props

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import org.mosyagin.project.models.versioning.PropStatus

/**
 * Общие утилиты для визуального отображения реквизита.
 * Содержит логику выбора цветов и иконок на основе категорий и статусов.
 */
object PropUiUtils {
    /**
     * Возвращает характерный цвет для каждой категории реквизита.
     * Используется для цветового кодирования в списках и деталях.
     */
    fun getCategoryColor(category: String): Color {
        return when (category.lowercase()) {
            "персонажный" -> Color(0xFFFF2D55) // Розовый/Красный
            "транспорт" -> Color(0xFFFF9500)   // Оранжевый
            "типографика" -> Color(0xFF34C759) // Зеленый
            "графика" -> Color(0xFF5856D6)    // Фиолетовый
            "исходящий" -> Color(0xFFFF3B30)   // Красный
            "животные" -> Color(0xFFA2845E)    // Коричневый
            "оружие" -> Color(0xFF8E8E93)      // Серый
            "прочее" -> Color(0xFF5AC8FA)      // Голубой
            else -> Color(0xFF8E8E93)
        }
    }

    /**
     * Возвращает иконку Material Design для категории.
     * Оружие теперь отображается иконкой Shield (Щит).
     */
    fun getCategoryIcon(category: String): androidx.compose.ui.graphics.vector.ImageVector {
        return when (category.lowercase()) {
            "персонажный" -> Icons.Default.Person
            "транспорт" -> Icons.Default.DirectionsCar
            "типографика" -> Icons.Default.TextFields
            "графика" -> Icons.Default.Brush
            "исходящий" -> Icons.Default.Restaurant
            "животные" -> Icons.Default.Pets
            "оружие" -> Icons.Default.Shield
            else -> Icons.Default.Inventory2
        }
    }

    /**
     * Возвращает цвет, соответствующий текущему статусу готовности реквизита.
     */
    fun getStatusColor(statusStr: String): Color {
        return when (PropStatus.fromString(statusStr)) {
            PropStatus.PLANNED -> Color(0xFFFF9500) // В планах (Оранжевый)
            PropStatus.BOUGHT -> Color(0xFF5AC8FA)  // Куплен (Голубой)
            PropStatus.READY -> Color(0xFF34C759)   // Готов (Зеленый)
            PropStatus.LOST -> Color(0xFFFF3B30)    // Утерян (Красный)
        }
    }
}
