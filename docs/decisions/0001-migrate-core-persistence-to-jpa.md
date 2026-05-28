# 0001 - Migrate Core Persistence to Spring Data JPA

## Context

ResHub is a Spring Boot portfolio backend. The current implementation uses Flyway for schema ownership and `JdbcTemplate` for all production persistence:

- `AuthService` queries `app_user` for token issuance.
- `AgencyHotelAuthorizationService` checks agency-hotel authorization rows.
- `ReservationCommandService` inserts and mutates reservations/comments.
- `ReservationQueryService` handles role-scoped reads, filters, keyset pagination, and export queries.

The project already depends on Spring Data JPA and configures Hibernate validation, but it has no entities or repositories. That makes the persistence architecture look inconsistent in an interview.

## Decision

Migrate core persistence to Spring Data JPA in a follow-up implementation PR, while keeping Flyway as the only schema migration mechanism.

Core aggregate persistence should move first:

- `hotel`
- `agency`
- `app_user`
- `reservation`
- `reservation_comment`
- `agency_hotel_auth`

Query-heavy paths may keep explicit SQL where it is clearer or more efficient, especially:

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

## Implementation Plan

1. Add JPA entities matching the existing Flyway schema.
2. Add repositories for core lookup and command paths.
3. Move `AuthService` user lookup to an `AppUserRepository`.
4. Move simple reservation/comment create and status update paths to repositories.
5. Keep complex list/export queries explicit initially, either through `JdbcTemplate` or custom repository implementations.
6. Keep `spring.jpa.hibernate.ddl-auto=validate` in dev/test to catch entity-schema drift.
7. Add focused tests for repository mappings and preserve existing API integration tests.

## Future Evolution

If the JPA migration creates more complexity than value in a specific query path, keep that path explicit and document why. The final architecture can be hybrid: JPA for aggregate persistence, explicit SQL for operational reporting and role-scoped search.
