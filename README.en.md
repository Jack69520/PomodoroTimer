# Pomodoro Timer

**Language:** [简体中文](README.zh.md) · [繁體中文](README.zh-Hant.md) · [English](README.en.md)

Pomodoro Timer is an Android focus app for study and work: time focus and break intervals, keep tasks in a todo list, review effort in statistics and a calendar, and cut distractions during sessions with app blocking, Do Not Disturb, and a fullscreen lock-screen timer.

All data stays on the device—no network, sync, or upload at runtime. This repository ships Java/MVVM source so you can read, build, and extend the app.

| Item | Value |
|------|-------|
| Package | `com.skyinit.pomodorotimer` |
| Min / target SDK | Android 9 (API 28) / API 36 |
| License | [Apache License 2.0](LICENSE) |

## Features

### Pomodoro timer

- Default 25-minute focus and 5-minute break; adjust focus duration on the home screen (long-press the timer) or under **Profile → Settings → Pomodoro & Todos** (1 minute–3 hours); break length 1–30 minutes
- Quick presets: 45 / 60 / 90 / 120 / 150 / 180 minutes; durations ≥ 1 hour display as `HH:MM:SS`
- Short breaks and optional long breaks (every N=2–8 pomodoros → 10–15 minutes; off by default)
- Pause during focus with a reason (stepped away, interrupted, inspiration, etc.); max pauses 1–5 (default 2); pausing over 5 minutes fails the round
- About 5 minutes of study is required before a session is suggested as a completed record; after a break, continue manually or enable auto-start for the next focus round
- **`TimerService` foreground service** (`specialUse` / `pomodoro_focus_timer`) keeps timing with notification progress; exact alarms cover completion and pause timeout; on boot, alarms are re-scheduled from the session snapshot
- After process death, sessions can be restored or settled from the local snapshot
- Ringtone: system default / silent / custom; optional vibration
- Dedicated timer screen supports a landscape minimal UI (timer + controls only)

### Lock-screen fullscreen timer

- Setting **Show timer fullscreen on lock screen**: during focus and break, show the timer fullscreen on the lock screen (display only; long-press to unlock)
- When the screen is on and locked, the app tries to bring the timer back; it does not force-wake a dark screen
- Requires notification permission; granting full-screen intent is recommended for reliable return after leaving the timer page (some devices may only show a lock-screen countdown notification)

### Tasks & todos

- **Simple todos** and **todo collections** (with subtasks)
- Categories: work, study, life, sports, entertainment, other; priority, tags, due dates, estimated pomodoros
- Pin up to 3; filter by priority / due date / category; swipe to delete
- Simple todos support recurrence: daily / weekly / monthly (advanced on completion via domain policy—**not** WorkManager)
- Home groups: pinned / overdue / today / upcoming / no date / completed
- Start timing from a task or subtask; pomodoro progress updates automatically; collections show progress and a next-subtask flow
- Optional auto-delete of completed tasks (cleared 3 days after completion)

### Statistics & calendar

- Today / this week / this month focus counts and duration; 7-day trend, hourly distribution, category pie chart (drill-down)
- Streaks, week-over-week comparison, monthly insights, interruption diagnostics (pause-reason distribution)
- Monthly calendar with per-day history; session details support **notes** (up to 200 characters); blocked-app records available
- Profile shows **total completed pomodoros** for the active account

### Focus aids: app blocking & DND

- **App blocking**: Usage Access + overlay mask to intercept distracting apps (**no** Accessibility Service or Device Admin)
  - Enable standalone blocking from Profile, or turn on **auto-block during Pomodoro** in settings (active only while focus is running—not while paused or on break)
  - Management UI: search, categories, all / blocked / allowed, scan installed apps; rules from a local JSON policy engine
  - A blocking service run stops automatically after about 5 hours
- **Do Not Disturb during focus**: optionally enable system DND when a focus session starts (notification policy access required); the prior system interruption filter is persisted to disk so it can be restored when the session ends—including after process death and alarm-driven settlement—with conditional orphan recovery on cold start when no active session remains

### Accounts & personalization

- Startup flow: privacy consent → first-time registration (skippable) → feature onboarding → main UI (Home / Statistics / Calendar / Profile)
- **Guest**: can browse the UI and adjust default display duration, but **cannot** start timers, manage todos, view real stats/calendar, or enable app blocking
- **Registered account**: local 12-digit account ID, password login, change password, recover with ID + nickname; multi-profile switching with per-account data isolation
- Passwords stored with **PBKDF2-HMAC-SHA256**; custom nickname, signature, and avatar (camera / gallery)
- Theme skins: standard, Chinese colors, traditional gradients, Morandi; night resources supported

### Settings & more

- Settings hub: account / account & security / theme / ringtone / Pomodoro & todos / **system permissions**
- Home-screen **App Shortcuts** (long-press icon—not widgets): 25-minute focus, open statistics, blocking mode
- Built-in searchable FAQ and **Dev Lab** (version, device, storage/memory, runtime logs)
- About: Privacy Policy, User Agreement, third-party open-source licenses

## Tech stack

| Category | Technology |
|----------|------------|
| Language | Java 11 |
| Build | Android Gradle Plugin 9.1.1 · Gradle 9.3.1 |
| Min / target SDK | API 28 / 36 |
| Architecture | **MVVM** (UI → ViewModel → Repository → Room / SharedPreferences); pure domain policies; manual DI via `AppContainer` |
| UI | AppCompat · Material Components · ConstraintLayout · Navigation |
| State | LiveData / ViewModel; some screens use lightweight **MVI** (`Intent` / `UiState` / `Effect` + `dispatch`) |
| Local DB | Room 2.6.1 (schemas exported to `app/schemas/`) |
| Charts | MPAndroidChart |
| Background | Dual `specialUse` foreground services + exact Alarm + BootReceiver (**no** WorkManager) |
| Tests | JUnit · Robolectric · Architecture Components Testing · Room Testing |

Release builds enable minify and shrinkResources by default.

## Requirements

- **Android Studio** (latest stable recommended)
- **JDK 11** or newer
- **Android SDK** with compileSdk / targetSdk 36
- No backend, API keys, or network setup

## Build & run

### Clone the repository

```bash
git clone https://github.com/Jack69520/PomodoroTimer.git
cd PomodoroTimer
```

### Android Studio

1. Open Android Studio, choose **Open**, and select the project root
2. Wait for Gradle sync to finish
3. Connect a device or start an emulator, then click **Run**

On first sync, Android Studio reads the SDK path from `local.properties`, which is generated locally and is not committed.

### Command line

**Windows:**

```bat
gradlew.bat assembleDebug
```

**macOS / Linux:**

```bash
./gradlew assembleDebug
```

Debug APK output: `app/build/outputs/apk/debug/`.

### Release signing

Copy [`keystore.properties.example`](keystore.properties.example) to `keystore.properties` at the project root and fill in real values (listed in `.gitignore`—do not commit). Without it, release builds fall back to the debug signing key.

```properties
storeFile=release_key_for_PomodoroTimer.jks
storePassword=YOUR_STORE_PASSWORD
keyAlias=YOUR_KEY_ALIAS
keyPassword=YOUR_KEY_PASSWORD
```

### Tests

**Windows:**

```bat
gradlew.bat test
```

**macOS / Linux:**

```bash
./gradlew test
```

Unit tests cover timer policy and session recovery, lock-screen presentation, app blocking, account isolation, todo domain logic, password hashing, Room migrations, and more (mainly JUnit + Robolectric). Instrumented tests currently only verify the package name.

## Project structure

```
PomodoroTimer/
├── app/                          # Sole application module
│   ├── schemas/                  # Room schema exports
│   └── src/main/
│       ├── assets/               # Legal HTML, blocking/category/identity JSON
│       ├── java/com/skyinit/pomodorotimer/
│       │   ├── App.java / AppContainer.java / MainActivity.java / …
│       │   ├── data/             # entities, DAOs, database, repositories, models
│       │   ├── domain/           # timer, todo, blocking, appidentity, account
│       │   ├── security/         # PasswordHasher (PBKDF2)
│       │   ├── service/          # TimerService, AppBlockingService, Alarm/Boot receivers
│       │   ├── ui/               # home, statistics, calendar, profile, settings,
│       │   │                     # account, auth, consent, onboarding, bootstrap, theme, …
│       │   └── util/             # lock screen, overlay, DND, permissions, shortcuts, …
│       └── res/                  # layouts (incl. layout-land), navigation, values(-night), xml
├── gradle/libs.versions.toml     # Version Catalog
├── keystore.properties.example
├── LICENSE
├── README*.md
└── …
```

## Main permissions

| Permission | Purpose |
|------------|---------|
| `POST_NOTIFICATIONS` | Timer / blocking notifications |
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_SPECIAL_USE` | Timer and blocking FGS |
| `SCHEDULE_EXACT_ALARM` | Completion and pause-timeout fallback |
| `RECEIVE_BOOT_COMPLETED` | Re-schedule alarms on boot |
| `USE_FULL_SCREEN_INTENT` | Lock-screen fullscreen return |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Reduce background kills |
| `ACCESS_NOTIFICATION_POLICY` | Focus DND |
| `PACKAGE_USAGE_STATS` | Detect foreground apps (blocking) |
| `SYSTEM_ALERT_WINDOW` | Blocking overlay |
| `QUERY_ALL_PACKAGES` | Scan installed apps |
| `VIBRATE` | Reminder vibration |
| `CAMERA` / `READ_MEDIA_IMAGES` | Avatar |
| `READ_MEDIA_AUDIO` (and legacy storage read) | Custom ringtone |

The manifest actively removes `ACCESS_NETWORK_STATE`; the app does not declare internet permission. Use **Profile → Settings → System permissions** for a guided overview.

## Privacy & data

- **Fully offline**: no upload or sharing of user data with the developer or third-party servers at runtime
- **Local storage**: sessions, todos, stats, and accounts in Room; device-level prefs (theme, ringtone) in SharedPreferences; registered accounts are isolated from each other
- **System backup**: if auto/cloud backup is enabled on the device, some local data may be backed up by the OS to OEM or Google backup services—see the in-app Privacy Policy
- **Permissions on demand**: notifications, camera, DND, usage access, etc. are used only after you grant them
- Full details in the in-app Privacy Policy and User Agreement under **Profile → About**

## Third-party open source

The app uses AndroidX, Material Components, MPAndroidChart, and other libraries. See **Profile → About → Open Source Licenses** in the app, or [`OpenSourceLicensesActivity`](app/src/main/java/com/skyinit/pomodorotimer/ui/profile/OpenSourceLicensesActivity.java).

## License

This project is licensed under the [Apache License 2.0](LICENSE).

## Contributing

Issues and pull requests are welcome. Before submitting:

- Match existing code style and conventions
- Ensure unit tests pass (`./gradlew test` / `gradlew.bat test`)
- Do not commit secrets, `local.properties`, `keystore.properties`, or other sensitive files
