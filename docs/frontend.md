# Frontend Behavior

Status: Ready — target React application behavior.

## Structure

Use one main page with React, Vite, JavaScript/JSX, native `fetch`, and shadcn/ui. No router, Redux, WebSockets, or global state library is required.

```text
frontend/src/
├── api/
│   ├── client.js
│   ├── users.js
│   ├── catalog.js
│   └── parking.js
├── components/
│   └── ui/
├── features/
│   ├── users/
│   ├── wallet/
│   └── parking/
├── hooks/
├── lib/
│   └── format.js
├── App.jsx
└── main.jsx
```

These are responsibility boundaries; create individual files when needed. Keep city and zone controls with the parking form. Shared request handling and formatting should have one implementation.

## Page sections

1. **Current user:** labeled selector populated from demo users.
2. **Balance and top-up:** EUR balance, amount input, Add Funds action, and local feedback.
3. **Start parking:** vehicle, city, and zone selectors; hourly price; Start Parking action.
4. **Active parking:** one card or row per active vehicle with city, zone, start time, captured hourly rate, and Stop Parking action.
5. **Completed / unpaid parking:** completed unpaid items with exact stored amount and an explicit Pay action.
6. **Parking history:** all completed items with vehicle, city, zone, start, end, captured rate, amount, payment status, and paid time.

Show that balances are fictional and pricing is per started hour with a one-hour minimum. Display paid/unpaid status as text as well as visual styling. Active sessions should be labeled active without an unpaid badge.

## State ownership

The main page or a small shared hook owns the selected user and loaded user-specific data. The parking form owns vehicle/city/zone selections. Form inputs and feedback stay close to their feature component.

Prefer a single `currentUser` object containing the balance instead of maintaining duplicate balance values. Store `history` once and derive unpaid items from it. Derive parked vehicle IDs from active sessions. All authoritative amounts and status values come from the server.

Keep loading and mutation state specific enough that users can see which section is busy. Avoid one global boolean that incorrectly marks unrelated requests complete.

## Initial load

- Fetch users and cities independently.
- Select the first returned user, then load user details, vehicles, active sessions, and history.
- Leave vehicle and city unselected until chosen. Zone remains unselected until a city's zones load and the user chooses one.
- If the user list is empty, show an empty-state message and disable user-specific actions.
- If a catalog or user-data request fails, show a readable error with a retry control. Do not show a failed balance load as a zero balance.

## Selection changes

### User change

Clear the previous user's details, vehicles, active sessions, history, top-up input, and feature feedback immediately. Reset the selected vehicle. Reload details, vehicles, active sessions, and history for the new user. Retain the selected city/zone because they are shared catalog context, provided they remain valid.

Cancel obsolete read requests with `AbortController` and also guard response application by the selected user/request generation. Responses from a prior selection must never populate the new user's page. A loading state is preferable to displaying another user's previous data.

### City change

Immediately clear the selected zone and its options. Load active zones for the new city. Cancel or ignore obsolete requests. A slow response for the previous city must not replace the new city's options. An existing city with no active zones shows an empty state and disables Start Parking.

## Actions and refreshes

| Action | Submit | After success |
| --- | --- | --- |
| Top up | Trimmed decimal input string | Refresh current user; clear amount; show success |
| Start | Current user, vehicle, and zone IDs | Refresh active sessions; clear selected vehicle; show success |
| Stop | Session ID and current user ID | Refresh active sessions and history; reveal unpaid item |
| Pay | Session ID and current user ID | Refresh current user and history; remove item from unpaid section |

If independent refreshes are needed, run them together and handle each failure. Distinguish a successful mutation followed by a failed refresh from a rejected mutation. For example, show “Payment succeeded; history could not be refreshed” and provide a read retry, without inviting another payment.

Serialize mutation submissions in the UI while one is pending: disable mutation buttons and user/city/vehicle/zone selectors until it completes. Keep the captured user and request context for all result handling. Guards must still protect against stale results after unmount or a later selection change. Server-side rules remain authoritative for other tabs and direct API calls.

After a known conflict such as already stopped, already paid, or already parked, show the server message and refresh the relevant state. After an ambiguous mutation network failure, do not automatically resubmit; refresh state and explain that the user should review it before another action.

## Form and money behavior

- Use a labeled text input with decimal input mode for top-up. Keep the amount as a string.
- Validate the [API decimal format](api.md#top-up) before submission and render a specific message for missing, zero, negative, malformed, or over-precise amounts.
- Do not silently round user input.
- Display server decimal strings with exactly two digits and a EUR label or symbol. Formatting can split/group the string; do not convert it to a floating-point value for calculations.
- Do not calculate charges or balances in the browser. Show an active session's captured hourly rate; a live accrued-charge estimate is outside scope.
- Allow a Pay action on unpaid sessions even if funds may be insufficient, so the server returns the authoritative result. Keep the item visible and expose top-up after rejection.
- Disable vehicles already present in the active list, with an explanation. Backend validation still handles races and direct requests.

## Dates and display

Parse the explicit UTC API timestamps and format them in the browser's local timezone. Show a small “Times shown in your local timezone” note. Preserve full date context in history, where sessions can cross midnight. Use an em dash or equivalent for non-applicable values rather than rendering `null`.

## API client behavior

Centralize relative `/api` URL construction, JSON headers, response parsing, and errors. `fetch` resolves on HTTP errors, so inspect `response.ok`. Prefer the API's error message when its JSON error shape is present. Handle network failures, aborted requests, and non-JSON proxy errors separately. Avoid automatically retrying POST requests.

## Loading, empty, error, and accessible states

- Use persistent labels for all inputs and accessible names for action buttons.
- Support keyboard selection and activation; preserve visible focus indication.
- Associate field errors with the corresponding input and announce action feedback with an appropriate live region.
- Indicate busy actions with text such as “Paying…” and disable duplicate submissions.
- Provide distinct empty messages for no vehicles, no zones, no active parking, no unpaid parking, and no history.
- Make the page usable on a narrow viewport; permit horizontal scrolling for a wide history table or use a compact card presentation.
- Display backend messages near the relevant action without clearing unrelated completed data.

The [feature specs](README.md#feature-specifications) own acceptance criteria. The [testing guide](testing.md) includes manual race, failure, and accessibility checks.
