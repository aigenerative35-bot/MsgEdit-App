# MsgEdit — Android app (Kotlin + Jetpack Compose)

MsgEdit is a small, offline, dark-theme Android app. You pick a message you have
received, keep the original text as-is, write an **edited personal copy** next to
it, and save both locally on the device.

It is a personal text workspace, not a messaging client. It can read the messages
that have already been received on the device **only if you grant it SMS access**,
and it never changes or sends anything. Editing here never alters the message that
was actually received — it only stores the copy you type in.

## What it does

- Tap a message in the **received inbox** to load it, or paste text into the
  **Original** box yourself.
- Press **Start editing** to copy it into the **Edited copy** box, then change it.
- **Save** keeps both versions together in a local list.
- Each saved entry shows the original and the edited copy side by side, with
  **Load**, **Copy** and **Delete** actions.
- **Copy** buttons put a version on the clipboard so you can paste it elsewhere.
- Everything is stored in app-private `SharedPreferences` and stays on the device.

## SMS access — what it can and cannot do

- The app can **read messages already received** on the device, after you grant the
  `READ_SMS` permission at runtime. The inbox list is shown inside the app.
- The app **cannot change the message that was actually received**. No app can
  rewrite an SMS that a carrier has already delivered and stored. MsgEdit only
  creates a separate edited copy that you type.
- The app does **not** send SMS, does not write or delete SMS, has no notification
  access, and makes no network calls. `READ_SMS` is the only sensitive permission
  it requests.

## Why it is safe by design

- Only one sensitive permission: `READ_SMS` (requested at runtime, optional — the
  app still works by pasting text if you decline).
- No `SEND_SMS` / `WRITE_SMS` permission.
- No notification access.
- No internet permission, no network calls.

## Tech stack

- Kotlin
- Jetpack Compose (Material 3), single-activity
- AndroidX `activity-compose`, `compose-bom`
- `SharedPreferences` + `org.json` for local persistence
- SMS inbox read via `ContentResolver` / `Telephony.Sms.Inbox`
- Gradle (Kotlin DSL), AGP 8.7.3, Kotlin 2.0.21, Gradle 8.11.1

## Requirements

- Android Studio (Ladybug or newer recommended)
- JDK 17
- Android SDK 35
- Internet access on the first build, so Gradle can download dependencies

## Open and run

1. Unzip the repository.
2. Open the extracted folder in Android Studio (`File > Open`).
3. Let the Gradle sync finish.
4. Run on an emulator or a phone with USB debugging enabled.

From the command line, if you have JDK 17 and the Android SDK set up
(`ANDROID_HOME` / `local.properties` with `sdk.dir`):

```bash
./gradlew assembleDebug
```

## Project layout

```
MsgEdit/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/example/msgedit/MainActivity.kt
│       └── res/
│           ├── drawable/            launcher icon vectors
│           ├── mipmap-*/            launcher icon PNGs (pre-API 26)
│           ├── mipmap-anydpi-v26/   adaptive launcher icon
│           ├── values/              strings, colors, theme
│           └── xml/                 backup rules
├── gradle/wrapper/                  Gradle wrapper (8.11.1)
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew / gradlew.bat
└── .gitignore
```

## Notes and limitations

- Package / application ID: `com.example.msgedit`
- Minimum Android version: Android 6.0 (API 23); target SDK 35.
- Reading SMS is a restricted permission on Google Play; for sideloaded builds the
  runtime prompt is enough.
- Saved data is app-private but **not encrypted**.
- Uninstalling the app or clearing its data removes all saved entries.
- `allowBackup` is off, and app data is excluded from cloud backup and device
  transfer, so saved copies do not leave the device.

## Possible next steps (all optional, all offline)

- Live capture of newly arriving SMS via a `RECEIVE_SMS` receiver.
- Tag or search saved entries.
- Rename "Original"/"Edited" labels to a custom title per entry.
- Export the local list to a plain text file you choose.
