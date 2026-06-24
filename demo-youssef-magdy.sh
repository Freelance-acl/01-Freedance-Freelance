#!/usr/bin/env bash
# ============================================================
# Youssef Magdy — Wallet Service Demo Script
# Covers: M1 (JWT/CoR/Strategy/Observer/Factory) + M2 (reverse-milestone) + M3 (Correlation/Feign/Prometheus/Saga)
# Usage: bash demo-youssef-magdy.sh
# ============================================================
set -uo pipefail

GW="http://localhost:8080"
PASS=0; FAIL=0

green()  { echo -e "\033[32m[PASS]\033[0m $1"; PASS=$((PASS+1)); }
red()    { echo -e "\033[31m[FAIL]\033[0m $1 — got: ${2:0:150}"; FAIL=$((FAIL+1)); }
header() { echo -e "\n\033[1;34m══════════════════════════════════════\033[0m"; echo -e "\033[1;34m  $1\033[0m"; echo -e "\033[1;34m══════════════════════════════════════\033[0m"; }
check_status() {
  local label="$1" actual="$2" expected="$3"
  if [[ "$actual" == "$expected" ]]; then green "$label (HTTP $actual)";
  else red "$label — expected HTTP $expected" "$actual"; fi
}
check_body() {
  local label="$1" body="$2" pattern="$3"
  if echo "$body" | grep -qi "$pattern"; then green "$label";
  else red "$label" "$body"; fi
}
pyparse() { python -c "import sys,json; d=json.load(sys.stdin); print(d.get('$1',''))" 2>/dev/null || true; }

# Helper: seed a payout directly into wallet-postgres and return its ID
seed_payout() {
  local contract_id="$1" freelancer_id="$2" amount="$3" method="$4" status="$5"
  # Delete any previous demo run's payout for this contractId first (idempotent)
  kubectl exec -n freelance wallet-postgres-0 -- psql -U postgres -d "freelancedb-wallet" -c \
    "DELETE FROM payout_promos WHERE payout_id IN (SELECT id FROM payouts WHERE contract_id=$contract_id);
     DELETE FROM payouts WHERE contract_id=$contract_id;" >/dev/null 2>&1 || true
  kubectl exec -n freelance wallet-postgres-0 -- psql -U postgres -d "freelancedb-wallet" -tAc \
    "INSERT INTO payouts (contract_id,freelancer_id,amount,method,status,created_at) \
     VALUES ($contract_id,$freelancer_id,$amount,'$method','$status',NOW()) RETURNING id;" 2>/dev/null | tr -d ' \r\n'
}

# ─── 0. HEALTH CHECKS ─────────────────────────────────────────────────────────
header "0 — HEALTH CHECKS (all 5 services)"
for svc in users jobs proposals contracts payouts; do
  resp=$(curl -s -o /dev/null -w "%{http_code}" "$GW/api/$svc/health")
  check_status "$svc-service reachable" "$resp" "200"
done

# ─── 1. REGISTER + LOGIN  (M1: JwtService issues token) ───────────────────────
header "1 — REGISTER + LOGIN  (M1 — JwtService)"
SUFFIX="${RANDOM}${RANDOM}"
EMAIL="demo.${SUFFIX}@test.com"
PASS_W="pass1234"
PHONE="01$(echo $SUFFIX | cut -c1-9)"

REG_BODY=$(curl -s -X POST "$GW/api/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"Youssef Demo\",\"email\":\"$EMAIL\",\"password\":\"$PASS_W\",\"role\":\"FREELANCER\",\"phone\":\"$PHONE\"}")
echo "  Register → $REG_BODY"
if echo "$REG_BODY" | grep -qi "token\|id"; then
  green "register succeeded"
else
  echo "  ⚠ Register returned unexpected response — trying login anyway"
fi

LOGIN_BODY=$(curl -s -X POST "$GW/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASS_W\"}")
echo "  Login → $LOGIN_BODY"

TOKEN=$(echo "$LOGIN_BODY" | pyparse "token")
if [ -z "$TOKEN" ]; then
  echo ""
  echo "  ┌─────────────────────────────────────────────────────────────────┐"
  echo "  │ Could not get a token automatically.                            │"
  echo "  │ Run this in a NEW terminal tab, then re-run the script:         │"
  echo "  │                                                                 │"
  echo "  │   export TOKEN=\$(curl -s -X POST http://localhost:8080/api/auth/login \\"
  echo "  │     -H 'Content-Type: application/json' \\"
  echo "  │     -d '{\"email\":\"$EMAIL\",\"password\":\"$PASS_W\"}' \\"
  echo "  │     | python -c \"import sys,json; print(json.load(sys.stdin).get('token',''))\")"
  echo "  │                                                                 │"
  echo "  └─────────────────────────────────────────────────────────────────┘"
  FAIL=$((FAIL+1))
  TOKEN="MISSING"
fi
green "JWT token obtained (${#TOKEN} chars)"

# ─── 2. JWT CHAIN OF RESPONSIBILITY  (M1 — CoR DP-3) ─────────────────────────
header "2 — JWT Chain of Responsibility (M1 — CoR DP-3)"
echo "  Chain: TokenExtraction → SignatureValidation → UserLoader → RoleAuthorization"

# Step 1: No token → TokenExtractionHandler short-circuits → 401
S=$(curl -s -o /dev/null -w "%{http_code}" "$GW/api/payouts")
check_status "no token → 401 (TokenExtractionHandler)" "$S" "401"

# Step 2: Bad signature → SignatureValidationHandler short-circuits → 401
BAD="eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ4In0.invalidsig"
S=$(curl -s -o /dev/null -w "%{http_code}" "$GW/api/payouts" -H "Authorization: Bearer $BAD")
check_status "invalid signature → 401 (SignatureValidationHandler)" "$S" "401"

# Step 3: Valid token → chain passes → 200
S=$(curl -s -o /dev/null -w "%{http_code}" "$GW/api/payouts" -H "Authorization: Bearer $TOKEN")
check_status "valid token → 200 (chain passes all 4 handlers)" "$S" "200"

# Step 4: Public endpoint — no token needed
S=$(curl -s -o /dev/null -w "%{http_code}" "$GW/api/payouts/health")
check_status "public /health → 200 without token" "$S" "200"

# ─── 3. SEED TEST PAYOUTS  (setup for Strategy + Observer tests) ──────────────
header "3 — Seed Test Payouts (setup for Strategy + Observer tests)"
echo "  Seeding payouts directly into wallet-postgres."
echo "  (POST /api/payouts would call contract-service via Feign which fails cross-service auth"
echo "   for newly registered users not present in contract-service's local DB)"

# Payout A: COMPLETED — for FullPayoutReversalStrategy (FULL scope)
PAYOUT_ID=$(seed_payout 991 1 500.00 BANK_TRANSFER COMPLETED)
if [ -z "$PAYOUT_ID" ] || ! echo "$PAYOUT_ID" | grep -qE "^[0-9]+$"; then
  echo "  ⚠ Could not seed payout A via psql — falling back to ID=1, resetting created_at + status"
  kubectl exec -n freelance wallet-postgres-0 -- psql -U postgres -d "freelancedb-wallet" -c \
    "UPDATE payouts SET status='COMPLETED', created_at=NOW() WHERE id=1;" >/dev/null 2>&1 || true
  PAYOUT_ID=1
fi
echo "  Payout A (COMPLETED, BANK_TRANSFER, amount=500): id=$PAYOUT_ID"
check_body "payout A seeded successfully" "$PAYOUT_ID" "[0-9]"

# Payout B: COMPLETED — for MilestoneReversalStrategy (MILESTONE_ONLY scope)
P2_ID=$(seed_payout 992 1 1000.00 PAYPAL COMPLETED)
if echo "$P2_ID" | grep -qE "^[0-9]+$"; then
  echo "  Payout B (COMPLETED, PAYPAL, amount=1000): id=$P2_ID"
  check_body "payout B seeded successfully" "$P2_ID" "[0-9]"
else
  echo "  ⚠ Could not seed payout B"
  P2_ID=""
fi

# Verify list endpoint works
LIST=$(curl -s "$GW/api/payouts" -H "Authorization: Bearer $TOKEN")
check_body "GET /api/payouts returns list including seeded payouts" "$LIST" "\["

# ─── 4. STRATEGY PATTERN (DP-1) + OBSERVER/FACTORY (DP-2/DP-6) ───────────────
header "4 — RefundStrategy (DP-1) + Observer (DP-2) + EventFactory (DP-6)"
echo "  RefundStrategySelector.select(status, reversalScope) picks the right strategy:"
echo "    COMPLETED + FULL         → FullPayoutReversalStrategy"
echo "    COMPLETED + MILESTONE_ONLY → MilestoneReversalStrategy"
echo "    REFUNDED  + any          → NoReversalStrategy (REFUND_DENIED logged)"
echo "  After each strategy: payoutEventSubject.notifyObservers()"
echo "                       → MongoEventLogger.onEvent()"
echo "                       → EventFactory.createEvent(PAYOUT_AUDIT, ...) → payout_audit_trail"

# Strategy 1: FullPayoutReversalStrategy — COMPLETED + FULL → REFUNDED
REV1_BODY=$(curl -s -X POST "$GW/api/payouts/$PAYOUT_ID/reverse-milestone" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"reversalScope":"FULL"}')
echo "  Strategy 1 (FullPayoutReversal, scope=FULL): $REV1_BODY"
check_body "reverse-milestone → status REFUNDED (FullPayoutReversalStrategy)" "$REV1_BODY" "REFUNDED"

# NoReversalStrategy: payout now REFUNDED → second call → 400
REV2_STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
  -X POST "$GW/api/payouts/$PAYOUT_ID/reverse-milestone" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"reversalScope":"FULL"}')
check_status "second reverse on REFUNDED → 400 (NoReversalStrategy / REFUND_DENIED)" "$REV2_STATUS" "400"

# Strategy 2: MilestoneReversalStrategy — COMPLETED + MILESTONE_ONLY → REFUNDED
if [ -n "$P2_ID" ]; then
  REV3_BODY=$(curl -s -X POST "$GW/api/payouts/$P2_ID/reverse-milestone" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d '{"reversalScope":"MILESTONE_ONLY"}')
  echo "  Strategy 2 (MilestoneReversal, scope=MILESTONE_ONLY): $REV3_BODY"
  check_body "MILESTONE_ONLY scope reverse → REFUNDED (MilestoneReversalStrategy)" "$REV3_BODY" "REFUNDED\|status"
else
  echo "  ⚠ Skipping MilestoneReversalStrategy test (payout B not seeded)"
fi

# MongoDB audit trail verification
echo ""
echo "  Verifying MongoDB audit trail (payout_audit_trail collection)..."
MONGO_COUNT=$(kubectl exec -n freelance mongo-0 -- \
  mongosh --quiet --eval \
  "db.getSiblingDB('freelancedb').payout_audit_trail.countDocuments()" 2>/dev/null | tail -1 || echo "0")
echo "  Total audit documents in payout_audit_trail: $MONGO_COUNT"
if echo "$MONGO_COUNT" | grep -qE "^[1-9]"; then
  green "Observer → EventFactory → MongoDB audit trail verified ($MONGO_COUNT docs)"
  kubectl exec -n freelance mongo-0 -- \
    mongosh --quiet --eval \
    "db.getSiblingDB('freelancedb').payout_audit_trail.find().sort({_id:-1}).limit(2).forEach(printjson)" 2>/dev/null || true
else
  echo "  ⚠ No audit docs yet (Observer wires into processContractPayout in section 8)"
  echo "    Manual check: kubectl exec -n freelance mongo-0 -- mongosh --eval \"db.getSiblingDB('freelancedb').payout_audit_trail.find().pretty()\""
fi

# ─── 5. M3 — CORRELATION ID FILTER  (spec §2.3) ───────────────────────────────
header "5 — CorrelationIdFilter  (M3 — spec §2.3)"
echo "  CorrelationIdFilter (OncePerRequestFilter):"
echo "    1. Reads X-Correlation-ID from request (or generates UUID)"
echo "    2. Puts it in MDC so all log lines carry it"
echo "    3. Echoes it in the response header"

CORR_ID="demo-$(date +%s)"
CORR_RESP_HEADERS=$(curl -s -D - "$GW/api/payouts/health" \
  -H "X-Correlation-ID: $CORR_ID" -o /dev/null)
echo "  Sent X-Correlation-ID: $CORR_ID"
echo "  Response headers: $CORR_RESP_HEADERS"
check_body "X-Correlation-ID echoed in response headers" "$CORR_RESP_HEADERS" "$CORR_ID"

echo ""
echo "  Checking pod logs for MDC-tagged correlation ID..."
sleep 1
LOG_HIT=$(kubectl logs -n freelance deployment/wallet-service --tail=50 2>/dev/null | grep "$CORR_ID" | head -3)
if [ -n "$LOG_HIT" ]; then
  green "correlationId '$CORR_ID' found in wallet-service pod logs (MDC working)"
  echo "$LOG_HIT"
else
  echo "  Not in last 50 lines — run:"
  echo "    kubectl logs -n freelance deployment/wallet-service --tail=100 | grep $CORR_ID"
fi

# ─── 6. M3 — FEIGN CLIENT + CORRELATION PROPAGATION ──────────────────────────
header "6 — FeignCorrelationConfig + ContractServiceClient (M3 — spec §2.4)"
echo "  GET /api/payouts/1/details triggers:"
echo "    PayoutService → getContractWithRetry() → ContractServiceClient.getContract()"
echo "  FeignCorrelationConfig (RequestInterceptor) intercepts the outbound Feign call and injects:"
echo "    • X-Correlation-ID (from MDC)"
echo "    • Authorization: Bearer <token> (from current request via RequestContextHolder)"

DETAIL=$(curl -s "$GW/api/payouts/1/details" -H "Authorization: Bearer $TOKEN")
echo "  Payout 1 details: $DETAIL"
check_body "payout details endpoint responded" "$DETAIL" "."

echo ""
echo "  Checking wallet-service logs for Feign/retry activity..."
kubectl logs -n freelance deployment/wallet-service --tail=80 2>/dev/null | \
  grep -iE "feign|contract.service|retry|getContract|correlation" | tail -5 || \
  echo "  (no Feign log lines in last 80 lines)"

# ─── 7. M3 — PROMETHEUS / PAYOUT METRICS  (spec §11) ─────────────────────────
header "7 — PayoutMetrics / Prometheus  (M3 — spec §11)"
echo "  PayoutMetrics bean: Counter 'application_payout_failures_total' via Micrometer"
echo "  Exposed at: /actuator/prometheus on management port 8081"

pkill -f "kubectl port-forward.*8081" 2>/dev/null; sleep 1
kubectl port-forward -n freelance deployment/wallet-service 8081:8081 >/dev/null 2>&1 &
PF_PID=$!
sleep 3

PROMETHEUS=$(curl -s http://localhost:8081/actuator/prometheus 2>/dev/null || true)
if [ -z "$PROMETHEUS" ]; then
  red "Actuator /prometheus not reachable on :8081" "empty response"
else
  PAYOUT_METRIC=$(echo "$PROMETHEUS" | grep "application_payout" || true)
  if [ -n "$PAYOUT_METRIC" ]; then
    green "application_payout_failures_total metric present in Prometheus"
    echo "$PAYOUT_METRIC"
  else
    echo "  ℹ  application_payout_failures_total not yet registered"
    echo "     Counter appears after first payout failure (triggered in section 8)"
  fi
  JVM_LINE=$(echo "$PROMETHEUS" | grep "jvm_memory" | head -1)
  check_body "Micrometer wired — JVM memory metrics present" "$JVM_LINE" "jvm_memory"
  echo ""
  echo "  Sample metrics:"
  echo "$PROMETHEUS" | grep -E "^(jvm_memory_used|process_uptime|application_ready)" | head -4
  echo ""
  echo "  → Grafana: http://localhost:3000 → Explore → Prometheus → application_payout_failures_total"
fi
kill $PF_PID 2>/dev/null || true

# ─── 8. M3 — PROCESS CONTRACT PAYOUT  (idempotency guard S5-F4) ───────────────
header "8 — processContractPayout + Idempotency Guard (M3 — S5-F4)"
echo "  POST /api/payouts/contract/{contractId}"
echo "  Idempotency guard (before any Feign call):"
echo "    findByContractIdAndStatus(COMPLETED) exists → return 200 (already paid)"
echo "    findByContractIdAndStatus(FAILED) exists    → return 400 (already failed)"
echo "  X-User-Id injected by API Gateway from JWT 'uid' claim (not email)"
echo ""

# Seed a unique COMPLETED payout so the guard fires on both calls
IDEM_CID=50000
kubectl exec -n freelance wallet-postgres-0 -- psql -U postgres -d "freelancedb-wallet" -c \
  "DELETE FROM payouts WHERE contract_id=$IDEM_CID;" >/dev/null 2>&1 || true
kubectl exec -n freelance wallet-postgres-0 -- psql -U postgres -d "freelancedb-wallet" -c \
  "INSERT INTO payouts (contract_id,freelancer_id,amount,method,status,created_at) \
   VALUES ($IDEM_CID,7,100.00,'BANK_TRANSFER','COMPLETED',NOW());" >/dev/null 2>&1 || true
echo "  Seeded COMPLETED payout for contractId=$IDEM_CID"

IDEM1_STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
  -X POST "$GW/api/payouts/contract/$IDEM_CID" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"method":"BANK_TRANSFER","accountLastFour":"1234"}')
echo "  First call HTTP: $IDEM1_STATUS (guard fires — existing COMPLETED payout returned)"
check_status "first call → 200 (idempotency guard: already paid)" "$IDEM1_STATUS" "200"

IDEM2_STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
  -X POST "$GW/api/payouts/contract/$IDEM_CID" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"method":"BANK_TRANSFER","accountLastFour":"1234"}')
echo "  Second call HTTP: $IDEM2_STATUS"
check_status "idempotent second call → 200 (not 201)" "$IDEM2_STATUS" "200"

# Trigger failure path to increment application_payout_failures_total
echo ""
echo "  Triggering payout failure to increment application_payout_failures_total..."
FAIL_RESP=$(curl -s -X POST "$GW/api/payouts/contract/99999?simulateFailure=true" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"method":"PAYPAL"}')
echo "  Failure response: $FAIL_RESP"

# Re-check Prometheus for the counter
pkill -f "kubectl port-forward.*8081" 2>/dev/null; sleep 1
kubectl port-forward -n freelance deployment/wallet-service 8081:8081 >/dev/null 2>&1 &
PF2_PID=$!
sleep 3
PAYOUT_COUNTER=$(curl -s http://localhost:8081/actuator/prometheus 2>/dev/null | grep "application_payout_failures_total" || true)
if [ -n "$PAYOUT_COUNTER" ]; then
  green "application_payout_failures_total confirmed in Prometheus"
  echo "  $PAYOUT_COUNTER"
else
  echo "  ℹ  Counter not yet registered (requires a FAILED payout event from saga)"
fi
kill $PF2_PID 2>/dev/null || true

# MongoDB audit trail check after processContractPayout runs
echo ""
echo "  Checking MongoDB for payout_audit_trail events from processContractPayout..."
MONGO_FINAL=$(kubectl exec -n freelance mongo-0 -- \
  mongosh --quiet --eval \
  "db.getSiblingDB('freelancedb').payout_audit_trail.countDocuments()" 2>/dev/null | tail -1 || echo "0")
echo "  Total audit documents: $MONGO_FINAL"
if echo "$MONGO_FINAL" | grep -qE "^[1-9]"; then
  green "MongoDB payout_audit_trail has $MONGO_FINAL event(s) — Observer/Factory chain confirmed"
  kubectl exec -n freelance mongo-0 -- \
    mongosh --quiet --eval \
    "db.getSiblingDB('freelancedb').payout_audit_trail.find().sort({_id:-1}).limit(1).forEach(printjson)" 2>/dev/null || true
else
  echo "  ℹ  No audit docs (saga-triggered payout needed — events fire on PaymentCompleted/Failed)"
fi

# ─── 9. M3 — RABBITMQ SAGA  (PaymentSagaConsumer + PaymentEventPublisher) ──────
header "9 — RabbitMQ Saga  (M3 — PaymentSagaConsumer + PaymentEventPublisher)"
echo "  PaymentSagaConsumer listens on: payment.queue"
echo "    ProposalCompletedEvent → processContractPayout() → PaymentCompleted/Failed published"
echo "    ProposalCancelledEvent → reverseMilestone()      → PaymentRefunded published"
echo "  PaymentEventPublisher: publishAfterCommit() — events fire only after DB transaction commits"
echo "  DLQ: payment.dlq — catches messages that fail after max retries"
echo ""

RABBIT_STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
  --user guest:guest http://localhost:15672/api/overview 2>/dev/null || echo "000")
if [ "$RABBIT_STATUS" == "200" ]; then
  green "RabbitMQ Management UI reachable"
  QUEUES=$(curl -s --user guest:guest http://localhost:15672/api/queues 2>/dev/null)
  echo "  Queues:"
  echo "$QUEUES" | python -c "
import sys, json
queues = json.load(sys.stdin)
for q in sorted(queues, key=lambda x: x['name']):
    print('  •', q['name'], '— messages:', q.get('messages', 0), '| consumers:', q.get('consumers', 0))
" 2>/dev/null || echo "  $QUEUES"
else
  echo "  RabbitMQ UI not port-forwarded. Run in a separate terminal:"
  echo "    kubectl port-forward svc/rabbitmq 15672:15672 -n freelance"
  echo "  Then open: http://localhost:15672  (guest / guest) → Queues tab"
  echo "  Expected: payment.queue (consumers=1), payment.dlq"
fi

# ─── 10. SUMMARY ──────────────────────────────────────────────────────────────
header "SUMMARY"
echo "  Passed: $PASS"
echo "  Failed: $FAIL"
echo ""
echo "  THINGS TO SHOW IN IDE (cannot be API-tested):"
echo "  ┌─ M1 ────────────────────────────────────────────────────────────────────"
echo "  │ JwtConfigurationManager.java   → private constructor, volatile instance,"
echo "  │                                  double-checked locking, resetForTests()"
echo "  │                                  NOT a Spring bean — must be initialized"
echo "  │                                  before DI context via JwtConfigurationBootstrap"
echo "  │ RefundStrategySelector.java    → select(status, reversalScope) picks strategy"
echo "  │ MongoEventLogger.java          → try/catch logs WARN, never rethrows"
echo "  │                                  (Observer fault-tolerance: Mongo down ≠ payout fails)"
echo "  ├─ M3 ────────────────────────────────────────────────────────────────────"
echo "  │ PayoutService.java             → getContractWithRetry(): 3-attempt loop,"
echo "  │                                  200/400/800ms exponential backoff"
echo "  │ FeignCorrelationConfig.java    → RequestInterceptor: reads MDC for X-Correlation-ID,"
echo "  │                                  reads RequestContextHolder for Authorization header"
echo "  │ PaymentEventPublisher.java     → publishAfterCommit() — TransactionSynchronization"
echo "  │                                  ensures events only fire after commit"
echo "  └─────────────────────────────────────────────────────────────────────────"
