package com.jotocol.domain

import java.time.Instant
import java.time.ZonedDateTime

/**
 * Decides when the next reminder for a habit should fire. Pure and deterministic so it can be
 * unit tested; the scheduler only translates the answer into a single alarm per habit.
 *
 * Rules:
 *  - Disabled habits or habits with reminders switched off: no reminder.
 *  - Completed today: next reminder is tomorrow at the scheduled time.
 *  - Not yet due today: today at the scheduled time.
 *  - Due or overdue with no reminder sent today: fire now (catch-up after a missed alarm).
 *  - Due or overdue with reminders already sent today: last sent + escalation interval
 *    (or now, if that moment has already passed). Escalation stops at midnight; the next
 *    reminder is then tomorrow's scheduled time.
 */
object ReminderPlanner {

    fun nextReminderAt(
        habit: Habit,
        completion: Completion?,
        reminderLogs: List<ReminderLog>,
        now: ZonedDateTime,
    ): Instant? {
        if (!habit.enabled || !habit.remindersEnabled) return null

        val today = now.toLocalDate()
        val zone = now.zone
        val tomorrowDue = habit.dueAt(today.plusDays(1), zone).toInstant()

        if (completion != null && completion.date == today) return tomorrowDue

        val todayDue = habit.dueAt(today, zone)
        if (now.isBefore(todayDue)) return todayDue.toInstant()

        val lastSentToday = reminderLogs
            .filter { it.habitId == habit.id && it.date == today }
            .maxOfOrNull { it.sentAt }
            ?: return now.toInstant()

        val next = lastSentToday.plusSeconds(habit.escalationMinutes * 60L)
        val candidate = if (next.isBefore(now.toInstant())) now.toInstant() else next
        val endOfDay = today.plusDays(1).atStartOfDay(zone).toInstant()
        return if (candidate.isBefore(endOfDay)) candidate else tomorrowDue
    }

    /**
     * Whether a reminder that has just fired should actually be shown. Guards against stale
     * alarms (the habit was completed or disabled after the alarm was set).
     */
    fun shouldDeliver(
        habit: Habit,
        completion: Completion?,
        now: ZonedDateTime,
    ): Boolean {
        if (!habit.enabled || !habit.remindersEnabled) return false
        val state = HabitStatus.stateFor(habit, completion, now)
        return state == HabitState.DUE || state == HabitState.OVERDUE
    }
}
