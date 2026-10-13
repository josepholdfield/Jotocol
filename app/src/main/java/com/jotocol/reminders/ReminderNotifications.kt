package com.jotocol.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.jotocol.R
import com.jotocol.domain.Habit
import com.jotocol.ui.MainActivity

/** Posting side of reminders. Notifications are a view of state, never the state. */
interface ReminderNotifier {
    fun hasPermission(): Boolean
    /** Posts (or re-posts, re-alerting) the reminder for [habit]. */
    fun showReminder(habit: Habit, remindersSentToday: Int)
    fun cancelReminder(habitId: Long)
}

class ReminderNotifications(private val context: Context) : ReminderNotifier {

    fun ensureChannel() {
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.channel_reminders),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.channel_reminders_description)
            enableVibration(true)
            vibrationPattern = REMINDER_VIBRATION
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    override fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    override fun showReminder(habit: Habit, remindersSentToday: Int) {
        if (!hasPermission()) return
        ensureChannel()

        val openIntent = PendingIntent.getActivity(
            context,
            habit.id.toInt(),
            MainActivity.openHabitIntent(context, habit.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val doneIntent = PendingIntent.getBroadcast(
            context,
            habit.id.toInt(),
            CompleteActionReceiver.intent(context, habit.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val text = if (remindersSentToday <= 1) {
            context.getString(R.string.reminder_text_due)
        } else {
            context.getString(R.string.reminder_text_overdue, remindersSentToday)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(habit.name)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVibrate(REMINDER_VIBRATION)
            .setLocalOnly(true)
            .setAutoCancel(true)
            .setOnlyAlertOnce(false)
            .setContentIntent(openIntent)
            .addAction(R.drawable.ic_notification, context.getString(R.string.action_done), doneIntent)
            .addAction(R.drawable.ic_notification, context.getString(R.string.action_open), openIntent)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId(habit.id), notification)
    }

    override fun cancelReminder(habitId: Long) {
        NotificationManagerCompat.from(context).cancel(notificationId(habitId))
    }

    private fun notificationId(habitId: Long) = NOTIFICATION_ID_BASE + habitId.toInt()

    companion object {
        const val CHANNEL_ID = "habit_reminders"
        private const val NOTIFICATION_ID_BASE = 1000
        /** Distinctive triple pulse: short, short, long. */
        val REMINDER_VIBRATION = longArrayOf(0, 150, 100, 150, 100, 450)
    }
}
