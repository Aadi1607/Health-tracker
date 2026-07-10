# HealthTrack

A native Android habit tracker built with Kotlin and Jetpack Compose (Material 3). *Track today, live better.*

## Features

- **Add habits** with a name, an emoji icon, one of six preset colors, and a goal: N times **per day** (8 glasses of water) or **per week** (3 gym sessions)
- **Progress-ring check button**: each tap logs one; the ring fills and snaps into a solid check at the target, with a scale-bounce animation and haptic feedback
- **Streaks** in the habit's own unit — consecutive days or consecutive weeks; a pending today/this-week doesn't break the streak until the period ends
- **7-day chain** on each card: dots for the last 7 days, consecutive completed days joined by a colored line, today's dot outlined and pulsing while pending, partial days dimmed
- **Daily progress** header with partial credit, and a confetti burst when everything is done
- **Exact-time reminders** via AlarmManager: a daily summary at a chosen time plus optional nudges every 1–4 hours (8:00–22:00) while habits are pending; alarms survive reboots and app updates
- **Actionable notifications**: "+1" buttons log a habit straight from the notification
- **Home-screen widget** (Glance): today's habits with live progress, tap a row to log
- **Long-press** a card to edit, reorder, or delete a habit
- **Stats screen**: current/best streak and completion rate per habit, plus a monthly calendar heatmap
- **Export / import** all data as JSON (Storage Access Framework)
- **Dark ink-blue theme** by default (`#14161F` background, `#1C1F2B` cards) with an optional Material You dynamic color mode
- Edge-to-edge layout, friendly empty state, state survives rotation and process death

## Signing

Builds are signed with the checked-in `app/signing/shared.keystore` so sideloaded
updates always install over the previous version. This key is intentionally not
secret — generate and use a private keystore before distributing through a store.

## Tech stack

| Layer | Choice |
| --- | --- |
| UI | Jetpack Compose, Material 3, single-activity |
| Architecture | MVVM — `ViewModel` + `StateFlow`, unidirectional data flow |
| Persistence | Room (`Habit`, `Completion` entities) behind a repository |
| Preferences | DataStore |
| Background | WorkManager (daily reminder) |
| Serialization | kotlinx.serialization (JSON backup) |

- Min SDK 26, target/compile SDK 36, single `:app` module.
- Streak/date logic lives in `domain/Streaks.kt` as pure Kotlin with JUnit coverage.

## Building

```
./gradlew assembleDebug
```

The debug APK lands in `app/build/outputs/apk/debug/`. Unit tests run with:

```
./gradlew testDebugUnitTest
```

CI builds every push via GitHub Actions (`.github/workflows/android.yml`) and uploads the debug APK as a workflow artifact.

## Project layout

```
app/src/main/java/io/github/aadi1607/habittracker/
├── HabitApplication.kt      # manual DI container, notification channel
├── MainActivity.kt          # edge-to-edge, theme, home/stats navigation
├── data/
│   ├── db/                  # Room: entities, DAO, database
│   ├── backup/              # JSON export/import
│   ├── HabitRepository.kt
│   └── SettingsRepository.kt (DataStore)
├── domain/Streaks.kt        # pure streak/date math (unit tested)
├── reminder/                # WorkManager scheduler + worker
└── ui/
    ├── home/                # home screen, habit card, add sheet, settings sheet
    ├── stats/               # stats screen with monthly heatmap
    └── theme/               # ink-blue dark theme, palette, typography
```
