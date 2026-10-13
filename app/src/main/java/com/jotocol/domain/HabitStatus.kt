package com.jotocol.domain

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Pure derivation of a habit's state for a day from the schedule, the clock and the completion
 * record. Notifications are never the source of truth.
 */
object HabitStatus {

    fun stateFor(
        habit: Habit,
        completion: Completion?,
        now: ZonedDateTime,
    ): HabitState {
        val today = now.toLocalDate()
        if (!habit.enabled) return HabitState.DISABLED
        if (completion != null && completion.date == today) return HabitState.COMPLETED
        val dueAt = habit.dueAt(today, now.zone)
        if (now.isBefore(dueAt)) return HabitState.PENDING
        val overdueAt = dueAt.plusMinutes(habit.escalationMinutes.toLong())
        return if (now.isBefore(overdueAt)) HabitState.DUE else HabitState.OVERDUE
    }

    fun today(
        habit: Habit,
        completion: Completion?,
        reminderLogs: List<ReminderLog>,
        now: ZonedDateTime,
    ): HabitToday {
        val today = now.toLocalDate()
        val todaysCompletion = completion?.takeIf { it.date == today }
        return HabitToday(
            habit = habit,
            date = today,
            state = stateFor(habit, todaysCompletion, now),
            completion = todaysCompletion,
            remindersSentToday = reminderLogs.count { it.habitId == habit.id && it.date == today },
        )
    }
}

/** The moment this habit becomes due on [date] in [zone]. */
fun Habit.dueAt(date: LocalDate, zone: ZoneId): ZonedDateTime =
    ZonedDateTime.of(date, reminderTime, zone)
