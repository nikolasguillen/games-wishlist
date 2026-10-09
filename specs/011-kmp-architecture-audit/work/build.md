# R-BUILD — Build configuration

Baseline `b982bf11`. Evidence: all `build.gradle.kts`, the four convention plugins, `settings.gradle.kts`, root
`build.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`.

## Rule outcomes

| Rule | Outcome | Evidence |
|---|---|---|
| R-BUILD-01 | **pass** | 18 of 19 modules apply exactly one `questlog.*` convention plugin: `kmp.library` ×7 (`core:{model, navigation, common, domain, network, database, data}`), `kmp.compose` ×3 (`core:{designsystem, ui}`, `shared`), `kmp.feature` ×7, `android.application` ×1. `:core:ai` is the one hand-written exception, by design. Modules declare only their own dependencies |
| R-BUILD-02 | **pass** | A `namespace`, `compileSdk`, `minSdk`, `JavaVersion` or `jvmTarget` appears in no KMP module's build file. They appear only in `core/ai/build.gradle.kts` (hand-written by design) and `androidApp` (its own `namespace`, which the application plugin cannot derive). `KmpLibraryConventionPlugin` derives namespaces from the project path (`:feature:game-detail` → `…feature.gamedetail`) |
| R-BUILD-03 | **pass** | `core/ai/build.gradle.kts` uses `alias(libs.plugins.android.library)` with its own `android {}`; `androidApp` applies `questlog.android.application` |
| R-BUILD-04 | **pass** | All 7 feature build files apply `questlog.kmp.feature` and add only their own extras (Coil, `kotlinx-datetime`, haze, back handler) |
| R-BUILD-05 | **pass** | Root `build.gradle.kts` registers `test`, depending on `testAndroidHostTest` of every module that applies `org.jetbrains.kotlin.multiplatform`. `./gradlew test` executed the 14 multiplatform modules plus `:androidApp:test` and `:core:ai:test` (the plain `test` tasks of the two Android-only modules, which Gradle selects by name) |
| R-BUILD-06 | **pass** | `KmpLibraryConventionPlugin` adds `iosArm64()` and `iosSimulatorArm64()` to every KMP module; `core/database` has `kspAndroid`, `kspIosArm64`, `kspIosSimulatorArm64`. `iosX64` is absent on purpose (`research.md:339`) |
| R-BUILD-07 | **pass** | `gradle.properties:28` `kotlin.native.ignoreDisabledTargets=true` |
| R-BUILD-08 | **pass** | `shared/build.gradle.kts`: `coil3.network.ktor3` in `commonMain`, `ktor.client.okhttp` in `androidMain`, `ktor.client.darwin` in `iosMain`; `core/ui`, `feature/search|lists|wishlist` depend on `coil3.compose` only |
| R-BUILD-09 | **violated, minor** | `core/ui` exposes `material-icons-core`, `material-icons-extended` and `haze` through `api(...)`; `shared/build.gradle.kts:44` re-declares `jetbrains.material.icons.extended`, which `QuestLogBottomBar.kt` uses but already receives transitively. See C-BUILD-1 |
| R-BUILD-10 | see `doc-claude.md` | Version header vs catalog |

## Observations (not findings)

- `gradle.properties:22` `android.disallowKotlinSourceSets=false` makes Gradle print "The option setting … is
  experimental" on every build. It predates the migration (commit `729ffb14`), has no comment or doc, and may no longer
  be needed now that KSP runs only in a KMP library module. Whether it can be removed has not been tested here, since
  that would change the build. Left as an observation; low.
- Gradle's dependency report shows `:core:network`'s implementation dependencies as children of the project edge in
  `allSourceSetsCompileDependenciesMetadata` (see `dep.md`). That is Gradle's metadata view and not a declared edge.

## Candidates

| ID | Title | Detail |
|---|---|---|
| C-BUILD-1 | `:shared` re-declares `material-icons-extended`, which `:core:ui` already exposes with `api(...)` | `shared/build.gradle.kts:44` vs `core/ui/build.gradle.kts:22-25` and the rule in `core/ui/CLAUDE.md` ("Build notes"): "feature modules get them transitively — do not re-declare them downstream". Redundant but harmless. Severity low, follow-up, size S |
| C-BUILD-2 | Comments in the root build files still describe the migration as in progress or the pre-migration module count | `gradle.properties:12` "14 modules with no cross-dependencies at the leaves" (there are 19); root `build.gradle.kts` "while modules are converted one by one" and "skip the Android modules not yet converted" (all modules are converted). Misleading comments only. Severity low, follow-up, size S |
