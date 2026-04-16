"""Validate team.json shape per Milestone 1 PDF (read JSON from stdin)."""
from __future__ import annotations

import json
import sys

ALLOWED_SERVICES = frozenset(
    {
        "user-service",
        "job-service",
        "proposal-service",
        "contract-service",
        "wallet-service",
    }
)
REQUIRED_KEYS = frozenset({"studentId", "name", "githubUsername", "service"})


def main() -> int:
    try:
        data = json.load(sys.stdin)
    except json.JSONDecodeError as e:
        print("pre-commit: team.json is not valid JSON:", e, file=sys.stderr)
        return 1
    if not isinstance(data, list):
        print("pre-commit: team.json must be a JSON array", file=sys.stderr)
        return 1
    for i, entry in enumerate(data):
        if not isinstance(entry, dict):
            print(f"pre-commit: team.json[{i}] must be an object", file=sys.stderr)
            return 1
        if not REQUIRED_KEYS <= entry.keys():
            missing = sorted(REQUIRED_KEYS - entry.keys())
            print(f"pre-commit: team.json[{i}] missing keys: {missing}", file=sys.stderr)
            return 1
        svc = entry.get("service")
        if svc not in ALLOWED_SERVICES:
            print(f"pre-commit: team.json[{i}] invalid service: {svc!r}", file=sys.stderr)
            return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
