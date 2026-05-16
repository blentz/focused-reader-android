# Focused Reader (Android)

POC RSVP speed-reader. Highlight text in another app → Share → Focused Reader.
Volume keys change WPM. Tap or face-down pauses; tap or face-up resumes.

## Build

Requires Android SDK. Set `local.properties`:

```
sdk.dir=/home/blentz/Android/Sdk
```

Then: `./gradlew :app:assembleDebug`

Install: `./gradlew :app:installDebug`

## Capture paths

1. **Share** — from any app's Share sheet → "Focused Reader".
2. **Accessibility** — enable in System Settings → Accessibility → Focused Reader. Trigger via Quick Settings tile "Capture text".
3. **Clipboard** — copy text in any app, return to Focused Reader Home, tap "Paste from clipboard".

## Design

See `docs/superpowers/specs/2026-05-16-focused-reader-android-design.md`.
