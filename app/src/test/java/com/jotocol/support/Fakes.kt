package com.jotocol.support

import com.jotocol.data.HabitRepository
import com.jotocol.domain.Completion
import com.jotocol.domain.Habit
import com.jotocol.domain.ReminderLog
import com.jotocol.reminders.AlarmPort
import com.jotocol.reminders.Haptics
import com.jotocol.reminders.ReminderNotifier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

val ZONE: ZoneId = ZoneId.of("Europe/London")

fun at(date: String, time: String): ZonedDateTime =
    ZonedDateTime.of(LocalDate.parse(date), LocalTime.parse(time), ZONE)

fun fixedClock(now: ZonedDateTime): Clock = Clock.fixed(now.toInstant(), ZONE)

/** A clock whose "now" can be moved forward by tests. */
class MutableClock(var now: ZonedDateTime) : Clock() {
    override fun getZone(): ZoneId = now.zone
    override fun withZone(zone: ZoneId): Clock = MutableClock(now.withZoneSameInstant(zone))
    override fun instant(): Instant = now.toInstant()
}

fun creatine(
    id: Long = 1,
    time: String = "09:00",
    escalation: Int = 30,
    enabled: Boolean = true,
    remindersEnabled: Boolean = true,
    nfcTagId: String? = null,
) = Habit(
    id = id,
    name = "Take creatine",
    enabled = enabled,
    reminderTime = LocalTime.parse(time),
    escalationMinutes = escalation,
    remindersEnabled = remindersEnabled,
    nfcTagId = nfcTagId,
)

class FakeRepository : HabitRepository {
    private val habits = MutableStateFlow<List<Habit>>(emptyList())
    private val completions = MutableStateFlow<List<Completion>>(emptyList())
    private val logs = MutableStateFlow<List<ReminderLog>>(emptyList())
    private var nextId = 1L

    fun seed(vararg list: Habit) {
        habits.value = list.toList()
        nextId = (list.maxOfOrNull { it.id } ?: 0L) + 1
    }

    val allCompletions: List<Completion> get() = completions.value
    val allLogs: List<ReminderLog> get() = logs.value

    override fun observeHabits(): Flow<List<Habit>> = habits
    override fun observeCompletions(date: LocalDate): Flow<List<Completion>> =
        completions.map { l -> l.filter { it.date == date } }
    override fun observeReminderLogs(date: LocalDate): Flow<List<ReminderLog>> =
        logs.map { l -> l.filter { it.date == date } }

    override suspend fun getHabits(): List<Habit> = habits.value
    override suspend fun getHabit(id: Long): Habit? = habits.value.firstOrNull { it.id == id }
    override suspend fun getHabitByNfcTag(tagId: String): Habit? = habits.value.firstOrNull { it.nfcTagId == tagId }
    override suspend fun getCompletion(habitId: Long, date: LocalDate): Completion? =
        completions.value.firstOrNull { it.habitId == habitId && it.date == date }
    override suspend fun getCompletions(date: LocalDate): List<Completion> = completions.value.filter { it.date == date }
    override suspend fun getReminderLogs(date: LocalDate): List<ReminderLog> = logs.value.filter { it.date == date }

    override suspend fun insertHabit(habit: Habit): Long {
        val id = nextId++
        habits.value = habits.value + habit.copy(id = id)
        return id
    }

    override suspend fun updateHabit(habit: Habit) {
        habits.value = habits.value.map { if (it.id == habit.id) habit else it }
    }

    override suspend fun deleteHabit(habit: Habit) {
        habits.value = habits.value.filterNot { it.id == habit.id }
        completions.value = completions.value.filterNot { it.habitId == habit.id }
        logs.value = logs.value.filterNot { it.habitId == habit.id }
    }

    override suspend fun markCompleted(habitId: Long, date: LocalDate, at: Instant): Boolean {
        if (getCompletion(habitId, date) != null) return false
        completions.value = completions.value + Completion(habitId, date, at)
        return true
    }

    override suspend fun clearCompletion(habitId: Long, date: LocalDate) {
        completions.value = completions.value.filterNot { it.habitId == habitId && it.date == date }
    }

    override suspend fun recordReminderSent(habitId: Long, date: LocalDate, at: Instant) {
        logs.value = logs.value + ReminderLog(habitId, date, at)
    }

    override suspend fun pruneReminderLogs(olderThan: LocalDate) {
        logs.value = logs.value.filterNot { it.date.isBefore(olderThan) }
    }

    override suspend fun seedIfEmpty() {
        if (habits.value.isEmpty()) insertHabit(creatine(id = 0))
    }
}

class FakeAlarms(override var canScheduleExact: Boolean = true) : AlarmPort {
    val scheduled = linkedMapOf<Long, Pair<Instant, Boolean>>()
    val setCalls = mutableListOf<Long>()
    val cancelCalls = mutableListOf<Long>()

    override fun set(habitId: Long, at: Instant, exact: Boolean) {
        setCalls += habitId
        scheduled[habitId] = at to exact
    }

    override fun cancel(habitId: Long) {
        cancelCalls += habitId
        scheduled.remove(habitId)
    }
}

class FakeNotifier : ReminderNotifier {
    val shown = mutableListOf<Pair<Habit, Int>>()
    val cancelled = mutableListOf<Long>()
    override fun hasPermission() = true
    override fun showReminder(habit: Habit, remindersSentToday: Int) {
        shown += habit to remindersSentToday
    }
    override fun cancelReminder(habitId: Long) {
        cancelled += habitId
    }
}

class FakeHaptics : Haptics {
    var successes = 0
    var warnings = 0
    override fun success() { successes++ }
    override fun warning() { warnings++ }
}
