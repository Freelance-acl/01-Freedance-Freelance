# Freelance Marketplace - API Endpoints & Setup Guide

## Quick Start

### Git hooks (Milestone 1)

This repository ships hooks under `.githooks/` that enforce `team.json`, block committing `target/` output, validate commit messages (`feat` / `fix` / merge / `init:`), check `feat/*` branch naming, and run **`mvn test`** for all five services before each push.

First-time **`./setup.bash`** runs `git config core.hooksPath .githooks` for this clone. You can also set it yourself from the repo root:

```bash
git config core.hooksPath .githooks
```

Git for Windows runs these with **sh**; keep hooks executable (`chmod +x .githooks/*` on Unix, or re-add with `git update-index --chmod=+x`).

To skip the test gate on a push when needed: `SKIP_TESTS=1 git push` or `NO_VERIFY=1 git push`.

### Prerequisites
- Java 25+
- Maven (included as `./mvnw` or `./mvnw.cmd`)
- Docker & Docker Compose
- PostgreSQL (runs in Docker container)

### Steps to Run

#### 1. Setup (First Time Only)
```bash
# Copy environment configuration (do this once)
cp .env.example .env

# Then run setup (either script enables Git hooks for this clone)
./setup.bash
# or: sh ./setup.sh
```
This runs:
- `./mvnw.cmd clean install` - Install packages
- `./mvnw.cmd package -DskipTests` - Build all modules

#### 2. Run the Application
```bash
./run.bash
```
This runs:
- `./mvnw.cmd package -DskipTests` - Rebuild all modules
- `docker-compose up -d --build` - Start all services + PostgreSQL in Docker

`docker-compose.yaml` sets **`SPRING_DATASOURCE_URL`** (and credentials) on each app service so JDBC uses the hostname **`postgres`** on the Compose network. `application.properties` still uses **`localhost`** for running **`./mvnw spring-boot:run`** on your machine.

Each service **Dockerfile** is **multi-stage**: it runs `./mvnw package -DskipTests` in a **JDK** layer, then ships a **JRE-only** runtime (`eclipse-temurin:25.0.2_10-jre-noble` on Ubuntu Noble—smaller than carrying the JDK). `JAVA_TOOL_OPTIONS` enables **container CPU/memory awareness** and a faster RNG init. You do **not** need a local `target/*.jar` before `docker compose build`.

#### 3. Stop the Application
```bash
docker-compose down
```

#### Dev with hot reload (Docker)

The default compose stack runs a **fat JAR** in a JRE image, so the JVM does not watch your source tree. For **Spring Boot DevTools** restarts while containers are running, use the dev overlay (bind-mounted module + `./mvnw spring-boot:run` + shared Maven cache):

```bash
docker compose -f docker-compose.dev.yml up --build
```

Use the same host ports as the table below (8081–8085, Postgres 5432). **Do not run** `docker compose up` and `docker compose -f docker-compose.dev.yml up` at the same time if both publish Postgres on **5432**.

After you change `.java` (or resources under `src/main/resources`), trigger a compile so `target/classes` updates—your IDE build or `./mvnw compile` inside the mounted service directory—then DevTools restarts the app in the container.

For hot reload **without** Docker, run `./mvnw spring-boot:run` from a service module with `SPRING_PROFILES_ACTIVE=dev` and Postgres reachable at `localhost:5432`.

---

## Service Port Mapping

| Service | Internal Port | Docker Port | Base URL |
|---------|---------------|-------------|----------|
| user-service | 8080 | 8081 | http://localhost:8081 |
| job-service | 8080 | 8082 | http://localhost:8082 |
| proposal-service | 8080 | 8083 | http://localhost:8083 |
| contract-service | 8080 | 8084 | http://localhost:8084 |
| wallet-service | 8080 | 8085 | http://localhost:8085 |
| PostgreSQL | 5432 | 5432 | postgresql://postgres:postgres@localhost:5432/freelancedb |

---

## Automated tests (JUnit 5)

Each service includes **`spring-boot-starter-test`**, **H2** (test scope), `src/test/resources/application-test.properties`, and a base class **`…/support/AbstractIntegrationTest`** (`@SpringBootTest` + `@ActiveProfiles("test")`) so full-context tests run against an in-memory DB without Docker. Add `@WebMvcTest` for controller slices. From the repo root: `./mvnw test`.

---

## API Endpoints

Milestone 1 health checks: **`GET …/health`** returns **`OK`** (HTTP 200). The **`/api/...`** prefix is **not** hard-coded on controllers: each module sets **`server.servlet.context-path`** once in `application.properties` (and the same line in `application-test.properties`). Controllers extend **`BaseApiController`** with **`@RequestMapping("/")`** and only declare paths like **`/health`**.

**Why not “strip `-service`” for the path?** That turns `user-service` into `user`, not **`users`**, and `wallet-service` into **`wallet`**, not **`payouts`**. The milestone uses **resource-style** path segments (`/api/users`, `/api/payouts`, …), which are not the same as the Maven artifact id. A single property per service avoids wrong singular/plural guesses.

### User Service
**Base URL:** `http://localhost:8081/api/users`

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/health` | GET | Health check |

**Package:** `com.team01.freelance.user.controllers.HealthController`

---

### Job Service
**Base URL:** `http://localhost:8082/api/jobs`

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/health` | GET | Health check |

**Package:** `com.team01.freelance.job.controllers.HealthController`

---

### Proposal Service
**Base URL:** `http://localhost:8083/api/proposals`

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/health` | GET | Health check |

**Package:** `com.team01.freelance.proposal.controllers.HealthController`

---

### Contract Service
**Base URL:** `http://localhost:8084/api/contracts`

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/` | GET | Welcome message |
| `/health` | GET | Health check |

**Package:** `com.team01.freelance.contract.controllers.HealthController` (also exposes `GET /` as a welcome string)

---

### Wallet Service
**Base URL:** `http://localhost:8085/api/payouts`

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/health` | GET | Health check |

**Package:** `com.team01.freelance.wallet.controllers.HealthController`

---

## Testing Endpoints

### Test User Service
```bash
curl http://localhost:8081/api/users/health
```

### Test Job Service
```bash
curl http://localhost:8082/api/jobs/health
```

### Test Proposal Service
```bash
curl http://localhost:8083/api/proposals/health
```

### Test Contract Service
```bash
curl http://localhost:8084/api/contracts/health
# or
curl http://localhost:8084/api/contracts/
```

### Test Wallet Service
```bash
curl http://localhost:8085/api/payouts/health
```

---

## Troubleshooting

### Controllers Not Updating
After changing controller code:
1. Rebuild the specific module:
   ```bash
   cd <service-name>
   mvn clean package -DskipTests
   ```
2. Or rebuild all and restart:
   ```bash
   mvn clean package -DskipTests
   docker-compose up -d --build
   ```

### Port Already in Use
Kill the process using the port or stop Docker containers:
```bash
docker-compose down
```

### Database Connection Issues
Check if PostgreSQL container is running:
```bash
docker ps
docker logs freelance-db
```

### Duplicate controllers
If you introduce a second `HealthController` (or overlapping `@RequestMapping`) under another package, Spring may fail at startup or map only one handler. Keep a single health controller per service under `…/<domain>/controllers/`.

---

## Project Structure

```
root/
├── setup.bash              # Initial setup script
├── run.bash               # Run script for development
├── docker-compose.yaml    # Main services composition
├── docker-compose.db.yml  # Database only composition
├── pom.xml               # Root Maven POM
├── user-service/
├── job-service/
├── proposal-service/
├── contract-service/
├── wallet-service/
└── tstModule/            # Test module
```
