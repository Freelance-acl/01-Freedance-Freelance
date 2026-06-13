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
MINIKUBE_MEMORY="${MINIKUBE_MEMORY:-6144}"
MINIKUBE_CPUS="${MINIKUBE_CPUS:-4}"

echo "[run] Checking minikube..."
if ! minikube status --format='{{.Host}}' 2>/dev/null | grep -q "Running"; then
  echo "[run] Minikube not running — starting (${MINIKUBE_MEMORY} MB RAM, ${MINIKUBE_CPUS} CPUs)..."
  minikube start --memory="${MINIKUBE_MEMORY}" --cpus="${MINIKUBE_CPUS}" --driver=docker 2>/dev/null \
    || minikube start --memory="${MINIKUBE_MEMORY}" --cpus="${MINIKUBE_CPUS}"
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
# Summary
# ──────────────────────────────────────────────────────────────
MINIKUBE_IP="$(minikube ip)"
GW="http://${MINIKUBE_IP}:30080"
echo
echo "================================================================"
echo "  API Gateway -> ${GW}"
echo "================================================================"
echo
echo "  All routes except /api/auth/** require a JWT."
echo "  Get one from /api/auth/login, then pass:  Authorization: Bearer <token>"
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
