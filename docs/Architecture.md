# 🏗️ ResHub — Architecture (Domain & Data Model)

**Version:** 0.4 (MVP)  
**Status:** Draft — authoritative design for MVP; kept in sync with migrations

---

## 📚 Table of Contents
1. [🎯 Scope & Non-Goals](#1--scope--non-goals)
2. [🏷️ Tenancy & Conventions](#2--tenancy--conventions)
3. [🔐 Roles & Access (RBAC surface)](#3--roles--access-rbac-surface)
4. [🧩 Entities & Relationships](#4--entities--relationships)  
   - [4.1 🏨 hotel](#41--hotel)  
   - [4.2 🤝 agency](#42--agency)  
   - [4.3 👤 app_user](#43--app_user)  
   - [4.4 🛏️ room_type (hotel-scoped)](#44--room_type-hotel-scoped)  
   - [4.5 🔗 room_type_channel_map](#45--room_type_channel_map)  
   - [4.6 🧾 reservation](#46--reservation)  
   - [4.7 💬 reservation_comment](#47--reservation_comment)  
   - [4.8 ✅ agency_hotel_auth (coarse whitelist)](#48--agency_hotel_auth-coarse-whitelist)  
   - [4.9 🧾 agency_hotel_room_type_allow (optional granular)](#49--agency_hotel_room_type_allow-optional-granular)
5. [📏 Business Rules](#5--business-rules)
6. [🧪 Room Type Canonicalization (RAW → canonical)](#6--room-type-canonicalization-raw--canonical)
7. [🔎 Index Strategy (MVP)](#7--index-strategy-mvp)
8. [🗂️ Migrations Policy & Plan (V1–V4)](#8--migrations-policy--plan-v1v4)
9. [📜 API Contract Notes (role-aware)](#9--api-contract-notes-role-aware)
10. [📈 Observability (late binding binder)](#10--observability-late-binding-binder)
11. [🛡️ Security & Privacy Notes](#11--security--privacy-notes)
12. [❓ Open Questions](#12--open-questions)
13. [📝 Change Log](#13--change-log)

---

## 1) 🎯 Scope & Non-Goals

**MVP In-scope**
- Single database; multi-tenancy by `hotel_id`.
- Entities: `hotel`, `agency`, `app_user`, `room_type`, `room_type_channel_map`, `reservation`, `reservation_comment`.
- Reservation lifecycle: `NEW → CONFIRMED → CANCELLED | NOSHOW`; “never delete, only cancel”.
- Idempotency: unique `(hotel_id, agency_id, external_ref)`.
- Search: date range, status filters, free-text guest; pagination; CSV/JSON export.
- Room types: hotel-scoped, JSONB attributes with canonicalization; channel mapping; **late binding** for agency bookings.

**Non-goals (defer)**
- Inventory/capacity & overbooking logic, pricing/contracts, outbox/webhooks, advanced full-text, payments.

---

## 2) 🏷️ Tenancy & Conventions

- **Tenancy:** single DB; reference & operational rows scoped by `hotel_id` where applicable.
- **Persistence:** application code uses Spring `JdbcTemplate` with explicit SQL. Flyway migrations define the schema; there are no JPA entities or Hibernate-managed DDL in the MVP.
- **PKs:** UUID (application-assigned) — avoids DB extensions.
- **Time:** `TIMESTAMPTZ` in UTC; JVM `-Duser.timezone=UTC`.  
- **Naming:** snake_case table/column names; singular table names.
- **Soft state:** `active BOOLEAN` for reference entities; reservations use lifecycle, not deletes.

---

## 3) 🔐 Roles & Access (RBAC surface)

- **MANAGER** — hotel-wide access within their hotel.
- **RECEPTIONIST** — CRUD on own-created reservations; may comment on any reservation in their hotel.
- **AGENCY** — CRUD only on reservations created by that agency; cannot post comments after creation (may include a one-time `notes` at create).
- **ADMIN** — global cross-tenant reservation/comment access for support and operational recovery.

(Enforcement in service/API layer with standardized `ProblemDetail` codes; DB encodes integrity + lifecycle constraints.)

---

## 4) 🧩 Entities & Relationships

### ER (text)
- `hotel (1) ──< app_user (many; staff)`  
- `agency (1) ──< app_user (many; agency users)`  
- `hotel (1) ──< room_type (many)`  
- `room_type (1) ──< room_type_channel_map (many)`  
- `hotel (1) ──< reservation (many)`  
- `agency (1) ──< reservation (many)`  
- `app_user (1) ──< reservation (many; created_by)`  
- `reservation (1) ──< reservation_comment (many)`  

> In `app_user` exactly one of `hotel_id` or `agency_id` is set (XOR).

---

### 4.1 🏨 hotel
- `id UUID PK`
- `code VARCHAR(24) UNIQUE NOT NULL`
- `name VARCHAR(120) NOT NULL`
- `active BOOLEAN NOT NULL DEFAULT TRUE`
- `created_at TIMESTAMPTZ NOT NULL DEFAULT now()`
- `updated_at TIMESTAMPTZ NOT NULL DEFAULT now()`

**Indexes**
- `UNIQUE(code)`

---

### 4.2 🤝 agency
- `id UUID PK`
- `code VARCHAR(24) UNIQUE NOT NULL`
- `name VARCHAR(120) NOT NULL`
- `active BOOLEAN NOT NULL DEFAULT TRUE`
- `created_at TIMESTAMPTZ NOT NULL DEFAULT now()`
- `updated_at TIMESTAMPTZ NOT NULL DEFAULT now()`

**Indexes**
- `UNIQUE(code)`

---

### 4.3 👤 app_user
- `id UUID PK`
- `email VARCHAR(254) UNIQUE NOT NULL` *(lower-cased by app)*
- `password_hash VARCHAR(72) NOT NULL` *(BCrypt)*
- `role VARCHAR(24) NOT NULL` *(ADMIN | MANAGER | RECEPTIONIST | AGENCY)*
- `hotel_id UUID NULL REFERENCES hotel(id) ON DELETE RESTRICT`
- `agency_id UUID NULL REFERENCES agency(id) ON DELETE RESTRICT`
- `CHECK ((hotel_id IS NULL) <> (agency_id IS NULL))`
- `created_at TIMESTAMPTZ NOT NULL DEFAULT now()`
- `updated_at TIMESTAMPTZ NOT NULL DEFAULT now()`

**Indexes**
- `UNIQUE(email)`
- `IDX app_user_hotel (hotel_id)`, `IDX app_user_agency (agency_id)`

---

### 4.4 🛏️ room_type (hotel-scoped)
- `id UUID PK`
- `hotel_id UUID NOT NULL REFERENCES hotel(id) ON DELETE RESTRICT`
- `code VARCHAR(32) NOT NULL` *(unique per hotel)*
- `name VARCHAR(120) NOT NULL` *(marketing name)*
- `class VARCHAR(24) NULL` *(STANDARD | SUPERIOR | DELUXE | SUITE | JR_SUITE)*
- `base_occupancy SMALLINT NOT NULL` *(included guests in base rate)*
- `max_occupancy SMALLINT NOT NULL` *(hard cap)*
- `attributes_raw JSONB NOT NULL` *(verbatim upstream)*
- `attributes_canonical JSONB NOT NULL` *(normalized domain shape)*
- `flexible_bedding BOOLEAN NOT NULL DEFAULT FALSE`
- `created_at TIMESTAMPTZ NOT NULL DEFAULT now()`
- `updated_at TIMESTAMPTZ NOT NULL DEFAULT now()`

**Indexes**
- `UNIQUE (hotel_id, code)`
- `UNIQUE (hotel_id, id)` *(supports composite FK from room_type_channel_map)*
- `GIN (attributes_canonical)`

**Canonical JSON (examples)**
```json
{
  "view": "sea",
  "balcony": true,
  "size_m2": 32,
  "beds": [
    { "type": "twin", "size_cm": 90, "count": 2 }
  ],
  "kitchenette": false,
  "jacuzzi": false,
  "accessibility": ["wheelchair"],
  "flexible_bedding": false
}
````

---

### 4.5 🔗 room_type_channel_map

* `id UUID PK`
* `hotel_id UUID NOT NULL REFERENCES hotel(id) ON DELETE RESTRICT`
* `room_type_id UUID NOT NULL`
* `channel VARCHAR(32) NOT NULL`  *(or agency code)*
* `external_code VARCHAR(64) NOT NULL`
* `external_name VARCHAR(160) NULL`
* `meta_json JSONB NULL`
* `created_at TIMESTAMPTZ NOT NULL DEFAULT now()`

**Indexes**

* `UNIQUE (hotel_id, channel, external_code)`

**Constraints**

* `FOREIGN KEY (hotel_id, room_type_id) REFERENCES room_type(hotel_id, id) ON DELETE RESTRICT`

---

### 4.6 🧾 reservation

* `id UUID PK`
* `hotel_id UUID NOT NULL REFERENCES hotel(id) ON DELETE RESTRICT`
* `agency_id UUID NOT NULL REFERENCES agency(id) ON DELETE RESTRICT`
* `created_by_user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE RESTRICT`
* `room_type_id UUID NULL` *(nullable for late binding)*
* `external_room_type_code VARCHAR(64) NULL` *(required at API for AGENCY)*
* `external_room_type_name VARCHAR(160) NULL`
* `external_ref VARCHAR(64) NOT NULL` *(idempotency key)*
* `status VARCHAR(16) NOT NULL` *(NEW | CONFIRMED | CANCELLED | NOSHOW)*
* `arrival_date DATE NOT NULL`
* `departure_date DATE NOT NULL`
* `guest_name VARCHAR(160) NOT NULL`
* `guest_email VARCHAR(254) NULL`
* `guest_phone VARCHAR(32) NULL`
* `adults SMALLINT NOT NULL DEFAULT 1`
* `children SMALLINT NOT NULL DEFAULT 0`
* `notes TEXT NULL`
* `cancelled_at TIMESTAMPTZ NULL`
* `cancel_reason VARCHAR(160) NULL`
* `created_at TIMESTAMPTZ NOT NULL DEFAULT now()`
* `updated_at TIMESTAMPTZ NOT NULL DEFAULT now()`

**Constraints**

* `arrival_date < departure_date`
* `adults >= 1`, `children >= 0`
* `status='CANCELLED' => cancelled_at IS NOT NULL`
* `status<>'CANCELLED' => cancelled_at IS NULL`

**Uniqueness**

* `UNIQUE (hotel_id, agency_id, external_ref)`

**FK integrity**

* `FOREIGN KEY (hotel_id, room_type_id) REFERENCES room_type(hotel_id, id) ON DELETE RESTRICT`

**Indexes**

* `(hotel_id, arrival_date)`
* `(hotel_id, status)`

---

### 4.7 💬 reservation_comment (staff-internal)

* `id UUID PK`
* `reservation_id UUID NOT NULL REFERENCES reservation(id) ON DELETE CASCADE`
* `author_user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE RESTRICT`
* `body TEXT NOT NULL`
* `created_at TIMESTAMPTZ NOT NULL DEFAULT now()`

**Policy**

* Post-creation comments allowed for ADMIN/MANAGER/RECEPTIONIST; agencies cannot comment post-create.

---

### 4.8 ✅ agency_hotel_auth (coarse whitelist; feature-flagged)

* `id UUID PK`
* `agency_id UUID NOT NULL REFERENCES agency(id)`
* `hotel_id UUID NOT NULL REFERENCES hotel(id)`
* `status VARCHAR(12) NOT NULL` *(ACTIVE | SUSPENDED | ENDED)*
* `valid_from DATE NULL`, `valid_to DATE NULL`
* `terms_json JSONB NULL`
* `created_at TIMESTAMPTZ NOT NULL DEFAULT now()`
* `updated_at TIMESTAMPTZ NOT NULL DEFAULT now()`

**Indexes**

* `UNIQUE (agency_id, hotel_id)`
* `IDX auth_agency (agency_id)`, `IDX auth_hotel (hotel_id)`

**Semantics**

* If feature flag `enforceAgencyHotelAuth=true`: at create/read/list (AGENCY), require ACTIVE row whose validity window covers the reservation arrival date.

---

### 4.9 🧾 agency_hotel_room_type_allow (optional granular)

* `auth_id UUID NOT NULL REFERENCES agency_hotel_auth(id) ON DELETE CASCADE`
* `room_type_id UUID NOT NULL REFERENCES room_type(id) ON DELETE RESTRICT`
* `PRIMARY KEY (auth_id, room_type_id)`

**Semantics**

* If rows exist for an auth, they represent the only allowed room types; if none, treat as “all allowed”.

---

## 5) 📏 Business Rules

* **Lifecycle**:

  * Allowed: `NEW → CONFIRMED | CANCELLED | NOSHOW`
  * Allowed: `CONFIRMED → CANCELLED | NOSHOW`
  * Terminal: `CANCELLED`, `NOSHOW` (non-mutable; row updates rejected by DB trigger)
  * On `CANCELLED`, set `cancelled_at` and optional `cancel_reason`.
* **Never delete reservations**; enforce with status, not deletions.
* **Idempotency**: `(hotel_id, agency_id, external_ref)` uniquely identifies an external booking attempt.
* **RBAC** (service/API layer):

  * ADMIN: global cross-tenant read/write access for reservations/comments.
  * AGENCY: own reservations only; no post-create comments.
  * RECEPTIONIST: CRUD own; comment any in hotel.
  * MANAGER: hotel-wide.
* **Audit**: successful ADMIN write operations emit structured audit logs with actor, action, target id, and timestamp.
* **Room mapping**:

  * AGENCY create: **require** `external_room_type_code`; resolve `room_type_id` via mapping; if not found, store external fields and **late bind**.
  * STAFF create: **require** `room_type_id` from internal catalog.

---

## 6) 🧪 Room Type Canonicalization (RAW → canonical)

* **Two-layer JSON** on `room_type`:

  * `attributes_raw` — verbatim upstream payload(s).
  * `attributes_canonical` — normalized **domain shape** with controlled enums/keys.
* **Synonym dictionary**: per-agency map (checked-in YAML/JSON) that normalizes values (e.g., “matrimonial” → `double`; “individual bed” → `twin`).
* **JSON Schema** validates `attributes_canonical` (e.g., `beds[].type ∈ {twin,double,queen,king,sofa_bed,bunk,rollaway,crib,futon}`; `count ≥ 1`; optional `size_cm`).
* **Permissive mode (MVP)**: unknown keys remain in RAW; canonical includes only whitelisted keys.
* **Flag** for flexible bedding scenarios: `flexible_bedding: true`.

---

## 7) 🔎 Index Strategy (MVP)

* **reservation**:

  * `(hotel_id, arrival_date)` — arrivals queries.
  * `(hotel_id, status)` — operational filters.
* **room_type**:

  * `UNIQUE (hotel_id, code)` — catalog integrity.
  * `GIN (attributes_canonical)` — JSON containment filters.
* Later (if needed): functional indexes for text (`LOWER(guest_name)`), trgm for fuzzy, etc.

---

## 8) 🗂️ Migrations Policy & Plan (V1–V5)

**Policy**

* Files named `V{N}__{short-kebab}.sql`; never edit past migrations.
* One concern per file; keep reversible and small.
* Production parity; avoid permanent seed data in Flyway.

**Plan**

* **V1 — Baseline**: `hotel`, `agency`, `app_user` (+ constraints/indexes).
* **V2 — Room modeling**: `room_type`, `room_type_channel_map` (+ GIN index).
* **V3 — Room type channel map integrity**: FK `room_type_channel_map.hotel_id → hotel(id)` and composite FK `(hotel_id, room_type_id) → room_type(hotel_id, id)`.
* **V4 — Reservations & comments**: `reservation`, `reservation_comment` (+ constraints/indexes; nullable `room_type_id`; idempotency).
* **V5 — Agency authorization (feature-flagged)**: `agency_hotel_auth`, `agency_hotel_room_type_allow`.

---

## 9) 📜 API Contract Notes (role-aware)

* **AGENCY create**: require `external_room_type_code`; optional `external_room_type_name`. Attempt mapping; late bind if unresolved.
* **STAFF create**: require `room_type_id` (must belong to the same `hotel_id`).
* **ADMIN access**: may read/write reservations and comments across all hotels/agencies.
* **List endpoint**: `GET /reservations` uses keyset pagination ordered by `(arrival_date ASC, id ASC)`.
* **ProblemDetail** codes to standardize:

  * `unauthorized_actor_context`
  * `forbidden_scope`
  * `reservation_not_found`
  * `duplicate_external_ref`
  * `invalid_status_transition`
  * `invalid_reservation_payload`
  * `invalid_comment_payload`
  * `invalid_cursor`
  * `invalid_limit`
  * `agency_not_authorized_for_hotel` (when flag enabled)
  * `room_type_not_authorized` (when flag enabled + granular list active)

---

## 10) 📈 Observability (late binding binder)

* **Approach (MVP)**: scheduled task within the API (single container), optional ShedLock for multi-replica.
* **Metrics**: `binder.scanned`, `binder.bound`, `binder.alreadyBound`, `binder.errors`.
* **Logs**: correlation by reservation id; summary per run; backoff on errors.

---

## 11) 🛡️ Security & Privacy Notes

* **Passwords**: store only BCrypt hashes; never plaintext.
* **PII**: guest_* fields are PII; no debug logging of values; redact in logs.
* **Audit**: comments are internal; avoid PII spillover.

---

## 12) ❓ Open Questions

* Do we need an **agency↔hotel authorization** join right away (flag off by default), or later? *(Planned V5; flag default false).*
* Add monetary fields (`total_amount_minor`, `currency`, tax semantics) when pricing enters scope? *(Deferred.)*
* Any per-hotel **rate plans** to keep as external code only in MVP? *(Likely yes; defer modeling.)*

---

## 13) 📝 Change Log

* v0.4 — Implemented V5 agency authorization schema and feature-flagged AGENCY create/read/list enforcement with `agency_not_authorized_for_hotel`.
* v0.3 — Added reservations/comments lifecycle constraints (V4), RBAC service/API enforcement, and standardized `ProblemDetail` error codes.
* v0.2 — Added room_type_channel_map hotel integrity constraints (V3).
* v0.1 — Initial MVP model (this document).
