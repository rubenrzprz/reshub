# ResHub — Hotel Reservation API (Spring Boot)

[![CI (Build & Test)](https://github.com/rubenrzprz/reshub/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/rubenrzprz/reshub/actions/workflows/ci.yml)

**Status:** WIP 🛠️

Secure, multi-tenant reservation API demonstrating **JWT + RBAC**, robust search (pagination/filters), and **CSV/JSON** exports. Built for clarity, testability, and easy demo.

---

## 📚 Table of Contents
- [🧭 Overview](#-overview)
- [✨ Features](#-features)
- [🧰 Tech Stack](#-tech-stack)
- [📐 Architecture](#-architecture)
- [🚀 Run & Explore](#-run--explore)
  - [🧪 Run locally (Maven)](#-run-locally-maven)
  - [🐳 Run with Docker Compose (API only)](#-run-with-docker-compose-api-only)
- [🗺️ Milestones](#-milestones)
- [✍️ Author](#-author)

---

## 🧭 Overview
ResHub showcases a production-style backend: authenticated operations, role-based permissions, clean error design, and repeatable local/CI runs.
See the full domain & data model in **[📐 Architecture](docs/Architecture.md)**.

---

## ✨ Features
- 🔐 JWT login with roles: **MANAGER**, **RECEPTIONIST**, **AGENCY**
- 🔎 Search by date range, status, and free-text guest; pagination & sorting
- 📤 CSV/JSON export of filtered results
- 🧭 Idempotent `externalRef` per (hotel, agency)
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
- **Idempotency:** unique `(hotel_id, agency_id, external_ref)`.
- **Room types:** hotel-scoped with `attributes_raw` + `attributes_canonical` (JSONB), GIN index; channel mapping; **late binding**.
- **Migrations:** V1 (baseline) → V2 (room modeling) → V3 (reservations/comments) → V4 (agency authorization, flag-gated).

---

## 🚀 Run & Explore

### 🧪 Run locally (Maven)
Clone the repository and start the service locally.

```bash
git clone https://github.com/rubenrzprz/reshub.git
cd reshub

# build
mvn -q -DskipTests package

# run
mvn spring-boot:run
````

* Base URL: **[http://localhost:8080](http://localhost:8080)**
* Swagger UI: **[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)**

> The server port can be configured in `src/main/resources/application.yml`.

### 🐳 Run with Docker Compose (API only)
One command to build and run the API container.

1) Copy environment defaults:
```bash
cp .env.example .env
````

2) Build & start:

```bash
docker compose up --build
```

3) Verify:

* Health: [http://localhost:8080/health](http://localhost:8080/health) → **200** `ResHub OK`
* Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

**Notes**

* If port 8080 is busy, set `APP_PORT=8081` in `.env` and re-run.
* Stop with `Ctrl + C` (foreground) or `docker compose down` (background).

---

## 🗺️ Milestones

* [x] Bootstrap application with `/health` and Swagger UI
* [x] CI for PRs and `main` (build + tests)
* [ ] Database baseline (PostgreSQL + Flyway)
* [ ] Auth (JWT) + Roles enforcement
* [ ] Reservations CRUD + search + exports
* [ ] Integration tests (Testcontainers)

---

## ✍️ Author

**Ruben R.P.** — Backend Developer

* GitHub: 👤 [https://github.com/rubenrzprz](https://github.com/rubenrzprz)
* LinkedIn: 💼 [https://www.linkedin.com/in/ruben-rp/](https://www.linkedin.com/in/ruben-rp/)

```
