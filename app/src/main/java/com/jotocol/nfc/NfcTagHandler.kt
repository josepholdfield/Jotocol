package com.jotocol.nfc

import com.jotocol.data.HabitRepository
import com.jotocol.domain.Habit
import com.jotocol.reminders.HabitActions
import com.jotocol.reminders.Haptics
import java.time.Clock
import java.time.ZonedDateTime

/** Outcome of scanning a tag, rendered by the UI as a short message. */
sealed class NfcEvent {
    data class Registered(val habit: Habit) : NfcEvent()
    data class Completed(val habit: Habit) : NfcEvent()
    data class AlreadyDone(val habit: Habit) : NfcEvent()
    data class TagInUse(val tagId: String, val owner: Habit) : NfcEvent()
    data class UnknownTag(val tagId: String) : NfcEvent()
}

/**
 * Pure tag-handling policy, independent of the Android NFC APIs. [handle] is called with a
 * tag's identifier and, when the user is on a habit's NFC setup screen, that habit's id.
 */
class NfcTagHandler(
    private val repository: HabitRepository,
    private val actions: HabitActions,
    private val haptics: Haptics,
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    suspend fun handle(tagId: String, registrationTarget: Long?): NfcEvent {
        val owner = repository.getHabitByNfcTag(tagId)

        if (registrationTarget != null) {
            val target = repository.getHabit(registrationTarget)
            if (target != null) {
                if (owner != null && owner.id != target.id) {
                    haptics.warning()
                    return NfcEvent.TagInUse(tagId, owner)
                }
                actions.save(target.copy(nfcTagId = tagId))
                haptics.success()
                return NfcEvent.Registered(target.copy(nfcTagId = tagId))
            }
        }

        if (owner == null) {
            haptics.warning()
            return NfcEvent.UnknownTag(tagId)
        }
        val today = ZonedDateTime.now(clock).toLocalDate()
        if (repository.getCompletion(owner.id, today) != null) {
            haptics.warning()
            return NfcEvent.AlreadyDone(owner)
        }
        actions.complete(owner.id)
        return NfcEvent.Completed(owner)
    }
}
