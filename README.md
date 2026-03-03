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
- 🔐 Role-based access control for **ADMIN**, **MANAGER**, **RECEPTIONIST**, **AGENCY**
- 🔎 Search by date range, status, and free-text guest; pagination & sorting
- 📤 CSV/JSON export of filtered results
- 🧭 Idempotent `externalRef` per (hotel, agency)
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
| 🔁 **Migrations** | Flyway                                    |
| 🧪 **Testing**    | JUnit 5, Spring Boot Test, Testcontainers |
| 📚 **API Docs**   | OpenAPI (springdoc)                       |
| ▶️ **Runtime**    | Docker / Docker Compose                   |

---

## 📐 Architecture

High-level overview of the domain & data model. For the full spec, see **[docs/Architecture.md](docs/Architecture.md)**.

- **Tenancy:** single DB; entities scoped by `hotel_id` where applicable.
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

### 🐳 Run with Docker Compose (API + DB)

One command builds and runs the complete development stack:

- **API** (Spring Boot)
- **Database** (PostgreSQL + Flyway migrations)

#### ▶️ Start

1) Copy environment defaults:

```bash
cp .env.example .env
```

2. Build & start the stack:

```bash
docker compose up --build
```

3. Verify:

* Health: **[http://localhost:8080/health](http://localhost:8080/health)** → `200`
* Swagger UI: **[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)**

#### 📌 Notes

* On startup, **Flyway automatically applies database migrations**.
* The API runs with the `dev` profile when started via Docker Compose.
* If port `8080` is busy, set `APP_PORT=8081` in `.env` and re-run.
* Stop with `Ctrl + C` (foreground) or `docker compose down`.

### 🧪 Tests

```bash
mvn -q test
```

* Integration tests that hit PostgreSQL use **Testcontainers**.
* The test suite verifies that the baseline database schema is applied.

---

## 🗺️ Milestones

* [x] Bootstrap application with `/health` and Swagger UI
* [x] CI for PRs and `main` (build + tests)
* [x] Database baseline (PostgreSQL + Flyway)
* [ ] Auth (JWT) + token issuance
* [x] Roles enforcement (service/API scope checks)
* [x] Reservations CRUD + list pagination (RBAC-scoped)
* [x] Integration tests (Testcontainers)

---

## ✍️ Author

**Ruben R.P.** — Backend Developer

* GitHub: 👤 [https://github.com/rubenrzprz](https://github.com/rubenrzprz)
* LinkedIn: 💼 [https://www.linkedin.com/in/ruben-rp/](https://www.linkedin.com/in/ruben-rp/)
