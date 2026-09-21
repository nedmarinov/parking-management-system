# NNN — Feature Name

Status: Draft

## Purpose

Describe the user problem and the observable outcome in one or two paragraphs.

## Scope

State what this feature adds and the boundaries needed to keep the work focused. Link related features and dependencies.

## Business rules

Number the rules that implementation and tests must enforce. Include ownership, validation, state transitions, money/time behavior, and duplicate/concurrent request behavior where applicable. Link shared decisions instead of copying them.

## User flow

Describe the main sequence and meaningful empty, failure, and recovery paths.

## API and database changes

Link the authoritative API and database sections. Identify new or changed endpoints, fields, constraints, and migrations. Write “None” for an area that does not change.

## UI behavior

Describe controls, loading state, errors, successful refreshes, and accessibility considerations that are specific to this feature.

## Acceptance criteria

Use observable cases, for example:

- [ ] Given a valid initial state, when the user takes the action, the expected persisted state and UI result occur.
- [ ] Given invalid input, the documented error is returned and existing state is unchanged.
- [ ] Ownership and duplicate/concurrent requests behave as specified.

## Verification

Specify relevant unit, API, database, or UI checks. When implemented, record the commands/scenarios actually run and their results. Do not mark planned tests as passed.

## Open questions

List decisions that materially affect implementation. Resolve required decisions before changing the status to Ready. Write “None” when resolved.
