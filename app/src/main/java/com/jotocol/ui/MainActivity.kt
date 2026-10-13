package com.jotocol.ui

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.jotocol.JotocolApplication
import com.jotocol.nfc.NfcController
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    private val graph by lazy { JotocolApplication.graph(this) }
    private val viewModel: HabitsViewModel by viewModels { HabitsViewModel.Factory(graph) }
    private lateinit var nfc: NfcController

    /** Habit id to open, set from a notification tap; consumed by the navigation host. */
    private val pendingOpen = MutableStateFlow<Long?>(null)

    /** Keeps the derived state current across minute ticks, clock edits and date changes. */
    private val clockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            viewModel.refreshNow()
        }
    }
    private val clockFilter = IntentFilter().apply {
        addAction(Intent.ACTION_TIME_TICK)
        addAction(Intent.ACTION_TIME_CHANGED)
        addAction(Intent.ACTION_TIMEZONE_CHANGED)
        addAction(Intent.ACTION_DATE_CHANGED)
    }

    private val requestNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            viewModel.refreshPermissions(this)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfc = NfcController(this, graph.nfcHandler)
        pendingOpen.value = intent.habitIdOrNull()

        setContent {
            JotocolApp(
                viewModel = viewModel,
                nfc = nfc,
                pendingOpen = pendingOpen,
                onRequestNotifications = ::requestNotificationPermission,
            )
        }
        requestNotificationPermission()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingOpen.value = intent.habitIdOrNull()
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(clockReceiver, clockFilter)
        viewModel.onResumed(this)
        nfc.onResume()
    }

    override fun onPause() {
        nfc.onPause()
        unregisterReceiver(clockReceiver)
        super.onPause()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !graph.notifications.hasPermission()) {
            requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun Intent.habitIdOrNull(): Long? =
        getLongExtra(EXTRA_HABIT_ID, -1L).takeIf { it >= 0 }

    companion object {
        const val EXTRA_HABIT_ID = "habit_id"

        fun openHabitIntent(context: Context, habitId: Long): Intent =
            Intent(context, MainActivity::class.java)
                .setAction(Intent.ACTION_VIEW)
                .putExtra(EXTRA_HABIT_ID, habitId)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }
}
