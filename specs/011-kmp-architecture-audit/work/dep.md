# R-DEP — Dependency edges

Baseline `b982bf11`. Evidence: every `build.gradle.kts` (19 modules + root), the four convention plugins in
`build-logic/convention/src/main/kotlin/`, `settings.gradle.kts`, and Gradle's resolved graph
(`./gradlew <all 19 modules>:dependencies`, log in `build/audit/deps-all.log`, configurations
`androidCompileClasspath`, `androidRuntimeClasspath`, `allSourceSetsCompileDependenciesMetadata`,
`debug`/`releaseCompileClasspath`, `debug`/`releaseRuntimeClasspath`).

## Edge matrix (project edges only; declared = in a build file or convention plugin, resolved = compile classpath)

| Module | Plugin | Declared project edges | Resolved compile-classpath project edges |
|---|---|---|---|
| `:androidApp` | `questlog.android.application` | `:shared`, `:core:domain` | `:shared`, `:core:domain` |
| `:shared` | `questlog.kmp.compose` | 9 core (all except `:core:ai`) + 7 features | the same 16 |
| `:feature:{radar,search,game-detail,lists,wishlist,settings,onboarding}` | `questlog.kmp.feature` | none in the module; the plugin injects `:core:{common, model, domain, ui, navigation, designsystem}` | exactly those six, in all 7 modules |
| `:core:ui` | `questlog.kmp.compose` | `:core:{common, designsystem, model}` | the same |
| `:core:designsystem` | `questlog.kmp.compose` | none | none |
| `:core:navigation` | `questlog.kmp.library` | none | none |
| `:core:data` | `questlog.kmp.library` | `:core:{common, model, domain, network, database}`; `androidMain`: `:core:ai` | those six (the metadata view unions `androidMain`) |
| `:core:domain` | `questlog.kmp.library` | `:core:{common, model}` | the same |
| `:core:network` | `questlog.kmp.library` | `:core:{common, model}` | the same |
| `:core:database` | `questlog.kmp.library` | `:core:{common, model}` | the same |
| `:core:common` | `questlog.kmp.library` | `:core:model` | the same |
| `:core:model` | `questlog.kmp.library` | none (`kotlinx-serialization-core` only) | none |
| `:core:ai` | hand-written `com.android.library` | none (ML Kit GenAI, `kotlinx-coroutines-android`) | none |

`settings.gradle.kts` includes exactly these 19 modules, and each has a build file.

## Rule outcomes

| Rule | Outcome | Evidence |
|---|---|---|
| R-DEP-01 | **pass** | The 7 feature modules resolve to exactly the six allowed edges (compile and metadata views). No feature has an edge to `:core:data`, `:core:network`, `:core:database` or `:core:ai`; `KmpFeatureConventionPlugin` documents that those are deliberately absent |
| R-DEP-02 | **pass** | `:core:ai` is declared only in `core/data/build.gradle.kts` (`androidMain`) and resolves only on `:core:data`'s compile classpath. It reaches the *runtime* classpath of `:androidApp` transitively through `:core:data`, which is the intended packaging (its `consumer-rules.pro` carries the R8 rule) |
| R-DEP-03 | **pass** | `core/ai/build.gradle.kts`: only `mlkit-genai-prompt` and `kotlinx-coroutines-android`; no `:core:model` |
| R-DEP-04 | **pass** | `:shared` declares 9 core modules + 7 features and not `:core:ai` (comment in the file says why) |
| R-DEP-05 | **pass** (edges); docs differ | Build: `:shared` + `:core:domain`. `MainActivity` imports only `shared.*` and `QuestLogApp` imports `core.domain.radar.ReleaseRefreshScheduler`; the deep link is passed to `QuestLogRoot` as a plain `Int` (`pendingDeepLinkGameId`), so `:androidApp` has no need for `:core:navigation`. See C-DEP-1 |
| R-DEP-06 | **pass** | Declared and resolved match `module-contracts.md` |
| R-DEP-07 | **pass** | Table above |
| R-DEP-08 | **pass** | No `org.koin` import in `feature/`, `core/ui`, `core/designsystem`, `core/navigation` or `core/model`; no Koin on any feature/UI module's `androidCompileClasspath`. Koin is imported only in `androidApp`, `shared`, `core/{common,data,database,domain,network}` |
| R-DEP-09 | **pass** | `:core:data` declares no Ktor artifact, has 0 Ktor artifacts on `androidCompileClasspath`, and no `io.ktor` import exists outside `core/network`, `shared` and `androidApp` |
| R-DEP-10 | **pass** | 19 `include(...)` lines, 19 build files, no module outside the settings file |

## Observation, not a finding

In `allSourceSetsCompileDependenciesMetadata` (the multiplatform metadata view that the IDE imports from), Gradle
lists `:core:network`'s *implementation* dependencies (Ktor, Koin, serialization) as children of the project edge under
`:core:data`, and `:core:domain`'s Koin under `:feature:*` and `:core:ui`. That view can make such types *visible* to a
dependent's `commonMain` in the IDE even when no module declares the dependency. The actual `androidCompileClasspath`
shows none of them, and a source scan finds no `io.ktor`, `org.koin` (outside the allowed modules), `androidx.room` or
`androidx.sqlite` import crossing a boundary. So the rule is respected in practice. Nothing enforces it if someone
adds such an import later and relies on the metadata view; the iOS compile (`R-VER-03`) would be the first thing to
reject an unresolved reference, and an Android compile would fail at once. No candidate.

## Candidates

| ID | Title | Detail |
|---|---|---|
| C-DEP-1 | `module-contracts.md` lists an `:androidApp → :core:navigation` edge that does not exist and is not needed | `specs/010-kmp-migration/contracts/module-contracts.md:26` says `:androidApp` depends on `:shared`, `:core:navigation` and `:core:domain`. The build declares `:shared` and `:core:domain` only; `platform-contracts.md` ("Deep link to a game") already says `MainActivity` hands `QuestLogRoot` a game id. Documentation drift in a migration spec. Severity low, follow-up. (Same source drift: the root `CLAUDE.md` graph shows `:androidApp → :shared` without the `:core:domain` edge; recorded in `doc-claude.md`.) |
