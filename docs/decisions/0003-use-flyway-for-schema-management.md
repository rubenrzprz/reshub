# 0003 - Use Flyway for schema management

## Context

ResHub uses PostgreSQL and needs a repeatable way to create and evolve schema across local development, CI, and Testcontainers integration tests.
Relying on Hibernate DDL generation would make schema ownership less explicit and harder to review.

## Decision

Use Flyway versioned migrations as the source of truth for database schema.
Hibernate runs with `ddl-auto=validate` so JPA mappings are checked against the Flyway-managed schema instead of generating tables.

## Rationale

Flyway keeps database changes reviewable, ordered, and reproducible.
It also lets the project demonstrate SQL constraints, indexes, triggers, and PostgreSQL-specific features directly.

## Trade-offs

- Schema changes require explicit migration files.
- JPA entity changes can fail startup until the matching migration exists.
- Developers must understand both JPA mappings and SQL migrations.

## Future Evolution

For larger teams, migrations could include rollback guidance and stricter review rules for destructive changes.
