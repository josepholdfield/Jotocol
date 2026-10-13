package com.jotocol.nfc

import android.app.Activity
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Bridges the Android NFC reader-mode API to [NfcTagHandler]. Reader mode is only active while
 * the activity is resumed, so tags are read only when Jotocol is in the foreground.
 *
 * Watches whose NFC controller is reserved for payments report it in one of two ways: either
 * [NfcAdapter.getDefaultAdapter] returns null, or (Galaxy Watch Ultra 2, Wear OS 6) the adapter
 * exists but [NfcAdapter.enableReaderMode] throws [UnsupportedOperationException]. Both cases
 * make [isSupported] false and the app falls back to manual completion only.
 */
class NfcController(
    private val activity: Activity,
    private val handler: NfcTagHandler,
) : NfcAdapter.ReaderCallback {

    private val adapter: NfcAdapter? = runCatching { NfcAdapter.getDefaultAdapter(activity) }.getOrNull()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Cleared the first time the platform refuses reader mode. */
    private val _readerModeSupported = MutableStateFlow(adapter != null)
    val readerModeSupported: StateFlow<Boolean> = _readerModeSupported

    val isSupported: Boolean get() = adapter != null && _readerModeSupported.value
    val isEnabled: Boolean get() = runCatching { adapter?.isEnabled == true }.getOrDefault(false)

    /** Habit id whose NFC setup screen is open, or null for normal completion scanning. */
    val registrationTarget = MutableStateFlow<Long?>(null)

    private val _events = MutableStateFlow<NfcEvent?>(null)
    val events: StateFlow<NfcEvent?> = _events

    fun onResume() {
        val a = adapter ?: return
        if (!_readerModeSupported.value) return
        val options = Bundle().apply { putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250) }
        try {
            a.enableReaderMode(activity, this, READER_FLAGS, options)
        } catch (e: UnsupportedOperationException) {
            Log.w(TAG, "Reader mode not supported on this device; NFC completion disabled", e)
            _readerModeSupported.value = false
        } catch (e: RuntimeException) {
            // e.g. SecurityException or a dead NFC service; treat as unavailable rather than crash.
            Log.w(TAG, "Could not enable NFC reader mode", e)
            _readerModeSupported.value = false
        }
    }

    fun onPause() {
        val a = adapter ?: return
        if (!_readerModeSupported.value) return
        try {
            a.disableReaderMode(activity)
        } catch (e: RuntimeException) {
            Log.w(TAG, "Could not disable NFC reader mode", e)
        }
    }

    override fun onTagDiscovered(tag: Tag?) {
        val id = tag?.id?.toHex() ?: return
        scope.launch {
            _events.value = handler.handle(id, registrationTarget.value)
        }
    }

    fun consumeEvent() {
        _events.value = null
    }

    companion object {
        private const val TAG = "NfcController"
        private const val READER_FLAGS =
            NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_NFC_F or NfcAdapter.FLAG_READER_NFC_V or
                NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK

        fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
    }
}
