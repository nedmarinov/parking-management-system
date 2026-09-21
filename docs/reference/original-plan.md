# Parking Management System — Development Plan

## 1. Objective

Build a working full-stack prototype for a paid parking management system.

The platform should support:

- Multiple users
- Multiple vehicles per user
- Multiple cities
- Multiple parking zones per city
- Different pricing between parking zones
- Active parking sessions
- Parking completion and price calculation
- Fictional account balance
- Account top-up
- Payment using account balance
- Parking history

The goal is not to build a production-ready system.

The focus is on:

- Correct business logic
- Clean domain modeling
- Clear technical decisions
- A working end-to-end flow
- PostgreSQL persistence
- Relevant automated tests
- Simple startup with Docker Compose

---

# 2. Technology Stack

## Backend

- Java 21
- Spring Boot
- Spring Web
- Spring Data JPA
- Jakarta Bean Validation
- PostgreSQL Driver
- Flyway
- Maven
- JUnit 5
- Mockito

## Frontend

- React
- Vite
- Native `fetch`
- shadcn/ui

## Persistence

- PostgreSQL
- Flyway migrations

## Infrastructure

- Docker
- Docker Compose
- Nginx

The entire application should start with:

```bash
docker compose up --build
```

Frontend:

```text
http://localhost:3000
```

---

# 3. Scope

## Included

The prototype will support:

- Multiple users
- Selecting the current demo user
- Fictional account balance per user
- Top-up with a valid monetary amount
- Multiple vehicles per user
- Multiple cities
- Multiple parking zones per city
- Different hourly rates between zones
- Starting a parking session
- Viewing active parking sessions
- Preventing duplicate active parking for the same vehicle
- Stopping a parking session
- Parking price calculation
- Paying for completed parking
- Insufficient-balance handling
- Payment status
- Parking history
- PostgreSQL persistence
- Flyway migrations
- Automated tests
- Docker Compose
- README documentation

## Out of Scope

Do not implement unless clearly necessary:

- Authentication
- Passwords
- JWT
- OAuth
- User registration
- Admin panel
- User CRUD
- Vehicle CRUD
- City CRUD
- Parking-zone CRUD
- Refunds
- Wallet transaction history
- Redux
- WebSockets
- Microservices
- Kubernetes
- Cloud deployment
- Complex UI
- Production observability
- CI/CD

---

# 4. User Handling

Authentication is intentionally outside the scope of the prototype.

Instead, the application will provide a simple user selector.

Example:

```text
Current User

[ Alex Johnson ▼ ]
```

Changing the current user changes:

- Account balance
- Vehicles
- Active parking sessions
- Completed unpaid parking
- Parking history

This allows the application to demonstrate actual user ownership rules without introducing authentication complexity.

User-specific operations should include a `userId`.

---

# 5. Domain Model

## User

```text
User
----
id
name
balance
```

Fields:

```text
id: Long
name: String
balance: BigDecimal
```

Rules:

- Balance cannot be negative.
- Monetary values use `BigDecimal`.
- Balance represents fictional money.

Relationship:

```text
User 1 -> N Vehicle
User 1 -> N ParkingSession
```

---

## Vehicle

```text
Vehicle
-------
id
plateNumber
user
```

Fields:

```text
id: Long
plateNumber: String
user: User
```

Rules:

- A vehicle belongs to exactly one user.
- Plate number should be unique.

Relationship:

```text
User 1 -> N Vehicle
```

---

## City

```text
City
----
id
name
```

Fields:

```text
id: Long
name: String
```

The prototype must contain at least two cities.

Initial cities:

```text
Sofia
Plovdiv
```

Relationship:

```text
City 1 -> N ParkingZone
```

`City` is a separate entity because the system is intended to operate across multiple cities.

---

## ParkingZone

```text
ParkingZone
-----------
id
name
city
pricePerHour
active
```

Fields:

```text
id: Long
name: String
city: City
pricePerHour: BigDecimal
active: boolean
```

Relationship:

```text
City 1 -> N ParkingZone
```

Example data:

```text
Sofia
  Blue Zone  -> 2.00 EUR/hour
  Green Zone -> 1.00 EUR/hour

Plovdiv
  Blue Zone  -> 1.50 EUR/hour
  Green Zone -> 1.00 EUR/hour
```

Rates are prototype seed data and can be adjusted.

---

## ParkingSession

```text
ParkingSession
--------------
id
user
vehicle
zone
startedAt
endedAt
amount
status
```

Fields:

```text
id: Long
user: User
vehicle: Vehicle
zone: ParkingZone
startedAt: timestamp
endedAt: timestamp | null
amount: BigDecimal | null
status: ParkingStatus
```

Status:

```text
ACTIVE
COMPLETED
```

Rules:

While active:

```text
startedAt != null
endedAt = null
amount = null
status = ACTIVE
```

After stopping:

```text
startedAt != null
endedAt != null
amount != null
status = COMPLETED
```

Relationships:

```text
User        1 -> N ParkingSession
Vehicle     1 -> N ParkingSession
ParkingZone 1 -> N ParkingSession
```

---

## Payment

```text
Payment
-------
id
parkingSession
amount
status
paidAt
```

Fields:

```text
id: Long
parkingSession: ParkingSession
amount: BigDecimal
status: PaymentStatus
paidAt: timestamp
```

Status:

```text
PAID
```

Relationship:

```text
ParkingSession 1 -> 0..1 Payment
```

A payment record is created only when payment succeeds.

Therefore:

```text
ACTIVE parking
payment = null

COMPLETED unpaid parking
payment = null

COMPLETED paid parking
payment.status = PAID
```

The UI can derive:

```text
payment == null -> UNPAID
payment != null -> PAID
```

---

# 6. Entity Relationships

```text
User
 ├── Vehicles
 ├── Parking Sessions
 └── Balance

City
 └── Parking Zones

ParkingSession
 ├── User
 ├── Vehicle
 ├── Parking Zone
 └── Payment
```

Database relationships:

```text
User 1 -------- N Vehicle

City 1 -------- N ParkingZone

User 1 -------- N ParkingSession

Vehicle 1 ----- N ParkingSession

ParkingZone 1 - N ParkingSession

ParkingSession 1 --- 0..1 Payment
```

---

# 7. Account Balance and Top-Up

The account balance is a deliberate prototype extension.

It makes the payment flow meaningful without requiring a real payment provider.

## Top-Up Flow

Example UI:

```text
Balance: €5.00

Top Up
[ 20.00 ]

[ Add Funds ]
```

After successful top-up:

```text
Balance: €25.00
```

## Top-Up Rules

The amount must:

- Be present
- Be greater than zero
- Have at most two decimal places
- Use `BigDecimal`

Valid:

```text
20
20.00
5.50
0.01
```

Invalid:

```text
0
-1
-10.00
10.123
abc
empty value
```

Suggested DTO:

```text
TopUpRequest
------------
amount: BigDecimal
```

Suggested validation:

```java
@NotNull
@DecimalMin(value = "0.01")
@Digits(integer = 10, fraction = 2)
```

Top-up operation should be transactional.

---

# 8. Starting Parking

When starting a parking session:

1. User must exist.
2. Vehicle must exist.
3. Vehicle must belong to the selected user.
4. Parking zone must exist.
5. Parking zone must be active.
6. `startedAt` must be stored.
7. Parking zone must be stored.
8. User must be associated with the parking session.
9. Vehicle must not already have an active parking session.

## Important Invariant

The active parking restriction applies to the **vehicle**, not the user.

This is valid:

```text
User A

Vehicle A -> ACTIVE
Vehicle B -> ACTIVE
```

This must fail:

```text
Vehicle A -> ACTIVE
Vehicle A -> another ACTIVE parking
```

The user may therefore have multiple active parking sessions when using different vehicles.

---

# 9. Stopping Parking

When stopping a parking session:

1. Parking session must exist.
2. Parking session must belong to the selected user.
3. Parking session must be `ACTIVE`.
4. `endedAt` is stored.
5. Parking amount is calculated.
6. Calculated amount is persisted.
7. Status changes to `COMPLETED`.

Stopping a parking session does **not** automatically charge the user.

Payment is a separate explicit action.

This preserves the required flow:

```text
Stop parking
-> show amount
-> user chooses Pay
```

---

# 10. Pricing

Initial pricing examples:

```text
Sofia

Blue Zone
2.00 EUR/hour

Green Zone
1.00 EUR/hour
```

Additional prototype data:

```text
Plovdiv

Blue Zone
1.50 EUR/hour

Green Zone
1.00 EUR/hour
```

Every started hour is charged as a full hour.

Examples:

```text
1 minute    -> 1 hour
59 minutes  -> 1 hour
60 minutes  -> 1 hour
61 minutes  -> 2 hours
120 minutes -> 2 hours
121 minutes -> 3 hours
```

Use `BigDecimal` for all monetary calculations.

Never use `double` for money.

Pricing logic should be isolated in:

```text
PricingService
```

Conceptually:

```text
calculate(
    ParkingZone zone,
    startedAt,
    endedAt
)
```

For the initial implementation:

```text
chargedHours = ceiling(duration / 1 hour)

amount =
    zone.pricePerHour
    *
    chargedHours
```

Be careful with seconds.

For example:

```text
60 minutes 0 seconds -> 1 hour
60 minutes 1 second  -> 2 hours
```

The implementation should therefore round based on the complete duration, not only truncated minutes.

---

# 11. Pricing Extensibility

The domain allows parking zones to have different charging rules.

The prototype only implements hourly pricing.

Do not overengineer the first version with a complex strategy hierarchy unless necessary.

Keep pricing isolated behind `PricingService`.

Document the decision:

> The current implementation uses hourly pricing based on the examples from the assignment. Pricing logic is isolated so additional zone-specific charging strategies can be added later without modifying the parking lifecycle.

---

# 12. Payment Flow

Payment is made from the user's fictional account balance.

Payment is only allowed when:

- Parking exists.
- Parking belongs to the user.
- Parking status is `COMPLETED`.
- Parking has not already been paid.
- Parking amount exists.
- User has sufficient balance.

Example:

```text
User balance: €10.00
Parking amount: €4.00

Pay

New balance: €6.00
Payment status: PAID
```

Insufficient balance:

```text
User balance: €2.00
Parking amount: €4.00

Pay

-> Payment rejected
-> Balance remains €2.00
-> Parking remains UNPAID
```

The balance deduction and payment creation must happen in a single transaction.

Conceptually:

```text
BEGIN TRANSACTION

validate parking
validate payment does not exist
validate balance >= parking amount

user.balance -= parking.amount

create Payment(
    amount = parking.amount,
    status = PAID,
    paidAt = now
)

COMMIT
```

If any operation fails, neither balance nor payment should change.

---

# 13. Backend Architecture

Use a simple layered architecture:

```text
Controller
    |
    v
Service
    |
    v
Repository
    |
    v
PostgreSQL
```

Suggested package structure:

```text
controller/
dto/
entity/
repository/
service/
exception/
```

Optional:

```text
mapper/
```

only if mapping logic becomes repetitive.

Rules:

- Business logic belongs in services.
- Controllers remain thin.
- Repositories only handle persistence.
- Do not expose JPA entities directly.
- Use request and response DTOs.
- Use constructor injection.
- Use transactions around state changes.

---

# 14. Main Services

Suggested services:

```text
UserService
  getUsers()
  getUser()
  topUp()

CityService
  getCities()

VehicleService
  getVehiclesForUser()

ParkingZoneService
  getZones()
  getZonesForCity()

PricingService
  calculate()

ParkingService
  startParking()
  getActiveParkings()
  stopParking()
  getHistory()

PaymentService
  pay()
```

State-changing operations should normally use `@Transactional`:

```text
topUp()
startParking()
stopParking()
pay()
```

---

# 15. REST API

## Users

### List Users

```http
GET /api/users
```

Response example:

```json
[
  {
    "id": 1,
    "name": "Alex Johnson",
    "balance": 20.00
  },
  {
    "id": 2,
    "name": "Maria Smith",
    "balance": 10.00
  }
]
```

### Get User

```http
GET /api/users/{userId}
```

Returns:

```text
id
name
balance
```

### Top Up User Balance

```http
POST /api/users/{userId}/top-up
```

Request:

```json
{
  "amount": 20.00
}
```

Response:

```json
{
  "userId": 1,
  "balance": 40.00
}
```

---

# 16. Vehicle API

```http
GET /api/users/{userId}/vehicles
```

Returns only vehicles owned by that user.

Example:

```json
[
  {
    "id": 1,
    "plateNumber": "CA1234AB"
  },
  {
    "id": 2,
    "plateNumber": "CB5678CD"
  }
]
```

---

# 17. City API

```http
GET /api/cities
```

Example:

```json
[
  {
    "id": 1,
    "name": "Sofia"
  },
  {
    "id": 2,
    "name": "Plovdiv"
  }
]
```

---

# 18. Parking Zone API

Recommended:

```http
GET /api/cities/{cityId}/zones
```

Returns active parking zones for the selected city.

Example:

```json
[
  {
    "id": 1,
    "name": "Blue Zone",
    "pricePerHour": 2.00,
    "active": true
  }
]
```

Optionally support:

```http
GET /api/zones
```

if a general zone endpoint is useful internally.

---

# 19. Start Parking API

```http
POST /api/parkings
```

Request:

```json
{
  "userId": 1,
  "vehicleId": 1,
  "zoneId": 1
}
```

Validations:

```text
user exists
vehicle exists
vehicle belongs to user
zone exists
zone is active
vehicle does not already have active parking
```

Example response:

```json
{
  "id": 10,
  "vehicle": {
    "id": 1,
    "plateNumber": "CA1234AB"
  },
  "zone": {
    "id": 1,
    "name": "Blue Zone",
    "city": "Sofia",
    "pricePerHour": 2.00
  },
  "startedAt": "2026-09-21T10:15:00",
  "status": "ACTIVE"
}
```

---

# 20. Active Parking API

Because one user can have multiple active parking sessions, return a collection.

Recommended:

```http
GET /api/users/{userId}/parkings/active
```

Response:

```json
[
  {
    "id": 10,
    "vehicle": {
      "id": 1,
      "plateNumber": "CA1234AB"
    },
    "zone": {
      "id": 1,
      "name": "Blue Zone",
      "city": "Sofia"
    },
    "startedAt": "2026-09-21T10:15:00",
    "status": "ACTIVE"
  }
]
```

This is better than assuming one active parking per user.

---

# 21. Stop Parking API

```http
POST /api/parkings/{parkingId}/stop
```

The user context should also be validated.

Choose one API style and use it consistently.

Response example:

```json
{
  "id": 10,
  "vehicle": "CA1234AB",
  "zone": "Blue Zone",
  "city": "Sofia",
  "startedAt": "2026-09-21T10:15:00",
  "endedAt": "2026-09-21T11:20:00",
  "amount": 4.00,
  "status": "COMPLETED",
  "paymentStatus": "UNPAID"
}
```

---

# 22. Payment API

```http
POST /api/parkings/{parkingId}/payment
```

Request can contain the user ID if required by the chosen API style:

```json
{
  "userId": 1
}
```

On success:

```json
{
  "parkingId": 10,
  "amount": 4.00,
  "paymentStatus": "PAID",
  "paidAt": "2026-09-21T11:22:00",
  "remainingBalance": 16.00
}
```

---

# 23. Parking History API

Recommended:

```http
GET /api/users/{userId}/parkings/history
```

Order:

```text
startedAt DESC
```

Each history item should include:

```text
parkingId
vehicle
zone
city
startedAt
endedAt
amount
parkingStatus
paymentStatus
paidAt
```

---

# 24. Error Handling

Implement:

```text
GlobalExceptionHandler
```

Suggested errors:

| Scenario | Status |
|---|---:|
| User not found | 404 |
| Vehicle not found | 404 |
| City not found | 404 |
| Parking zone not found | 404 |
| Parking session not found | 404 |
| Vehicle does not belong to user | 403 or 400 |
| Parking zone inactive | 400 |
| Invalid top-up amount | 400 |
| Attempt to stop completed parking | 409 |
| Vehicle already has active parking | 409 |
| Attempt to pay active parking | 400 |
| Parking already paid | 409 |
| Insufficient account balance | 409 |

Example:

```json
{
  "message": "Insufficient account balance"
}
```

Do not build an unnecessarily complex error framework.

---

# 25. Database

Use PostgreSQL.

Flyway should be responsible for the database schema.

Suggested migrations:

```text
backend/src/main/resources/db/migration/

V1__create_schema.sql
V2__seed_demo_data.sql
```

Prefer:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

Do not rely on Hibernate to create the schema.

---

# 26. Database Tables

Expected tables:

```text
users
vehicles
cities
parking_zones
parking_sessions
payments
```

Possible schema:

```text
users
-----
id
name
balance

vehicles
--------
id
plate_number
user_id

cities
------
id
name

parking_zones
-------------
id
name
city_id
price_per_hour
active

parking_sessions
----------------
id
user_id
vehicle_id
zone_id
started_at
ended_at
amount
status

payments
--------
id
parking_session_id
amount
status
paid_at
```

---

# 27. Important Database Constraints

Where practical, add constraints for:

```text
users.balance >= 0

parking_zones.price_per_hour >= 0

payments.amount >= 0

vehicles.plate_number UNIQUE

payments.parking_session_id UNIQUE
```

Application-level validation remains necessary.

If practical with PostgreSQL, preventing duplicate active parking for the same vehicle can also be protected with a partial unique index:

```sql
CREATE UNIQUE INDEX ...
ON parking_sessions(vehicle_id)
WHERE status = 'ACTIVE';
```

This is optional but a strong additional safeguard.

The service layer must still validate the business rule.

---

# 28. Seed Data

Use at least two users.

Example:

```text
User 1
------
Alex Johnson
Balance: 20.00 EUR

Vehicles:
CA1234AB
CB5678CD
```

```text
User 2
------
Maria Smith
Balance: 10.00 EUR

Vehicles:
PB1234CD
```

Cities:

```text
Sofia
Plovdiv
```

Zones:

```text
Sofia

Blue Zone
2.00 EUR/hour
active = true

Green Zone
1.00 EUR/hour
active = true
```

```text
Plovdiv

Blue Zone
1.50 EUR/hour
active = true

Green Zone
1.00 EUR/hour
active = true
```

Optionally include one inactive zone to demonstrate validation:

```text
Plovdiv

Red Zone
3.00 EUR/hour
active = false
```

---

# 29. React Application

Keep the frontend to a single main page.

Suggested layout:

```text
Parking Management
────────────────────────────────

Current User
[ Alex Johnson ▼ ]

Balance
€20.00

Top Up
[ 10.00 ] [ Add Funds ]

────────────────────────────────

Start Parking

Vehicle
[ CA1234AB ▼ ]

City
[ Sofia ▼ ]

Parking Zone
[ Blue Zone — €2.00/hour ▼ ]

[ Start Parking ]

────────────────────────────────

Active Parking

CA1234AB
Sofia / Blue Zone
Started: 10:15

[ Stop Parking ]

────────────────────────────────

Completed / Unpaid Parking

Vehicle: CA1234AB
Amount: €4.00
Payment: UNPAID

Balance: €20.00

[ Pay €4.00 ]

────────────────────────────────

Parking History

Vehicle | City | Zone | Start | End | Amount | Payment
```

---

# 30. Frontend State

Keep state simple.

Use:

```text
useState
useEffect
fetch
```

Potential state:

```text
users
selectedUser

balance

vehicles
selectedVehicle

cities
selectedCity

zones
selectedZone

activeParkings

history

loading
error
```

Avoid Redux.

---

# 31. Frontend Behaviour

## On User Change

Reload:

```text
user balance
vehicles
active parking sessions
parking history
```

Reset:

```text
selected vehicle
```

## On City Change

Reload:

```text
parking zones for selected city
```

Reset:

```text
selected parking zone
```

## On Top-Up

After success:

```text
refresh user balance
clear top-up input
show success state/message
```

## On Start Parking

After success:

```text
refresh active parking
```

## On Stop Parking

After success:

```text
refresh active parking
refresh parking history
```

The completed unpaid parking should expose a Pay button.

## On Payment

After success:

```text
refresh balance
refresh history
refresh relevant parking state
```

---

# 32. Docker Architecture

Project structure:

```text
parking-management/
│
├── backend/
│   ├── Dockerfile
│   └── ...
│
├── frontend/
│   ├── Dockerfile
│   ├── nginx.conf
│   └── ...
│
├── docker-compose.yml
└── README.md
```

Services:

```text
frontend
backend
postgres
```

Architecture:

```text
Browser
   |
   v
localhost:3000
   |
   v
Nginx
   |
   | /api
   v
Spring Boot :8080
   |
   v
PostgreSQL :5432
```

React should be able to call:

```javascript
fetch("/api/users")
```

instead of hardcoding backend hostnames.

---

# 33. Docker Compose

Expected services:

```text
postgres
backend
frontend
```

PostgreSQL example configuration:

```text
POSTGRES_DB=parking
POSTGRES_USER=parking
POSTGRES_PASSWORD=parking
```

Backend datasource:

```text
jdbc:postgresql://postgres:5432/parking
```

Prefer adding a PostgreSQL health check.

Backend should wait for PostgreSQL readiness where practical.

---

# 34. Testing Strategy

Do not optimize for maximum code coverage.

Focus on business-critical behaviour.

---

# 35. Pricing Tests

Required high-value cases:

```text
pricing_oneMinute_chargesOneHour

pricing_59Minutes_chargesOneHour

pricing_exactlyOneHour_chargesOneHour

pricing_oneHourAndOneSecond_chargesTwoHours

pricing_61Minutes_chargesTwoHours

pricing_121Minutes_chargesThreeHours
```

---

# 36. Parking Tests

Recommended:

```text
startParking_success

startParking_vehicleDoesNotBelongToUser_fails

startParking_inactiveZone_fails

startParking_vehicleAlreadyHasActiveParking_fails

differentVehicles_canHaveActiveParkingAtSameTime

stopParking_success

stopParking_calculatesAndStoresAmount

stopParking_alreadyCompleted_fails
```

---

# 37. Top-Up Tests

Recommended:

```text
topUp_validAmount_increasesBalance

topUp_zeroAmount_fails

topUp_negativeAmount_fails
```

DTO validation can cover excessive decimal places.

---

# 38. Payment Tests

Recommended:

```text
payment_success

payment_deductsCorrectBalance

payment_insufficientBalance_fails

payment_insufficientBalance_doesNotModifyBalance

payment_duplicate_fails

payment_forActiveParking_fails
```

A particularly important test:

```text
payment_success_isAtomic
```

The payment and balance deduction must behave as one transaction.

---

# 39. Integration Tests

Integration tests are optional if time is limited.

Priority should remain on service-level business tests.

If time permits, add one end-to-end backend integration test for:

```text
start
-> stop
-> pay
-> history
```

Do not introduce Testcontainers if it creates unnecessary setup overhead.

---

# 40. README

The repository should contain:

```text
README.md
```

Recommended structure:

```text
# Parking Management System

## Overview

## Features

## Tech Stack

## Architecture

## Domain Model

## Running the Application

## Database and Migrations

## API

## Business Rules

## Pricing

## Account Balance and Payments

## Testing

## Assumptions and Trade-offs
```

---

# 41. README — Important Assumptions

Document explicitly:

> Authentication is intentionally outside the scope of this prototype. A user selector is provided so ownership and user-specific behaviour can still be demonstrated.

> The application contains multiple predefined demo users.

> Account balances use fictional money.

> Top-up is simulated by entering a valid monetary value.

> Real payment providers are intentionally outside the scope of the prototype.

> Payments are made from the fictional user balance.

> The wallet/top-up functionality is an intentional extension of the original assignment to provide a meaningful implementation of the required payment flow.

> The current pricing implementation uses hourly pricing based on the provided examples.

> Pricing logic is isolated so different charging strategies can be introduced in the future.

> The application includes multiple cities because multi-city usage is part of the business context.

---

# 42. Development Order

Follow approximately this sequence.

## Phase 1 — Bootstrap

1. Create backend project.
2. Create React frontend.
3. Create root Docker Compose structure.
4. Verify backend builds.
5. Verify frontend builds.

## Phase 2 — Database

6. Configure PostgreSQL.
7. Configure Flyway.
8. Create initial migration.
9. Add demo data.

## Phase 3 — Domain

10. Implement `User`.
11. Implement `Vehicle`.
12. Implement `City`.
13. Implement `ParkingZone`.
14. Implement `ParkingSession`.
15. Implement `Payment`.
16. Implement enums.

## Phase 4 — Persistence

17. Create repositories.
18. Add required repository queries.
19. Verify application starts with `ddl-auto=validate`.

## Phase 5 — Core Business Logic

20. Implement `PricingService`.
21. Implement top-up logic.
22. Implement `ParkingService.startParking()`.
23. Implement active parking retrieval.
24. Implement `ParkingService.stopParking()`.
25. Implement `PaymentService.pay()`.
26. Implement history retrieval.

This is the most important phase.

## Phase 6 — API

27. Create DTOs.
28. Create user endpoints.
29. Create top-up endpoint.
30. Create vehicle endpoint.
31. Create city endpoint.
32. Create zone endpoint.
33. Create parking endpoints.
34. Create payment endpoint.
35. Add `GlobalExceptionHandler`.

## Phase 7 — Manual Backend Verification

Verify manually:

```text
GET users
GET vehicles
GET cities
GET zones

top up account

start parking
get active parking
stop parking

attempt payment with enough balance
attempt payment with insufficient balance

get history
```

## Phase 8 — Frontend

36. Create main application layout.
37. Add user selector.
38. Add balance display.
39. Add top-up form.
40. Add vehicle selector.
41. Add city selector.
42. Add zone selector.
43. Add start parking.
44. Add active parking section.
45. Add stop parking.
46. Add unpaid/completed parking.
47. Add payment button.
48. Add history table.
49. Add loading/error states.

## Phase 9 — Docker

50. Add backend Dockerfile.
51. Add frontend Dockerfile.
52. Add Nginx configuration.
53. Complete Docker Compose.
54. Verify clean startup.

## Phase 10 — Tests

55. Add pricing tests.
56. Add parking business-rule tests.
57. Add top-up tests.
58. Add payment tests.
59. Add any high-value integration test if time allows.

## Phase 11 — Documentation

60. Write README.
61. Document architecture.
62. Document database migrations.
63. Document assumptions.
64. Document wallet/top-up extension.
65. Document startup instructions.

---

# 43. Final Smoke Test

Start from a clean environment:

```bash
docker compose down -v
docker compose up --build
```

Verify the following.

## Users

```text
At least two users appear.
Switching users works.
Balances are different.
Vehicles change with the selected user.
```

## Top-Up

```text
Enter valid amount.
Balance increases.

Enter zero.
Rejected.

Enter negative amount.
Rejected.
```

## Cities

```text
At least Sofia and Plovdiv appear.

Changing city changes available zones.
```

## Parking

```text
Select user.

Select one of their vehicles.

Select city.

Select zone.

Start parking.

Parking appears as ACTIVE.
```

Try starting another parking for the same vehicle:

```text
Rejected.
```

Use another vehicle:

```text
Second parking can be started.
```

## Stop Parking

Stop the first parking.

Verify:

```text
endedAt exists
amount is calculated
status = COMPLETED
payment = UNPAID
```

## Insufficient Balance

If required, switch to or configure a user with insufficient funds.

Attempt payment.

Verify:

```text
payment rejected
balance unchanged
parking remains UNPAID
```

## Top Up and Pay

Top up the account.

Pay again.

Verify:

```text
payment succeeds
balance decreases
payment = PAID
```

## History

Verify the completed parking appears in history with:

```text
vehicle
city
zone
start
end
amount
payment status
```

## Persistence

Restart containers without deleting the volume.

Verify:

```text
balances persisted
parking history persisted
payments persisted
```

---

# 44. Definition of Done

The application is complete when the following flow works:

```text
docker compose up --build

        |
        v

Select User

        |
        v

View Balance

        |
        v

Top Up Balance

        |
        v

Select Vehicle

        |
        v

Select City

        |
        v

Select Zone

        |
        v

Start Parking

        |
        v

View Active Parking

        |
        v

Stop Parking

        |
        v

Amount Calculated

        |
        v

Pay From Balance

        |
        v

Payment = PAID

        |
        v

Updated Balance

        |
        v

Parking Appears In History
```

---

# 45. Submission Checklist

- [ ] Source code stored in Git repository
- [ ] Java / Spring Boot backend
- [ ] React frontend
- [ ] PostgreSQL persistence
- [ ] Flyway migrations
- [ ] Docker Compose
- [ ] Multiple users
- [ ] Multiple vehicles per user
- [ ] At least two cities
- [ ] Multiple parking zones
- [ ] User balance
- [ ] Top-up functionality
- [ ] Start parking
- [ ] Active parking
- [ ] Per-vehicle active parking restriction
- [ ] Stop parking
- [ ] Correct pricing
- [ ] Payment from balance
- [ ] Insufficient balance handling
- [ ] Payment status
- [ ] Parking history
- [ ] Automated tests
- [ ] README
- [ ] Architecture explanation
- [ ] Database setup / migration instructions
- [ ] `docker compose up --build` works

---

# 46. Priority If Time Becomes Limited

If time becomes limited, prioritize:

1. Correct parking lifecycle
2. Correct ownership rules
3. Correct per-vehicle active parking restriction
4. Correct pricing
5. PostgreSQL persistence
6. Payment from account balance
7. Top-up
8. Multiple cities
9. Docker Compose
10. Critical automated tests
11. README
12. UI polish

Do not sacrifice functional correctness for:

- Styling
- Complex abstractions
- Design patterns
- Extra infrastructure
- Unnecessary CRUD
- Production features