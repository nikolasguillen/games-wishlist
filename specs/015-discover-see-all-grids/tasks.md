---

description: "Task list for see-all grids for Discover rows"
---

# Tasks: See-All Grids for Discover Rows

**Input**: Design documents from `/specs/015-discover-see-all-grids/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, quickstart.md (no `contracts/`: no new platform
capability)

**Tests**: Included. The constitution (principle V) requires a test in the module's own `androidHostTest` for new
ViewModel and mapper logic, and this feature is mostly that logic (the cap, shelf identity, state transitions, sort
order). They are JUnit4 + MockK + `kotlinx-coroutines-test` with a `StandardTestDispatcher`, mocking use cases and
never the repository. Tests come before the code they cover; a test that names a type that does not exist yet fails to
compile until the code lands, so commit a test and its code together. The composables (`DiscoverFeed`, the search bar
changes) are checked by previews and by the quickstart walkthrough, not by JVM tests: the project has no Compose UI
tests (`docs/tech-debt.md`).

**Organization**: Grouped by user story. Foundational holds the behaviour-neutral refactor that every story builds on
(`activeGenre` → `activeBrowse`, shelf identity, facet visibility). US1 is the cap. US2 is the arrow and the grid.
US3–US5 are, in this design, mostly *verified* rather than built: the placeholder, exit paths, "Top picks" order and
facets are wired by Foundational and US2 (plan.md, research R3–R6), so those phases are tests and walkthroughs that
prove each requirement, and any gap a test exposes is fixed in the file the task names. Tasks marked **(Mac only)**
cannot be compiled or run without a Mac and must not be reported as verified unless they were run on one.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependency on an unfinished task)
- **[Story]**: Which user story the task belongs to (US1–US5)
- Every path is relative to the repository root

## Rules that apply to every task

- Android must build and pass its tests at every commit: `./gradlew :<module>:compileAndroidMain` after a change,
  `./gradlew :<module>:testAndroidHostTest` for a module with tests, `./gradlew test` and
  `./gradlew :androidApp:assembleDebug` at each checkpoint. Never delete or weaken an existing test. Where a task says
  "update the accessors", only the way the test reads the state changes; every assertion stays.
- One `data class` / `class` / `enum` per file, with the one exception of a sealed hierarchy keeping its cases in the
  same file. New types are `internal`.
- Shelves are identified by `DiscoverShelfKey` (a lane enum or a signal id), never by their displayed title (FR-012).
  Every user-visible string is a resource; nothing is hardcoded (FR-028).
- Use `MaterialTheme.spacing` tokens, never a `dp` literal. Comments and KDoc are in English and say *why*.
- Every new composable gets a private `@QuestLogPreviews` wrapped in `QuestLogTheme { }`.
- Commit messages follow `type(scope): subject` (scopes in `.githooks/commit-msg`, `search` for this feature) and end
  with the `Co-Authored-By` trailer given for this repository.
- Do **not** touch `GetDiscoverFeedUseCase`, the lane cache or the shelf de-duplication (FR-004), and do not fix any
  entry in `docs/tech-debt.md`.

---

## Phase 1: Setup

**Purpose**: Know the starting point, so a red test later is attributable to this feature.

- [ ] T001 Run `./gradlew :feature:search:testAndroidHostTest :core:domain:testAndroidHostTest --console=plain -q` on
  the branch before any change and confirm it is green. If anything is already red, stop and tell the owner instead of
  continuing.

---

## Phase 2: Foundational (blocking prerequisites)

**Purpose**: Generalise 014's single `activeGenre` into `activeBrowse` (a genre browse or a shelf grid), introduce the
shelf identity type, and make the facet rule visible to the feature. **Behaviour does not change in this phase**: the
genre browse works exactly as before. Every story reads `activeBrowse`, so none can start before it lands.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

**Commit boundary**: T005–T012 break the build one by one and are green only together (T004 is a new file and
harmless alone). Land T002–T013 in one commit, for example
`refactor: generalise the genre browse marker into a browse sealed type`. It spans two modules (`:core:domain` and
`:feature:search`), so the scope is omitted, as `.githooks/commit-msg` says for multi-module changes.

- [ ] T002 [P] Make the facet function public in
  `core/domain/src/commonMain/kotlin/com/nikolasguillen/questlog/core/domain/usecase/search/SearchResultFacets.kt`:
  change `internal fun List<Game>.toSearchResult(excludedGenreId: Int? = null)` to a public function. The body does not
  change. Extend its KDoc with one line saying a third caller exists, Search's shelf grid, which passes no exclusion
  because a shelf is not defined by one genre. This is the owner-approved cross-module touch (research R5, spec
  FR-027). No new dependency edge: `:feature:search` already depends on `:core:domain`.
- [ ] T003 [P] Create
  `feature/search/src/commonMain/kotlin/com/nikolasguillen/questlog/feature/search/model/DiscoverShelfKey.kt`:
  `@Immutable internal sealed interface DiscoverShelfKey` with three cases in the same file:
  `data class Lane(val lane: DiscoverLane)` (the `:core:model` enum, import
  `com.nikolasguillen.questlog.core.model.DiscoverLane`), `data class ByGenre(val genreId: Int)` and
  `data class ByDeveloper(val developerId: Int)`. KDoc: it says *which* shelf, by the id of the signal the shelf was
  built from and never by its displayed name or its position, because a feed refresh can reorder the personalised
  shelves between render and tap (research R2); within one feed a key is unique.
- [ ] T004 Create
  `feature/search/src/commonMain/kotlin/com/nikolasguillen/questlog/feature/search/model/SearchBrowseUiModel.kt`:
  `@Immutable internal sealed interface SearchBrowseUiModel` with, in the same file,
  `data class Genre(val genre: BrowseGenreUiModel)` and `data class Shelf(val key: DiscoverShelfKey, val title: UiText)`
  (`UiText` from `com.nikolasguillen.questlog.core.ui.model.UiText`). KDoc: what the committed grid is showing when it
  is not a typed query; needs T003.
- [ ] T005 Edit
  `feature/search/src/commonMain/kotlin/com/nikolasguillen/questlog/feature/search/model/SearchUiState.kt`: replace
  `val activeGenre: BrowseGenreUiModel? = null` with `val activeBrowse: SearchBrowseUiModel? = null`. Rewrite the
  `@property` KDoc and keep both invariants stated there: `activeBrowse != null` implies `contentState` is not
  `SearchContentState.Idle`, and `activeBrowse is SearchBrowseUiModel.Shelf` implies `contentState` is
  `SearchContentState.Success`. Needs T004.
- [ ] T006 Edit
  `feature/search/src/commonMain/kotlin/com/nikolasguillen/questlog/feature/search/mapper/SearchUiMapper.kt`: (a)
  rename the parameter of `List<SortingUiModel>.withRelevanceLabel(isGenreBrowse: Boolean)` to `isBrowse` and update its
  KDoc ("a browse is ordered by its source, not by how well a query matches"); behaviour is unchanged. (b) Add
  `internal fun SearchBrowseUiModel.toPlaceholder(): UiText`: `Genre` returns
  `UiText.StringResource(Res.string.genre_browse_placeholder, genre.label)` (the existing "genre: %1$s"; a `UiText`
  argument is resolved by `asString()`/`resolve()`), `Shelf` returns `title` as it is. Use an exhaustive `when`. Import
  `genre_browse_placeholder` from the module's own `Res`. Needs T004.
- [ ] T007 [P] Create
  `feature/search/src/androidHostTest/kotlin/com/nikolasguillen/questlog/feature/search/mapper/SearchUiMapperTest.kt`
  (KDoc header saying what it covers). Cases: `Genre(BrowseGenreUiModel(12, label)).toPlaceholder()` equals
  `UiText.StringResource(Res.string.genre_browse_placeholder, label)`; `Shelf(key, title).toPlaceholder()` is
  `title` (same instance), for a `Lane` key and for a `ByGenre` key with a formatted title; `withRelevanceLabel(true)`
  rewrites only the `RELEVANCE` entry to `sort_top_picks` and leaves `selected`, `descending` and the other three
  entries untouched; `withRelevanceLabel(false)` restores `CoreUiRes.string.sort_relevance`. Needs T006.
- [ ] T008 Edit
  `feature/search/src/commonMain/kotlin/com/nikolasguillen/questlog/feature/search/SearchViewModel.kt` for the rename
  only (research R3 table): `performSearch` sets `activeBrowse = null`; `browseGenre` sets
  `activeBrowse = SearchBrowseUiModel.Genre(genre)`; `clearSearch` sets `activeBrowse = null`; the private helper
  `SortBottomSheetState.withRelevanceLabel(isGenreBrowse)` becomes `isBrowse`; rename the `keepServerOrder` parameter of
  `sortGames` to `keepSourceOrder` (update its KDoc: the order comes from a server ranking or from a shelf) and have
  `updateSearchContent` pass `_uiState.value.activeBrowse != null`; `retrySearch` becomes an exhaustive `when` on
  `_uiState.value.activeBrowse`: `Genre` re-runs `browseGenre(browse.genre)`, `null` re-runs
  `performSearch(textFieldState.text.toString())`, and `Shelf` does nothing, with a comment citing the invariant
  (a shelf grid is only ever `Success`, so Retry cannot be shown for it; research R6). `browseGenre`'s call to
  `sortGames(..., keepServerOrder = true)` becomes `keepSourceOrder = true`. Needs T005, T006.
- [ ] T009 Edit
  `feature/search/src/commonMain/kotlin/com/nikolasguillen/questlog/feature/search/components/SearchBars.kt`:
  `SearchInputField`'s `activeGenreLabel: UiText?` becomes `browsePlaceholder: UiText?`. It resolves it with
  `asString()` for the placeholder text and the `stateDescription`, and shows the clear (X) button when
  `textFieldState.text.isNotEmpty() || browsePlaceholder != null`. Remove the `stringResource(genre_browse_placeholder,
  ...)` call from the composable (the format now lives in `toPlaceholder()`) and its import if unused. In
  `SearchTopBar`, pass `browsePlaceholder = uiState.activeBrowse?.toPlaceholder()`. Update the existing previews
  (`activeGenreLabel = UiText.DynamicString("RPG")` becomes a `browsePlaceholder`). Update the comments that say
  "genre browse" to "browse" where they are now general. Needs T006.
- [ ] T010 Edit
  `feature/search/src/commonMain/kotlin/com/nikolasguillen/questlog/feature/search/components/SearchMainContent.kt`:
  replace the `activeGenre: BrowseGenreUiModel?` parameter with `activeBrowse: SearchBrowseUiModel?`. The empty state
  shows `genre_browse_no_results` only when `activeBrowse is SearchBrowseUiModel.Genre` (using its
  `genre.label.asString()`), otherwise `search_no_results`. Update the KDoc and the previews
  (`SearchMainContentGenreBrowseEmptyPreview` passes `SearchBrowseUiModel.Genre(BrowseGenreUiModel(...))`). Needs T004.
- [ ] T011 Edit
  `feature/search/src/commonMain/kotlin/com/nikolasguillen/questlog/feature/search/SearchScreen.kt`: the `BackHandler`
  `enabled` condition reads `uiState.activeBrowse != null` instead of `uiState.activeGenre != null` (update the comment
  above it), and `SearchMainContent(... activeGenre = uiState.activeGenre ...)` passes
  `activeBrowse = uiState.activeBrowse`. Needs T010.
- [ ] T012 Update `feature/search/src/androidHostTest/kotlin/com/nikolasguillen/questlog/feature/search/SearchViewModelTest.kt`
  for every read of `activeGenre` (`grep -n activeGenre` on the file lists them). Add one private helper beside `sortOption`:
  `private fun SearchViewModel.activeGenre(): BrowseGenreUiModel? = (uiState.value.activeBrowse as? SearchBrowseUiModel.Genre)?.genre`,
  and replace each `uiState.value.activeGenre` / `viewModel.uiState.value.activeGenre` with `viewModel.activeGenre()`. No
  assertion changes; no test is added, removed or weakened. Needs T005.
- [ ] T013 Checkpoint: `./gradlew :feature:search:compileAndroidMain --console=plain -q`, then
  `./gradlew :feature:search:testAndroidHostTest :core:domain:testAndroidHostTest --console=plain -q`. Expected: green,
  with exactly the same tests as T001 plus `SearchUiMapperTest`. Commit (boundary above).

**Checkpoint**: The genre browse behaves as before, on `activeBrowse`. The shelf type, the key and the facet rule exist.

---

## Phase 3: User Story 1 - Shorter rows, so every shelf is within reach (Priority: P1) 🎯 MVP

**Goal**: Every Discover shelf shows at most six games in its existing order, and the mapper knows whether a shelf holds
more (spec FR-001–FR-004). The hero is untouched. There is no arrow yet, so this is shippable on its own as a pure
shortening, but the games beyond six are then not reachable until US2.

**Independent Test**: With a feed whose "Popular this month" holds well over six games, open Search and confirm that
shelf shows exactly its first six, in the usual order; that every other shelf holding more than six does the same; that
a shelf holding six or fewer shows all of its games; and that the hero is unchanged (quickstart §3, rows 1–2).

**Commit boundary**: T016–T022 are green only together (the shelf fields change type; T014 and T015 compile only
beside them). Land T014–T023 in one commit, for example `feat(search): cap Discover rows at six games`.

- [ ] T014 [P] [US1] Create
  `feature/search/src/androidHostTest/kotlin/com/nikolasguillen/questlog/feature/search/mapper/DiscoverMapperTest.kt`
  (KDoc header). Build `DiscoverFeed`s from `Game`s, call `toDiscoverContentState(savedIds)` and assert: `popular` with
  0 games is `null`; with 1 game it has that game and `hasMore == false`; with 6 games `hasMore == false`; with 7 games
  `hasMore == true` and `games` are the first six ids in order; `upcoming` is the feed's `upcoming.drop(1)` capped the
  same way (hero + 6 → 6, `hasMore == false`; hero + 7 → 6, `hasMore == true`) and `hero` is `upcoming.first()`;
  `recommended` entries carry key `DiscoverShelfKey.ByGenre(genre.id)` / `ByDeveloper(developer.id)` and titles
  `UiText.StringResource(Res.string.discover_because_you_like, genre.name)` / `discover_more_from` with the developer's
  name; the lane keys are `Lane(POPULAR_THIS_MONTH)` / `Lane(MOST_ANTICIPATED)` and their titles
  `UiText.StringResource(Res.string.discover_popular_this_month)` / `discover_most_anticipated`; the cap does not change
  the order (ids ascending in, ascending out); `DISCOVER_SHELF_ROW_SIZE == 6`. Also `shelfGames`: `Lane(POPULAR…)` is
  `popular`, `Lane(MOST_ANTICIPATED)` is `upcoming.drop(1)`, `ByGenre(id)` / `ByDeveloper(id)` return that shelf's whole
  game list, an unknown key returns `null`, and for every shelf the row's ids are a prefix of `shelfGames(key)`.
- [ ] T015 [P] [US1] Create
  `feature/search/src/commonMain/kotlin/com/nikolasguillen/questlog/feature/search/model/DiscoverShelfUiModel.kt`:
  `@Immutable internal data class DiscoverShelfUiModel(val key: DiscoverShelfKey, val title: UiText, val games:
  List<GameItemUiModel>, val hasMore: Boolean)`. KDoc: `games` is the row, the first `DISCOVER_SHELF_ROW_SIZE` games of
  the shelf in shelf order; `hasMore` is true exactly when the shelf holds more than that, and is the only case that
  gets an arrow; `title` is both the heading and the grid's placeholder.
- [ ] T016 [US1] Edit
  `feature/search/src/commonMain/kotlin/com/nikolasguillen/questlog/feature/search/model/DiscoverContentState.kt`:
  in `Content`, `popular` and `upcoming` become `DiscoverShelfUiModel?` (`null` when the lane is empty, so "render only
  if non-empty" is decided by the mapper) and `recommended` becomes `List<DiscoverShelfUiModel> = emptyList()`.
  `hero`, `isStale`, `isRefreshing` stay. Update the KDoc: `hero` and `upcoming` remain disjoint. Needs T015.
- [ ] T017 [US1] Edit
  `feature/search/src/commonMain/kotlin/com/nikolasguillen/questlog/feature/search/mapper/DiscoverMapper.kt`: add
  `internal const val DISCOVER_SHELF_ROW_SIZE = 6` with KDoc (a display cap only; the feed fetches and caches more and
  the grid shows all of it; it stays here and not in `UiConstants` because nothing else reads it). Add
  `internal fun DiscoverFeed.shelfGames(key: DiscoverShelfKey): List<Game>?` exactly as data-model.md defines it:
  `Lane(MOST_ANTICIPATED)` → `upcoming.drop(1)`, `Lane(POPULAR_THIS_MONTH)` → `popular`, `ByGenre(id)` → the
  `recommended` shelf whose `reason` is `ShelfReason.ByGenre` with `genre.id == id`, `ByDeveloper(id)` likewise for
  `developer.id`, `null` when none matches (bind `reason` to a local val to smart-cast; see the existing comment in the
  file). Rewrite `toDiscoverContentState(savedIds)` so each shelf is built by one private helper from
  `shelfGames(key)`: `games = shelfGames.take(DISCOVER_SHELF_ROW_SIZE).toGameItemList(savedIds)`,
  `hasMore = shelfGames.size > DISCOVER_SHELF_ROW_SIZE`, `title` as `UiText.StringResource(...)` (the two lane titles
  move here from `DiscoverFeed.kt`; import `discover_most_anticipated` and `discover_popular_this_month`), and an empty
  lane maps to `null`. The recommended shelves are built with the keys from `RecommendedShelf.reason`. `hero` and the
  `upcoming.drop(1)` split are unchanged. Needs T015, T016.
- [ ] T018 [US1] Delete
  `feature/search/src/commonMain/kotlin/com/nikolasguillen/questlog/feature/search/model/RecommendedShelfUiModel.kt`
  (replaced by `DiscoverShelfUiModel`) and remove every import of it. Needs T017.
- [ ] T019 [P] [US1] Edit `observeWishlistedGameIds` in
  `feature/search/src/commonMain/kotlin/com/nikolasguillen/questlog/feature/search/SearchViewModel.kt` so the saved-state
  patch follows the new shape: `popular = discover.popular?.let { it.copy(games = it.games.withSavedState(ids)) }`,
  likewise `upcoming`, and `recommended = discover.recommended.map { it.copy(games = it.games.withSavedState(ids)) }`.
  `hero` is patched as today. Needs T016.
- [ ] T020 [P] [US1] Edit
  `feature/search/src/commonMain/kotlin/com/nikolasguillen/questlog/feature/search/components/DiscoverFeed.kt`:
  `DiscoverFeed` takes `popular: DiscoverShelfUiModel?`, `upcoming: DiscoverShelfUiModel?` and
  `recommended: List<DiscoverShelfUiModel>` in place of the three lists. The empty-feed check becomes
  `hero == null && popular == null && upcoming == null && recommended.isEmpty()`. `DiscoverShelf` takes one
  `DiscoverShelfUiModel`, renders `shelf.title.asString()` and its `games` (the row), and keys the recommended items by
  `shelf.key`. Remove the `stringResource(Res.string.discover_most_anticipated / discover_popular_this_month)` calls and
  their imports (the mapper owns the titles now). No arrow yet. Update the existing previews to build
  `DiscoverShelfUiModel`s and add a preview of a shelf holding exactly six cards. Needs T015.
- [ ] T021 [US1] Edit
  `feature/search/src/commonMain/kotlin/com/nikolasguillen/questlog/feature/search/components/SearchMainContent.kt`:
  `DiscoverContent` passes `state.popular`, `state.upcoming` and `state.recommended` to the new `DiscoverFeed`
  parameters, and `SearchMainContentDiscoverPreview` builds `DiscoverContentState.Content(popular =
  DiscoverShelfUiModel(...), upcoming = DiscoverShelfUiModel(...))`. Needs T020.
- [ ] T022 [P] [US1] Update the existing Discover assertions in
  `feature/search/src/androidHostTest/kotlin/com/nikolasguillen/questlog/feature/search/SearchViewModelTest.kt` to the new
  accessors: `contentState.popular.map { it.id }` becomes `contentState.popular?.games?.map { it.id }`, `popular.single()`
  becomes `popular?.games?.single()`, and `upcoming` likewise (lines near 210, 212, 225, 269, 362, 384, 413, 698, 729,
  750). `hero` and the other assertions are untouched. Needs T016.
- [ ] T023 [US1] Checkpoint: `./gradlew :feature:search:compileAndroidMain --console=plain -q`, then
  `./gradlew :feature:search:testAndroidHostTest --console=plain -q` (`DiscoverMapperTest` and the updated
  `SearchViewModelTest` green), then `./gradlew :androidApp:assembleDebug`. Walk quickstart §3 rows 1 and 2 on an
  emulator (the cap; no arrow exists yet, so only check the arrow's absence). Commit (boundary above).

**Checkpoint**: Every Discover row shows at most six games. `popular`/`upcoming`/`recommended` carry their key and
`hasMore`.

---

## Phase 4: User Story 2 - See the whole shelf as a grid (Priority: P1)

**Goal**: A shelf holding more than six games shows an arrow at the end of its title row; tapping the row replaces the
feed with the committed grid of every game of that shelf, in shelf order, with no loading state, no network request, and
the same cards, save and long-press actions as a text search result (FR-005–FR-014).

**Independent Test**: Tap the arrow on a shelf with more than six games and confirm the grid shows exactly the shelf's
games in the shelf's order, appears with no spinner, works with airplane mode on, and that opening, saving and
long-pressing a card behave as in a text search result (quickstart §3, rows 3, 4, 9, 10, 12).

**Dependency**: US1 (the shelf model, key and `hasMore`) and Foundational (`activeBrowse`).

- [ ] T024 [US2] Add the shelf-grid cases to
  `feature/search/src/androidHostTest/kotlin/com/nikolasguillen/questlog/feature/search/SearchViewModelTest.kt`, in a
  block under the 014 browse tests, built from `getDiscoverFeedUseCase` emissions (as the existing Discover tests do)
  and taking the shelf to open from `viewModel.uiState.value.discover` (the way the screen gets it). Add a helper
  `givenShelves(...)` for the feeds. Cases (plan.md cases 1–5 and 10–13): (1) opening "Popular this month" with eight
  games sets `activeBrowse` to `Shelf(Lane(POPULAR_THIS_MONTH), title)` and `contentState` to `Success` with `allGames`
  holding all eight ids in order, and collecting `uiState` into a list shows **no** `Loading` value between `Idle` and
  `Success`; (2) opening "Most anticipated" holds `upcoming.drop(1)`, so the hero id is absent; (3) opening then
  `OnClearSearch` leaves `getDiscoverFeedUseCase` invoked exactly once and `searchGamesUseCase`, `browseGenreUseCase` and
  `addSearchToHistoryUseCase` never called (`coVerify(exactly = 0)`); (4) snapshot: after opening, a new feed emission
  with different games changes `uiState.discover` but not `contentState`; (5) `OnToggleSave(id)` of a grid card calls
  `toggleWishlistUseCase` with that `Game`, and `OnOpenListSelector(id)` opens the selector for it, and a
  `getWishlistedGameIdsUseCase` emission patches `isSaved` on the grid's cards; (10) two personalised shelves
  `ByGenre(12)` and `ByGenre(31)` whose titles are the same text each open their own games; (11) opening a shelf whose
  key is not in the current feed leaves `activeBrowse == null` and `contentState` `Idle`; (12) two
  `OnShelfSeeAllTriggered` for the same shelf with a changed feed emission in between keep the first snapshot;
  (13) with a text search suspended on a `CompletableDeferred`, open a shelf, then complete the search: the shelf's
  `Success` and `activeBrowse` are still there. Needs T025.
- [ ] T025 [P] [US2] Edit
  `feature/search/src/commonMain/kotlin/com/nikolasguillen/questlog/feature/search/model/SearchUiEvent.kt`: add
  `data class OnShelfSeeAllTriggered(val shelf: DiscoverShelfUiModel) : SearchUiEvent` with KDoc ("the user tapped a
  Discover shelf's title row; carries the whole shelf, like `OnGenreBrowseTriggered`, so the placeholder names exactly
  what was tapped").
- [ ] T026 [P] [US2] Add to
  `feature/search/src/commonMain/composeResources/values/strings.xml`:
  `<string name="discover_see_all_shelf">See all: %1$s</string>` beside the other `discover_*` entries (positional
  `%1$s`, plain text, no escapes; the argument is the shelf's resolved title). The module ships only `values/`, so there
  is no locale to add it to.
- [ ] T027 [US2] Edit
  `feature/search/src/commonMain/kotlin/com/nikolasguillen/questlog/feature/search/SearchViewModel.kt`: add the
  `OnShelfSeeAllTriggered` branch to `onEvent` calling a new `private fun openShelf(shelf: DiscoverShelfUiModel)`,
  **synchronous, no coroutine** (research R6): (a) return if `activeBrowse` is already a `Shelf` with `shelf.key`
  (double tap); (b) `val games = discoverFeed?.shelfGames(shelf.key)`, return if it is `null` or empty (the shelf is
  gone after a refresh); (c) `searchJob?.cancel(); searchJob = null` **before** touching state, because that is what
  guarantees no `Loading`/`Error`/`Empty` can land under a shelf (comment it, citing the invariant); (d) empty the text
  field exactly as `browseGenre` does and clear the suggestions (`suggestions = SearchSuggestionsUiModel()`,
  `suggestionsResetTrigger.tryEmit("")`); (e) build the facets with `games.toSearchResult()` (**no**
  `excludedGenreId`, so no genre is withheld; do not call `BrowseGenreUseCase`), `filters = result.platforms
  .toPlatformFilters() + result.genres.toGenreFilters() + getInitialGameTypeFilters()`, sort with
  `sortGames(games, selectedSorting, keepSourceOrder = true)`, and in **one** `_uiState.update` set
  `activeBrowse = SearchBrowseUiModel.Shelf(shelf.key, shelf.title)`, the sort sheet via `withRelevanceLabel(isBrowse =
  true)`, and `contentState = SearchContentState.Success(games = sorted.toGameItemList(wishlistedGameIds.value), filters
  = filters, allGames = games)`. `allGames` is the **shelf-order** list, so "Top picks" is recoverable after a re-sort
  (as in `browseGenre`). Add the import of `toSearchResult` from `core.domain.usecase.search`. Needs T002, T008, T025.
- [ ] T028 [US2] Edit
  `feature/search/src/commonMain/kotlin/com/nikolasguillen/questlog/feature/search/components/DiscoverFeed.kt`: give
  `DiscoverFeed` an `onSeeAllClick: (DiscoverShelfUiModel) -> Unit` parameter (before `modifier`, after `onLongClick`)
  and thread it into `DiscoverShelf`. Rebuild the title row of the private `DiscoverShelf` as in research R7: a `Row`
  (`verticalAlignment = CenterVertically`, horizontal padding `MaterialTheme.spacing.large`), the title `Text` with
  `Modifier.weight(1f)`, `maxLines = 1`, `overflow = TextOverflow.Ellipsis` and the existing style, and **only when
  `shelf.hasMore`** a trailing `Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint =
  MaterialTheme.colorScheme.primary, modifier = Modifier.size(MaterialTheme.spacing.mediumLarge))` (the same idiom as
  `SeeAllResultsRow` in `components/SuggestionRow.kt`). When `hasMore`, the row's modifier is
  `Modifier.minimumInteractiveComponentSize().clickable(role = Role.Button) { onSeeAllClick(shelf) }` followed by
  `clearAndSetSemantics { contentDescription = seeAllLabel }`, where
  `val seeAllLabel = stringResource(Res.string.discover_see_all_shelf, shelf.title.asString())`, so the row is one
  merged button node (one TalkBack stop). When `hasMore` is false the row is the plain heading it is today: no
  `clickable`, no icon, no semantics change. No `dp` literal; the existing `140.dp` card width stays as it is. Add
  previews: a shelf with `hasMore = true`, one with `hasMore = false`, and one with a very long title that keeps the
  arrow visible. Needs T015, T025, T026.
- [ ] T029 [US2] Edit
  `feature/search/src/commonMain/kotlin/com/nikolasguillen/questlog/feature/search/components/SearchMainContent.kt`:
  add `onSeeAllClick: (DiscoverShelfUiModel) -> Unit` to `SearchMainContent` (after `activeBrowse`), pass it through
  `DiscoverContent` to `DiscoverFeed`, and update the previews' calls. Needs T028.
- [ ] T030 [US2] Edit
  `feature/search/src/commonMain/kotlin/com/nikolasguillen/questlog/feature/search/SearchScreen.kt`: add `val
  onOpenShelf: (DiscoverShelfUiModel) -> Unit = remember(onResetScroll, onEvent, scope) { { shelf ->
  scope.launch { onResetScroll() }; onEvent(SearchUiEvent.OnShelfSeeAllTriggered(shelf)) } }`, built like
  `onBrowseGenre`, with a comment that the results grid is shared across searches, so it must start at the top, while
  `onResetScroll` does not touch `discoverListState`. The search bar is already collapsed (the arrow is on the feed), so
  no collapse call is needed. Pass `onSeeAllClick = onOpenShelf` to `SearchMainContent`. Needs T029.
- [ ] T031 [US2] Checkpoint: `./gradlew :feature:search:testAndroidHostTest --console=plain -q` (T024 green), then
  `./gradlew :androidApp:assembleDebug`. Walk quickstart §3 rows 1–4, 9, 10 and 12 on an emulator. Commit, for example
  `feat(search): open a Discover shelf as a grid`.

**Checkpoint**: A shelf's games are one tap away, in order, instantly and offline. US1 + US2 is the smallest release.

---

## Phase 5: User Story 3 - Know what the grid is, and get back out (Priority: P1)

**Goal**: The search bar's placeholder names the shelf, X is visible with an empty field, and X, the back arrow and the
system back return to the feed at the scroll position it had (FR-015–FR-020).

**Independent Test**: Open a shelf's grid and confirm the placeholder names the shelf and X is visible with an empty
field; leave via X, via the back arrow and via system back, each time confirming the feed returns with the default
placeholder at the scroll position it had (quickstart §3, rows 3, 7, 8, 11, 13, 14).

**Dependency**: US2 (a grid to leave). The placeholder, the X button and the back handler were generalised in
Foundational (T006, T009, T011); this phase proves they work for a shelf.

- [ ] T032 [US3] Add exit and replacement cases to
  `feature/search/src/androidHostTest/kotlin/com/nikolasguillen/questlog/feature/search/SearchViewModelTest.kt`, next to
  T024's block: `OnClearSearch` from a shelf grid gives `activeBrowse == null`, `contentState` `Idle` and the sort label
  back to "Relevance", and does not re-invoke `getDiscoverFeedUseCase`; `OnSearchTriggered("zelda")`
  from a shelf grid gives `activeBrowse == null` and text results; `OnGenreBrowseTriggered(rpgChip)` from a shelf grid
  gives `activeBrowse is Genre` with the genre's `Loading` then results; typing without submitting leaves the shelf
  grid on screen (`activeBrowse` and `contentState` unchanged), even if the text is deleted again; opening a shelf while
  a genre browse is showing replaces it and empties the text field; the shelf placeholder is
  `activeBrowse.toPlaceholder() == shelf.title` (the same `UiText` as the heading) for a lane and for a formatted
  `ByGenre` title.
- [ ] T033 [US3] Verify scroll restoration (FR-018) from the code, and record the result. In
  `feature/search/src/commonMain/kotlin/com/nikolasguillen/questlog/feature/search/SearchScreen.kt` confirm that
  `discoverListState = rememberLazyListState()` is created in `SearchScreenContent`, above `SearchMainContent`'s `when
  (contentState)`, that `onResetScroll` only touches `gridState`, and that `SearchMainContent` passes the same
  `discoverListState` back to `DiscoverFeed`. Then confirm it on a device: quickstart §3 row 7 (scroll the feed to
  "Popular this month", open it, leave by X, back arrow and system back; the feed is where it was) and row 11
  (rotation). If it does **not** hold, do not bend the design: add a note to `specs/015-discover-see-all-grids/plan.md`
  and to FR-018 in `spec.md` stating the gap, as the spec allows (FR-018 is nice-to-have), and tell the owner.
- [ ] T034 [US3] Walk quickstart §3 rows 3, 7, 8, 11 and 13 on an emulator or device (placeholder and X with an empty
  field, the three exits, a submitted query replacing the grid, rotation and backgrounding, the stale prompt not
  showing over the grid) and row 14 with TalkBack for the bar's announced shelf name. Note each row as passed or failed;
  fix any failure in the file it points to (`SearchBars.kt`, `SearchScreen.kt` or `SearchViewModel.kt`).

**Checkpoint**: The grid says what it is and has three ways out, all returning to the feed where it was.

---

## Phase 6: User Story 4 - Sort and filter a shelf grid consistently with the genre browse (Priority: P2)

**Goal**: "Relevance" reads "Top picks" and means the shelf's own order; the filter sheet offers the platforms, genres
and game types of the shelf's games; no genre is withheld; filters narrow with AND semantics (FR-021–FR-023).

**Independent Test**: Open a shelf grid, open Sort and confirm the first option reads "Top picks" and keeps the shelf's
order; pick another sort, leave, and confirm "Relevance" is back with the selection untouched. Open Filter and confirm
platforms, genres and game types from the shelf's games, and that selecting some narrows the grid (quickstart §3, rows
5, 6).

**Dependency**: US2. The label, `keepSourceOrder` and the facet derivation were written in T006, T008 and T027; this
phase proves them. A failing test here is a bug in `SearchViewModel.openShelf` or `sortGames`, to be fixed there.

- [ ] T035 [US4] Add label and order cases to
  `feature/search/src/androidHostTest/kotlin/com/nikolasguillen/questlog/feature/search/SearchViewModelTest.kt`: the
  `RELEVANCE` option's label is `UiText.StringResource(Res.string.sort_top_picks)` while a shelf is open and
  `UiText.StringResource(CoreUiRes.string.sort_relevance)` after `OnClearSearch` and after a submitted query; select
  Rating descending via `OnSortChanged`, open a shelf, then leave, and `selected` and `descending` are the same
  throughout (only the label differs, FR-022); "Top picks" descending yields the shelf's id order, ascending yields it
  reversed, and both stay true after a filter is applied (follow the existing 014 test
  `Top picks keeps the server order and ascending reverses it, even after a filter`); Name, Rating and Release date sort
  a shelf grid as they sort a text search.
- [ ] T036 [US4] Add filter cases to
  `feature/search/src/androidHostTest/kotlin/com/nikolasguillen/questlog/feature/search/SearchViewModelTest.kt`: a shelf
  whose games carry genres 12, 31 and 5 offers all three in `Success.filters` (none withheld, and
  `browseGenreUseCase` never called); platforms and the initial game types are offered; every filter starts unselected,
  even after a filtered text search (as the 014 test `a browse starts with every filter unselected` does); selecting a
  platform and a genre through `OnFilterClick` narrows `Success.games` to games that carry both, in "Top picks" order;
  a selection nothing matches gives `Success` with `games` empty (not `Empty`), and `OnClearActiveFilters` restores the
  full shelf; `OnOpenFilters` while a shelf is open fills the sheet from `Success.filters` with `matchCount ==
  games.size`.
- [ ] T037 [US4] Walk quickstart §3 rows 5 and 6 on an emulator (the label and order; every genre offered, AND
  narrowing, and "no matches" with "Clear filters").

**Checkpoint**: Sort and filter on a shelf grid behave like the genre browse, with the facets from the shelf.

---

## Phase 7: User Story 5 - A shelf grid leaves no trace in history (Priority: P2)

**Goal**: Opening shelf grids writes nothing to the recent searches (FR-020).

**Independent Test**: Note the recent searches, open several shelf grids, reopen the expanded search view, and confirm
the recent searches are exactly as before (quickstart §3, row 8).

**Dependency**: US2.

- [ ] T038 [US5] Add to
  `feature/search/src/androidHostTest/kotlin/com/nikolasguillen/questlog/feature/search/SearchViewModelTest.kt`: opening
  three different shelves in turn (clearing in between) never calls `addSearchToHistoryUseCase`
  (`coVerify(exactly = 0)`); after that, `OnSearchTriggered("zelda")` makes `addSearchToHistoryUseCase("zelda")` the
  only call; model it on the 014 tests `a genre browse never writes to the search history` and
  `a query submitted after a browse is the only thing written to history`.
- [ ] T039 [US5] Walk quickstart §3 row 8 on an emulator (recent searches unchanged after opening grids; a submitted
  query is recorded).

**Checkpoint**: All five stories work.

---

## Phase 8: Polish & cross-cutting concerns

**Purpose**: The static checks, the full suites and the platform verification.

- [ ] T040 Run the static checks of quickstart §2: only `values/` exists in both `composeResources` directories (no locale
  to translate); `discover_see_all_shelf` is defined; no `Text("...")` / `contentDescription = "..."` literal in
  `DiscoverFeed.kt`, `SearchBars.kt` or `SearchMainContent.kt`; no new `dp` literal in the `DiscoverFeed.kt` diff (the
  existing `140.dp` card width and the refresh prompt's literals predate this feature); no `take(` in `DiscoverFeed.kt`
  (the cap is applied in the mapper, FR-029). Fix anything found.
- [ ] T041 Re-run the stale-reference search and fix any hit that is not under `specs/`:
  `grep -rn "RecommendedShelfUiModel\|activeGenre\|keepServerOrder\|isGenreBrowse" --include='*.md' --include='*.kt' .`
  (excluding `/build/`). At planning time no `CLAUDE.md`, `docs/` or roadmap text referred to them. Also confirm
  `docs/tech-debt.md` needs no edit (plan.md, research R11) and leave its entries alone.
- [ ] T042 Run `./gradlew test --console=plain -q` (every module's host tests, including `SearchResultFacetsTest` in
  `:core:domain` and `KoinGraphTest` in `:shared`) and `./gradlew :androidApp:assembleDebug`. Expected: all green, no
  test deleted or weakened.
- [ ] T043 Walk quickstart §3 rows 14–16 on an emulator or device: TalkBack reads a capped shelf's title row as one
  button, "See all: <title>", and an uncapped one as plain text; the arrow mirrors under RTL (Arabic, or "Force RTL layout
  direction"); "Show layout bounds" shows the title row at least 48dp tall. These are the only checks of the row's
  semantics and size, since there are no Compose UI tests. If `minimumInteractiveComponentSize()` does not give 48dp on
  either platform, fix the row in `DiscoverFeed.kt` and say so.
- [ ] T044 **(Mac only)** Build and run on iOS: `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64 -q`, then the
  `xcodebuild` command in the root `CLAUDE.md`, then repeat quickstart §3 rows 1–9 and 12–13 on the simulator, row 11 as
  backgrounding and returning only, and rows 14–16 with VoiceOver, the right-to-left pseudolanguage and the view
  debugger (quickstart §4). Row 10 (offline) cannot be run on a simulator without taking the Mac offline. If there is no
  Mac, write "iOS: not run" in the final report for this task and for every row of §4; do not report it as passed.

---

## Dependencies & execution order

### Phase dependencies

- **Setup (T001)**: none.
- **Foundational (T002–T013)**: needs T001; **blocks every story**. T002 and T003 are independent of each other and of
  the rest; T005–T012 form one compiling slice.
- **US1 (T014–T023)**: needs Foundational (it uses `DiscoverShelfKey` from T003 and edits files T008/T010 touched). It is
  the MVP and the base of every other story.
- **US2 (T024–T031)**: needs US1 (`DiscoverShelfUiModel`, `hasMore`, the keys) and Foundational (`activeBrowse`,
  `toSearchResult` public).
- **US3 (T032–T034)**: needs US2.
- **US4 (T035–T037)**: needs US2.
- **US5 (T038–T039)**: needs US2.
- **Polish (T040–T044)**: needs the stories you ship.

US3, US4 and US5 do not depend on one another, but they all append to `SearchViewModelTest.kt`, so write them one after
another unless two people merge carefully.

### Within a story

- Tests first (they will not compile until their code exists), then models and strings, then the mapper, then the
  ViewModel, then the UI.
- `SearchViewModel.kt` is edited by T008, T019 and T027: keep them in that order. `DiscoverFeed.kt` by T020 then T028,
  `SearchMainContent.kt` by T010, T021, T029, `SearchScreen.kt` by T011 then T030, and `SearchViewModelTest.kt` by T012,
  T022, T024, T032, T035, T036 and T038, in that order.
- A phase's commit boundary is stated in the phase; Android must build at every commit.

### Parallel opportunities

- Foundational: T002 and T003 are independent; after T004, T005, T006 and T010 touch different files; T007 can be
  written beside T008–T012 once T006 exists.
- US1: T014 and T015 are independent; after T016, T019, T020 and T022 touch different files.
- US2: T025 and T026 are independent files, and T024 can be written beside T027.
- US4: T035 and T036 are independent cases but share a file.
- Polish: T040 and T041 are independent.

### Parallel example: Foundational

```text
# Together, at the start:
T002  core/domain/.../usecase/search/SearchResultFacets.kt
T003  feature/search/.../model/DiscoverShelfKey.kt

# After T004 (SearchBrowseUiModel), together:
T005  SearchUiState.kt     T006  SearchUiMapper.kt     T010  SearchMainContent.kt
```

### Parallel example: User Story 1

```text
# At the start, together:
T014  feature/search/src/androidHostTest/.../mapper/DiscoverMapperTest.kt
T015  feature/search/.../model/DiscoverShelfUiModel.kt

# After T016 (DiscoverContentState), together:
T019  SearchViewModel.kt (observeWishlistedGameIds)     T020  DiscoverFeed.kt     T022  SearchViewModelTest.kt
```

---

## Implementation strategy

### MVP first (User Story 1 + 2)

1. T001, then T002–T013: the behaviour-neutral refactor. Commit.
2. T014–T023: the cap. Commit. Stop and look at it: the rows are short, but the games beyond six are unreachable.
3. T024–T031: the arrow and the grid. Commit. **Stop and validate** with quickstart §3 rows 1–4, 9, 10 and 12. US1 + US2
   is the smallest release; ship the two together, never US1 alone.

### Incremental delivery

1. Foundational → no visible change (the genre browse on `activeBrowse`).
2. + US1 + US2 → short rows and the instant grid (the release).
3. + US3 → proves the label and exits (they already work from Foundational; this is the verification, and the scroll
   check, which may defer FR-018 if it fails).
4. + US4 → proves "Top picks" and the shelf's facets.
5. + US5 → locks in the no-history rule with a test.
6. Polish → static checks, full suites, accessibility and the iOS run.

## Notes

- [P] tasks touch different files and have no unfinished dependency.
- US3–US5 are verification phases by design: a task there that finds a bug fixes it in the file the plan assigned, it does
  not add a parallel mechanism.
- `docs/tech-debt.md` lists known deviations; do not fix any of them here. Noticed and left alone: the lists screen's
  `Icons.Default.ChevronRight` does not mirror in RTL; "Because you like %1$s" still formats IGDB's raw genre name.
- Out of scope, do not add: pagination or a larger fetch, reordering shelves, changing the hero, changing how shelves are
  ranked or de-duplicated, persisting the open grid, any change to Radar or Wishlist, a new module, route or dependency.
