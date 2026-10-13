package com.jotocol.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/**
 * Opens the system "Alarms & reminders" screen if the device has one. Some Wear OS builds do
 * not expose it; in that case nothing happens and the app keeps using inexact alarms.
 */
fun openExactAlarmSettings(context: Context) {
    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
        .setData(Uri.parse("package:${context.packageName}"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
    }
}
