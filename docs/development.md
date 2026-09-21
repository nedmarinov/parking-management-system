# Development and Operations Guide

Status: Bootstrap setup is present. Local backend/frontend builds and API connectivity have been verified. Compose configuration validates, but container builds/startup remain unverified because the Docker engine returned an API error. Business endpoints, migrations, and database integration remain pending. See [checkpoint 001](checkpoints/001-bootstrap.md) for the exact current scope.

## Prerequisites

For the packaged application, install Docker with Docker Compose and start the Docker engine. The application must build and run without host Java, Maven, or Node installations.

For local application development, use Java 21 and the included Maven wrapper, which selects Maven 3.9.16. The frontend `.nvmrc` selects Node 24.21.0; the project also supports Node 22.12+ and 24+. Use `npm ci` with the included `package-lock.json`.

## Current bootstrap

The backend currently runs without PostgreSQL. From `backend/`, run `./mvnw spring-boot:run`. From `frontend/`, run `npm ci` and `npm run dev` in another terminal. Open `http://127.0.0.1:5173` and confirm **Parking service connected**. `GET /api/health` returns `{"status":"UP"}` through the Vite proxy or directly on backend port 8080.

The Dockerfiles, Nginx configuration, database container, and volume are present. The application does not yet read database settings or apply migrations. The database-backed behavior and integration-test commands below describe later phases.

## Target Compose services

| Service | Responsibility | Host access |
| --- | --- | --- |
| `frontend` | Nginx serving the React production build and proxying `/api` | `http://localhost:3000` |
| `backend` | Spring Boot listening on container port 8080 | Reached through frontend in the full stack |
| `postgres` | PostgreSQL with a named data volume | Publish `127.0.0.1:5432:5432` for local development and integration tests |

PostgreSQL has a `pg_isready` health check. Backend depends on a healthy database, and frontend depends on backend health. The current backend check uses `/api/health`; switch to database-backed `/api/cities` when that endpoint exists. Flyway startup migrations will be added in Phase 2. The backend runtime image installs curl for its health check.

The frontend image builds with Node and serves only generated assets through Nginx at runtime. The backend image builds with Maven/Java 21 and runs the packaged application on Java 21. Configure Nginx so `/api/...` reaches `http://backend:8080/api/...` unchanged, while ordinary frontend paths serve the application.

## Planned configuration contract

The root `.env.example` documents defaults, and local `.env` files are ignored by Git. Compose supplies these defaults without manual configuration:

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

From the repository root with a working Docker engine:

```bash
docker compose up --build
```

Open [http://localhost:3000](http://localhost:3000). The bootstrap serves the coming-soon page. Check its API connection with:

```bash
curl -i http://localhost:3000/api/health
```

Expected: `200` with `{"status":"UP"}`. After the persistence and catalog phases, first startup will also create the schema and demo fixtures, and `/api/users` will return Alex and Maria. Persistence of changed balances and records will be verified when those features exist.

For background operation:

```bash
docker compose up --build -d
docker compose ps
docker compose logs --tail=100 backend
```

## Local backend and frontend development

For the current bootstrap, skip database startup. Once database integration is added, start only PostgreSQL from the root:

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

The current `verify` command packages the bootstrap application; no application test cases exist yet. As the features are implemented, use Surefire for `*Test` unit/validation tests and configure Failsafe for `*IT` PostgreSQL integration tests. At that point `verify` will require the dedicated test database below. A successful build alone is not evidence that business tests passed.

From `frontend/`:

```bash
npm ci
npm run build
```

No lint or frontend test runner has been added. Add and document those commands when the corresponding tool is introduced. Temporary browser checks used during bootstrap are recorded in its checkpoint.

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

Pinned versions and observed bootstrap results are recorded in [checkpoint 001](checkpoints/001-bootstrap.md). Update this guide as persistence, business features, and container verification are completed.
