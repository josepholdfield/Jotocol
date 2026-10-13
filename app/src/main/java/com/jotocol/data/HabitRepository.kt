package com.jotocol.data

import com.jotocol.domain.Completion
import com.jotocol.domain.Habit
import com.jotocol.domain.ReminderLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/**
 * All persisted state is read and written through this interface by the UI, the reminder
 * scheduler and the NFC handler. [RoomHabitRepository] is the production implementation;
 * tests use an in-memory fake.
 */
interface HabitRepository {
    fun observeHabits(): Flow<List<Habit>>
    fun observeCompletions(date: LocalDate): Flow<List<Completion>>
    fun observeReminderLogs(date: LocalDate): Flow<List<ReminderLog>>
    suspend fun getHabits(): List<Habit>
    suspend fun getHabit(id: Long): Habit?
    suspend fun getHabitByNfcTag(tagId: String): Habit?
    suspend fun getCompletion(habitId: Long, date: LocalDate): Completion?
    suspend fun getCompletions(date: LocalDate): List<Completion>
    suspend fun getReminderLogs(date: LocalDate): List<ReminderLog>
    suspend fun insertHabit(habit: Habit): Long
    suspend fun updateHabit(habit: Habit)
    suspend fun deleteHabit(habit: Habit)
    suspend fun markCompleted(habitId: Long, date: LocalDate, at: Instant): Boolean
    suspend fun clearCompletion(habitId: Long, date: LocalDate)
    suspend fun recordReminderSent(habitId: Long, date: LocalDate, at: Instant)
    suspend fun pruneReminderLogs(olderThan: LocalDate)
    suspend fun seedIfEmpty()
}

class RoomHabitRepository(private val db: JotocolDatabase) : HabitRepository {

    override fun observeHabits(): Flow<List<Habit>> =
        db.habitDao().observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeCompletions(date: LocalDate): Flow<List<Completion>> =
        db.completionDao().observeForDate(date.toString()).map { list -> list.map { it.toDomain() } }

    override fun observeReminderLogs(date: LocalDate): Flow<List<ReminderLog>> =
        db.reminderLogDao().observeForDate(date.toString()).map { list -> list.map { it.toDomain() } }

    override suspend fun getHabits(): List<Habit> = db.habitDao().getAll().map { it.toDomain() }

    override suspend fun getHabit(id: Long): Habit? = db.habitDao().getById(id)?.toDomain()

    override suspend fun getHabitByNfcTag(tagId: String): Habit? = db.habitDao().getByNfcTag(tagId)?.toDomain()

    override suspend fun getCompletion(habitId: Long, date: LocalDate): Completion? =
        db.completionDao().get(habitId, date.toString())?.toDomain()

    override suspend fun getCompletions(date: LocalDate): List<Completion> =
        db.completionDao().getForDate(date.toString()).map { it.toDomain() }

    override suspend fun getReminderLogs(date: LocalDate): List<ReminderLog> =
        db.reminderLogDao().getForDate(date.toString()).map { it.toDomain() }

    override suspend fun insertHabit(habit: Habit): Long = db.habitDao().insert(HabitEntity.fromDomain(habit))

    override suspend fun updateHabit(habit: Habit) = db.habitDao().update(HabitEntity.fromDomain(habit))

    override suspend fun deleteHabit(habit: Habit) {
        db.completionDao().deleteAllForHabit(habit.id)
        db.reminderLogDao().deleteAllForHabit(habit.id)
        db.habitDao().delete(HabitEntity.fromDomain(habit))
    }

    override suspend fun markCompleted(habitId: Long, date: LocalDate, at: Instant): Boolean {
        val rowId = db.completionDao().insert(
            CompletionEntity.fromDomain(Completion(habitId, date, at)),
        )
        return rowId != -1L
    }

    override suspend fun clearCompletion(habitId: Long, date: LocalDate) =
        db.completionDao().delete(habitId, date.toString())

    override suspend fun recordReminderSent(habitId: Long, date: LocalDate, at: Instant) =
        db.reminderLogDao().insert(ReminderLogEntity.fromDomain(ReminderLog(habitId, date, at)))

    override suspend fun pruneReminderLogs(olderThan: LocalDate) = db.reminderLogDao().prune(olderThan.toString())

    override suspend fun seedIfEmpty() {
        if (db.habitDao().count() == 0) {
            insertHabit(
                Habit(
                    name = "Take creatine",
                    reminderTime = LocalTime.of(9, 0),
                    escalationMinutes = Habit.DEFAULT_ESCALATION_MINUTES,
                ),
            )
        }
    }
}
