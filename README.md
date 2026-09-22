# Parking Management System

A full-stack prototype for managing paid parking across multiple cities. Demo users select a vehicle and parking zone, start and stop parking, and pay using a fictional account balance.

**Project status:** Working prototype. All features are implemented and tested, and `docker compose up --build` starts the full stack. The frontend has not yet had a manual browser review. See the [implementation plan](docs/PLAN.md) for per-phase verification.

## User flow

1. Select a predefined demo user and view their balance and vehicles.
2. Add fictional funds to the account.
3. Select a vehicle, city, and active parking zone.
4. Start parking and view active sessions.
5. Stop parking and see the calculated charge.
6. Explicitly pay from the account balance.
7. Review completed parking and payment status in history.

Each vehicle can have one active session. A user can park multiple vehicles at once. Every started hour is billed, with a one-hour minimum, at the rate captured when parking started. Stopping parking does not charge the account.

## Technology

| Area | Technology |
| --- | --- |
| Backend | Java 21, Spring Boot 3.5.16 (Web, Validation, Data JPA), Maven 3.9.16 via wrapper |
| Database | PostgreSQL 17, Flyway migrations, Hibernate schema validation |
| Frontend | React 19.3.0, Vite 8.3.0, JavaScript/JSX, native `fetch`, Tailwind CSS 4, shadcn/ui |
| Tests | JUnit 5, Mockito, Spring MockMvc, PostgreSQL integration tests; Vitest 5.0.1 |
| Runtime | Docker Compose, Nginx 1.30 |

Direct frontend dependencies and container image versions are pinned. The Maven wrapper pins Maven; the Spring Boot parent manages backend dependency versions. The frontend's `.nvmrc` selects Node 24.21.0; Node 22.12+ and 24+ are supported.

## Repository layout

```text
parking-management-system/
├── README.md
├── PLAN.MD                      # Navigation to the maintained plan
├── docs/                        # Specifications and development guidance
├── backend/                     # Spring Boot API, Flyway migrations, tests
├── frontend/                    # React application, Vitest tests, Nginx configuration
└── docker-compose.yml           # PostgreSQL, backend, and frontend
```

## Running the application

With Docker, from the repository root:

```bash
docker compose up --build
```

Open [http://localhost:3000](http://localhost:3000). Nginx serves the React app and forwards `/api` to the backend. The first start creates the schema and demo data: Alex Johnson (20.00 EUR, two vehicles) and Maria Smith (10.00 EUR, one vehicle), with zones in Sofia and Plovdiv. Data persists in a named volume across `docker compose down` and `up`.

If host port 5432 is already in use, set `POSTGRES_PORT` (for example `POSTGRES_PORT=5433 docker compose up --build`). For local development without the packaged images, see the [development guide](docs/development.md).

## Testing

Backend, from `backend/`. Integration tests need a database whose name ends in `_test`; create it once with `docker compose exec postgres psql -U parking -d postgres -c 'CREATE DATABASE parking_test OWNER parking;'`.

```bash
./mvnw test      # unit and web-slice tests, no database needed
TEST_DATABASE_URL=jdbc:postgresql://localhost:5432/parking_test \
TEST_DATABASE_USERNAME=parking TEST_DATABASE_PASSWORD=parking \
./mvnw verify    # adds the PostgreSQL integration tests
```

The integration tests cover the schema and constraints, every endpoint and error code, the pricing rules, and real concurrent requests: duplicate starts, stops, and payments, plus top-ups racing payments. They also force a failure mid-payment to prove it rolls back completely.

Frontend, from `frontend/`:

```bash
npm ci
npm test         # Vitest unit tests
npm run build
```

See the [testing strategy](docs/testing.md) for the required cases and the Compose smoke test.

## Scope and assumptions

- Authentication is outside the prototype scope. A user selector demonstrates ownership and user-specific behavior; selecting a user is not an authenticated identity check.
- Users, vehicles, cities, and zones are predefined demo data. There are no management or registration screens.
- Money is fictional, in EUR. Top-up simulates adding funds without an external payment provider.
- Wallet and top-up functionality extend the original assignment to make the payment flow meaningful.
- Hourly pricing uses the zone rate captured at session start. Pricing stays isolated so future rules can be added without changing the parking lifecycle.
- Sofia and Plovdiv are the initial cities. Their prices are illustrative seed data, not statements of current municipal tariffs.
- Correctness, persistence, focused tests, and simple startup take priority over UI polish.

## Documentation

Start with the [documentation index](docs/README.md). The [methodology](docs/methodology.md) explains our specification-driven, AI-assisted workflow, and the [implementation plan](docs/PLAN.md) defines the delivery sequence. The [feature specifications](docs/README.md#feature-specifications) define behavior and acceptance criteria. The [API contract](docs/api.md), [database design](docs/database.md), and [architecture](docs/architecture.md) define shared implementation contracts.
