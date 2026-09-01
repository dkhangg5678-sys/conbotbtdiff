#!/usr/bin/env bash
set -euo pipefail
SERVER_URL="${SERVER_URL:-https://conbotbtdi.onrender.com}"
BOOTSTRAP_TOKEN="${BOOTSTRAP_TOKEN:-}"
cd "$(dirname "$0")/../android-client"
./gradlew --no-daemon clean assembleDebug -PserverUrl="$SERVER_URL" -PbootstrapToken="$BOOTSTRAP_TOKEN"
apk="app/build/outputs/apk/debug/app-debug.apk"
apksigner verify --verbose --print-certs "$apk"
sha256sum "$apk" | tee "$apk.sha256"
