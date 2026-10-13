package com.jotocol.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jotocol.JotocolApplication
import com.jotocol.domain.ReminderPlanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

/**
 * Fired by AlarmManager when a habit's reminder is due. Re-derives state from the database
 * before showing anything, records that the reminder was issued, and schedules the next one.
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getLongExtra(EXTRA_HABIT_ID, -1L)
        if (habitId < 0) return
        val graph = JotocolApplication.graph(context)
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val now = ZonedDateTime.now(graph.clock)
                val today = now.toLocalDate()
                val habit = graph.repository.getHabit(habitId)
                if (habit == null) {
                    graph.scheduler.cancel(habitId)
                    return@launch
                }
                val completion = graph.repository.getCompletion(habitId, today)
                if (ReminderPlanner.shouldDeliver(habit, completion, now)) {
                    graph.repository.recordReminderSent(habitId, today, now.toInstant())
                    val sentToday = graph.repository.getReminderLogs(today).count { it.habitId == habitId }
                    graph.notifications.showReminder(habit, sentToday)
                }
                graph.scheduler.reconcile(habit)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val EXTRA_HABIT_ID = "habit_id"

        fun intent(context: Context, habitId: Long): Intent =
            Intent(context, ReminderReceiver::class.java)
                .setAction("com.jotocol.action.REMIND")
                .putExtra(EXTRA_HABIT_ID, habitId)
    }
}
