# 001 — Demo User Selection

Status: Implemented (backend and UI); browser review pending.

## Purpose

Let a person switch between predefined accounts and demonstrate user-specific balances, vehicles, parking, and payments without introducing authentication.

## Scope

Includes listing users, loading one user's details, listing their vehicles, and switching the current UI context. User registration, editing, authentication, and vehicle management are excluded. Demo data is defined in the [database design](../database.md#demo-data).

## Business rules

1. A vehicle has exactly one owner. A session belongs to the selected user and an owned vehicle.
2. User-specific reads and writes require an explicit user ID through their documented path or body.
3. A missing user returns `USER_NOT_FOUND`; an existing user with no matching vehicles or sessions receives an empty collection.
4. Start rejects another user's vehicle with `VEHICLE_NOT_OWNED`. Stop and pay reject another user's session with `PARKING_NOT_OWNED`.
5. The selector does not authenticate or restrict who can select a demo account. It demonstrates business ownership checks only.
6. Changing selection does not mutate any account or session.
7. User selection is in-memory UI state. A full page reload selects the first returned demo user again.

## User flow

Load users on application startup and select the first returned user. Fetch that user's current details, vehicles, active sessions, and completed history. When the person selects another user, clear the old user-specific data immediately, reset the selected vehicle and top-up input, and fetch the new context.

Preserve shared city/zone selection if valid. Ignore responses belonging to a previous user selection. An error loading a balance must be displayed as a failed load, not as a zero balance.

## API and database changes

Endpoints: `GET /api/users`, `GET /api/users/{userId}`, and `GET /api/users/{userId}/vehicles`. Their shapes and ordering are defined in [the API contract](../api.md#users-and-vehicles).

Tables: `users`, `vehicles`, and the session ownership foreign key, defined in [the database design](../database.md). Balance mutation is owned by [top-up](002-wallet-top-up.md) and [payments](004-parking-payments.md).

## UI behavior

Provide a labeled user selector and show the current user's balance. Disable user-specific mutations until their required data has loaded. Display a clear empty state if no users or vehicles exist. Request cancellation and stale-response handling follow [frontend selection behavior](../frontend.md#selection-changes).

## Acceptance criteria

- [ ] Alex Johnson and Maria Smith appear in the initial user list.
- [ ] Alex initially has balance `20.00` and vehicles `CA1234AB` and `CB5678CD`; Maria has balance `10.00` and `PB1234CD`.
- [ ] Switching users changes balance, vehicle options, active sessions, unpaid items, and history.
- [ ] Switching users resets the vehicle selection and top-up input.
- [ ] A delayed response for Alex cannot overwrite Maria's selected context.
- [ ] An existing user with no vehicles receives `200 []` and a usable empty state.
- [ ] A nonexistent user returns `404 USER_NOT_FOUND` on user-specific endpoints.
- [ ] Alex cannot start parking for Maria's vehicle or stop/pay Maria's session; each rejection leaves data unchanged.
- [ ] Changing selection alone leaves balances, sessions, and payments unchanged.

## Verification

Use service/repository checks for ownership and collection filtering, API tests for missing/invalid identifiers and status codes, and a UI check with delayed responses while switching users. Test stop/pay ownership as part of their feature suites.

Verification performed: none; implementation is pending.

## Open questions

None.
