# Jotocol

A small, standalone Wear OS app that reminds you to do daily habits and keeps nagging, at a
configurable interval, until you explicitly mark them done. The first habit is "Take creatine".

It needs no companion phone, no account and no network.

## What it does

- Home screen shows today's date, outstanding habits (with a clear overdue marker) and
  completed habits. Each outstanding row has a one-tap "done" button; tapping the row opens the
  habit.
- At the habit's reminder time the watch vibrates with a distinctive triple pulse and shows a
  notification with **Done** and **Open** actions. Dismissing the notification does nothing: the
  habit stays outstanding and reminders keep coming.
- If the habit is still incomplete after the escalation interval (default 30 min), another
  reminder is sent, and so on until midnight.
- Marking a habit done (from the home screen, the detail screen, the notification action or an
  NFC tag) records the completion for today, cancels the notification and the pending alarm,
  gives a short success haptic and moves the next reminder to tomorrow.
- Everything is persisted in a local Room database. Completion is stored per habit per local
  calendar date, so reopening the app or rebooting never resets a habit; the next calendar day
  does.
- On app start, reboot, app update, clock/timezone change or exact-alarm permission change the
  app rebuilds every alarm from the database. There is exactly one alarm per habit, keyed by
  habit id, so reconciling can never create duplicates.

## Project layout

```
app/src/main/java/com/jotocol/
  JotocolApplication.kt      Hand-wired object graph (no DI framework)
  domain/                    Pure Kotlin, no Android dependencies
    Habit.kt                 Habit, Completion, ReminderLog, HabitState, HabitToday
    HabitStatus.kt           Derives PENDING / DUE / OVERDUE / COMPLETED from schedule + clock
    ReminderPlanner.kt       Decides when the next reminder fires (escalation policy)
  data/                      Room entities, DAOs, HabitRepository (interface + Room impl)
  reminders/
    ReminderScheduler.kt     One alarm per habit; reconcile() is idempotent
    AlarmPort.kt             Tiny AlarmManager abstraction (tested with a fake)
    ReminderReceiver.kt      Alarm fired -> re-check state -> notify -> log -> reschedule
    CompleteActionReceiver   Notification "Done" action
    RescheduleReceiver.kt    Boot / update / time change recovery
    ReminderNotifications.kt Channel + notification building
    HabitActions.kt          complete / uncomplete / save / delete / recover use cases
    Haptics.kt               Success and warning vibrations
  nfc/
    NfcTagHandler.kt         Pure policy: register tag, complete by tag, reject unknown/in-use
    NfcController.kt         Android reader-mode bridge (foreground only)
  ui/                        Compose for Wear OS (Material 3) screens + one shared ViewModel
app/src/test/                JVM unit tests with in-memory fakes
docs/                        Limitations and the manual test checklist
```

State is always derived from the database and the clock. Notifications and alarms are
projections of that state, never the source of truth.

## Requirements

- Android Studio (the bundled JDK is used; AGP 9.4 needs JDK 17+)
- Android SDK platform 37 and build-tools 36.0.0 (Gradle downloads missing SDK parts if the
  licence is accepted in the SDK Manager)
- A Wear OS device or emulator running API 30 (Wear OS 3) or later. Target SDK is 37.

The build was verified with Gradle 9.8.1, AGP 9.4.1, Kotlin 2.4.21, KSP 2.3.12, Room 2.8.5,
Compose BOM 2026.09.00 and Wear Compose Material 3 1.7.1.

## Build and install

```bash
# from the repo root; local.properties must point sdk.dir at your Android SDK
./gradlew :app:assembleDebug            # APK at app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:installDebug             # installs on the connected watch / emulator
./gradlew :app:testDebugUnitTest        # JVM unit tests
```

### Installing on a physical watch

1. On the watch: Settings > System > About > tap *Build number* seven times, then
   Settings > Developer options > enable *ADB debugging* and *Wireless debugging*.
2. Pair from your machine (watch shows the address and pairing code):
   ```bash
   adb pair <ip>:<pairing-port>
   adb connect <ip>:<port>
   ```
3. `./gradlew :app:installDebug`
4. Launch Jotocol. Allow notifications when asked. If the home screen shows the
   "exact alarms" warning, tap it to open the system setting (see Limitations).

### Running on the emulator

Create a Wear OS AVD (e.g. "Wear OS Large Round", API 37) in Device Manager, start it, then
`./gradlew :app:installDebug`.

To exercise a reminder without waiting, edit the sample habit so its time is a minute or two
ahead, pick a 5 minute interval, and leave the app. The alarm fires, the watch vibrates and the
notification appears. You can also fire the receiver directly:

```bash
adb shell am broadcast -n com.jotocol/.reminders.ReminderReceiver \
    -a com.jotocol.action.REMIND --el habit_id 1
adb shell dumpsys alarm | grep -A4 com.jotocol     # inspect the scheduled alarm
```

## Testing

Unit tests (`./gradlew :app:testDebugUnitTest`) cover:

- state derivation and the daily reset (`HabitStatusTest`)
- escalation eligibility and timing, midnight cut-off, catch-up after a missed alarm,
  completion moving the reminder to tomorrow (`ReminderPlannerTest`)
- one-alarm-per-habit reconciliation, cancellation on disable/delete, exact vs inexact
  (`ReminderSchedulerTest`)
- the whole daily workflow against fakes: reminder, dismiss, escalate, complete, stale alarm,
  next-day reset, restart without duplicates (`HabitActionsTest`)
- NFC registration and completion, unknown and in-use tags (`NfcTagHandlerTest`)

The on-watch checks that cannot be automated are in
[docs/MANUAL_TEST_CHECKLIST.md](docs/MANUAL_TEST_CHECKLIST.md).

## Known limitations

See [docs/LIMITATIONS.md](docs/LIMITATIONS.md) for exact-alarm permissions, background
restrictions and NFC support. In short:

- Exact alarms are used when the OS grants them (the app declares `USE_EXACT_ALARM`, which is
  auto-granted, plus `SCHEDULE_EXACT_ALARM` as a settings-based fallback). If neither is
  granted the app falls back to a 10 minute alarm window and says so on the home screen.
- NFC tag reading is implemented and used only if the watch exposes an NFC adapter to apps and
  the app is in the foreground. Most current watches reserve NFC for payments; manual
  completion always remains available.

## Out of scope for this MVP

Watch faces, tiles and complications, phone companion app, cloud sync, accounts, streaks and
statistics, continuous vibration. The domain/data split is intended to make these additions
straightforward later.
