#!/usr/bin/env bash
set -e
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"

# Milestone 1 / team: use repo-managed hooks (each clone runs setup once)
git config core.hooksPath .githooks

# INSTALL PACKAGES
./mvnw.cmd clean install

# TO BUILD PROJECT -> creates .jar file
./mvnw.cmd package -DskipTests
