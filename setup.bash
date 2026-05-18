#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"

# This project targets Java 25. Auto-detect JDK when JAVA_HOME is unset (macOS, Linux, Git Bash on Windows).
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

  # Use java/javac from PATH when they are a real JDK (Maven needs both if JAVA_HOME is unset).
  if command -v java >/dev/null 2>&1 && command -v javac >/dev/null 2>&1; then
    if java -version >/dev/null 2>&1; then
      java_path="$(command -v java)"
      java_dir="$(cd "$(dirname "$java_path")" && pwd)"
      if [ "$(basename "$java_dir")" = "bin" ]; then
        try_java_home "$(dirname "$java_dir")" && return 0
      fi
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
  echo "[setup] macOS: brew install openjdk" >&2
  echo "[setup] Windows: install JDK 25 and set JAVA_HOME (or use Git Bash + run this script)." >&2
  exit 1
}

prefer_java25

# Use repo-managed hooks (each clone runs setup once)
git config core.hooksPath .githooks

if [ -f "$ROOT/mvnw" ]; then
  if [ -x "$ROOT/mvnw" ]; then
    MVNW="./mvnw"
  else
    chmod +x "$ROOT/mvnw" 2>/dev/null || true
    if [ -x "$ROOT/mvnw" ]; then
      MVNW="./mvnw"
    else
      MVNW="bash ./mvnw"
    fi
  fi
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
