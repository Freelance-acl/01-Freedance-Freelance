#!/usr/bin/env bash
set -euo pipefail
# setup.bash — ONE-TIME first-time setup.
# Assumes each teammate has already installed Kubernetes tooling (Docker Desktop,
# minikube, kubectl). This script VERIFIES that tooling is present (and tells you
# what to install if not), then handles everything after that:
#   git hooks -> .env -> Maven build -> minikube cluster -> Docker images -> infra.
#
# After this succeeds, run:  ./run.bash
#
# To rebuild a service and redeploy after code changes:  ./run.bash [service]

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"

# ──────────────────────────────────────────────────────────────
# Prerequisite tooling check.
# Kubernetes (Docker + minikube + kubectl) is assumed already installed on each
# machine. We verify it is present and point to the installer if not, then this
# script handles the rest. (It does not auto-install system packages — that is
# intentionally left to each OS's package manager / Docker Desktop installer.)
# ──────────────────────────────────────────────────────────────
check_prerequisites() {
  local missing=0
  echo "[setup] Checking required tooling..."

  if ! command -v docker >/dev/null 2>&1; then
    echo "[setup]   MISSING docker — install Docker Desktop: https://www.docker.com/products/docker-desktop/" >&2
    missing=1
  elif ! docker info >/dev/null 2>&1; then
    echo "[setup]   docker found but its daemon is not running — start Docker Desktop, then re-run." >&2
    missing=1
  else
    echo "[setup]   ok: docker"
  fi

  if ! command -v minikube >/dev/null 2>&1; then
    echo "[setup]   MISSING minikube — install: https://minikube.sigs.k8s.io/docs/start/" >&2
    missing=1
  else
    echo "[setup]   ok: minikube"
  fi

  if ! command -v kubectl >/dev/null 2>&1; then
    echo "[setup]   MISSING kubectl — install: https://kubernetes.io/docs/tasks/tools/" >&2
    missing=1
  else
    echo "[setup]   ok: kubectl"
  fi

  if [ "$missing" -ne 0 ]; then
    echo "[setup] Install the missing tool(s) above, then re-run ./setup.bash." >&2
    exit 1
  fi
}

check_prerequisites

# ──────────────────────────────────────────────────────────────
# Java 25 detection
# ──────────────────────────────────────────────────────────────
prefer_java25() {
  java_bin() {
    if [ -x "${1}/bin/java" ]; then
      echo "${1}/bin/java"
    elif [ -x "${1}/bin/java.exe" ]; then
      echo "${1}/bin/java.exe"
    else
      return 1
    fi
  }

  try_java_home() {
    local home="$1"
    local java
    java="$(java_bin "$home")" || return 1
    local java_major
    java_major="$("$java" -version 2>&1 | awk -F'[".]' '/version/ {print $2; exit}')"
    if [ "$java_major" = "25" ]; then
      export JAVA_HOME="$home"
      export PATH="${JAVA_HOME}/bin:${PATH}"
      echo "[setup] Using JAVA_HOME=$JAVA_HOME"
      return 0
    fi
    return 1
  }

  if [ -n "${JAVA_HOME:-}" ]; then
    try_java_home "$JAVA_HOME" && return 0
  fi

  if command -v java >/dev/null 2>&1 && command -v javac >/dev/null 2>&1; then
    java_path="$(command -v java)"
    java_dir="$(cd "$(dirname "$java_path")" && pwd)"
    if [ "$(basename "$java_dir")" = "bin" ]; then
      try_java_home "$(dirname "$java_dir")" && return 0
    fi
  fi

  for home in \
    "/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home" \
    "/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home" \
    "/usr/local/opt/openjdk/libexec/openjdk.jdk/Contents/Home" \
    "/usr/local/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home" \
    "/usr/lib/jvm/java-25-openjdk-amd64" \
    "/usr/lib/jvm/temurin-25-jdk-amd64" \
    "/c/Program Files/Java/jdk-25" \
    "/c/Program Files/Java/jdk-25.0.2" \
    "/c/Program Files/Eclipse Adoptium/jdk-25.0.2.10-hotspot"
  do
    try_java_home "$home" && return 0
  done

  if command -v /usr/libexec/java_home >/dev/null 2>&1; then
    home="$(/usr/libexec/java_home -v 25 2>/dev/null || true)"
    if [ -n "$home" ]; then
      try_java_home "$home" && return 0
    fi
  fi

  echo "[setup] Java 25 not found. Set JAVA_HOME to your JDK 25 install." >&2
  exit 1
}

prefer_java25

# ──────────────────────────────────────────────────────────────
# Git hooks + .env
# ──────────────────────────────────────────────────────────────
git config core.hooksPath .githooks

if [ ! -f "$ROOT/.env" ]; then
  if [ -f "$ROOT/.env.example" ]; then
    cp "$ROOT/.env.example" "$ROOT/.env"
    echo "[setup] Created .env from .env.example"
  fi
else
  echo "[setup] .env already exists, skipping."
fi

# ──────────────────────────────────────────────────────────────
# Maven build
# ──────────────────────────────────────────────────────────────
if [ -f "$ROOT/mvnw" ]; then
  chmod +x "$ROOT/mvnw" 2>/dev/null || true
  MVNW=(bash "./mvnw")
  [ -x "$ROOT/mvnw" ] && MVNW=("./mvnw")
elif [ -f "$ROOT/mvnw.cmd" ]; then
  MVNW=("./mvnw.cmd")
else
  echo "[setup] Neither ./mvnw nor ./mvnw.cmd found." >&2
  exit 1
fi

echo "[setup] Maven clean install (verifies the build compiles)..."
"${MVNW[@]}" clean install -DskipTests -q

# ──────────────────────────────────────────────────────────────
# Minikube
# ──────────────────────────────────────────────────────────────
# Minimum supported host: 16 GB RAM. Per the Lab 9 install guide, the
# "Recommended" tier for 16 GB hosts is 6144 MB. CPUs default to 4 (the most
# Docker Desktop typically exposes). Override with MINIKUBE_MEMORY / MINIKUBE_CPUS.
MINIKUBE_MEMORY="${MINIKUBE_MEMORY:-6144}"
MINIKUBE_CPUS="${MINIKUBE_CPUS:-4}"
echo "[setup] Starting minikube (${MINIKUBE_MEMORY} MB RAM, ${MINIKUBE_CPUS} CPUs)..."
minikube start --memory="${MINIKUBE_MEMORY}" --cpus="${MINIKUBE_CPUS}" --driver=docker 2>/dev/null \
  || minikube start --memory="${MINIKUBE_MEMORY}" --cpus="${MINIKUBE_CPUS}"

echo "[setup] Pointing Docker CLI to minikube's Docker daemon..."
eval "$(minikube docker-env)"

# ──────────────────────────────────────────────────────────────
# Docker images — build all 6 modules into minikube's daemon
# ──────────────────────────────────────────────────────────────
MODULES=(user-service job-service proposal-service contract-service wallet-service api-gateway)

echo "[setup] Building Docker images..."
for mod in "${MODULES[@]}"; do
  echo "[setup]   -> freelance/${mod}:latest"
  docker build \
    -f docker/Dockerfile.service \
    --build-arg MODULE="$mod" \
    -t "freelance/${mod}:latest" \
    --quiet \
    .
done
echo "[setup] All images built."

# ──────────────────────────────────────────────────────────────
# Infrastructure — namespaces, secrets, storage, databases, monitoring
# (does NOT deploy app services — that is run.bash)
# ──────────────────────────────────────────────────────────────
echo "[setup] Applying infrastructure manifests..."
kubectl apply -f k8s/namespaces/
kubectl apply -f k8s/secrets/
kubectl apply -f k8s/pvcs/
kubectl apply -f k8s/statefulsets/

echo "[setup] Waiting for databases and message broker to be ready..."
for db in user-postgres job-postgres proposal-postgres contract-postgres wallet-postgres rabbitmq; do
  echo "[setup]   waiting for $db..."
  kubectl wait --for=condition=ready pod -l app="$db" -n freelance --timeout=120s \
    || echo "[setup]   WARNING: $db not ready yet — it may still be starting"
done

kubectl apply -f k8s/configmaps/

echo "[setup] Applying monitoring stack..."
kubectl apply -f k8s/monitoring/loki/
kubectl apply -f k8s/monitoring/prometheus/
kubectl apply -f k8s/monitoring/grafana/
kubectl delete configmap grafana-dashboards-files -n monitoring --ignore-not-found
kubectl create configmap grafana-dashboards-files \
  --from-file=k8s/monitoring/grafana/dashboards/ \
  -n monitoring

echo "[setup] Waiting for monitoring to be ready..."
kubectl wait --for=condition=available deployment --all -n monitoring --timeout=120s \
  || echo "[setup]   WARNING: some monitoring pods not ready yet"

# ──────────────────────────────────────────────────────────────
# Done
# ──────────────────────────────────────────────────────────────
MINIKUBE_IP="$(minikube ip)"
echo
echo "[setup] ✓ Infrastructure ready."
echo
echo "  Next step — deploy and start the application services:"
echo "    ./run.bash"
echo
echo "  To rebuild a service after code changes:"
echo "    ./run.bash user-service            # rebuild just one service + redeploy"
echo "    ./run.bash                         # rebuild all services + redeploy"
echo
echo "  Grafana      -> http://${MINIKUBE_IP}:30030  (admin / admin)"
echo "  RabbitMQ UI  -> kubectl port-forward svc/rabbitmq 15672:15672 -n freelance"
echo "                  then open http://localhost:15672  (guest / guest)"
echo
echo "[setup] To stop everything: ./stop.bash"
