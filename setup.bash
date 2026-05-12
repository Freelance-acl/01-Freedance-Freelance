#!/usr/bin/env bash
set -euo pipefail

echo "[setup] Clean install..."
./mvnw clean install

echo "[setup] Packaging services (skip tests)..."
./mvnw package -DskipTests

echo "[setup] Done."
