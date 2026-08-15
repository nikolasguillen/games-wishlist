# CLAUDE.md

Modular Android app for tracking a videogame wishlist, backed by the IGDB API.

Kotlin 2.4.10 · AGP 9.3.1 · Gradle 9.5.1 (JVM toolchain 21) · compileSdk/targetSdk 37 · minSdk 29 · Java 11
Jetpack Compose (BOM 2026.06.01) · Hilt 2.60.1 · Room 2.8.4 (KSP) · Retrofit 3 + Moshi · Navigation 3 · Coil 2

## Commands

```bash
./gradlew :app:assembleDebug                                    # full debug build
./gradlew :feature:search:compileDebugKotlin --console=plain -q  # fast single-module check
./gradlew test                                                   # all JVM unit tests
./gradlew :core:data:testDebugUnitTest --console=plain -q        # single-module tests
```

Prefer a single-module `compileDebugKotlin` for quick feedback; only run `:app:assembleDebug` when the
change spans modules or touches DI wiring.

This project is developed on both macOS and Windows. The commands above use the Unix wrapper; on Windows
(PowerShell) use the batch wrapper instead — `.\gradlew.bat :app:assembleDebug`. Check the platform before
suggesting a command.

- **No detekt, ktlint, spotless, or CI are configured.** Do not propose a lint gate that does not exist.
- Never edit anything under `**/build/generated/**`.
- IGDB credentials (`IGDB_CLIENT_ID`, `IGDB_CLIENT_SECRET`) live in `local.properties` and are injected as
  `BuildConfig` fields by `core/network/build.gradle.kts`. Never commit them or move them into source.

## Module graph and dependency rules

14 modules, all under the `com.example.gameswishlist.*` namespace. Sources live in `src/main/java/`.

```
:app  →  everything
:feature:{search, game-detail, lists, wishlist}
:core:{common, model, network, database, data, domain, ui, designsystem, navigation}
```

These boundaries are load-bearing — check them before adding a dependency:

- **`:app` is the only module that knows about navigation.** Feature modules own no nav graph.
- **`feature/*` depends only on** `:core:common`, `:core:model`, `:core:domain`, `:core:ui`,
  `:core:navigation`, `:core:designsystem`. **Never** on `:core:data`, `:core:network`, or `:core:database`.
- `:core:model` has no Android and no Compose dependency (only `kotlinx-serialization-core`). Keep it that way.
  `:core:model` and `:core:navigation` are the only modules without Hilt/KSP.
- **There is no `build-logic` or `buildSrc`.** Every `build.gradle.kts` repeats `compileSdk = 37`,
  `minSdk = 29`, and Java 11 by hand. When adding a module, copy `feature/search/build.gradle.kts`
  (feature) or `core/data/build.gradle.kts` (core) and register it in `settings.gradle.kts`.

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
- A cross-module `R` import is always aliased **`<Module>R`**: `CoreUiR` for `:core:ui`, `SearchR` for
  `:feature:search`, `DatabaseR` for `:core:database`. Never a bare `R` or a shortened alias — the point is
  that the reader can tell which module owns the resource.
- **User-facing text is always `UiText`** (`core/ui/model/UiText.kt`) in UiState and UiModels, never `String`.
  Strings belong in `strings.xml`; never hardcode them in models, mappers, or logic. Domain models hold raw
  data or enums only — display labels are resolved in the UI layer.
- Before writing a `dp` literal, look for the token in `MaterialTheme.spacing`.
- Dialogs: use `CustomAlertDialog` from `:core:ui`, never Material's `AlertDialog`.
- Filter and sort by **ID, never by name**. That logic belongs in a ViewModel, UseCase, or Mapper —
  never in a composable. Composables render and emit events, nothing else.
- Collect flows with `collectAsStateWithLifecycle()`.
- **Before writing a modifier or a component, check whether it already exists** in
  `core/ui/util/Modifiers.kt` and `core/ui/component/`.
- Comments, KDoc, and all internal documentation are written **in English**, regardless of the language of
  the conversation.

## Where things live

| What | Where |
|---|---|
| `AppResult`, `RepositoryError` | `core/model/` (not `core/data`) |
| `UiText` | `core/ui/model/UiText.kt` |
| Spacing / color / typography tokens | `core/designsystem/theme/` |
| Shared composables, reusable modifiers | `core/ui/component/`, `core/ui/util/Modifiers.kt` |
| Nav routes (`NavKey`) | `core/navigation/Routes.kt` |
| Navigation entry point (`NavDisplay`) | `app/src/main/java/com/example/gameswishlist/MainActivity.kt` |
| Shared UI constants | `core/ui/util/Constants.kt` (`object UiConstants`) |
| Network↔domain↔entity mappers | `core/data/mapper/GameMapper.kt` |

## Directory-specific instructions

Additional `CLAUDE.md` files are loaded automatically when you work inside these directories:
`feature/`, `core/data/`, `core/network/`, `core/database/`, `core/ui/`, `core/designsystem/`.
Read them before changing code in those modules.

**These files are instructions, not a changelog.** When a change deletes the thing a rule was about,
delete the rule in the same commit — do not rewrite it into a note saying the thing is gone. Check whether
a neighbouring rule already covers what is left. The same applies to `docs/tech-debt.md`: remove a
resolved entry instead of annotating it as fixed. History belongs in commit messages.

## Planned direction: Kotlin Multiplatform

The owner intends to migrate this project to KMP (with Compose Multiplatform for the UI) at some point.
No timeline, no work started — the repo is Android-only today, with zero `commonMain` source sets.

**Do not start KMP restructuring, and do not swap libraries pre-emptively.** The rule is to avoid choices
that would be expensive to reverse, not to migrate early:

- Keep `:core:model` free of Android and Compose dependencies. It is already the most KMP-ready module and
  the natural first candidate for `commonMain`.
- Retrofit (`:core:network`) and Hilt are JVM/Android-only. Their KMP counterparts would be Ktor and Koin.
  Leave both alone until the migration is actually scoped.
- Room is on the legacy `SupportSQLiteOpenHelper` path. Moving to the driver-based API
  (`BundledSQLiteDriver`) belongs to that migration — see `core/database/CLAUDE.md`.

When a decision would be hard to undo after a KMP move, say so and let the owner choose.

## Known deviations

`docs/tech-debt.md` lists the places where the codebase does **not** follow the rules above, plus known
technical risks. When you encounter one of them: do not silently "fix" it while working on something else,
and do not treat it as the convention to imitate. Mention it and move on unless the fix was asked for.
