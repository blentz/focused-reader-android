# Changelog

All notable changes to Focused Reader. Format roughly follows [Keep a Changelog](https://keepachangelog.com/).

## [0.2.1] - 2026-05-18

### Added
- **Document preview** in Pause overlay — replaces the position slider with a scrollable rendering of the source text; tap a line to jump there. Auto-scrolls so the line nearest the viewport centre is the current word.

### Fixed
- Reader respects display cutout (camera hole / notch) when sizing words so glyphs are not clipped on devices with a punch-hole.
- `seekTo` updates state synchronously, eliminating a race where resume could replay the pre-seek word.
- Settings license label corrected from MIT to AGPL-3.0.

## [0.2.0] - 2026-05-16

Major scope cut + dependency / toolchain modernization.

### Removed
- **AccessibilityService + Quick Settings tile** — eliminated the Play Store accessibility-permission review gate and the security risk of an always-bound a11y service.
- **NFC NDEF intent handling** — `android.permission.NFC` dropped, NDEF intent-filters removed from `MainActivity`.
- **TaskerReceiver + `com.focusedreader.permission.IMPORT_TEXT`** custom permission.
- **PDF file support** — `pdfbox-android` dependency removed. The library is unmaintained and bundled a `TrustAllX509TrustManager` that flagged the app in dependency audits. Debug APK shrank ~13.5 MB (42.3 → 28.8 MB).
- `docs/A11Y_PURPOSE.md` (no longer needed).

### Changed
- WPM cap lowered from 900 to **400** per peer-reviewed comprehension research (Rayner et al. 2016, Schotter et al. 2014); comprehension drops sharply above ~400 WPM in RSVP.
- WPM step slider max lowered from 100 to 50.
- AGP 8.5.2 → 8.7.3, Kotlin 2.0.20 → 2.0.21, KSP matched.
- Compose BOM 2024.09.02 → 2024.12.01.
- compileSdk + targetSdk 35 → 36 (Android 16).
- activity-compose, lifecycle, navigation-compose, core-ktx, jsoup, junit5, mockk bumped to latest patch / minor.

### Manifest permissions after this release
Only `VIBRATE` and `INTERNET`.

## [Unreleased]

### Added
- End-of-text auto-exit to Home (no more "No session" idle state).
- About section in Settings (version, links, attributions).
- Export / import session as JSON via Maintenance.
- Reverse-WPM one-shot hint HUD.
- TalkBack semantics on Home / Settings / Reader controls.
- GitHub Actions: CI (unit tests + assemble), Release (signed AAB on `v*` tag), Instrumented (PR + dispatch).
- `version.properties` + `bump-version.sh` for semver releases.
- 16KB page size compliance verified.
- `data-extraction-rules.xml` for Android 12+.
- Marketing assets under `marketing/`: 512px icon, 1024×500 feature graphic, four store screenshots.
- `docs/PRIVACY.md` and `docs/A11Y_PURPOSE.md` for Play Console.
- On-device crash log with share/clear in Settings.
- Release signing config + `docs/RELEASE.md` walkthrough.
- ProGuard rules for Hilt / Compose / Room / Jsoup / PDFBox.
- 50 MB file size cap with `FileTooLarge` toast.
- Cancel button on URL fetch overlay; 10s timeout.

### Changed
- Per-word fit-to-width font sizing (drops session-constant size + length buckets).
- ORP pivot rule: middle alphanumeric (odd) / after-middle (even); non-alphanumerics excluded from count.
- Pivot anchored at 50% of screen width (was 38%).
- Theme system simplified: dropped Pure/Soft palette distinction; only LIGHT / DARK / AUTO.
- `TaskerReceiver` now gated by `com.focusedreader.permission.IMPORT_TEXT` (signature-protected).
- Room schema export enabled; destructive migration fallback removed.

### Removed
- `POST_NOTIFICATIONS` permission (was reserved for future use; YAGNI).

## [0.1.0] - 2026-05-16

Initial unreleased prototype. RSVP reader with three capture paths (Share, Accessibility, Clipboard), single-slot Room session, four accessibility-focused fonts, TTS calibration wizard, sensor pause/resume, haptic feedback, scrub slider, immersive fullscreen, file picker, PDF support, NFC handler, app shortcuts, direct share, Tasker broadcast.
