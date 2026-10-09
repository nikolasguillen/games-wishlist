# Research: KMP Architecture Audit Before Merge

Phase 0 decisions for [plan.md](plan.md). Each one fixes *how* a part of the audit is carried out, so two
people running it reach the same findings. No `NEEDS CLARIFICATION` remains.

## R1 — The standard the structure is judged against

**Decision**: Rules are taken from, in order of precedence: the source itself, then the `CLAUDE.md` files (root plus
`feature/`, `core/{data,network,database,ui,designsystem}/`), then `.specify/memory/constitution.md`, then the
migration's own design documents (`specs/010-kmp-migration/{plan,research,data-model}.md`,
`contracts/module-contracts.md`, `contracts/platform-contracts.md`). `docs/tech-debt.md` and `docs/roadmap.md` are
not rule sources. They decide whether a finding is *known* (FR-012).

**Rationale**: This is the precedence the constitution's Governance section already states: CLAUDE.md beats the
constitution, and the source beats both. The migration documents record *intent* (what was decided), so they are
what turns a suspected issue into an *intentional* one.

**Alternatives considered**: Judging against general KMP best practice was rejected. It would produce findings the
owner never agreed to, and the spec scopes the audit to the project's own rules. Best-practice observations are
allowed only as follow-ups, labelled as such.

## R2 — Baseline commit

**Decision**: The audit runs against the tip of `010-kmp-migration` (`b982bf11` when this plan was written). It runs
from `011-kmp-architecture-audit`, which differs from it only by `specs/011-kmp-architecture-audit/`. `develop` is an
ancestor of `010-kmp-migration` with no commits of its own since the branch point, so the merge diff is the
migration diff. Every finding records the SHA it was checked against (FR-010).

**Rationale**: The spec's assumptions freeze the scope at the audit's start. Running from the audit branch costs
nothing, because the only extra files are this spec's own documents.

**Alternatives considered**: Checking out `010-kmp-migration` detached was rejected: it would leave the report
without a home branch, and it changes nothing about the code being audited.

## R3 — How dependency edges are established

**Decision**: An edge is whatever the build declares, read from two places: each module's `build.gradle.kts`,
**and** the convention plugins in `build-logic/convention/src/main/kotlin/`. `questlog.kmp.feature` injects the six
allowed `:core:*` edges into every feature module, so the feature build files declare none themselves. Any edge in
doubt is confirmed with Gradle's resolved view
(`./gradlew :<module>:dependencies --configuration <sourceSet>CompileClasspath`). Both `implementation` and `api`
count, and an `api` edge is followed transitively when the rule is about reachability (`:core:ai` reachable only
from `:core:data`).

**Rationale**: Reading the module files alone would report every feature module as dependency-free, which is
wrong. Gradle's resolved graph is authoritative but slow, so it is reserved for confirming.

**Alternatives considered**: Relying on the code-review graph was rejected for edges. It was built on `develop` and
is stale on this branch, and it models Kotlin symbols, not Gradle edges.

## R4 — How "shared code uses no platform API" is checked

**Decision**: Use two passes. (a) A locator: grep `commonMain` for `import android.`, `import java.` and
`import javax.`, all of which are violations. (b) The authority: the iOS framework link
(`:shared:linkDebugFrameworkIosSimulatorArm64`). If `commonMain` reached an Android-only API, the iOS compile fails.
`androidx.*` imports are **not** violations by themselves. Lifecycle, ViewModel, Navigation 3, Room, DataStore,
Compose and Annotation all ship multiplatform artifacts under `androidx` package names, so each `androidx.*` import
found in `commonMain` is checked against the artifact it resolves to, and only Android-only ones (`activity`, `work`,
`core-ktx`, `splashscreen`) are findings. The model module's purity is checked on its declared dependencies (only
`kotlinx-serialization-core` is allowed) and its imports.

**Rationale**: The scoping scan matched `shared/.../QuestLogNavDisplay.kt` with a naive `androidx` pattern. That is
almost certainly the multiplatform lifecycle or navigation artifact, and a grep-only rule would report a false
positive. The compiler is the one check that cannot be fooled.

**Alternatives considered**: Grep only was rejected because it is both over-inclusive (`androidx`) and
under-inclusive (fully qualified names with no import).

## R5 — Mapping platform sources to capability contracts

**Decision**: List every git-tracked `.kt` file under `*/src/androidMain` and `*/src/iosMain` (48 at plan time: 27
Android and 21 iOS) and assign each one to exactly one row in `specs/010-kmp-migration/contracts/platform-contracts.md`:
a contract, a "compatibility seam", the "platform shell" table, or a Koin `*PlatformModule` that binds those. A
file with no row is a finding. In reverse, every contract is checked for an implementation (or a documented no-op)
on both platforms, every `expect` for an `actual` on both, and every row of the capability exception register
against what the code actually does on iOS. `:androidApp` and `:core:ai` are checked against the platform-shell
table and their Android-only status rather than the contract list.

**Rationale**: The root CLAUDE.md says every file in `androidMain` or `iosMain` implements a listed capability.
That rule can only be verified file by file.

**Alternatives considered**: Sampling was rejected. 48 files is small enough to check in full, and SC-001 asks for
every rule to have an outcome.

## R6 — What counts as a migration leftover

**Decision**: Only **git-tracked** state counts toward the merge, because only tracked state gets merged.
Untracked, ignored or local-only artifacts are listed separately as local hygiene, never as blockers. The
untracked, empty `core/model/src/main` and `core/navigation/src/main` directories, and the root `build/` and
`captures/` (both in `.gitignore`), fall into that category. Tracked leftovers that are in scope:
- source folders a module no longer compiles;
- dependencies, plugin aliases and version-catalog entries nothing uses (for example the Hilt, Retrofit, Moshi, Coil 2
  and `shadowglow` entries the migration removed);
- `android {}` blocks or settings that the convention plugins now own (namespace, SDKs, Java version);
- Android `res/` that should have become Compose resources;
- template or example files.

**Rationale**: The spec's question is "should this be fixed before merging". Something that is not in the repository
cannot be merged.

**Alternatives considered**: Treating every local directory as a finding was rejected. It would bury real findings
under IDE noise.

## R7 — Duplicated platform logic

**Decision**: Every `androidMain`/`iosMain` pair (same contract, same module) is compared. Logic that does not touch
a platform API, such as constants, intervals, file naming, error classification tables or downscale limits, is
flagged if it is written twice. One example is the 24-hour refresh interval, which `platform-contracts.md` requires
to be "a single constant shared by both implementations". Logic that wraps a platform API is legitimate by
definition.

**Rationale**: Duplicated pure logic is the classic slow drift in a KMP codebase. It compiles today and diverges
the first time one side is edited.

**Alternatives considered**: Running a clone-detection tool was rejected. None is configured (constitution
Principle V), and 48 files can be read.

## R8 — Documentation drift: scope

**Decision**: The documents checked against the source are the root `CLAUDE.md`, the six directory `CLAUDE.md`
files, `AGENTS.md`, `README.md`, the constitution, `docs/tech-debt.md`, `docs/roadmap.md` and the 010 `contracts/`.
Each concrete claim (a module count, a dependency edge, a path, a file name, a command, a "where things live" row)
is checked. Leads already seen while scoping:
- The root `CLAUDE.md` graph draws `:androidApp → :shared` only, while `module-contracts.md` also lists
  `:androidApp → :core:domain`, which the build has, and `:androidApp → :core:navigation`, which the build file does
  not declare.
- `docs/tech-debt.md` still describes a "cleanup pass in progress on `develop`" and says convention plugins are
  "deliberately last", although `build-logic/` now exists.

**Rationale**: The project instructions say they are "instructions, not a changelog" and must be kept true. A
migration this size is when they go stale, and stale agent instructions mislead every later session.

**Alternatives considered**: Leaving the docs for later was rejected. FR-008 and SC-007 put them in scope, and
fixing them is cheap.

## R9 — Severity and the blocker rubric

**Decision**: Every finding gets one of three severities and one classification.

Severities:
- **High**: breaks the Android build or tests, breaks the iOS build, or violates a NON-NEGOTIABLE rule (constitution
  Principle I, or `commonMain` platform purity).
- **Medium**: violates another documented rule, or creates a structure that later work would build on (a misplaced
  contract, a wrong dependency edge, duplicated logic in a contract implementation).
- **Low**: hygiene, naming, or documentation drift that misleads but does not structure anything.

Classifications:
- **Blocker** = any High, plus any Medium that would be materially more expensive to fix once other branches build
  on `develop`.
- **Follow-up** = everything else.
- **Known** / **Intentional** override the classification when FR-012 applies, unless the migration made the issue
  worse.

The verdict is "do not merge yet" if and only if at least one blocker remains.

**Rationale**: This puts the spec's assumption about what a blocker is into a rule that can be applied
mechanically, so the verdict follows from the findings rather than from judgement at the end.

**Alternatives considered**: A numeric risk score was rejected as false precision for a list this size.

## R10 — Verification commands and the iOS side

**Decision**: The audit machine is a Mac (darwin), so all of these are run and recorded:
- `./gradlew :androidApp:assembleDebug`
- `./gradlew test`
- `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64`
- `./gradlew :core:network:iosSimulatorArm64Test`
- the `xcodebuild` simulator build from the root CLAUDE.md

`KoinGraphTest` is part of `./gradlew test` and proves the DI graph is complete. A command that cannot run (no
simulator runtime, Xcode missing) is recorded as **not run** with the reason, never as passed (SC-005). A manual
simulator walkthrough is out of scope. The 010 `baseline.md` already records one, and repeating it is not needed to
judge structure.

**Rationale**: These are the project's own documented verification commands (constitution Principle V). No lint
gate or CI is invented.

**Alternatives considered**: Running `allTests` was rejected. The root CLAUDE.md says it is not the project's
command.

## R11 — Use of the code-review graph

**Decision**: Rebuild the graph on the audit baseline before using it (it was built on `develop`). Use it only to
locate code: callers of a contract, who reaches a symbol, how big the impact of a candidate fix would be. Every
finding is confirmed in source, and where the graph and the source disagree, the source wins.

**Rationale**: The project's CLAUDE.md requires graph-first exploration with source verification, and the graph
is stale on this branch.

**Alternatives considered**: Skipping the graph entirely was rejected. It is the cheapest way to size a fix's blast
radius for FR-011.

## R12 — Shape and location of the deliverable

**Decision**: One report, `specs/011-kmp-architecture-audit/audit-report.md`, following
[contracts/audit-report.md](contracts/audit-report.md). Findings are numbered `F-001`… in the order found, and rules
`R-<area>-NN`. The verdict is at the top, and the full rule ledger and findings follow. No source, build or
documentation file outside this directory changes during the audit (FR-014, SC-006).

**Rationale**: A single file the owner reads once (SC-003), versioned with the spec so the next pass can diff it.

**Alternatives considered**: Opening one GitHub issue per finding was rejected. That is outward-facing, and
`/speckit-taskstoissues` exists if the owner wants it later.

## Leads carried into the audit

These came out of scoping. They are **leads, not findings**: each one is checked, classified and either reported or
dismissed with its evidence.

| Lead | First observation | Likely outcome |
|---|---|---|
| `:androidApp → :core:domain` edge | `QuestLogApp` imports `ReleaseRefreshScheduler`; listed in `module-contracts.md` but missing from the root CLAUDE.md graph | Intentional in code; documentation drift |
| `:androidApp → :core:navigation` | Listed in `module-contracts.md` for deep links, not declared in `androidApp/build.gradle.kts` | Transitive through `:shared`, or a doc error. Check how `MainActivity` builds `GameDetailRoute` |
| `koin-core` in `:core:domain` | `DomainKoin.kt` | Intentional (`module-contracts.md` lists it); confirm and dismiss |
| `androidx` imports in `shared/commonMain/QuestLogNavDisplay.kt` | Matched a naive pattern | Multiplatform artifacts; dismiss if R4 confirms |
| Stale `docs/tech-debt.md` sections | "Cleanup pass in progress", "convention plugins deliberately last" | Documentation follow-up |
| Untracked empty `src/main` dirs in `:core:model`, `:core:navigation` | Local only | Local hygiene; not a merge issue (R6) |
| iOS test not part of the root `test` task | Already in tech-debt | Known |
