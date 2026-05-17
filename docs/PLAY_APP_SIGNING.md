# Play App Signing Runbook

Goal: hand the existing `release.jks` to Google as the **app signing key**,
keep a separate `upload.jks` we sign CI builds with. Protects against losing
our key — if upload.jks leaks or breaks, Google rotates it for us. App
signing key never leaves Google.

## Terminology

- **App signing key** — signs APKs that ship to users. Held by Google after
  enrollment. Currently `release.jks`.
- **Upload key** — signs AABs we upload to Play Console. Held by us. New:
  `upload.jks`.

## One-time migration

### 1. Generate upload key locally

```bash
./scripts/generate-upload-key.sh .
```

Produces:
- `upload.jks` — keep safe, **never commit**
- `upload-cert.pem` — public cert, upload to Google

Pick a strong password when prompted. Store in a password manager.

### 2. Enroll the existing release.jks as Google's app signing key

In Play Console: **Setup → App integrity → App signing → Use existing app
signing key from Java keystore**.

Google provides the PEPK (Play Encrypt Private Key) tool. Run it locally
against `release.jks`:

```bash
java -jar pepk.jar \
    --keystore=release.jks \
    --alias=<existing-alias> \
    --output=release-encrypted.zip \
    --include-cert \
    --rsa-aes-encryption \
    --encryption-key-path=<google-supplied-public-key.pem>
```

Upload `release-encrypted.zip` in the Console.

### 3. Register upload key cert

Same screen → **Upload key certificate** → upload `upload-cert.pem`.

### 4. Rotate CI to sign with upload.jks

Replace GitHub secrets:

```bash
base64 -w 0 upload.jks    # paste into KEYSTORE_BASE64
```

Update secrets in repo settings:

| Secret | New value |
|---|---|
| `KEYSTORE_BASE64` | base64 of `upload.jks` |
| `KEYSTORE_PASSWORD` | upload keystore password |
| `KEY_ALIAS` | `upload` |
| `KEY_PASSWORD` | upload key password |

No workflow changes — `release.yml` is env-driven (decodes
`KEYSTORE_BASE64` into a temp file and passes path via `KEYSTORE_FILE`).

### 5. Verify

Tag a release. CI signs AAB with upload.jks. Upload to Internal Testing
track. Play Console accepts it because upload cert matches what we
registered. Google re-signs with release.jks before distribution.

## After migration

- `release.jks` stays in a vault (cold backup). Never used in CI again.
- `upload.jks` lives on disk + GitHub secret. If lost, request reset via
  Play Console support.
- All future signing key concerns are Google's problem.
