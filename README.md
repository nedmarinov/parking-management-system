# Parking Management System

A full-stack prototype for managing paid parking across multiple cities. Demo users select a vehicle and parking zone, start and stop parking, and pay using a fictional account balance.

**Project status:** specification complete; application implementation has not started. The commands and application behavior below describe the implementation target. Backend, frontend, migrations, and Docker configuration do not exist yet.

## Intended user flow

1. Select a predefined demo user and view their balance and vehicles.
2. Add fictional funds to the account.
3. Select a vehicle, city, and active parking zone.
4. Start parking and view active sessions.
5. Stop parking and see the calculated charge.
6. Explicitly pay from the account balance.
7. Review completed parking and payment status in history.

Each vehicle can have one active session. A user can park multiple vehicles at once. Every started hour is billed, with a one-hour minimum. Stopping parking does not charge the account.

## Technology

| Area | Planned technology |
| --- | --- |
| Backend | Java 21, Spring Boot, Spring Web, Spring Data JPA, Jakarta Bean Validation, Maven |
| Database | PostgreSQL, Flyway |
| Frontend | React, Vite, JavaScript/JSX, native `fetch`, shadcn/ui |
| Tests | JUnit 5, Mockito, targeted PostgreSQL integration tests |
| Runtime | Docker Compose, Nginx |

Dependency and image versions will be pinned during bootstrap after Java 21 compatibility is verified.

## Repository layout

```text
parking-management-system/
├── README.md
├── PLAN.MD                      # Navigation to the maintained plan
├── docs/                        # Specifications and development guidance
│   ├── PLAN.md
│   ├── architecture.md
│   ├── api.md
│   ├── database.md
│   ├── frontend.md
│   ├── development.md
│   ├── testing.md
│   ├── specs/
│   └── reference/original-plan.md
├── backend/                     # Planned Spring Boot application
├── frontend/                    # Planned React application and Nginx
└── docker-compose.yml           # Planned application stack
```

## Running the application

After implementation, an installed and running Docker engine with Docker Compose will be sufficient:

```bash
docker compose up --build
```

Open [http://localhost:3000](http://localhost:3000). Nginx will serve React and forward `/api` to the backend. PostgreSQL data will live in a named volume. Demo data will be installed by Flyway on first startup.

Startup, local development, configuration, shutdown, and database reset instructions are defined in the [development guide](docs/development.md).

## Testing

The planned backend commands, run from `backend/`, are:

```bash
./mvnw test
./mvnw verify
```

`test` will run unit and validation tests. `verify` will also run integration tests against a dedicated PostgreSQL database. The wrapper and build configuration will be added during implementation. See the [testing strategy](docs/testing.md) for required cases and the final smoke test.

## Scope and assumptions

- Authentication is outside the prototype scope. A user selector demonstrates ownership and user-specific behavior; selecting a user is not an authenticated identity check.
- Users, vehicles, cities, and zones are predefined demo data. There are no management or registration screens.
- Money is fictional, in EUR. Top-up simulates adding funds without an external payment provider.
- Wallet and top-up functionality extend the original assignment to make the payment flow meaningful.
- Hourly pricing uses the zone rate captured at session start. Pricing stays isolated so future rules can be added without changing the parking lifecycle.
- Sofia and Plovdiv are the initial cities. Their prices are illustrative seed data, not statements of current municipal tariffs.
- Correctness, persistence, focused tests, and simple startup take priority over UI polish.

## Documentation

Start with the [documentation index](docs/README.md), then the [implementation plan](docs/PLAN.md). The [feature specifications](docs/README.md#feature-specifications) define behavior and acceptance criteria. The [API contract](docs/api.md), [database design](docs/database.md), and [architecture](docs/architecture.md) define shared implementation contracts.
