# Technical debt

Known places where the codebase deviates from the rules in `CLAUDE.md`, plus technical risks worth
tracking. This file exists so those rules can stay strict without an agent either "fixing" legacy code
unasked or copying a deviation as if it were the convention.

**For AI agents:** do not fix anything listed here as a side effect of unrelated work, and do not treat
these patterns as the house style. Mention the relevant item if it blocks you, then move on.

**When an item is fixed, delete it.** No "fixed in", no note explaining what changed, no leftover row —
`git log` is the history, this file is only the present. It is meant to shrink until it is empty, and then
to be deleted.

Last audited: 2026-08-16.

## Cleanup pass in progress

An ordered pass over this list is underway on `develop`, one fix per commit; `git log` is the record of
what has already been done. Entries are deleted from this file as they are fixed, so whatever is still
written below is still true. Agreed order for the rest:

1. **Uniform UDF** — `feature/lists` is the last module left.
2. **The `@Immutable` audit** — adjacent to the UDF work, so it goes right after it.
3. **Stale cross-ref rows** — a behaviour change, so it waits for its own commit.
4. **The `R` alias convention** — small and mechanical, any time.
5. **Release signing** — on hold: blocked on the owner generating a keystore, and not being chased in the
   meantime.

Convention plugins, CI and the test-coverage gaps are deliberately last — see the KMP section in the root
`CLAUDE.md`, since a multiplatform move would rewrite the build logic anyway.

## Rule violations

### Uniform UDF

`feature/lists` exposes a bare `lists: StateFlow<List<WishlistListUiModel>>` and a `createList(...)`
method instead of a `UiState` + `onEvent(UiEvent)` pair.

### `R` alias on cross-module imports

Eight files import `com.example.gameswishlist.core.ui.R` bare from inside a feature module, so the reader
cannot tell which module owns the resource: `GameDescriptionCard`, `GameDetailPersonalCard`,
`GameReleaseInfoCard`, `ListSelectorSheet`, `DetailErrorLoadingWrapper` and `mapper/GameDetailUiMapper` in
`feature/game-detail`, `SearchBars` and `SuggestionRow` in `feature/search`. They all need `as CoreUiR`.
A module importing its own `R` bare is correct and must stay that way.

## Technical risks

- **`@Immutable` is applied by feel**: only `UiState` is covered by a rule, so every other state holder
  and UI model got the annotation or not depending on who wrote it. Missing it where it belongs costs
  recomposition; claiming it where a field is genuinely mutable is a correctness bug, so each one has to
  be looked at rather than annotated in bulk. Still unannotated: `GameItemUiModel` in `core/ui`;
  `GameStatusUiModel`, `PriorityUiModel`, `RatingUiModel`, `PlatformTileUiModel`,
  `PlatformReleaseDateUiModel` and `WishlistListUiModel` in `feature/game-detail`; `WishlistListUiModel`
  in `feature/lists`; `SearchHistoryUiModel`, `SortingUiModel`, `GameFilterUiModel`,
  `FilterBottomSheetState` and `SortBottomSheetState` in `feature/search`. `UiEvent` and `UiEffect`
  hierarchies are deliberately out of it — they travel as lambda parameters, where stability buys nothing.
- **Room migrations are deferred until release**: the database is deliberately pinned to `version = 1`
  with `.fallbackToDestructiveMigration(true)`, so every entity change wipes the device. That is the
  owner's decision while the app is unpublished — **it is not a bug to fix, and the version must not be
  bumped**. Before the first release: freeze the schema, decide where `Migration` objects live, and
  replace the blanket fallback. `exportSchema` is already on and `schemas/1.json` is checked in, which is
  the starting point.
- **Stale cross-ref rows on update**: `GameDao.saveGame` re-inserts the genre, platform and company
  cross-refs with `OnConflictStrategy.REPLACE` but never deletes the previous ones, so a game that loses a
  genre, a platform or a publisher upstream keeps the link forever — and the detail screen keeps rendering
  it. Artworks, engine cross-refs and related games do delete first, inside the same transaction; those
  three are the ones left. The fix is three `deleteXByGameId` queries called from `saveGame`, which is
  cheap, but it is a behaviour change and belongs in its own commit.
- **Release is signed with the debug key**: `app/build.gradle.kts` still uses
  `signingConfigs.getByName("debug")`, so the APK cannot be distributed. Needs a real keystore read from a
  git-ignored `keystore.properties`.

## Infrastructure

- **No convention plugins**: no `build-logic`, no `buildSrc`. `compileSdk = 37`, `minSdk = 29` and Java 11
  are repeated by hand in all 14 module build files.
- **No CI** (`.github/` does not exist) and **no static analysis** (no detekt, ktlint, spotless,
  `.editorconfig`, or `lint {}` block).
- **Test coverage gaps**: no tests at all for `:feature:wishlist`, `:core:domain` use cases,
  `:core:database` DAOs, or `:core:ui` mappers. In `:core:network` only `IgdbAuthManager` is covered —
  `IgdbHttpErrorInterceptor` and the API service are not. No Compose UI tests — `ui-test-junit4` is
  wired into `:app` but only the template `ExampleInstrumentedTest` exists. `app/src/test/ExampleUnitTest.kt`
  is also an untouched template.
