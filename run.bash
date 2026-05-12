#!/usr/bin/env bash
set -euo pipefail

echo "[run] Building all services (skip tests)..."
./mvnw package -DskipTests

echo "[run] Starting containers..."
docker-compose up -d --build

echo "[run] Services started:"
echo "  user-service     -> http://localhost:8081/api/users/health"
echo "  job-service      -> http://localhost:8082/api/jobs/health"
echo "  proposal-service -> http://localhost:8083/api/proposals/health2"
echo "  contract-service -> http://localhost:8084/api/contracts/health"
echo "  wallet-service   -> http://localhost:8085/api/payouts/health"
echo "  instructions     -> http://localhost:8085/api/instructions"
echo
echo "[run] To stop all services, run: ./stop.bash"