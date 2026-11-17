Sharing Freedance, a freelance marketplace platform built with our engineering team.

I served as Scrum Master, coordinating sprint planning, backlog refinement, and collaboration throughout delivery, alongside my technical contributions.

This video follows a real frontend-to-backend workflow:
Registration -> job posting -> proposal submission and acceptance -> automatic contract creation -> work completion -> simulated payout -> reload to verify persistence.

Architecture and tools:
- 5 Spring Boot microservices: User, Job, Proposal, Contract, and Wallet/Payment, plus a Spring Cloud Gateway API gateway.
- Java 25, Spring Boot 4, Spring Security/JWT, Spring Data JPA/Hibernate, Maven, and OpenFeign for REST communication.
- RabbitMQ for asynchronous domain events and Saga choreography; Observer-based event handling and Builder-based DTO construction.
- PostgreSQL: five isolated transactional databases, one per core service.
- MongoDB audit/event logs, Redis caching, Neo4j interaction-based recommendations, and Elasticsearch full-text job search. Cassandra is also included in the infrastructure stack.
- Next.js, React, TypeScript, Tailwind CSS, TanStack Query, Axios, React Hook Form, Zod, and Recharts on the frontend.

Docker images in the local setup:
- Custom images: user-service, job-service, proposal-service, contract-service, wallet-service, and api-gateway.
- postgres:17 (five instances), mongo:latest, redis:latest, neo4j:latest, cassandra:latest, elasticsearch:9.2.6, and rabbitmq:3-management.
- Multi-stage Java builds: eclipse-temurin:25.0.2_10-jdk-noble and eclipse-temurin:25.0.2_10-jre-noble.

The repository also includes Kubernetes Deployments, StatefulSets, ConfigMaps, and Secrets, plus monitoring configurations using prom/prometheus:v2.51.2, grafana/grafana:10.4.2, and grafana/loki:2.9.4. The video demonstrates the local Docker Compose setup.

Verification and recording tools: JUnit, Mockito, H2, Playwright, and FFmpeg. This recorded run completed 112 successful API requests with zero browser/API errors; final records were checked directly in PostgreSQL.

Payments are simulated by the backend; no real money is transferred.

A valuable lesson from this project: healthy containers are only the starting point. The real test is whether authentication, service calls, events, database writes, and the UI work together.

Code: https://github.com/Freelance-acl/01-Freedance-Freelance

#SpringBoot #Java #Microservices #Docker #Kubernetes #FullStackDevelopment
