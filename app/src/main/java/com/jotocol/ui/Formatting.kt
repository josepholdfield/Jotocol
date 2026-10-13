package com.jotocol.ui

import android.content.Context
import com.jotocol.R
import com.jotocol.domain.HabitState
import com.jotocol.domain.HabitToday
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM")

fun LocalTime.display(): String = format(timeFormatter)

fun LocalDate.display(): String = format(dateFormatter)

fun Instant.displayTime(): String = atZone(ZoneId.systemDefault()).toLocalTime().display()

fun HabitToday.stateLabel(context: Context): String = when (state) {
    HabitState.PENDING -> context.getString(R.string.state_pending, habit.reminderTime.display())
    HabitState.DUE -> context.getString(R.string.state_due)
    HabitState.OVERDUE -> context.getString(R.string.state_overdue)
    HabitState.COMPLETED -> context.getString(
        R.string.state_completed,
        completion?.completedAt?.displayTime() ?: "",
    )
    HabitState.DISABLED -> context.getString(R.string.state_disabled)
}
