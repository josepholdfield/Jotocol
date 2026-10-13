package com.jotocol.reminders

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

interface Haptics {
    /** Short, positive confirmation on completion. */
    fun success()
    /** Gentle nudge for a rejected action, e.g. an unknown NFC tag. */
    fun warning()
}

class AndroidHaptics(private val context: Context) : Haptics {

    private val vibrator: Vibrator?
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }

    override fun success() {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 60, 60, 120), -1))
    }

    override fun warning() {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        v.vibrate(VibrationEffect.createOneShot(250, VibrationEffect.DEFAULT_AMPLITUDE))
    }
}
