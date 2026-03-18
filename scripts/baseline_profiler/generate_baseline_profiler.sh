#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ANDROID_DIR="$SCRIPT_DIR/../../android"

echo "==> Verifying benchmark build..."
"$ANDROID_DIR/gradlew" -p "$ANDROID_DIR" :demo-app:assembleBenchmark

echo "==> Generating baseline profile..."
"$ANDROID_DIR/gradlew" -p "$ANDROID_DIR" :demo-app:generateBaselineProfile

echo "==> Baseline profile generated successfully."
