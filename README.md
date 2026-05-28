# ResHub — Hotel Reservation API (Spring Boot)

[![CI (Build & Test)](https://github.com/rubenrzprz/reshub/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/rubenrzprz/reshub/actions/workflows/ci.yml)

**Status:** WIP 🛠️

Secure, multi-tenant reservation API demonstrating **RBAC enforcement**, robust search (pagination/filters), and **CSV/JSON** exports. Built for clarity, testability, and easy demo.

---

## 📚 Table of Contents
- [🧭 Overview](#-overview)
- [✨ Features](#-features)
- [🧰 Tech Stack](#-tech-stack)
- [📐 Architecture](#-architecture)
- [🚀 Run & Explore](#-run--explore)
  - [🐳 Run with Docker Compose (API + DB)](#-run-with-docker-compose-api--db)
  - [🧪 Tests](#-tests)
- [🗺️ Milestones](#-milestones)
- [✍️ Author](#-author)

---

## 🧭 Overview
ResHub showcases a production-style backend: authenticated operations, role-based permissions, clean error design, and repeatable local/CI runs.
See the full domain & data model in **[📐 Architecture](docs/Architecture.md)**.

---

## ✨ Features
- 🔑 JWT authentication (`/auth/token`) with bearer-protected reservation endpoints
- 🔐 Role-based access control for **ADMIN**, **MANAGER**, **RECEPTIONIST**, **AGENCY**
- 🔎 Search by date range, status, and free-text guest; pagination & sorting
- 📤 CSV/JSON export of filtered results
- 🧭 Idempotent `externalRef` per (hotel, agency)
- 🧾 Feature-flagged agency-hotel authorization (`features.enforce-agency-hotel-auth`)
- 🧱 Standardized `ProblemDetail` error codes (401/403/404/409/400 paths)
- ✅ Integration tests with **Testcontainers (Postgres)**

---

## 🧰 Tech Stack
| 🧩 **Area**       | ⚙️ **Choice**                             |
|-------------------|-------------------------------------------|
| 💬 **Language**   | Java 21                                   |
| 🧱 **Framework**  | Spring Boot 3                             |
| 🧷 **Build**      | Maven                                     |
| 🗄️ **Database**  | PostgreSQL 16                             |
| 🧾 **Persistence**| Spring Data JPA + targeted JdbcTemplate  |
| 🔁 **Migrations** | Flyway                                    |
| 🔐 **Security**   | Spring Security + JWT                    |
| 🧪 **Testing**    | JUnit 5, Spring Boot Test, Testcontainers |
| 📚 **API Docs**   | OpenAPI (springdoc)                       |
| ▶️ **Runtime**    | Docker / Docker Compose                   |

---

## 📐 Architecture

High-level overview of the domain & data model. For the full spec, see **[docs/Architecture.md](docs/Architecture.md)**.

- **Tenancy:** single DB; entities scoped by `hotel_id` where applicable.
- **Persistence:** Spring Data JPA for core aggregate commands/lookups; explicit `JdbcTemplate` SQL for role-scoped list/export queries.
- **Core entities:** `hotel`, `agency`, `app_user`, `room_type`, `room_type_channel_map`, `reservation`, `reservation_comment`.
- **Reservations:** lifecycle `NEW → CONFIRMED → CANCELLED | NOSHOW`; never delete (use status + `cancelled_at`).
- **Lifecycle guardrails:** terminal reservations (`CANCELLED`, `NOSHOW`) are immutable.
- **Idempotency:** unique `(hotel_id, agency_id, external_ref)`.
- **Room types:** hotel-scoped with `attributes_raw` + `attributes_canonical` (JSONB), GIN index; channel mapping; **late binding**.
- **Migrations:** V1 (baseline) → V2 (room modeling) → V3 (room type channel map integrity) → V4 (reservations/comments) → V5 (agency authorization, flag-gated).

---

## 🚀 Run & Explore

ResHub is designed to be run as a **full local stack** using Docker Compose.
This guarantees the same behavior locally, in CI, and in future environments.

### Requirements

* **Java 21** for Maven builds and tests. The build enforces Java 21 so local runs match CI.
* **Docker** for Docker Compose and Testcontainers-backed integration tests.

### 🐳 Run with Docker Compose (API + DB)

One command builds and runs the complete development stack:

- **API** (Spring Boot)
- **Database** (PostgreSQL + Flyway migrations)

#### ▶️ Start

1) Copy environment defaults:

```bash
cp .env.example .env
```

2) Set a strong JWT signing secret in `.env` (`JWT_SECRET`, min 32 chars).

3) Build & start the stack:

```bash
docker compose up --build
```

4) Verify:

* Health: **[http://localhost:8080/health](http://localhost:8080/health)** → `200`
* Swagger UI: **[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)**

#### 📌 Notes

* On startup, **Flyway automatically applies database migrations**.
* The API runs with the `dev` profile when started via Docker Compose, including demo seed data.
* If port `8080` is busy, set `APP_PORT=8081` in `.env` and re-run.
* `JWT_SECRET` is required; startup fails if missing/weak.
* Stop with `Ctrl + C` (foreground) or `docker compose down`.

### 🧪 Tests

```bash
mvn -q verify
```

Run this command with a Java 21 JDK. Newer JDKs are rejected by Maven Enforcer so failures are explicit instead of surfacing as test framework incompatibilities.

* Integration tests that hit PostgreSQL use **Testcontainers**.
* The test suite verifies that the baseline database schema is applied.

### 🚩 Feature Flags

Agency-hotel authorization enforcement is controlled by:

```yaml
features:
  enforce-agency-hotel-auth: false
```

When enabled, AGENCY create/read/list operations require an `ACTIVE` `agency_hotel_auth` row
for the target hotel whose validity window includes the reservation `arrival_date`.

### 🔐 Authentication (JWT)

Docker Compose runs the API with the `dev` profile and seeds these demo users:

| Role | Email | Password |
|------|-------|----------|
| ADMIN | `admin@reshub.local` | `secret123` |
| MANAGER | `manager@reshub.local` | `secret123` |
| RECEPTIONIST | `reception@reshub.local` | `secret123` |
| AGENCY | `agency@reshub.local` | `secret123` |

1. Request a token:

```bash
curl -X POST http://localhost:8080/auth/token \
  -H "Content-Type: application/json" \
  -d '{"email":"manager@reshub.local","password":"secret123"}'
```

2. Use the `accessToken` value as bearer token:

```bash
curl http://localhost:8080/reservations \
  -H "Authorization: Bearer <accessToken>"
```

#### 📌 Notes

* Reservation endpoints use Spring Security bearer authentication.
* JWT claims are resolved into `SecurityContext`; service-level RBAC still enforces tenant scope and ownership.
* Legacy actor headers (`X-User-Id`, `X-Role`, `X-Hotel-Id`, `X-Agency-Id`) are not accepted as authentication.
* Demo users are loaded only by the `dev` Flyway location (`classpath:db/dev`), not by test or production profiles.
* If agency-hotel auth is enabled and denied, API returns `403` with code `agency_not_authorized_for_hotel`.

### 📤 Export Endpoints

Use the same role-scoped filters as `GET /reservations`:

* JSON: `GET /reservations/export?status=&arrivalFrom=&arrivalTo=&guestQuery=`
* CSV: `GET /reservations/export.csv?status=&arrivalFrom=&arrivalTo=&guestQuery=`

---

## 🗺️ Milestones

* [x] Bootstrap application with `/health` and Swagger UI
* [x] CI for PRs and `main` (build + tests)
* [x] Database baseline (PostgreSQL + Flyway)
* [x] Auth (JWT) + token issuance
* [x] Roles enforcement (service/API scope checks)
* [x] Reservations CRUD + list pagination (RBAC-scoped)
* [x] Integration tests (Testcontainers)

---

## ✍️ Author

**Ruben R.P.** — Backend Developer

* GitHub: 👤 [https://github.com/rubenrzprz](https://github.com/rubenrzprz)
* LinkedIn: 💼 [https://www.linkedin.com/in/ruben-rp/](https://www.linkedin.com/in/ruben-rp/)
