# Tiffin Tracker

A native Android app to track daily tiffin (meal box) deliveries from one or more tiffin services and calculate the monthly bill.

## Features

- **Daily Logging** — One-tap meal tracking (Received / Skipped / Not Ordered) for each provider and meal type
- **Multiple Providers** — Manage several tiffin services with per-meal-type pricing (Breakfast, Lunch, Dinner)
- **Calendar View** — Color-coded monthly calendar showing delivery status at a glance
- **Billing & Payments** — Auto-calculated monthly bills, payment recording, balance tracking
- **Reminders** — Configurable daily notification with quick-action buttons
- **Statistics** — Charts for meals per month, spending trends, and skip rates
- **Bulk Actions** — Mark holidays or away ranges to auto-skip meals
- **Export/Share** — Share bills as text via WhatsApp or any sharing app
- **Backup/Restore** — Export and import data as JSON
- **Bilingual** — English and Hindi language support

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 (dynamic color, dark mode) |
| Architecture | MVVM (ViewModel + StateFlow) |
| DI | Hilt |
| Database | Room (offline-first local storage) |
| Background | WorkManager (daily reminders) |
| Preferences | DataStore |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 35 |

## Project Structure

```
app/src/main/java/com/tiffintracker/
├── data/
│   ├── db/              # Room entities, DAOs, database
│   └── repository/      # Repository layer, settings, backup
├── di/                  # Hilt dependency injection modules
├── reminder/            # WorkManager worker, scheduler, receivers
├── ui/
│   ├── bills/           # Bills & payments screen
│   ├── calendar/        # Calendar view screen
│   ├── components/      # Reusable UI components
│   ├── navigation/      # Bottom nav & routing
│   ├── onboarding/      # First-run onboarding
│   ├── provider/        # Provider CRUD screens
│   ├── settings/        # Settings screen
│   ├── stats/           # Statistics & charts screen
│   ├── theme/           # Material 3 theming
│   └── today/           # Today (home) screen
└── util/                # Date, currency, sharing utilities
```

## Building & Running

### Prerequisites

- Android Studio Ladybug (2024.2) or later
- JDK 17
- Android SDK with API 35

### Steps

1. Clone the repository:
   ```bash
   git clone https://github.com/aadi1607/health-tracker.git
   cd health-tracker
   ```

2. Open the project in Android Studio.

3. Sync Gradle — the version catalog (`gradle/libs.versions.toml`) manages all dependencies.

4. Build and run on an emulator or device (min API 26):
   ```bash
   ./gradlew assembleDebug
   ```

5. Run unit tests:
   ```bash
   ./gradlew test
   ```

## Data Model

### Provider
Stores tiffin service details: name, phone, per-meal-type prices, offered meal types, and billing cycle start day.

### MealEntry
Tracks each meal delivery with provider, date, meal type (Breakfast/Lunch/Dinner), status (Received/Skipped/Not Ordered), quantity, and optional notes.

### Payment
Records payments made to providers with amount, date, and payment mode (Cash/UPI/Other).

## Key Screens

| Screen | Description |
|--------|-------------|
| **Today** | Quick-toggle cards for logging today's meals |
| **Calendar** | Monthly view with color-coded day status |
| **Bills** | Auto-calculated bills per provider with payment tracking |
| **Stats** | Charts showing meal counts, spending, and skip rates |
| **Settings** | Reminders, theme, backup/restore, language |

## License

This project is for personal use. All rights reserved.
