# Privacy Policy — Focused Reader

**Effective date:** 2026-05-16
**App:** Focused Reader (Android)
**Source:** https://github.com/blentz/focused-reader-android

## Summary

Focused Reader does not collect, transmit, or share any personal data. Every piece of text you read with the app stays on your device. There is no analytics, no telemetry, no advertising, no user accounts, and no server we control.

## Data the app handles

| Data | Where it lives | Sent off device? |
|---|---|---|
| Imported reading text (clipboard / share / file) | On-device Room database, single-slot, overwritten on each new import | No |
| Reading position (word index) | On-device Room database | No |
| User settings (WPM, theme, font, haptic, TTS, etc.) | On-device DataStore preferences | No |
| URL content (when shared text is a URL) | Fetched over HTTPS from the URL you provided, parsed in-memory, the extracted text is then stored in the Room database. The original URL is not stored. | The HTTP request goes to the URL you asked for; nothing else is sent. |

The app does not contact any first-party server. It contacts third-party servers only when you explicitly hand it a URL to fetch.

## Permissions and why we need them

| Permission | Purpose |
|---|---|
| `android.permission.INTERNET` | Fetch the page content when you share a URL. |
| `android.permission.VIBRATE` | Haptic tick per word when you enable haptic feedback in Settings. |

## Where text goes

1. You import text via Share / Clipboard / File picker.
2. Focused Reader stores the text in the on-device Room database (single slot, overwritten on each new import).
3. Focused Reader displays it one word at a time on your screen.
4. If you enable TTS, Android's on-device TextToSpeech engine speaks each word. Whether the system TTS engine sends audio to any cloud service is governed by **your selected TTS engine's** privacy policy, not ours.

Text is never:
- Uploaded to any server we run.
- Shared with third parties.
- Used to train any model.
- Indexed for search or recommendation.

## Children

Focused Reader is not directed at children under 13. We do not knowingly collect any personal information from anyone, regardless of age.

## Open source

The app is open source under the GNU AGPL v3 license. The complete source code is available at https://github.com/blentz/focused-reader-android and you can verify every claim in this policy by reading the code.

## Changes

If we materially change this policy, the change will appear in this file's git history. Subscribe to the repository to be notified.

## Contact

Open an issue at https://github.com/blentz/focused-reader-android/issues.
