#!/usr/bin/env bash
# Writes app/google-services.json from GOOGLE_SERVICES_JSON_BASE64, or falls back to the
# placeholder so PR/fork builds still compile (Firebase calls will fail at runtime).
set -euo pipefail
target="app/google-services.json"
if [ -n "${GOOGLE_SERVICES_JSON_BASE64:-}" ]; then
  echo "$GOOGLE_SERVICES_JSON_BASE64" | base64 --decode > "$target"
  echo "google-services.json written from secret."
elif [ "${REQUIRE_FIREBASE:-false}" = "true" ]; then
  echo "::error::Secret GOOGLE_SERVICES_JSON_BASE64 is required for release builds."
  exit 1
else
  cp app/google-services.placeholder.json "$target"
  echo "::warning::GOOGLE_SERVICES_JSON_BASE64 not set; using placeholder Firebase config."
fi
