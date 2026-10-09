# Contract: Modules, Build Conventions and Dependency Edges

This is what each module may depend on after the migration, and the build API that enforces the shape.
Principle I's rules are preserved; this file lists the only edges that change.

## Dependency graph after the migration

```
:androidApp (android.application) ──► :shared
iosApp (Xcode)             ──► QuestLogShared.framework  (built from :shared)

:shared ──► every :feature:*  and every :core:* except :core:ai
:feature:* ──► :core:{common, model, domain, ui, navigation, designsystem}       (unchanged)
:core:data ──► :core:{common, model, domain, network, database}
:core:data (androidMain only) ──► :core:ai                                       (was: whole module)
:core:ui ──► :core:{common, model, designsystem}                                 (unchanged)
:core:network, :core:database ──► :core:{common, model}                          (unchanged)
:core:domain ──► :core:{common, model}                                           (unchanged)
:core:model, :core:navigation ──► nothing internal                               (unchanged)
```

### Edges that change

| Edge | Before | After | Why |
|---|---|---|---|
| `:androidApp` → `:feature:*`, `:core:*` | direct | **removed**. `:androidApp` depends on `:shared` and `:core:domain` (for `QuestLogApp`'s `ReleaseRefreshScheduler` call). It turns the `questlog://game/{id}` intent into a plain game id for `QuestLogRoot`, so it needs no navigation types. The workers are declared in `:core:data`'s Android Koin module, so `:androidApp` never sees them | Navigation, scaffold and DI assembly move to `:shared` (research R2) |
| `:shared` → features and core | did not exist | new | `:shared` takes over `:androidApp`'s role as the module that knows navigation |
| `:core:data` → `:core:ai` | `implementation` | `androidMain` `implementation` | ML Kit stays Android-only |

**Navigation ownership**: `:shared` is the only module that knows navigation. It holds the single
`entryProvider` in `QuestLogNavDisplay`. `:androidApp` hosts `QuestLogRoot` and owns no route, no
`entryProvider` and no screen. Adding a route still means three edits outside the feature:

- a `NavKey` in `core/navigation/Routes.kt`;
- a `subclass(...)` line for it in `core/navigation/GameNavSavedStateConfiguration.kt`;
- a branch in `:shared`'s `entryProvider`.

## External dependencies allowed per layer (after the migration)

| Module group | May use | Must not use |
|---|---|---|
| `:core:model` | `kotlinx-serialization-core` | Compose, Koin, anything Android |
| `:core:navigation` | `kotlinx-serialization-core`, `navigation3-runtime` (JetBrains) | Koin, Compose UI |
| `:core:common`, `:core:domain` | coroutines, `kotlinx-datetime`, `koin-core` | Compose, Ktor, Room |
| `:core:network` | Ktor client + engines (per source set), `kotlinx-serialization-json`, `koin-core` | Room, Compose |
| `:core:database` | Room runtime, `sqlite-bundled`, `koin-core` | Ktor, Compose |
| `:core:data` | DataStore Preferences, `kotlinx-io`, `koin-core`; `androidMain`: WorkManager, `koin-androidx-workmanager`, `core-ktx`, ML Kit via `:core:ai` | Compose |
| `:core:designsystem`, `:core:ui` | Compose Multiplatform (runtime, foundation, material3, resources), material icons 1.7.3, Haze, Coil 3; `androidMain`: `activity-compose`, `core-ktx` | Koin (UI modules receive dependencies via parameters, as today) |
| `feature/*` | as `:core:ui`, plus JetBrains `lifecycle-viewmodel-compose` / `lifecycle-runtime-compose` | Koin (ViewModels are registered in `:shared`); every module the feature rules already forbid |
| `:shared` | everything above, plus `koin-compose`, `koin-compose-viewmodel`, `navigation3-ui`, `lifecycle-viewmodel-navigation3` (JetBrains) | — |
| `:androidApp` | `:shared`, `activity-compose`, `core-splashscreen`, `koin-android`, `koin-androidx-workmanager`, WorkManager | Hilt (removed) |

`ViewModel`s keep constructor parameters only. Koin resolves them in `:shared`, so feature modules don't
depend on Koin. This mirrors today's rule that features own no DI module.

## Convention plugins (`build-logic/`)

Every library module applies exactly one of the plugins below. No `build.gradle.kts` repeats SDK
levels, targets or the JVM target.

| Plugin id | Applies | Configures | Used by |
|---|---|---|---|
| `questlog.kmp.library` | `org.jetbrains.kotlin.multiplatform`, `com.android.kotlin.multiplatform.library` | `android { compileSdk = 37; minSdk = 29; withHostTest {} }`, JVM target 11, `iosArm64()`, `iosSimulatorArm64()`, `namespace` derived from the project path | all non-UI KMP modules |
| `questlog.kmp.compose` | `questlog.kmp.library`, `org.jetbrains.compose`, `org.jetbrains.kotlin.plugin.compose` | `androidResources { enable = true }`, Compose Multiplatform resources (`packageOfResClass` derived from the namespace) | `:core:designsystem`, `:core:ui`, `:shared` |
| `questlog.kmp.feature` | `questlog.kmp.compose` | `commonMain` dependencies on the six allowed `:core:*` modules and the JetBrains lifecycle artifacts; `androidHostTest` dependencies on JUnit4, MockK, `kotlinx-coroutines-test` | the 7 feature modules |
| `questlog.android.application` | `com.android.application`, `org.jetbrains.kotlin.plugin.compose` | `compileSdk`/`minSdk`/`targetSdk`, Java 11 | `:androidApp` |

**Module-specific additions** stay in the module's own `build.gradle.kts`:

- KSP + Room in `:core:database`;
- the `buildconfig` plugin in `:core:network`;
- `publicResClass = true` in `:core:ui`;
- the framework definition in `:shared`.

**Unchanged**:

- **`:core:ai`** keeps its hand-written `com.android.library` build file, because it is the one Android-only library.
- **Namespaces** stay exactly as they are today (`com.nikolasguillen.questlog.<layer>.<module>`).

## Source layout

| Before | After |
|---|---|
| `<module>/src/main/java/...` | `<module>/src/commonMain/kotlin/...` (shared), `src/androidMain/kotlin/...`, `src/iosMain/kotlin/...` |
| `<module>/src/main/res/values/strings.xml` | `<module>/src/commonMain/composeResources/values/strings.xml` (UI and feature modules) |
| `<module>/src/main/res/drawable/*` | `<module>/src/commonMain/composeResources/drawable/*` |
| `core/data/src/main/res/values/strings.xml` (notification texts) | `core/data/src/androidMain/res/values/strings.xml` (Android-only) |
| `<module>/src/test/java/...` | `<module>/src/androidHostTest/kotlin/...` |
| `androidApp/src/main/AndroidManifest.xml` | unchanged (the only manifest in the repo; permissions, `questlog://game` deep link, `Application`) |

Files are moved with `git mv`, so `git log --follow` keeps their history. Moving a file and changing it
happen in separate commits wherever the change is more than an import.
