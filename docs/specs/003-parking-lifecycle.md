# 003 — Parking Lifecycle and Pricing

Status: Ready — not implemented.

## Purpose

Let a user park an owned vehicle in an active zone, see active sessions, and stop parking to obtain a final charge.

## Scope

Includes start, active-session retrieval, stop, captured hourly pricing, and duplicate-session prevention. Payment is a separate feature. Reservations, cancellation, parking schedules, free periods, maximum-stay enforcement, and live price estimates are excluded.

Depends on [demo users](001-demo-user-selection.md) and [city/zone selection](006-city-zone-selection.md).

## Business rules

### Starting

1. The selected user, vehicle, and zone must exist.
2. The vehicle must belong to the selected user and the zone must be active.
3. One vehicle can have at most one active session across all zones and cities.
4. Different owned vehicles can be active simultaneously. There is no per-user active-session limit.
5. A zero balance or a completed unpaid session does not prevent starting new parking.
6. The backend stores the user, vehicle, zone, captured hourly rate, and server start instant. The initial status is `ACTIVE`; end time and amount are null; no payment exists.
7. Use the [shared locking policy](../architecture.md#transactions-and-concurrent-requests) and required active-vehicle unique index, including for simultaneous requests.

### Stopping

1. The selected user and session must exist; the session must belong to that user.
2. Only an active session can stop. A repeated stop returns `PARKING_ALREADY_COMPLETED`.
3. Read the server clock after locking and validation, then calculate using the stored start instant and captured hourly rate.
4. Store end time, final amount, and `COMPLETED` in one transaction.
5. Do not deduct balance or create a payment. The completed session is now `UNPAID` in the API.
6. A later stop attempt must not alter the already recorded end time or amount.
7. A zone becoming inactive after start does not prevent stopping or paying that session.
8. Reject a server end instant before the start or an amount above storage capacity, leaving the session active and otherwise unchanged.

## Pricing

All zones use hourly pricing in this version. Store and calculate at millisecond precision as defined in [architecture](../architecture.md#time-and-pricing).

For a nonnegative elapsed duration, use integer quotient and remainder:

```text
elapsedMilliseconds = endedAt - startedAt
wholeHours = elapsedMilliseconds / 3_600_000       (integer division)
partialHour = 1 if elapsedMilliseconds % 3_600_000 > 0, otherwise 0
chargedHours = max(1, wholeHours + partialHour)
amount = capturedHourlyRate * chargedHours
```

Use integer duration arithmetic and `BigDecimal` multiplication. Do not truncate duration to whole minutes or whole seconds before rounding. Reject negative duration rather than applying the one-hour minimum to it. Rates already have two decimals, so multiplying by whole hours needs no fractional-cent rounding.

| Elapsed time | Charged hours | Amount at 2.00 EUR/hour |
| --- | --- | --- |
| 0 milliseconds | 1 | 2.00 |
| 1 millisecond | 1 | 2.00 |
| 1 minute | 1 | 2.00 |
| 59 minutes | 1 | 2.00 |
| 60 minutes exactly | 1 | 2.00 |
| 60 minutes + 1 millisecond | 2 | 4.00 |
| 60 minutes + 1 second | 2 | 4.00 |
| 61 minutes | 2 | 4.00 |
| 120 minutes | 2 | 4.00 |
| 121 minutes | 3 | 6.00 |

A session beginning at `2.00` EUR/hour remains priced at `2.00` even if the zone later changes to `3.00`. The resulting completed amount is immutable. A zero-rate zone yields a zero charge but follows the same lifecycle.

Keep this behavior in `PricingService`, accepting the captured rate, start instant, and end instant. Future zone-specific charging rules can be introduced there without altering start/stop/payment responsibilities.

## User flow

Select a user, an available owned vehicle, a city, and an active zone. Start parking and confirm the vehicle appears in the active list. When finished, choose Stop Parking on that session. The session leaves the active list and appears as completed/unpaid with its final charge. The person can then choose Pay through the separate payment flow.

## API and database changes

Endpoints: `POST /api/parkings`, `GET /api/users/{userId}/parkings/active`, and `POST /api/parkings/{parkingId}/stop`. See [the parking API](../api.md#parking-response-shape).

Store sessions and their captured `hourly_rate` using [the session schema](../database.md#parking_sessions), state checks, and active-vehicle index.

## UI behavior

The start form requires an owned available vehicle, city, and active zone. Show the zone's current hourly rate and started-hour billing policy. Each active card has its own Stop action. After start, refresh active sessions; after stop, refresh active sessions and history so the unpaid item becomes visible.

Selecting another city affects future starts only. It must not hide already-active sessions from other cities.

## Acceptance criteria

- [ ] A valid start returns `201` with an active session and server-controlled captured rate/time.
- [ ] Missing resources, ownership violations, and inactive zones return the documented errors without inserting a session.
- [ ] Two active sessions can exist for Alex's two different vehicles.
- [ ] Two starts for the same vehicle result in exactly one active row and one conflict.
- [ ] A completed unpaid session does not prevent another start for that vehicle.
- [ ] Active listing contains only the selected user's active sessions in the documented order.
- [ ] All pricing table cases produce the expected charge.
- [ ] Crossing midnight or a daylight-saving transition uses elapsed UTC time correctly.
- [ ] Changing the current catalog rate after start does not change the captured rate or final calculation.
- [ ] Stop records a completed unpaid session, leaving balance and payment count unchanged.
- [ ] Repeated/concurrent stop requests cannot replace the first committed end time or amount.
- [ ] Another user cannot stop the session.
- [ ] A negative duration or unrepresentable charge rejects stop with no persisted session change.

## Verification

Use parameterized pricing unit tests, service tests for ownership and state rules, and fixed clocks. Use PostgreSQL integration tests for competing starts/stops, state constraints, and captured-rate persistence. Exercise the start/stop flow through HTTP as part of the integration smoke test.

Verification performed: none; implementation is pending.

## Open questions

None.
