#!/usr/bin/env bash
# ==============================================================================
# MindSparQ Jarvis — Reproducible Release Packaging & Checksum Generator
# ==============================================================================
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

VERSION_TAG="${1:-$(git describe --tags --always --dirty 2>/dev/null || echo "v2.0-enterprise")}"
DIST_DIR="$ROOT_DIR/dist/$VERSION_TAG"
mkdir -p "$DIST_DIR"

echo "======================================================================"
echo "Packaging MindSparQ Jarvis Release: $VERSION_TAG"
echo "Target Artifacts Directory: $DIST_DIR"
echo "======================================================================"

echo "Building reproducible APK..."
./gradlew assembleOnlineDebug --no-daemon --max-workers=2 -Dorg.gradle.jvmargs="-Xmx1536m"

SOURCE_APK="$ROOT_DIR/app/build/outputs/apk/online/debug/app-online-debug.apk"
TARGET_APK="$DIST_DIR/mindsparq-jarvis-$VERSION_TAG.apk"
CHECKSUM_FILE="$DIST_DIR/mindsparq-jarvis-$VERSION_TAG.sha256"
METADATA_FILE="$DIST_DIR/release-metadata.json"

cp "$SOURCE_APK" "$TARGET_APK"
sha256sum "$TARGET_APK" | awk '{print $1}' > "$CHECKSUM_FILE"

FILE_SIZE_BYTES=$(stat -c%s "$TARGET_APK" 2>/dev/null || stat -f%z "$TARGET_APK")
SHA256_HASH=$(cat "$CHECKSUM_FILE")

cat <<EOF > "$METADATA_FILE"
{
  "product": "MindSparQ Mobile Jarvis",
  "version": "$VERSION_TAG",
  "builtAt": "$(date -u +"%Y-%m-%dT%H:%M:%SZ")",
  "gitCommit": "$(git rev-parse HEAD)",
  "artifact": "mindsparq-jarvis-$VERSION_TAG.apk",
  "sizeBytes": $FILE_SIZE_BYTES,
  "sha256": "$SHA256_HASH",
  "minAndroidSdk": 28,
  "targetAndroidSdk": 36,
  "supportedArchitectures": ["arm64-v8a"]
}
EOF

echo "======================================================================"
echo "RELEASE PACKAGED SUCCESSFULLY!"
echo "APK: $TARGET_APK"
echo "SHA-256 Checksum: $SHA256_HASH"
echo "Metadata: $METADATA_FILE"
echo "======================================================================"
