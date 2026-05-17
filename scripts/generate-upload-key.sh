#!/usr/bin/env bash
# Generate a Play App Signing *upload key*.
#
# Background: when you enroll in Play App Signing, Google holds the app
# signing key (release.jks). You sign uploads with a separate upload key
# (upload.jks). Google strips the upload signature and re-signs with the
# app signing key for distribution.
#
# After running this script:
#   1. Keep upload.jks safe (back up + add password to GitHub secrets)
#   2. Upload upload-cert.pem to Play Console:
#        Setup -> App integrity -> App signing -> Upload key certificate
#   3. Rotate GitHub Actions secret KEYSTORE_BASE64 to upload.jks contents:
#        base64 -w 0 upload.jks
#   4. Update KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD secrets to match
#
# Existing release.jks stays as the app signing key Google holds (uploaded
# separately via PEPK tool during enrollment).

set -euo pipefail

OUT_DIR="${1:-.}"
KEYSTORE="${OUT_DIR}/upload.jks"
CERT="${OUT_DIR}/upload-cert.pem"
ALIAS="upload"
VALIDITY_DAYS=10000   # ~27 years; Google requires >= 25 years

if [[ -e "$KEYSTORE" ]]; then
    echo "ERROR: $KEYSTORE already exists. Refusing to overwrite." >&2
    exit 1
fi

echo "Generating upload key at $KEYSTORE"
echo "You will be prompted for a keystore password and key password."
echo "Use the same value for both to keep things simple."
echo

keytool -genkeypair \
    -v \
    -keystore "$KEYSTORE" \
    -alias "$ALIAS" \
    -keyalg RSA \
    -keysize 2048 \
    -validity "$VALIDITY_DAYS" \
    -storetype PKCS12 \
    -dname "CN=Focused Reader Upload, O=Focused Reader, C=US"

echo
echo "Exporting upload certificate to $CERT"
keytool -export -rfc \
    -keystore "$KEYSTORE" \
    -alias "$ALIAS" \
    -file "$CERT"

echo
echo "Done."
echo "Next steps:"
echo "  1. Upload $CERT to Play Console (App integrity -> Upload key certificate)"
echo "  2. base64 -w 0 $KEYSTORE   # paste into GitHub secret KEYSTORE_BASE64"
echo "  3. Update GitHub secrets:"
echo "       KEYSTORE_PASSWORD = <your keystore password>"
echo "       KEY_ALIAS         = $ALIAS"
echo "       KEY_PASSWORD      = <your key password>"
