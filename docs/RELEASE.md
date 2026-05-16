# Release Build Guide

## 1. Generate a signing keystore (one-time)

```bash
keytool -genkeypair -v \
  -keystore release.jks \
  -alias focusedreader \
  -keyalg RSA -keysize 4096 \
  -validity 10000 \
  -storetype JKS
```

Keep `release.jks` outside the repo. Back it up safely — losing it means you can never publish an update with the same signature.

## 2. Configure signing for local builds

Copy `keystore.properties.example` → `keystore.properties` (gitignored) at the repo root:

```properties
storeFile=/abs/path/to/release.jks
storePassword=YOUR_STORE_PASSWORD
keyAlias=focusedreader
keyPassword=YOUR_KEY_PASSWORD
```

## 3. Build artifacts

```bash
./gradlew :app:bundleRelease    # produces app/build/outputs/bundle/release/app-release.aab
./gradlew :app:assembleRelease  # produces app/build/outputs/apk/release/app-release.apk
```

Both are signed if `keystore.properties` is present. Without it, the release build still completes minified but unsigned (good enough for ProGuard testing).

## 4. CI signing (GitHub Actions)

Set repo secrets:
- `KEYSTORE_FILE` — path to keystore inside the runner workspace
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

The signing config reads env vars first, falling back to `keystore.properties`. CI workflow decodes a base64-encoded keystore into the workspace then runs `bundleRelease`.

## 5. Verify the .aab

```bash
# Inspect signature
jarsigner -verify -verbose app/build/outputs/bundle/release/app-release.aab

# Convert to .apks for local install testing
bundletool build-apks --bundle=app-release.aab --output=app-release.apks --mode=universal
bundletool install-apks --apks=app-release.apks
```

## 6. Upload to Play Console

Internal testing track first. Submit Permissions Declaration for the Accessibility Service citing `docs/A11Y_PURPOSE.md`. Privacy policy URL: the raw `docs/PRIVACY.md` on the GitHub repo.
