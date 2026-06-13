#!/usr/bin/env bash
set -euo pipefail
# Usage:
#   ./stop.bash            — scale down all pods, keep PVCs (data preserved)
#   ./stop.bash --hard     — delete both namespaces entirely (all data lost)
#   ./stop.bash --minikube — soft stop + stop minikube VM

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"

HARD=false
STOP_MINIKUBE=false

while [[ $# -gt 0 ]]; do
  case "$1" in
    --hard)      HARD=true ; shift ;;
    --minikube)  STOP_MINIKUBE=true ; shift ;;
    *)
      echo "[stop] Unknown argument: $1" >&2
      echo "[stop] Usage: ./stop.bash [--hard] [--minikube]" >&2
      exit 1
      ;;
  esac
done

# Stop the background gateway port-forward started by run.bash, if any.
PF_PID_FILE="$ROOT/.gateway-portforward.pid"
if [ -f "$PF_PID_FILE" ]; then
  PF_PID="$(cat "$PF_PID_FILE" 2>/dev/null || true)"
  if [ -n "${PF_PID:-}" ] && kill -0 "$PF_PID" 2>/dev/null; then
    echo "[stop] Stopping gateway port-forward (PID $PF_PID)..."
    kill "$PF_PID" 2>/dev/null || true
  fi
  rm -f "$PF_PID_FILE"
fi

if $HARD; then
  echo "[stop] Hard teardown — deleting namespaces freelance and monitoring..."
  echo "[stop] WARNING: all PVCs and data will be permanently deleted."
  read -r -p "[stop] Are you sure? (yes/no): " confirm
  if [ "$confirm" != "yes" ]; then
    echo "[stop] Aborted."
    exit 0
  fi
  kubectl delete namespace freelance  --ignore-not-found
  kubectl delete namespace monitoring --ignore-not-found
  echo "[stop] Namespaces deleted."
else
  echo "[stop] Scaling down deployments and statefulsets (PVCs preserved)..."

  # Scale down application deployments
  kubectl scale deployment --all --replicas=0 -n freelance  2>/dev/null || true
  kubectl scale deployment --all --replicas=0 -n monitoring 2>/dev/null || true

  # Scale down statefulsets (databases, rabbitmq, loki)
  kubectl scale statefulset --all --replicas=0 -n freelance  2>/dev/null || true
  kubectl scale statefulset --all --replicas=0 -n monitoring 2>/dev/null || true

  echo "[stop] All pods scaled to zero."
  echo "[stop] PVCs and Secrets are preserved — run ./run.bash to start again with existing data."
fi

if $STOP_MINIKUBE; then
  echo "[stop] Stopping minikube VM..."
  minikube stop
  echo "[stop] Minikube stopped."
fi

echo
echo "[stop] Done."
echo
echo "[stop] Other options:"
echo "  ./run.bash                   # restart"
echo "  ./stop.bash --hard           # full wipe (deletes all data)"
echo "  ./stop.bash --minikube       # soft stop + stop minikube"
echo "  minikube delete              # delete the entire minikube VM"
