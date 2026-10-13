package com.jotocol.domain

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/**
 * A daily habit definition. Completion for a given day lives in [Completion]; the current
 * state of a habit is always derived (see [HabitStatus]) rather than stored.
 */
data class Habit(
    val id: Long = 0,
    val name: String,
    val enabled: Boolean = true,
    val reminderTime: LocalTime,
    /** Minutes between repeat reminders while the habit stays incomplete. */
    val escalationMinutes: Int = DEFAULT_ESCALATION_MINUTES,
    /** When false the habit is tracked but never notifies. */
    val remindersEnabled: Boolean = true,
    /** Hex string identifier of a registered NFC tag, or null if none. */
    val nfcTagId: String? = null,
) {
    companion object {
        const val DEFAULT_ESCALATION_MINUTES = 30
        const val MIN_ESCALATION_MINUTES = 5
        val ESCALATION_CHOICES = listOf(5, 10, 15, 30, 60, 120)
    }
}

/** A record that [habitId] was completed on local calendar [date]. */
data class Completion(
    val habitId: Long,
    val date: LocalDate,
    val completedAt: Instant,
)

/** A record that a reminder was delivered for [habitId] on local calendar [date]. */
data class ReminderLog(
    val habitId: Long,
    val date: LocalDate,
    val sentAt: Instant,
)

enum class HabitState {
    /** Scheduled time has not yet arrived today. */
    PENDING,
    /** Scheduled time has passed, within the first escalation interval. */
    DUE,
    /** Still incomplete after at least one escalation interval. */
    OVERDUE,
    /** Completed today. */
    COMPLETED,
    /** Habit is disabled: never due, never reminded. */
    DISABLED,
}

/** A habit together with its derived state for a particular day. */
data class HabitToday(
    val habit: Habit,
    val date: LocalDate,
    val state: HabitState,
    val completion: Completion?,
    val remindersSentToday: Int,
) {
    val isOutstanding: Boolean
        get() = state == HabitState.PENDING || state == HabitState.DUE || state == HabitState.OVERDUE
}
