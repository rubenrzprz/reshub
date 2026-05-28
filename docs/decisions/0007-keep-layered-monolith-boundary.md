# 0007 - Keep a layered monolith boundary

## Context

ResHub is a backend portfolio project for a single hotel reservation domain.
The project needs to show serious backend design without adding distributed-system complexity that the problem does not require.

## Decision

Keep the application as a layered Spring Boot monolith with clear API, service, persistence, migration, and test boundaries.
Do not split into microservices for the current scope.

## Rationale

A monolith is easier to run, test, review, and explain for this domain.
It keeps the focus on correctness, data modeling, transactions, validation, security, and API design instead of infrastructure.

## Trade-offs

- Module boundaries are enforced by package structure and discipline rather than deployment boundaries.
- Independent scaling is not available per domain area.
- Future teams would need to watch for service-layer coupling as features grow.

## Future Evolution

If the domain expanded substantially, bounded contexts could first be separated into modules before considering service extraction.
