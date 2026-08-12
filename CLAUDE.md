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

- One `data class` / `sealed interface` / `class` per file.
- UI-layer models carry the `UiModel` suffix. Domain models stay clean.
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

## Known deviations

`docs/tech-debt.md` lists the places where the codebase does **not** follow the rules above, plus known
technical risks. When you encounter one of them: do not silently "fix" it while working on something else,
and do not treat it as the convention to imitate. Mention it and move on unless the fix was asked for.
