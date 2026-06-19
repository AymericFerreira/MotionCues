# Motion Cues

A free, open-source Android app that helps reduce motion sickness in vehicles. It draws a field of
dots over your screen that drift opposite to the vehicle's motion — giving your peripheral vision a
cue that matches what your inner ear feels, similar to Apple's *Vehicle Motion Cues*.

It's designed for reading or browsing as a passenger (Kindle, web, articles…).

## What it does

- A motion-driven **dot overlay** powered by the phone's accelerometer.
- Customization: **color, count, size, motion sensitivity, background dimming**, and a
  **pattern style** (dynamic dots vs. a static reference grid).
- **Auto-start in a moving vehicle** using motion sensors only — plus a manual toggle and a
  **Quick Settings tile**.
- **No data collection, no analytics, no ads, no network access.** Everything runs on-device.

## Important limitation

The dots appear **over normal apps** (readers, browsers). They **cannot** be drawn over the status
bar, notification shade, Quick Settings, or the lock screen — that requires a system feature Android
hasn't released yet. Every third-party app of this kind has the same limit.

## How it works

| Layer | Where | Notes |
|------|-------|-------|
| Motion math | `:core` (pure Kotlin) | `MotionEngine`, `VehicleDetector`, `DotField`, `Settings`. Unit-tested on the JVM. |
| Overlay | `:app` `overlay/` | `OverlayService` (foreground service) owns a `WindowManager` overlay and the sensors; `OverlayView` renders the dots each frame via `Choreographer`. |
| UI | `:app` `ui/` | Jetpack Compose (Material 3): onboarding, settings, about. |
| Persistence | `:app` `settings/` | Jetpack DataStore (Preferences). |

The motion logic lives in a pure-Kotlin module with **no Android dependencies**, so the trickiest
parts are verified by fast JVM unit tests.

## Building

Requires the Android SDK (API 35) and JDK 17.

```bash
./gradlew assembleDebug      # debug APK -> app/build/outputs/apk/debug/
./gradlew test               # all unit tests (core motion math + app)
```

CI (GitHub Actions) builds the APK and runs the tests on every push and pull request.

## Permissions

- **Display over other apps** (`SYSTEM_ALERT_WINDOW`) — to show the dots over your app.
- **Notifications** — for the ongoing foreground-service notification while the overlay runs.
- **Foreground service (special use)** — to keep the overlay alive over other apps.

No location, no internet, no sensors beyond the accelerometer.

## Support

Motion Cues is free. If it helps you, you can support development on
[Ko-fi](https://ko-fi.com/aymericferreira).

## License

[GNU GPL v3](LICENSE).
