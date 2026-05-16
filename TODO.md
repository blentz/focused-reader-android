# TODO — Play Store Publishing

Code + CI are done. Remaining work is Google-side admin and Play Console submission.

## Google account / one-time setup

- [ ] Pay $25 Google Play Console developer account fee
- [ ] Complete identity verification (gov ID; 1–3 days)
- [ ] Tax form (W-9 or equivalent) — skip if free app with no IAP

## Play Console — create app

- [ ] Create app entry → "Free" + category **Books & Reference**
- [ ] Upload AAB to **Internal Testing** track first (not production)
  - Source: `app-release.aab` from GitHub release v0.1.2

## Store listing assets (already produced)

- [ ] Title (≤30 chars): `Focused Reader`
- [ ] Short description (≤80 chars): `Speed-read anything you can share — one word at a time.`
- [ ] Full description (≤4000 chars): adapt README features section
- [ ] App icon: `marketing/icon-512.png`
- [ ] Feature graphic: `marketing/feature-graphic-1024x500.png`
- [ ] Phone screenshots (min 2, max 8): `marketing/screen-1..4.png`
- [ ] Optional: 7" + 10" tablet screenshots
- [ ] Contact email
- [ ] Privacy policy URL: `https://raw.githubusercontent.com/blentz/focused-reader-android/main/docs/PRIVACY.md`

## Mandatory questionnaires

- [ ] Content rating (IARC) — expect "Everyone"
- [ ] Target audience — 13+
- [ ] Data safety form — declare "no data collected" per PRIVACY.md
- [ ] News app — No
- [ ] COVID-19 / government / financial — No
- [ ] Ads — No
- [ ] App access — "no login required"

## Hard gate: Accessibility Permissions Declaration

- [ ] Submit form citing [`docs/A11Y_PURPOSE.md`](docs/A11Y_PURPOSE.md):
  - Purpose: capture text from foreground app for speed reading
  - Justify why Share / Clipboard / File / NFC do not cover the use case
  - Record ≤30s video demo: enable a11y → open browser → drop shade → tap "Capture text" tile → open Focused Reader → text plays
- [ ] Expect 1–3 review cycles. Accessibility apps face strict review; rejection possible if reviewer thinks alternatives suffice.

## Recommended before submitting

- [ ] Enroll in **Play App Signing** (Google manages signing key; we keep an upload key)
  - Generate separate `upload.jks`
  - Rotate CI to use upload.jks
  - Give Google our existing `release.jks` as the signing key
  - Protects against key loss
- [ ] Bump `compileSdk` + `targetSdk` to 36 before Aug 2026 (Android 16 deadline)
- [ ] Test on physical Android 11, 14, 15 devices (we tested Android 16 only)
- [ ] Live TalkBack walkthrough on-device

## Optional / nice-to-have

- [ ] F-Droid submission (parallel; no review queue for AGPL code; wants reproducible builds)
- [ ] Localize strings to Spanish + German
- [ ] Better screenshots with UI annotations / device frames

## Time estimate

| Step | Calendar time |
|---|---|
| Account + ID verification | 1–3 days |
| Filling Console forms | 2–4 hours |
| Recording a11y demo video | 30 min |
| First Internal Testing review | 1–2 days |
| Production review (after testing) | 3–7 days |
| Accessibility permission review | up to 2–3 weeks |

**Total: 3–4 weeks** if accessibility review goes smoothly, longer if rejected.
