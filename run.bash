#!/usr/bin/env bash
set -euo pipefail
# run.bash — build images and deploy the application services.
# Run this after setup.bash, or whenever you want to restart/redeploy services.
# By default it rebuilds all Docker images so you never have to think about it.
#
# Usage:
#   ./run.bash                                 — rebuild all images + deploy (default)
#   ./run.bash user-service api-gateway        — rebuild specific services only + deploy
#   ./run.bash --no-rebuild                    — skip image build, just re-apply manifests

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"

# ──────────────────────────────────────────────────────────────
# Parse args
# ──────────────────────────────────────────────────────────────
ALL_MODULES=(user-service job-service proposal-service contract-service wallet-service api-gateway)
REBUILD=true
REBUILD_TARGETS=("${ALL_MODULES[@]}")

while [[ $# -gt 0 ]]; do
  case "$1" in
    --no-rebuild)
      REBUILD=false
      shift
      ;;
    --*)
      echo "[run] Unknown flag: $1" >&2
      echo "[run] Usage: ./run.bash [service ...] [--no-rebuild]" >&2
      exit 1
      ;;
    *)
      # Any non-flag argument is treated as a specific service to rebuild
      if [ ${#REBUILD_TARGETS[@]} -eq ${#ALL_MODULES[@]} ]; then
        REBUILD_TARGETS=()
      fi
      REBUILD_TARGETS+=("$1")
      shift
      ;;
  esac
done

# ──────────────────────────────────────────────────────────────
# Minikube — start if stopped (e.g. after a system reboot)
# ──────────────────────────────────────────────────────────────
# Defaults must match setup.bash. Only used when CREATING a fresh cluster — an
# existing cluster is resumed as-is, because minikube cannot change memory/CPUs
# on an existing cluster and a forced restart can corrupt the API server.
MINIKUBE_MEMORY="${MINIKUBE_MEMORY:-12288}"
MINIKUBE_CPUS="${MINIKUBE_CPUS:-6}"

echo "[run] Checking minikube..."
if ! minikube status --format='{{.Host}}' 2>/dev/null | grep -q "Running"; then
  if docker ps -a --format '{{.Names}}' 2>/dev/null | grep -qx minikube; then
    echo "[run] Resuming existing minikube cluster..."
    minikube start
  else
    echo "[run] Creating minikube cluster (${MINIKUBE_MEMORY} MB RAM, ${MINIKUBE_CPUS} CPUs)..."
    minikube start --memory="${MINIKUBE_MEMORY}" --cpus="${MINIKUBE_CPUS}" --driver=docker 2>/dev/null \
      || minikube start --memory="${MINIKUBE_MEMORY}" --cpus="${MINIKUBE_CPUS}"
  fi
else
  echo "[run] Minikube already running."
fi

echo "[run] Pointing Docker CLI to minikube's Docker daemon..."
eval "$(minikube docker-env)"

# ──────────────────────────────────────────────────────────────
# Build Docker images
# ──────────────────────────────────────────────────────────────
if $REBUILD; then
  echo "[run] Building images: ${REBUILD_TARGETS[*]}"
  for mod in "${REBUILD_TARGETS[@]}"; do
    echo "[run]   -> freelance/${mod}:latest"
    docker build \
      -f docker/Dockerfile.service \
      --build-arg MODULE="$mod" \
      -t "freelance/${mod}:latest" \
      --quiet \
      .
  done
else
  echo "[run] Skipping image build (--no-rebuild)."
fi

# ──────────────────────────────────────────────────────────────
# Deploy app services (idempotent)
# ──────────────────────────────────────────────────────────────
echo "[run] Applying application manifests..."
kubectl apply -f k8s/deployments/
kubectl apply -f k8s/services/
kubectl apply -f k8s/api-gateway/

# ──────────────────────────────────────────────────────────────
# Wait for pods — show status on timeout instead of just failing
# ──────────────────────────────────────────────────────────────
echo "[run] Waiting for services to become ready (up to 5 min)..."
if ! kubectl wait --for=condition=available deployment --all -n freelance --timeout=300s 2>/dev/null; then
  echo
  echo "[run] Some services did not become ready in time. Current pod status:"
  echo
  kubectl get pods -n freelance
  echo
  echo "[run] To see why a pod is failing:"
  echo "  kubectl logs -n freelance deploy/<name>"
  echo "  kubectl logs -n freelance deploy/<name> --previous   # if it crashed"
  echo "  kubectl describe pod -n freelance <pod-name>         # show events"
  echo
  echo "[run] Common fixes:"
  echo "  Pod in CrashLoopBackOff  -> check logs above for the Java exception"
  echo "  Pod in Pending           -> kubectl describe pod ... and check Events"
  echo "  ImagePullBackOff         -> run './run.bash <service>'"
  echo
fi

# ──────────────────────────────────────────────────────────────
# Local access — background port-forward so ONE terminal is enough.
# With the Docker driver (Windows/macOS) the minikube IP is not directly
# reachable from the host, so we forward the gateway to localhost. The forward
# runs detached; run.bash returns control to the same terminal you launched it from.
# Disable with NO_FORWARD=1 ./run.bash ; change the port with GATEWAY_LOCAL_PORT=9000.
# ──────────────────────────────────────────────────────────────
PF_PORT="${GATEWAY_LOCAL_PORT:-8080}"
PF_PID_FILE="$ROOT/.gateway-portforward.pid"

# Stop any port-forward a previous run.bash started (pods may have changed).
if [ -f "$PF_PID_FILE" ]; then
  OLD_PID="$(cat "$PF_PID_FILE" 2>/dev/null || true)"
  if [ -n "${OLD_PID:-}" ] && kill -0 "$OLD_PID" 2>/dev/null; then
    kill "$OLD_PID" 2>/dev/null || true
  fi
  rm -f "$PF_PID_FILE"
fi

if [ "${NO_FORWARD:-0}" != "1" ]; then
  echo "[run] Starting background gateway access on http://localhost:${PF_PORT} ..."
  nohup kubectl port-forward -n freelance svc/api-gateway "${PF_PORT}:8080" \
    > "$ROOT/.gateway-portforward.log" 2>&1 &
  PF_PID=$!
  disown "$PF_PID" 2>/dev/null || true
  echo "$PF_PID" > "$PF_PID_FILE"
  sleep 2
  if kill -0 "$PF_PID" 2>/dev/null; then
    echo "[run] Gateway reachable at http://localhost:${PF_PORT} (port-forward PID $PF_PID)."
  else
    echo "[run] WARNING: port-forward did not stay up — see .gateway-portforward.log."
    echo "[run]          You can still reach the gateway via:  minikube service api-gateway -n freelance --url"
  fi
fi

# ──────────────────────────────────────────────────────────────
# Summary
# ──────────────────────────────────────────────────────────────
MINIKUBE_IP="$(minikube ip)"
GW="http://localhost:${PF_PORT}"
echo
echo "================================================================"
echo "  API Gateway -> ${GW}   (background port-forward; one terminal is enough)"
echo "================================================================"
echo
echo "  You do NOT need a second terminal — run.bash forwarded the gateway to"
echo "  ${GW} and returned control here. Just curl it from this shell."
echo "  Alternative (no forward):  minikube service api-gateway -n freelance --url"
echo "  Stop the forward:          ./stop.bash   (or: kill \$(cat .gateway-portforward.pid))"
echo
echo "  All routes except /api/auth/** require a JWT."
echo "  Get one from /api/auth/login, then pass:  Authorization: Bearer <token>"
echo
echo "  HEALTH CHECK (public, no token — quickest way to confirm the stack is up):"
echo "    curl ${GW}/health                 # gateway -> user-service, returns: OK"
echo "    curl ${GW}/api/users/health       # user-service"
echo "    curl ${GW}/api/jobs/health        # job-service"
echo "    curl ${GW}/api/proposals/health   # proposal-service"
echo "    curl ${GW}/api/contracts/health   # contract-service"
echo "    curl ${GW}/api/payouts/health     # wallet-service"
echo
echo "  PUBLIC (no token needed):"
echo "    POST  ${GW}/api/auth/register"
echo "    POST  ${GW}/api/auth/login"
echo
echo "  AUTHENTICATED:"
echo "    user-service      GET  ${GW}/api/users"
echo "                      GET  ${GW}/api/user-skills"
echo "    job-service       GET  ${GW}/api/jobs"
echo "                      GET  ${GW}/api/jobs/{id}"
echo "    proposal-service  GET  ${GW}/api/proposals"
echo "                      PUT  ${GW}/api/proposals/{id}/complete"
echo "    contract-service  GET  ${GW}/api/contracts"
echo "                      GET  ${GW}/api/contracts/proposal/{id}/active"
echo "    wallet-service    GET  ${GW}/api/payouts"
echo "                      GET  ${GW}/api/promo-codes"
echo
echo "  Quick smoke test:"
echo "    TOKEN=\$(curl -s -X POST ${GW}/api/auth/login \\"
echo "      -H 'Content-Type: application/json' \\"
echo "      -d '{\"email\":\"you@example.com\",\"password\":\"...\"}' | jq -r .token)"
echo "    curl -s \${GW}/api/jobs -H \"Authorization: Bearer \$TOKEN\""
echo
echo "  Grafana      -> http://${MINIKUBE_IP}:30030  (admin / admin)"
echo "  RabbitMQ UI  -> kubectl port-forward svc/rabbitmq 15672:15672 -n freelance"
echo "                  then open http://localhost:15672  (guest / guest)"
echo
echo "================================================================"
echo "[run] Useful commands:"
echo "  kubectl get pods -n freelance -w                        # watch pod status"
echo "  kubectl logs -n freelance deploy/<name> -f              # stream logs"
echo "  kubectl logs -n freelance deploy/<name> --previous      # last crash logs"
echo "  kubectl describe pod -n freelance <pod-name>            # show K8s events"
echo "  ./run.bash user-service                                 # rebuild one service only"
echo "  ./run.bash --no-rebuild                                 # re-apply manifests, skip build"
echo "  ./stop.bash                                             # stop everything"
