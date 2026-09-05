#!/usr/bin/env bash
cd "$(dirname "$0")"
java -jar zzz-codes.jar
code=$?
if [ -z "$DISPLAY" ] && [ -z "$WAYLAND_DISPLAY" ]; then
    echo
    read -rp "Press Enter to close this window (exit code $code)..." 2>/dev/null
fi