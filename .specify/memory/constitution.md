# QuestLog Constitution

## Core Principles

### I. Module Boundaries Are Load-Bearing (NON-NEGOTIABLE)

The project is 17 modules under `com.nikolasguillen.questlog.*`, and the dependency graph between
them is a design decision, not an accident of history.

- `feature/*` MUST depend only on `:core:common`, `:core:model`, `:core:domain`, `:core:ui`,
  `:core:navigation` and `:core:designsystem`. A feature module MUST NEVER depend on `:core:data`,
  `:core:network`, `:core:database` or `:core:ai`.
- `:core:ai` MUST be reachable only from `:core:data`. It wraps ML Kit's on-device GenAI client and
  depends on nothing beyond that SDK, Hilt and coroutines.
- `:core:model` MUST stay free of Android and Compose dependencies. It is the most KMP-ready module
  and the natural first candidate for `commonMain`.
- `:app` is the only module that knows about navigation. Feature modules own no nav graph, no nav
  entry provider and no per-feature DI module; screens receive lambdas and never navigate
  themselves. Adding a route means two edits outside the feature: a `NavKey` in
  `core/navigation/Routes.kt` and a branch in the single `entryProvider` in
  `app/.../QuestLogNavDisplay.kt`.

A plan that requires a new edge in this graph MUST say so explicitly and justify it in its
Complexity Tracking section. Silently adding the dependency is a constitution violation.

**Rationale**: These boundaries are what keep a KMP migration tractable and what stop the UI layer
from reaching into persistence. They are cheap to respect now and expensive to reinstate later.

### II. Typed Errors Cross Layers, Exceptions Do Not

Exceptions stop at `:core:data`. Everything above it sees typed results.

- `:core:network` throws. Service methods return bare `List<T>` — no `Response<T>`, no `Call`, no
  retries, no recovery, no result wrapper. The one translation it performs is
  `IgdbHttpErrorInterceptor`, which turns every non-2xx response into an `IgdbHttpException`.
- `:core:data` is the error boundary. `Throwable.toRepositoryError()` maps to `RepositoryError`, and
  results MUST be built with the `AppResult` factory functions, never the constructors.
- `CancellationException` MUST always be rethrown before any mapping, never folded into a
  `RepositoryError`.
- Only repository methods that touch the network return `AppResult`. DB-only methods MUST return a
  bare `Flow<T>` or `Unit`.
- `RepositoryError.toUiText()` in `:core:ui` is the single error-to-text boundary. All error
  rendering MUST route through it.

**Rationale**: Matching `IgdbHttpException` rather than Retrofit's `HttpException` is what keeps
Retrofit out of `:core:data`; a single mapping seam is what makes error handling auditable.

### III. The UI Layer Renders, It Does Not Decide

Composables render and emit events. Nothing else.

- Filtering and sorting happen by ID, never by name, and MUST live in a ViewModel, a UseCase or a
  Mapper — never in a composable.
- Every screen splits into a public ViewModel-taking composable and an `internal` stateless content
  composable, driven by an `@Immutable` UiState, a sealed UiEvent handled by one exhaustive
  `onEvent`, and one-shot UiEffects delivered through `Channel(Channel.BUFFERED)` +
  `receiveAsFlow()`.
- Flows MUST be collected with `collectAsStateWithLifecycle()`.
- Any text that could come from `strings.xml` MUST be `UiText`. Display text MUST NEVER be hardcoded
  in a model, a mapper or a composable. Domain models hold raw data or enums only; labels are
  resolved in the UI layer.

**Rationale**: A composable that decides is a composable that cannot be tested without Compose. The
state/event/effect shape keeps the decisions in the ViewModel, where the JVM test suites can reach
them.

### IV. Reuse the Shared Layer Before Adding to It

The shared layer already covers most of what a new screen needs. Check it before writing anything.

- Before writing a component or a modifier, check `core/ui/component/` and
  `core/ui/util/modifiers/`. They already cover dialogs, game cards, list rows, empty/error/loading
  pages, shimmer, fading edges and metallic effects. Dialogs MUST use `CustomAlertDialog`, never
  Material's `AlertDialog`.
- Before writing a `dp` literal, look for the token in `MaterialTheme.spacing`. A missing value is
  added there, never inlined at the call site.
- Colors and typography come from `MaterialTheme.appColors` and `AppTypography`.
- The app is dark-theme only. There is no light scheme, no dynamic color, and no
  `isSystemInDarkTheme()` branches.
- Shared UI constants belong in `UiConstants`, not inline in a composable.
- There is exactly one `GameRepository` implementation. Adding a second repository requires the
  owner's agreement.

**Rationale**: Duplicated components drift apart visually and each copy has to be fixed separately.
The inventory exists precisely so it does not have to be rediscovered.

### V. Verification Is Local, Not Automated

There is no CI, no detekt, ktlint, spotless or `.editorconfig`, and no `build-logic` or `buildSrc`.
A plan MUST NEVER depend on a pipeline, a lint gate or a formatter that does not exist.

Verification means compiling and running the JVM test suites locally:

- `./gradlew :<module>:compileDebugKotlin` for a single-module change.
- `./gradlew :app:assembleDebug` when the change spans modules or touches DI wiring.
- `./gradlew test` for the suites.

The existing test source sets — `core/data`, `core/domain`, `core/network`,
`feature/{search,radar,lists,game-detail,settings}` and `app` — MUST stay green. New ViewModel,
mapper, use-case or error-mapping logic gets a test in its own module's `src/test` using JUnit4 +
MockK + `kotlinx-coroutines-test`, mocking the use cases rather than the repository, with a
`StandardTestDispatcher` and `Dispatchers.setMain`/`resetMain`.

Modules listed in `docs/tech-debt.md` as untested are a known gap, not a blocker. Do not plan a
coverage campaign unless coverage is the feature being asked for.

**Rationale**: Planning against gates that do not exist produces tasks nobody can execute. Naming
the real commands makes the verification step actionable.

## Additional Constraints

**Platform**: Kotlin 2.4.10, AGP 9.4.1, Gradle 9.7.1 (JVM toolchain 21), compileSdk/targetSdk 37,
minSdk 29, Java 11, Compose BOM 2026.09.00, Hilt 2.60.1, Room 2.8.5 (KSP), Retrofit 3 + Moshi,
Navigation 3, Coil 2, WorkManager. Every `build.gradle.kts` repeats its configuration by hand. When
adding a module, copy `feature/search/build.gradle.kts` (feature) or `core/data/build.gradle.kts`
(core) and register it in `settings.gradle.kts`.

**Persistence**: The app is unpublished, so the database stays at `version = 1` with
`fallbackToDestructiveMigration(true)` and no `Migration` objects. The version MUST NOT be bumped.
An entity change regenerates the exported schema under `core/database/schemas/` and commits it in
the same commit. No list-shaped columns. Persisting a game always goes through `GameDao.saveGame`.

**Injection**: Hilt everywhere except `:core:model` and `:core:navigation`. Routes without arguments
use `@HiltViewModel`; routes with arguments use `@HiltViewModel(assistedFactory = ...)` +
`@AssistedInject`. `SavedStateHandle` is not used in this project and MUST NOT be introduced.

**Secrets**: `IGDB_CLIENT_ID` and `IGDB_CLIENT_SECRET` live in `local.properties` and are injected
as `BuildConfig` fields by `core/network/build.gradle.kts`. They MUST NEVER be committed or moved
into source.

**Kotlin Multiplatform**: The owner intends to migrate to KMP with Compose Multiplatform eventually,
but no work has started and none should. Do not restructure source sets, do not swap Retrofit for
Ktor or Hilt for Koin, and do not move Room to the driver-based API. The rule is only to avoid
choices that would be expensive to reverse. When a decision would be hard to undo after a KMP move,
say so in the plan and let the owner choose.

## Development Workflow

One `data class` / `sealed interface` / `class` per file. The single exception is a sealed hierarchy
keeping its direct implementations in the same file — `core/navigation/Routes.kt` and the
state/event/effect files under `feature/*/model/`. UI-layer models carry the `UiModel` suffix;
domain models stay clean. A cross-module `R` import is always aliased `<Module>R` (`CoreUiR`); a
module's own `R` is imported bare. Default to `internal` for anything local to a module. Comments,
KDoc and all internal documentation are written in English regardless of the conversation language.

`docs/tech-debt.md` lists the known deviations from these rules. Do not silently fix them while
working on something else, and do not imitate them as convention. When a fix lands, delete the entry
instead of annotating it.

The `CLAUDE.md` files are instructions, not a changelog: when a change removes the thing a rule was
about, delete the rule in the same commit.

`docs/roadmap.md` holds the agreed shape and build order of unbuilt features. Read it before
proposing an alternative structure for any of them — the trade-offs were already weighed.

## Governance

This constitution records rules that are already in force in the repository; it does not introduce
new ones.

It governs `/speckit.plan`, `/speckit.tasks` and `/speckit.analyze`. A plan that violates a
principle MUST either be revised or carry an explicit, owner-approved justification in its
Complexity Tracking section.

Precedence is strict. Where this document and the `CLAUDE.md` files disagree, the `CLAUDE.md` files
win and this document is corrected. Where the `CLAUDE.md` files and the source disagree, the source
wins.

Amendments require the owner's approval and are made in the same commit as the change that motivates
them. Versioning follows semantic versioning: MAJOR for a removed or redefined principle, MINOR for
a new or materially expanded principle or section, PATCH for clarifications and wording.

**Version**: 1.0.0 | **Ratified**: 2026-09-25 | **Last Amended**: 2026-09-25
