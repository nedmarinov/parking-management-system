# 002 — Wallet Top-Up

Status: Backend implemented; UI pending (Phase 9).

## Purpose

Allow a demo user to add fictional EUR funds so completed parking can be paid without a real payment provider.

## Scope

Includes balance display, valid amount input, transactional balance increase, storage-limit handling, and feedback. No payment instruments, external transactions, refunds, or wallet transaction history are included.

## Business rules

1. The user must exist.
2. Input must follow the exact decimal-string format and amount bounds in [the top-up API](../api.md#top-up). The smallest accepted value is `0.01`.
3. Parse to `BigDecimal` and normalize valid values to scale two without rounding invalid precision.
4. The resulting balance must remain between `0.00` and `9999999999.99`. A valid amount that would exceed that maximum returns `BALANCE_LIMIT_EXCEEDED`.
5. Lock the user and perform validation of the resulting balance and update within one transaction, following [the shared mutation lock rule](../architecture.md#transactions-and-concurrent-requests).
6. Invalid requests and failed transactions leave the persisted balance unchanged.
7. Concurrent top-ups must both be reflected if both succeed. A payment racing a top-up must use serialized, current state rather than overwrite either balance change.
8. Each accepted request is a new top-up. There is no idempotency key or automatic request retry.

## User flow

The person enters an amount and chooses Add Funds. On success the UI reloads user details, clears the input, and confirms the added funds. On validation failure it keeps the input available for correction and displays the reason. If an unpaid session exists, the user can then explicitly choose Pay.

Example: a `5.00` balance plus a `20.00` top-up becomes `25.00`.

## API and database changes

Use `POST /api/users/{userId}/top-up` with `{ "amount": "20.00" }` and return the resulting balance. See [the API contract](../api.md#top-up) for the canonical response and errors. Validate the JSON token type as well as its text; request binding must not silently coerce a numeric token to a string and bypass the contract.

Only `users.balance` changes. No payment or parking record is created. See [money limits](../database.md#monetary-limits).

## UI behavior

Keep input as a string with decimal input mode. The UI may trim surrounding whitespace before submitting. Disable mutation submission while a request is pending. Show a failed refresh separately from a failed top-up, and never automatically retry a top-up after an ambiguous network failure. See [frontend action handling](../frontend.md#actions-and-refreshes).

## Acceptance criteria

- [ ] `"20"`, `"20.0"`, `"20.00"`, `"5.50"`, and `"0.01"` are accepted and increase the balance exactly.
- [ ] Missing/null input, JSON numeric input, empty strings, zero, negatives, and nonnumeric input are rejected.
- [ ] `"10.123"`, `"1e2"`, `"01.00"`, `"1,00"`, and values with more than ten integer digits are rejected without rounding.
- [ ] A nonexistent user returns `USER_NOT_FOUND` for an otherwise valid request.
- [ ] A top-up equal to the remaining balance capacity succeeds; exceeding it returns `BALANCE_LIMIT_EXCEEDED` and changes nothing.
- [ ] Two concurrent successful top-ups of `5.00` and `7.00` increase the original balance by `12.00`.
- [ ] Top-up concurrent with payment preserves both committed changes; payment's eligibility depends on the balance when its lock is acquired.
- [ ] Successful top-up refreshes the balance and clears the input.
- [ ] A failed top-up leaves existing unpaid sessions unchanged and payable later.
- [ ] Repeating a completed top-up request intentionally adds the amount again.

## Verification

Unit-test arithmetic and limit handling; exercise HTTP binding and decimal validation using real request bodies; use PostgreSQL transactions for concurrent top-ups and payment/top-up interactions. A mocked repository alone cannot prove absence of lost updates.

Verification performed: `TopUpAmountTest`, `WalletApiIT`, and `TopUpConcurrencyIT` cover format/range validation, JSON type checks, exact increases, repeat requests, limit handling, unknown users, and concurrent top-ups. Top-up racing payment is verified with payments (Phase 7).

## Open questions

None.
