# Implementation Plan: Multiplatform Migration

**Branch**: `010-kmp-migration` | **Date**: 2026-10-08 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/010-kmp-migration/spec.md`

## Summary

Move QuestLog to Kotlin Multiplatform with Compose Multiplatform. iOS becomes the second target, and
Android keeps shipping unchanged throughout.

**Architecture**:

- **Shared code**: every module except `:core:ai` becomes a KMP library on AGP 9's `com.android.kotlin.multiplatform.library` plugin.
- **New `:shared` module**: the cross-platform app root. It owns the root composable, `QuestLogNavDisplay`, the bottom bar and the DI graph, and exports the iOS framework.
- **`:app`** shrinks to the Android entry point.
- **`iosApp/`**: a new Xcode project that hosts the framework.

**Library swaps** (research R4–R6): Hilt → Koin, Retrofit + Moshi → Ktor + kotlinx.serialization,
framework SQLite → Room's bundled driver, Coil 2 → Coil 3. `java.time` → `kotlinx-datetime`, and Android
resources → Compose Multiplatform resources.

**Platform capabilities** sit behind contracts owned by shared code (FR-007). On iOS, release reminders
and on-device translation are absent and their entry points hidden (FR-008), while Radar's release dates
still refresh in-process.

**Order of work** (research R14): every swap happens while the code is still Android-only, then modules
convert leaf-first, then iOS is switched on. Every commit leaves Android building and its tests green.

## Technical Context

**Language/Version**:

- **Kotlin**: 2.4.20 (from 2.4.10).
- **JVM**: target 11 for the Android target, JVM toolchain 21.
- **Swift**: 5 for the `iosApp/` shell only.

**Primary Dependencies** (versions are pinned by the task that introduces each one, from the source named
in research):

- **Compose Multiplatform 1.12.1**: runtime, foundation, Material 3 (multiplatform artifact, alpha on iOS; Android still resolves `androidx` 1.5.0-beta01), resources, and material icons 1.7.3.
- **JetBrains multiplatform AndroidX artifacts**: Navigation 3 UI, lifecycle (`viewmodel-compose`, `runtime-compose`, `viewmodel-navigation3`).
- **Koin 4.2.2**: `koin-core`, `koin-compose`, `koin-compose-viewmodel`; `koin-android` and `koin-androidx-workmanager` on Android.
- **Networking**: Ktor client 3.x (OkHttp / Darwin engines), `kotlinx-serialization-json`.
- **Persistence**: Room 2.8.5 with `sqlite-bundled` and the Room Gradle plugin; DataStore Preferences 1.2.1.
- **Other libraries**: Coil 3 (`coil-compose`, `coil-network-ktor3`), Haze (unchanged), `kotlinx-datetime` 0.8.0 (already present).
- **Build**: gmazzo `buildconfig` Gradle plugin (IGDB credentials).
- **Unchanged and Android-only**: WorkManager, ML Kit GenAI (`:core:ai`), `core-splashscreen`.

**Removed**: Hilt (plugin, runtime, `hilt-navigation-compose`, `hilt-work`), Retrofit, Moshi + codegen,
OkHttp logging interceptor, Coil 2, `shadowglow`.

**Storage**: Room (same database file name, `version = 1`, destructive fallback kept) and Preferences
DataStore (same file on Android). There are no schema changes. One iOS-only preference key (data-model).

**Testing**:

- **Existing suites**: the 48 JUnit4 + MockK + `kotlinx-coroutines-test` test files move to each module's `androidHostTest` with no assertion changed. A few get type-only edits, all named in `tasks.md` and audited in its final verification: an extra constructor argument, `RequestBody` → `String`, `File` → `String`, and `R` → `Res` handles.
- **New host tests** cover the date pipeline, compact numbers, connectivity classification, the reminders-unavailable branches of four ViewModels, and Koin `verify()`.
- **`commonTest`** (`kotlin.test`) is used only for new mock-free tests.
- **iOS** is validated manually (`quickstart.md`); there is no XCTest suite.

**Target Platform**: Android (minSdk 29, targetSdk 37), iOS 16.0+ (`iosArm64`, `iosSimulatorArm64`). iOS
builds need macOS with Xcode; Windows builds and tests Android only.

**Project Type**: modular mobile app. 18 Gradle modules today become 19 (`:shared` added), plus
`build-logic/` (included build) and `iosApp/` (Xcode).

**Performance Goals**:

- **Android**: no regression. The splash is held exactly as today (one DataStore read), and lists scroll at the frame rate they do today.
- **iOS**: first interactive frame within 2 s of launch on a recent simulator or device, and 60 fps scrolling in the Discover shelves, search grid and wishlist (checked in the walkthrough).
- **Both**: SC-003, a new iOS user saves a first game in under 3 minutes.

**Constraints**:

- **Process**: no CI; verification is local (Principle V).
- **Android green at every commit**, with no test deleted or weakened (FR-011).
- **Credentials**: never committed (FR-013).
- **No new permissions on iOS**: `PHPicker` needs no photo-library permission, and there are no notifications.
- **Data continuity**: no data reset for existing Android installs. The paths are kept, even though FR-015 would tolerate a reset.

**Scale/Scope**:

- **Kotlin code**: ~392 main files and 48 test files.
- **Resources**: ~385 lines of `strings.xml` in one locale (English) across 11 modules, 7 drawables.
- **UI**: 9 routes, 2 ViewModels with assisted IDs, 2 WorkManager workers.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

Checked against constitution v1.1.0.

| Rule | Status | Notes |
|---|---|---|
| **I.** Feature modules depend only on the six allowed `:core:*` modules | ✅ Pass | Unchanged. Features gain no Koin dependency, because ViewModels are registered in `:shared` (contracts/module-contracts) |
| **I.** `:core:ai` reachable only from `:core:data` | ✅ Pass | It narrows to `:core:data`'s `androidMain` |
| **I.** `:core:model` free of Android and Compose | ✅ Pass | It becomes pure `commonMain` |
| **I.** "`:app` is the only module that knows about navigation" | ⚠️ Justified change | Navigation moves to `:shared`, forced by AGP 9 (research R2). Still exactly one owner, and still two edits per route. Approved by the owner on 2026-10-08; see Complexity Tracking |
| **I.** New module or edge called out | ✅ Pass | `:shared`, its edges, and `:app`'s reduced edges are listed in contracts/module-contracts |
| **II.** Exceptions stop at `:core:data`; `IgdbHttpException` is the only HTTP error crossing `:core:network` | ✅ Pass | The Ktor `HttpResponseValidator` throws `IgdbHttpException`; `ResponseException` never leaves `:core:network`. `CancellationException` is rethrown first in the common mapper |
| **II.** `AppResult` only for network-touching repository methods | ✅ Pass | Repository signatures unchanged |
| **III.** UI renders, ViewModels decide; UiState / UiEvent / UiEffect shape | ✅ Pass | Capability hiding is decided in ViewModels via `ReleaseRemindersAvailability`. The picker and sharer composables only launch and return |
| **III.** `collectAsStateWithLifecycle()` | ✅ Pass | Available from JetBrains `lifecycle-runtime-compose` in `commonMain` |
| **III.** Resource-backed text is `UiText` | ✅ Pass | `UiText` keeps its role; only the resource handle type changes (data-model) |
| **IV.** Reuse `core/ui` components, `CustomAlertDialog`, spacing tokens, `appColors` | ✅ Pass | Components move as-is. `shadowglow` is replaced by Compose's `dropShadow` at its single call site. No new component duplicates an existing one |
| **IV.** Theme resolved once; no `isSystemInDarkTheme()` in components | ✅ Pass | The resolution moves from `MainActivity` into `QuestLogRoot` (`:shared`), still once. Wording update in the constitution (Phase G) |
| **IV.** Exactly one `GameRepository` implementation | ✅ Pass | Unchanged, in `commonMain` |
| **V.** No dependency on CI, lint gates or formatters | ✅ Pass | All checks are local commands (quickstart) |
| **V.** Existing suites stay green; new logic gets JUnit4 + MockK tests | ✅ Pass | Suites move to `androidHostTest` with no assertion changed (type-only edits are listed in `tasks.md`). The task names change (`testAndroidHostTest`), and the commands are updated in Phase C |
| **V.** "no `build-logic` or `buildSrc`" | ⚠️ Justified change | Convention plugins for 17 KMP modules (research R3). Approved by the owner on 2026-10-08; see Complexity Tracking |
| **Persistence**: `version = 1`, destructive fallback, no list-shaped columns, `GameDao.saveGame` path | ✅ Pass | No entity change. The schema is re-exported only if Room's output differs |
| **Injection**: Hilt everywhere | ✅ Pass (by v1.1.0) | The swap to Koin is decided here and made in Phase B's tasks. `SavedStateHandle` is still not introduced |
| **Secrets**: credentials in `local.properties` only | ✅ Pass | Generated under `build/` by the `buildconfig` plugin; never committed |
| **KMP**: Android green per commit; swaps only in their task; `kotlinx-datetime` | ✅ Pass | This is the order of the phases below |
| **Workflow**: `CLAUDE.md` files are instructions, not a changelog; delete stale rules in the same commit | ✅ Pass | Each phase updates the docs it makes stale (Phase G is a final sweep, not the only update) |

**Gate result**: passes. Both justified changes were approved by the owner on 2026-10-08 (Complexity
Tracking).

**When the rule text changes**: the constitution and `CLAUDE.md` keep describing the code as it is, so
each amendment lands in the commit that makes the old rule false, not earlier:

- "no `build-logic`" is amended in the first Phase C commit, which adds `build-logic/`;
- "`:app` is the only module that knows about navigation" is amended in the Phase E commit that moves
  `QuestLogNavDisplay` into `:shared`.

**Re-check after Phase 1 design**: the result is unchanged. The design added no edge beyond those
listed, and the contracts keep every platform type out of `commonMain` signatures.

## Phases of work

Each phase is a run of commits that each satisfy "Checks after every commit" in `quickstart.md`. The
work can pause after any commit. `/speckit-tasks` turns each phase into tasks.

### Phase A — Toolchain

- Record the parity baseline (quickstart, Phase 0).
- Bump Kotlin to 2.4.20, and add the version-catalog entries for everything introduced later.
- No module changes.

### Phase B — Make the Android code platform-neutral and swap libraries, still Android-only

1. **Platform-neutral code**:
   - **Dates and numbers**: `java.time` → `kotlinx-datetime` behind `DateUtils`; `String.format` and `Locale` replaced (R7).
   - **Text**: `HtmlCompat` → a small common parser for `<b>`/`<i>`/`<u>` (research R8).
   - **Plain values replace platform types**:
     - `File` → path strings in UI models. (`UUID` and `TimeUnit` stay in the Android-only classes that use them.)
   - **Classes become interfaces**: `AppVersionProvider`, `NetworkStatusProvider` and `WishlistCoverImageStorage`.
   - **`RepositoryErrorMapper`**: the `java.net` checks are isolated in one function.
   - **`shadowglow`** → `Modifier.dropShadow`.
   - **New contracts**: `ReleaseRemindersAvailability` (Android binding `true`, with the four ViewModel gates and their tests). `DefaultWishlistSeed` arrives with the database conversion in Phase C.
2. **Hilt → Koin**: Koin modules and `verify()` are added beside Hilt, then everything switches and Hilt is removed. This includes the two assisted ViewModels and the WorkManager workers (R4).
3. **Retrofit + Moshi → Ktor + kotlinx.serialization**, OkHttp engine; credentials via `buildconfig` (R5).
4. **Room on `BundledSQLiteDriver`** with the Room Gradle plugin; DataStore created by path (R6). Verify the Android update path (quickstart).
5. **Coil 2 → Coil 3.**

### Phase C — Non-UI modules to KMP (leaf-first)

- Add `build-logic/` with the `questlog.kmp.library` convention (R3). The convention declares the `android`, `iosArm64` and `iosSimulatorArm64` targets from the start, but the iOS platform source sets are **not compiled until Phase F**: until then `expect` declarations have only their Android `actual`.
- Convert, in order: `:core:model`, `:core:navigation` (JetBrains Navigation 3 + `SavedStateConfiguration`), `:core:common`, `:core:domain`, `:core:network`, `:core:database` (Room constructor, builder factory), `:core:data` (Android-only code moves to `androidMain`).
- Tests move to `androidHostTest` with `git mv`.
- Each module is checked with its Android host tests and with `compileCommonMainKotlinMetadata`, which catches a JVM-only API in `commonMain` before iOS exists. On Windows, a grep for `java.`, `javax.` and `android.` imports in `commonMain` stands in for it.
- The first conversion settles the test command (R11) and updates `CLAUDE.md` and constitution Principle V in the same commit.

### Phase D — UI modules to Compose Multiplatform

- Add the `questlog.kmp.compose` and `questlog.kmp.feature` conventions.
- Convert `:core:designsystem` (`SystemBarsAppearance`), then `:core:ui`:
  - resources to `composeResources`;
  - `UiText` reshaped;
  - picker, sharer and permission `expect`s;
  - `BackHandler`;
  - material icons 1.7.3.
- Then convert the seven feature modules, one at a time.
- `:app` still hosts the navigation during this phase. Android parity rows 1–10 are re-checked after each feature.

### Phase E — `:shared` app root

- Create `:shared`.
- Move `QuestLogNavDisplay`, `QuestLogBottomBar`, the scaffold and theme resolution (as `QuestLogRoot`), and the Koin assembly + `ViewModelModule`. Koin `verify()` moves to `:shared`'s host tests.
- `:app` keeps only the Android shell: Application, Activity, splash, the deep-link → route mapping, and `RoundedCorner`.

### Phase F — iOS

1. **Enable the iOS source sets, leaf-first**, in the same order as Phase C then D. For each module, add its `iosMain` actuals from `contracts/platform-contracts.md` and compile it for `iosSimulatorArm64`. `:core:data` takes the larger share:
   - in-process refresh scheduler;
   - no-op reminder implementations;
   - `UNSUPPORTED` translator;
   - cover storage and connectivity check.
2. Configure `:shared`'s `QuestLogShared` static framework and `MainViewController()`.
3. Create `iosApp/` (SwiftUI host, launch screen, icons, Info.plist, iOS 16 deployment target).
4. Run the iOS walkthrough. Fix the issues it finds in shared code unless they are shell-only.

Android is complete and releasable before this phase starts, so Phases A–E are the stopping point if iOS has to wait.

### Phase G — Documentation and governance sweep

Make every instruction file match the code. Each phase above already updates the rules it makes stale;
this sweep catches what is left:

- the root `CLAUDE.md` (stack line, commands, module graph, source paths, the `Res` alias rule, injection rules);
- the six directory `CLAUDE.md` files;
- constitution amendments (Principle I wording for `:shared`, Principle IV's "resolved once in `MainActivity`", Principle V, Platform and Injection);
- `docs/tech-debt.md`: delete "No convention plugins", and update the test-coverage note to the new source-set names;
- `docs/roadmap.md`: the iOS follow-ups stay.

## Project Structure

### Documentation (this feature)

```text
specs/010-kmp-migration/
├── plan.md                         # This file
├── research.md                     # Phase 0: decisions R1–R14
├── data-model.md                   # Phase 1: stores, new types, changed shapes, source-set map
├── quickstart.md                   # Phase 1: per-phase and end-to-end validation
├── contracts/
│   ├── platform-contracts.md       # Capability contracts + exception register (FR-007, FR-008)
│   └── module-contracts.md         # Dependency edges, conventions, source layout
├── checklists/requirements.md      # Spec quality checklist
└── tasks.md                        # Phase 2 (/speckit-tasks — not created here)
```

### Source Code (repository root)

```text
build-logic/                        # NEW included build
└── convention/src/main/kotlin/     # questlog.kmp.library / .kmp.compose / .kmp.feature / .android.application

app/                                # Android shell only (com.android.application)
└── src/main/{java/…/questlog/{QuestLogApp.kt, MainActivity.kt}, res/, AndroidManifest.xml}

shared/                             # NEW cross-platform app root (questlog.kmp.compose)
└── src/
    ├── commonMain/kotlin/…/shared/{QuestLogRoot.kt, QuestLogNavDisplay.kt, QuestLogBottomBar.kt, di/}
    ├── androidMain/kotlin/…/shared/di/
    ├── iosMain/kotlin/…/shared/{MainViewController.kt, di/}
    └── androidHostTest/kotlin/…/shared/KoinGraphTest.kt

iosApp/                             # NEW Xcode project (SwiftUI host, LaunchScreen, Assets, Info.plist)

core/
├── model/        src/commonMain/kotlin
├── navigation/   src/commonMain/kotlin                         (Routes.kt + SavedStateConfiguration)
├── common/       src/{commonMain, androidMain, iosMain}/kotlin
├── domain/       src/commonMain/kotlin,  src/androidHostTest/kotlin
├── network/      src/{commonMain, androidMain, iosMain}/kotlin, src/androidHostTest/kotlin
├── database/     src/{commonMain, androidMain, iosMain}/kotlin, schemas/
├── data/         src/{commonMain, androidMain, iosMain}/kotlin, src/androidMain/res, src/androidHostTest/kotlin
├── ai/           src/main/java                                 (unchanged, Android-only)
├── designsystem/ src/{commonMain, androidMain, iosMain}/kotlin
└── ui/           src/{commonMain, androidMain, iosMain}/kotlin, src/commonMain/composeResources, src/androidHostTest/kotlin

feature/{search, radar, game-detail, lists, wishlist, settings, onboarding}/
└── src/commonMain/{kotlin, composeResources}, src/androidHostTest/kotlin
```

**Structure Decision**:

- **`:app`** keeps its name and `applicationId`.
- **`:shared`** is the single new Gradle module and the only owner of navigation and DI assembly.
- **`iosApp/`** lives at the root, as in JetBrains' template layout.
- **Module names and namespaces** under `core/` and `feature/` are unchanged; only their source sets change.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|---|---|---|
| **`build-logic/` convention plugins**. The constitution's Platform constraint and the root `CLAUDE.md` say there is no `build-logic` and every build file repeats its configuration. **Approved by the owner on 2026-10-08.** | 17 modules each need identical KMP target, host-test, iOS and Android-library configuration. `docs/tech-debt.md` already lists the missing conventions and defers them to exactly this migration | Hand-copying ~30 lines × 17 modules makes every target change a 17-file edit and invites drift. `buildSrc` invalidates the configuration cache on every change |
| **Navigation ownership moves from `:app` to the new `:shared`** (Principle I wording: "`:app` is the only module that knows about navigation"). **Approved by the owner on 2026-10-08.** | AGP 9 forbids KMP in a `com.android.application` module, and iOS must reach the same `entryProvider`. One owner and two edits per route are preserved | A second, iOS-only nav graph duplicates every route (SC-007 fails). The deprecated AGP opt-outs stop building on AGP 10 |
