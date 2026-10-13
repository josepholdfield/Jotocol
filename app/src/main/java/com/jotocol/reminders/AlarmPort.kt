package com.jotocol.reminders

import java.time.Instant

/**
 * The minimal surface of AlarmManager that the scheduler needs. Abstracted so scheduling and
 * reconciliation logic can be unit tested without Android.
 */
interface AlarmPort {
    val canScheduleExact: Boolean

    /** Sets (or replaces) the single alarm for [habitId]. */
    fun set(habitId: Long, at: Instant, exact: Boolean)

    /** Cancels the alarm for [habitId], if any. */
    fun cancel(habitId: Long)
}
