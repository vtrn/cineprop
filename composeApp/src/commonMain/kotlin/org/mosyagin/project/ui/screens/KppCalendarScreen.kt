package org.mosyagin.project.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.Month
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.isoDayNumber // Важный импорт!
import org.koin.compose.koinInject
import org.mosyagin.project.Shift
import org.mosyagin.project.repository.ShiftRepository
import org.mosyagin.project.ui.components.CineCard

data class KppCalendarScreen(val projectId: Long) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val shiftRepository = koinInject<ShiftRepository>()
        val shifts by shiftRepository.getShiftsByProject(projectId).collectAsState(initial = emptyList())

        val today = remember {
            Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        }

        var currentMonth by remember { mutableStateOf(today) }

        LaunchedEffect(shifts) {
            if (shifts.isNotEmpty()) {
                val firstShiftDate = shifts.mapNotNull { parseShiftDate(it.date) }.minByOrNull { it }
                if (firstShiftDate != null && currentMonth == today) {
                    currentMonth = firstShiftDate
                }
            }
        }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Календарь смен", style = MaterialTheme.typography.titleLarge) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp)
            ) {
                CalendarGrid(
                    shifts = shifts,
                    currentDate = currentMonth,
                    onMonthChange = { currentMonth = it }
                )

                Spacer(Modifier.height(24.dp))

                Text(
                    text = "Смены в этом месяце",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                val monthlyShifts = shifts.filter { shift ->
                    val d = parseShiftDate(shift.date)
                    d?.month == currentMonth.month && d.year == currentMonth.year
                }

                if (monthlyShifts.isEmpty()) {
                    Text(
                        "Нет запланированных смен",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 32.dp)
                    ) {
                        items(monthlyShifts.sortedBy { parseShiftDate(it.date) }) { shift ->
                            ShiftListItem(shift)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun CalendarGrid(
        shifts: List<Shift>,
        currentDate: LocalDate,
        onMonthChange: (LocalDate) -> Unit
    ) {
        val firstDayOfMonth = LocalDate(currentDate.year, currentDate.month, 1)
        val daysInMonth = getDaysInMonth(currentDate.month, currentDate.year)

        // ИСПРАВЛЕНИЕ ЗДЕСЬ: используем isoDayNumber вместо value
        val firstDayOfWeek = firstDayOfMonth.dayOfWeek.isoDayNumber

        CineCard(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // УЛУЧШЕНИЕ: Красивое название месяца на русском
                    val monthName = getRussianMonthName(currentDate.month)

                    Text(
                        text = "$monthName ${currentDate.year}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Row {
                        // ИСПРАВЛЕНИЕ ЗДЕСЬ: Безопасное использование DatePeriod для месяцев
                        IconButton(onClick = { onMonthChange(currentDate.minus(DatePeriod(months = 1))) }) {
                            Icon(Icons.Default.ChevronLeft, null)
                        }
                        IconButton(onClick = { onMonthChange(currentDate.plus(DatePeriod(months = 1))) }) {
                            Icon(Icons.Default.ChevronRight, null)
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс").forEach { day ->
                        Text(
                            text = day,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                val days = (1 until firstDayOfWeek).map { null } + (1..daysInMonth).map { it }
                val rows = days.chunked(7)

                rows.forEach { weekDays ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        weekDays.forEach { day ->
                            Box(
                                modifier = Modifier.weight(1f).aspectRatio(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                if (day != null) {
                                    val hasShift = shifts.any { shift ->
                                        val d = parseShiftDate(shift.date)
                                        d != null && d.dayOfMonth == day && d.month == currentDate.month && d.year == currentDate.year
                                    }

                                    if (hasShift) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = day.toString(),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = day.toString(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }
                        // Заполнение пустых ячеек в конце месяца
                        if (weekDays.size < 7) {
                            repeat(7 - weekDays.size) { Spacer(modifier = Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun ShiftListItem(shift: Shift) {
        CineCard(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Смена №${shift.shiftNumber}", fontWeight = FontWeight.Bold)
                    Text(text = shift.date, style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    text = "Запланировано",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFB4E6B2),
                    modifier = Modifier
                        .background(Color(0xFFB4E6B2).copy(alpha = 0.1f), CircleShape)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }

    private fun parseShiftDate(dateStr: String): LocalDate? {
        return try {
            val parts = dateStr.split(".").map { it.trim() }
            if (parts.size == 3) {
                LocalDate(parts[2].toInt(), parts[1].toInt(), parts[0].toInt())
            } else null
        } catch (e: Exception) { null }
    }

    private fun getDaysInMonth(month: Month, year: Int): Int {
        return when (month) {
            Month.FEBRUARY -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
            Month.APRIL, Month.JUNE, Month.SEPTEMBER, Month.NOVEMBER -> 30
            else -> 31
        }
    }

    // Хелпер для красивого вывода месяцев
    private fun getRussianMonthName(month: Month): String {
        return when (month) {
            Month.JANUARY -> "Январь"
            Month.FEBRUARY -> "Февраль"
            Month.MARCH -> "Март"
            Month.APRIL -> "Апрель"
            Month.MAY -> "Май"
            Month.JUNE -> "Июнь"
            Month.JULY -> "Июль"
            Month.AUGUST -> "Август"
            Month.SEPTEMBER -> "Сентябрь"
            Month.OCTOBER -> "Октябрь"
            Month.NOVEMBER -> "Ноябрь"
            Month.DECEMBER -> "Декабрь"
            else -> month.name
        }
    }
}
