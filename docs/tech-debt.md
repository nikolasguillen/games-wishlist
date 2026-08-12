# Technical debt

Known places where the codebase deviates from the rules in `CLAUDE.md`, plus technical risks worth
tracking. This file exists so those rules can stay strict without an agent either "fixing" legacy code
unasked or copying a deviation as if it were the convention.

**For AI agents:** do not fix anything listed here as a side effect of unrelated work, and do not treat
these patterns as the house style. Mention the relevant item if it blocks you, then move on.

Last audited: 2026-08-12.

## Rule violations

### One declaration per file

| File | Declarations |
|---|---|
| `core/network/model/IgdbModels.kt` | 9 |
| `core/network/model/NetworkGame.kt` | 7 (dead code, see below) |
| `core/database/entity/GameEntity.kt` | 5 (`RelatedGameEntity`, `GameWithAllDetails`, `GamePlatformWithDetails`, `GameCompanyWithDetails`) |
| `core/navigation/Routes.kt` | 5 |
| `core/database/entity/ListEntity.kt` | 3 (`ListWithGameCount`, `GameListCrossRef`) |
| `core/model/Constants.kt` | 2 (`WishlistConstants`, `RelationType`) |
| `feature/search/model/SearchSuggestionUiModel.kt` | 2 |

The rule *is* followed inside `feature/*/model/` for state/event/effect types.

### No comma-separated list columns

`core/database/util/Converters.kt` stores `List<String>` as a comma-joined string for
`GameEntity.artworks` and `GameEntity.engines` — the exact pattern the persistence rule forbids. Fixing it
requires either CrossRef tables or a JSON column, plus a destructive schema change.

### UiText for all user-facing text

- `core/ui/model/GameItemUiModel.kt` uses plain `String` for `name`, `developer`, `releaseYear`,
  `coverImage`.
- `SearchHistoryUiModel.queries` is `List<String>`.

`feature/lists/WishlistListUiModel` and `WishlistUiState.listName` do use `UiText` correctly.

### `@property` KDoc on models

Present only in `core/model/Game.kt` and `core/network/model/IgdbModels.kt`. Missing in `AppResult.kt`,
`RepositoryError.kt`, `WishlistList.kt`, `SearchResult.kt`, `GameStatus.kt`, `Constants.kt`.

### `@Preview` on every composable

Solid in `core/ui` and `feature/wishlist`. Several files in `feature/search/components/` and
`feature/game-detail/components/` have none.

### Uniform UDF

- `feature/lists` exposes a bare `lists: StateFlow<List<WishlistListUiModel>>` and a `createList(...)`
  method instead of a `UiState` + `onEvent(UiEvent)` pair.
- `feature/wishlist` has no `ContentState`; it renders `EmptyPage` from `sections.isEmpty()`.
- `feature/search` is the least converted to `internal` visibility (4 declarations, vs 33 in game-detail);
  its `SearchUiState` is public and not `@Immutable`.

### Misc

Cross-module `R` import aliases are not standardized: `CoreUiR`, `UiR`, `SearchR`, `DatabaseR` are all in
use for the same purpose.

## Technical risks

- **Release build type is not release-ready**: `app/build.gradle.kts` uses the **debug signing config** and
  `isMinifyEnabled = false`.
- **Compose tooling ships in release**: `feature/search` and `feature/wishlist` declare
  `androidx.compose.ui.tooling` as `implementation`, while `core/ui` correctly uses `debugImplementation`.
  (`feature/lists` and `feature/game-detail` only pull `ui.tooling.preview`, which is fine.)
- **No IGDB token refresh**: `IgdbAuthManager` parses `expiresIn` but never uses it, and swallows auth
  failures with `catch (e: Exception) { null }`.
- **Room has no migration path**: `version = 1`, `exportSchema = false`,
  `.fallbackToDestructiveMigration(true)`. Any entity change wipes user data.
- **Dead code**: `core/network/model/NetworkGame.kt` (7 `Network*` DTOs) is a RAWG-era leftover with zero
  references.

## Infrastructure

- **No convention plugins**: no `build-logic`, no `buildSrc`. `compileSdk = 37`, `minSdk = 29` and Java 11
  are repeated by hand in all 14 module build files.
- **No CI** (`.github/` does not exist) and **no static analysis** (no detekt, ktlint, spotless,
  `.editorconfig`, or `lint {}` block).
- **`gradle.properties` is unoptimized**: parallel builds commented out, no configuration cache, no build
  cache, no `nonTransitiveRClass`.
- **Unused dependencies pulled into `:app`**: CameraX, accompanist-permissions,
  play-services-location.
- **Test coverage gaps**: no tests at all for `:feature:wishlist`, `:core:domain` use cases,
  `:core:database` DAOs, `:core:network`, or `:core:ui` mappers. No Compose UI tests — `ui-test-junit4` is
  wired into `:app` but only the template `ExampleInstrumentedTest` exists. `app/src/test/ExampleUnitTest.kt`
  is also an untouched template.
- **`README.md` claims an MIT license but there is no `LICENSE` file.**
