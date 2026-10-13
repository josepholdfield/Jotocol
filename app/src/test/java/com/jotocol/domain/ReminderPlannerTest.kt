package com.jotocol.domain

import com.jotocol.support.at
import com.jotocol.support.creatine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ReminderPlannerTest {

    private val habit = creatine(time = "09:00", escalation = 30)
    private val today = LocalDate.parse("2026-10-13")

    private fun log(time: String, date: String = "2026-10-13") =
        ReminderLog(habit.id, LocalDate.parse(date), at(date, time).toInstant())

    @Test
    fun `before due time - first reminder is at the scheduled time today`() {
        val next = ReminderPlanner.nextReminderAt(habit, null, emptyList(), at("2026-10-13", "07:00"))
        assertEquals(at("2026-10-13", "09:00").toInstant(), next)
    }

    @Test
    fun `due with nothing sent today - fire immediately (catch-up)`() {
        val now = at("2026-10-13", "09:00")
        assertEquals(now.toInstant(), ReminderPlanner.nextReminderAt(habit, null, emptyList(), now))
        val late = at("2026-10-13", "14:12")
        assertEquals(late.toInstant(), ReminderPlanner.nextReminderAt(habit, null, emptyList(), late))
    }

    @Test
    fun `escalation - next reminder is last sent plus the interval`() {
        val next = ReminderPlanner.nextReminderAt(habit, null, listOf(log("09:00")), at("2026-10-13", "09:00"))
        assertEquals(at("2026-10-13", "09:30").toInstant(), next)

        val afterSecond = ReminderPlanner.nextReminderAt(
            habit, null, listOf(log("09:00"), log("09:30")), at("2026-10-13", "09:30"),
        )
        assertEquals(at("2026-10-13", "10:00").toInstant(), afterSecond)
    }

    @Test
    fun `escalation interval is configurable`() {
        val tenMinutes = habit.copy(escalationMinutes = 10)
        val next = ReminderPlanner.nextReminderAt(tenMinutes, null, listOf(log("09:00")), at("2026-10-13", "09:00"))
        assertEquals(at("2026-10-13", "09:10").toInstant(), next)
    }

    @Test
    fun `missed escalation slot fires now rather than in the past`() {
        val now = at("2026-10-13", "11:00")
        val next = ReminderPlanner.nextReminderAt(habit, null, listOf(log("09:00")), now)
        assertEquals(now.toInstant(), next)
    }

    @Test
    fun `escalation stops at midnight and resumes at tomorrow's scheduled time`() {
        val next = ReminderPlanner.nextReminderAt(habit, null, listOf(log("23:45")), at("2026-10-13", "23:45"))
        assertEquals(at("2026-10-14", "09:00").toInstant(), next)
    }

    @Test
    fun `yesterday's reminder log does not affect today`() {
        val now = at("2026-10-13", "09:00")
        val next = ReminderPlanner.nextReminderAt(habit, null, listOf(log("23:30", "2026-10-12")), now)
        assertEquals(now.toInstant(), next)
    }

    @Test
    fun `completed today - next reminder is tomorrow at the scheduled time`() {
        val completion = Completion(habit.id, today, at("2026-10-13", "09:10").toInstant())
        val next = ReminderPlanner.nextReminderAt(habit, completion, listOf(log("09:00")), at("2026-10-13", "09:10"))
        assertEquals(at("2026-10-14", "09:00").toInstant(), next)
    }

    @Test
    fun `completed yesterday - today still fires normally`() {
        val completion = Completion(habit.id, today.minusDays(1), at("2026-10-12", "09:10").toInstant())
        val next = ReminderPlanner.nextReminderAt(habit, completion, emptyList(), at("2026-10-13", "07:00"))
        assertEquals(at("2026-10-13", "09:00").toInstant(), next)
    }

    @Test
    fun `disabled habit or reminders off - no reminder at all`() {
        assertNull(ReminderPlanner.nextReminderAt(habit.copy(enabled = false), null, emptyList(), at("2026-10-13", "07:00")))
        assertNull(ReminderPlanner.nextReminderAt(habit.copy(remindersEnabled = false), null, emptyList(), at("2026-10-13", "07:00")))
    }

    @Test
    fun `shouldDeliver only when due or overdue and incomplete`() {
        assertFalse(ReminderPlanner.shouldDeliver(habit, null, at("2026-10-13", "08:59")))
        assertTrue(ReminderPlanner.shouldDeliver(habit, null, at("2026-10-13", "09:00")))
        assertTrue(ReminderPlanner.shouldDeliver(habit, null, at("2026-10-13", "12:00")))
        val completion = Completion(habit.id, today, at("2026-10-13", "09:10").toInstant())
        assertFalse(ReminderPlanner.shouldDeliver(habit, completion, at("2026-10-13", "12:00")))
        assertFalse(ReminderPlanner.shouldDeliver(habit.copy(enabled = false), null, at("2026-10-13", "12:00")))
        assertFalse(ReminderPlanner.shouldDeliver(habit.copy(remindersEnabled = false), null, at("2026-10-13", "12:00")))
    }
}
