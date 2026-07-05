# Habit Tracker

A native Android habit tracker built with Kotlin and Jetpack Compose (Material 3).

## Features

- **Add habits** with a name, an emoji icon, and one of six preset colors, via a bottom-sheet form
- **Tap-to-complete** cards with a large (56dp) check button, scale-bounce animation, and haptic feedback
- **Streaks** that count consecutive days — a pending "today" doesn't break the streak until midnight passes
- **7-day chain** on each card: dots for the last 7 days, consecutive completed days joined by a colored line, today's dot outlined and pulsing while pending
- **Daily progress** header ("3 of 5 done") with a progress bar
- **Long-press to delete** a habit, with a confirmation dialog
- **Daily reminder** notification at a user-chosen time (WorkManager), with `POST_NOTIFICATIONS` permission handling on Android 13+
- **Stats screen**: current/best streak and completion rate per habit, plus a monthly calendar heatmap
- **Export / import** all data as JSON (Storage Access Framework)
- **Dark ink-blue theme** by default (`#14161F` background, `#1C1F2B` cards) with an optional Material You dynamic color mode
- Edge-to-edge layout, friendly empty state, state survives rotation and process death

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
