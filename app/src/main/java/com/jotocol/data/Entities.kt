package com.jotocol.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.jotocol.domain.Completion
import com.jotocol.domain.Habit
import com.jotocol.domain.ReminderLog
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val enabled: Boolean,
    /** Seconds since midnight, local time. */
    val reminderTimeSeconds: Int,
    val escalationMinutes: Int,
    val remindersEnabled: Boolean,
    val nfcTagId: String?,
) {
    fun toDomain() = Habit(
        id = id,
        name = name,
        enabled = enabled,
        reminderTime = LocalTime.ofSecondOfDay(reminderTimeSeconds.toLong()),
        escalationMinutes = escalationMinutes,
        remindersEnabled = remindersEnabled,
        nfcTagId = nfcTagId,
    )

    companion object {
        fun fromDomain(habit: Habit) = HabitEntity(
            id = habit.id,
            name = habit.name,
            enabled = habit.enabled,
            reminderTimeSeconds = habit.reminderTime.toSecondOfDay(),
            escalationMinutes = habit.escalationMinutes,
            remindersEnabled = habit.remindersEnabled,
            nfcTagId = habit.nfcTagId,
        )
    }
}

/** One row per habit per local calendar date; the primary key enforces idempotent completion. */
@Entity(tableName = "completions", primaryKeys = ["habitId", "date"])
data class CompletionEntity(
    val habitId: Long,
    /** ISO-8601 local date, e.g. 2026-10-13. */
    val date: String,
    val completedAtEpochMillis: Long,
) {
    fun toDomain() = Completion(
        habitId = habitId,
        date = LocalDate.parse(date),
        completedAt = Instant.ofEpochMilli(completedAtEpochMillis),
    )

    companion object {
        fun fromDomain(c: Completion) = CompletionEntity(
            habitId = c.habitId,
            date = c.date.toString(),
            completedAtEpochMillis = c.completedAt.toEpochMilli(),
        )
    }
}

@Entity(tableName = "reminder_log", indices = [Index("habitId", "date")])
data class ReminderLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: Long,
    val date: String,
    val sentAtEpochMillis: Long,
) {
    fun toDomain() = ReminderLog(
        habitId = habitId,
        date = LocalDate.parse(date),
        sentAt = Instant.ofEpochMilli(sentAtEpochMillis),
    )

    companion object {
        fun fromDomain(r: ReminderLog) = ReminderLogEntity(
            habitId = r.habitId,
            date = r.date.toString(),
            sentAtEpochMillis = r.sentAt.toEpochMilli(),
        )
    }
}
