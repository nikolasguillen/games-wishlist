# Feature Specification: KMP Architecture Audit Before Merge

**Feature Branch**: `011-kmp-architecture-audit`

**Created**: 2026-10-09

**Status**: Draft

**Input**: User description: "analyze the new kmp structure of this project after the recent migration and check if there are architectural issues that should be fixed before merging the branch into develop."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Merge-readiness verdict (Priority: P1)

The owner has finished the multiplatform migration on `010-kmp-migration` (about 660 files changed against
`develop`) and wants to know whether the branch can be merged as it stands. They need an audit of the
resulting structure that finishes with a clear verdict: every architectural issue found, each one classified
as either a blocker that must be fixed before the merge or a follow-up that can safely wait.

**Why this priority**: The merge is the decision this work exists to inform. A list of observations without a
blocker/non-blocker split does not let the owner decide, and merging a structural flaw into `develop` is the
costly outcome to avoid, because every later branch inherits it.

**Independent Test**: Can be fully tested by reading the audit report alone: it names every finding, gives each
a severity and a blocker/follow-up classification, and ends in an explicit "merge" or "do not merge yet" verdict
that follows from those classifications.

**Acceptance Scenarios**:

1. **Given** the migrated branch, **When** the audit is complete, **Then** the report lists each issue with its
   location, the rule or intent it breaks, its impact, and a blocker or follow-up classification.
2. **Given** the audit found at least one blocker, **When** the owner reads the verdict, **Then** it says the
   branch should not merge yet and lists exactly which findings must be fixed first.
3. **Given** the audit found no blockers, **When** the owner reads the verdict, **Then** it says the branch is
   safe to merge and lists the follow-ups that remain.
4. **Given** an issue is already recorded in the known-deviations list or the roadmap, **When** it appears in the
   audit, **Then** it is marked as already known rather than reported as new.

---

### User Story 2 - Structure matches the documented rules (Priority: P1)

The owner wants confidence that the migrated module structure still obeys the project's own architectural
rules: module dependency boundaries, what shared code may and may not reference, how platform capabilities
are exposed, and where navigation and dependency wiring live. The audit checks the actual source and build
configuration against those rules and reports every divergence.

**Why this priority**: The rules are declared load-bearing, and a migration of this size is exactly when they
erode silently. Checking them is the core of the audit; it is as important as the verdict itself.

**Independent Test**: Can be tested by taking each documented rule and confirming the audit records a pass or a
finding for it, with the evidence examined (the build declaration or source location checked).

**Acceptance Scenarios**:

1. **Given** the documented module-dependency rules, **When** the audit inspects every module's declared
   dependencies, **Then** each allowed and forbidden edge is confirmed or reported as a violation.
2. **Given** the rule that shared code must not use platform-specific APIs, **When** the audit inspects shared
   sources, **Then** any platform-specific reference is reported with its location.
3. **Given** the rule that every platform-specific source belongs to a documented capability contract, **When**
   the audit compares platform sources with the contract list, **Then** any platform source with no matching
   contract, and any contract with a missing implementation on a platform, is reported.
4. **Given** the documented homes for navigation, dependency wiring and resources, **When** the audit searches
   for those concerns, **Then** any that live elsewhere are reported.

---

### User Story 3 - Two-platform soundness and migration leftovers (Priority: P2)

The owner wants to know that the structure holds up on both platforms and that the migration left nothing
behind: no dead or half-migrated modules, source folders or build settings, no platform-specific code sitting
in the wrong place, no duplicated logic across platforms that should be shared, and no documentation that still
describes the pre-migration project.

**Why this priority**: These issues are real but rarely break a build. They matter because they mislead the next
contributor and are cheap to fix now and costly once new work builds on them, which makes them second after the
rule checks.

**Independent Test**: Can be tested by listing every leftover category (unused folders, stale settings,
duplicated platform logic, outdated documentation) and confirming each was searched and either found clean or
reported.

**Acceptance Scenarios**:

1. **Given** the migrated tree, **When** the audit looks for pre-migration remnants, **Then** unused source
   folders, orphaned build settings and stale files are listed.
2. **Given** logic implemented separately on each platform, **When** the audit compares the implementations,
   **Then** logic that could live once in shared code is flagged, and logic that must differ per platform is
   confirmed as legitimate.
3. **Given** the project's instruction and architecture documents, **When** the audit compares them with the
   migrated structure, **Then** every statement that no longer matches reality is listed.

---

### User Story 4 - Verification that the build and tests hold (Priority: P2)

Before issuing the verdict, the audit confirms the branch is healthy where it can be measured: the Android app
builds, the unit-test suites pass, and the iOS-only checks are either run or explicitly reported as not run.

**Why this priority**: A structural verdict resting on a branch that does not build is meaningless, and the
project requires Android to build and pass its tests at every commit. This is a gate on the verdict.

**Independent Test**: Can be tested by comparing the audit's verification section with the project's documented
verification commands: each is either recorded with its result or marked as not run with the reason.

**Acceptance Scenarios**:

1. **Given** the branch, **When** the documented Android build and full unit-test run are executed, **Then**
   their results are recorded in the report.
2. **Given** a check that only the Mac with an iOS simulator can verify, **When** it cannot be run in the
   audit environment, **Then** the report says it was not run and why, and does not assume it passes.
3. **Given** a failing build or test, **When** it is found, **Then** it is reported as a blocker.

---

### Edge Cases

- A finding is a deliberate deviation recorded in the known-deviations list, not a regression: it must be
  labelled as known and must not be reclassified as a blocker unless the migration made it worse.
- A rule in the documentation conflicts with the source: the source wins, and the audit reports the stale rule
  as a documentation finding rather than the code as a violation.
- A forbidden dependency exists but is currently unreachable or unused: it is still reported, with that
  context noted, because the build graph is what the rule governs.
- An issue can only be confirmed on a Mac with an iOS simulator and the audit runs elsewhere: it is reported
  as unverified, never as passing.
- The migration branch changes between the start of the audit and its end: findings state the commit they were
  checked against.
- A suspected issue turns out to be intentional per the migration's own plan or research notes: it is
  recorded as intentional, citing that source, so it is not rediscovered later.
- A candidate fix would be large or risky: it is classified as a follow-up with a rationale if it is not
  needed for the merge, rather than being forced into the pre-merge set.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The audit MUST examine the whole migrated project: every module, the platform shells, the build
  configuration shared across modules, and the project documentation.
- **FR-002**: The audit MUST check every documented module-dependency rule against the modules' actual declared
  dependencies and report each violation.
- **FR-003**: The audit MUST check that shared (platform-independent) code contains no platform-specific API
  references and that the model layer stays free of platform and UI-framework dependencies.
- **FR-004**: The audit MUST verify that every platform-specific source corresponds to a documented capability
  contract, and that every contract has an implementation for each platform that needs one or a documented
  reason it does not.
- **FR-005**: The audit MUST verify that navigation, dependency wiring and the application entry points live
  only in their documented modules.
- **FR-006**: The audit MUST identify pre-migration leftovers: unused source folders, orphaned or duplicated
  build settings, stale files and dead modules.
- **FR-007**: The audit MUST identify platform-specific logic that is duplicated across platforms and could be
  shared, and distinguish it from logic that legitimately differs.
- **FR-008**: The audit MUST compare the project's instruction, architecture, roadmap and known-deviation
  documents with the migrated reality and list every statement that is now wrong or missing.
- **FR-009**: The audit MUST run the documented Android build and full unit-test commands and record their
  results, and MUST mark any check it could not run, including those requiring an iOS simulator, as not run
  with the reason.
- **FR-010**: Every finding MUST record: a short title, the location, the rule or intent it breaks, the impact
  if left unfixed, a severity, a blocker or follow-up classification, the evidence examined, and the commit it
  was checked against.
- **FR-011**: Every blocker MUST include a recommended fix with an estimated size (small, medium or large) and
  any decision the owner has to make; fixes that would be hard to undo MUST be flagged for the owner's choice
  rather than assumed.
- **FR-012**: Findings that duplicate an entry in the known-deviations list or the roadmap MUST be marked as
  known; findings that the migration plan records as intentional MUST be marked as intentional with the source
  cited.
- **FR-013**: The audit MUST end with an explicit merge verdict ("merge" or "do not merge yet") that follows
  directly from the blocker classifications, with the blockers listed when the verdict is "do not merge yet".
- **FR-014**: The audit MUST NOT change source code, build configuration or documentation as a side effect.
  Applying the fixes is a separate, owner-approved step, and the blocker fixes it produces MUST keep the
  Android build and tests passing at every step.
- **FR-015**: The report MUST be written so a reader who did not run the audit can reproduce each finding from
  the evidence given.

### Key Entities

- **Finding**: One architectural issue. Has a title, location, violated rule or intent, impact, severity,
  classification (blocker, follow-up, known or intentional), evidence, recommended fix, fix size and the commit
  it was observed on.
- **Rule**: A documented architectural constraint (a dependency boundary, a shared-code restriction, a
  capability-contract requirement, a placement rule). Each rule is checked and ends as pass, violated or not
  verifiable.
- **Capability contract**: A platform capability that shared code owns and each platform implements. Linked to
  its implementations and to the platform sources that realise it.
- **Verification result**: The outcome of one build, test or manual check: passed, failed or not run, with the
  reason when not run.
- **Verdict**: The final merge recommendation, derived from the findings and verification results, naming the
  blockers when the answer is not to merge.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of the documented architectural rules have a recorded outcome (pass, violated or not
  verifiable) with the evidence examined.
- **SC-002**: 100% of findings carry all the fields FR-010 requires, and every blocker carries a recommended
  fix and size.
- **SC-003**: The owner can reach a merge decision from the report in a single reading, without having to
  re-inspect the code to learn which issues block the merge.
- **SC-004**: After the blockers are fixed, a repeat of the audit's rule checks reports zero blocker-level
  violations, the Android build succeeds and the full unit-test run passes.
- **SC-005**: No check is reported as passing without having been run; every check that could not run is
  listed as not run with a reason.
- **SC-006**: The audit changes no project files other than its own report artifacts.
- **SC-007**: Every documentation statement found to be stale is listed, so that after the follow-up edits no
  documented rule contradicts the source.

## Assumptions

- The audit covers the state of `010-kmp-migration` at the time it starts, and findings record the commit they
  were checked against. Changes made to that branch afterwards are out of scope.
- "Architectural issues" means structural problems in module boundaries, shared-versus-platform code placement,
  platform capability contracts, dependency wiring, build configuration and documentation accuracy. Feature
  behaviour bugs, visual polish and performance tuning are out of scope unless they trace back to a structural
  cause.
- The project's own documents are the standard the structure is judged against: the root and per-directory
  instruction files, the project constitution, and the migration's plan, research and platform-contract notes.
  Where a document and the source disagree, the source wins and the document is reported as stale.
- Debt already listed in the known-deviations list (no CI, untested iOS 16 floor, split Material 3 versions,
  Room destructive migration, and similar) is not re-argued; it is marked as known unless the migration made it
  worse.
- A blocker is an issue that breaks the Android build or tests, violates a non-negotiable rule, or would be
  materially more expensive to fix once the branch is on `develop` and other work depends on it. Everything
  else is a follow-up.
- The audit runs on a Mac, so the iOS framework build and iOS simulator tests can be attempted; if the
  environment cannot run them, they are reported as not run.
- The audit is read-only. Fixing blockers happens afterwards, in separate owner-approved work that follows the
  project's commit conventions.
- Scope is limited to the existing platforms, Android and iOS; no new platform is considered.
