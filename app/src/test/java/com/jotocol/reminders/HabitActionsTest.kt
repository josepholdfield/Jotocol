package com.jotocol.reminders

import com.jotocol.domain.ReminderPlanner
import com.jotocol.support.FakeAlarms
import com.jotocol.support.FakeHaptics
import com.jotocol.support.FakeNotifier
import com.jotocol.support.FakeRepository
import com.jotocol.support.MutableClock
import com.jotocol.support.at
import com.jotocol.support.creatine
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Drives the same flow the BroadcastReceivers do, end to end against fakes: alarm fires,
 * reminder is recorded, escalation is scheduled, completion cancels everything.
 */
class HabitActionsTest {

    private val repo = FakeRepository()
    private val alarms = FakeAlarms()
    private val notifier = FakeNotifier()
    private val haptics = FakeHaptics()
    private val clock = MutableClock(at("2026-10-13", "07:00"))
    private val scheduler = ReminderScheduler(repo, alarms, clock)
    private val actions = HabitActions(repo, scheduler, notifier, haptics, clock)

    /** Mirrors ReminderReceiver.onReceive. */
    private suspend fun fireAlarm(habitId: Long) {
        val now = clock.now
        val habit = repo.getHabit(habitId)!!
        val completion = repo.getCompletion(habitId, now.toLocalDate())
        if (ReminderPlanner.shouldDeliver(habit, completion, now)) {
            repo.recordReminderSent(habitId, now.toLocalDate(), now.toInstant())
            notifier.showReminder(habit, repo.getReminderLogs(now.toLocalDate()).count { it.habitId == habitId })
        }
        scheduler.reconcile(habit)
    }

    @Test
    fun `full day - reminder, escalation, completion stops reminders, next day resets`() = runTest {
        repo.seed(creatine(id = 1, time = "09:00", escalation = 30))
        actions.recover()
        assertEquals(at("2026-10-13", "09:00").toInstant(), alarms.scheduled[1L]!!.first)

        // 09:00 alarm fires: reminder shown and logged, habit NOT completed, escalation queued.
        clock.now = at("2026-10-13", "09:00")
        fireAlarm(1)
        assertEquals(1, notifier.shown.size)
        assertEquals(1, repo.allLogs.size)
        assertNull(repo.getCompletion(1, clock.now.toLocalDate()))
        assertEquals(at("2026-10-13", "09:30").toInstant(), alarms.scheduled[1L]!!.first)

        // User dismisses the notification: nothing changes. 09:30 alarm fires again.
        clock.now = at("2026-10-13", "09:30")
        fireAlarm(1)
        assertEquals(2, notifier.shown.size)
        assertEquals(2, notifier.shown.last().second)
        assertEquals(at("2026-10-13", "10:00").toInstant(), alarms.scheduled[1L]!!.first)

        // User completes at 09:45: notification cancelled, alarm moves to tomorrow, success haptic.
        clock.now = at("2026-10-13", "09:45")
        assertTrue(actions.complete(1))
        assertEquals(listOf(1L), notifier.cancelled)
        assertEquals(1, haptics.successes)
        assertNotNull(repo.getCompletion(1, clock.now.toLocalDate()))
        assertEquals(at("2026-10-14", "09:00").toInstant(), alarms.scheduled[1L]!!.first)

        // A stale 10:00 alarm (e.g. delivered late) must not show anything.
        clock.now = at("2026-10-13", "10:00")
        fireAlarm(1)
        assertEquals(2, notifier.shown.size)

        // Next day: state resets, the 09:00 alarm fires and shows reminder #1 again.
        clock.now = at("2026-10-14", "09:00")
        assertNull(repo.getCompletion(1, clock.now.toLocalDate()))
        fireAlarm(1)
        assertEquals(3, notifier.shown.size)
        assertEquals(1, notifier.shown.last().second)
    }

    @Test
    fun `completing twice is idempotent`() = runTest {
        repo.seed(creatine(id = 1))
        clock.now = at("2026-10-13", "09:10")
        assertTrue(actions.complete(1))
        assertFalse(actions.complete(1))
        assertEquals(1, haptics.successes)
        assertEquals(1, repo.allCompletions.size)
    }

    @Test
    fun `uncomplete restores the escalation schedule`() = runTest {
        repo.seed(creatine(id = 1, time = "09:00"))
        clock.now = at("2026-10-13", "09:10")
        actions.complete(1)
        actions.uncomplete(1)
        assertNull(repo.getCompletion(1, clock.now.toLocalDate()))
        // Nothing was sent today, so the catch-up reminder is due now.
        assertEquals(clock.now.toInstant(), alarms.scheduled[1L]!!.first)
    }

    @Test
    fun `turning reminders off cancels the alarm and the notification`() = runTest {
        repo.seed(creatine(id = 1))
        actions.recover()
        actions.save(creatine(id = 1, remindersEnabled = false))
        assertFalse(alarms.scheduled.containsKey(1L))
        assertEquals(listOf(1L), notifier.cancelled)
    }

    @Test
    fun `recover seeds the sample habit on first run and prunes old logs`() = runTest {
        clock.now = at("2026-10-13", "07:00")
        repo.recordReminderSent(1, at("2026-10-01", "09:00").toLocalDate(), at("2026-10-01", "09:00").toInstant())
        actions.recover()
        val habits = repo.getHabits()
        assertEquals(1, habits.size)
        assertEquals("Take creatine", habits.single().name)
        assertTrue(repo.allLogs.isEmpty())
        assertEquals(1, alarms.scheduled.size)
    }

    @Test
    fun `restart recovery after a reboot does not duplicate alarms`() = runTest {
        repo.seed(creatine(id = 1), creatine(id = 2, time = "20:00"))
        actions.recover() // app launch
        actions.recover() // BOOT_COMPLETED
        actions.recover() // MY_PACKAGE_REPLACED
        assertEquals(2, alarms.scheduled.size)
        assertEquals(6, alarms.setCalls.size)
    }
}
