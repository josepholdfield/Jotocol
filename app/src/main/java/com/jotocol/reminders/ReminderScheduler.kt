package com.jotocol.reminders

import com.jotocol.data.HabitRepository
import com.jotocol.domain.Habit
import com.jotocol.domain.ReminderPlanner
import java.time.Clock
import java.time.ZonedDateTime

/**
 * Keeps exactly one pending alarm per habit in sync with persisted state. Every entry point
 * (app start, boot, completion, edit, alarm delivery) calls [reconcile] or [reconcileAll],
 * which is idempotent: the alarm for a habit is always replaced or cancelled, never added.
 */
class ReminderScheduler(
    private val repository: HabitRepository,
    private val alarms: AlarmPort,
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    val isExact: Boolean get() = alarms.canScheduleExact

    suspend fun reconcileAll() {
        repository.getHabits().forEach { reconcile(it) }
    }

    suspend fun reconcile(habitId: Long) {
        val habit = repository.getHabit(habitId)
        if (habit == null) {
            alarms.cancel(habitId)
            return
        }
        reconcile(habit)
    }

    suspend fun reconcile(habit: Habit) {
        val now = ZonedDateTime.now(clock)
        val today = now.toLocalDate()
        val next = ReminderPlanner.nextReminderAt(
            habit = habit,
            completion = repository.getCompletion(habit.id, today),
            reminderLogs = repository.getReminderLogs(today),
            now = now,
        )
        if (next == null) {
            alarms.cancel(habit.id)
        } else {
            alarms.set(habit.id, next, exact = alarms.canScheduleExact)
        }
    }

    fun cancel(habitId: Long) = alarms.cancel(habitId)
}
