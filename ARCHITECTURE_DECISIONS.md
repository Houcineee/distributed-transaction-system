# Architecture Decisions

This document records the main architectural decisions behind the Banking
System. The project is a learning-oriented backend application, so each
decision favors understanding core engineering principles while keeping the
system deployable and easy to evolve.

## ADR-001: Banking System as the Learning Vehicle

**Status:** Accepted

The project uses a banking system to make correctness, data integrity,
concurrency, authentication, authorization, and auditability central concerns.
These constraints provide a practical context for learning production backend
engineering rather than building isolated CRUD features.

## ADR-002: Java, Spring Boot, and PostgreSQL

**Status:** Accepted

The backend uses Java and Spring Boot with PostgreSQL.

Spring Boot provides an established ecosystem for web APIs, security,
transactions, persistence, validation, and observability. PostgreSQL was
chosen for its transactional guarantees, constraints, indexing, and
production relevance.

Redis and event-driven components are planned for later stages when the
current monolith has a well-understood foundation.

## ADR-003: Database Migrations with Flyway

**Status:** Accepted

Database schema changes are versioned with Flyway migrations, while Hibernate
is configured with `ddl-auto=validate`.

This keeps schema ownership explicit and prevents the ORM from silently
changing production tables. Application startup validates that the entity
model and database schema are compatible.

## ADR-004: Money Stored as Integer Cents

**Status:** Accepted

Account balances and transaction amounts are stored as PostgreSQL `BIGINT`
values and Java `Long` values representing the smallest currency unit.

Integer arithmetic avoids floating-point rounding errors. Formatting into
human-readable decimal values happens at the API boundary.

## ADR-005: UUIDv7 Primary Keys

**Status:** Accepted

Entities use UUIDv7 identifiers.

UUIDs avoid exposing predictable sequential identifiers through the API.
UUIDv7 also preserves time ordering, which is more suitable for database
indexes than randomly distributed UUIDv4 values.

## ADR-006: Pessimistic Locking for Transfers

**Status:** Accepted

Transfers acquire pessimistic write locks for both accounts in deterministic
UUID order before changing balances.

The deterministic order reduces deadlock risk when concurrent transfers
involve the same accounts. Database locking and the surrounding transaction
ensure that competing requests cannot overwrite each other's balance changes.

The current balance model is intentionally a first stage. A future ledger
model will provide immutable double-entry records and reconciliation.

## ADR-007: JWT Authentication in HttpOnly Cookies

**Status:** Accepted

The backend issues signed JWTs in an `HttpOnly`, `SameSite=Strict` cookie
instead of returning the token for browser storage.

This prevents normal frontend JavaScript from reading the token and reduces
cross-site request forgery risk for the current same-site deployment model.
The frontend obtains the authenticated user through `/api/v1/users/me`.

Current behavior:

- JWT validity is one hour by default.
- The cookie is retained by the browser for seven days, but JWT expiration
  remains authoritative.
- `Secure` cookies are enabled in production.
- Local HTTP development may use `Secure=false`.
- Logout removes the browser cookie but does not revoke an already-issued
  stateless token.

Future authentication work may introduce refresh-token rotation and
server-side revocation if longer sessions or immediate invalidation become
requirements.

## ADR-008: Server-Side Authorization

**Status:** Accepted

Authorization is enforced by Spring Security and method-level checks rather
than relying on frontend route protection.

Customers can access only their own users, accounts, cards, and transfers.
Administrative operations require the `ADMIN` role. Frontend route guards are
treated as a user-experience feature, not as a security boundary.

## ADR-009: Audit Logging and Correlation IDs

**Status:** Accepted

Security-sensitive actions and important banking operations are recorded in
audit logs. Requests receive correlation IDs that are returned in responses
and included in application logs.

This supports troubleshooting and provides an application-level record of
authentication, authorization, account, card, and transfer events. Database
triggers make audit logs append-only.

## ADR-010: Testcontainers for Integration Tests

**Status:** Accepted

Integration tests run against a real PostgreSQL container through Testcontainers
instead of an in-memory database.

This verifies database migrations, constraints, locking behavior, triggers,
and transaction semantics against the same database family used by the
application.

## ADR-011: React and Vite Frontend

**Status:** Accepted

The frontend uses React with TypeScript, Vite, and Tailwind CSS.

Vite provides fast development feedback and a production build based on
standard browser modules. TypeScript and the generated OpenAPI client keep
frontend API usage aligned with the backend contract.

## ADR-012: Containerized Deployment

**Status:** Accepted

The application is deployed as separate backend, frontend, PostgreSQL, Nginx,
Prometheus, Grafana, Loki, and Promtail containers.

Nginx terminates TLS and routes browser traffic to the frontend and API.
The observability containers make logs and metrics available while keeping the
application independently deployable.

The current deployment is intentionally simple and suitable for a first
working release. Immutable image tags, health-gated rollouts, and rollback
automation are planned operational improvements.

## ADR-013: Single-Row Transfer Ledger vs. Double-Entry Bookkeeping

**Status:** Accepted (Deliberate Trade-Off)

The transfer engine records money movement as a single row containing `source_account_id`, `destination_account_id`, and `amount` within an atomic PostgreSQL transaction protected by deterministic lock-ordered pessimistic concurrency control (`PESSIMISTIC_WRITE`).

**Production Context & Trade-Off:**
Real-world core banking systems typically model financial transactions using an immutable double-entry ledger (a parent transaction with balanced Debit and Credit line entries). Double-entry eliminates database `OR` query bottlenecks when fetching account statements and natively accommodates multi-party flows (such as platform fees or tax withholding).

**Decision Rationale:**
For the current scope of the application (peer-to-peer account transfers), a single-row ledger model was chosen intentionally. This prioritized mastering database concurrency primitives, row-level locking, and deadlock elimination without introducing premature schema complexity.

**Evolution Trigger:**
Transitioning to a full double-entry ledger schema (`transactions` and `ledger_entries`) is planned when multi-legged financial workflows (such as fee distribution, external deposit sources, or multi-currency conversions) are introduced.

## ADR-014: Issuer-Direct Card Simulation vs. PCI-DSS Compliance

**Status:** Accepted (Deliberate Trade-Off)

The application models payment cards directly in PostgreSQL, generating Luhn-valid card numbers, tracking expiration dates, CVVs, and managing lifecycle state machines (Active, Blocked, Expired).

**Production Context & Trade-Off:**
Commercial web applications avoid handling raw Primary Account Numbers (PAN) and CVVs directly, delegating card capture and tokenization to PCI-DSS compliant third-party payment vaults (such as Stripe Elements or Adyen) to avoid regulatory compliance overhead.

**Decision Rationale:**
The goal of this project is learning core backend systems engineering, data integrity, and cryptography from first principles, rather than integrating third-party SaaS SDKs. The application acts as the *issuing bank engine* rather than a simple merchant frontend.

**Mitigation:**
To protect sensitive card details at rest without commercial tokenization vendors, application-level AES-256-GCM encryption (`AttributeConverter`) is used to ensure sensitive attributes remain ciphertext in database snapshots and backups.

