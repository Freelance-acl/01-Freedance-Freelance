#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"

# Use repo-managed hooks (each clone runs setup once)
git config core.hooksPath .githooks

echo "[setup] Clean install..."
./mvnw clean install

echo "[setup] Packaging services (skip tests)..."
./mvnw package -DskipTests

echo "[setup] Done."
