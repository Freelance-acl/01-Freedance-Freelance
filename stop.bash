#!/usr/bin/env bash
set -euo pipefail

echo "[stop] Stopping and removing containers..."
docker-compose down
echo "[stop] Done."