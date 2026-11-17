"""Exercise the live local saga through the frontend proxy using isolated demo records.

Run: python scripts/verify-demo.py
No real payment provider is used by this project's wallet service.
"""
import base64
import json
import time
import sys
import urllib.error
import urllib.request
from datetime import datetime

BASE = "http://localhost:3000/api"
PASSWORD = "DemoPass123!"


def request(path, token=None, body=None, method=None):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    req = urllib.request.Request(BASE + path, headers=headers,
        data=json.dumps(body).encode() if body is not None else None,
        method=method or ("POST" if body is not None else "GET"))
    try:
        with urllib.request.urlopen(req, timeout=25) as response:
            raw = response.read()
            return json.loads(raw) if raw else None
    except urllib.error.HTTPError as error:
        raise RuntimeError(f"{req.method} {path}: HTTP {error.code}: {error.read().decode()}") from error


def account(role, phone, namespace="demo"):
    email = role.lower() + "@" + namespace + ".freedance.local"
    try:
        auth = request("/auth/login", body={"email": email, "password": PASSWORD})
    except RuntimeError as error:
        if "HTTP 401" not in str(error):
            raise
        auth = request("/auth/register", body={"name": namespace.title() + " " + role.title(), "email": email,
            "password": PASSWORD, "phone": phone, "role": role})
    token = auth["token"]
    claims = json.loads(base64.urlsafe_b64decode(token.split(".")[1] + "=="))
    user = request(f"/users/{claims['uid']}", token)
    assert user["role"] == role
    print(f"PASS login/profile: {email} (user {user['id']})", flush=True)
    return token, user


def wait_for(label, load, predicate):
    for attempt in range(30):
        data = load()
        if predicate(data):
            print("PASS " + label, flush=True)
            return data
        time.sleep(1)
    raise AssertionError(f"Timed out waiting for {label}; last response: {data}")


def main():
    client_token, client = account("CLIENT", "+201099900001")
    freelancer_token, freelancer = account("FREELANCER", "+201099900002")
    title = "Demo verification: Marketplace dashboard " + datetime.now().strftime("%H%M%S")
    job = request("/jobs", client_token, {"title": title,
        "description": "Build a responsive dashboard connected to the live marketplace services.",
        "clientId": client["id"], "category": "WEB_DEV", "budgetMin": 1000, "budgetMax": 1000,
        "requirements": {"description": "React, TypeScript, and backend integration"}})
    assert request(f"/jobs/{job['id']}", client_token)["title"] == title
    print(f"PASS persisted job {job['id']}", flush=True)
    proposal = request("/proposals", freelancer_token, {"jobId": job["id"],
        "freelancerId": freelancer["id"], "bidAmount": 900, "estimatedDays": 7,
        "coverLetter": "I will implement and verify the live dashboard with responsive layouts."})
    print(f"PASS submitted proposal {proposal['id']}", flush=True)
    request(f"/proposals/{proposal['id']}/accept", client_token, method="PUT")
    contracts = wait_for("RabbitMQ created a persisted contract",
        lambda: request("/contracts", client_token),
        lambda values: any(c["proposalId"] == proposal["id"] for c in values))
    contract = next(c for c in contracts if c["proposalId"] == proposal["id"])
    request(f"/proposals/{proposal['id']}/complete", freelancer_token, method="PUT")
    wait_for("contract completed after work completion",
        lambda: request(f"/contracts/{contract['id']}", client_token), lambda c: c["status"] == "COMPLETED")
    payouts = wait_for("wallet created a pending payout",
        lambda: request("/payouts", client_token),
        lambda values: any(p["contractId"] == contract["id"] and p["status"] == "PENDING" for p in values))
    payout = next(p for p in payouts if p["contractId"] == contract["id"])
    paid = request(f"/payouts/contract/{contract['id']}", client_token,
        {"method": "BANK_TRANSFER", "accountLastFour": "0000"})
    assert paid["status"] == "COMPLETED"
    assert paid["amount"] == 900 and paid["transactionDetails"]["platformFee"] == 90
    assert paid["amount"] - paid["transactionDetails"]["platformFee"] == 810
    wait_for("payment event changed proposal to PAID",
        lambda: request(f"/proposals/{proposal['id']}", freelancer_token), lambda p: p["status"] == "PAID")
    persisted = request(f"/payouts/{payout['id']}", freelancer_token)
    assert persisted["status"] == "COMPLETED" and persisted["amount"] == 900
    repeat = request(f"/payouts/contract/{contract['id']}", client_token,
        {"method": "BANK_TRANSFER", "accountLastFour": "0000"})
    assert repeat["id"] == payout["id"]
    print("PASS payout persisted; repeat release did not duplicate it", flush=True)
    print(json.dumps({"jobId": job["id"], "proposalId": proposal["id"],
        "contractId": contract["id"], "payoutId": payout["id"]}), flush=True)


if __name__ == "__main__":
    if "--prepare-recording" in sys.argv:
        account("CLIENT", "+201099900011", "recording")
        account("FREELANCER", "+201099900012", "recording")
    else:
        main()
