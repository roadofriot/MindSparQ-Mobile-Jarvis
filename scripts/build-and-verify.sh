#!/usr/bin/env bash
# ==============================================================================
# MindSparQ Jarvis — Local Build & Verification Gate
# Hardware profile: Intel Core i3 / 10 GB RAM safe execution.
# ==============================================================================
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

echo "======================================================================"
echo "MindSparQ Jarvis — Pre-Flight Quality Gate & Verification"
echo "Host OS: $(uname -s) $(uname -m)"
echo "Memory Constraint: Max 1.5GB JVM Heap, Max 2 Parallel Workers"
echo "======================================================================"

GRADLE_FLAGS="--no-daemon --max-workers=2 -Dorg.gradle.jvmargs=\"-Xmx1536m\""

echo "[1/4] Scanning for hardcoded API keys or plaintext secrets..."
if git grep -E "(AIzaSy[0-9A-Za-z_-]{33}|sk-[0-9A-Za-z]{32,}|ghp_[0-9A-Za-z]{36})" -- ':!docs/' ':!*.md' ':!*.png' 2>/dev/null; then
    echo "ERROR: Potential plaintext secret detected in git tree!"
    exit 1
fi
echo "Secret scan clean. Zero credentials leaked."

echo "[2/4] Running multi-module unit test suites..."
./gradlew \
    :engine:loop:testDebugUnitTest \
    :ai:skills:testDebugUnitTest \
    :ai:mcp:testDebugUnitTest \
    :ai:gateway:testDebugUnitTest \
    --no-daemon --max-workers=2 -Dorg.gradle.jvmargs="-Xmx1536m"

echo "[3/4] Compiling SQLDelight database & Android App..."
./gradlew \
    :core:database:compileDebugKotlin \
    :app:compileOnlineDebugKotlin \
    --no-daemon --max-workers=2 -Dorg.gradle.jvmargs="-Xmx1536m"

echo "[4/4] Assembling Online Debug APK..."
./gradlew assembleOnlineDebug --no-daemon --max-workers=2 -Dorg.gradle.jvmargs="-Xmx1536m"

APK_PATH="app/build/outputs/apk/online/debug/app-online-debug.apk"
if [ -f "$APK_PATH" ]; then
    APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
    SHA256=$(sha256sum "$APK_PATH" | cut -d' ' -f1)
    echo "======================================================================"
    echo "VERIFICATION SUCCESSFUL!"
    echo "Output: $APK_PATH ($APK_SIZE)"
    echo "SHA-256: $SHA256"
    echo "======================================================================"
else
    echo "ERROR: Expected APK not found at $APK_PATH"
    exit 1
fi
