# Parking Management System

A full-stack prototype for managing paid parking across multiple cities. Demo users select a vehicle and parking zone, start and stop parking, and pay using a fictional account balance.

**Project status:** Phase 1 bootstrap is complete. The backend, frontend shell, Maven wrapper, pinned dependencies, and Docker Compose files are present. Parking features, database integration, and migrations are still pending. See the [bootstrap checkpoint](docs/checkpoints/001-bootstrap.md) for verification and review instructions.

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
| Backend | Java 21, Spring Boot 3.5.16, Spring Web, Jakarta Bean Validation, Maven 3.9.16; Spring Data JPA planned |
| Database | PostgreSQL, Flyway |
| Frontend | React 19.3.0, Vite 8.3.0, JavaScript/JSX, native `fetch`, shadcn/ui, Tailwind CSS |
| Tests | JUnit 5, Mockito, targeted PostgreSQL integration tests |
| Runtime | Docker Compose, Nginx |

Direct frontend dependencies and container image versions are pinned. The Maven wrapper pins Maven; the Spring Boot parent manages backend dependency versions. The frontend's `.nvmrc` selects Node 24.21.0; Node 22.12+ and 24+ are supported by the project configuration.

## Repository layout

```text
parking-management-system/
├── README.md
├── PLAN.MD                      # Navigation to the maintained plan
├── docs/                        # Specifications and development guidance
│   ├── PLAN.md
│   ├── methodology.md
│   ├── architecture.md
│   ├── api.md
│   ├── database.md
│   ├── frontend.md
│   ├── development.md
│   ├── testing.md
│   ├── checkpoints/
│   ├── specs/
│   └── reference/original-plan.md
├── backend/                     # Spring Boot application and Maven wrapper
├── frontend/                    # React application and Nginx configuration
└── docker-compose.yml           # Bootstrap application stack
```

## Running the application

The current checkpoint can run locally in two terminals. From `backend/`:

```bash
./mvnw spring-boot:run
```

From `frontend/`:

```bash
npm ci
npm run dev
```

Open [http://127.0.0.1:5173](http://127.0.0.1:5173). The page should show **Parking service connected**. PostgreSQL is not needed by this bootstrap backend.

The Compose files also provide the intended packaged startup:

```bash
docker compose up --build
```

Once Docker starts the stack, open [http://localhost:3000](http://localhost:3000). Nginx serves React and forwards `/api` to the backend. Compose configuration validation passed; container builds and startup remain unverified because the local Docker engine returned an API error. PostgreSQL has a named volume but is not yet connected to the application. Flyway and demo data belong to Phase 2.

Startup, local development, configuration, shutdown, and database reset instructions are defined in the [development guide](docs/development.md).

## Testing

Backend commands, run from `backend/`:

```bash
./mvnw test
./mvnw verify
```

The bootstrap has been packaged successfully with `verify`. No application test cases exist yet; this establishes a successful build, not verified parking behavior. The planned unit, validation, and PostgreSQL integration suites will be added with their features. See the [testing strategy](docs/testing.md) for required cases and the final smoke test.

From `frontend/`, `npm ci` installs locked dependencies and `npm run build` produces the application assets.

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
