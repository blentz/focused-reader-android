# Focused Reader (Android)

A focused, configurable RSVP (Rapid Serial Visual Presentation) speed-reader for Android. Highlight text anywhere on your phone, share it, and read one word at a time at speeds from -100 to 900 WPM, with the optimal recognition point (ORP) highlighted to keep your eyes still.

[![License: AGPL v3](https://img.shields.io/badge/License-AGPL_v3-blue.svg)](https://www.gnu.org/licenses/agpl-3.0)

## Features

- **RSVP engine** — one word per tick, 100–900 WPM forward, 0 = pause, -1 to -100 = reverse
- **ORP highlighting** — middle alphanumeric letter rendered in a contrast color, visually anchored at screen center for zero eye movement
- **Dynamic font sizing** — words scale to fill the screen, with per-word width clamp for very long words and per-bucket scaling (1–8 chars: full, 9–16: 80%, 17+: 60%)
- **Four accessibility-focused fonts** — OpenDyslexic, Lexend, Atkinson Hyperlegible, Inclusive Sans
- **Three text-capture paths**:
  - **Share intent** — system share sheet from any app
  - **Accessibility Service + Quick Settings tile** — capture text from the foreground app on demand
  - **Clipboard** — paste from any source while in the foreground
  - URLs are auto-detected and the page is fetched + extracted (HTML → readable text via Jsoup, plain-text short-circuit for `.txt` URLs)
- **Sensor pause/resume** — flip phone face-down to pause, face-up to resume (configurable, with 2-confirmation debounce to filter noise)
- **TTS integration** — speak each word in sync with the visual tick, with a calibration wizard that binary-searches the device's reliable WPM ceiling
- **Haptic feedback** — per-word or per-punctuation, intensity-scaled, USAGE_HARDWARE_FEEDBACK
- **Per-session persistence** — single-slot Room store: resume where you left off
- **Themes** — light/dark × pure/soft palette combinations
- **Pause-mode scrubbing** — slider to jump to any word position
- **Volume keys** control speed live with an on-screen WPM HUD
- **Keep-screen-awake** toggle for long sessions
- **Permissions UI** — Home banner prompts to enable Accessibility service; notification permission requested on Android 13+

## Build

Requires an Android SDK with platform 35 installed.

1. Copy `local.properties.example` to `local.properties` and set your SDK path:
   ```
   sdk.dir=/path/to/your/Android/Sdk
   ```
2. Build and install on a connected device:
   ```
   ./gradlew :app:installDebug
   ```
3. Unit tests:
   ```
   ./gradlew :app:testDebugUnitTest
   ```

Target: `minSdk 30`, `targetSdk 35`, Kotlin 2.0, Jetpack Compose, Hilt, Room, DataStore, Jsoup.

## Usage

1. **Import text**
   - Share from any app's Share sheet → "Focused Reader" (URLs are fetched + extracted automatically)
   - Or open Focused Reader, tap **Paste from clipboard**
   - Or enable the Accessibility Service (Home banner prompts you) and tap the **Capture text** Quick Settings tile while in the source app
2. **Tap Read** to start playback in landscape
3. **Volume Up / Volume Down** adjusts WPM by the configured step
4. **Tap screen** to pause; **flip face-down** also pauses
5. In Pause overlay: drag slider to jump to any word, **Resume** to keep reading, **Stop** to end

## Settings

- **Speed** — WPM (-100 to 900) and step (5–100)
- **Pause / Resume** — resume countdown delay, face-down sensor toggle
- **Haptic** — off / per-word / per-punctuation, intensity 0–33%
- **TTS** — enable + WPM cap + Calibrate wizard
- **Theme** — light / dark × pure / soft
- **Display** — keep screen awake + font picker (4 fonts)
- **Capture** — deep link to system Accessibility Settings

## Architecture

- `data/` — Room single-slot session + DataStore preferences
- `reader/` — RSVP engine, ORP calculator, WPM math, orientation/haptic/TTS controllers
- `capture/` — Share receiver, Accessibility service + Quick Settings tile, clipboard importer, Jsoup URL fetcher
- `ui/` — Compose screens (Home, Reader, Settings, TTS calibration) and theming
- `nav/` — Compose navigation graph
- `di/` — Hilt module

See `docs/superpowers/specs/2026-05-16-focused-reader-android-design.md` for the original design document and `docs/superpowers/plans/2026-05-16-focused-reader-android.md` for the implementation plan.

## License

This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, version 3.

See [LICENSE](LICENSE) for the full text.
