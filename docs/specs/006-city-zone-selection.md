# 006 — City and Zone Selection

Status: Ready — not implemented.

## Purpose

Let a person select where to park from multiple cities, each with its own active zones and hourly prices.

## Scope

Includes read-only city and active-zone catalogs and dependent frontend selectors. Catalog CRUD, real municipal tariff synchronization, maps, geolocation, and operating schedules are excluded.

## Business rules

1. Cities are distinct persistent entities. A zone belongs to exactly one city.
2. The same zone name can occur in different cities with different prices.
3. List cities ordered by name and ID; list only active zones of the requested city, ordered by name and ID.
4. A nonexistent city returns `CITY_NOT_FOUND`. An existing city without active zones returns an empty array.
5. Prices are fictional EUR amounts, and all zones use started-hour pricing in this version.
6. Start must validate zone existence and activity even if the UI obtained it from the active-zone catalog.
7. Start derives city from the chosen zone. The API does not accept a separate city/zone combination that could disagree.
8. An inactive zone is unavailable for new sessions. Previously started sessions remain stoppable and payable.

## User flow

Load the city catalog at page startup. The person chooses a city; the UI clears the previous zone selection and fetches that city's active zones. They choose a zone after seeing its hourly rate. Start remains disabled until a valid user, vehicle, city, and zone are selected.

## API and database changes

Use `GET /api/cities` and `GET /api/cities/{cityId}/zones`, defined in [the catalog API](../api.md#cities-and-zones). Store cities and zones according to [the database design](../database.md).

Seed Sofia Blue/Green zones at `2.00`/`1.00` and Plovdiv Blue/Green zones at `1.50`/`1.00`, plus an inactive Plovdiv Red Zone at `3.00`. Exact identities live in [demo data](../database.md#demo-data).

## UI behavior

Use labeled City and Parking Zone selectors. Show city-specific prices with the zone names. Clear old zone options immediately on a city change; cancel or ignore stale catalog responses. Show a readable empty or failed-load state. User switching can retain the shared city/zone context.

## Acceptance criteria

- [ ] Sofia and Plovdiv appear as separate city choices.
- [ ] Sofia exposes two active zones with rates `2.00` and `1.00`.
- [ ] Plovdiv exposes two active zones with rates `1.50` and `1.00`; its inactive Red Zone is absent.
- [ ] Starting directly with the inactive Red Zone ID returns `ZONE_INACTIVE`.
- [ ] Changing city clears the selected zone before loading new choices.
- [ ] A delayed zone response for the previous city cannot replace the current city's zones.
- [ ] An existing city with no active zones disables starting and explains why.
- [ ] A missing city returns `404 CITY_NOT_FOUND`.
- [ ] A started session records the selected zone and its corresponding city and price snapshot.
- [ ] Changing selected city does not change or hide existing active sessions in other cities.

## Verification

Use catalog repository/API tests for filtering, ordering, and missing parents. Include inactive-zone start rejection in lifecycle tests. Verify dependent-selector resets and delayed responses in the UI.

Verification performed: none; implementation is pending.

## Open questions

None.
