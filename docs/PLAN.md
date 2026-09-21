# Implementation Plan

Status: In progress — Phases 1–6 are complete; Phase 7 (payment and history) is next.

## Current checkpoint

Phase 6 (parking) is complete: start, active list, and stop follow the API contract with captured rates, millisecond UTC timestamps, started-hour pricing in `PricingService`, and user-then-session locking; the active-vehicle index is translated to `VEHICLE_ALREADY_PARKED` as a backstop. `PricingServiceTest` (14), `ParkingApiIT` (24), and `ParkingConcurrencyIT` (racing starts and stops) pass. Phase 5 (wallet) is complete: `POST /api/users/{id}/top-up` validates the JSON string token and exact format without rounding, enforces the inclusive balance limit, and updates under the user lock. `TopUpAmountTest` (29 unit cases), `WalletApiIT` (22 HTTP cases), and `TopUpConcurrencyIT` (two top-ups queued on a held row lock both apply) pass. Phase 4 (user and catalog reads) is complete: `GET /api/users`, `/api/users/{id}`, `/api/users/{id}/vehicles`, `/api/cities`, and `/api/cities/{id}/zones` follow the API contract, with the shared `{code, message}` error format, `INVALID_REQUEST` for bad IDs, and `USER_NOT_FOUND`/`CITY_NOT_FOUND`. `CatalogApiIT` (17 cases) checks exact JSON, ordering, empty lists, and errors. The frontend does not use these endpoints yet. Phase 3 (domain and repositories) is complete: JPA entities pass Hibernate schema validation, and `RepositoryIT` (5 tests) covers documented list ordering, owner-scoped vehicle lookup, session/payment round trips with fetch-joined details, and a real row lock on the user. A UTC `Clock` bean is configured. Phase 2 (persistence) is complete. Flyway `V1`/`V2` create the [schema](database.md) and demo data; Hibernate runs with `validate`. `MigrationIT` (8 tests, `./mvnw verify` against `parking_test`) covers the empty-database migration, seed data, identity sequences, no reseeding on rerun, and the key constraints. A real application start migrated the Compose database and a restart preserved a changed balance. The integration profile refuses any database whose name does not end in `_test`. Full Compose image builds remain unverified. See [001 — Bootstrap](checkpoints/001-bootstrap.md) for the earlier checkpoint.

## Objective

Deliver a working full-stack prototype with correct parking, ownership, pricing, fictional balance, and payment behavior. The application must persist data in PostgreSQL and start with `docker compose up --build` at `http://localhost:3000`.

## Scope

Included: multiple predefined users and vehicles, demo user selection, fictional EUR balances, valid top-up, multiple cities and zones with different rates, starting and stopping parking, one active session per vehicle, explicit payment, insufficient-balance handling, completed history, Flyway migrations, focused automated tests, Docker Compose, and documentation.

Excluded: authentication, passwords, registration, JWT/OAuth, user/vehicle/city/zone CRUD, administration, real payment providers, refunds, wallet transaction history, Redux, WebSockets, microservices, Kubernetes, cloud deployment, CI/CD, production observability, and elaborate UI.

## Delivery sequence

Tests should accompany business implementation. The final verification phase consolidates evidence and exercises the complete application.

| Phase | Work | Exit condition |
| --- | --- | --- |
| 1. Bootstrap | Create Maven/Java 21 backend, Maven wrapper, React/Vite frontend, root ignore rules, and Compose skeleton; pin compatible dependencies | Both applications build; frontend has committed package lockfile |
| 2. Persistence | Configure PostgreSQL and Flyway; implement [schema and seed data](database.md); set Hibernate to validate | Empty database migrates successfully; subsequent startup preserves data |
| 3. Domain and repositories | Implement entities, enums, DTO mapping support, catalog queries, ownership queries, lock queries | JPA mappings validate against Flyway schema; database constraints are verified |
| 4. User and catalog reads | Implement [demo users](specs/001-demo-user-selection.md) and [cities/zones](specs/006-city-zone-selection.md) | Read endpoints return the specified seed data and errors |
| 5. Wallet | Implement [top-up](specs/002-wallet-top-up.md), money validation, balance limit, user lock | Valid requests update exactly once per request; invalid amounts leave state unchanged |
| 6. Parking | Implement [lifecycle and pricing](specs/003-parking-lifecycle.md), injected clock, rate snapshot, uniqueness safeguard | Pricing boundaries, ownership, start/stop, and simultaneous-request cases pass |
| 7. Payment and history | Implement [payments](specs/004-parking-payments.md) and [history](specs/005-parking-history.md) | Atomicity, insufficient funds, duplicate payment, and completed history are verified |
| 8. API completion | Complete DTOs, validation, consistent errors, and controller coverage against [API contract](api.md) | Every documented endpoint and failure category is exercised |
| 9. Frontend | Implement [single-page UI](frontend.md), forms, selectors, session cards, history, loading/error/success feedback | User can complete the full flow and switch users/cities without stale results |
| 10. Packaging | Complete backend/frontend Dockerfiles, Nginx proxy, database health check, named volume, and readiness behavior | Clean Compose build starts the application with no host Java/Node installation |
| 11. Verification and handoff | Run [tests and smoke test](testing.md), verify persistence, update actual commands and versions | All required checks pass and README accurately describes the delivered application |

## Delivery checklist

- [x] Source code is stored in a Git repository.
- [x] Java 21 / Spring Boot backend builds.
- [x] React frontend builds.
- [x] PostgreSQL schema and demo data are managed by Flyway.
- [ ] At least two demo users and three vehicles are available.
- [ ] User ownership is enforced on applicable operations.
- [ ] Sofia and Plovdiv expose multiple active zones.
- [ ] Fictional balances and top-up work correctly.
- [ ] Start and active parking support multiple vehicles per user.
- [ ] Concurrent starts cannot create duplicate active sessions for a vehicle.
- [ ] Stop records a final amount using the captured rate and exact stored duration.
- [ ] Stop leaves parking unpaid.
- [ ] Payment deducts the balance and creates one payment atomically.
- [ ] Insufficient balance and duplicate payments leave state correct.
- [ ] Completed sessions appear in history with payment information.
- [ ] Critical unit, validation, and PostgreSQL integration tests pass.
- [ ] Frontend handles loading, empty, success, error, and stale-response cases.
- [ ] `docker compose up --build` starts the complete application.
- [ ] Restarting without deleting the volume preserves balances and records.
- [ ] Setup instructions and documentation match the implementation.

## Definition of done

From an empty demo database, a user can select an account, add funds, start a vehicle in a selected city and zone, stop it, see the amount, explicitly pay, and see the updated balance and paid history. A second vehicle can park concurrently. Another user's vehicle cannot be used. Rejected payments do not change balances. Data survives a restart.

Completion requires observed behavior and passing checks, not only implemented classes or unchecked documentation examples.

## Priorities if time is limited

Preserve correctness of lifecycle, ownership, uniqueness, pricing, persistence, and atomic payment first. Then complete top-up, catalogs, startup, critical tests, and usable documentation. Simplify visual polish and optional abstractions before reducing these behaviors. Any unimplemented requirement must remain explicitly unchecked.

See the [documentation index](README.md) for current contracts and the [original plan](reference/original-plan.md) for historical context.
