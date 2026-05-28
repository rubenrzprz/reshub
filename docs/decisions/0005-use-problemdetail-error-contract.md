# 0005 - Use ProblemDetail error responses

## Context

Clients need predictable API errors for validation failures, authentication failures, forbidden scope, not found, conflicts, and malformed requests.
Scattering error response construction across controllers would make the contract inconsistent.

## Decision

Use Spring `ProblemDetail` as the standard error shape and centralize mapping in the global exception handler.
Each application error includes a stable `code` property that tests and clients can rely on.

## Rationale

`ProblemDetail` is the Spring Boot 3-native representation for RFC 9457-style API errors.
Stable error codes are easier to assert in tests and easier for clients to handle than parsing human-readable messages.

## Trade-offs

- Error codes must be maintained as part of the API contract.
- Some low-level exceptions need explicit mapping to avoid leaking implementation details.
- The project still keeps the contract intentionally compact rather than modeling every possible error subtype.

## Future Evolution

OpenAPI examples can be expanded so Swagger shows the common error bodies for each endpoint.
