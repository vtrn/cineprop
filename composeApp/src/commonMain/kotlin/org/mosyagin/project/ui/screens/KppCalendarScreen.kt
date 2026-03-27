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
import kotlinx.datetime.*
import kotlinx.datetime.LocalDate
import org.koin.compose.koinInject
import org.mosyagin.project.Shift
import org.mosyagin.project.repository.ShiftRepository
import org.mosyagin.project.ui.components.CineCard
import kotlin.time.Clock

data class KppCalendarScreen(val projectId: Long) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val shiftRepository = koinInject<ShiftRepository>()
        val shifts by shiftRepository.getShiftsByProject(projectId).collectAsState(initial = emptyList())

        // 1. Инициализируем стейт первым числом текущего месяца.
        val initialDate = remember {
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            LocalDate(today.year, today.month, 1)
        }

        var currentMonthDate by remember { mutableStateOf(initialDate) }

        // 2. Оптимизация: парсим даты смен только тогда, когда меняется список shifts.
        val parsedShiftsWithDates = remember(shifts) {
            shifts.mapNotNull { shift ->
                val date = parseShiftDate(shift.date)
                if (date != null) shift to date else null
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
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Unspecified,
                        navigationIconContentColor = Color.Unspecified,
                        titleContentColor = Color.Unspecified,
                        actionIconContentColor = Color.Unspecified
                    )
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
                    parsedShifts = parsedShiftsWithDates,
                    currentDate = currentMonthDate,
                    onMonthChange = { currentMonthDate = it }
                )

                Spacer(Modifier.height(24.dp))

                Text(
                    text = "Смены в этом месяце",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                val monthlyShifts = remember(parsedShiftsWithDates, currentMonthDate) {
                    parsedShiftsWithDates.filter { (_, date) ->
                        date.month == currentMonthDate.month && date.year == currentMonthDate.year
                    }.sortedBy { it.second }
                }

                if (monthlyShifts.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)) {
                        Text(
                            "Нет запланированных смен",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 32.dp)
                    ) {
                        items(monthlyShifts) { (shift, _) ->
                            ShiftListItem(shift)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun CalendarGrid(
        parsedShifts: List<Pair<Shift, LocalDate>>,
        currentDate: LocalDate,
        onMonthChange: (LocalDate) -> Unit
    ) {
        val firstDayOfMonth = LocalDate(currentDate.year, currentDate.month, 1)
        val daysInMonth = getDaysInMonth(currentDate.month, currentDate.year)
        val firstDayOfWeek = firstDayOfMonth.dayOfWeek.isoDayNumber

        // Оптимизация: предвычисляем дни со сменами для текущего месяца
        val shiftDays = remember(parsedShifts, currentDate.month, currentDate.year) {
            parsedShifts
                .filter { (_, date) ->
                    date.month == currentDate.month && date.year == currentDate.year
                }
                .map { (_, date) -> date.day }
                .toSet()
        }

        CineCard(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)) {
            Column(modifier = Modifier.padding(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${getRussianMonthName(currentDate.month)} ${currentDate.year}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Row {
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
                                    val hasShift = day in shiftDays  // O(1) проверка

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
            Row(
                modifier = Modifier.padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Смена №${shift.shiftNumber}", fontWeight = FontWeight.Bold)
                    Text(text = shift.date, style = MaterialTheme.typography.bodySmall)
                }
                Surface(
                    color = Color(0xFFB4E6B2).copy(alpha = 0.2f),
                    shape = CircleShape
                ) {
                    Text(
                        text = "Запланировано",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF2E7D32),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }

    private fun parseShiftDate(dateStr: String): LocalDate? {
        return try {
            val parts = dateStr.split(".").map { it.trim() }
            if (parts.size == 3) {
                LocalDate(parts[2].toInt(), parts[1].toInt(), parts[0].toInt())
            } else null
        } catch (_: Exception) { null }
    }

    private fun getDaysInMonth(month: Month, year: Int): Int {
        return when (month) {
            Month.FEBRUARY -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
            Month.APRIL, Month.JUNE, Month.SEPTEMBER, Month.NOVEMBER -> 30
            else -> 31
        }
    }

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
        }
    }
}