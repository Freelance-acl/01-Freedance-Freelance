# Freelance Marketplace Backend

Microservices backend for a freelance marketplace, using Spring Boot + PostgreSQL + Docker.

## Services and Ports

| Service | Internal Port | Docker Port | Base URL |
|---------|---------------|-------------|----------|
| user-service | 8080 | 8081 | http://localhost:8081 |
| job-service | 8080 | 8082 | http://localhost:8082 |
| proposal-service | 8080 | 8083 | http://localhost:8083 |
| contract-service | 8080 | 8084 | http://localhost:8084 |
| wallet-service | 8080 | 8085 | http://localhost:8085 |
| PostgreSQL | 5432 | 5432 | postgresql://postgres:postgres@localhost:5432/freelancedb |

---

## Prerequisites

- Java 25+
- **Maven uses `JAVA_HOME` for compilation.** If `./mvnw.cmd` fails with **release version 25 not supported**, run **`. .\scripts\use-jdk25.ps1`** once per session on PowerShell (or set `JAVA_HOME` permanently to your JDK 25 install, e.g. `C:\Program Files\Java\jdk-25`).
- Maven (included as `./mvnw` or `./mvnw.cmd`)
- Docker & Docker Compose
- PostgreSQL (runs in Docker container)

---

## Setup and Run

### 1. First-time setup

```bash
# Copy environment configuration (do this once)
cp .env.example .env

# Then run setup
./setup.bash
```

This runs:
- `git config core.hooksPath .githooks` — enables repo-managed git hooks
- `./mvnw clean install` — install packages
- `./mvnw package -DskipTests` — build all modules

### 2. Start all services

```bash
./run.bash
```

`docker-compose.yaml` sets `SPRING_DATASOURCE_URL` (and credentials) on each service so JDBC uses the hostname `postgres` on the Compose network. `application.properties` uses `localhost` for running `./mvnw spring-boot:run` directly on your machine.

Each service **Dockerfile** is **multi-stage**: builds in a JDK layer, ships a JRE-only runtime (smaller image). You do **not** need a local `target/*.jar` before `docker compose build`.

### 3. Stop all services

```bash
docker-compose down
```

### Dev with hot reload (Docker)

```bash
docker compose -f docker-compose.dev.yml up --build
```

Bind-mounts each service's source into the container and runs `spring-boot:run` with DevTools. After editing `.java` files, trigger a compile in your IDE (or `./mvnw compile -pl <service>`) so `target/classes` updates and DevTools restarts.

**Do not** run both `docker-compose up` and `docker-compose -f docker-compose.dev.yml up` simultaneously if both publish Postgres on port 5432.

---

## Git Hooks

Hooks live under `.githooks/` and are activated by `setup.bash`. They:
- Enforce `team.json` membership for the committing user
- Block committing `target/` output
- Validate commit messages: `feat(<service>): <description> (<studentId>)` or `fix(...)`
- Check `feat/*` branch naming conventions
- Run `mvn test` for all five services before each push

To skip the test gate when needed: `SKIP_TESTS=1 git push` or `NO_VERIFY=1 git push`.

Git for Windows runs hooks with **sh** — keep them executable (`chmod +x .githooks/*` on Unix, or `git update-index --chmod=+x .githooks/*`).

---

## Automated Tests (JUnit 5)

Each service has `spring-boot-starter-test`, H2 (test scope), `src/test/resources/application-test.properties`, and a base class `…/support/AbstractIntegrationTest` (`@SpringBootTest` + `@ActiveProfiles("test")`). Full-context tests run against an in-memory H2 DB — **no Docker required**.

```bash
# All services
./mvnw test

# Single service
./mvnw test -pl job-service

# Specific test class
./mvnw test -pl user-service -Dtest=UserControllerTest
```

---

## Health Endpoints

| Service | URL |
|---------|-----|
| user-service | `GET http://localhost:8081/api/users/health` |
| job-service | `GET http://localhost:8082/api/jobs/health` |
| proposal-service | `GET http://localhost:8083/api/proposals/health` |
| contract-service | `GET http://localhost:8084/api/contracts/health` |
| wallet-service | `GET http://localhost:8085/api/payouts/health` |

---

## Core API Endpoints (CRUD)

Each resource supports:
- `GET /` — list all
- `GET /{id}` — get by id
- `POST /` — create
- `PUT /{id}` — update
- `DELETE /{id}` — delete one
- `DELETE /all` — delete all

### User Service (`8081`)

- `http://localhost:8081/api/users`
- `http://localhost:8081/api/user-skills`

### Job Service (`8082`)

- `http://localhost:8082/api/jobs`
- `http://localhost:8082/api/job-attachments`

### Proposal Service (`8083`)

- `http://localhost:8083/api/proposals`
- `http://localhost:8083/api/proposal-milestones`

### Contract Service (`8084`)

- `http://localhost:8084/api/contracts`

### Wallet Service (`8085`)

- `http://localhost:8085/api/payouts`
- `http://localhost:8085/api/promo-codes`
- `http://localhost:8085/api/payout-promos`

---

## Recommended Creation Flow (with sample payloads)

Create records in this order to satisfy foreign-key constraints:

1. User → 2. Job → 3. Proposal → 4. Contract → 5. Payout

### 1) Create User

`POST http://localhost:8081/api/users`

```json
{
  "name": "Youssef1122",
  "email": "youssef1@x.x",
  "password": "youssef1",
  "phone": "+201550830082",
  "role": "ADMIN",
  "status": "ACTIVE",
  "preferences": {
    "language": "en",
    "notifications": { "email": true, "sms": false },
    "timezone": "Africa/Cairo",
    "profileVisibility": "PUBLIC",
    "hourlyRateRange": { "min": 300, "max": 600 }
  }
}
```

### 2) Create Job

`POST http://localhost:8082/api/jobs`

```json
{
  "clientId": 1,
  "title": "Software Developer",
  "description": "develops software",
  "category": "WEB_DEV",
  "status": "IN_PROGRESS",
  "budgetMin": 10000,
  "budgetMax": 20000,
  "requirements": {
    "requiredSkills": ["Java", "Spring Boot", "PostgreSQL"],
    "experienceLevel": "SENIOR",
    "estimatedDuration": 8,
    "remoteAllowed": true,
    "preferredTimezone": "GMT+2"
  }
}
```

### 3) Create Proposal

`POST http://localhost:8083/api/proposals`

```json
{
  "jobId": 1,
  "freelancerId": 20,
  "coverLetter": "freelancer cover letter",
  "bidAmount": 100.0,
  "estimatedDays": 12,
  "status": "ACCEPTED",
  "submittedAt": "2026-05-06T14:30:00",
  "metadata": {
    "approachSummary": "Microservices with Spring Boot",
    "relevantExperience": "5 years in similar projects",
    "toolsProposed": ["IntelliJ", "Docker", "GitHub"],
    "availabilityStart": "2026-04-01",
    "portfolioLinks": ["https://portfolio.example.com/project1"]
  }
}
```

### 4) Create Contract

`POST http://localhost:8084/api/contracts`

```json
{
  "jobId": 1,
  "freelancerId": 20,
  "clientId": 1,
  "proposalId": 1,
  "agreedAmount": 150,
  "status": "COMPLETED",
  "startDate": "2026-05-06T14:30:00",
  "metadata": {
    "paymentTerms": "MILESTONE",
    "revisionLimit": 3,
    "ndaSigned": true,
    "weeklyHoursExpected": 40,
    "progressPercentage": 65,
    "lastActivityDate": "2026-03-15"
  }
}
```

### 5) Create Payout

`POST http://localhost:8085/api/payouts`

```json
{
  "contractId": 1,
  "freelancerId": 22,
  "amount": 140.5,
  "method": "BANK_TRANSFER",
  "status": "COMPLETED",
  "transactionDetails": {
    "gatewayResponse": "approved",
    "accountLastFour": "9876",
    "receiptUrl": "https://receipts.example.com/abc",
    "failureReason": null
  }
}
```

---

## Important Field Notes

- Use `freelancerId` (not `freeLancerId`).
- Use `contractId` (not `conractId`).
- Preferred Job budget fields: `budgetMin`, `budgetMax`.
- The payout API currently accepts alias fields:
  - `contract_id`
  - `conractId` (typo alias)
  - `freelancer_id`

---

## Helpful Commands

### Rebuild a single service

```bash
./mvnw -pl wallet-service -am compile
```

### Full rebuild

```bash
./mvnw clean install
```

### Container status / logs

```bash
docker-compose ps
docker-compose logs -f
```

---

## Troubleshooting

### JAVA_HOME points to wrong JDK
Run `. .\scripts\use-jdk25.ps1` on PowerShell, or set `JAVA_HOME` permanently to your JDK 25 install.

### Port already in use
```bash
docker-compose down
```

### Database connection issues
```bash
docker ps
docker logs freelance-db
```

### Service won't start after code change
```bash
./mvnw clean package -DskipTests -pl <service-name>
docker-compose up -d --build <service-name>
```
