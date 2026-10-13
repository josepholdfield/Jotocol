# Manual test checklist (physical watch)

Run through this once on the target watch after `./gradlew :app:installDebug`.

## Setup

- [ ] App installs and launches; the home screen shows today's date and "Take creatine".
- [ ] Notification permission prompt appears on first launch; allow it.
- [ ] If an "exact alarms" warning row appears on the home screen, tap it and grant the
      permission; the row disappears after returning to the app.
- [ ] Open the habit > Edit. Set the reminder time to 2 minutes from now and the interval to
      5 min. Save. The home row shows "Due at HH:MM".

## Reminder delivery

- [ ] Lower the wrist / let the screen go off. At the reminder time the watch vibrates
      (short, short, long) and shows a "Take creatine" notification with Done and Open.
- [ ] Swipe the notification away. Open Jotocol: the habit is still outstanding and shows
      "Due now" (within 5 min) then "Overdue" with a red marker.
- [ ] Wait 5 minutes without completing. A second reminder arrives; the detail screen shows
      "Reminders sent today: 2".
- [ ] Tap **Open** on a reminder: Jotocol opens on that habit's detail screen.

## Completion

- [ ] Tap **Done** on a notification: short success haptic, notification disappears, home
      screen shows the habit under Completed with the time. No further reminders arrive.
- [ ] Reset: detail screen > Undo completion. Habit returns to outstanding and a catch-up
      reminder arrives promptly.
- [ ] Complete from the home screen tick button and from the detail screen edge button; both
      behave the same.

## Persistence and recovery

- [ ] Complete the habit, force-stop Jotocol (Settings > Apps) and reopen: still completed.
- [ ] With an outstanding habit and a reminder due in a few minutes, reboot the watch. The
      reminder still arrives after boot, exactly once (check `adb shell dumpsys alarm | grep
      com.jotocol` shows a single entry for the habit).
- [ ] Change the watch time zone or clock forward past the reminder time: a catch-up reminder
      arrives and only one alarm remains scheduled.
- [ ] After midnight (or set the clock to the next day): the habit is outstanding again with
      "Due at HH:MM", and yesterday's completion is no longer shown.

## Settings

- [ ] Turn Reminders off in Edit: no notifications arrive, the habit still appears outstanding.
- [ ] Disable the habit: it moves to the bottom of the list as Disabled and never reminds.
- [ ] Add a second habit via "Add habit" (name entered via voice/keyboard); both schedule
      independently. Delete it; its alarm is gone.

## NFC (only if the NFC screen does not say "not supported")

- [ ] Habit > NFC tag shows "Hold a tag to the watch". Hold an NTAG213/215 sticker to the
      back/face of the watch (position varies by model). Screen changes to "Tag linked".
- [ ] Go back to the home screen with the habit outstanding and scan the tag again:
      "Take creatine done" overlay, success haptic, habit moves to Completed.
- [ ] Scan it once more: "already done today" message, no second completion.
- [ ] Scan a different, unregistered tag: "Unknown tag" with a warning buzz.
- [ ] Try to link the same tag to a second habit: "Tag already linked to Take creatine".
- [ ] Confirm tags are not read while Jotocol is in the background (expected: reader mode is
      foreground only).

If the NFC screen says the watch does not let apps read tags, record the watch model and OS
version here and rely on manual completion:

Watch: ______________________ Wear OS version: ________ NFC available to apps: yes / no
