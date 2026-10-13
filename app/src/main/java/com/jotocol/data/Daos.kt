package com.jotocol.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits ORDER BY reminderTimeSeconds, id")
    fun observeAll(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits ORDER BY reminderTimeSeconds, id")
    suspend fun getAll(): List<HabitEntity>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getById(id: Long): HabitEntity?

    @Query("SELECT * FROM habits WHERE nfcTagId = :tagId LIMIT 1")
    suspend fun getByNfcTag(tagId: String): HabitEntity?

    @Query("SELECT COUNT(*) FROM habits")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(habit: HabitEntity): Long

    @Update
    suspend fun update(habit: HabitEntity)

    @Delete
    suspend fun delete(habit: HabitEntity)
}

@Dao
interface CompletionDao {
    @Query("SELECT * FROM completions WHERE date = :date")
    fun observeForDate(date: String): Flow<List<CompletionEntity>>

    @Query("SELECT * FROM completions WHERE habitId = :habitId AND date = :date")
    suspend fun get(habitId: Long, date: String): CompletionEntity?

    @Query("SELECT * FROM completions WHERE date = :date")
    suspend fun getForDate(date: String): List<CompletionEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(completion: CompletionEntity): Long

    @Query("DELETE FROM completions WHERE habitId = :habitId AND date = :date")
    suspend fun delete(habitId: Long, date: String)

    @Query("DELETE FROM completions WHERE habitId = :habitId")
    suspend fun deleteAllForHabit(habitId: Long)
}

@Dao
interface ReminderLogDao {
    @Query("SELECT * FROM reminder_log WHERE date = :date")
    fun observeForDate(date: String): Flow<List<ReminderLogEntity>>

    @Query("SELECT * FROM reminder_log WHERE date = :date")
    suspend fun getForDate(date: String): List<ReminderLogEntity>

    @Insert
    suspend fun insert(log: ReminderLogEntity)

    @Query("DELETE FROM reminder_log WHERE habitId = :habitId")
    suspend fun deleteAllForHabit(habitId: Long)

    @Query("DELETE FROM reminder_log WHERE date < :olderThanDate")
    suspend fun prune(olderThanDate: String)
}
