package com.jotocol.reminders

import com.jotocol.support.FakeAlarms
import com.jotocol.support.FakeRepository
import com.jotocol.support.MutableClock
import com.jotocol.support.at
import com.jotocol.support.creatine
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderSchedulerTest {

    private val repo = FakeRepository()
    private val alarms = FakeAlarms()
    private val clock = MutableClock(at("2026-10-13", "07:00"))
    private val scheduler = ReminderScheduler(repo, alarms, clock)

    @Test
    fun `reconcileAll sets exactly one alarm per habit and is idempotent`() = runTest {
        repo.seed(creatine(id = 1, time = "09:00"), creatine(id = 2, time = "21:00"))

        scheduler.reconcileAll()
        scheduler.reconcileAll()
        scheduler.reconcileAll()

        assertEquals(setOf(1L, 2L), alarms.scheduled.keys)
        assertEquals(at("2026-10-13", "09:00").toInstant(), alarms.scheduled[1L]!!.first)
        assertEquals(at("2026-10-13", "21:00").toInstant(), alarms.scheduled[2L]!!.first)
        // Each reconcile replaces the habit's single alarm rather than adding another.
        assertEquals(6, alarms.setCalls.size)
    }

    @Test
    fun `completed habit gets tomorrow's alarm, not cancelled`() = runTest {
        repo.seed(creatine(id = 1, time = "09:00"))
        clock.now = at("2026-10-13", "09:05")
        repo.markCompleted(1, clock.now.toLocalDate(), clock.now.toInstant())

        scheduler.reconcile(1)

        assertEquals(at("2026-10-14", "09:00").toInstant(), alarms.scheduled[1L]!!.first)
    }

    @Test
    fun `disabled habit has its alarm cancelled`() = runTest {
        repo.seed(creatine(id = 1))
        scheduler.reconcile(1)
        assertTrue(alarms.scheduled.containsKey(1L))

        repo.updateHabit(creatine(id = 1, enabled = false))
        scheduler.reconcile(1)

        assertFalse(alarms.scheduled.containsKey(1L))
        assertEquals(listOf(1L), alarms.cancelCalls)
    }

    @Test
    fun `deleted habit has its alarm cancelled`() = runTest {
        repo.seed(creatine(id = 7))
        scheduler.reconcile(7)
        repo.deleteHabit(creatine(id = 7))

        scheduler.reconcile(7)

        assertFalse(alarms.scheduled.containsKey(7L))
    }

    @Test
    fun `uses exact alarms when permitted and inexact otherwise`() = runTest {
        repo.seed(creatine(id = 1))

        alarms.canScheduleExact = true
        scheduler.reconcile(1)
        assertTrue(alarms.scheduled[1L]!!.second)

        alarms.canScheduleExact = false
        scheduler.reconcile(1)
        assertFalse(alarms.scheduled[1L]!!.second)
    }
}
