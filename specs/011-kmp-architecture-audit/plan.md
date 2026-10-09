# Implementation Plan: KMP Architecture Audit Before Merge

**Branch**: `011-kmp-architecture-audit` | **Date**: 2026-10-09 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/011-kmp-architecture-audit/spec.md`

## Summary

This plan audits the multiplatform structure that `010-kmp-migration` produced against the project's own rules,
before that branch merges into `develop`. The deliverable is one report, `audit-report.md`. It holds:
- a merge verdict;
- a ledger of every rule checked;
- a finding per issue, classified as a blocker or a follow-up;
- the build and test results on both platforms.

The audit is **read-only**: no source, build or documentation file outside this spec directory changes. Fixing
the blockers is a separate step the owner approves after reading the verdict.

**Approach** (research R1–R12):
1. Turn the CLAUDE.md files, the constitution and the 010 contracts into a numbered rule list.
2. Check each rule with a locator (grep, build files, convention plugins), then confirm with an authority (Gradle's
   resolved graph, the iOS compile, reading the source).
3. Map all 48 platform source files to the capability contracts.
4. Run the five documented verification commands.
5. Classify every finding with a fixed rubric, so the verdict follows mechanically.

## Technical Context

**Language/Version**: Kotlin 2.4.20 (the code under audit), Gradle 9.7.1 with JVM toolchain 21, and Swift 5 for
`iosApp/`. The audit itself is documentation, not code.

**Primary Dependencies**: None added. The tools used are the Gradle wrapper, `git`, `grep`, Xcode's `xcodebuild`,
and the code-review graph, which is rebuilt on the baseline before use (R11).

**Storage**: N/A. The only output is Markdown in `specs/011-kmp-architecture-audit/`.

**Testing**: The project's own commands are the verification:
- `./gradlew :androidApp:assembleDebug`
- `./gradlew test`, which includes `KoinGraphTest`
- `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64`
- `./gradlew :core:network:iosSimulatorArm64Test`
- the `xcodebuild` simulator build

There are no new tests: the audit adds no code.

**Target Platform**: The audit runs on macOS (darwin). The code under audit targets Android (minSdk 29) and
iOS 16+.

**Project Type**: Mobile app, Kotlin Multiplatform: 19 Gradle modules, `build-logic/` and the `iosApp/` Xcode project.

**Performance Goals**: N/A. SC-003 sets a readability goal instead: one reading to a decision.

**Constraints**:
- Read-only outside the spec directory (FR-014).
- Nothing is reported as passing without being run (SC-005).
- The source wins over the docs (R1).
- Only git-tracked state counts toward the merge (R6).

**Scale/Scope**:
- The diff is 659 files (+8 817 / −3 108) against `develop`.
- 48 platform source files to map.
- 8 instruction files plus the constitution, `docs/` and the 010 contracts to check for drift.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Effect on this plan | Status |
|---|---|---|
| I. Module boundaries | Checked by the audit (rules `R-DEP-*`). The plan adds no module and no edge | ✅ Pass |
| II. Typed errors | Checked by the audit only where the migration touched the boundary (`isConnectivityFailure`/`isTimeoutFailure` per platform). No change | ✅ Pass |
| III. UI renders, does not decide | Out of scope unless a structural cause is found (spec Assumptions). No change | ✅ Pass |
| IV. Reuse the shared layer | No component written | ✅ Pass |
| V. Verification is local | The plan uses only the documented commands. It invents no lint gate or CI (R10) | ✅ Pass |
| KMP constraints (Android green at every commit, no library swap, contracts for platform capabilities) | The audit makes no commits to code. Fixes afterwards must each keep Android green (quickstart §4) | ✅ Pass |
| Governance: hard-to-undo decisions go to the owner | Enforced through `owner_decision` on findings (FR-011) | ✅ Pass |
| Development workflow: `docs/tech-debt.md` items are not silently fixed | The audit marks them `known` and fixes nothing (FR-012, FR-014) | ✅ Pass |

**Post-design re-check** (after Phase 1): unchanged, all pass. The design adds only Markdown under
`specs/011-kmp-architecture-audit/`, with no source, dependency or module change. Complexity Tracking is not needed.

## Project Structure

### Documentation (this feature)

```text
specs/011-kmp-architecture-audit/
├── spec.md                 # Feature spec
├── plan.md                 # This file
├── research.md             # Phase 0: R1–R12 and the leads table
├── data-model.md           # Phase 1: Rule, Finding, Capability mapping, Verification result, Verdict
├── quickstart.md           # Phase 1: how to run the checks and validate the report
├── contracts/
│   └── audit-report.md     # Phase 1: layout of the deliverable
├── checklists/
│   └── requirements.md     # Spec quality checklist
├── work/                   # Per-area evidence notes written by parallel checks; the report is assembled from them
├── audit-report.md         # The deliverable (written during /speckit-implement)
└── tasks.md                # Phase 2 (/speckit-tasks; not created here)
```

### Source Code (repository root)

Nothing is created or modified. These are the areas the audit **reads**, by rule area:

```text
settings.gradle.kts, build.gradle.kts, gradle/libs.versions.toml     # R-BUILD, R-LEFT
build-logic/convention/src/main/kotlin/                              # R-DEP (feature edges), R-BUILD
androidApp/                                                          # R-PLACE (Android shell), R-DEP
shared/src/{commonMain,androidMain,iosMain}/                         # R-PLACE (nav, Koin, entry points), R-PURE
core/{common,model,network,database,data,domain,ui,designsystem,navigation}/src/{commonMain,androidMain,iosMain}/
core/ai/                                                             # R-DEP (reachable only from :core:data)
feature/{search,radar,game-detail,lists,wishlist,settings,onboarding}/src/
iosApp/                                                              # R-PLACE (iOS shell)
CLAUDE.md, */CLAUDE.md, AGENTS.md, README.md, docs/, .specify/memory/constitution.md   # R-DOC
specs/010-kmp-migration/contracts/                                   # R-CAP, R-DOC
```

**Structure Decision**: This is a documentation-only feature. All output lives in the spec directory, and the
source tree above is input.

## Phases for `/speckit-tasks`

The order is fixed by dependency. The rule list comes first because the ledger needs it. Verification runs early
because a failing build changes the verdict on its own.

1. **Setup**: Record the baseline SHA and environment; rebuild the code-review graph; create the report skeleton
   from the contract.
2. **Rule inventory**: Extract a numbered rule list from the sources in R1, with each rule's source cited.
3. **Verification (US4)**: Run the five commands (R10) and record the results. A failure becomes a High finding
   immediately.
4. **Rule checks (US2)**, which can run in parallel by area:
   - `R-DEP`, with convention plugins included (R3);
   - `R-PURE`, with the iOS compile as the authority (R4);
   - `R-CAP`, the full file mapping (R5);
   - `R-PLACE`: navigation, Koin assembly, entry points, `Res` aliasing.
5. **Leftovers and soundness (US3)**: `R-LEFT` (tracked only, R6), `R-DUP` (pairs, R7) and `R-DOC` (R8).
6. **Leads**: Resolve every row of the research leads table into a finding or a dismissal.
7. **Classification and verdict (US1)**: Apply the R9 rubric, write the fixes and sizes for blockers, collect the
   owner decisions, and derive the verdict.
8. **Validation**: Run the quickstart §3 checklist against the report, and confirm `git status` touches only the spec
   directory.

## Complexity Tracking

Not needed: no constitution principle is violated, and no module or dependency edge is added.
