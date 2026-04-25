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

**Package:** `com.team01.freelance.user.controller.HealthController`

---

### Job Service
**Base URL:** `http://localhost:8082/api/jobs`

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/health` | GET | Health check |

**Package:** `com.team01.freelance.job.controller.HealthController`

---

### Proposal Service
**Base URL:** `http://localhost:8083/api/proposals`

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/health2` | GET | Health check (v2) |
| `/health` | GET | Health check (from alternative controller) |

**Packages:**
- `com.team01.freelance.proposal.controller.HealthController` (uses `/health2`)
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
- `com.team01.freelance.contract.controller.HealthController` (has both endpoints)
- `com.team01.freelance.contractservice.controllers.HealthController` (health only)

---

### Wallet Service / Payouts Service
**Base URL:** `http://localhost:8085/api/payouts`

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/health` | GET | Health check |

**Packages:**
- `com.team01.freelance.wallet.controller.HealthController`
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

---

## 14 Appendix: JSONB Sample Data

Below is one example per service showing what the JSONB columns look like in practice.
A full database seeder will be provided separately.

### `User.preferences`
```json
{
  "language": "en",
  "notifications": {
    "email": true,
    "sms": false
  },
  "timezone": "Africa/Cairo",
  "profileVisibility": "PUBLIC",
  "hourlyRateRange": {
    "min": 300,
    "max": 600
  }
}
```

### `Job.requirements`
```json
{
  "requiredSkills": [
    "Java",
    "Spring Boot",
    "PostgreSQL"
  ],
  "experienceLevel": "SENIOR",
  "estimatedDuration": 8,
  "remoteAllowed": true,
  "preferredTimezone": "GMT+2"
}
```

### `Proposal.metadata`
```json
{
  "approachSummary": "Microservices with Spring Boot",
  "relevantExperience": "5 years in similar projects",
  "toolsProposed": [
    "IntelliJ",
    "Docker",
    "GitHub"
  ],
  "availabilityStart": "2026-04-01",
  "portfolioLinks": [
    "https://portfolio.example.com/project1"
  ]
}
```

### `Contract.metadata`
```json
{
  "paymentTerms": "MILESTONE",
  "revisionLimit": 3,
  "ndaSigned": true,
  "weeklyHoursExpected": 40,
  "progressPercentage": 65,
  "lastActivityDate": "2026-03-15"
}
```

### `Payout.transactionDetails`
```json
{
  "gatewayResponse": "approved",
  "accountLastFour": "9876",
  "receiptUrl": "https://receipts.example.com/abc",
  "failureReason": null
}
```

---

## 15 Appendix: Example Database Tables

The following tables show what your database should look like after seeding some test data. Use these as a reference when testing your CRUD operations and features via Postman. All tables coexist in the same `freelancedb` database.

Note: JSONB columns are shown as formatted JSON code blocks below each table for readability. In the actual database, each block is stored as a single JSONB column value in the corresponding row.

### 15.1 User Service Tables

#### 15.1.1 `users` table

| id | name | email | password | phone | role | status | created_at |
|---|---|---|---|---|---|---|---|
| 1 | Ahmed Hassan | ahmed@mail.com | pass123 | +201011111111 | FREELANCER | ACTIVE | 2026-03-01 10:00 |
| 2 | Sara Mohamed | sara@mail.com | pass456 | +201022222222 | CLIENT | ACTIVE | 2026-03-02 14:30 |
| 3 | Admin User | admin@freelance.com | admin789 | +201033333333 | ADMIN | ACTIVE | 2026-03-01 08:00 |

JSONB column - `preferences`:

User 1 (Ahmed - Freelancer):
```json
{
  "language": "ar",
  "notifications": {
    "email": true,
    "sms": false
  },
  "timezone": "Africa/Cairo",
  "profileVisibility": "public",
  "hourlyRateRange": {
    "min": 300,
    "max": 600
  }
}
```

User 2 (Sara - Client):
```json
{
  "language": "en",
  "notifications": {
    "email": true,
    "sms": true
  },
  "timezone": "Africa/Cairo",
  "profileVisibility": "public"
}
```

#### 15.1.2 `user_skills` table

| id | user_id | skill_name | category | yrs | proficiency | is_primary |
|---|---|---|---|---|---|---|
| 1 | 1 | Java | DEVELOPMENT | 5 | EXPERT | true |
| 2 | 1 | Spring Boot | DEVELOPMENT | 3 | INTERMEDIATE | false |
| 3 | 1 | React | DEVELOPMENT | 2 | INTERMEDIATE | false |

(`yrs` = `yearsOfExperience`)

JSONB column - `metadata`:

Skill 1 (Ahmed - Java):
```json
{
  "certifications": [
    "Oracle Certified Professional"
  ],
  "portfolioUrls": [
    "https://github.com/ahmed-dev"
  ],
  "endorsementCount": 12,
  "lastUsedDate": "2026-03-10",
  "tools": [
    "IntelliJ IDEA",
    "Maven",
    "Docker"
  ]
}
```

Skill 2 (Ahmed - Spring Boot):
```json
{
  "certifications": [],
  "portfolioUrls": [
    "https://github.com/ahmed-dev/spring-projects"
  ],
  "endorsementCount": 7,
  "lastUsedDate": "2026-03-10",
  "tools": [
    "Spring Data JPA",
    "Spring Security"
  ]
}
```

### 15.2 Job Service Tables

#### 15.2.1 `jobs` table

| id | client | title | category | status | min | max | rating | total_r |
|---|---|---|---|---|---|---|---|---|
| 1 | 2 | E-Commerce Backend | WEB_DEV | CLOSED | 5000 | 15000 | 4.8 | 1 |
| 2 | 2 | Mobile App UI | DESIGN | OPEN | 3000 | 8000 | 0.0 | 0 |

(`client` = `clientId`, `min/max` = `budgetMin`/`budgetMax`, `total_r` = `totalRatings`)

JSONB column - `requirements`:

Job 1 (E-Commerce Backend):
```json
{
  "requiredSkills": [
    "Java",
    "Spring Boot",
    "PostgreSQL"
  ],
  "experienceLevel": "MID",
  "estimatedDuration": 8,
  "remoteAllowed": true,
  "preferredTimezone": "Africa/Cairo"
}
```

Job 2 (Mobile App UI):
```json
{
  "requiredSkills": [
    "Figma",
    "UI/UX",
    "Mobile Design"
  ],
  "experienceLevel": "JUNIOR",
  "estimatedDuration": 4,
  "remoteAllowed": true,
  "preferredTimezone": null
}
```

#### 15.2.2 `job_attachments` table

| id | job_id | type | file_url | expiry_date | verified |
|---|---|---|---|---|---|
| 1 | 1 | BRIEF | /docs/ecomm-brief.pdf | 2026-12-31 | true |
| 2 | 1 | REFERENCE | /docs/api-spec.pdf | 2026-09-30 | true |
| 3 | 2 | MOCKUP | /docs/app-mockup.fig | 2026-06-30 | false |

JSONB column - `metadata`:

Attachment 1 (Brief - Verified):
```json
{
  "fileSize": 245,
  "fileFormat": "PDF",
  "versionNumber": 2,
  "verificationNotes": "Verified by admin on 2026-03-05"
}
```

### 15.3 Proposal Service Tables

#### 15.3.1 `proposals` table

| id | job_id | freelancer | bid | days | status | submitted_at |
|---|---|---|---|---|---|---|
| 1 | 1 | 1 | 10000 | 45 | ACCEPTED | 2026-03-05 09:00 |
| 2 | 2 | 1 | 5000 | 20 | SUBMITTED | 2026-03-18 14:00 |

(`freelancer` = `freelancerId`, `bid` = `bidAmount`, `days` = `estimatedDays`)

JSONB column - `metadata`:

Proposal 1 (Ahmed -> E-Commerce, Accepted):
```json
{
  "approachSummary": "Microservices architecture with Spring Boot",
  "relevantExperience": "Built 3 similar e-commerce backends",
  "toolsProposed": [
    "Java 25",
    "Spring Boot 3",
    "PostgreSQL",
    "Docker"
  ],
  "availabilityStart": "2026-03-10",
  "portfolioLinks": [
    "https://github.com/ahmed-dev/ecomm"
  ]
}
```

#### 15.3.2 `proposal_milestones` table

| id | prop_id | order | title | amount | status |
|---|---|---|---|---|---|
| 1 | 1 | 1 | Database Design | 2000 | COMPLETED |
| 2 | 1 | 2 | API Development | 5000 | COMPLETED |
| 3 | 1 | 3 | Testing & Deployment | 3000 | COMPLETED |

(`prop_id` = `proposal_id`, `order` = `milestoneOrder`)

JSONB column - `metadata`:

Milestone 1 (Database Design - Completed):
```json
{
  "deliverables": [
    "ER diagram",
    "Migration scripts",
    "Seed data"
  ],
  "acceptanceCriteria": "All tables created with proper indexes",
  "estimatedDays": 7,
  "actualCompletionDate": "2026-03-17",
  "revisionCount": 1
}
```

Milestone 3 (Testing - Completed):
```json
{
  "deliverables": [
    "Unit tests",
    "Integration tests",
    "Docker Compose"
  ],
  "acceptanceCriteria": "80% code coverage, all endpoints tested",
  "estimatedDays": 14,
  "actualCompletionDate": "2026-03-25",
  "revisionCount": 0
}
```

Reading the data: Ahmed submitted a proposal for Sara's E-Commerce Backend job at 10,000 EGP with 3 milestones (Database Design, API Development, Testing). All three milestones are completed. Ahmed also submitted a second proposal for the Mobile App UI job that's still pending.

### 15.4 Contract Service Tables

#### 15.4.1 `contracts` table

| id | job | free | client | prop | amount | status | start_date | end_date | created_at |
|---|---|---|---|---|---|---|---|---|---|
| 1 | 1 | 1 | 2 | 1 | 10000 | COMPLETED | 2026-03-10 | 2026-03-25 | 2026-03-10 |

(`job` = `jobId`, `free` = `freelancerId`, `prop` = `proposalId`, `amount` = `agreedAmount`)

JSONB column - `metadata`:

Contract 1 (Ahmed <-> Sara, Completed):
```json
{
  "paymentTerms": "MILESTONE",
  "revisionLimit": 3,
  "ndaSigned": true,
  "weeklyHoursExpected": 30,
  "progressPercentage": 100,
  "lastActivityDate": "2026-03-25"
}
```

Reading the data: Contract #1 links Ahmed (freelancer) with Sara (client) for the E-Commerce Backend job. The agreed amount is 10,000 EGP with milestone-based payments. The contract is now completed (all milestones delivered) with an NDA signed.

### 15.5 Wallet Service Tables

#### 15.5.1 `payouts` table

| id | contract_id | freelancer_id | amount | method | status | created_at |
|---|---|---|---|---|---|---|
| 1 | 1 | 1 | 10000 | BANK_TRANSFER | COMPLETED | 2026-03-25 17:50 |

JSONB column - `transactionDetails`:

Payout 1 (Ahmed - Contract Completed, 10K EGP):
```json
{
  "gatewayResponse": "approved",
  "accountLastFour": "9876",
  "receiptUrl": "https://pay.example/payout001",
  "failureReason": null
}
```

#### 15.5.2 `promo_codes` table

| id | code | discount_type | value | max | used | expiry_date | active |
|---|---|---|---|---|---|---|---|
| 1 | FIRSTJOB20 | PERCENTAGE | 20.0 | 50 | 1 | 2026-09-30 23:59 | true |
| 2 | FLAT200 | FIXED | 200.0 | 100 | 0 | 2026-12-31 23:59 | true |
| 3 | LAUNCH10 | PERCENTAGE | 10.0 | 30 | 30 | 2026-02-28 23:59 | false |

(`value` = `discountValue`, `max` = `maxUses`, `used` = `currentUses`)

JSONB column - `metadata`:

PromoCode 1 (FIRSTJOB20):
```json
{
  "eligibleJobCategories": [
    "WEB_DEV",
    "MOBILE",
    "DESIGN"
  ],
  "minimumContractAmount": 1000,
  "termsAndConditions": "First completed contract only"
}
```

PromoCode 2 (FLAT200):
```json
{
  "eligibleJobCategories": [
    "WEB_DEV"
  ],
  "minimumContractAmount": 5000,
  "applicableRegions": [
    "Egypt",
    "UAE"
  ]
}
```

#### 15.5.3 `payout_promos` table (Join Entity)

| id | payout_id | promo_code_id | discount_applied | applied_at |
|---|---|---|---|---|
| 1 | 1 | 1 | 2000.00 | 2026-03-25 17:55 |

Reading the data:
- Payout #1 (Ahmed's contract payout, 10,000 EGP) had promo code FIRSTJOB20 applied - 20% of 10,000 = 2,000 EGP platform fee discount.
- PromoCode FIRSTJOB20 has `current_uses = 1` because it was used once by Ahmed.
- PromoCode LAUNCH10 is inactive because it reached its max uses (`30/30`) and its expiry date has passed.

### 15.6 How the Tables Connect (Cross-Service)

Since all services share one database, you can trace data across tables:

a) Sara (user 2, CLIENT) posted Job #1 (E-Commerce Backend, 5K-15K EGP budget). Ahmed (user 1, FREELANCER) submitted Proposal #1 with a 10K EGP bid and 3 milestones.

b) Sara accepted the proposal, creating Contract #1 (10K EGP agreed). The contract is now COMPLETED.

c) Ahmed completed all 3 milestones (Database Design, API Development, Testing & Deployment). Upon contract completion, Payout #1 (10,000 EGP) was created via S3-F4, processed via S5-F4, and had FIRSTJOB20 promo code applied via S5-F5 (saved 2,000 EGP).

d) Ahmed's skills (Java, Spring Boot, React) match the job's requirements (Java, Spring Boot, PostgreSQL), which is how he was able to submit a competitive proposal.

e) A native SQL query in the Proposal Service can JOIN proposals with users (`freelancer_id`), jobs (`job_id`), contracts (`proposal_id`), and payouts (`contract_id`) to build a complete freelancer dashboard DTO.

This is the kind of cross-service data access you will implement using native SQL JOINs on the shared database.
