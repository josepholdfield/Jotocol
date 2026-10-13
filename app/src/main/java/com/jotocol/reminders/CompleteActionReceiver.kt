package com.jotocol.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jotocol.JotocolApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Handles the "Done" action on a reminder notification. */
class CompleteActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getLongExtra(EXTRA_HABIT_ID, -1L)
        if (habitId < 0) return
        val graph = JotocolApplication.graph(context)
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                graph.actions.complete(habitId)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val EXTRA_HABIT_ID = "habit_id"

        fun intent(context: Context, habitId: Long): Intent =
            Intent(context, CompleteActionReceiver::class.java)
                .setAction("com.jotocol.action.COMPLETE")
                .putExtra(EXTRA_HABIT_ID, habitId)
    }
}
