# Focused Reader (Android) — Design Spec

**Date:** 2026-05-16
**Status:** Approved for implementation planning
**Target:** Android, Kotlin, minSdk 30 (Android 11), single-activity Compose app

## 1. Purpose

A speed-reading screen reader using Rapid Serial Visual Presentation (RSVP). The user imports text from another source (web browser, Kindle, etc.), and the app displays it one word at a time with Spritz-style Optimal Recognition Point (ORP) highlighting, sized to fill the screen in landscape. The user controls speed with the volume keys and pause/resume with screen taps or by flipping the phone.

This document specifies a POC focused on a single reading session at a time.

## 2. Non-Goals

- Multi-document library or history
- Cloud sync, accounts, telemetry, crash reporting
- iOS or other platforms
- EPUB/PDF parsing (input is always plain text)
- In-app text selection from rendered web/PDF — input arrives via OS-level capture paths only

## 3. Inputs (Text Capture)

Three paths feed a single `ImportText(text: String, source: ImportSource)` use case. The use case writes the text to single-slot persistent storage and navigates to the Reader.

### 3.1 Share Intent (primary fallback)
- Manifest declares `ACTION_SEND` + `text/plain`.
- `ShareReceiverActivity` reads `EXTRA_TEXT`, invokes use case, finishes.
- User flow: highlight in Kindle/browser → system Share sheet → Focused Reader.

### 3.2 Accessibility Service (preferred)
- `FocusedReaderA11yService extends AccessibilityService`.
- Idle by default — does NOT continuously observe accessibility events.
- Triggered on demand by: (a) Quick Settings tile, (b) notification action "Capture now".
- On trigger: `rootInActiveWindow` → DFS the node tree → concatenate non-empty `text` / `contentDescription` from leaf nodes with whitespace separators → invoke use case.
- Requires user to enable in system Accessibility settings. Settings screen provides deep link.
- Known limitation: Kindle may obfuscate via DRM rendering; capture may return partial or empty.

### 3.3 Clipboard (foreground fallback)
- "Paste clipboard" button on Home screen.
- Reads `ClipboardManager.primaryClip` only while app is foreground (Android 10+ restriction).

### 3.4 ImportSource enum
`SHARE | A11Y | CLIPBOARD`. Persisted with the row and shown on Home screen as "Last import: A11y, 2 min ago".

## 4. Reader Engine

### 4.1 Tokenization
- Strict whitespace split: `text.split(Regex("\\s+")).filter { it.isNotBlank() }`.
- One token per tick. No phrase grouping, no merging of short connectives. **Hard requirement.**
- Punctuation stays attached to the token it abuts (no stripping).

### 4.2 ORP Pivot (Spritz formula)

| Word length | Pivot index (0-based) |
|---|---|
| 1     | 0 |
| 2–5   | 1 |
| 6–9   | 2 |
| 10–13 | 3 |
| 14+   | 4 |

### 4.3 Layout
Three-column row:
- **Left**: substring before pivot, right-aligned to a fixed screen X (the "pivot anchor", ~38% from the left edge in landscape).
- **Pivot char**: centered on the anchor, rendered in the theme's highlight color.
- **Right**: substring after pivot, left-aligned from the anchor.

Result: the pivot character occupies the same screen X across all words. **Design goal: zero eye movement.**

### 4.4 Font sizing
- On session load, measure the widest token's width at a reference sp size.
- Pick a session-constant sp such that widest token + safety margin = `screenWidthDp * 0.9`.
- Constant for the whole session — never reflow per word.

### 4.5 Tick loop
- Coroutine on a dedicated `Default` dispatcher.
- `delay(60_000L / currentWpm)` between emissions.
- WPM changes mid-loop are picked up on the next tick (read from a `StateFlow`).
- Pause cancels the job; resume relaunches at the persisted index.

### 4.6 Position persistence
- Written to Room on every Pause transition.
- Also written every 5 ticks during Reading (crash resilience).
- Restored on app launch — Home screen shows "Resume from word N/M".

## 5. State Machine

```
       ┌─────┐  import     ┌────────┐  tap | face-down  ┌────────┐
       │Idle │──────────▶ │Reading │ ──────────────────▶│Paused  │
       └─────┘             └────────┘                    └────────┘
          ▲                    ▲                              │
          │ done | Stop        │ countdown elapsed            │
          │                    │                              │ tap | face-up
          │                ┌────────┐                         │
          └────────────────│Resuming│◀────────────────────────┘
                           └────────┘
                                │ tap (cancel)
                                └─────▶ Paused
```

### 5.1 Triggers
- **Reading → Paused**: screen tap OR orientation = face-down.
- **Paused → Resuming**: screen tap OR orientation = face-up.
- **Resuming → Reading**: countdown elapsed (default 3s, configurable 0–10s).
- **Resuming → Paused**: any tap during countdown.
- **Paused → Idle**: pause-menu "Stop".
- **Reading → Idle**: end of text reached.

### 5.2 Sensors
- `SensorManager` listener on `TYPE_DEVICE_ORIENTATION` (API 30+).
- Registered on entry to Reading; unregistered on Idle.
- Debounce: ignore orientation events that occur within 500ms of the previous one to filter accidental flips.

### 5.3 Volume keys
- `Activity.onKeyDown` intercepts `KEYCODE_VOLUME_UP` / `KEYCODE_VOLUME_DOWN` while Reader is foreground.
- Up = current WPM + step; Down = current WPM − step. Clamp to `[100, effectiveMax]` where `effectiveMax = ttsWpmCap` if TTS enabled, else 900.
- Default step 50; user-configurable 10–100.

### 5.4 Pause menu
Options: **Resume**, **Stop** (→ Idle), **Settings**. (Note: spec mentions only Read/Settings; Stop added for usability — flag for user review during implementation.)

### 5.5 Startup menu
On launch with persisted session: **Read** (→ Reader at resume position), **Settings**.
On launch with no session: **Capture** (deep-link explainer), **Settings**.

## 6. TTS Integration

- Mode: **augment visual** — TTS speaks each word as it displays (mode 2).
- Engine: Android `TextToSpeech`, default voice (user can pick in Settings).
- Per tick: `tts.speak(word, QUEUE_FLUSH, params, utteranceId)`. Visual leads — no awaiting completion.
- **WPM cap** applies when TTS is on: current WPM clamped to calibrated upper bound.

### 6.1 Calibration wizard
- Triggered from Settings → TTS → "Calibrate".
- Speaks a fixed 15-word test sentence at successively higher WPM values, binary-searching between 100 and 900.
- After each playback, user taps "Understandable" or "Too fast".
- Final value written to Settings as `ttsWpmCap`.
- Recalibration available anytime.

## 7. Haptic Feedback

- Mode (Settings): `OFF | PER_WORD | PER_PUNCTUATION`.
- `PER_WORD`: tick every word.
- `PER_PUNCTUATION`: tick only on tokens whose last char is in `.!?,;:`.
- Intensity: 0–33% slider, mapped to `VibrationEffect.createOneShot(durationMs, amplitude)` where amplitude = `(intensityPercent / 100) * 255`. Duration = 15ms.

## 8. Theming

Two themes × two palettes = four combinations. ORP is always red.

| Theme | Palette | Background | Word | ORP |
|---|---|---|---|---|
| Light | Pure | `#FFFFFF` | `#000000` | `#FF0000` |
| Light | Soft | `#FAFAFA` | `#121212` | `#E53935` |
| Dark  | Pure | `#000000` | `#FFFFFF` | `#FF0000` |
| Dark  | Soft | `#121212` | `#FAFAFA` | `#E53935` |

Soft palette recommended for OLED screens and reduced eye strain.

## 9. Persistence

**DataStore Preferences** — settings only.

**Room database** — single table `current_session`, single row (id = 0, REPLACE on insert):
```
current_session(
  id INTEGER PRIMARY KEY,            -- always 0
  text TEXT NOT NULL,
  position INTEGER NOT NULL,         -- word index
  source TEXT NOT NULL,              -- enum name
  imported_at INTEGER NOT NULL       -- epoch millis
)
```

Importing new text overwrites the row. POC scope — no library.

## 10. Architecture

Single-Activity Compose app, MVVM, modularized:

- **`:app`** — `MainActivity`, navigation graph, theme provider.
- **`:reader`** — `ReaderViewModel`, `RsvpEngine`, `OrpCalculator`, `WordTokenizer`, `OrientationMonitor`, `HapticController`, `TtsController`.
- **`:capture`** — `ShareReceiverActivity`, `FocusedReaderA11yService`, `QuickSettingsTileService`, `CaptureNotification`, `ClipboardImporter`, `ImportTextUseCase`.
- **`:data`** — Room `SessionDao`, `SettingsDataStore`, repository.
- **`:settings`** — `SettingsScreen`, `TtsCalibrationScreen`, ViewModels.

Concurrency: coroutines + `Flow`. Single `ReaderState` `StateFlow` drives the Reader screen.

DI: Hilt (small surface, mostly default scopes).

## 11. Screens

1. **Home** — last-import preview (first 80 chars), buttons: Read / Capture / Settings.
2. **Reader** — fullscreen landscape (`requestedOrientation = LANDSCAPE`), system bars hidden, ORP word, countdown overlay during Resuming.
3. **Pause overlay** (within Reader) — Resume / Stop / Settings.
4. **Settings** — sections per §12.
5. **TTS Calibration** — wizard per §6.1.
6. **Capture explainer** — onboarding for A11y service + share intent + clipboard.

## 12. Settings Layout

- **Speed**: WPM step (slider 10–100), current WPM (slider 100–900 or cap).
- **Pause/Resume**: resume delay (slider 0–10s), face-down detection toggle.
- **Haptic**: mode (radio), intensity (slider 0–33%).
- **TTS**: enable toggle, voice picker, WPM cap (read-only display), "Calibrate" button.
- **Theme**: light/dark (radio), palette pure/soft (radio).
- **Capture**: A11y status indicator + "Open Accessibility Settings" deep link.

## 13. Error Handling

| Condition | Behavior |
|---|---|
| Empty/whitespace-only import | Toast "No text to read", stay on Home |
| A11y capture returns empty tree | Toast "No readable text in active window" |
| TTS init failure | Disable TTS toggle, show inline error in Settings |
| Sensor unavailable (e.g. emulator) | Fall back to tap-only, log warning |
| Room corruption on open | Recreate DB (lose session), toast notification |
| Volume key held while no Reader foreground | Pass through to system (normal volume control) |

No crash reporting, no analytics in POC.

## 14. Testing

### Unit (JVM, JUnit5 + Turbine)
- `OrpCalculator` — table-driven, every length bucket + boundary.
- `WordTokenizer` — whitespace, punctuation preservation, empty input.
- `WpmToDelay` — boundary values 100, 900, custom cap.
- State machine — every transition edge.

### Instrumented (Espresso + Compose UI test)
- Share intent end-to-end → first word displayed.
- Volume key press → WPM increments and persists.
- Mocked sensor face-down event → state transitions to Paused.
- Settings persistence survives process death.
- TTS calibration wizard completes and writes cap.

### Manual
- A11y capture from Chrome, Kindle, Gmail.
- Real-device sensor flip behavior.
- TTS playback on multiple voices.

## 15. Permissions

| Permission | Purpose |
|---|---|
| `BIND_ACCESSIBILITY_SERVICE` (service-level) | A11y capture |
| `VIBRATE` | Haptic feedback |
| (none for sensor, clipboard foreground, TTS) | — |

No network permission. Offline-only app.

## 16. Build

- Kotlin DSL Gradle, version catalog (`libs.versions.toml`).
- AGP latest stable.
- Compose BOM.
- Hilt, Room, DataStore, Coroutines, Turbine, JUnit5.
- minSdk 30, targetSdk current stable, compileSdk current stable.
- ProGuard/R8 enabled for release.

## 17. Open Items (resolve during implementation)

1. Pause-menu "Stop" inclusion — confirm with user.
2. Quick Settings tile vs. notification action for A11y capture — pick one or ship both.
3. Exact countdown overlay visual style.
