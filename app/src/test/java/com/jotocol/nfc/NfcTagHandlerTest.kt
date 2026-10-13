package com.jotocol.nfc

import com.jotocol.reminders.HabitActions
import com.jotocol.reminders.ReminderScheduler
import com.jotocol.support.FakeAlarms
import com.jotocol.support.FakeHaptics
import com.jotocol.support.FakeNotifier
import com.jotocol.support.FakeRepository
import com.jotocol.support.MutableClock
import com.jotocol.support.at
import com.jotocol.support.creatine
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NfcTagHandlerTest {

    private val repo = FakeRepository()
    private val haptics = FakeHaptics()
    private val clock = MutableClock(at("2026-10-13", "09:10"))
    private val scheduler = ReminderScheduler(repo, FakeAlarms(), clock)
    private val actions = HabitActions(repo, scheduler, FakeNotifier(), haptics, clock)
    private val handler = NfcTagHandler(repo, actions, haptics, clock)

    @Test
    fun `registering then scanning completes the habit`() = runTest {
        repo.seed(creatine(id = 1))

        val registered = handler.handle("04a1b2c3", registrationTarget = 1)
        assertTrue(registered is NfcEvent.Registered)
        assertEquals("04a1b2c3", repo.getHabit(1)!!.nfcTagId)

        val completed = handler.handle("04a1b2c3", registrationTarget = null)
        assertTrue(completed is NfcEvent.Completed)
        assertNotNull(repo.getCompletion(1, clock.now.toLocalDate()))
    }

    @Test
    fun `scanning again after completion reports already done`() = runTest {
        repo.seed(creatine(id = 1, nfcTagId = "aa"))
        handler.handle("aa", null)
        val second = handler.handle("aa", null)
        assertTrue(second is NfcEvent.AlreadyDone)
        assertEquals(1, repo.allCompletions.size)
    }

    @Test
    fun `unknown tag is rejected with a warning haptic`() = runTest {
        repo.seed(creatine(id = 1, nfcTagId = "aa"))
        val event = handler.handle("zz", null)
        assertTrue(event is NfcEvent.UnknownTag)
        assertEquals(1, haptics.warnings)
        assertTrue(repo.allCompletions.isEmpty())
    }

    @Test
    fun `tag registered to another habit cannot be re-registered`() = runTest {
        repo.seed(creatine(id = 1, nfcTagId = "aa"), creatine(id = 2).copy(name = "Vitamin D"))
        val event = handler.handle("aa", registrationTarget = 2)
        assertTrue(event is NfcEvent.TagInUse)
        assertEquals(1L, (event as NfcEvent.TagInUse).owner.id)
        assertEquals(null, repo.getHabit(2)!!.nfcTagId)
    }

    @Test
    fun `re-registering the same tag to the same habit is allowed`() = runTest {
        repo.seed(creatine(id = 1, nfcTagId = "aa"))
        val event = handler.handle("aa", registrationTarget = 1)
        assertTrue(event is NfcEvent.Registered)
    }
}
