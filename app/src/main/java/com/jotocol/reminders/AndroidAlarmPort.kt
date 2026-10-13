package com.jotocol.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.time.Instant

class AndroidAlarmPort(private val context: Context) : AlarmPort {

    private val alarmManager: AlarmManager
        get() = context.getSystemService(AlarmManager::class.java)

    override val canScheduleExact: Boolean
        get() = alarmManager.canScheduleExactAlarms()

    override fun set(habitId: Long, at: Instant, exact: Boolean) {
        val pi = pendingIntent(habitId)
        val triggerAt = at.toEpochMilli()
        if (exact) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } else {
            // Inexact fallback. Android clips windows below 10 minutes, so promise no better.
            alarmManager.setWindow(AlarmManager.RTC_WAKEUP, triggerAt, INEXACT_WINDOW_MILLIS, pi)
        }
    }

    override fun cancel(habitId: Long) {
        alarmManager.cancel(pendingIntent(habitId))
    }

    /**
     * One PendingIntent per habit, keyed by request code. Re-setting with FLAG_UPDATE_CURRENT
     * replaces the previous alarm, which is what prevents duplicate alarms on restart.
     */
    private fun pendingIntent(habitId: Long): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            habitId.toInt(),
            ReminderReceiver.intent(context, habitId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    companion object {
        const val INEXACT_WINDOW_MILLIS = 10 * 60 * 1000L
    }
}
