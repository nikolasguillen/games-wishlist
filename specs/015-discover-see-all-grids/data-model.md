# Data Model: See-All Grids for Discover Rows

**Feature**: [spec.md](spec.md) | **Plan**: [plan.md](plan.md) | **Research**: [research.md](research.md)

Nothing is persisted. There is no Room entity, no DataStore key, no schema change and no new domain type. The
open shelf lives in `SearchViewModel` for the session only (FR-025). Below are the new and changed types,
layer by layer.

---

## `:core:model` (unchanged, relied on)

- `DiscoverFeed(popular, upcoming, recommended, hasStaleRecommendations)` is the source of every shelf's games.
- `DiscoverLane { MOST_ANTICIPATED, POPULAR_THIS_MONTH }` names the two generic lanes inside
  `DiscoverShelfKey.Lane`.
- `RecommendedShelf(reason, games)` and `ShelfReason.ByGenre(genre)` / `ByDeveloper(developer)` give
  `genre.id` and `developer.id`, which key the personalised shelves.
- `SearchResult(games, platforms, genres)` is the return type of the facet function.

## `:core:domain`

### `List<Game>.toSearchResult(excludedGenreId: Int? = null): SearchResult` (visibility change only)

`usecase/search/SearchResultFacets.kt`: `internal` becomes public. The body does not change. Its KDoc gains
one line naming its third caller: Search's shelf grid, which passes no exclusion. `SearchResultFacetsTest`
already covers the behaviour. This was an owner decision (research R5), and spec FR-027 was amended to allow it.

---

## `:feature:search`

### `DiscoverShelfKey` (new, `model/DiscoverShelfKey.kt`)

```text
@Immutable sealed interface DiscoverShelfKey {
    data class Lane(val lane: DiscoverLane) : DiscoverShelfKey
    data class ByGenre(val genreId: Int) : DiscoverShelfKey
    data class ByDeveloper(val developerId: Int) : DiscoverShelfKey
}
```

Which shelf a row or a grid is. It is never a display name (FR-012). Within one feed it is unique:
`GetDiscoverFeedUseCase` builds at most one shelf per signal, and developer and genre shelves are separate
cases. It is a sealed hierarchy, so the cases share the file.

### `DiscoverShelfUiModel` (new, `model/DiscoverShelfUiModel.kt`; replaces `RecommendedShelfUiModel.kt`, which is deleted)

| Field | Type | Meaning |
|-------|------|---------|
| `key` | `DiscoverShelfKey` | What the arrow opens. |
| `title` | `UiText` | The heading, and the grid's placeholder: `discover_most_anticipated`, `discover_popular_this_month`, or `discover_because_you_like` / `discover_more_from` with the signal's name. |
| `games` | `List<GameItemUiModel>` | The row: the first `DISCOVER_SHELF_ROW_SIZE` (6) games of the shelf, in shelf order. |
| `hasMore` | `Boolean` | `true` when the shelf holds more than 6 games. That is the only case with an arrow (FR-002, FR-005). |

`@Immutable`. Rule: `games == shelfGames(key).take(6)` mapped to UI models, and `hasMore == (shelfGames(key).size > 6)`.

### `DiscoverContentState.Content` (changed)

| Field | Before | After |
|-------|--------|-------|
| `popular` | `List<GameItemUiModel>` | `DiscoverShelfUiModel?`, `null` when the lane is empty |
| `upcoming` | `List<GameItemUiModel>` (hero already removed) | `DiscoverShelfUiModel?` (hero still removed), `null` when empty |
| `recommended` | `List<RecommendedShelfUiModel>` | `List<DiscoverShelfUiModel>` |
| `hero`, `isStale`, `isRefreshing` | unchanged | unchanged |

`observeWishlistedGameIds` patches `popular?.games`, `upcoming?.games` and each `recommended[i].games`, as it
patches the lists today.

### `SearchBrowseUiModel` (new, `model/SearchBrowseUiModel.kt`)

```text
@Immutable sealed interface SearchBrowseUiModel {
    data class Genre(val genre: BrowseGenreUiModel) : SearchBrowseUiModel           // spec 014
    data class Shelf(val key: DiscoverShelfKey, val title: UiText) : SearchBrowseUiModel
}
```

This is what the committed grid is showing when it is not a typed query. The placeholder comes from it through
`toPlaceholder()` (below).

### `SearchUiState` (changed)

| Field | Before | After |
|-------|--------|-------|
| `activeGenre: BrowseGenreUiModel?` | 014's browse marker | **removed** |
| `activeBrowse: SearchBrowseUiModel?` | n/a | `null` = no browse; `Genre` = genre browse; `Shelf` = shelf grid |

Invariants (KDoc, and tested):
- `activeBrowse != null ⇒ contentState !is SearchContentState.Idle`
- `activeBrowse is SearchBrowseUiModel.Shelf ⇒ contentState is SearchContentState.Success`

### `SearchUiEvent` (changed)

`data class OnShelfSeeAllTriggered(val shelf: DiscoverShelfUiModel) : SearchUiEvent`: the user tapped a
shelf's title row (or its arrow).

### `SearchContentState` (unchanged)

A shelf grid only ever uses `Success`. `games` are sorted per the selected sort, with "Top picks" giving
shelf order. `filters` are platforms + genres from `toSearchResult()` + the initial game types, all
unselected. `allGames` holds the shelf's games **in shelf order**. When filters leave nothing, the state is
`Success(games = [])`, which renders as "no matches".

### Mappers

`mapper/DiscoverMapper.kt`:
- `internal const val DISCOVER_SHELF_ROW_SIZE = 6`
- `internal fun DiscoverFeed.shelfGames(key: DiscoverShelfKey): List<Game>?` is the single definition of a
  shelf's games. `Lane(MOST_ANTICIPATED)` is `upcoming.drop(1)` (the hero is split out),
  `Lane(POPULAR_THIS_MONTH)` is `popular`, and the personalised keys match the shelf by its reason's id. It
  returns `null` when no shelf matches.
- `toDiscoverContentState(savedIds)` builds every shelf through `shelfGames` and caps it. An empty lane
  becomes `null`.

`mapper/SearchUiMapper.kt`:
- `withRelevanceLabel(isBrowse: Boolean)`, renamed from `isGenreBrowse`. Behaviour is unchanged.
- `SearchBrowseUiModel.toPlaceholder(): UiText`. `Genre` gives `genre_browse_placeholder(genre.label)`, and
  `Shelf` gives `title`.

### Strings (`feature/search/.../values/strings.xml`)

| Name | Value |
|------|-------|
| `discover_see_all_shelf` | See all: %1$s |

---

## State transitions

`S` = `SearchUiState`. `text` = the `TextFieldState`'s content. Only the rows this feature adds or changes
are listed. 014's genre rows hold with `activeGenre` read as `activeBrowse is Genre`.

| From | Trigger | `activeBrowse` | `contentState` | Other effects |
|------|---------|----------------|----------------|---------------|
| feed (`Idle`) | `OnShelfSeeAllTriggered(s)`, shelf found | `Shelf(s.key, s.title)` | `Success` at once, **no** `Loading` | cancel `searchJob`; empty `text`; clear suggestions; label → "Top picks"; no history write; no use case called |
| feed | `OnShelfSeeAllTriggered(s)`, `shelfGames` is `null` or empty | unchanged (`null`) | unchanged (`Idle`) | nothing |
| shelf open | `OnShelfSeeAllTriggered(s)` with the same key | unchanged | unchanged | nothing (double tap) |
| shelf open | `OnClearSearch` (X, back arrow, system back) | `null` | `Idle` | label → "Relevance"; the feed comes back at its scroll position |
| shelf open | `OnSearchTriggered(q)` | `null` | `Loading` → … | label → "Relevance"; `q` written to history |
| shelf open | `OnGenreBrowseTriggered(g)` | `Genre(g)` | `Loading` → … | label stays "Top picks" |
| shelf open | `OnSortChanged`, filter apply/clear | unchanged | `Success`, re-sorted/filtered | "Top picks" = shelf order (desc) or reversed (asc) |
| shelf open | a new feed emission | unchanged | unchanged | only `discover` changes (snapshot) |
| shelf open | wishlisted ids change | unchanged | `Success.games[*].isSaved` patched | as for any result |
| shelf open | `OnRetrySearch` | unreachable: no `Error` can exist while `Shelf` is set (research R6) | n/a | the `when` branch does nothing |
| shelf open | rotation | unchanged | unchanged | ViewModel retained; `discoverListState` saved |
| any | process death | `null` | `Idle` | ViewModel recreated (FR-025) |
