package com.jotocol.reminders

import com.jotocol.data.HabitRepository
import com.jotocol.domain.Habit
import java.time.Clock
import java.time.ZonedDateTime

/**
 * The write-side use cases shared by the UI, the notification action and the NFC handler.
 * Every mutation ends by reconciling the habit's alarm so scheduled reminders always reflect
 * persisted state.
 */
class HabitActions(
    private val repository: HabitRepository,
    private val scheduler: ReminderScheduler,
    private val notifications: ReminderNotifier,
    private val haptics: Haptics,
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    /** Marks the habit complete for today. Returns true if this call changed state. */
    suspend fun complete(habitId: Long): Boolean {
        val now = ZonedDateTime.now(clock)
        val changed = repository.markCompleted(habitId, now.toLocalDate(), now.toInstant())
        notifications.cancelReminder(habitId)
        scheduler.reconcile(habitId)
        if (changed) haptics.success()
        return changed
    }

    /** Reverts today's completion (accidental tap). */
    suspend fun uncomplete(habitId: Long) {
        repository.clearCompletion(habitId, ZonedDateTime.now(clock).toLocalDate())
        scheduler.reconcile(habitId)
    }

    suspend fun save(habit: Habit): Long {
        val id = if (habit.id == 0L) repository.insertHabit(habit) else {
            repository.updateHabit(habit)
            habit.id
        }
        if (!habit.enabled || !habit.remindersEnabled) notifications.cancelReminder(id)
        scheduler.reconcile(id)
        return id
    }

    suspend fun delete(habit: Habit) {
        repository.deleteHabit(habit)
        notifications.cancelReminder(habit.id)
        scheduler.cancel(habit.id)
    }

    /** Full recovery pass: seed, prune old logs, and reconcile every alarm. */
    suspend fun recover() {
        repository.seedIfEmpty()
        repository.pruneReminderLogs(ZonedDateTime.now(clock).toLocalDate().minusDays(7))
        scheduler.reconcileAll()
    }
}
