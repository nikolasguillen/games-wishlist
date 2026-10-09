---

description: "Task list for the KMP architecture audit"
---

# Tasks: KMP Architecture Audit Before Merge

**Input**: Design documents from `/specs/011-kmp-architecture-audit/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/audit-report.md, quickstart.md

**Tests**: None. The audit adds no code (spec FR-014), so there are no test tasks. Verification means running the
project's existing commands (User Story 4).

**Organization**: Tasks are grouped by user story. Because the verdict (US1) is derived from everything the other
stories find, the phases run **US2 → US3 → US4 → US1**, not in spec order. US4's builds do not depend on US2 or US3
and can run alongside them.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependency on an incomplete task)
- **[Story]**: US1 verdict, US2 rules, US3 leftovers and soundness, US4 verification
- Paths are relative to the repository root. `$S` means `specs/011-kmp-architecture-audit`.

## Conventions

- **Read-only (FR-014).** The audit writes only under `$S/`. It never edits source, build files or other docs.
  Gradle and Xcode write to `build/` directories, which are git-ignored, so they are allowed.
- **Per-area notes.** Each check writes its results to its own file under `$S/work/`, which is why checks can run in
  parallel. Each note holds: the rule IDs checked with `pass | violated | not verifiable` and the evidence examined
  (the command, grep or file and line), then candidate findings with provisional IDs `C-<AREA>-n`, using the
  [data-model.md](data-model.md) fields. The `known_ref` field is filled in later, in T024.
- **Source wins.** Where a document and the source disagree, the document is the stale one (research R1). Every
  candidate is confirmed in source before it counts.
- **No silent fixes.** An item already in `docs/tech-debt.md` is noted as a candidate with its entry cited, and is
  never repaired.
- **Baseline.** Every candidate records the baseline SHA from T001.

---

## Phase 1: Setup

**Purpose**: Fix the baseline, create the scaffolding, and refresh the structural index.

- [X] T001 Record the baseline in `$S/work/baseline.md`: `git rev-parse --short 010-kmp-migration`, `sw_vers`, `xcodebuild -version`, `./gradlew --version`, the output of `git diff --stat 010-kmp-migration..HEAD` (must list only `specs/011-kmp-architecture-audit/`), and `git status --short`
- [X] T002 Create the `$S/work/` directory and the report skeleton `$S/audit-report.md`, following the layout in `$S/contracts/audit-report.md` (header with the T001 baseline, empty Verdict, Verification, Findings, Rule ledger, Capability mapping and Leads sections)
- [X] T003 [P] Rebuild the code-review graph on this branch (`code-review-graph build`; it was built on `develop`), so it can be used to locate callers and size fixes. Verify any graph result in source (research R11)

**Checkpoint**: Baseline SHA recorded, `audit-report.md` skeleton exists, graph fresh.

---

## Phase 2: Foundational (blocks all user stories)

**Purpose**: The rule list and the lead list that every check reports against.

- [X] T004 Build the numbered rule inventory in `$S/work/rules.md`. Extract every checkable rule from, in precedence order (research R1): the root `CLAUDE.md` (module graph, dependency rules, non-negotiable rules, KMP section, where-things-live table), then `feature/CLAUDE.md`, `core/data/CLAUDE.md`, `core/network/CLAUDE.md`, `core/database/CLAUDE.md`, `core/ui/CLAUDE.md`, `core/designsystem/CLAUDE.md`, then `.specify/memory/constitution.md`, then `specs/010-kmp-migration/contracts/module-contracts.md` and `contracts/platform-contracts.md`. Give each rule an ID `R-<AREA>-NN` (areas: DEP, PURE, CAP, PLACE, BUILD, LEFT, DUP, DOC, VER; see data-model.md), a one-sentence statement and its source file and section. De-duplicate rules that appear in more than one source
- [X] T005 [P] Seed `$S/work/leads.md` with the seven rows of the "Leads carried into the audit" table in `$S/research.md`, each with an empty `resolution` column

**Checkpoint**: `rules.md` and `leads.md` exist. The checks below can start.

---

## Phase 3: User Story 2 — Structure matches the documented rules (Priority: P1)

**Goal**: Every documented dependency, purity, capability and placement rule has a recorded outcome with evidence.

**Independent Test**: Every `R-DEP`, `R-PURE`, `R-CAP`, `R-PLACE` and `R-BUILD` rule from `rules.md` appears in the
`work/` notes of this phase with an outcome and evidence, and every `violated` one has a candidate finding.

- [X] T006 [P] [US2] Declared dependency edges (research R3): read every `build.gradle.kts` (19 modules plus the root) and the four convention plugins in `build-logic/convention/src/main/kotlin/` (`questlog.kmp.feature` injects the six allowed `:core:*` edges into every feature module). Write the complete edge matrix and the outcome of every `R-DEP` rule to `$S/work/dep.md`: no feature edge to `:core:data`, `:core:network`, `:core:database` or `:core:ai`; `:core:ai` reached only from `:core:data`; `:shared` depends on everything except `:core:ai`; `:androidApp` edges versus the root `CLAUDE.md` graph; no feature module depends on Koin; `:core:model` declares only `kotlinx-serialization-core`
- [X] T007 [US2] Confirm the doubtful edges with Gradle's resolved graph (depends on T006; research R3): `./gradlew :feature:search:dependencies --configuration debugCompileClasspath`, the same for `:core:data`, `:core:ui` and `:androidApp`, plus the iOS classpath of `:feature:search` (`iosSimulatorArm64CompileKlibraries`). Check that `:core:ai` is unreachable from every module except `:core:data`, and whether `:androidApp` reaches `:core:navigation` (the `questlog://game/{id}` deep link in `androidApp/src/main/java/.../MainActivity.kt`) directly or only through `:shared`. Append the evidence and any candidates to `$S/work/dep.md`
- [X] T008 [P] [US2] Shared-code purity (research R4), recorded in `$S/work/pure.md`: grep every git-tracked `*/src/commonMain` source for `import android.`, `import java.` and `import javax.`; list every distinct `import androidx.` and classify each by the artifact it resolves to (multiplatform or Android-only), starting with `shared/src/commonMain/kotlin/com/nikolasguillen/questlog/shared/QuestLogNavDisplay.kt`; check fully qualified platform names that have no import; check that `core/model` has no Android or Compose import or dependency. The iOS compile in T019 is the authority; reference its result here once it exists
- [X] T009 [P] [US2] Capability mapping (research R5): map each of the 48 git-tracked `.kt` files under `*/src/androidMain` and `*/src/iosMain` (`git ls-files '*/src/androidMain/*.kt' '*/src/iosMain/*.kt'`) to exactly one row of `specs/010-kmp-migration/contracts/platform-contracts.md` (a contract, a compatibility seam, the platform-shell table, or a `*PlatformModule` binding). Write the table with the columns `file | platform | maps_to | counterpart | status` to `$S/work/cap-mapping.md`. Record `unmapped` and `counterpart missing` rows as candidates. Include `shared/src/iosMain/.../MainViewController.kt` and `core/common/src/iosMain/.../ApplicationSupport.kt` and `core/ui/src/iosMain/.../TopViewController.kt`, which the contract file lists as iOS-only helpers
- [X] T010 [P] [US2] Contract completeness, recorded in `$S/work/cap-contracts.md`: for every contract in `platform-contracts.md` confirm an implementation or the documented no-op on both platforms and its binding in the matching `*PlatformModule`; for every `expect` (`grep -rnE '\b(expect|actual) ' $(git ls-files '*.kt')`) confirm an `actual` in both `androidMain` and `iosMain`; check each row of the "Capability exception register" against the iOS code (reminders hidden through `ReleaseRemindersAvailability`, translator `UNSUPPORTED`, in-process refresh); check that no platform type appears in a contract signature; check that test doubles are not in `commonMain`
- [X] T011 [P] [US2] Placement rules, recorded in `$S/work/place.md`: navigation lives only in `:shared` (`NavDisplay` and the single `entryProvider` in `QuestLogNavDisplay.kt`, routes in `core/navigation/Routes.kt` plus `GameNavSavedStateConfiguration.kt`); no `NavKey` handling, `koinViewModel` or `koinInject` call inside a feature module; every ViewModel is registered with `viewModelOf` in `shared/.../di/ViewModelModule.kt`; `initKoin` lives in `shared/.../di/SharedKoin.kt`; the Android entry-point concerns (splash, edge-to-edge, deep-link parsing) live only in `androidApp`; `iosApp/` hosts only `MainViewController`; `KoinGraphTest` exists and is included by the root `test` task; cross-module `Res` imports are aliased `CoreUiRes` and a module's own `Res` is imported bare (`grep -rn 'import .*\.generated\.resources\.Res'`)
- [X] T012 [P] [US2] Build configuration, recorded in `$S/work/build.md`: no module declares its own `namespace`, `compileSdk`, `minSdk`, Java version or Kotlin JVM target where a convention plugin owns it; `:core:ai` keeps its hand-written build file (Android-only, per `CLAUDE.md`); `:androidApp` applies `questlog.android.application`; every module is registered in `settings.gradle.kts`; the root `build.gradle.kts` plugin aliases and the `test` aggregate task (`subprojects.forEach … testAndroidHostTest`) match the modules that exist; `gradle.properties` flags (`kotlin.native.ignoreDisabledTargets` and similar) are intentional

**Checkpoint**: US2 notes complete. Every dependency, purity, capability, placement and build rule has an outcome.

---

## Phase 4: User Story 3 — Two-platform soundness and migration leftovers (Priority: P2)

**Goal**: Leftovers, duplicated platform logic and stale documentation are found, or each category is recorded as clean.

**Independent Test**: Each of the four categories (tracked leftovers, duplicated logic, stale instruction files, stale
other docs) has a `work/` note that names the searches made and either lists the findings or states "clean".

- [X] T013 [P] [US3] Tracked leftovers (research R6), recorded in `$S/work/left.md`, **tracked files only** (`git ls-files`; untracked or ignored local state is listed under a separate "local hygiene" heading and never as a finding): source folders no module compiles (`*/src/main` that `questlog.kmp.library` modules no longer use; `androidApp/src/main` and `core/ai/src/main` are expected); entries in `gradle/libs.versions.toml` that no build file references (check at least Hilt, Retrofit, Moshi, OkHttp logging, Coil 2, `shadowglow`, KSP/kapt aliases); Android `res/` strings or drawables that should have become Compose resources; hard-coded `android {}` blocks; template or example files (`ExampleUnitTest.kt`, `ExampleInstrumentedTest.kt` are already in `docs/tech-debt.md`, so mark them known)
- [X] T014 [P] [US3] Duplicated platform logic (research R7), recorded in `$S/work/dup.md`: compare each `androidMain`/`iosMain` pair from `$S/work/cap-mapping.md` (build it from `platform-contracts.md` if T009 is not finished yet), looking for logic that does not touch a platform API and is written twice. Pay particular attention to: the 24-hour refresh interval (`platform-contracts.md` requires "a single constant shared by both implementations"), `WishlistCoverImageStorageImpl` (file naming and the max dimension), `AppVersionProviderImpl`, `NetworkStatusProviderImpl`, and the `isConnectivityFailure`/`isTimeoutFailure` tables. For each pair record "duplicated pure logic" or "legitimate platform difference"
- [X] T015 [P] [US3] Instruction-file drift, recorded in `$S/work/doc-claude.md`: check each concrete claim in the root `CLAUDE.md` and the six directory `CLAUDE.md` files against the source (module count, module graph, the `:androidApp` edges, paths in the "Where things live" table, commands, version numbers against `gradle/libs.versions.toml`, the "no `core/` or `feature/` Hilt" statements, mentions of `src/main/java` versus `src/commonMain/kotlin`). A claim that is wrong or missing is a candidate with the correct text
- [X] T016 [P] [US3] Other document drift, recorded in `$S/work/doc-other.md`: check `AGENTS.md`, `README.md`, `.specify/memory/constitution.md`, `docs/tech-debt.md`, `docs/roadmap.md` and `specs/010-kmp-migration/contracts/*.md` against the source. Start with `docs/tech-debt.md` ("Cleanup pass in progress" on `develop`, "convention plugins … deliberately last" while `build-logic/` exists, "Last audited" date) and the constitution's module and file references. Per the project rule, a stale entry is to be deleted, not annotated; say so in the candidate's `fix`

**Checkpoint**: US3 notes complete. All four categories are listed or recorded as clean.

---

## Phase 5: User Story 4 — The build and tests hold (Priority: P2)

**Goal**: Each documented verification command has a recorded result, and nothing is claimed to pass without running.

**Independent Test**: `$S/work/verification.md` has one row per command with `passed`, `failed` or `not run`
(with a reason), and a `failed` row names its candidate finding.

These can start right after Phase 2 and overlap Phases 3–4 if long builds run in the background. Gradle tasks run one
at a time (they share the Gradle daemon and build directories). Do not run `clean`.

- [X] T017 [US4] Run `./gradlew :androidApp:assembleDebug --console=plain` and record the result, the duration and the tail of the output in `$S/work/verification.md`. On failure, add a High candidate `C-VER-1` with the first error
- [X] T018 [US4] Run `./gradlew test --console=plain`, which includes `KoinGraphTest`. Record the per-module result and the totals (tests, failures, skipped) in `$S/work/verification.md`. On failure, add a High candidate per failing test class. No test is deleted, skipped or weakened to get a pass
- [X] T019 [US4] Run `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64 -q --console=plain`. Record the result in `$S/work/verification.md`. A pass is also the authority for `R-PURE` (T008): note that `commonMain` compiles for iOS. A failure caused by a platform API in shared code is a High candidate
- [X] T020 [US4] Run `./gradlew :core:network:iosSimulatorArm64Test --console=plain -q`. Record the result in `$S/work/verification.md`. If no simulator runtime is available, record `not run` with the reason
- [X] T021 [US4] Run the simulator build: `xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' -derivedDataPath iosApp/build build`. Record the result in `$S/work/verification.md`. If Xcode or a runtime is missing, record `not run` with the reason. Then add the `R-VER` rule outcomes (build passes, tests pass, Koin graph complete, iOS framework links) to the same file

**Checkpoint**: Five verification rows recorded; `R-VER` rules have outcomes.

---

## Phase 6: User Story 1 — Merge-readiness verdict (Priority: P1)

**Goal**: One report that ends in "merge" or "do not merge yet", derived from classified findings.

**Independent Test**: `$S/audit-report.md` is readable on its own. Its verdict agrees with its Blockers section, and
every blocker has a fix and a size.

**Depends on Phases 3–5 being complete.**

- [X] T022 [US1] Resolve every row of `$S/work/leads.md` to either a confirmed candidate (cite the `C-…` ID) or a dismissal with its evidence and reason. A lead is never dropped silently (FR-012)
- [X] T023 [US1] Consolidate all candidates from `$S/work/*.md` into one list. Merge duplicates reached from two angles (for example an `:androidApp` edge that appears in `dep.md` and `doc-claude.md`), and drop candidates the evidence refutes into the dismissed table
- [X] T024 [US1] Classify each remaining candidate (research R9). First look it up in `docs/tech-debt.md` and `docs/roadmap.md` (`known`) and in `specs/010-kmp-migration/{plan,research,data-model}.md` and the contracts (`intentional`), citing `known_ref`; a `known` item the migration made worse is classified on its merits. Then assign severity (`high` / `medium` / `low`) and classification (`blocker` / `follow-up`) with the rubric, and number the findings `F-001`… in the order found, with each carrying the baseline SHA
- [X] T025 [US1] For every blocker, write the recommended `fix` and `fix_size` (`S`, `M`, `L`) in `$S/work/blockers.md`, using the graph (T003) to size the blast radius and verifying in source. Add an `owner_decision` for any fix that is hard to undo or has real alternatives (module moves, public API changes, library changes, anything the constitution says the owner must choose). Each blocker fix must state that it keeps `./gradlew :androidApp:assembleDebug` and `./gradlew test` green, in its own commit
- [X] T026 [US1] Assemble `$S/audit-report.md`: the Verification table from `work/verification.md`, the Findings section (blockers, follow-ups, known and intentional), the full Rule ledger from `work/rules.md` with the outcomes recorded in `work/*.md`, the Capability mapping from `work/cap-mapping.md`, and the Leads-dismissed table
- [X] T027 [US1] Write the Verdict at the top of `$S/audit-report.md`: "Merge" if and only if no blocker remains; otherwise "Do not merge yet" with the blocker list and fix sizes. Add the owner decisions needed and the **Not verified** list (every `not verifiable` rule and every `not run` command, with its reason). Re-read the verdict against the Blockers section until they agree

**Checkpoint**: The report is complete, and the verdict follows from the findings.

---

## Phase 7: Polish & validation

- [X] T028 Walk the `quickstart.md` §3 validation table against `$S/audit-report.md`, one check at a time: verdict and blocker list agree; every rule has an outcome and evidence; every violated rule links a finding; every blocker has a fix and a size; every finding has every FR-010 field; every verification row is `passed`, `failed` or `not run` with a reason; every lead is a finding or dismissed. Fix the report where a check fails
- [X] T029 Confirm the audit stayed read-only (FR-014, SC-006): `git status --short` and `git diff --stat` must list only paths under `$S/`. Confirm that the report and `work/` notes contain no IGDB credential values (`grep -rn "IGDB_CLIENT" $S` must show names only, never values)
- [X] T030 Report the verdict to the owner with the blocker list, the owner decisions needed and the items not verified. Do not start fixing. Applying fixes is a separate, owner-approved step (spec Assumptions), and the follow-up path is `/speckit-taskstoissues` or a new feature spec

---

## Dependencies & Execution Order

### Phase dependencies

- **Phase 1 (Setup)**: no dependencies. T001 → T002; T003 in parallel.
- **Phase 2 (Foundational)**: needs T001–T002. T004 is required before any rule outcome is final; T005 is independent.
- **Phase 3 (US2)**, **Phase 4 (US3)**: need T004. Inside a phase, [P] tasks write different `work/` files and run together.
- **Phase 5 (US4)**: needs only Phase 1 (a build does not need the rule list). Run it in parallel with Phases 3–4. The tasks are sequential.
- **Phase 6 (US1)**: needs Phases 3, 4 and 5 complete. T022 → T023 → T024 → T025 → T026 → T027.
- **Phase 7**: needs Phase 6.

### Task dependencies worth knowing

- T007 needs T006 (both write `work/dep.md`).
- T014 reads `work/cap-mapping.md` from T009, or the contracts directly.
- T008 cites T019, whose result may arrive later. Complete T008's grep first and add the compiler result afterwards.
- T025 needs T024 (only blockers get a fix and a size).

### Story completion order

```text
Setup → Foundational ─┬─► US2 (rules) ──┐
                      ├─► US3 (soundness)┼─► US1 (verdict) → Polish
                      └─► US4 (builds) ──┘
```

### Parallel opportunities

```text
# After T004: all US2 checks except T007 together, with US3 checks
T006  dep edges            T008  purity            T009  cap mapping
T010  cap contracts        T011  placement         T012  build config
T013  leftovers            T014  duplication       T015  CLAUDE.md drift
T016  other docs drift

# In the background, from Phase 2: the build chain (sequential among themselves)
T017 → T018 → T019 → T020 → T021
```

---

## Implementation Strategy

### Minimum useful audit (US2 + US4, then US1)

1. Phase 1 and Phase 2.
2. Start the build chain (US4) in the background. If `assembleDebug` or `test` fails, that alone is a blocker, and
   the verdict is already "do not merge yet" for that reason.
3. Phase 3 (US2): the rule checks, the heart of the audit.
4. Phase 6 (US1): classify and write the verdict.
5. **Stop and show the owner** the verdict, before polishing Phase 4.

### Full audit

Add Phase 4 (US3: leftovers, duplication, documentation) before Phase 6, so the follow-up list is complete. Documentation
drift and leftovers are mostly follow-ups, but one of them can be a blocker if it misleads work that starts straight
after the merge.

### Notes

- The owner decides what to fix after reading the report; this plan produces no code changes.
- If an area turns out clean, its `work/` note still records the searches made, so the clean result can be reproduced.
- Commit the spec directory with the `/speckit-git-commit` hook when asked. Nothing outside it changes.
