@file:OptIn(ExperimentalTime::class)

package org.mosyagin.project.util

import kotlinx.datetime.TimeZone
import kotlinx.datetime.LocalDate

import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

fun currentDate(): LocalDate {
    return Clock.System.todayIn(TimeZone.currentSystemDefault())
}

fun currentTimestamp(): Long {
    return Clock.System.now().toEpochMilliseconds()
}
