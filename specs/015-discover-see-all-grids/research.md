# Research: See-All Grids for Discover Rows

**Feature**: [spec.md](spec.md) | **Plan**: [plan.md](plan.md) | **Date**: 2026-10-13

The spec left no `[NEEDS CLARIFICATION]` markers. The open points below came from reading the code the
feature touches: `SearchViewModel`, `DiscoverMapper.kt`, `DiscoverFeed.kt`, `SearchMainContent.kt`,
`SearchBars.kt` and `SearchScreen.kt`. They also draw on the genre browse's design in
[014's research](../014-genre-quick-browse/research.md), which this feature builds on. Each point is resolved
here. R5's facet placement was decided by the owner. Everything else is decided by this plan.

---

## R1. Where the shelf's games come from

**Decision**: When the grid opens, look up the shelf's games in the domain `DiscoverFeed` that
`SearchViewModel` already holds in `private var discoverFeed`. One mapper function defines what a shelf holds,
and both the row and the grid read it:

```text
internal fun DiscoverFeed.shelfGames(key: DiscoverShelfKey): List<Game>?   // DiscoverMapper.kt
  Lane(MOST_ANTICIPATED)  -> upcoming.drop(1)        // the hero is split out, exactly as the row does today
  Lane(POPULAR_THIS_MONTH) -> popular
  ByGenre(id)             -> the recommended shelf whose reason is ByGenre with genre.id == id
  ByDeveloper(id)         -> the recommended shelf whose reason is ByDeveloper with developer.id == id
  (null when no shelf matches)
```

`toDiscoverContentState` builds each row from `shelfGames(key).take(DISCOVER_SHELF_ROW_SIZE)`, and the
ViewModel builds the grid from the whole `shelfGames(key)`. The row is therefore always the first six games
of the grid (SC-003), and the hero split is written once.

**Rationale**:
- `discoverFeed` already exists for this purpose. `findGame` resolves a Discover card's save and long-press
  back to a domain `Game` through it. It is written in `observeDiscoverFeed` from the same emission as
  `uiState.discover`, so the two describe the same feed.
- The grid needs domain `Game`s. `SearchContentState.Success.allGames` is `List<Game>`, and `filterGames`,
  `sortGames` and the facets all run on it. `DiscoverContentState.Content` only holds `GameItemUiModel`s,
  and after this feature only the first six of each shelf.
- No network request, no new use case and no repository call, so the grid opens at once and offline (FR-010).
  `GetDiscoverFeedUseCase`, the lane cache and the de-duplication across shelves are not touched (FR-004).

**Alternatives considered**:
- *Keep each shelf's full domain list beside its UI model in `DiscoverContentState`.* Rejected. It
  duplicates `discoverFeed` inside the `@Immutable` UI state. Every shelf would carry domain games the UI
  never renders. And `observeWishlistedGameIds` patches that state on every save, so it would copy those
  lists too.
- *A use case that returns a shelf's games.* Rejected. It would either fetch again, against FR-010, or
  reach into the lane cache, against FR-004. The games are already in memory.
- *Keep the uncapped `GameItemUiModel` list in state and map it back to `Game`s by id.* Rejected. That is a
  second lookup that can miss, to recover data the ViewModel already has.

---

## R2. Shelf identity

**Decision**: A new sealed `DiscoverShelfKey` in `feature/search/model/DiscoverShelfKey.kt`. It is a sealed
hierarchy, so its cases stay in the one file:

```text
Lane(lane: DiscoverLane)        // MOST_ANTICIPATED, POPULAR_THIS_MONTH: the existing :core:model enum
ByGenre(genreId: Int)           // a personalised shelf built from a genre signal
ByDeveloper(developerId: Int)   // a personalised shelf built from a developer signal
```

Every shelf's `DiscoverShelfUiModel` carries its key. The arrow sends
`SearchUiEvent.OnShelfSeeAllTriggered(shelf: DiscoverShelfUiModel)`. That is the whole model, as
`OnGenreBrowseTriggered(genre)` does, so the ViewModel gets the title the user tapped for the placeholder.
The ViewModel takes the games from `discoverFeed.shelfGames(shelf.key)`.

**Rationale**:
- `DiscoverLane` already exists in `:core:model` and names exactly the two generic lanes. The feature
  depends on `:core:model`, so reusing it adds no edge.
- For the personalised shelves, the key is the **id of the signal the shelf was built from**, not the
  shelf's position in `recommended`. `GetDiscoverFeedUseCase.recommendationPlan` builds at most one shelf
  per developer and one per genre, and the two kinds are separate cases, so the key is unique within a
  feed (`MAX_RECOMMENDED_SHELVES` = 2).
- **The feed refreshing between render and tap** is handled by the key:
  - A refresh can reorder `recommended`, for example when a developer starts to qualify and takes slot 0. An
    index would then open a *different* shelf under the title the user tapped, and the grid would not match
    its placeholder. A signal id still finds the same shelf.
  - If the shelf no longer exists in the refreshed feed, `shelfGames` returns `null` and the tap does
    nothing. The feed on screen has already re-rendered without that shelf, so the user sees why.
- Display names are never compared (FR-012). Two shelves with the same visible title open their own
  games, because they cannot share a key.

**Alternatives considered**:
- *`DiscoverLane` plus the index in `recommended`.* Rejected because of the refresh reordering above.
- *Carry the domain `ShelfReason` in the key.* Rejected. Its equality includes the display name, and it
  would put a domain type into a UI identifier when two ids are all the lookup needs.
- *The event carries the key only, and the ViewModel looks up the title.* Rejected. It needs a second
  lookup, in `uiState.discover`, that can miss in the same race. With the whole model, the placeholder
  always names exactly what the user tapped.

---

## R3. How an open shelf is held in state

**Decision**: Generalise 014's `activeGenre: BrowseGenreUiModel?` into one field,
`activeBrowse: SearchBrowseUiModel?`. `SearchBrowseUiModel` is a sealed interface in
`model/SearchBrowseUiModel.kt`:

```text
Genre(genre: BrowseGenreUiModel)                 // 014's genre browse
Shelf(key: DiscoverShelfKey, title: UiText)      // this feature
```

Every site that reads `activeGenre` today changes, as follows:

| Site | Today | After |
|------|-------|-------|
| `SearchUiState` | `activeGenre` field + KDoc invariant | `activeBrowse` + invariant extended (see below) |
| `SearchViewModel.updateSearchContent` | `keepServerOrder = activeGenre != null` | `keepSourceOrder = activeBrowse != null` |
| `SearchViewModel.performSearch` | `activeGenre = null`, label off | `activeBrowse = null`, label off |
| `SearchViewModel.browseGenre` | `activeGenre = genre`, label on | `activeBrowse = Genre(genre)`, label on |
| `SearchViewModel.clearSearch` | `activeGenre = null`, label off | `activeBrowse = null`, label off |
| `SearchViewModel.retrySearch` | `activeGenre?.let(::browseGenre) ?: performSearch` | exhaustive `when` (R6) |
| `SearchViewModel.withRelevanceLabel(isGenreBrowse)` | private helper | `isBrowse` |
| `SearchViewModel.openShelf` | n/a | **new**: `activeBrowse = Shelf(...)`, label on |
| `SearchScreen` `BackHandler` | `text.isNotEmpty() \|\| activeGenre != null` | `\|\| activeBrowse != null` |
| `SearchScreen` → `SearchMainContent` | `activeGenre = uiState.activeGenre` | `activeBrowse = uiState.activeBrowse` |
| `SearchBars.SearchTopBar` | `activeGenreLabel = activeGenre?.label` | `browsePlaceholder = activeBrowse?.toPlaceholder()` |
| `SearchBars.SearchInputField` | formats "genre: %1$s"; X if `text \|\| label != null` | renders the given placeholder; X if `text \|\| placeholder != null` |
| `SearchMainContent` empty state | genre message if `activeGenre != null` | genre message if `activeBrowse is Genre` |
| `SearchUiMapper.withRelevanceLabel(isGenreBrowse)` | parameter name | `isBrowse` |

Invariants, both kept in `SearchUiState`'s KDoc and tested:
- `activeBrowse != null ⇒ contentState !is Idle`. This is 014's invariant, extended.
- `activeBrowse is Shelf ⇒ contentState is Success`. This one is new, and R6 relies on it.

**Rationale**:
- "Is some browse active?" is asked in five places: the clear button, the back handler, the "Top picks"
  label, `keepSourceOrder` and the "Relevance"/"Top picks" switch. With one field each place stays a single
  null check. A sibling field would turn each into `activeGenre != null || activeShelf != null` and allow an
  impossible state where both are set.
- The two cases differ only where they should: where the placeholder comes from, which empty message is
  shown, and whether there is anything to retry. Each of those is an exhaustive `when` on the sealed type, so
  a third kind of browse would be a compile error rather than a silent fallthrough.
- 014's R4 rejected storing a source inside `SearchContentState`, and that reasoning still holds.
  `contentState` stays the only thing that decides what the content area shows.

**Alternatives considered**:
- *A sibling `activeShelf: DiscoverShelfUiModel?`.* Rejected for the reasons above.
- *Keep `activeGenre` as a computed property on top of `activeBrowse`.* Rejected. It would be a second way
  to read the same state, kept only to avoid changing tests. The tests are updated instead (plan, "Existing
  tests").

---

## R4. The placeholder

**Decision**: `SearchUiMapper` gains `SearchBrowseUiModel.toPlaceholder(): UiText`:
- `Genre` → `UiText.StringResource(Res.string.genre_browse_placeholder, genre.label)`, the existing "genre:
  %1$s". The genre label is itself a `UiText`, and `UiText.asString()`/`resolve()` already resolve `UiText`
  arguments.
- `Shelf` → `title`, as it is. That is the same `UiText` the shelf's heading renders:
  `discover_popular_this_month`, `discover_most_anticipated`, or the formatted `discover_because_you_like`
  / `discover_more_from` with the signal's name.

`SearchInputField`'s `activeGenreLabel: UiText?` becomes `browsePlaceholder: UiText?`. The composable only
resolves it with `asString()`, uses it as the placeholder text and the `stateDescription`, and shows the
clear button while it is non-null. The genre format moves out of the composable into the mapper.

**Rationale**:
- A shelf's title already reads as a label for what is on screen ("Popular this month", "Because you like
  RPG"). A prefix would only add noise.
- Choosing the format by browse kind is a decision, so it belongs in a mapper (FR-029). The composable
  renders the given text, as it already does for the default placeholder.
- No hardcoded text. Every placeholder is a resource with positional arguments (FR-028).

**Alternatives considered**:
- *A sibling format, "shelf: %1$s".* Rejected. "shelf: Because you like RPG" reads badly, and "shelf" is
  not a word the UI uses anywhere else.
- *One generalised format for both, "browsing: %1$s".* Rejected. It changes 014's shipped wording for no
  gain.
- *Store the resolved placeholder in `SearchBrowseUiModel`.* Rejected. It would duplicate data that can be
  derived from the model, and a mapper is where this project keeps label choices.

---

## R5. Sort and filter for a shelf grid

**Decision**:
- **Sort**: reuse 014's mechanism as it is. `keepServerOrder` is renamed `keepSourceOrder`, because the order
  now comes from a server ranking *or* a shelf's order. It is true for any browse. `Success.allGames` holds the
  shelf's games **in shelf order**. "Top picks" descending gives that order, and ascending reverses it. The
  label is switched by `withRelevanceLabel(isBrowse = true)` when the shelf opens, and back when it closes.
  The user's selected sort and direction are not touched (FR-021, FR-022).
- **Filter**: the filters are built exactly as for a text search: platforms + genres + the initial game types,
  all unselected (FR-023). The facets come from `games.toSearchResult()` with **no** `excludedGenreId`, so
  every genre present in the shelf is offered.
- **Facet placement (owner decision, 2026-10-13)**: `toSearchResult` in
  `core/domain/.../usecase/search/SearchResultFacets.kt` goes from `internal` to public. The ViewModel calls
  it directly on the shelf's games. Spec FR-027 was amended to allow exactly this.

**Where the genre browse withholds a genre, and why the shelf path avoids it**: the exclusion lives in
`BrowseGenreUseCase.invoke`, which calls `toSearchResult(excludedGenreId = genreId)`, and nowhere else. The
shelf path never calls `BrowseGenreUseCase` (or any use case). It calls `toSearchResult()` with the default
`excludedGenreId = null`, which `SearchGamesUseCase` also uses. The test "a shelf grid offers every genre its
games carry" pins this down.

**Rationale**:
- A shelf is not defined by one genre. A "More from Larian" shelf spans genres, and even a "Because you like
  RPG" shelf's games carry other genres. Every genre in the shelf narrows something, so none is withheld.
- One facet rule. The "distinct, ordered by how often they occur" rule stays in one place, already covered by
  `SearchResultFacetsTest`. No module edge is added: `:feature:search` already depends on `:core:domain`.

**Alternatives considered** (presented to the owner):
- *A copy of the facet function in `:feature:search`.* This stays literally inside FR-027, but the ordering
  rule would then live in two places and could drift.
- *Move `toSearchResult` to `:core:model`.* This touches two modules and puts behaviour into a module that
  holds types.
- *A pass-through use case.* Rejected. It wraps a pure function and adds a constructor parameter and a Koin
  binding for nothing.

---

## R6. No loading or error state; retry; double tap; snapshot

**Decision**: `openShelf(shelf)` is synchronous. It does not launch a coroutine:

1. If `activeBrowse` is already `Shelf` with the same key, return. This handles the double tap.
2. `val games = discoverFeed?.shelfGames(shelf.key)`. If that is `null` or empty, return: the shelf is gone
   after a refresh.
3. `searchJob?.cancel(); searchJob = null`.
4. Empty the text field and clear the suggestions, as `browseGenre` does.
5. One `_uiState.update`: `activeBrowse = Shelf(key, title)`, sort label "Top picks", and
   `contentState = Success(games = sorted(games).toGameItemList(savedIds), filters = facets + game types,
   allGames = games)`.

**How `retrySearch` is guaranteed unreachable for a shelf, rather than assumed**:
- The Retry button exists only on `SearchContentState.Error` (`SearchMainContent` → `ErrorPage`).
- `Error`, `Loading` and `Empty` are written only inside the `searchJob` coroutines of `performSearch` and
  `browseGenre`. Before launching, both replace `activeBrowse` (with `null` or `Genre`).
- `openShelf` cancels `searchJob` *before* it sets `Shelf`. So no coroutine is left that could write
  `Error`, `Loading` or `Empty` while a shelf is open. `toRepositoryError()` rethrows `CancellationException`,
  so a cancelled job never reaches its `update`.
- Filtering and sorting (`updateSearchContent`) only ever write `Success`. Filtering a shelf grid down to
  nothing is `Success(games = [])`, which `SearchResultGrid` shows as the "no matches" state with "Clear
  filters" (spec US4 scenario 6). It is never `Empty`.
- So `activeBrowse is Shelf ⇒ contentState is Success` holds by construction. `retrySearch` becomes an
  exhaustive `when`. Its `Shelf` branch does nothing and carries a comment that cites the invariant. A test
  checks it: a shelf opened while a text search is still in flight is not overwritten when that search
  completes.

**Snapshot, not live**: the grid's games are copied into `Success.allGames` at the tap.
`observeDiscoverFeed` writes only `discoverFeed` and `uiState.discover`, so a refresh under an open grid
changes the feed waiting behind it but not the grid. The cards' saved state still follows the wishlist,
because `observeWishlistedGameIds` already patches `Success.games`. Save and long-press keep working, because
`findGame` looks in `Success.allGames` first.

**Rationale**: by the time the user can tap an arrow, everything the grid needs is in memory. A loading
state would flash for one frame and break FR-010.

---

## R7. The cap, the arrow, and what was reused

**Decision**:
- **Constant**: `internal const val DISCOVER_SHELF_ROW_SIZE = 6`, in `DiscoverMapper.kt`. It is `internal`
  so `DiscoverMapperTest` can use it.
  - Not `UiConstants`: that object holds values several features share (`SCROLL_TO_TOP_AFTER_ITEM_INDEX` is
    read by Search and the wishlist). Only this mapper reads the cap.
  - Not in the use case: the cap is display-only (FR-004).
- **Where the cap is applied**: in the mapper, `toDiscoverContentState`. It is never applied in a composable
  (FR-029) or in the ViewModel (the mapper is already the single place that builds the feed's UI state).
- **Shape**: the three shelf kinds become one model, `DiscoverShelfUiModel` (it replaces
  `RecommendedShelfUiModel`):

  ```text
  key: DiscoverShelfKey, title: UiText, games: List<GameItemUiModel> (≤ 6), hasMore: Boolean
  ```

  - `DiscoverContentState.Content` becomes `popular: DiscoverShelfUiModel?`,
    `upcoming: DiscoverShelfUiModel?` and `recommended: List<DiscoverShelfUiModel>`. A lane is `null` when it
    is empty, so the "render only if non-empty" check moves from the composable into the mapper.
  - `hasMore` is `shelfGames.size > DISCOVER_SHELF_ROW_SIZE`. It is plain data, and the composable only
    tests it.
  - The two lane titles move from `stringResource(...)` calls in `DiscoverFeed.kt` into the mapper as
    `UiText.StringResource`. That way every shelf renders `title.asString()`, and the same `UiText` reaches
    the placeholder (R4).
- **Title row** (`DiscoverShelf` in `DiscoverFeed.kt`, private):
  - It is a `Row`. The title is `Text(weight(1f), maxLines = 1, overflow = Ellipsis)` at the start. When
    `hasMore`, an `Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null)` sits at the
    end, sized `MaterialTheme.spacing.mediumLarge` and tinted `colorScheme.primary`, exactly like
    `SeeAllResultsRow` in `SuggestionRow.kt`.
  - Only when `hasMore`, the row gets `minimumInteractiveComponentSize()` for the 48dp target,
    `clickable(role = Role.Button)` and `clearAndSetSemantics { contentDescription = <see all label> }`, so it
    is announced as one button: "See all: Popular this month".
  - Without `hasMore`, the row is the plain heading it is today: not clickable, no icon, no change in
    semantics.
  - Horizontal padding stays `MaterialTheme.spacing.large`.

**What was searched first**:
- `core/ui/component/`: nothing is a section heading with a trailing action. `CustomContentCard`,
  `GameListRow`, `PlatformRow` and `MainScreenHeader` are all something else.
- `core/ui/util/modifiers/` (`VisualModifiers`, `ShimmerModifiers`, `MetallicModifiers`, `LineShimmerState`):
  nothing for touch targets or headings. So the row stays a private composable in its one caller.
- **Forward arrows in use**:
  - `Icons.AutoMirrored.Filled.ArrowForwardIos` in `feature/search/.../SuggestionRow.kt` (the "See all results
    for" row) and in `feature/game-detail/.../GameReleaseInfoCard.kt`.
  - `Icons.AutoMirrored.Filled.KeyboardArrowRight` in `feature/settings/.../SettingsRow.kt`.
  - `Icons.Default.ChevronRight` in `feature/lists/.../WishlistRow.kt`. That one is *not* auto-mirrored. It
    is noted here, not fixed.
  - `ArrowForwardIos` is chosen: it is this module's own "see all" glyph and it mirrors in RTL (FR-008).
- **48dp**: there is no 48dp token in `Spacing`, and adding one for a touch target would be wrong, since
  spacing tokens are gaps. Material 3's `Modifier.minimumInteractiveComponentSize()` reserves the platform
  minimum (48dp) without a literal. `IconButton` uses it internally. It is new to this codebase, so the
  quickstart checks the size on both platforms.

**Alternatives considered**:
- *An `IconButton` for the arrow (48dp built in) plus a clickable title.* Rejected. That makes two focusable
  controls for one action, so TalkBack would stop twice (spec Edge Cases, Accessibility).
- *Cap in the composable with `games.take(6)`.* Rejected by FR-029, and it would separate "has more" from the
  data that defines it.
- *Keep `popular`/`upcoming` as `List<GameItemUiModel>` with two `hasMore*` booleans beside them.* Rejected.
  It leaves three shapes for one concept, and the composable would have to build the lane keys and titles
  itself, which is identity logic in the UI layer.

---

## R8. Resources

**Decision**: one new string, in `feature/search/src/commonMain/composeResources/values/strings.xml`:

| Name | Value | Use |
|------|-------|-----|
| `discover_see_all_shelf` | `See all: %1$s` | The title row's spoken label. `%1$s` is the shelf's resolved title. |

The lane titles reuse `discover_most_anticipated` and `discover_popular_this_month`. The personalised titles
reuse `discover_because_you_like` and `discover_more_from`. No `:core:ui` string is added, so no `CoreUiRes`
import is needed. Inside `:feature:search` the module's own `Res` is imported bare, with one import per
accessor.

**Locales**: `:feature:search` and `:core:ui` ship only `values/` (English). There are no other locale
directories, so there is nothing to translate. The quickstart checks this rather than assuming it.

---

## R9. Scroll position on return (FR-018)

**Finding**: it already holds, so FR-018 is **not** deferred.
- `SearchScreenContent` creates `discoverListState = rememberLazyListState()` at the screen level, above
  `SearchMainContent`'s `when (contentState)`. It passes the state down to `DiscoverFeed`.
- When the grid replaces the feed, the `LazyColumn` leaves composition, but its `LazyListState` does not.
  It belongs to `SearchScreenContent`, which stays composed. When `contentState` returns to `Idle`, the
  `LazyColumn` is recomposed with the same state, at the same first visible item and offset.
- `rememberLazyListState` is backed by `rememberSaveable(LazyListState.Saver)`, so the position also
  survives rotation while the grid is open.

**One existing behaviour, not changed**: the search bar's `scrollBehavior` is recreated on every switch
between feed and results (`key(isDiscoverActive)`). On return the bar is shown in full even if it had been
scrolled away. A text search behaves the same way today.

**Needed for the grid, not the feed**: the results grid shares one `gridState` across every search and
browse. Opening a shelf goes through a new `onOpenShelf` lambda in `SearchScreenContent`, built like
`onBrowseGenre`: `scope.launch { onResetScroll() }`, then send the event. The grid therefore starts at the
top, not where a previous search left it. `onResetScroll` never touches `discoverListState`.

---

## R10. Rotation and process death

- All of the grid's state lives in `SearchViewModel._uiState`: `activeBrowse`, `contentState`, and the sort
  and filter sheets. The ViewModel is kept across configuration changes, so the grid survives rotation
  (FR-024). iOS has no rotation-driven recreation, and the quickstart covers backgrounding there instead.
- Nothing is written to `SavedStateHandle` (not used in this project), Room or DataStore. After process
  death the ViewModel is new, so `activeBrowse = null` and `contentState = Idle`, and the user lands on the
  Discover feed (FR-025).

---

## R11. Known deviations touched, and follow-ups

- `docs/tech-debt.md`:
  - **"More from <studio>" misses multi-studio franchises.** This is about which shelves exist. The feature
    changes only how they are shown, so it is unaffected and left alone.
  - **iOS scroll smoothness never measured on a device.** The cap shortens the Discover rows, but this plan
    does not claim it as a fix. The entry stays.
  - **Coverage line.** `usecase/search/`'s shared facet function is already listed as covered. It stays
    covered after the visibility change, so the line needs no edit.
- 014's plan left this follow-up: "Because you like %1$s" still formats IGDB's raw genre name ("Role-playing
  (RPG)") rather than `Genre.toLabelUiText()`. That title is now also the grid's placeholder. It is still out
  of scope here (spec: shelf titles unchanged) and stays a follow-up in the plan.

---

## Values fixed by this plan

| Value | Initial | Where |
|-------|---------|-------|
| Games per Discover row | 6 | `DISCOVER_SHELF_ROW_SIZE`, `DiscoverMapper.kt` |
| Arrow shown when | shelf holds > 6 games | `DiscoverShelfUiModel.hasMore`, set by the mapper |
| Arrow glyph | `Icons.AutoMirrored.Filled.ArrowForwardIos`, `spacing.mediumLarge` | `DiscoverFeed.kt` |
| Spoken label | `See all: %1$s` | `discover_see_all_shelf` |
