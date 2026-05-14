#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"

# Use repo-managed hooks (each clone runs setup once)
git config core.hooksPath .githooks

if [ -x "$ROOT/mvnw" ]; then
  MVNW="./mvnw"
elif [ -f "$ROOT/mvnw.cmd" ]; then
  MVNW="./mvnw.cmd"
else
  echo "setup: neither ./mvnw nor ./mvnw.cmd was found." >&2
  exit 1
fi

echo "[setup] Clean install..."
"$MVNW" clean install

echo "[setup] Packaging services (skip tests)..."
"$MVNW" package -DskipTests

echo "[setup] Done."
