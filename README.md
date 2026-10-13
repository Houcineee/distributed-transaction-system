# Cine Bank

![CI](https://github.com/Houcineee/bank-app/actions/workflows/ci.yml/badge.svg)

A production-minded banking system built while learning how real software gets designed, protected, tested, observed, and deployed.

## Why I built this

I did not start this project knowing how to build production systems. I started with a simpler question: what actually has to happen for a transfer to be safe when requests arrive at the same time, a message is delivered twice, a service fails halfway through, or an operator needs to understand what happened later?

I chose a banking application because money makes vague engineering decisions impossible to ignore. A transfer forced me to learn about database locking, idempotency, authentication, authorization, audit trails, event delivery, fraud checks, observability, integration testing, and containerized deployment as connected parts of one system.

This is a learning project, not a regulated bank or a claim of production readiness. The point is the engineering journey: turning a basic CRUD application into something that can explain its decisions, fail in understandable ways, and be improved deliberately.

## What this project is teaching me

- How to protect a transfer with transactional boundaries and deterministic pessimistic locks.
- How to make retries safe with idempotency keys and an outbox event flow.
- How authentication and authorization belong on the server, not only in the UI.
- How Kafka events can feed an asynchronous fraud-velocity check.
- How metrics, logs, correlation IDs, and audit records make a running system debuggable.
- How tests against real PostgreSQL containers reveal problems that mocks hide.
- How Docker, Nginx, health checks, and deployment configuration turn code into a service.

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java |
| Framework | Spring Boot |
| Database | PostgreSQL 17 |
| Cache | Redis |
| ORM | Spring Data JPA / Hibernate |
| Build Tool | Maven |
| Containers | Docker / Docker Compose |
| CI/CD | GitHub Actions |
| Hosting | Oracle Cloud Free Tier |

## Architecture

The transfer path is protected by idempotency and transactional locking, then published through Kafka for asynchronous fraud-velocity checks. Prometheus and Loki feed Grafana for metrics and searchable logs.

![Cine Bank distributed transaction system](frontend/public/media/infrastructure-transfer-flow.gif)

*Animated transfer flow: customer request → secure ingress → transaction core → Kafka fraud stream → Grafana observability.*

## Getting Started

### Prerequisites
- Java 17+
- Docker & Docker Compose
- Maven

### Run the database
```bash
cd bank-app
docker-compose up -d
```

### Run the application
```bash
cd bank-app
./mvnw spring-boot:run
```

## Branching Strategy

We use **GitHub Flow**:

- `main` is the default branch and is always deployable.
- For each new feature or ticket, create a branch off `main` using the naming convention: `feature/BAS-<ticket-number>-<short-description>` (e.g., `feature/BAS-17-git-strategy`).
- When the work is done, open a Pull Request into `main`.
- PRs are merged using **regular merge commits** to preserve the full commit history.
- After merging, the feature branch is deleted.
