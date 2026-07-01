# Pomodoro Timer

**Language:** [简体中文](README.zh.md) · [繁體中文](README.zh-Hant.md) · [English](README.en.md)

A fully offline Android Pomodoro app for managing focus sessions, todos, and study habits. Timer data, tasks, and statistics are stored locally on your device—nothing is uploaded or synced over the network.

## Features

### Pomodoro Timer

- Default 25-minute focus and 5-minute break; focus duration adjustable on the home screen (long-press the timer) or in Settings (1 minute to 3 hours)
- Short breaks and optional long breaks (10–15 minutes after N completed pomodoros)
- Pause during focus with reason tracking; configurable max pause count per session (1–5)
- Pausing over 5 minutes fails the session; sessions count toward stats only after 5+ minutes of effective focus
- After a break, start another round or end the session; optional auto-start for the next focus round
- Timer runs as a foreground service with progress in the notification shade
- Custom ringtones, vibration, and exact-alarm reminders

### Tasks & Todos

- **Simple todos** and **todo collections** (with subtasks)
- Categories (work, study, life, etc.), priority, tags, due dates, estimated pomodoro count
- Pin (up to 3), filter, swipe-to-delete, recurring creation (daily / weekly / monthly)
- Start timing from a task or subtask; pomodoro progress tracked automatically

### Statistics & Calendar

- Today / this week / this month focus counts and duration charts
- 7-day trends, hourly distribution, category pie chart (drill down to details)
- Pause-reason distribution
- Monthly calendar with per-day session history, detail views, and session notes

### Focus Tools

- **App blocking**: blocks entertainment, social, and shopping apps during focus (requires usage access, overlay, and related permissions)
- **Do Not Disturb during focus**: optionally enable system DND when a focus session starts
- Landscape home layout with a minimal timer UI for distraction-free use

### Accounts & Personalization

- Local profile created on first launch—no login required
- Optional upgrade to a registered account (password protection, multi-profile switching); passwords stored with encryption
- Custom nickname, signature, and avatar
- Multiple theme palettes (standard, Chinese colors, gradient, Morandi)

### More

- Home-screen shortcuts: start 25-minute focus, open statistics, enable blocking
- Built-in FAQ and Dev Lab (app/device info, runtime logs)

## Tech Stack

| Category | Technology |
|----------|------------|
| Language | Java 11 |
| Min SDK | Android 9 (API 28) |
| Target SDK | API 36 |
| Architecture | ViewModel + LiveData + Repository + Room |
| Local DB | Room |
| Navigation | AndroidX Navigation |
| Background work | WorkManager |
| Charts | MPAndroidChart |
| UI | Material Components |

## Requirements

- **Android Studio** (latest stable recommended)
- **JDK 11** or newer
- **Android SDK** with API 36 build tools

## Build & Run

### Clone the repository

```bash
git clone https://github.com/Jack69520/PomodoroTimer.git
cd PomodoroTimer
```


### Android Studio

1. Open Android Studio and choose **Open**, then select the project root
2. Wait for Gradle sync to finish
3. Connect a device or start an emulator, then click **Run**

On first sync, Android Studio reads the SDK path from `local.properties`, which is generated locally and is not committed to the repo.

### Command line

**Windows:**

```bat
gradlew.bat assembleDebug
```

**macOS / Linux:**

```bash
./gradlew assembleDebug
```

The debug APK is output to `app/build/outputs/apk/debug/`.

### Release signing

Release builds require a local `keystore.properties` at the project root (listed in `.gitignore`—do not commit). Without it, release builds fall back to the debug signing key.

```properties
storeFile=your-release-key.jks
storePassword=your-store-password
keyAlias=your-key-alias
keyPassword=your-key-password
```

### Unit tests

```bash
./gradlew test
```

## Project Structure

```
app/src/main/java/com/skyinit/pomodorotimer/
├── data/           # Room entities, DAOs, repositories
├── service/        # Timer foreground service, app blocking service
├── ui/             # Activities, fragments, ViewModels
│   ├── home/       # Home, timer, todo editing
│   ├── statistics/ # Statistics charts
│   ├── calendar/   # Calendar and session details
│   ├── profile/    # Settings, about, app blocking
│   ├── account/    # Login, register, account management
│   └── consent/    # First-launch privacy consent
├── util/           # Utilities
└── worker/         # Recurring todo scheduling
```

## Privacy & Data

- **Fully offline**: the app does not use the network and does not upload or share user data
- **Local storage**: sessions, todos, stats, and account data live in a local Room database
- **Permissions on demand**: notifications, camera, DND, usage access, etc. are used only when you grant them
- Full details in the in-app Privacy Policy and User Agreement under **Profile → About**

## Third-Party Open Source

The app uses AndroidX, Material Components, MPAndroidChart, and other libraries. See **Profile → About → Open Source Licenses** in the app, or [`OpenSourceLicensesActivity`](app/src/main/java/com/skyinit/pomodorotimer/ui/profile/OpenSourceLicensesActivity.java).

## License

This project is licensed under the [Apache License 2.0](LICENSE).

## Contributing

Issues and pull requests are welcome. Before submitting:

- Match existing code style and conventions
- Ensure unit tests pass (`./gradlew test`)
- Do not commit secrets, `local.properties`, `keystore.properties`, or other sensitive files
