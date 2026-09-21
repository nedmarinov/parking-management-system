# 005 — Parking History

Status: Implemented (backend and UI); browser review pending.

## Purpose

Show a selected user's completed parking, final charges, and payment status, and make unpaid completed sessions easy to settle.

## Scope

Includes completed history and a derived unpaid section. Active parking uses its dedicated list. Pagination, filtering, export, wallet transaction history, and live polling are excluded for the small demo dataset.

Depends on [parking completion](003-parking-lifecycle.md) and [payments](004-parking-payments.md).

## Business rules

1. The user must exist.
2. Return that user's completed sessions only, including both paid and unpaid sessions.
3. Sort by `startedAt DESC`, then `id DESC` for deterministic ties.
4. Include session and vehicle identity, zone and city, captured hourly rate, start/end times, final amount, parking status, derived payment status, and payment time.
5. An unpaid item has `paymentStatus = UNPAID` and `paidAt = null`. A paid item has `paymentStatus = PAID` and its recorded payment time.
6. Return stored final amounts. Reading history never recalculates charges or mutates records.
7. Derive the unpaid list from the history response. No separate unpaid persistence or endpoint is needed.
8. History survives application and database container restarts while the data volume is retained.

## User flow

Load history when selecting a user. After stop, refresh history and show the new unpaid item. After payment, refresh history to show paid status and remove the item from the unpaid section. The item remains in completed history.

## API and database changes

Use `GET /api/users/{userId}/parkings/history`, returning the shared `ParkingResponse` array. See [the history API](../api.md#history).

Read sessions and related vehicle/zone/city/payment data using the [database indexes](../database.md#indexes-and-deletion-behavior) and an intentional fetch plan. No additional table is required.

## UI behavior

Show vehicle, city, zone, start, end, captured rate, amount, payment status, and paid time. Use local date/time formatting with the timezone convention stated. Preserve the distinction between no history and a failed request.

An unpaid item appears in both the unpaid section and overall completed history. Provide its Pay action in the unpaid section so there is one obvious action surface. When switching users, clear old history immediately and reject stale responses.

## Acceptance criteria

- [ ] Active sessions do not appear in completed history.
- [ ] A newly stopped session appears with the final amount and `UNPAID`.
- [ ] A paid session remains in history with `PAID` and its payment timestamp.
- [ ] Only the selected user's sessions are returned.
- [ ] Sessions are ordered by start time descending, with descending ID for ties.
- [ ] An existing user without completed sessions receives `200 []` and an empty UI state.
- [ ] A nonexistent user receives `404 USER_NOT_FOUND`.
- [ ] Changing a catalog rate does not alter recorded rates or amounts in history.
- [ ] Refreshing the page or restarting the stack with its volume intact preserves history.
- [ ] Switching users while a history request is pending cannot display the previous user's records.
- [ ] A failed history refresh is shown as an error, not as proof that the history is empty.

## Verification

Use repository/API tests for ownership, status filtering, sorting, and payment mapping. Exercise stop-to-history and pay-to-history in the integration flow. Verify persistence during the Compose smoke test and stale-response handling in the UI.

Verification performed: `PaymentApiIT` covers status filtering, ownership, ordering, empty history, unknown users, unpaid and paid mapping, and captured rates after a catalog change. Restart persistence and UI stale-response handling remain for Phases 9–11.

## Open questions

None.
