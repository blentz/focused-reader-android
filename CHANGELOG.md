# Changelog

All notable changes to Focused Reader. Format roughly follows [Keep a Changelog](https://keepachangelog.com/).

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
