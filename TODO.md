# TODO — Play Store Publishing

## Google account / one-time setup

- [ ] Pay $25 Google Play Console developer account fee
- [ ] Complete identity verification (gov ID; 1–3 days)
- [ ] Tax form (W-9 or equivalent) — skip if free app with no IAP

## Play Console — create app

- [ ] Create app entry → "Free" + category **Books & Reference**
- [ ] Upload AAB to **Internal Testing** track first (not production)

## Store listing assets

- [ ] Title (≤30 chars): `Focused Reader`
- [ ] Short description (≤80 chars): `Speed-read anything you can share — one word at a time.`
- [ ] Full description (≤4000 chars): adapt README features section
- [ ] App icon: `marketing/icon-512.png`
- [ ] Feature graphic: `marketing/feature-graphic-1024x500.png`
- [ ] Phone screenshots (min 2, max 8): `marketing/screen-1..4.png`
- [ ] Optional: 7" + 10" tablet screenshots
- [ ] Contact email
- [ ] Privacy policy URL: `https://github.com/blentz/focused-reader-android/blob/main/docs/PRIVACY.md`

## Mandatory questionnaires

- [ ] Content rating (IARC) — expect "Everyone"
- [ ] Target audience — 13+
- [ ] Data safety form — declare "no data collected" per PRIVACY.md
- [ ] News app — No
- [ ] COVID-19 / government / financial — No
- [ ] Ads — No
- [ ] App access — "no login required"

## Signing + device coverage

- [ ] Enroll in **Play App Signing**
  - Generate separate `upload.jks`
  - Rotate CI to use upload.jks
  - Give Google existing `release.jks` as signing key
- [ ] Test on physical Android 11, 14, 15 devices
- [ ] Live TalkBack walkthrough on-device
