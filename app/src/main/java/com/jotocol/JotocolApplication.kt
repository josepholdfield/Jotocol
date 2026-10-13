package com.jotocol

import android.app.Application
import android.content.Context
import com.jotocol.data.HabitRepository
import com.jotocol.data.RoomHabitRepository
import com.jotocol.data.JotocolDatabase
import com.jotocol.reminders.AndroidAlarmPort
import com.jotocol.reminders.HabitActions
import com.jotocol.reminders.AndroidHaptics
import com.jotocol.reminders.Haptics
import com.jotocol.reminders.ReminderNotifications
import com.jotocol.reminders.ReminderScheduler
import com.jotocol.nfc.NfcTagHandler
import java.time.Clock

/**
 * Hand-wired object graph. There is no DI framework; everything is a lazily created singleton
 * reachable from any Context via [graph].
 */
class Graph(context: Context) {
    val clock: Clock = Clock.systemDefaultZone()
    val database: JotocolDatabase by lazy { JotocolDatabase.create(context) }
    val repository: HabitRepository by lazy { RoomHabitRepository(database) }
    val notifications: ReminderNotifications by lazy { ReminderNotifications(context) }
    val haptics: Haptics by lazy { AndroidHaptics(context) }
    val scheduler: ReminderScheduler by lazy {
        ReminderScheduler(repository, AndroidAlarmPort(context), clock)
    }
    val actions: HabitActions by lazy {
        HabitActions(repository, scheduler, notifications, haptics, clock)
    }
    val nfcHandler: NfcTagHandler by lazy { NfcTagHandler(repository, actions, haptics, clock) }
}

class JotocolApplication : Application() {
    val graph: Graph by lazy { Graph(this) }

    override fun onCreate() {
        super.onCreate()
        graph.notifications.ensureChannel()
    }

    companion object {
        fun graph(context: Context): Graph = (context.applicationContext as JotocolApplication).graph
    }
}
