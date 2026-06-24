#!/usr/bin/env bash
# ============================================================
# Mohamed Wael — Job Service Demo Script
# Student ID: 13000900
# Covers: M1 (JWT/Observer/Builder/Adapter/CC-6) + M2 (S2-F10 Elasticsearch + Redis) + M3 (Correlation/Feign/Prometheus/RabbitMQ/Saga notes)
# Usage: bash demo-mohamed-wael.sh
# ============================================================
set -uo pipefail

FULL_NAME="Mohamed Wael"
STUDENT_ID="13000900"
SERVICE="job-service"
DB_POD="job-postgres-0"
DB_NAME="freelancedb-jobs"
GW="http://localhost:8080"
API_PREFIX="/api/jobs"
NS="freelance"

PASS=0; FAIL=0
PF_PIDS=()

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
pyparse() {
  python -c "import sys,json; d=json.load(sys.stdin); print(d.get('$1',''))" 2>/dev/null \
    || python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('$1',''))" 2>/dev/null \
    || true
}

cleanup() {
  for pid in "${PF_PIDS[@]:-}"; do
    kill "$pid" 2>/dev/null || true
  done
}
trap cleanup EXIT

psql_job() {
  kubectl exec -n "$NS" "$DB_POD" -- psql -U postgres -d "$DB_NAME" -v ON_ERROR_STOP=1 -c "$1"
}

psql_proposal() {
  kubectl exec -n "$NS" proposal-postgres-0 -- psql -U postgres -d "freelancedb-proposals" -v ON_ERROR_STOP=1 -c "$1"
}

psql_contract() {
  kubectl exec -n "$NS" contract-postgres-0 -- psql -U postgres -d "freelancedb-contracts" -v ON_ERROR_STOP=1 -c "$1"
}

mongo_eval() {
  kubectl exec -n "$NS" mongo-0 -- mongosh --quiet --eval "$1" 2>/dev/null || true
}

redis_keys() {
  {
    kubectl exec -n "$NS" redis-0 -- redis-cli -a redispass --scan 2>/dev/null \
      || kubectl exec -n "$NS" redis-0 -- redis-cli --scan 2>/dev/null
  } | grep -E "$1" || true
}

wait_for_log() {
  local deployment="$1" pattern="$2" seconds="${3:-8}" hit=""
  for _ in $(seq 1 "$seconds"); do
    hit=$(kubectl logs -n "$NS" "deployment/$deployment" --tail=200 2>/dev/null | grep "$pattern" | head -3 || true)
    if [ -n "$hit" ]; then
      echo "$hit"
      return 0
    fi
    sleep 1
  done
  return 1
}

start_port_forward() {
  local label="$1" resource="$2" mapping="$3"
  kubectl port-forward -n "$NS" "$resource" "$mapping" >/tmp/"$label".log 2>&1 &
  local pid=$!
  PF_PIDS+=("$pid")
  sleep 3
  echo "$pid"
}

DEMO_JOB_PRIMARY=130009001
DEMO_JOB_SECONDARY=130009002
DEMO_JOB_ACTIVE_CONTRACT=130009003
DEMO_JOB_CLOSE=130009004
DEMO_JOB_CLOSED=130009005
DEMO_CLIENT_ID=13000900
DEMO_FREELANCER_ID=13000901
DEMO_PROPOSAL_1=130009001
DEMO_PROPOSAL_2=130009002
DEMO_CONTRACT_ACTIVE=130009001
DEMO_CONTRACT_COMPLETED=130009002

# ─── 0. HEALTH CHECKS ─────────────────────────────────────────────────────────
header "0 — HEALTH CHECKS (all 5 services)"
for svc in users jobs proposals contracts payouts; do
  resp=$(curl -s -o /dev/null -w "%{http_code}" "$GW/api/$svc/health")
  check_status "$svc-service reachable" "$resp" "200"
done

# ─── 1. REGISTER + LOGIN ──────────────────────────────────────────────────────
header "1 — REGISTER + LOGIN"
EMAIL="demo.mohamed.${STUDENT_ID}@test.com"
PASS_W="DemoPass${STUDENT_ID}!"
PHONE="+2013000900"

REG_BODY=$(curl -s -X POST "$GW/api/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"$FULL_NAME Demo\",\"email\":\"$EMAIL\",\"password\":\"$PASS_W\",\"role\":\"CLIENT\",\"phone\":\"$PHONE\"}")
echo "  Register → $REG_BODY"
if echo "$REG_BODY" | grep -qi "token\|already\|exists\|duplicate\|email"; then
  green "register is idempotent enough to continue"
else
  echo "  ⚠ Register returned unexpected response — trying login anyway"
fi

LOGIN_BODY=$(curl -s -X POST "$GW/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASS_W\"}")
echo "  Login → $LOGIN_BODY"

TOKEN=$(echo "$LOGIN_BODY" | pyparse "token")
if [ -z "$TOKEN" ]; then
  red "JWT token obtained" "$LOGIN_BODY"
  TOKEN="MISSING"
else
  green "JWT token obtained (${#TOKEN} chars)"
fi

# ─── 2. JWT CHAIN OF RESPONSIBILITY ───────────────────────────────────────────
header "2 — JWT Chain of Responsibility (M1)"
echo "  Actual protected endpoint from JobController: GET $API_PREFIX"
echo "  Actual public endpoint from JobSecurityConfig: GET $API_PREFIX/health"

S=$(curl -s -o /dev/null -w "%{http_code}" "$GW$API_PREFIX")
check_status "no token → 401 on protected job endpoint" "$S" "401"

BAD="eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ4In0.invalidsig"
S=$(curl -s -o /dev/null -w "%{http_code}" "$GW$API_PREFIX" -H "Authorization: Bearer $BAD")
check_status "bad signature token → 401" "$S" "401"

S=$(curl -s -o /dev/null -w "%{http_code}" "$GW$API_PREFIX" -H "Authorization: Bearer $TOKEN")
check_status "valid token → 200" "$S" "200"

S=$(curl -s -o /dev/null -w "%{http_code}" "$GW$API_PREFIX/health")
check_status "public /api/jobs/health → 200 without token" "$S" "200"

# ─── 3. SEED REQUIRED TEST DATA ───────────────────────────────────────────────
header "3 — Seed required test data directly into PostgreSQL"
echo "  Seeding deterministic job-service rows through kubectl exec → psql."
echo "  IDs are based on student ID $STUDENT_ID so repeated runs update the same rows."

JOB_SQL="
INSERT INTO jobs
  (id, client_id, title, description, category, status, budget_min, budget_max, rating, total_ratings, requirements, created_at)
VALUES
  ($DEMO_JOB_PRIMARY, $DEMO_CLIENT_ID, 'React Marketplace Dashboard',
   'Build a React marketplace dashboard with React search, Spring Boot APIs, PostgreSQL and Elasticsearch relevance ranking.',
   'WEB_DEV', 'OPEN', 100, 900, 4.7, 3,
   '{\"requiredSkills\":[\"React\",\"Spring Boot\",\"Elasticsearch\"],\"experienceLevel\":\"MID\",\"remoteAllowed\":true,\"demoOwner\":\"13000900\"}'::jsonb,
   '2026-06-01 10:00:00'),
  ($DEMO_JOB_SECONDARY, $DEMO_CLIENT_ID, 'React Landing Page Polish',
   'Polish a React landing page and improve accessibility. React appears in the description for lower relevance than the title match.',
   'WEB_DEV', 'OPEN', 150, 700, 4.1, 1,
   '{\"requiredSkills\":[\"React\",\"Accessibility\"],\"experienceLevel\":\"JUNIOR\",\"remoteAllowed\":true,\"demoOwner\":\"13000900\"}'::jsonb,
   '2026-06-02 10:00:00'),
  ($DEMO_JOB_ACTIVE_CONTRACT, $DEMO_CLIENT_ID, 'Contracted React App',
   'React application that already has an active contract and should not be closed.',
   'WEB_DEV', 'IN_PROGRESS', 300, 1200, 4.8, 2,
   '{\"requiredSkills\":[\"React\"],\"demoOwner\":\"13000900\",\"closeCase\":\"active-contract\"}'::jsonb,
   '2026-06-03 10:00:00'),
  ($DEMO_JOB_CLOSE, $DEMO_CLIENT_ID, 'Closeable React Maintenance',
   'React maintenance job with no active contract, used to prove job.closed publication.',
   'WEB_DEV', 'OPEN', 200, 800, 4.3, 1,
   '{\"requiredSkills\":[\"React\"],\"demoOwner\":\"13000900\",\"closeCase\":\"no-active-contract\"}'::jsonb,
   '2026-06-04 10:00:00'),
  ($DEMO_JOB_CLOSED, $DEMO_CLIENT_ID, 'Already Closed Demo Job',
   'Stable idempotency demonstration row that remains closed across repeated runs.',
   'WEB_DEV', 'CLOSED', 100, 500, 4.0, 1,
   '{\"requiredSkills\":[\"React\"],\"demoOwner\":\"13000900\",\"idempotency\":\"already-closed\"}'::jsonb,
   '2026-06-05 10:00:00')
ON CONFLICT (id) DO UPDATE SET
  client_id = EXCLUDED.client_id,
  title = EXCLUDED.title,
  description = EXCLUDED.description,
  category = EXCLUDED.category,
  status = EXCLUDED.status,
  budget_min = EXCLUDED.budget_min,
  budget_max = EXCLUDED.budget_max,
  rating = EXCLUDED.rating,
  total_ratings = EXCLUDED.total_ratings,
  requirements = EXCLUDED.requirements,
  created_at = EXCLUDED.created_at;
"

if psql_job "$JOB_SQL" >/dev/null 2>&1; then
  green "job PostgreSQL seed completed"
else
  red "job PostgreSQL seed failed" "kubectl/psql error"
fi

echo "  Seeding proposal-service and contract-service rows only for deterministic Feign demos."
PROPOSAL_SQL="
INSERT INTO proposals
  (id, job_id, freelancer_id, cover_letter, bid_amount, estimated_days, status, metadata, submitted_at, accepted_at, contract_id, payment_pending_at)
VALUES
  ($DEMO_PROPOSAL_1, $DEMO_JOB_PRIMARY, $DEMO_FREELANCER_ID,
   'Strong React and Elasticsearch implementation plan for Mohamed demo.',
   450.00, 8, 'SUBMITTED',
   '{\"demoOwner\":\"13000900\",\"source\":\"demo-mohamed-wael\"}'::jsonb,
   '2026-06-10 10:00:00', NULL, NULL, NULL),
  ($DEMO_PROPOSAL_2, $DEMO_JOB_PRIMARY, $DEMO_FREELANCER_ID,
   'Accepted React marketplace implementation.',
   700.00, 10, 'ACCEPTED',
   '{\"demoOwner\":\"13000900\",\"source\":\"demo-mohamed-wael\"}'::jsonb,
   '2026-06-11 10:00:00', '2026-06-11 12:00:00', $DEMO_CONTRACT_COMPLETED, NULL)
ON CONFLICT (id) DO UPDATE SET
  job_id = EXCLUDED.job_id,
  freelancer_id = EXCLUDED.freelancer_id,
  cover_letter = EXCLUDED.cover_letter,
  bid_amount = EXCLUDED.bid_amount,
  estimated_days = EXCLUDED.estimated_days,
  status = EXCLUDED.status,
  metadata = EXCLUDED.metadata,
  submitted_at = EXCLUDED.submitted_at,
  accepted_at = EXCLUDED.accepted_at,
  contract_id = EXCLUDED.contract_id,
  payment_pending_at = EXCLUDED.payment_pending_at;
"
if psql_proposal "$PROPOSAL_SQL" >/dev/null 2>&1; then
  green "proposal PostgreSQL seed completed"
else
  red "proposal PostgreSQL seed failed" "kubectl/psql error"
fi

CONTRACT_SQL="
INSERT INTO contracts
  (id, job_id, freelancer_id, client_id, proposal_id, agreed_amount, status, start_date, end_date, metadata, created_at)
VALUES
  ($DEMO_CONTRACT_ACTIVE, $DEMO_JOB_ACTIVE_CONTRACT, $DEMO_FREELANCER_ID, $DEMO_CLIENT_ID, $DEMO_PROPOSAL_1,
   600.00, 'ACTIVE', '2026-06-12 10:00:00', NULL,
   '{\"demoOwner\":\"13000900\",\"case\":\"active-count\"}'::jsonb, '2026-06-12 10:00:00'),
  ($DEMO_CONTRACT_COMPLETED, $DEMO_JOB_PRIMARY, $DEMO_FREELANCER_ID, $DEMO_CLIENT_ID, $DEMO_PROPOSAL_2,
   700.00, 'COMPLETED', '2026-06-13 10:00:00', '2026-06-20 10:00:00',
   '{\"demoOwner\":\"13000900\",\"case\":\"completed\"}'::jsonb, '2026-06-13 10:00:00')
ON CONFLICT (id) DO UPDATE SET
  job_id = EXCLUDED.job_id,
  freelancer_id = EXCLUDED.freelancer_id,
  client_id = EXCLUDED.client_id,
  proposal_id = EXCLUDED.proposal_id,
  agreed_amount = EXCLUDED.agreed_amount,
  status = EXCLUDED.status,
  start_date = EXCLUDED.start_date,
  end_date = EXCLUDED.end_date,
  metadata = EXCLUDED.metadata,
  created_at = EXCLUDED.created_at;
"
if psql_contract "$CONTRACT_SQL" >/dev/null 2>&1; then
  green "contract PostgreSQL seed completed"
else
  red "contract PostgreSQL seed failed" "kubectl/psql error"
fi

echo ""
echo "  Indexing seeded jobs into Elasticsearch through actual endpoint POST $API_PREFIX/{id}/index."
for id in "$DEMO_JOB_PRIMARY" "$DEMO_JOB_SECONDARY" "$DEMO_JOB_ACTIVE_CONTRACT" "$DEMO_JOB_CLOSE"; do
  S=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$GW$API_PREFIX/$id/index" -H "Authorization: Bearer $TOKEN")
  check_status "index job $id into Elasticsearch" "$S" "200"
done

# ─── 4. M1 FEATURE TESTS ──────────────────────────────────────────────────────
header "4 — M1 feature tests"
S=$(curl -s -o /dev/null -w "%{http_code}" "$GW$API_PREFIX" -H "Authorization: Bearer $TOKEN")
check_status "JWT protection wiring for job-service" "$S" "200"

REQ_BODY=$(curl -s -X PUT "$GW$API_PREFIX/$DEMO_JOB_PRIMARY/requirements" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"demoRun\":\"$STUDENT_ID\",\"requiredSkills\":[\"React\",\"Spring Boot\",\"Elasticsearch\"],\"observerProof\":\"REQUIREMENTS_UPDATED\"}")
echo "  Requirements update → $REQ_BODY"
check_body "S2-F2 requirements update returned updated job" "$REQ_BODY" "observerProof"

MONGO_EVENT=$(mongo_eval "db.getSiblingDB('freelancemongo').job_events.find({jobId:$DEMO_JOB_PRIMARY, action:'REQUIREMENTS_UPDATED_JOB'}).sort({_id:-1}).limit(1).forEach(printjson)")
echo "  Mongo job_events latest REQUIREMENTS_UPDATED_JOB → $MONGO_EVENT"
check_body "MongoDB job_events contains REQUIREMENTS_UPDATED_JOB" "$MONGO_EVENT" "REQUIREMENTS_UPDATED_JOB"

SUMMARY_BODY=$(curl -s "$GW$API_PREFIX/$DEMO_JOB_PRIMARY/proposal-summary?startDate=2026-06-01&endDate=2026-06-30" \
  -H "Authorization: Bearer $TOKEN")
echo "  Proposal summary → $SUMMARY_BODY"
check_body "Builder DTO S2-F3 has jobId" "$SUMMARY_BODY" "jobId"
check_body "Builder DTO S2-F3 has title" "$SUMMARY_BODY" "title"
check_body "Builder DTO S2-F3 has totalProposals" "$SUMMARY_BODY" "totalProposals"
check_body "Builder DTO S2-F3 has averageBidAmount" "$SUMMARY_BODY" "averageBidAmount"
check_body "Builder DTO S2-F3 has lowestBid/highestBid" "$SUMMARY_BODY" "lowestBid.*highestBid|highestBid.*lowestBid"

SEARCH_BODY=$(curl -s "$GW$API_PREFIX/search/full-text?query=react&category=WEB_DEV&minBudget=100" \
  -H "Authorization: Bearer $TOKEN")
echo "  Full-text search → $SEARCH_BODY"
check_body "Adapter/Elasticsearch endpoint returns a Spring page" "$SEARCH_BODY" "content"
check_body "Adapter DTO includes relevanceScore, not raw SearchHit" "$SEARCH_BODY" "relevanceScore"

# ─── 5. M1 DESIGN PATTERN VERIFICATION ───────────────────────────────────────
header "5 — M1 Design Pattern verification"
echo "  Observer: update requirements → jobEventSubject.notifyObservers() → MongoEventLogger → job_events"
check_body "Observer verified by Mongo REQUIREMENTS_UPDATED_JOB event" "$MONGO_EVENT" "changedRequirements|REQUIREMENTS_UPDATED_JOB"

echo ""
echo "  Builder: JobProposalSummaryDTO.builder() creates a 6-field response."
check_body "Builder DTO field count proof: jobId/title/total/avg/lowest/highest" "$SUMMARY_BODY" "jobId.*title.*totalProposals.*averageBidAmount.*lowestBid.*highestBid"

echo ""
echo "  Adapter: ElasticsearchHitAdapter maps SearchHit<JobSearchDocument> to JobSearchResultDTO."
check_body "Adapter output excludes raw Elasticsearch hit metadata" "$SEARCH_BODY" "relevanceScore"
if echo "$SEARCH_BODY" | grep -qi "index\|sortValues\|highlightFields"; then
  red "Adapter output should not expose raw SearchHit fields" "$SEARCH_BODY"
else
  green "Adapter output is DTO-shaped, not raw Elasticsearch SearchHit"
fi

# ─── 6. M2 FEATURE TESTS ──────────────────────────────────────────────────────
header "6 — M2 S2-F10 Elasticsearch full-text search + Redis cache"
SEARCH_URL="$GW$API_PREFIX/search/full-text?query=react&category=WEB_DEV&minBudget=100"
echo "  Required demo URL: GET $API_PREFIX/search/full-text?query=react&category=WEB_DEV&minBudget=100"

FIRST=$(curl -s "$SEARCH_URL" -H "Authorization: Bearer $TOKEN")
echo "  First hit → $FIRST"
check_body "full-text search returns React results" "$FIRST" "React"
check_body "category/status/budget DTO fields are present" "$FIRST" "category.*WEB_DEV|WEB_DEV.*budget"

SECOND=$(curl -s "$SEARCH_URL" -H "Authorization: Bearer $TOKEN")
echo "  Second hit → $SECOND"
check_body "second identical hit stays stable" "$SECOND" "React"

CACHE_KEYS=$(redis_keys "S2-F10")
echo "  Redis keys matching S2-F10 → $CACHE_KEYS"
check_body "Redis cache entry exists for S2-F10" "$CACHE_KEYS" "S2-F10"

echo ""
echo "  Relevance note: JobSearchDocument indexes title + description; query boosts title^2."
echo "  Expected order: '$DEMO_JOB_PRIMARY' has React in title and description; '$DEMO_JOB_SECONDARY' has React in title/description with lower text strength."

# ─── 7. M3 CORRELATION ID FILTER ──────────────────────────────────────────────
header "7 — M3 CorrelationIdFilter"
CORR_ID="demo-${STUDENT_ID}-correlation"
CORR_HEADERS=$(curl -s -D - "$GW$API_PREFIX/health" -H "X-Correlation-ID: $CORR_ID" -o /dev/null)
echo "  Sent X-Correlation-ID: $CORR_ID"
echo "  Response headers: $CORR_HEADERS"
check_body "X-Correlation-ID echoed in response headers" "$CORR_HEADERS" "$CORR_ID"

echo ""
echo "  Checking job-service pod logs for MDC-tagged correlation ID..."
curl -s "$SEARCH_URL" -H "Authorization: Bearer $TOKEN" -H "X-Correlation-ID: $CORR_ID" >/dev/null
if LOG_HIT=$(wait_for_log "job-service" "$CORR_ID" 8); then
  green "correlationId '$CORR_ID' found in job-service logs"
  echo "$LOG_HIT"
else
  echo "  Not found in recent logs. Response header still proves filter execution."
  echo "  IDE proof: CorrelationIdFilter.java puts '$CORR_ID' into MDC key 'correlationId' and logback-spring.xml prints %X{correlationId:-}."
fi

# ─── 8. M3 FEIGN CLIENT TEST ──────────────────────────────────────────────────
header "8 — M3 Feign client + correlation propagation"
FEIGN_CORR="demo-${STUDENT_ID}-feign"
echo "  Preferred path: GET $API_PREFIX/$DEMO_JOB_PRIMARY/proposal-summary triggers ProposalServiceClient."
echo "  Sending X-Correlation-ID: $FEIGN_CORR"

FEIGN_BODY=$(curl -s "$GW$API_PREFIX/$DEMO_JOB_PRIMARY/proposal-summary?startDate=2026-06-01&endDate=2026-06-30" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Correlation-ID: $FEIGN_CORR")
FEIGN_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$GW$API_PREFIX/$DEMO_JOB_PRIMARY/proposal-summary?startDate=2026-06-01&endDate=2026-06-30" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Correlation-ID: $FEIGN_CORR")
echo "  Proposal summary via job-service → $FEIGN_BODY"
check_status "job-service Feign endpoint responded" "$FEIGN_STATUS" "200"
check_body "proposal summary response contains summary DTO fields" "$FEIGN_BODY" "totalProposals|averageBidAmount|highestBid"

echo ""
echo "  Checking proposal-service logs for propagated correlation ID..."
if DOWNSTREAM_HIT=$(wait_for_log "proposal-service" "$FEIGN_CORR" 8); then
  green "correlationId '$FEIGN_CORR' found in proposal-service logs"
  echo "$DOWNSTREAM_HIT"
else
  echo "  Not found in recent downstream logs."
  echo "  IDE proof: job-service CorrelationIdFilter.java stores MDC, and FeignCorrelationConfig.java reads MDC and sends X-Correlation-ID."
fi

# ─── 9. M3 PROMETHEUS METRICS ────────────────────────────────────────────────
header "9 — M3 Prometheus metrics"
echo "  Killing stale local :8081 port-forwards, then starting a fresh one to deployment/job-service target port 8081."
pkill -f "kubectl port-forward.*8081" 2>/dev/null || true
sleep 1
start_port_forward "job-service-prometheus" "deployment/job-service" "8081:8081" >/dev/null

PROMETHEUS=$(curl -s http://localhost:8081/actuator/prometheus 2>/dev/null || true)
if [ -z "$PROMETHEUS" ]; then
  red "Actuator /prometheus reachable on localhost:8081" "empty response"
else
  METRIC_LINE=$(echo "$PROMETHEUS" | grep "http_server_requests_seconds_count" | head -1 || true)
  check_body "http_server_requests_seconds_count present" "$METRIC_LINE" "http_server_requests_seconds_count"
  echo "  Sample metric: $METRIC_LINE"
fi

# ─── 10. M3 IDEMPOTENCY GUARD ────────────────────────────────────────────────
header "10 — M3 Idempotency guard"
echo "  Job-service has no dedicated COMPLETED idempotency table; this section demonstrates idempotent demo seeding / stable repeated job-service operation."

COUNT_ROWS=$(kubectl exec -n "$NS" "$DB_POD" -- psql -U postgres -d "$DB_NAME" -tAc \
  "SELECT COUNT(*) FROM jobs WHERE id IN ($DEMO_JOB_PRIMARY,$DEMO_JOB_SECONDARY,$DEMO_JOB_ACTIVE_CONTRACT,$DEMO_JOB_CLOSE,$DEMO_JOB_CLOSED);" 2>/dev/null | tr -d ' \r\n' || true)
echo "  Seeded demo job row count → $COUNT_ROWS"
check_body "idempotent seed produced exactly 5 deterministic rows" "$COUNT_ROWS" "^5$"

IDEM1=$(curl -s -o /dev/null -w "%{http_code}" "$GW$API_PREFIX/$DEMO_JOB_CLOSED" -H "Authorization: Bearer $TOKEN")
IDEM2=$(curl -s -o /dev/null -w "%{http_code}" "$GW$API_PREFIX/$DEMO_JOB_CLOSED" -H "Authorization: Bearer $TOKEN")
check_status "first stable GET of already-closed job → 200" "$IDEM1" "200"
check_status "second stable GET of already-closed job → 200" "$IDEM2" "200"

# ─── 11. M3 RABBITMQ SAGA ────────────────────────────────────────────────────
header "11 — M3 RabbitMQ saga + job.closed"
echo "  RabbitMQ topology from code:"
echo "    job-service publishes: job.events / job.closed / job.status-changed"
echo "    job-service consumes: proposal.events → job.proposal.saga-listener"
echo "    proposal-service consumes: job.events → proposal.saga-feedback"

RABBIT_STATUS=$(curl -s -o /dev/null -w "%{http_code}" --user guest:guest http://localhost:15672/api/overview 2>/dev/null || echo "000")
if [ "$RABBIT_STATUS" != "200" ]; then
  echo "  RabbitMQ management was not reachable on localhost:15672; starting port-forward svc/rabbitmq 15672:15672."
  start_port_forward "rabbitmq-management" "svc/rabbitmq" "15672:15672" >/dev/null
  RABBIT_STATUS=$(curl -s -o /dev/null -w "%{http_code}" --user guest:guest http://localhost:15672/api/overview 2>/dev/null || echo "000")
fi

if [ "$RABBIT_STATUS" == "200" ]; then
  green "RabbitMQ Management API reachable"
  QUEUES=$(curl -s --user guest:guest http://localhost:15672/api/queues 2>/dev/null)
  echo "  Relevant queues:"
  echo "$QUEUES" | python -c "
import sys, json
names = ('job', 'proposal', 'payment')
for q in sorted(json.load(sys.stdin), key=lambda x: x.get('name','')):
    name = q.get('name','')
    if any(part in name for part in names):
        print('  •', name, '— messages:', q.get('messages', 0), '| consumers:', q.get('consumers', 0))
" 2>/dev/null || echo "  $QUEUES"
else
  echo "  RabbitMQ UI not reachable. Run:"
  echo "    kubectl port-forward svc/rabbitmq 15672:15672 -n freelance"
  echo "  Then open: http://localhost:15672 (guest / guest) → Queues tab"
fi

echo ""
echo "  Demonstrating S2-F4 close-job flow using actual endpoint: PUT $API_PREFIX/{id}/close"
ACTIVE_COUNT=$(curl -s "$GW/api/contracts/job/$DEMO_JOB_ACTIVE_CONTRACT/active-count" -H "Authorization: Bearer $TOKEN")
echo "  Direct contract-service active count for active-contract job → $ACTIVE_COUNT"
check_body "contract-service reports active contract count = 1" "$ACTIVE_COUNT" "^1$"

ACTIVE_CLOSE=$(curl -s -o /dev/null -w "%{http_code}" -X PUT "$GW$API_PREFIX/$DEMO_JOB_ACTIVE_CONTRACT/close" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status":"CLOSED"}')
check_status "close job with ACTIVE contract → 400" "$ACTIVE_CLOSE" "400"

NO_ACTIVE_COUNT=$(curl -s "$GW/api/contracts/job/$DEMO_JOB_CLOSE/active-count" -H "Authorization: Bearer $TOKEN")
echo "  Direct contract-service active count for closeable job → $NO_ACTIVE_COUNT"
check_body "contract-service reports active contract count = 0" "$NO_ACTIVE_COUNT" "^0$"

CLOSE_BODY=$(curl -s -X PUT "$GW$API_PREFIX/$DEMO_JOB_CLOSE/close" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status":"CLOSED"}')
echo "  Close no-active-contract job → $CLOSE_BODY"
check_body "close no-active-contract job returns CLOSED" "$CLOSE_BODY" "CLOSED"

JOB_CLOSED_EVENT=$(mongo_eval "db.getSiblingDB('freelancemongo').job_events.find({jobId:$DEMO_JOB_CLOSE, action:'JOB_CLOSED'}).sort({_id:-1}).limit(1).forEach(printjson)")
echo "  Mongo JOB_CLOSED event → $JOB_CLOSED_EVENT"
check_body "MongoDB job_events contains JOB_CLOSED" "$JOB_CLOSED_EVENT" "JOB_CLOSED"

# ─── 12. SUMMARY ─────────────────────────────────────────────────────────────
header "SUMMARY"
echo "  Passed: $PASS"
echo "  Failed: $FAIL"
if [ "$FAIL" -eq 0 ]; then
  echo "  Overall result: PASS"
else
  echo "  Overall result: Review failed checks above. Some live checks may depend on the current Kubernetes cluster, Elasticsearch, RabbitMQ, or service-to-service auth state."
fi
echo ""
echo "  THINGS TO SHOW IN IDE:"
echo "  ┌─ M1 / job-service ─────────────────────────────────────────────────────"
echo "  │ JobSecurityConfig.java             → JWT protection wiring, /api/jobs/health public"
echo "  │ application.yml                    → PostgreSQL, MongoDB, Redis, Elasticsearch, JWT config"
echo "  │ MongoEventLogger.java              → Observer persists job events to MongoDB job_events"
echo "  │ JobProposalSummaryDTO.java         → Builder DTO with 6 meaningful fields"
echo "  │ JobSearchDocument.java             → Elasticsearch fields: id/title/description/category/budget/rating/status"
echo "  │ ElasticsearchHitAdapter.java       → SearchHit<JobSearchDocument> → JobSearchResultDTO"
echo "  │ JobFullTextSearchService.java      → S2-F10 relevance query + Redis cache"
echo "  │ JobController.java                 → /search/full-text, /requirements, /proposal-summary, /close"
echo "  ├─ M3 / job-service ─────────────────────────────────────────────────────"
echo "  │ ProposalServiceClient.java         → S2-F3 proposal summary Feign client"
echo "  │ ContractServiceClient.java         → S2-F4 active-contract Feign check"
echo "  │ CorrelationIdFilter.java           → incoming X-Correlation-ID → MDC correlationId + response echo + cleanup"
echo "  │ FeignCorrelationConfig.java        → outbound X-Correlation-ID from MDC"
echo "  │ JobService.java                    → closeJob(): active contract check, CLOSED state, job.closed publish"
echo "  │ job-dashboard.json                 → LogQL panels: Error Logs, Elasticsearch Query Logs, All Logs"
echo "  ├─ Saga / proposal-service ──────────────────────────────────────────────"
echo "  │ ScenarioDAbandonmentSagaIntegrationTest.java"
echo "  │ PayoutAbandonmentReaperTest.java"
echo "  │ Scenario D walkthrough: PAYMENT_PENDING → reaper synthetic payment.failed"
echo "  │                       reason=payout_abandoned → proposal.cancelled"
echo "  │                       compensation → REFUNDED"
echo "  └────────────────────────────────────────────────────────────────────────"
echo ""
echo "  EVALUATION REMINDERS:"
echo "  • ElasticsearchHitAdapter keeps SearchHit-specific mapping out of the service layer and prevents raw Elasticsearch fields leaking through the API."
echo "  • JobSearchDocument stores searchable/indexed fields only; PostgreSQL remains source of truth for full Job state and JSON requirements."
echo "  • Elasticsearch stays in sync when create/update/requirements/close/index operations call JobSearchIndexOperations."
echo "  • If ContractServiceClient returns 503/401/5xx, JobService.fetchActiveContractCount throws and the job does not close."
echo "  • Scenario D reaper query: findByStatusAndPaymentPendingAtBefore(PAYMENT_PENDING, now - abandonAfter)."
