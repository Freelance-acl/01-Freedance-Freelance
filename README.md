# Freelance Marketplace - API Endpoints & Setup Guide

## Quick Start

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

# Then run setup
./setup.bash
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

#### 3. Stop the Application
```bash
docker-compose down
```

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

## API Endpoints

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
| `/health2` | GET | Health check (v2) |
| `/health` | GET | Health check (from alternative controller) |

**Packages:**
- `com.team01.freelance.proposal.controllers.HealthController` (uses `/health2`)
- `com.team01.freelance.proposalservice.controllers.HealthController` (uses `/health`)

**Note:** There are two HealthController classes in different packages. The active one depends on which package Spring Boot's component scan finds first (typically the one closest to the main application class).

---

### Contract Service
**Base URL:** `http://localhost:8084/api/contracts`

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/` | GET | Welcome message |
| `/health` | GET | Health check |

**Packages:**
- `com.team01.freelance.contract.controllers.HealthController` (has both endpoints)
- `com.team01.freelance.contractservice.controllers.HealthController` (health only)

---

### Wallet Service / Payouts Service
**Base URL:** `http://localhost:8085/api/payouts`

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/health` | GET | Health check |

**Packages:**
- `com.team01.freelance.wallet.controllers.HealthController`
- `com.team01.freelance.walletservice.controllers.HealthController`

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
curl http://localhost:8083/api/proposals/health2
# or
curl http://localhost:8083/api/proposals/health
```

### Test Contract Service
```bash
curl http://localhost:8084/api/contracts/health
# or
curl http://localhost:8084/api/contracts
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

### Duplicate Controllers
Note: Some services have multiple HealthController classes in different packages. Only one will be active (the one in the main package scan path). To clean this up, delete or consolidate duplicate controllers.

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
