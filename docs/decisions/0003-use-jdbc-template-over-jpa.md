# 0003 - Use JdbcTemplate Over JPA

## Context

ResHub is a compact backend portfolio project with a PostgreSQL-first data model, Flyway-managed schema, role-scoped queries, keyset pagination, and database constraints for reservation integrity.

The codebase already uses explicit SQL through Spring `JdbcTemplate`. Keeping Spring Data JPA in the build without entities or repositories made the persistence story look accidental.

## Decision

Use Spring `JdbcTemplate` and explicit SQL for the MVP persistence layer. Remove the unused Spring Data JPA dependency and Hibernate-specific configuration.

## Rationale

This keeps the database contract visible and aligned with Flyway migrations. It also makes query behavior for RBAC scoping, filters, exports, and keyset pagination straightforward to inspect and test.

For this project, showing SQL literacy and schema ownership is more valuable than adding an ORM layer that would mostly wrap simple persistence operations.

## Trade-offs

`JdbcTemplate` requires manual row mapping and more explicit query maintenance. It also means the project is less conventional than a typical Spring Data JPA CRUD service.

The trade-off is intentional: ResHub prioritizes transparent SQL, PostgreSQL constraints, and predictable query behavior over ORM abstraction.

## Future Evolution

If the model grows enough that entity state management, relationship traversal, or repository conventions become valuable, selected areas can move to Spring Data JPA. Complex reporting, exports, and authorization-scoped queries may still remain explicit SQL.
