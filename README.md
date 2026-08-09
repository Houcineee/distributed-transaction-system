# Banking Application

![CI](https://github.com/Houcineee/bank-app/actions/workflows/ci.yml/badge.svg)

A production-grade banking system built with Java and Spring Boot, designed as a learning project to deeply understand backend engineering, from database transactions to deployment.

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
