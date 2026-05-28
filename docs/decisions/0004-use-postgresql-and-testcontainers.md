# 0004 - Use PostgreSQL and Testcontainers

## Context

Reservation behavior depends on database constraints, date filtering, JSONB room attributes, and migration correctness.
An in-memory database would not reliably match PostgreSQL behavior.

## Decision

Use PostgreSQL as the development, CI, and test database.
Use Testcontainers for integration tests that need a real PostgreSQL instance.

## Rationale

Testing against PostgreSQL catches migration, SQL, constraint, and dialect issues before merge.
It keeps local and CI behavior close to production assumptions without requiring developers to manage a long-lived local test database.

## Trade-offs

- Integration tests require Docker.
- Test startup is slower than in-memory database tests.
- CI depends on container support.

## Future Evolution

If the test suite grows, tests can be split into faster unit slices and slower database/API suites while keeping PostgreSQL for persistence verification.
