# 001 — Bootstrap Checkpoint

Status: Ready for review. Implementation changes are uncommitted. Database and business-feature work have not started.

## Delivered scope

- Java 21 / Spring Boot application with a Maven wrapper and a packaged executable JAR.
- A small `GET /api/health` endpoint returning `{"status":"UP"}`.
- React/Vite frontend with JavaScript/JSX, Tailwind, shadcn configuration, and a responsive coming-soon page.
- A real frontend API connection check, loading/error feedback, and a retry action.
- Exact direct dependency versions, a generated npm lockfile, `.nvmrc`, and root ignore rules.
- Backend/frontend Dockerfiles, an Nginx `/api` proxy, and a three-service Compose configuration.
- A PostgreSQL container definition with a health check and persistent volume. The application does not yet use the database.

This increment establishes executable foundations. It does not implement demo-user selection, parking, wallet, payment, or history behavior.

## Version decisions

| Component | Selected version |
| --- | --- |
| Java language/runtime baseline | 21 |
| Spring Boot | 3.5.16 |
| Maven wrapper / Maven distribution | 3.3.4 / 3.9.16 |
| Node container and `.nvmrc` | 24.21.0 |
| React / React DOM | 19.3.0 |
| Vite / React plugin | 8.3.0 / 6.1.1 |
| Tailwind / Tailwind Vite plugin | 4.3.3 |
| shadcn CLI and styles | 4.21.0, Radix Nova preset |
| Java build/runtime images | `eclipse-temurin:21.0.12_8-jdk-jammy` / `21.0.12_8-jre-jammy` |
| Node image | `node:24.21.0-alpine3.24` |
| Nginx image | `nginx:1.30.5-alpine3.24` |
| PostgreSQL image | `postgres:17.11-alpine3.24` |

Spring Boot 3.5 preserves the plan's JUnit 5 baseline; its managed dependency set resolved JUnit 5.12.2 during the build. See [Spring Boot's managed dependencies](https://docs.spring.io/spring-boot/3.5/appendix/dependency-versions/coordinates.html). The Maven wrapper came from the official Spring Initializr generator; the application POM selects the version listed above.

The project accepts Node 22.12+ and 24+ for local use, with the exact Node 24 version selected for the container and `.nvmrc`. Vite's [runtime requirements](https://vite.dev/guide/) and the [official shadcn Vite setup](https://ui.shadcn.com/docs/installation/vite) informed the frontend foundation. Dependency manifests and the lockfile are the authoritative version records.

## Verification performed

| Check | Result |
| --- | --- |
| Maven `verify` with Java 21 | Passed; executable application JAR produced |
| Clean `npm ci` from the lockfile | Passed; subsequent production build also passed |
| Frontend production build | Passed |
| Direct backend `GET /api/health` | Passed; HTTP 200, `{"status":"UP"}` |
| Vite development proxy `/api/health` | Passed; HTTP 200 with the backend response |
| `docker compose config --quiet` | Passed |
| Production-page browser check | Passed; page renders and connects to the backend |
| Browser at 390px width | Passed; no horizontal overflow; screenshot reviewed |
| Simulated API 503 and keyboard retry | Passed; visible failure followed by successful recovery |
| Uncaught browser JavaScript errors | None observed during the checks |
| Container image builds and Compose startup | Unverified; local Docker engine returned HTTP 500 on its API |

Browser checks used temporary Playwright 1.58.2 tooling outside the repository. No application unit/integration test suite has been added yet. The Maven result is build evidence only. Business tests will accompany the corresponding implementation.

The Docker failure occurred with both the default client API and an older API version. Compose syntax has been checked, but image builds, Nginx container behavior, PostgreSQL startup, and container health ordering still need execution with a working Docker engine.

## Validate locally

Use two terminals. In `backend/`:

```bash
./mvnw spring-boot:run
```

In `frontend/`:

```bash
npm ci
npm run dev
```

Open [http://127.0.0.1:5173](http://127.0.0.1:5173). Check that:

1. The Parking Management coming-soon page renders.
2. The status becomes **Parking service connected**.
3. The layout remains readable at a narrow viewport.
4. If you stop the backend and reload the page, it shows a connection failure and a Try again button.
5. After restarting the backend, Try again restores the connected status.

You can also inspect the API directly:

```bash
curl http://127.0.0.1:8080/api/health
curl http://127.0.0.1:5173/api/health
```

Both should return `{"status":"UP"}`. Stop the development processes with Ctrl+C in their terminals when finished. If the review servers are already running, use their existing URLs instead of starting another instance on the same ports.

With a working Docker engine, the additional packaging check is:

```bash
docker compose up --build
```

Then open `http://localhost:3000` and check `/api/health` through that port. This verifies the Compose bootstrap only; database migrations and demo rows are not part of this checkpoint.

## Next increment

After this checkpoint is validated, record it in Git and begin Phase 2: add PostgreSQL/Flyway dependencies, configure persistence, and implement schema and demo-data migrations. All six business feature specs remain Ready, not Implemented.
