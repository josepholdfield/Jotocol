package com.jotocol.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jotocol.JotocolApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Rebuilds every alarm from persisted state after events that wipe or invalidate them:
 * reboot, app update, clock or timezone change, exact-alarm permission change.
 */
class RescheduleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in HANDLED_ACTIONS) return
        val graph = JotocolApplication.graph(context)
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                graph.actions.recover()
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private val HANDLED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED",
        )
    }
}
