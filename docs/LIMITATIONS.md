# Hardware and OS limitations

## Exact alarms

Jotocol uses `AlarmManager` with one `RTC_WAKEUP` alarm per habit.

| Situation | Behaviour |
|---|---|
| Exact alarms permitted (`canScheduleExactAlarms()` is true) | `setExactAndAllowWhileIdle`. Delivery is near-exact, including in Doze. |
| Exact alarms not permitted | `setWindow` with a 10 minute window (the smallest Android 12+ honours). The home screen shows a warning row; tapping it opens the system *Alarms & reminders* setting if the watch has one. |

Permissions declared:

- `USE_EXACT_ALARM`: granted automatically on install, cannot be revoked by the user. Google
  Play restricts it to alarm/calendar apps; Jotocol is a personal, sideloaded reminder app so
  this is acceptable. If you ever publish to Play, drop this permission and rely on the next one.
- `SCHEDULE_EXACT_ALARM`: the user-grantable version, kept as a fallback. When the system
  grants or revokes it, `RescheduleReceiver` rebuilds every alarm.

Other scheduling caveats:

- Reminders are inexact if the watch is in Bedtime mode, Battery Saver or if the app is put in a
  restricted App Standby bucket. Escalation continues on the next delivery; the planner fires a
  catch-up reminder immediately if an alarm was missed and the habit is still incomplete.
- Escalation stops at local midnight. The next reminder is the following day's scheduled time.
- Alarms do not survive a reboot on their own; `RescheduleReceiver` listens for
  `BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`, `TIME_SET` and `TIMEZONE_CHANGED` and reconciles.
  Direct-boot (before first unlock) is not handled: the first reconcile happens after unlock.
- WorkManager was considered and rejected for delivery: it guarantees execution but not timing,
  which is the wrong trade-off for a time-sensitive nudge. It would be a reasonable addition as a
  safety-net periodic reconcile if an exact-alarm-free device proves unreliable.

## Notifications

- `POST_NOTIFICATIONS` is requested on first launch (API 33+). Without it reminders are silently
  skipped; the home screen shows a warning row that re-requests it.
- Vibration comes from the notification channel's pattern (short, short, long). Users can
  silence the channel in watch settings; the app does not try to bypass that.
- The app never runs a foreground service or a vibration loop.

## NFC

Whether a third-party Wear OS app can read NFC tags depends entirely on the watch. On most
current watches (Pixel Watch, Galaxy Watch) the NFC controller is reserved for Google Wallet /
Samsung Wallet host-card emulation. The Wear OS emulator reports no NFC hardware at all.

**Verified on the Galaxy Watch Ultra 2 (SM-L715F, Wear OS 6 / API 37, 2026-10-13):**
the watch declares `android.hardware.nfc.hce`, `nfc.ese` and `nfc.any` but **not**
`android.hardware.nfc` (the tag reader/writer feature). `NfcAdapter.getDefaultAdapter()`
returns a non-null adapter, but `NfcAdapter.enableReaderMode()` throws
`UnsupportedOperationException`. Tag reading is not available to apps on this watch. Jotocol catches this, reports "This watch does not let apps
read NFC tags" on the NFC screen, and relies on manual completion.

Jotocol therefore treats NFC as optional:

- `android.hardware.nfc` is declared with `required="false"`.
- The NFC screen reports one of: not supported on this watch, NFC switched off, hold a tag to
  link, or tag linked.
- Tag reading uses reader mode and is active only while Jotocol is in the foreground. The tag's
  hardware UID (hex) is stored on the habit. NDEF content is ignored.
- Unknown tags and tags linked to another habit are rejected with a warning haptic.

If your watch cannot read tags, manual completion is the supported path. Reading the tag on a
phone and relaying completion to the watch (Wearable Data Layer) is a possible future
enhancement but is deliberately not a dependency of the MVP.

## Build toolchain

- AGP 9.4 has built-in Kotlin; the `kotlin-android` plugin is not applied. The Compose
  compiler plugin and KSP (for Room) are applied on top.
- compileSdk/targetSdk 37, minSdk 30. Lower minSdk is untested.
