# REST API Contract

Status: Only the bootstrap health endpoint below is implemented. All user, catalog, parking, and payment endpoints remain specified but unimplemented.

## Bootstrap health endpoint

`GET /api/health` returns `200` with `{"status":"UP"}`. It verifies that the HTTP application is responding and lets the frontend check its API connection. It does not verify PostgreSQL readiness or any parking feature. Compose uses it during bootstrap; the planned database-backed `/api/cities` check will replace it for container readiness once the catalog is implemented.

The business API conventions and contracts below are implementation targets. Their global error handler is not present in the bootstrap.

## Conventions

- Base path: `/api`. Browser requests use relative URLs.
- JSON request bodies use `Content-Type: application/json`; JSON responses use the same media type.
- IDs are positive integers represented as Java `Long`. Missing required IDs, non-integers, and nonpositive IDs are invalid requests.
- Monetary fields are decimal strings in EUR. Responses always use two fractional digits, such as `"2.00"`. Top-up input accepts one or two fractional digits, or an integer string.
- Timestamps are UTC ISO 8601 strings with millisecond precision and an explicit `Z`, such as `2026-09-21T10:15:00.000Z`.
- Nullable response fields are included as `null` rather than omitted.
- Collections are JSON arrays, including `[]` when an existing parent has no matching records.
- Missing parent resources return `404`; they are not represented as empty collections.
- User context is mandatory for user-specific operations. The selected user is demo context, not authenticated identity.
- Clients supply identifiers and top-up amount only. They cannot set session times, hourly rates, charges, status, payment amounts, or balances.

Examples are independent illustrations, not a sequence of requests sharing state.

## Endpoints

| Method | Path | Success | Response |
| --- | --- | --- | --- |
| GET | `/api/users` | 200 | `UserResponse[]` |
| GET | `/api/users/{userId}` | 200 | `UserResponse` |
| POST | `/api/users/{userId}/top-up` | 200 | `BalanceResponse` |
| GET | `/api/users/{userId}/vehicles` | 200 | `VehicleResponse[]` |
| GET | `/api/cities` | 200 | `CityResponse[]` |
| GET | `/api/cities/{cityId}/zones` | 200 | `ZoneResponse[]` |
| POST | `/api/parkings` | 201 | `ParkingResponse` |
| GET | `/api/users/{userId}/parkings/active` | 200 | `ParkingResponse[]` |
| POST | `/api/parkings/{parkingId}/stop` | 200 | `ParkingResponse` |
| POST | `/api/parkings/{parkingId}/payment` | 201 | `PaymentResponse` |
| GET | `/api/users/{userId}/parkings/history` | 200 | `ParkingResponse[]` |

No generic zone endpoint, CRUD routes, login routes, separate unpaid endpoint, or payment-attempt endpoint is required.

## Users and vehicles

### List users

`GET /api/users` returns users ordered by `name ASC, id ASC`:

```json
[
  { "id": 1, "name": "Alex Johnson", "balance": "20.00" },
  { "id": 2, "name": "Maria Smith", "balance": "10.00" }
]
```

### Get user

`GET /api/users/1` returns the current server balance:

```json
{ "id": 1, "name": "Alex Johnson", "balance": "20.00" }
```

Errors: invalid identifier, user not found.

### Top up

`POST /api/users/1/top-up`:

```json
{ "amount": "20.00" }
```

Success:

```json
{ "userId": 1, "balance": "40.00" }
```

The amount must be a JSON string matching `^(0|[1-9][0-9]{0,9})(\.[0-9]{1,2})?$` and have a parsed value from `0.01` through `9999999999.99`. Examples accepted: `"20"`, `"20.0"`, `"20.00"`, `"0.01"`. Reject JSON numbers, `null`, missing amounts, negatives, zero, excessive precision, whitespace, grouping separators, currency signs, exponent notation, and leading zeros other than `0` itself. The UI may trim the input before submitting it.

Validate the new balance against the storage maximum before changing it. Errors: invalid request/body, invalid top-up amount, user not found, balance limit exceeded.

### List a user's vehicles

`GET /api/users/1/vehicles` returns only that user's vehicles, ordered by `plateNumber ASC, id ASC`:

```json
[
  { "id": 1, "plateNumber": "CA1234AB" },
  { "id": 2, "plateNumber": "CB5678CD" }
]
```

Errors: invalid identifier, user not found. An existing user with no vehicles receives `[]`.

## Cities and zones

### List cities

`GET /api/cities` returns cities ordered by `name ASC, id ASC`:

```json
[
  { "id": 2, "name": "Plovdiv" },
  { "id": 1, "name": "Sofia" }
]
```

### List active zones

`GET /api/cities/1/zones` returns only active zones of that city, ordered by `name ASC, id ASC`:

```json
[
  { "id": 1, "name": "Blue Zone", "cityId": 1, "pricePerHour": "2.00", "active": true },
  { "id": 2, "name": "Green Zone", "cityId": 1, "pricePerHour": "1.00", "active": true }
]
```

Errors: invalid identifier, city not found. An existing city with no active zones receives `[]`. Backend start validation must still reject an inactive zone submitted directly.

## Parking response shape

Start, active-list, stop, and history endpoints share one shape. Rates in a session response are the captured session rate; `zone` contains identity/display data and does not expose a second, potentially different current rate.

```json
{
  "id": 10,
  "userId": 1,
  "vehicle": {
    "id": 1,
    "plateNumber": "CA1234AB"
  },
  "zone": {
    "id": 1,
    "name": "Blue Zone",
    "city": { "id": 1, "name": "Sofia" }
  },
  "hourlyRate": "2.00",
  "startedAt": "2026-09-21T10:15:00.000Z",
  "endedAt": null,
  "amount": null,
  "status": "ACTIVE",
  "paymentStatus": null,
  "paidAt": null
}
```

`status` is `ACTIVE` or `COMPLETED`. `paymentStatus` is `null` for active parking, `UNPAID` for completed parking without payment, and `PAID` for completed parking with payment. `paidAt` is present only for paid parking.

### Start parking

`POST /api/parkings`:

```json
{ "userId": 1, "vehicleId": 1, "zoneId": 1 }
```

Returns `201` and the active `ParkingResponse` above. The backend derives the city from the zone and the rate from the zone at start. No `cityId` is required in this request.

Errors: invalid request, missing user/vehicle/zone, vehicle owned by another user, inactive zone, vehicle already active.

### List active parking

`GET /api/users/1/parkings/active` returns an array of active `ParkingResponse` objects, ordered by `startedAt DESC, id DESC`. It can contain multiple vehicles. Errors: invalid identifier, user not found.

### Stop parking

`POST /api/parkings/10/stop`:

```json
{ "userId": 1 }
```

Success:

```json
{
  "id": 10,
  "userId": 1,
  "vehicle": { "id": 1, "plateNumber": "CA1234AB" },
  "zone": { "id": 1, "name": "Blue Zone", "city": { "id": 1, "name": "Sofia" } },
  "hourlyRate": "2.00",
  "startedAt": "2026-09-21T10:15:00.000Z",
  "endedAt": "2026-09-21T11:20:00.000Z",
  "amount": "4.00",
  "status": "COMPLETED",
  "paymentStatus": "UNPAID",
  "paidAt": null
}
```

Errors: invalid request, user/session not found, another user's session, already completed, clock earlier than start, calculated amount exceeds storage limit. Stop does not modify the balance or create a payment.

## Payments

`POST /api/parkings/10/payment`:

```json
{ "userId": 1 }
```

Success:

```json
{
  "parkingId": 10,
  "amount": "4.00",
  "paymentStatus": "PAID",
  "paidAt": "2026-09-21T11:22:00.000Z",
  "remainingBalance": "16.00"
}
```

The server charges the persisted session amount. Errors: invalid request, user/session not found, another user's session, session still active, already paid, insufficient balance, clock earlier than the session end. A completed row without an amount is an unexpected invariant failure, not a client-supplied charge opportunity.

## History

`GET /api/users/1/parkings/history` returns **completed sessions only**, both paid and unpaid, ordered by `startedAt DESC, id DESC`. Use the same `ParkingResponse` shape as stop. A paid item contains `paymentStatus: "PAID"` and its UTC `paidAt`; its stored amount and end time do not change after payment.

The frontend derives the completed/unpaid section by filtering this response for `paymentStatus === "UNPAID"`. No pagination or filtering parameters are required in the prototype. Errors: invalid identifier, user not found.

## Error contract

All application errors use a stable machine-readable code and a human-readable message:

```json
{
  "code": "INSUFFICIENT_BALANCE",
  "message": "Insufficient account balance"
}
```

| HTTP | Code | Situation |
| --- | --- | --- |
| 400 | `INVALID_REQUEST` | Malformed JSON, missing/invalid IDs, invalid request structure |
| 400 | `INVALID_TOP_UP_AMOUNT` | Missing or invalid top-up amount, including precision/range/type violations |
| 400 | `ZONE_INACTIVE` | Selected zone is inactive |
| 400 | `PARKING_NOT_COMPLETED` | Payment requested for active parking |
| 403 | `VEHICLE_NOT_OWNED` | Vehicle belongs to a different existing user |
| 403 | `PARKING_NOT_OWNED` | Session belongs to a different existing user |
| 404 | `USER_NOT_FOUND` | Selected user does not exist |
| 404 | `VEHICLE_NOT_FOUND` | Vehicle does not exist |
| 404 | `CITY_NOT_FOUND` | City does not exist |
| 404 | `ZONE_NOT_FOUND` | Zone does not exist |
| 404 | `PARKING_NOT_FOUND` | Session does not exist |
| 409 | `VEHICLE_ALREADY_PARKED` | Vehicle already has an active session |
| 409 | `PARKING_ALREADY_COMPLETED` | Stop requested for completed parking |
| 409 | `PARKING_ALREADY_PAID` | Session already has a payment |
| 409 | `INSUFFICIENT_BALANCE` | Balance is below the stored session amount |
| 409 | `BALANCE_LIMIT_EXCEEDED` | Top-up would exceed `9999999999.99` |
| 409 | `AMOUNT_LIMIT_EXCEEDED` | Calculated parking amount exceeds `9999999999.99` |
| 409 | `CLOCK_BEFORE_SESSION_TIME` | Stop time is before start or payment time is before end |
| 500 | `INTERNAL_ERROR` | Unexpected application, database, or stored-invariant failure |

Return simple messages without stack traces, SQL, or entity dumps. Client validation errors take precedence over service validation because request binding happens first. Services resolve the selected user before dependent resource/ownership checks. Check ownership before reporting a session's lifecycle or payment state.

## Duplicate requests and ambiguous outcomes

Repeated stop and payment requests return the corresponding `409`; they do not recalculate or charge again. An identical start while the session remains active returns `409`. A later start after completion can create a new session.

Each accepted top-up request adds funds, so top-up is not idempotent. After a timeout or connection failure on a mutation, the UI must refresh the current state and ask the user to review it before issuing another action. Do not automatically retry mutation requests. Reads can be retried explicitly from the UI.
