# Documentation

These documents specify the first implementation of the Parking Management System. No application features are implemented yet. A specification marked **Ready** has enough detail to implement; it does not indicate delivered functionality.

## Reading order

1. [Project overview](../README.md): purpose, scope, and intended startup.
2. [Implementation plan](PLAN.md): delivery sequence and definition of done.
3. [Architecture](architecture.md): boundaries and shared technical decisions.
4. [Feature specifications](#feature-specifications): detailed business behavior.
5. [API contract](api.md), [database design](database.md), and [frontend behavior](frontend.md).
6. [Development guide](development.md) and [testing strategy](testing.md).

## Feature specifications

| Spec | Scope | Specification status |
| --- | --- | --- |
| [001 — Demo user selection](specs/001-demo-user-selection.md) | Users, ownership context, vehicles, switching users | Ready |
| [002 — Wallet top-up](specs/002-wallet-top-up.md) | Money validation, balance updates, account limits | Ready |
| [003 — Parking lifecycle](specs/003-parking-lifecycle.md) | Start, active sessions, stop, pricing | Ready |
| [004 — Parking payments](specs/004-parking-payments.md) | Explicit payment, insufficient funds, atomicity | Ready |
| [005 — Parking history](specs/005-parking-history.md) | Completed sessions, unpaid items, history display | Ready |
| [006 — City and zone selection](specs/006-city-zone-selection.md) | City catalog, active zones, selection dependencies | Ready |

## Document ownership

| Document | Authoritative content |
| --- | --- |
| Feature specs | Business rules and feature acceptance criteria |
| `api.md` | Routes, request/response shapes, status codes, error codes |
| `database.md` | Tables, columns, constraints, indexes, migrations, seed data |
| `architecture.md` | Shared design decisions, transaction boundaries, locking |
| `frontend.md` | Page structure, state handling, request coordination, accessibility |
| `development.md` | Planned setup and operational commands |
| `testing.md` | Verification method and release smoke test |
| `PLAN.md` | Scope, implementation order, delivery tracking |

Examples illustrate their linked contracts. When behavior changes, update the owning document and affected examples in the same change. Resolve contradictions before implementing the affected behavior.

## Adding or changing functionality

1. For a substantial behavior change, copy [the feature template](specs/_template.md) to the next numbered filename.
2. Describe the user outcome, rules, failure cases, dependencies, and observable acceptance criteria.
3. Resolve required decisions and mark the specification **Ready**.
4. Update shared contracts where the feature changes API, database, architecture, or UI behavior.
5. Implement the feature and relevant tests. Mark it **Implemented** only after its acceptance criteria pass; record the verification performed.
6. Update the plan's progress checklist and project status when applicable.

Use **Draft**, **Ready**, **In progress**, and **Implemented** as status values. A task description is sufficient for small fixes and cosmetic changes that do not introduce new behavior. Add a separate decision document only when a complex decision needs more explanation than the architecture document can hold.

## Original plan and resolved decisions

The original 46-section plan is preserved unchanged in [reference/original-plan.md](reference/original-plan.md). It records the original brief; the maintained documents define the implementation contract.

The following decisions resolve gaps or deliberately refine the original examples:

- Store timestamps in UTC with millisecond precision and serialize an explicit `Z` offset.
- Charge at least one hour, including a session stopped at the same recorded instant.
- Capture the hourly rate when a session starts.
- Serialize monetary API values as decimal strings and use the same representation for top-up input.
- Use one consistent parking response shape; active parking has no payment status yet.
- Require `userId` in both stop and payment request bodies.
- Return completed sessions only in history; derive the unpaid section from history.
- Serialize mutations for each user using database row locks, and require uniqueness safeguards.
- Include targeted PostgreSQL integration tests for rollback and concurrency.

These are implementation decisions for this prototype, not claims that they were explicitly required by the original assignment.
