# 0001 - Migrate Core Persistence to Spring Data JPA

## Context

ResHub is a Spring Boot portfolio backend. Before this decision, the implementation used Flyway for schema ownership and `JdbcTemplate` for all production persistence:

- `AuthService` queries `app_user` for token issuance.
- `AgencyHotelAuthorizationService` checks agency-hotel authorization rows.
- `ReservationCommandService` inserts and mutates reservations/comments.
- `ReservationQueryService` handles role-scoped reads, filters, keyset pagination, and export queries.

The project already depended on Spring Data JPA and configured Hibernate validation, but it had no entities or repositories. That made the persistence architecture look inconsistent in an interview.

## Decision

Migrate core persistence to Spring Data JPA while keeping Flyway as the only schema migration mechanism.

Core aggregate persistence moves first:

- `hotel`
- `agency`
- `app_user`
- `reservation`
- `reservation_comment`
- `agency_hotel_auth`

Query-heavy paths keep explicit SQL where it is clearer or more efficient, especially:

- role-scoped reservation listing
- keyset pagination ordered by `(arrival_date, id)`
- CSV/JSON export filters
- authorization existence checks

## Rationale

For Java/Spring Boot interviews, JPA entities and repositories are a familiar signal. They show that the project can use the mainstream Spring persistence model rather than only raw SQL.

JPA also gives a cleaner place to express aggregate structure and repository boundaries. That helps separate business logic from SQL strings currently embedded in services.

Flyway remains responsible for DDL because the project already has a strong PostgreSQL schema, constraints, and migration tests. Hibernate should validate mapping compatibility, not generate or mutate schema.

## Trade-offs

JPA adds ORM complexity, especially around lazy loading, transaction boundaries, and entity state. Not every query should be forced into derived repository methods.

Keeping selected explicit SQL is acceptable when it protects clarity or performance. The goal is not to remove SQL completely; the goal is to make the default persistence style conventional and intentional.

## Implemented Boundary

- JPA entities match the existing Flyway schema for core aggregate tables.
- Repositories support core lookup and command paths.
- `AuthService` uses `AppUserRepository`.
- `AgencyHotelAuthorizationService` uses `AgencyHotelAuthRepository`.
- `ReservationCommandService` uses repositories for reservation/comment create and mutation paths.
- `ReservationQueryService` keeps explicit `JdbcTemplate` SQL for role-scoped list/read/export behavior.
- `spring.jpa.hibernate.ddl-auto=validate` stays enabled in dev/test to catch entity-schema drift.

## Future Evolution

If the JPA migration creates more complexity than value in a specific query path, keep that path explicit and document why. The final architecture can be hybrid: JPA for aggregate persistence, explicit SQL for operational reporting and role-scoped search.
