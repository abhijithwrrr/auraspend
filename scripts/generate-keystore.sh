#!/usr/bin/env bash
set -euo pipefail

KEYSTORE_DIR="$(cd "$(dirname "$0")/.." && pwd)/app/release"
KEYSTORE_FILE="$KEYSTORE_DIR/auraspend-keystore.p12"
KEYSTORE_PASSWORD="${1:-AuraSpend2024!}"
KEY_ALIAS="auraspend"
KEY_PASSWORD="$KEYSTORE_PASSWORD"
DAYS_VALID=10000
DNAME="/CN=AuraSpend/OU=AW Builds/O=AW Builds/L=Unknown/ST=Unknown/C=IN"

echo "==> Creating $KEYSTORE_DIR"
mkdir -p "$KEYSTORE_DIR"

echo "==> Generating RSA key pair"
openssl genrsa -out "$KEYSTORE_DIR/temp.key" 4096

echo "==> Generating self-signed certificate"
openssl req -x509 -new -nodes \
  -key "$KEYSTORE_DIR/temp.key" \
  -sha256 -days "$DAYS_VALID" \
  -out "$KEYSTORE_DIR/temp.crt" \
  -subj "$DNAME"

echo "==> Creating PKCS12 keystore"
openssl pkcs12 -export \
  -in "$KEYSTORE_DIR/temp.crt" \
  -inkey "$KEYSTORE_DIR/temp.key" \
  -out "$KEYSTORE_FILE" \
  -name "$KEY_ALIAS" \
  -password "pass:$KEYSTORE_PASSWORD"

echo "==> Cleaning up temp files"
rm -f "$KEYSTORE_DIR/temp.key" "$KEYSTORE_DIR/temp.crt"

echo "==> Keystore created at: $KEYSTORE_FILE"
echo "==> Keystore password: $KEYSTORE_PASSWORD"
echo "==> Key alias: $KEY_ALIAS"
echo ""
echo "To base64-encode for GitHub secret:"
echo "  base64 -w0 $KEYSTORE_FILE | tee auraspend-keystore-base64.txt"
echo ""
echo "Store these secrets in GitHub:"
echo "  - AURASPEND_KEYSTORE_BASE64  (base64 of $KEYSTORE_FILE)"
echo "  - AURASPEND_KEYSTORE_PASSWORD"
echo "  - AURASPEND_KEY_ALIAS"
echo "  - AURASPEND_KEY_PASSWORD"
