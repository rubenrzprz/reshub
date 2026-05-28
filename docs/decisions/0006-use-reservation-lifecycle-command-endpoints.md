# 0006 - Use reservation lifecycle command endpoints

## Context

Reservations have a small lifecycle: `NEW`, `CONFIRMED`, `CANCELLED`, and `NOSHOW`.
Status changes are business actions, not generic partial updates.

## Decision

Expose lifecycle transitions as command endpoints such as `/reservations/{id}/confirm`, `/cancel`, and `/noshow`.
Keep validation and authorization in the service layer, with database guardrails preventing invalid terminal-state mutations.

## Rationale

Command endpoints make allowed business actions explicit and easier to demo.
They also avoid ambiguous generic status updates where clients could attempt unsupported transitions.

## Trade-offs

- The API has more endpoints than a generic `PATCH /reservations/{id}` status field.
- Adding new transitions requires adding or documenting a new command.
- Some teams may prefer a generic state transition endpoint for larger workflows.

## Future Evolution

If the workflow grows, transitions could move behind a dedicated state-machine component while keeping the public command style.
