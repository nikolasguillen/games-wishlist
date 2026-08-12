# Technical debt

Known places where the codebase deviates from the rules in `CLAUDE.md`, plus technical risks worth
tracking. This file exists so those rules can stay strict without an agent either "fixing" legacy code
unasked or copying a deviation as if it were the convention.

**For AI agents:** do not fix anything listed here as a side effect of unrelated work, and do not treat
these patterns as the house style. Mention the relevant item if it blocks you, then move on.

Last audited: 2026-08-12.

## Cleanup pass in progress

An ordered pass over this list is underway on `develop`, one fix per commit; `git log` is the record of
what has already been done. Entries are deleted from this file as they are fixed, so whatever is still
written below is still true. Agreed order for the rest:

1. **IGDB token refresh** — the only remaining item that changes runtime behaviour, so it needs a device
   check and not just a green build.
2. **Room migrations** — turn `exportSchema` on *before* touching any entity, then drop
   `fallbackToDestructiveMigration`. Worth doing together with the comma-separated list columns, since both
   need a schema bump and the destructive fallback is what currently hides the problem.
3. **Release signing** — blocked on the owner generating a keystore; the config reads it from a
   git-ignored `keystore.properties`, like the IGDB credentials.
4. Small and independent, any order: the `R` alias convention, the missing `LICENSE`.

Convention plugins, CI and the test-coverage gaps are deliberately last — see the KMP section in the root
`CLAUDE.md`, since a multiplatform move would rewrite the build logic anyway.

## Rule violations

### One declaration per file

| File | Declarations |
|---|---|
| `core/network/model/IgdbModels.kt` | 9 |
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

- **Release is signed with the debug key**: `app/build.gradle.kts` still uses
  `signingConfigs.getByName("debug")`, so the APK cannot be distributed. Needs a real keystore read from a
  git-ignored `keystore.properties`.
- **No IGDB token refresh**: `IgdbAuthManager` parses `expiresIn` but never uses it, and swallows auth
  failures with `catch (e: Exception) { null }`.
- **Room has no migration path**: `version = 1`, `exportSchema = false`,
  `.fallbackToDestructiveMigration(true)`. Any entity change wipes user data.

## Infrastructure

- **No convention plugins**: no `build-logic`, no `buildSrc`. `compileSdk = 37`, `minSdk = 29` and Java 11
  are repeated by hand in all 14 module build files.
- **No CI** (`.github/` does not exist) and **no static analysis** (no detekt, ktlint, spotless,
  `.editorconfig`, or `lint {}` block).
- **Test coverage gaps**: no tests at all for `:feature:wishlist`, `:core:domain` use cases,
  `:core:database` DAOs, `:core:network`, or `:core:ui` mappers. No Compose UI tests — `ui-test-junit4` is
  wired into `:app` but only the template `ExampleInstrumentedTest` exists. `app/src/test/ExampleUnitTest.kt`
  is also an untouched template.
- **`README.md` claims an MIT license but there is no `LICENSE` file.**
