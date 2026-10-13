package com.jotocol.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [HabitEntity::class, CompletionEntity::class, ReminderLogEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class JotocolDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun completionDao(): CompletionDao
    abstract fun reminderLogDao(): ReminderLogDao

    companion object {
        fun create(context: Context): JotocolDatabase =
            Room.databaseBuilder(context.applicationContext, JotocolDatabase::class.java, "jotocol.db")
                .build()
    }
}
