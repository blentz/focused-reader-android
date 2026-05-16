# Accessibility Service Purpose — Focused Reader

This document is the Permissions Declaration for Google Play Console regarding Focused Reader's use of `android.permission.BIND_ACCESSIBILITY_SERVICE`.

## Why Focused Reader uses the Accessibility Service

Focused Reader is a Rapid Serial Visual Presentation (RSVP) speed-reader. The user imports text and the app displays it one word at a time at a configurable speed (-100 to 900 WPM) with the optimal recognition point highlighted.

The Accessibility Service exists to let the user capture text from another app on the screen — for example, an article in a web browser, a passage in an e-book reader, or a message in a chat app — and feed it to Focused Reader for speed-reading.

This is the **only** purpose of our Accessibility Service.

## How the service works

1. The service declares `android:canRetrieveWindowContent="true"` and `accessibilityEventTypes="typeWindowStateChanged"`.
2. The service does **not** process or store any accessibility events received in the background. The `onAccessibilityEvent` handler is empty.
3. The service is idle until the user explicitly taps a Quick Settings tile labelled "Capture text".
4. On user tap, the service walks `rootInActiveWindow`'s node tree, concatenates the `text` and `contentDescription` strings from leaf nodes, and writes the result to the app's on-device Room database.
5. The captured text never leaves the user's device. No network call, no third-party SDK, no analytics.

## Less-privileged alternatives provided

To respect users who prefer not to grant Accessibility:

- **Share intent** — the user picks Focused Reader from any app's system share sheet.
- **PROCESS_TEXT intent** — Focused Reader appears in the floating text-selection toolbar.
- **Clipboard paste** — Home screen "Paste from clipboard" button.
- **File picker** — open `.txt`, `.html`, `.md`, or `.pdf` files.
- **NFC tags** — tap an NFC tag carrying text or a URL.
- **Tasker broadcast** — power users can push text via `com.focusedreader.IMPORT_TEXT` broadcast (signature-protected).

The Accessibility Service exists purely as an additional convenience path. The Home screen makes its disabled state visible and offers a one-tap link to enable it; the app fully functions without it.

## Privacy guarantees

- Captured text is written to a single-slot on-device Room database, overwritten on each new capture.
- No captured text is transmitted off the device.
- The app contains no analytics SDK, no crash reporter (other than optional opt-in self-hosted Sentry), no advertising, no telemetry, no user accounts.
- See [PRIVACY.md](PRIVACY.md) for the full data policy.

## Open source verification

The complete source code is published under the GNU AGPL v3 at https://github.com/blentz/focused-reader-android. The Accessibility Service implementation is in `app/src/main/java/com/focusedreader/capture/FocusedReaderA11yService.kt`. Any third party can audit the claim that captured text never leaves the device.

## What the service is **not** used for

- Not for reading other apps' notifications.
- Not for monitoring user activity in the background.
- Not for typing assistance, autofill, or text input.
- Not for any form of automation or scripting outside of explicit user-initiated capture.
- Not for any data sale, sharing, or aggregation.

If Google Play reviewers want to see the service in action, the demo gif in the project README (`docs/demo.gif`) shows the in-reader experience; the capture flow is: enable service in System Settings → open any app with text → drop notification shade → tap "Capture text" tile → Focused Reader stores the visible text and the user opens the app to read it.
