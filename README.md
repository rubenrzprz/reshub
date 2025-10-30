# ResHub — Hotel Reservation API (Spring Boot)

Secure, multi-tenant reservation API with **JWT + RBAC**, search (pagination/filters), and CSV/JSON exports.  
Status: **WIP**

## Why
Showcase delivering a real, secure backend with clear domain boundaries and tests.

## Tech (planned)
Java 21 · Spring Boot 3 · Maven · PostgreSQL · Docker · Testcontainers

## Roadmap (short)
- [ ] Bootstrap Spring Boot app
- [ ] Add CI (Maven + Java 21)
- [ ] DB (PostgreSQL) + Flyway
- [ ] Auth (JWT) + Roles (MANAGER, RECEPTIONIST, AGENCY)
- [ ] Reservations CRUD + search + exports
- [ ] Integration tests (Testcontainers)

## Notes
- UI: **Swagger-first**
- Deletion policy: **never delete, only cancel**
