# Rule inventory

Checkable architectural rules, extracted in precedence order (research R1): root `CLAUDE.md` (**RC**), the six
directory `CLAUDE.md` files (**DC**: `feature/`, `core/{data,network,database,ui,designsystem}/`), the constitution
(**CN**), then `specs/010-kmp-migration/contracts/{module,platform}-contracts.md` (**MC**, **PC**). A rule that appears
in several sources is listed once, under the highest-precedence source, with the others in `also`.

Out of scope (spec Assumptions): UI-layer coding conventions (`UiText`, spacing tokens, `@Immutable`, screen
structure, naming of entities/DAOs, Room schema versioning). They are not structural; they are checked only when a
structural cause turns up. Each rule's outcome is recorded in the `work/*.md` note named in the `note` column.

## R-DEP — Dependency edges

| ID | Statement | Source (also) | Note |
|---|---|---|---|
| R-DEP-01 | `feature/*` depends only on `:core:{common, model, domain, ui, navigation, designsystem}`; never on `:core:data`, `:core:network`, `:core:database` or `:core:ai` | RC "Module graph" (CN I, MC) | dep |
| R-DEP-02 | `:core:ai` is reachable only from `:core:data`, and only from its `androidMain` | RC (CN I, MC "Edges that change") | dep |
| R-DEP-03 | `:core:ai` depends on nothing but the ML Kit SDK and coroutines — not even `:core:model` | RC | dep |
| R-DEP-04 | `:shared` depends on every module except `:core:ai` | RC | dep |
| R-DEP-05 | `:androidApp` depends on `:shared`, `:core:navigation` (deep link → `GameDetailRoute`) and `:core:domain` (scheduler call), and nothing else internal | MC "Edges that change" (RC graph shows `:shared` only) | dep |
| R-DEP-06 | `:core:data` depends on `:core:{common, model, domain, network, database}` | MC | dep |
| R-DEP-07 | `:core:ui` depends on `:core:{common, model, designsystem}`; `:core:network`/`:core:database` on `:core:{common, model}`; `:core:domain` on `:core:{common, model}`; `:core:model` and `:core:navigation` on nothing internal | MC | dep |
| R-DEP-08 | No feature module and no UI module (`:core:ui`, `:core:designsystem`) depends on Koin; ViewModels are registered in `:shared` | DC feature/ (CN "Injection", MC) | dep |
| R-DEP-09 | `:core:data` does not depend on Ktor (transport failures are translated in `:core:network`) | DC core/network, core/data | dep |
| R-DEP-10 | A new module or edge is called out explicitly; every module is registered in `settings.gradle.kts` | RC, CN | build |

## R-PURE — Shared-code purity

| ID | Statement | Source (also) | Note |
|---|---|---|---|
| R-PURE-01 | `commonMain` never imports `android.*` or `java.*` (dates: `kotlinx-datetime`; IO: `kotlinx-io`) | RC "KMP" (CN) | pure |
| R-PURE-02 | A common file never imports `androidx.activity.*` (or another Android-only artifact) | DC core/ui | pure |
| R-PURE-03 | `:core:model` has no Android and no Compose dependency (only `kotlinx-serialization-core`) | RC (CN, MC) | pure |
| R-PURE-04 | Feature modules contain no Android-only calls (`Intent`, `Context`, permissions, pickers); they sit behind an `expect` in `:core:ui` | DC feature/ | pure |
| R-PURE-05 | `:core:data` `commonMain` names no platform type; platform exception types appear only in `RepositoryErrorMapper.android.kt` | DC core/data | pure |
| R-PURE-06 | The Room module uses no `SupportSQLiteOpenHelper` types (`SupportSQLiteDatabase`, `openHelper`, `SupportSQLiteQuery`) | DC core/database | pure |
| R-PURE-07 | New date code uses `kotlinx-datetime`, not `java.time` | CN "KMP" | pure |

## R-CAP — Capability contracts

| ID | Statement | Source (also) | Note |
|---|---|---|---|
| R-CAP-01 | Every file in `androidMain` or `iosMain` implements a capability in `platform-contracts.md`, or is a listed compatibility seam, platform-shell item or `*PlatformModule` | RC "KMP" | cap-mapping |
| R-CAP-02 | A capability is a contract owned by shared code with one implementation per platform | RC (CN) | cap-contracts |
| R-CAP-03 | `expect`/`actual` is for a single function or composable; a service with state is an interface in `commonMain`, bound per platform in that module's `*PlatformModule` | RC | cap-contracts |
| R-CAP-04 | Every `expect` has an `actual` for both Android and iOS | RC (PC) | cap-contracts |
| R-CAP-05 | No platform type (`Context`, `Uri`, `NSURL`, `UIViewController`) appears in a contract signature | PC "Rules" | cap-contracts |
| R-CAP-06 | Test doubles for a contract live in the consuming module's test source set, never in `commonMain` | PC "Rules" | cap-contracts |
| R-CAP-07 | `:core:domain` never gains a platform source set | PC "Rules" | cap-contracts |
| R-CAP-08 | The release-refresh interval is one constant shared by both implementations | PC `ReleaseRefreshScheduler` | dup |
| R-CAP-09 | The capability exception register matches what the code does on iOS (reminders absent, translation `UNSUPPORTED`, in-process refresh) | PC "Capability exception register" | cap-contracts |
| R-CAP-10 | iOS `isConnectivityFailure`/`isTimeoutFailure` test only the network module's two exception types; the `NSError` translation lives in `:core:network` | PC, DC core/data | cap-contracts |
| R-CAP-11 | Platform-only capabilities (background refresh, notifications, on-device translation, splash) are reached through a contract owned by shared code | CN "KMP" | cap-contracts |

## R-PLACE — Where things live

| ID | Statement | Source (also) | Note |
|---|---|---|---|
| R-PLACE-01 | `:shared` is the only module that knows navigation: `NavDisplay`, the single `entryProvider`, back stack, bottom bar | RC (CN I, MC) | place |
| R-PLACE-02 | Feature modules own no nav graph, no entry provider, no per-feature DI module; screens receive lambdas and never navigate | RC (CN I, DC feature/) | place |
| R-PLACE-03 | Adding a route = `NavKey` in `core/navigation/Routes.kt` + `subclass(...)` in `GameNavSavedStateConfiguration.kt` + branch in `QuestLogNavDisplay.kt` | CN I / DC feature/ (MC says two edits) | place |
| R-PLACE-04 | Every ViewModel is registered with `viewModelOf` in `shared/.../di/ViewModelModule.kt`; `initKoin` lives in `shared/.../di/SharedKoin.kt` | RC "Where things live" | place |
| R-PLACE-05 | `SavedStateHandle` is not used | DC feature/ (CN) | place |
| R-PLACE-06 | `KoinGraphTest` exists in `:shared` and runs under the root `test` task | CN "Injection" | place |
| R-PLACE-07 | Splash, edge-to-edge and deep-link parsing live in `androidApp/.../MainActivity.kt`; `:androidApp` holds only the Activity, `Application` and manifest | RC | place |
| R-PLACE-08 | The iOS entry point is `MainViewController` in `shared/src/iosMain`, hosted by `iosApp/` | RC | place |
| R-PLACE-09 | A cross-module `Res` import is aliased `<Module>Res`; a module's own `Res` is imported bare | RC (DC feature/, core/ui) | place |
| R-PLACE-10 | Exactly one `GameRepository` implementation, in `:core:data`; the interface in `:core:domain` | RC (CN IV, DC core/data) | place |
| R-PLACE-11 | `AppResult`/`RepositoryError` live in `:core:model`; `UiText` in `core/ui/model/`; `RepositoryError.toUiText()` is the single error-to-text boundary | RC, CN II | place |
| R-PLACE-12 | `:core:network` returns bare lists and throws; no result wrapper, no retries; HTTP-client types never leave it | DC core/network (CN II) | place |
| R-PLACE-13 | Exactly one exported-schema directory under `core/database/schemas/` | DC core/database | place |
| R-PLACE-14 | IGDB credentials are generated into `IgdbCredentials` under `build/`, never committed | RC (CN) | place |
| R-PLACE-15 | `ElapsedRealtimeSource`, `IgdbAuthManager`, `IgdbAuthService` are `internal` | DC core/network | place |
| R-PLACE-16 | Anything local to a module defaults to `internal` (added during the audit; checked in `vis.md`) | CN "Development Workflow" | vis |

## R-BUILD — Build configuration

| ID | Statement | Source (also) | Note |
|---|---|---|---|
| R-BUILD-01 | Build config lives in `build-logic/` convention plugins; a module applies one and declares only its own dependencies | RC (MC "Convention plugins") | build |
| R-BUILD-02 | namespace, `compileSdk = 37`, `minSdk = 29`, Java 11 come from the plugin, never from a module | RC (MC) | build |
| R-BUILD-03 | `:core:ai` keeps a hand-written Android-library build file; `:androidApp` applies `questlog.android.application` | RC (MC) | build |
| R-BUILD-04 | Features apply `questlog.kmp.feature`, which supplies the six allowed `:core:*` modules | DC feature/ (MC) | build |
| R-BUILD-05 | A root `test` task depends on every multiplatform module's `testAndroidHostTest` | RC "Commands" | build |
| R-BUILD-06 | Each KMP module targets Android plus `iosArm64` and `iosSimulatorArm64`; KSP runs once per target in `:core:database` | MC, DC core/database | build |
| R-BUILD-07 | `kotlin.native.ignoreDisabledTargets` is set so the project builds on Windows | RC "Commands" | build |
| R-BUILD-08 | Coil's network fetcher and per-platform Ktor engine are carried by `:shared`; image modules depend on `coil-compose` only | DC core/ui | build |
| R-BUILD-09 | `core/ui` exposes material-icons and haze via `api(...)`; downstream modules do not re-declare them | DC core/ui | build |
| R-BUILD-10 | Versions in `CLAUDE.md`/constitution header match `gradle/libs.versions.toml` | RC (CN) | doc-claude |

## R-LEFT — Migration leftovers (tracked files only; research R6)

| ID | Statement | Source | Note |
|---|---|---|---|
| R-LEFT-01 | No tracked source folder that no module compiles (`<module>/src/main/java` in a KMP module) | MC "Source layout" | left |
| R-LEFT-02 | Android `res/` strings and drawables moved to `composeResources/` (only `core/data` `androidMain/res` and `androidApp` keep Android resources) | MC "Source layout" | left |
| R-LEFT-03 | No unused entry in the version catalog from removed libraries (Hilt, Retrofit, Moshi, OkHttp logging, Coil 2, `shadowglow`) | 010 `plan.md` "Removed" | left |
| R-LEFT-04 | No hand-written `android {}` block repeating convention-owned settings | MC | build |
| R-LEFT-05 | No template/example files except those already in `docs/tech-debt.md` | `docs/tech-debt.md` | left |
| R-LEFT-06 | Tests moved to `androidHostTest` (no `src/test` left in a KMP module) | MC "Source layout" | left |

## R-DUP — Duplicated platform logic (research R7)

| ID | Statement | Source | Note |
|---|---|---|---|
| R-DUP-01 | Logic that touches no platform API is written once in shared code, not per platform | RC "KMP" (research R7) | dup |

## R-DOC — Documentation accuracy

| ID | Statement | Source | Note |
|---|---|---|---|
| R-DOC-01 | The instruction files (`CLAUDE.md` root + six directory files) state only what the source does | CN Governance | doc-claude |
| R-DOC-02 | `AGENTS.md`, `README.md`, the constitution, `docs/tech-debt.md`, `docs/roadmap.md` and the 010 contracts match the source | CN Governance | doc-other |
| R-DOC-03 | A resolved `docs/tech-debt.md` entry is deleted, not annotated; instruction files are instructions, not a changelog | RC "Directory-specific instructions" | doc-other |
| R-DOC-04 | The "Known deviations" in `docs/tech-debt.md` still exist (no entry for a thing already fixed) | RC | doc-other |

## R-VER — Verification

| ID | Statement | Source | Note |
|---|---|---|---|
| R-VER-01 | `./gradlew :androidApp:assembleDebug` succeeds | RC (CN V) | verification |
| R-VER-02 | `./gradlew test` passes, including `KoinGraphTest`; no test deleted or weakened | RC (CN) | verification |
| R-VER-03 | The iOS framework links (`:shared:linkDebugFrameworkIosSimulatorArm64`) | RC | verification |
| R-VER-04 | The iOS-only tests (`:core:network:iosSimulatorArm64Test`) pass | RC | verification |
| R-VER-05 | The `xcodebuild` simulator build of `iosApp` succeeds | RC | verification |
| R-VER-06 | The minified release variant builds (added during the audit; the plan's five commands only build debug) | RC "Commands" (research R10 extra) | verification |
