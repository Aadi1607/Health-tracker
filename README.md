# Tiffin Tracker

A fully-offline Android app for two (or up to five) friends who share the same
tiffin (meal delivery) service: log lunches and dinners with one tap, see the
month at a glance, and let the app do the billing math.

Built with **Kotlin + Jetpack Compose (Material 3)**, **MVVM**, **Room**,
**DataStore**, and **WorkManager**. Min SDK 26 (Android 8.0), target SDK 36.

## Features

- **User profiles** — up to 5 users, each with an editable name, color tag,
  and their own per-tiffin price (prices are stored in paise so money math is
  exact).
- **Quick daily logging** — lunch/dinner toggle chips with haptic feedback, an
  extra-tiffin stepper, one-tap *Mark skipped*, an optional note per day, and
  arrows/date-picker to backfill any past date.
- **Calendar view** — monthly grid with one status dot per user per day
  (green = taken, grey = skipped, orange = not logged); tap a day to edit its
  entries in a bottom sheet.
- **Billing** — configurable cycle start day (e.g. 5th → 4th), automatic
  bill = tiffins × price, payment records with derived Paid / Partially paid /
  Pending status, and carry-forward of unpaid balances from earlier cycles.
- **Dashboard** — summary cards (tiffins, due, paid, skipped), a grouped bar
  chart of the last 6 months per user, and per-user streak counters.
- **Notifications** — daily "Did you log today's tiffin?" reminder (default
  21:30, only if someone is unlogged) with *Mark taken* / *Mark skipped*
  quick actions, a cycle-end summary, and a payment reminder N days after an
  unpaid cycle ends. Proper channels, DND-respecting, POST_NOTIFICATIONS
  requested on Android 13+.
- **Export & backup** — CSV report and a clean per-user PDF invoice for the
  current cycle shared via the Android share sheet; full JSON backup/restore
  through the Storage Access Framework (no storage permissions needed).
- **Settings** — users, cycle start day, reminder time, currency symbol,
  light/dark/system theme, reset-cycle and clear-all-data with confirmations.

## Build & run

1. Install **Android Studio** (Ladybug or newer, with SDK 36).
2. **File → Open** and select this repository's root folder.
3. Let Gradle sync (wrapper: Gradle 8.x, AGP 8.10, Kotlin 2.0, JDK 17).
4. Press **Run** on a device/emulator with Android 8.0+.

Command line:

```sh
./gradlew assembleDebug          # APK at app/build/outputs/apk/debug/
./gradlew testDebugUnitTest      # billing/streak unit tests
```

Every push also builds a signed debug APK in GitHub Actions and attaches it to
the `tiffin-latest` release.

## How notifications survive reboots

Reminders are **self-re-arming one-time WorkManager jobs**, not exact alarms:

1. `ReminderScheduler` enqueues a unique `OneTimeWorkRequest` whose initial
   delay is the time until the next occurrence (e.g. tonight 21:30 for the
   daily reminder, 20:00 for the billing check).
2. When a worker runs, it does its check (post the reminder only if someone
   is unlogged; post the cycle summary only on the cycle's last day; nag
   about unpaid bills N days after a cycle ends) and then **re-enqueues
   itself for the next day** — in a `finally` block, so the chain never dies.
3. WorkManager persists its queue in its own database, so pending work
   survives a reboot on its own. On top of that, a `BootReceiver` registered
   for `BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`, `TIMEZONE_CHANGED` and
   `TIME_SET` re-arms both chains with **freshly computed delays** — this
   matters because a one-time request's delay is relative, and a reboot,
   update, or clock/timezone change can make the old delay fire at the wrong
   wall-clock time.
4. App startup also calls the scheduler with `ExistingWorkPolicy.KEEP` as a
   belt-and-braces fallback (KEEP so an already-pending reminder isn't
   reset), while the boot receiver and settings changes use `REPLACE`.

This approach was chosen over `AlarmManager.setExact` because exact alarms
need the `SCHEDULE_EXACT_ALARM` special permission on Android 12+ and can be
revoked; WorkManager trades a few minutes of precision for guaranteed,
permission-free delivery.

## Project structure

```
app/src/main/java/io/github/aadi1607/tiffintracker/
├── TiffinApplication.kt      # DI container, channel + worker bootstrap
├── MainActivity.kt           # bottom-nav scaffold, permission prompt
├── data/
│   ├── db/                   # Room: User, TiffinEntry, Payment + DAOs
│   ├── TiffinRepository.kt   # single source of truth, Flow-based
│   ├── SettingsRepository.kt # DataStore preferences
│   ├── backup/               # JSON backup/restore (SAF)
│   └── export/               # CSV + PdfDocument invoice, share-sheet helper
├── domain/                   # pure logic: BillingCycle, BillCalculator, Streaks
├── notifications/            # channels, workers, scheduler, receivers
└── ui/                       # Compose screens: home, calendar, billing, stats, settings
```

The habit-tracker app this repository previously hosted lives on the
`claude/android-habit-tracker-uqp5sc` branch.
