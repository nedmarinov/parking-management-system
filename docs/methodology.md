# Development Methodology

This project uses **specification-driven, AI-assisted development**. We describe the intended behavior and acceptance criteria before implementing substantial features, use AI to help develop and verify the solution, and keep project context in the repository.

This is a lightweight workflow for a working prototype. Its priorities are correct business behavior, clear decisions, useful tests, and a runnable application.

## Current stage

The original brief has been analyzed and expanded into an implementation plan, shared technical contracts, and six feature specifications. The documentation is stored in a local Git repository with an initial commit.

Application implementation has not started. The documents describe the intended system; their existence does not prove that the behavior works. The workflow below defines how we will move from those specifications to verified software.

## How work progresses

```text
Understand the requested outcome
              ↓
Write or update the feature specification
              ↓
Resolve important decisions and dependencies
              ↓
Implement a manageable increment with relevant tests
              ↓
Verify behavior against acceptance criteria
              ↓
Update documentation and record a Git checkpoint
```

This cycle repeats as features are delivered. A failed check leads back to implementation. A discovered requirement gap leads back to the specification and affected contracts.

### 1. Understand the outcome

Start with the user's request, the [project scope](PLAN.md#scope), and the existing implementation state. Identify the observable result, affected features, dependencies, and constraints.

Inspect relevant repository files before assuming how the application behaves. Existing code, tests, and documentation provide context; contradictions need to be resolved rather than silently carried into new work.

### 2. Specify substantial behavior changes

Use the [feature template](specs/_template.md) for a new substantial feature. Record its purpose, business rules, user flow, failure cases, and acceptance criteria. For a change to an existing feature, update its specification rather than creating a competing description.

Keep each shared contract in its designated document. A feature spec links to API shapes and database constraints instead of maintaining independent copies. The [document ownership table](README.md#document-ownership) identifies those sources.

Small fixes and cosmetic changes can use a task description. Add documentation when it captures a lasting rule or changes the public behavior.

### 3. Resolve decisions at the appropriate level

The human sets product direction, scope, and business priorities. AI helps identify gaps, explain options, propose designs, and carry out the requested work.

Discuss unresolved choices that materially change business behavior or scope. Routine implementation choices can proceed within the existing instructions and specification. This workflow does not require a separate approval after every file edit or technical step.

Record shared technical decisions in [architecture](architecture.md) and feature-specific rules in the relevant spec. Keep assumptions visible, including decisions that refine the original brief. A specification marked Ready means it is sufficiently defined for implementation; it does not mean every assumption was explicitly selected by the user.

### 4. Implement a manageable increment

Follow the dependencies and delivery sequence in the [implementation plan](PLAN.md). Bootstrap and persistence work establish the foundation; feature work then implements the specified behavior across the layers it needs.

Keep changes focused enough to review and verify. Complete a meaningful part of the feature, including relevant validation, persistence, errors, and UI behavior where applicable. The plan may deliver backend behavior before the frontend, so an individual increment can be complete while the overall feature remains In progress.

Write relevant tests alongside business logic. Choose simple designs that meet the current requirements and preserve the boundaries already established in the architecture.

### 5. Verify observable behavior

Check the result against the feature's acceptance criteria and the [testing strategy](testing.md). Use the kind of evidence required by the behavior:

| Behavior | Appropriate evidence |
| --- | --- |
| Pricing arithmetic and boundaries | Deterministic unit tests |
| Request validation and error responses | HTTP/controller tests |
| Database constraints, payment rollback, and concurrent updates | PostgreSQL integration tests |
| User switching, form feedback, and stale responses | Focused browser checks or UI tests |
| Startup and persistence across restarts | Docker Compose smoke test |
| Documentation-only changes | Link, example, and consistency checks |

Record checks actually performed and their outcomes. Keep failed or unperformed checks visible. A build confirms compilation; business guarantees require their relevant tests and observations.

### 6. Keep the repository accurate

Update affected specifications and shared contracts as implementation decisions change. Record verification in the feature's Verification section, update acceptance checkboxes, and maintain the delivery checklist.

Use coherent Git commits as checkpoints for the work being delivered. A useful commit explains the resulting change and includes related documentation or tests. Committing a specification records a design checkpoint; it does not mark its feature implemented.

When resuming work, read the current task, relevant specifications, Git status, and existing implementation before continuing. Repository documents preserve context across sessions, while code and test results establish the actual delivered state.

## Feature status

| Status | Meaning |
| --- | --- |
| Draft | Requirements or consequential decisions are still being worked out |
| Ready | The feature is sufficiently specified to begin implementation |
| In progress | Implementation or required verification is incomplete |
| Implemented | The specified behavior is delivered and its acceptance criteria have passed |

Keep the status in the feature spec and the [feature index](README.md#feature-specifications) consistent. If an implemented feature gains new requirements, retain the evidence for existing behavior and identify the pending work explicitly.

## Example: parking payment

The [payment spec](specs/004-parking-payments.md) defines the outcome: a user explicitly pays for completed parking, the balance decreases, and one payment record is created.

The API document defines the request and response. Architecture defines the transaction and locking policy. Database design supplies the payment uniqueness constraint. Implementation connects those contracts in the service, persistence layer, endpoint, and UI.

Verification checks sufficient funds, insufficient funds, ownership, duplicate requests, rollback, and concurrent payment attempts. The feature becomes Implemented when those requirements and the user-facing flow are verified, with the results recorded in the spec.

## Relationship to AI-DLC

Our workflow shares principles with AWS's AI-Driven Development Life Cycle: AI contributes to planning and execution, people guide important decisions, and repository artifacts preserve context across the work. AWS describes these principles across requirements, construction, and operations in its [AI-DLC overview](https://aws.amazon.com/blogs/devops/ai-driven-development-life-cycle/).

This project has not formally adopted the AWS AI-DLC workflow, its stage structure, or its tooling. The current description is specification-driven, AI-assisted development. The folder layout supports that process; the repeated practice of specifying, implementing, verifying, and maintaining context makes it useful.

Production deployment, observability, and CI/CD remain outside the prototype scope. Delivery here means the verified local Docker Compose application described in the plan.
