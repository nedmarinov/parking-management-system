# Development and Operations Guide

Status: Ready — this is the setup contract for the future implementation. The repository currently contains documentation only; the commands that depend on application files cannot run yet.

## Prerequisites

For the packaged application, install Docker with Docker Compose and start the Docker engine. The application must build and run without host Java, Maven, or Node installations.

For local application development, use Java 21, the committed Maven wrapper, and a Node.js version supported by the pinned Vite version. Select and document the exact Node version during bootstrap. Commit `package-lock.json` and use `npm ci` once that lockfile exists.

## Target Compose services

| Service | Responsibility | Host access |
| --- | --- | --- |
| `frontend` | Nginx serving the React production build and proxying `/api` | `http://localhost:3000` |
| `backend` | Spring Boot listening on container port 8080 | Reached through frontend in the full stack |
| `postgres` | PostgreSQL with a named data volume | Publish `127.0.0.1:5432:5432` for local development and integration tests |

Give PostgreSQL a `pg_isready` health check. Backend depends on a healthy database and applies Flyway migrations at startup. Give backend an HTTP health check against `/api/cities` and make frontend depend on backend health; this exercises an available application and database without introducing a separate observability subsystem. Ensure the chosen backend image contains the utility used by its health check.

The frontend image builds with Node and serves only generated assets through Nginx at runtime. The backend image builds with Maven/Java 21 and runs the packaged application on Java 21. Configure Nginx so `/api/...` reaches `http://backend:8080/api/...` unchanged, while ordinary frontend paths serve the application.

## Planned configuration contract

Create a root `.env.example` during bootstrap and ignore local `.env` files in Git. Compose should supply these defaults so a fresh clone starts without manual configuration:

| Variable | Demo default | Used by |
| --- | --- | --- |
| `POSTGRES_DB` | `parking` | PostgreSQL and Compose datasource construction |
| `POSTGRES_USER` | `parking` | PostgreSQL and backend username |
| `POSTGRES_PASSWORD` | `parking` | PostgreSQL and backend password |

Set these Spring configuration values in the backend Compose service:

```text
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/parking
SPRING_DATASOURCE_USERNAME=parking
SPRING_DATASOURCE_PASSWORD=parking
SPRING_JPA_HIBERNATE_DDL_AUTO=validate
SPRING_JPA_OPEN_IN_VIEW=false
```

When the PostgreSQL variables are overridden, construct the URL and credentials from those values rather than maintaining independent defaults. The displayed credentials are intentional local demo defaults. Real payment or authentication credentials are not required.

Store default local datasource settings and ordinary application configuration in `backend/src/main/resources/application.yml`, with environment overrides. Inject a UTC clock through configuration. Money/time serialization must follow [the API contract](api.md#conventions).

## Start the complete application

From the repository root, once implementation exists:

```bash
docker compose up --build
```

Open [http://localhost:3000](http://localhost:3000). First startup creates the schema and demo fixtures. Check API readiness with:

```bash
curl -i http://localhost:3000/api/users
```

Expected: `200` with Alex and Maria and string-formatted balances. A subsequent startup must preserve changed balances and existing parking/payment records.

For background operation:

```bash
docker compose up --build -d
docker compose ps
docker compose logs --tail=100 backend
```

## Local backend and frontend development

Start only PostgreSQL from the root:

```bash
docker compose up -d postgres
```

In a terminal with working directory `backend/`, use localhost datasource defaults and run:

```bash
./mvnw spring-boot:run
```

The local backend listens at `http://localhost:8080`. In another terminal with working directory `frontend/`:

```bash
npm ci
npm run dev
```

Configure Vite's development server on port 5173 with `/api` proxied to `http://localhost:8080`, keeping the prefix intact. Open [http://localhost:5173](http://localhost:5173). Relative frontend fetch calls work in both development and Compose without separate API URLs or CORS configuration.

Do not simultaneously run the full Compose backend and the local backend against the same database during migration or fixture work. Use one application instance for the ordinary local workflow.

## Build and verification commands

From `backend/`:

```bash
./mvnw test
./mvnw verify
```

Configure Surefire for `*Test` unit/validation tests and Failsafe for `*IT` PostgreSQL integration tests. `verify` includes both and requires the dedicated test database described below. Normal application image packaging and full integration verification are separate steps; a successful Docker build alone is not evidence that all business tests passed.

From `frontend/`:

```bash
npm ci
npm run build
```

Add and document any lint or automated UI-test command when the corresponding tool is actually introduced. No frontend test runner is mandated by the initial specification.

## Dedicated integration database

Use `parking_test`, never the development `parking` database, for tests that clean up or replace fixtures. With the default Compose credentials, create it once from the repository root:

```bash
docker compose exec postgres psql -U parking -d postgres -c 'CREATE DATABASE parking_test OWNER parking;'
```

An already-existing database does not need to be recreated. The test profile must read these variables and fail clearly if the configured database is not an explicitly designated test database:

```text
TEST_DATABASE_URL=jdbc:postgresql://localhost:5432/parking_test
TEST_DATABASE_USERNAME=parking
TEST_DATABASE_PASSWORD=parking
```

From `backend/`, the planned full verification command is:

```bash
TEST_DATABASE_URL=jdbc:postgresql://localhost:5432/parking_test \
TEST_DATABASE_USERNAME=parking \
TEST_DATABASE_PASSWORD=parking \
./mvnw verify
```

Implement a test profile under `src/test/resources/` and activate it in integration tests. Apply the same Flyway migrations, then arrange isolated test fixtures and cleanup. Do not fall back to a development datasource when test configuration is missing. Testcontainers is optional if the team later prefers it; it is not required to run the first test setup.

See [the testing strategy](testing.md) for actual scenarios and transaction-boundary requirements.

## API walkthrough

List users, owned vehicles, cities, and zones through the frontend proxy:

```bash
curl http://localhost:3000/api/users
curl http://localhost:3000/api/users/1/vehicles
curl http://localhost:3000/api/cities
curl http://localhost:3000/api/cities/1/zones
```

Start a session:

```bash
curl -i -X POST http://localhost:3000/api/parkings \
  -H 'Content-Type: application/json' \
  -d '{"userId":1,"vehicleId":1,"zoneId":1}'
```

Read the returned session `id`. The following example assumes it is `1`; replace it with the actual ID if needed:

```bash
curl -i -X POST http://localhost:3000/api/parkings/1/stop \
  -H 'Content-Type: application/json' \
  -d '{"userId":1}'

curl -i -X POST http://localhost:3000/api/parkings/1/payment \
  -H 'Content-Type: application/json' \
  -d '{"userId":1}'

curl http://localhost:3000/api/users/1/parkings/history
```

Top up separately:

```bash
curl -i -X POST http://localhost:3000/api/users/1/top-up \
  -H 'Content-Type: application/json' \
  -d '{"amount":"10.00"}'
```

## Stop, restart, and reset

Stop containers and remove their Compose network while retaining the data volume:

```bash
docker compose down
```

Starting again with `docker compose up --build` must preserve balances, sessions, and payments.

For a deliberate clean demo reset, the following command **deletes the Compose data volume and all stored demo activity**:

```bash
docker compose down -v
docker compose up --build
```

Use this reset only when intending to return to seed data. PostgreSQL environment changes do not replace already-created database contents; use a migration or an intentional reset as appropriate for the local demo.

## Troubleshooting

| Symptom | Check |
| --- | --- |
| Frontend cannot connect to API | Backend health and logs; Nginx `/api` proxy preserves the path prefix |
| Backend cannot connect to PostgreSQL | Database health, credentials, and `postgres` hostname in Compose versus `localhost` on the host |
| Hibernate validation fails | JPA types/names agree with Flyway migrations; do not switch to schema auto-update to hide a mismatch |
| Flyway reports a changed migration | Restore the applied migration and write a new versioned migration for the change |
| Port already in use | Check host processes using ports 3000, 5173, 8080, or 5432 for the chosen workflow |
| Database integration tests cannot start | Dedicated database exists and all `TEST_DATABASE_*` variables are supplied |
| Data disappears after restart | Verify named volume configuration and that the volume was not removed |
| Seed balances do not reappear | This is expected on an existing volume; balances are persistent |

Update this guide with exact versions and observed commands when bootstrap and packaging are implemented.
