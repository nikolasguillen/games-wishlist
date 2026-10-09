# R-PURE — Shared-code purity

Baseline `b982bf11`. Scope: the 414 git-tracked `*/src/commonMain/**/*.kt` files. Method (research R4): grep as the
locator, the iOS compile as the authority. All commands run from the repository root.

## Evidence

| Check | Command (abridged) | Result |
|---|---|---|
| `android.`/`java.`/`javax.` imports | `git ls-files '*/src/commonMain/*.kt' \| xargs grep -nE '^import (android\|java\|javax)\.'` | **0 matches** |
| Fully-qualified platform names without an import | same file list, pattern `android.(os\|content\|app\|net\|util\|graphics\|…)`, `java.(util\|time\|io\|net\|text\|…)`, `javax.` outside imports and comments | **0 matches** |
| `platform.*`/`cocoapods.*` (Apple) imports in `commonMain` | same file list | **0 matches** |
| `androidx.*` imports in `commonMain` | distinct packages | `compose.{foundation,ui,material3,runtime,material,animation}`, `room`, `lifecycle{,.compose,.viewmodel,.viewmodel.compose}`, `datastore.{preferences,core}`, `navigation3.{runtime,ui}`, `sqlite{,.driver,.exec}`, `savedstate.serialization`. **All have Kotlin Multiplatform artifacts at the versions in use.** No `androidx.activity`, `androidx.work`, `androidx.core`, `androidx.appcompat` or `androidx.startup` |
| `core/model` | imports and source sets | 31 files, `commonMain` only; its only non-Kotlin imports are its own package; `build.gradle.kts` declares `kotlinx-serialization-core` and nothing else; resolved classpath is `kotlin-stdlib` + `kotlinx-serialization-core` |
| Feature modules, Android-only calls | `grep -E '\b(Intent\|Context\|Toast\|Activity\|PackageManager\|Uri.parse\|LocalContext)\b'` on `feature/*/src/commonMain` | **0 matches** (share sheet, permission state and cover picker sit behind `:core:ui` `expect`s) |
| `core/data` `commonMain` platform types | `UnknownHostException\|SocketTimeoutException\|ConnectException\|SocketException\|NSError\|NSURL\|WorkManager\|android.` | only 2 KDoc mentions of "WorkManager" (`InProcessReleaseRefreshScheduler.kt:26`, `ReleaseRefreshInterval.kt:7`); no type use |
| `SupportSQLite*`, `openHelper`, `@RawQuery` | all tracked `*.kt` | **0 matches** |
| `java.time` | all tracked `*.kt` | `DateRendering.android.kt` (the Android `actual` of the date-rendering contract, so legitimate) and two KDoc comments. None in `commonMain` code |
| iOS compile (authority) | `:shared:linkDebugFrameworkIosSimulatorArm64`: `compileKotlinIosSimulatorArm64` for every module | **up to date** for all modules: they were compiled from identical inputs, which `commonMain` could not do with an `android.*`/`java.*` reference (see `verification.md`, T019) |

## Rule outcomes

| Rule | Outcome | Evidence |
|---|---|---|
| R-PURE-01 | **pass** | 0 `android.`/`java.`/`javax.` imports and 0 qualified names; iOS compile succeeds |
| R-PURE-02 | **pass** | No `androidx.activity`/`work`/`core`; the `androidx.*` packages above are multiplatform artifacts |
| R-PURE-03 | **pass** | `core/model` build file and imports |
| R-PURE-04 | **pass** | 0 Android-only identifiers in feature `commonMain` |
| R-PURE-05 | **pass** | Only KDoc mentions; the platform exception types are named in `RepositoryErrorMapper.android.kt` |
| R-PURE-06 | **pass** | 0 matches |
| R-PURE-07 | **pass** | `java.time` only in the Android `actual` |

## Leads

- **L4** (`androidx` imports in `shared/.../QuestLogNavDisplay.kt`): the file imports `androidx.compose.*`,
  `androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator` and
  `androidx.navigation3.{runtime,ui}.*` (lines 3-18). All are multiplatform artifacts, and the file has no
  `android.*`, `androidx.activity` or `androidx.work` import. **Dismissed.**

## Candidates

None.
