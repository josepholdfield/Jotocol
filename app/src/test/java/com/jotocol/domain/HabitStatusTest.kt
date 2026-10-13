package com.jotocol.domain

import com.jotocol.support.at
import com.jotocol.support.creatine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HabitStatusTest {

    private val habit = creatine(time = "09:00", escalation = 30)

    @Test
    fun `pending before the scheduled time`() {
        assertEquals(HabitState.PENDING, HabitStatus.stateFor(habit, null, at("2026-10-13", "08:59")))
    }

    @Test
    fun `due from the scheduled time until one escalation interval has passed`() {
        assertEquals(HabitState.DUE, HabitStatus.stateFor(habit, null, at("2026-10-13", "09:00")))
        assertEquals(HabitState.DUE, HabitStatus.stateFor(habit, null, at("2026-10-13", "09:29")))
    }

    @Test
    fun `overdue once an escalation interval has elapsed`() {
        assertEquals(HabitState.OVERDUE, HabitStatus.stateFor(habit, null, at("2026-10-13", "09:30")))
        assertEquals(HabitState.OVERDUE, HabitStatus.stateFor(habit, null, at("2026-10-13", "23:59")))
    }

    @Test
    fun `completed today wins regardless of time`() {
        val completion = Completion(habit.id, LocalDate.parse("2026-10-13"), at("2026-10-13", "07:00").toInstant())
        assertEquals(HabitState.COMPLETED, HabitStatus.stateFor(habit, completion, at("2026-10-13", "07:01")))
        assertEquals(HabitState.COMPLETED, HabitStatus.stateFor(habit, completion, at("2026-10-13", "23:59")))
    }

    @Test
    fun `yesterday's completion does not carry over - daily reset`() {
        val yesterday = Completion(habit.id, LocalDate.parse("2026-10-12"), at("2026-10-12", "09:05").toInstant())
        assertEquals(HabitState.PENDING, HabitStatus.stateFor(habit, yesterday, at("2026-10-13", "08:00")))
        assertEquals(HabitState.DUE, HabitStatus.stateFor(habit, yesterday, at("2026-10-13", "09:05")))

        val today = HabitStatus.today(habit, yesterday, emptyList(), at("2026-10-13", "09:05"))
        assertEquals(null, today.completion)
        assertTrue(today.isOutstanding)
    }

    @Test
    fun `disabled habit is never outstanding`() {
        val disabled = habit.copy(enabled = false)
        assertEquals(HabitState.DISABLED, HabitStatus.stateFor(disabled, null, at("2026-10-13", "12:00")))
        assertFalse(HabitStatus.today(disabled, null, emptyList(), at("2026-10-13", "12:00")).isOutstanding)
    }

    @Test
    fun `reminders sent today are counted per habit and date`() {
        val logs = listOf(
            ReminderLog(habit.id, LocalDate.parse("2026-10-13"), at("2026-10-13", "09:00").toInstant()),
            ReminderLog(habit.id, LocalDate.parse("2026-10-13"), at("2026-10-13", "09:30").toInstant()),
            ReminderLog(habit.id, LocalDate.parse("2026-10-12"), at("2026-10-12", "09:00").toInstant()),
            ReminderLog(99, LocalDate.parse("2026-10-13"), at("2026-10-13", "09:00").toInstant()),
        )
        val today = HabitStatus.today(habit, null, logs, at("2026-10-13", "10:00"))
        assertEquals(2, today.remindersSentToday)
    }
}
