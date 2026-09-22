# Testing and Acceptance

Status: Implemented. The backend has unit, web-slice, and PostgreSQL integration suites covering the required cases below; the frontend has Vitest unit tests. The Compose smoke test has passed via curl. Manual browser checks in the frontend section are pending.

## Approach

Prioritize business behavior over coverage percentages. Use small unit tests for pricing and service decisions, HTTP tests for the public contract, and targeted PostgreSQL integration tests for guarantees that mocks cannot establish.

Use JUnit 5 and Mockito for backend unit tests. Use the actual PostgreSQL schema for persistence, locking, uniqueness, and rollback checks. An in-memory substitute is not sufficient evidence for PostgreSQL partial indexes or concurrency behavior.

## Test organization and commands

```text
backend/src/test/
├── java/.../parking/
│   ├── service/          # *Test: pricing and service behavior
│   ├── controller/       # *Test: HTTP binding, validation, error mapping
│   └── integration/      # *IT: PostgreSQL and full backend flows
└── resources/
    └── application-it.yml
```

From `backend/`, the target commands are `./mvnw test` for unit/HTTP slice tests and `./mvnw verify` for the full suite including integration tests. Configure Maven Surefire/Failsafe accordingly. Integration tests require the isolated PostgreSQL setup and environment variables in [the development guide](development.md#dedicated-integration-database).

Use deterministic fixtures rather than depending on tests running in a particular order. Inject a fixed or adjustable application clock; tests must not wait for real parking hours to pass. Do not point fixture cleanup at the ordinary development database.

## Pricing tests

Parameterize the [pricing table](specs/003-parking-lifecycle.md#pricing), including zero duration, one millisecond, one minute, 59 minutes, exactly one hour, one hour plus one millisecond, one hour plus one second, 61 minutes, exactly two hours, and 121 minutes.

Also verify different hourly rates, a zero rate, a negative duration, captured-rate use after a catalog change, the monetary storage boundary, and UTC elapsed time across date/daylight-saving boundaries. Assert decimal values exactly, avoiding floating-point expected values.

## Business and API coverage

| Area | Required checks |
| --- | --- |
| User context | Existing/missing users, owned-only vehicle and session lists, another user's vehicle/session rejected |
| Catalog | Cities and zone ordering, active-only city filtering, same zone names across cities, missing city, empty active-zone list |
| Top-up | Valid decimals; missing/null/numeric/malformed/zero/negative inputs; excessive digits; exact capacity and overflow; unchanged state after rejection |
| Start | Valid session; missing resources; ownership; inactive zone; one active session per vehicle; different vehicles concurrently active; zero-balance and unpaid-history starts allowed |
| Active list | Multiple vehicles, owned-only results, active-only results, deterministic ordering |
| Stop | End time/amount/status stored; rate snapshot; no balance deduction; no payment; duplicate/ownership/clock/amount-limit failures |
| Payment | Correct deduction; exact-balance success; insufficient balance; active/already-paid/ownership rejection; zero amount; timestamp validation |
| History | Completed-only owned records; paid and unpaid mapping; null paid time when unpaid; stored amount preserved; stable ordering |
| HTTP contract | Routes, required bodies, positive IDs, money strings, UTC timestamps, null fields, success statuses, stable error code/message shape |

Use real HTTP request binding when testing the top-up decimal format. Calling a DTO constructor directly does not verify JSON type handling or validation configuration. Include malformed JSON and invalid path variables in controller/error tests.

## Required PostgreSQL integration tests

### Migrations and constraints

Apply Flyway to an empty test database and start Hibernate with schema validation. Confirm demo rows and identity-sequence advancement. Verify an ordinary restart does not reseed balances.

Check database rejection of negative balances/rates/payment amounts, inconsistent active/completed fields, duplicate vehicle plates, mismatched session vehicle ownership, duplicate active vehicle sessions, and duplicate payments for one session. Check that two completed sessions for one vehicle are allowed.

### Atomic payment

Call the real transactional service through Spring. Arrange a completed unpaid session and sufficient balance. Force a controlled runtime or persistence failure after a balance update has been flushed to PostgreSQL but before payment commits. This can use a test-only failure seam or repository spy; it must leave the real transaction manager and real database active.

After the service fails, read in a fresh transaction/persistence context and assert that the original balance remains and no payment exists. Do not wrap the entire test invocation in an outer transaction whose eventual rollback could hide a defective service boundary. Also verify the successful operation commits both changes and that insufficient funds commit neither.

### Concurrent requests

Use separate transactions/connections and coordinated worker starts with bounded timeouts. Do not put a synchronization barrier after one worker has acquired a user lock that the other worker needs to reach that barrier.

| Concurrent actions | Required result |
| --- | --- |
| Two starts for the same vehicle | One active session and one conflict |
| Starts for two owned vehicles | Both sessions active |
| Two stops for one active session | One completion, one conflict, immutable first completion values |
| Two payments for one session | One payment and one deduction |
| Payments for two sessions with funds for only one | One payment; nonnegative correct remaining balance; other session unpaid |
| Two top-ups of 5.00 and 7.00 | Original balance plus 12.00 |
| Top-up and payment | No lost update; payment outcome consistent with the serialized order |

Repeat a concurrency test only when investigating a failure or a relevant implementation change. Assertions should check committed rows and balances, not only returned HTTP statuses.

### Complete backend flow

Exercise start → active list → stop → unpaid history → pay → paid history through the API with a controlled clock and isolated fixtures. Assert stored snapshots, amount, payment, and balance after each relevant step. Include a rejected payment, top-up, and successful retry in a second fixture scenario.

## Frontend verification

Run `npm ci` and `npm run build` from `frontend/`. Perform focused browser checks:

- Required form selections, decimal input errors, and disabled pending actions.
- Initial loading and distinct empty/error states in each section.
- Switching users while old user requests are delayed.
- Switching cities while old zone requests are delayed.
- Correct refresh after top-up, start, stop, and payment.
- A successful mutation followed by a refresh failure is reported accurately.
- A mutation timeout does not trigger automatic resubmission.
- Another tab's duplicate stop/payment/start is handled by showing the conflict and refreshing.
- Keyboard operation, accessible field labels, visible focus, status text, and a usable narrow layout.

Use throttled network responses or a request-interception test harness to make races observable. Add automated frontend tests if the resulting behavior needs continued protection, but a large test framework is not required solely for visual layout.

## Final Compose smoke test

Run against a disposable demo environment. If starting from an existing volume, the clean-reset command in [development](development.md#stop-restart-and-reset) deletes its stored activity; retain data when testing persistence instead.

1. Build and start the stack from an empty database with `docker compose up --build`.
2. Open `http://localhost:3000` and confirm the API is reachable through Nginx.
3. Verify Alex/Maria, their different balances, and their owned vehicles. Switch between them.
4. Verify Sofia/Plovdiv and their rates; verify the inactive Red Zone is absent. Submit its ID directly to confirm rejection.
5. Select Alex, add `10.00`, and confirm balance becomes `30.00`. Reject zero, negative, and over-precise input without another change.
6. Start Alex's first vehicle in Sofia Blue Zone. A duplicate start must conflict. Start Alex's second vehicle and verify both appear active.
7. Stop the first vehicle promptly. Verify it leaves the active list, has a final `2.00` amount for a duration under one hour, and appears unpaid. Balance remains `30.00`.
8. Pay that session. Verify balance becomes `28.00`, history says paid, and the unpaid item disappears. A second payment must conflict without further deduction.
9. With a session belonging to Alex, attempt stop/pay as Maria through the API and verify ownership rejection.
10. Exercise insufficient funds using a dedicated fixture. For a fresh disposable smoke database only, set Maria's balance to zero with the command below, then refresh her UI context. Start and promptly stop her vehicle in Plovdiv Blue Zone; payment of `1.50` must fail without creating a payment or changing the zero balance.
11. Top up Maria by `5.00`, explicitly pay again, and verify a `3.50` balance and paid history.
12. Confirm completed-only history, consistent dates, final amounts, and payment times. Confirm Alex's other active vehicle remains active when returning to his account.
13. Record current balances, active sessions, and history. Run `docker compose down`, then `docker compose up --build` without deleting the volume. Confirm those values persist.
14. Stop the remaining active session if desired. Record the exact test commands, outcomes, and any remaining limitation in the implementation handoff.

The optional fixture setup for step 10, using default demo credentials, is:

```bash
docker compose exec postgres psql -U parking -d parking \
  -c 'UPDATE users SET balance = 0.00 WHERE id = 2;'
```

This direct update prepares a test condition; it is not an application endpoint or a production workflow. Unit/integration tests should arrange their own isolated fixtures instead.

## Completion evidence

Before marking a feature Implemented or checking off the plan, record which relevant automated tests passed and which manual scenarios were exercised. A frontend build proves compilation, not ownership, billing, or transactional correctness. A passing mocked service test does not establish database atomicity. Keep failed or unperformed checks visible.
