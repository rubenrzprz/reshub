# 0002 - Use Spring Security for JWT authentication

## Context

Reservation endpoints need authenticated actor context for RBAC and tenant-scoped business rules.
The first implementation used a custom servlet filter that parsed JWTs and injected internal actor headers.
That worked for an MVP, but it was less idiomatic for a Spring Boot backend and harder to defend in interviews.

## Decision

Use Spring Security as the HTTP authentication boundary.
JWT bearer tokens are parsed by a `OncePerRequestFilter`, converted into an authenticated principal, and stored in `SecurityContext`.
Reservation controllers resolve the current `RequestActor` from the authenticated principal.

## Rationale

This keeps authentication aligned with common Spring Boot expectations while preserving the existing domain authorization model.
Endpoint authentication is handled by Spring Security.
Reservation-specific RBAC, tenant scope, ownership, and agency-hotel authorization stay in services because those rules depend on loaded reservation data.

## Trade-offs

- Spring Security adds framework complexity compared with a small custom filter.
- The project still uses a compact JWT implementation instead of a full OAuth2/OIDC resource server.
- Method-level annotations are not used for tenant decisions because those checks need domain state.

## Future Evolution

If this project moved closer to production, the JWT issuer could be externalized to an OAuth2/OIDC provider and the filter could be replaced with Spring Security resource server support.
