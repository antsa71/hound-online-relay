#!/usr/bin/env bash
set -euo pipefail

# Required environment variables:
#   HOUND_DISCOVERY_URL=https://hound-discovery.example.workers.dev/current
#   HOUND_DEVICE_ID=hound-server-1
#   HOUND_WRITE_KEY=write-secret-kept-on-the-server
# Optional: HOUND_LOCAL_URL=http://127.0.0.1:8080

: "${HOUND_DISCOVERY_URL:?Set HOUND_DISCOVERY_URL}"
: "${HOUND_DEVICE_ID:?Set HOUND_DEVICE_ID}"
: "${HOUND_WRITE_KEY:?Set HOUND_WRITE_KEY}"
LOCAL_URL="${HOUND_LOCAL_URL:-http://127.0.0.1:8080}"

command -v cloudflared >/dev/null || { echo "cloudflared is required" >&2; exit 1; }
command -v curl >/dev/null || { echo "curl is required" >&2; exit 1; }

TMP_LOG="$(mktemp)"
cleanup() { rm -f "$TMP_LOG"; }
trap cleanup EXIT INT TERM

cloudflared tunnel --no-autoupdate --url "$LOCAL_URL" 2>&1 | tee "$TMP_LOG" &
TUNNEL_PID=$!
trap 'kill "$TUNNEL_PID" 2>/dev/null || true; cleanup' EXIT INT TERM

while :; do
  TUNNEL_URL="$(sed -nE 's/.*(https:\/\/[-a-zA-Z0-9]+\.trycloudflare\.com).*/\1/p' "$TMP_LOG" | tail -n1)"
  if [[ -n "$TUNNEL_URL" ]]; then
    echo "Publishing $TUNNEL_URL"
    curl --fail-with-body --silent --show-error \
      -X POST "$HOUND_DISCOVERY_URL" \
      -H "Authorization: Bearer $HOUND_WRITE_KEY" \
      -H 'Content-Type: application/json' \
      --data "{\"device\":\"$HOUND_DEVICE_ID\",\"url\":\"$TUNNEL_URL\"}" || true
    : > "$TMP_LOG"
  fi
  if ! kill -0 "$TUNNEL_PID" 2>/dev/null; then
    echo "cloudflared stopped; restarting" >&2
    cloudflared tunnel --no-autoupdate --url "$LOCAL_URL" 2>&1 | tee "$TMP_LOG" &
    TUNNEL_PID=$!
  fi
  sleep 5
done
