# 004 — Parking Payments

Status: Implemented (backend and UI); browser review pending.

## Purpose

Let a user explicitly settle a completed session from their fictional balance, with a durable payment record and a correct remaining balance.

## Scope

Includes payment eligibility, sufficient/insufficient balance, duplicate prevention, and atomic persistence. No payment provider, refund, partial payment, failed-attempt history, or automatic charge at stop is included.

Depends on [parking completion](003-parking-lifecycle.md) and uses balances that can be replenished through [top-up](002-wallet-top-up.md).

## Business rules

1. The selected user and session must exist, and the session must belong to that user.
2. The session must be `COMPLETED` and have a persisted final amount.
3. It must not already have a payment.
4. The user's locked current balance must be at least the session amount. Equal balance is sufficient.
5. The client cannot supply or override the amount to charge.
6. Deduct the stored amount and insert one payment with the same amount, `PAID`, and the server's current UTC instant in one transaction.
7. Payment time cannot be earlier than the stored session end. If the server clock would violate this rule, reject without changing state.
8. Successful payment leaves session status `COMPLETED`; the response derives `paymentStatus = PAID` from the payment row.
9. Insufficient funds, duplicate payment, persistence failures, and other rejected attempts must not partially update the balance or create a payment.
10. A zero-amount completed session can be paid, producing a payment record with no balance change.

## Transaction and concurrency behavior

Acquire the user lock first, then the session lock, using [the shared policy](../architecture.md#transactions-and-concurrent-requests). Re-read and validate balance, status, and existing payment under the locks. Keep payment insertion and deduction in the same transaction; successful method return must represent a committed result.

Two requests for one session produce one successful payment and one `PARKING_ALREADY_PAID` response, with exactly one deduction. Payments for two sessions belonging to one user serialize their balance checks. If the available funds cover only one, only one succeeds. Both may succeed if sufficient funds exist for both.

The unique payment/session constraint remains a database safeguard. Unexpected insertion or commit failure rolls back any balance update.

## User flow

After stopping, the person sees the final amount and chooses Pay. On success, refresh balance and history, move the item out of the unpaid section, and show its paid state.

If funds are insufficient, leave the item unpaid and show the balance error with access to top-up. After adding funds, the person explicitly chooses Pay again; top-up does not trigger automatic settlement.

Example: `10.00` balance minus `4.00` parking gives `6.00`. With `2.00` available, the same request returns a conflict, keeps balance at `2.00`, and creates no payment.

## API and database changes

Use `POST /api/parkings/{parkingId}/payment` with the required `userId` body. See [the payment API](../api.md#payments) for success and error contracts.

Update `users.balance` and insert into [payments](../database.md#payments). Do not recalculate the session amount, change its end time, or store `UNPAID` as a payment record.

## UI behavior

Expose a Pay action only for completed unpaid items and display the stored charge. Disable mutation controls while a request is pending. Do not infer authoritative eligibility from a potentially stale balance. A timeout must trigger a state refresh and review message rather than an automatic retry.

## Acceptance criteria

- [ ] A completed owned session with sufficient funds returns `201`, creates one payment, and deducts the exact stored amount.
- [ ] A balance equal to the amount becomes `0.00` after successful payment.
- [ ] Insufficient funds return `INSUFFICIENT_BALANCE`; balance, session data, and payment count remain unchanged.
- [ ] Active parking cannot be paid.
- [ ] Another user cannot pay the session.
- [ ] A repeated payment returns `PARKING_ALREADY_PAID` without another deduction.
- [ ] Two concurrent payments for one session commit at most one payment and one deduction.
- [ ] Competing payments for two sessions cannot overdraw the account or lose a deduction.
- [ ] A database failure after the balance update but before successful payment commit rolls back the whole operation.
- [ ] A zero-amount session creates a `0.00` payment without changing the balance.
- [ ] A payment time before the session end returns the documented conflict and changes nothing.
- [ ] After top-up resolves insufficient funds, an explicit retry can pay the same completed session.
- [ ] History shows the stored amount, `PAID`, and `paidAt`; the session remains `COMPLETED`.

## Verification

Use service tests for eligibility and exact deduction. Use PostgreSQL integration tests for actual rollback, uniqueness, and concurrency. To establish atomicity, force a controlled failure within the transaction after an update has reached the database, then verify the original balance and no payment from a separate transaction. Do not treat Mockito interaction assertions as proof of rollback.

Verification performed: `PaymentApiIT` covers success, exact and equal-balance deduction, insufficient funds with top-up retry, repeat payment, zero amount, error order, and payment-before-end. `PaymentConcurrencyIT` covers duplicate payments, two sessions with funds for one, and top-up racing payment. `PaymentAtomicityIT` forces a failure after the deduction reaches PostgreSQL and confirms from a fresh transaction that nothing committed.

## Open questions

None.
