# Freelance Marketplace Backend

Microservices backend for a freelance marketplace, using Spring Boot + PostgreSQL + Docker.

## Services and Ports

- `user-service` -> `http://localhost:8081`
- `job-service` -> `http://localhost:8082`
- `proposal-service` -> `http://localhost:8083`
- `contract-service` -> `http://localhost:8084`
- `wallet-service` -> `http://localhost:8085`
- `postgres` -> `localhost:5432` (`freelancedb`)

All services run on internal container port `8080` and are mapped to host ports `8081..8085`.

## Prerequisites

- Java 25+
- Docker + Docker Compose
- Bash shell (`bash`) for running `*.bash` scripts
  - macOS/Linux: already available
  - Windows PowerShell: use `bash .\script.bash` (Git Bash/WSL)

## Setup and Run

### 1) First-time setup

```bash
cp .env.example .env
bash ./setup.bash
```

### 2) Start all services

```bash
bash ./run.bash
```

### 3) Stop all services

```bash
bash ./stop.bash
```

## Script Usage by OS

### macOS / Linux (bash)

```bash
bash ./setup.bash
bash ./run.bash
bash ./stop.bash
```

### Windows PowerShell

```powershell
bash .\setup.bash
bash .\run.bash
bash .\stop.bash
```

If `bash` is not recognized, install Git for Windows (Git Bash) or use WSL.

## Health Endpoints

- `GET http://localhost:8081/api/users/health`
- `GET http://localhost:8082/api/jobs/health`
- `GET http://localhost:8083/api/proposals/health2`
- `GET http://localhost:8084/api/contracts/health`
- `GET http://localhost:8085/api/payouts/health`

## Public Instructions Endpoint

- `GET http://localhost:8085/api/instructions`

Returns quick-start payloads and notes for the core flow:
`User -> Job -> Proposal -> Contract -> Payout`

## Core API Endpoints (CRUD)

Each group supports:
- `GET /` (list)
- `GET /{id}` (details)
- `POST /` (create)
- `PUT /{id}` (update)
- `DELETE /{id}` (delete one)
- `DELETE /all` (delete all)

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

## Recommended Creation Flow (with sample payloads)

Create records in this order to satisfy foreign-key checks:

1. User
2. Job
3. Proposal
4. Contract
5. Payout

### 1) Create User

`POST http://localhost:8081/api/users`

```json
{
  "name": "Youssef1122",
  "email": "youssef1@x.x",
  "password": "youssef1",
  "phone": "+201550830082",
  "role": "ADMIN",
  "status": "ACTIVE"
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
  "budgetMax": 20000
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
  "submittedAt": "2026-05-06T14:30:00"
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
  "startDate": "2026-05-06T14:30:00"
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
  "status": "COMPLETED"
}
```

## Important Field Notes

- Use `freelancerId` (not `freeLancerId`).
- Use `contractId` (not `conractId`).
- Preferred Job budget fields: `budgetMin`, `budgetMax`.
- The payout API currently accepts alias fields:
  - `contract_id`
  - `conractId` (typo alias)
  - `freelancer_id`

## Helpful Commands

### Rebuild only one service

```bash
./mvnw -pl wallet-service -am compile
```

### Full rebuild

```bash
./mvnw clean install
```

### Container status/logs

```bash
docker-compose ps
docker-compose logs -f
```

