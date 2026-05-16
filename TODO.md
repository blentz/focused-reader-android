# TODO — Play Store Publishing + Feature Backlog

Scope-cut v0.2.0 removed the AccessibilityService, Quick Settings tile,
NFC handler, Tasker receiver, and PDF support. The Play Store
"Accessibility Permissions Declaration" gate no longer applies, so the
calendar-time estimate for publication drops dramatically.

## Google account / one-time setup

- [ ] Pay $25 Google Play Console developer account fee
- [ ] Complete identity verification (gov ID; 1–3 days)
- [ ] Tax form (W-9 or equivalent) — skip if free app with no IAP

## Play Console — create app

- [ ] Create app entry → "Free" + category **Books & Reference**
- [ ] Upload AAB to **Internal Testing** track first (not production)
  - Source: `app-release.aab` from GitHub release v0.2.0

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

Note: no AccessibilityService = no NFC declaration = no special
permission declarations required.

## Recommended before submitting

- [ ] Enroll in **Play App Signing** (Google manages signing key; we keep an upload key)
  - Generate separate `upload.jks`
  - Rotate CI to use upload.jks
  - Give Google our existing `release.jks` as the signing key
  - Protects against key loss
- [ ] Test on physical Android 11, 14, 15 devices (we tested Android 16 only)
- [ ] Live TalkBack walkthrough on-device

## Feature backlog (post-v0.2.0)

- [ ] **Multi-session library** — replace single-slot Room with a named
  library + recent-list UI so users can switch between several texts
  without losing their place
- [ ] **EPUB support** — read .epub files via a maintained library
  (e.g. jEPub). PDF was removed in v0.2.0 because pdfbox-android is
  unmaintained; bring it back behind a vetted library if at all.
- [ ] **Localization** — strings to es / de / pt-BR / fr

## Optional / nice-to-have

- [ ] F-Droid submission (parallel; no review queue for AGPL code;
  wants reproducible builds)
- [ ] Better screenshots with UI annotations / device frames

## Time estimate

| Step | Calendar time |
|---|---|
| Account + ID verification | 1–3 days |
| Filling Console forms | 2–4 hours |
| First Internal Testing review | 1–2 days |
| Production review (after testing) | 3–7 days |

**Total: 1–2 weeks** for first publication.
