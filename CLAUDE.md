# CLAUDE.md

Modular Android app for tracking a videogame wishlist, backed by the IGDB API.

Kotlin 2.4.20 · AGP 9.4.1 · Gradle 9.7.1 (JVM toolchain 21) · compileSdk/targetSdk 37 · minSdk 29 · Java 11
Compose Multiplatform 1.12.1 (Material 3: JetBrains 1.12.0-alpha03, Jetpack 1.5.0-beta01 on Android) · Koin 4.2.2 · Room 2.8.5 (KSP, bundled SQLite driver) · Ktor 3 + kotlinx.serialization · Navigation 3
Coil 3 · WorkManager + `koin-androidx-workmanager` (Radar's release-date refresh)

## Git

**Project-specific override of the owner's global CLAUDE.md.** The global rule forbidding a
`Co-Authored-By: Claude` trailer does not apply here: commit messages in this repository **do** end with
that trailer. This applies only to the commit trailer — PR bodies still omit the "Generated with Claude
Code" footer, per the global rule.

Commit messages follow `type(scope): subject`, enforced by `.githooks/commit-msg`. Enable it once per clone
with `git config core.hooksPath .githooks`.

- **Types:** `feat`, `fix`, `refactor`, `perf`, `docs`, `test`, `style`, `build`, `chore`.
- **Scope** is optional; omit it when a commit spans several modules. When present it is a module name
  without the `core`/`feature` prefix (`ui`, `data`, `game-detail`, …) or `deps` / `docs`. The authoritative
  list is `SCOPES` in the hook.
- **Subject:** English, imperative, lowercase first letter, no trailing period, first line at most 72
  characters. Breaking changes use `!` (`feat(model)!: ...`).
- **Body** is optional, separated by a blank line, and explains *why*; wrap it at 72.
- Spec Kit commits keep their `[Spec Kit] ...` prefix and are exempt from the type/scope check.
- Merge, revert, `fixup!` and `squash!` messages are not checked.

## Commands

```bash
./gradlew :app:assembleDebug                                    # full debug build
./gradlew :feature:search:compileDebugKotlin --console=plain -q  # fast single-module check (Android module)
./gradlew :core:domain:compileCommonMainKotlinMetadata -q       # fast check of a multiplatform module's commonMain
./gradlew test                                                   # all JVM unit tests
./gradlew :core:data:testDebugUnitTest --console=plain -q        # single-module tests (Android module)
./gradlew :core:domain:testAndroidHostTest --console=plain -q    # single-module tests (multiplatform module)
```

`./gradlew test` covers both kinds: a root `test` task depends on every multiplatform module's
`testAndroidHostTest`. `allTests` is not the project's command — it also runs the iOS simulator tests and
skips the Android modules that are not converted yet.

Prefer a single-module `compileDebugKotlin` for quick feedback; only run `:app:assembleDebug` when the
change spans modules or touches DI wiring.

This project is developed on both macOS and Windows. The commands above use the Unix wrapper; on Windows
(PowerShell) use the batch wrapper instead — `.\gradlew.bat :app:assembleDebug`. Check the platform before
suggesting a command.

- **No detekt, ktlint, spotless, or CI are configured.** Do not propose a lint gate that does not exist.
- Never edit anything under `**/build/generated/**`.
- IGDB credentials (`IGDB_CLIENT_ID`, `IGDB_CLIENT_SECRET`) live in `local.properties` and are generated into
  `internal object IgdbCredentials` (under `build/`, never committed) by `core/network/build.gradle.kts`.
  Never commit them or move them into source.

## Module graph and dependency rules

19 modules, all under the `com.nikolasguillen.questlog.*` namespace. Sources live in `src/commonMain/kotlin/` and `src/androidMain/kotlin/` (`:core:ai` and `:app` keep `src/main/java/`).

```
:app  →  :shared  →  everything
:feature:{search, radar, game-detail, lists, wishlist, settings, onboarding}
:core:{common, model, network, database, data, domain, ui, designsystem, navigation, ai}
```

These boundaries are load-bearing — check them before adding a dependency:

- **`:shared` is the only module that knows about navigation.** It owns the back stack, the bottom bar, the single
  `entryProvider` and the Koin assembly; `:app` is only the Android entry point around it (the activity, the
  `Application`, the manifest). Feature modules own no nav graph. `:shared` depends on every other module except
  `:core:ai`.
- **`feature/*` depends only on** `:core:common`, `:core:model`, `:core:domain`, `:core:ui`,
  `:core:navigation`, `:core:designsystem`. **Never** on `:core:data`, `:core:network`, `:core:database`,
  or `:core:ai`.
- **`:core:ai` is reachable only from `:core:data`.** It wraps ML Kit's on-device GenAI client
  (`GeminiNanoClient`) and depends on nothing but that SDK and coroutines — not even `:core:model`.
  The KMP migration leaves it Android-only, since ML Kit GenAI has no multiplatform counterpart; see
  `docs/roadmap.md`.
- `:core:model` has no Android and no Compose dependency (only `kotlinx-serialization-core`). Keep it that way.
- **Build configuration lives in `build-logic/` convention plugins** (`questlog.kmp.library`, …). A module
  applies the plugin that matches its kind and declares only its own dependencies: the namespace,
  `compileSdk = 37`, `minSdk = 29` and Java 11 come from the plugin, never from the module. The migration is
  converting modules one by one (`specs/010-kmp-migration`). `:core:ai` keeps a hand-written build file on
  purpose, being Android-only; `:app` applies `questlog.android.application`, because AGP 9 does not allow an
  application module to be multiplatform. Register a new module in
  `settings.gradle.kts`.

## Data flow

```
ViewModel → UseCase → GameRepository → IgdbApiService / Room DAO → mapper → core:model
```

Concrete chain for search: `feature/search/SearchViewModel.kt` →
`core/domain/usecase/search/SearchGamesUseCase.kt` → `core/data/repository/GameRepositoryImpl.kt` →
`core/network/IgdbApiService.kt`.

`GameRepository` (the interface) lives in `core/domain/repository/`; the single implementation lives in
`core/data/`. Use cases are plain classes with `operator fun invoke(...)` — there is no base `UseCase` type.

## Non-negotiable rules

- One `data class` / `sealed interface` / `class` per file. **One exception:** a `sealed` hierarchy keeps
  its direct implementations in the same file — they are one closed set and only mean anything together.
  That is `core/navigation/Routes.kt` and the state/event/effect files under `feature/*/model/`. The
  exception does not extend to types that merely live nearby: a cross-ref, a relation POJO or a second
  model gets its own file.
- UI-layer models carry the `UiModel` suffix. Domain models stay clean.
- A cross-module `Res` import is always aliased **`<Module>Res`** — `CoreUiRes` for `:core:ui`, and the same
  shape for any other module that ends up exporting resources. Never a bare `Res` or a shortened alias: the
  point is that the reader can tell which module owns the resource. A module's **own** `Res` is imported
  bare, because there is nothing to disambiguate and the alias only adds noise. `:core:ui` is currently
  the only module whose resources are read from outside it (its `publicResClass` is on). Every resource
  also needs its own import; `core/ui/CLAUDE.md` lists the ways Compose resources differ from Android's.
- **Text that can come from `strings.xml` is `UiText`** (`core/ui/model/UiText.kt`) in UiState and
  UiModels — anything formatted through a resource (`platforms_format`), given a resource fallback
  (`release_date_tba`), or derived from an enum (`GameStatus.toLabelUiText()`). A value that can only
  ever come from the data source — a game's name, a studio, a year, the user's own search queries — stays
  `String`: `UiText.DynamicString` around it buys nothing and only adds an unwrap at the call site.
  The test is *could this string ever be a resource?*, not *is it shown on screen?* When that test is a
  genuine coin flip, pick `UiText`: widening a `String` later means touching the model, the mapper, every
  composable that reads it and every preview that builds it, while narrowing a `UiText` is deleting a
  wrapper.
- Never hardcode display text in a model, mapper, composable or any other logic — it belongs in
  `strings.xml`. Domain models hold raw data or enums only; labels are resolved in the UI layer.
- Before writing a `dp` literal, look for the token in `MaterialTheme.spacing`.
- Dialogs: use `CustomAlertDialog` from `:core:ui`, never Material's `AlertDialog`.
- Filter and sort by **ID, never by name**. That logic belongs in a ViewModel, UseCase, or Mapper —
  never in a composable. Composables render and emit events, nothing else.
- Collect flows with `collectAsStateWithLifecycle()`.
- **Before writing a modifier or a component, check whether it already exists** in
  `core/ui/util/modifiers/` and `core/ui/component/`.
- Comments, KDoc, and all internal documentation are written **in English**, regardless of the language of
  the conversation.

## Where things live

| What | Where |
|---|---|
| `AppResult`, `RepositoryError` | `core/model/` (not `core/data`) |
| `UiText` | `core/ui/model/UiText.kt` |
| Spacing / color / typography tokens | `core/designsystem/theme/` |
| Shared composables, reusable modifiers | `core/ui/component/`, `core/ui/util/modifiers/` |
| Nav routes (`NavKey`) | `core/navigation/Routes.kt` |
| Navigation entry point (`NavDisplay` + the single `entryProvider`) | `shared/src/commonMain/kotlin/com/nikolasguillen/questlog/shared/QuestLogNavDisplay.kt` |
| Theme resolution, scaffold, bottom bar and back stack | `shared/.../QuestLogRoot.kt`, `shared/.../QuestLogBottomBar.kt` |
| Koin assembly (`initKoin`) and every ViewModel binding | `shared/.../di/SharedKoin.kt`, `shared/.../di/ViewModelModule.kt` |
| Splash screen, edge-to-edge, deep link parsing | `app/.../MainActivity.kt` |
| Shared UI constants | `core/ui/util/Constants.kt` (`object UiConstants`) |
| Network↔domain↔entity mappers | `core/data/mapper/GameMapper.kt` |
| `GameDescriptionTranslator` | `core/domain/translation/` (impl in `core/data/translation/`) |

## Directory-specific instructions

Additional `CLAUDE.md` files are loaded automatically when you work inside these directories:
`feature/`, `core/data/`, `core/network/`, `core/database/`, `core/ui/`, `core/designsystem/`.
Read them before changing code in those modules.

**These files are instructions, not a changelog.** When a change deletes the thing a rule was about,
delete the rule in the same commit — do not rewrite it into a note saying the thing is gone. Check whether
a neighbouring rule already covers what is left. The same applies to `docs/tech-debt.md`: remove a
resolved entry instead of annotating it as fixed. History belongs in commit messages.

## Kotlin Multiplatform migration

The owner has decided to migrate the whole project to KMP, with Compose Multiplatform for the UI. The scope
is in `specs/010-kmp-migration/spec.md`: **iOS is the only added platform** (no desktop, no web), and iOS
launches **without** release reminders and on-device translation, whose entry points are hidden there (the
iOS follow-ups are in `docs/roadmap.md`). Every module except `:core:ai` and `:app` is multiplatform now
(Android target, and iOS targets that compile once Phase 4's `actual`s exist); the migration is sequenced by
that feature's plan and tasks, so follow them rather than restructuring ad hoc.

- **Android must build and pass its tests at every commit** (`./gradlew :app:assembleDebug`,
  `./gradlew test`). Never delete or weaken a test to get there. Each step must be one you could pause on.
- **Library swaps are decided in the plan, not on the side.** Do not swap one outside the task that calls
  for it.
- **The module-boundary rules above still hold** after the move. A new module or dependency edge must be
  called out explicitly.
- Keep `:core:model` free of Android and Compose dependencies. It is the natural first candidate for
  `commonMain`.
- Platform-only capabilities (background refresh, notifications, on-device translation, splash screen) are
  reached through a contract owned by shared code, with one implementation per platform — the shape
  `ReleaseRefreshScheduler` and `GameDescriptionTranslator` already have.
- Use `kotlinx-datetime`, not `java.time`, for any new date code.

When a decision would be hard to undo, say so and let the owner choose.

## Known deviations

`docs/tech-debt.md` lists the places where the codebase does **not** follow the rules above, plus known
technical risks. When you encounter one of them: do not silently "fix" it while working on something else,
and do not treat it as the convention to imitate. Mention it and move on unless the fix was asked for.

## Planned features

`docs/roadmap.md` holds the agreed shape and build order of the features that do not exist yet (Discover
feed, Radar timeline, release notifications), along with the design decisions behind them. Read it before
proposing an alternative structure for any of those — the trade-offs were already weighed.
