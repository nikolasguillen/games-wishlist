# Data Model: Search Dropdown on Wide Windows

Phase 1 of [plan.md](./plan.md). The feature changes how the panel is presented, not what it shows, so there is
**no change** to `SearchUiState`, `SearchUiEvent`, `SearchUiEffect`, the ViewModel, the mappers that feed the
overlay, the domain, data, network or database layers. Everything below is UI-layer state inside `:feature:search`.

## Data read (unchanged)

The panel reads exactly what the full-screen overlay reads today:

| Source | Field | Used for |
|---|---|---|
| `SearchUiState` | `selectedSearchType: SearchType` | the type chips |
| `SearchUiState` | `browseGenres: List<BrowseGenreUiModel>` | the genre quick-browse (five genres) |
| `SearchUiState` | `history: SearchHistoryUiModel` (`queries`, at most 10; `games`, at most 10) | the empty-query panel |
| `SearchUiState` | `suggestions: SearchSuggestionsUiModel` (`historySuggestions`, at most 5; `gameSuggestions`, at most 4; `isLoadingRemote`) | the typing panel |
| `SearchViewModel` | `textFieldState: TextFieldState` | the query, its caret and selection |

## New: `SearchDropdownItemUiModel` (model/SearchDropdownItemUiModel.kt)

A sealed interface, one closed set kept in one file: one actionable item of the wide panel. Each variant's equality
is its identity, which is what the highlight is keyed by (a data class or data object per variant).

| Variant | Carries | Activation (same callback the overlay uses) |
|---|---|---|
| `TypeChip` | `type: SearchType` | `OnSearchTypeSelected(type)`; the panel stays open |
| `Genre` | `genreId: Int` | the screen's `onBrowseGenre(genre)`: browse, panel collapses |
| `ClearHistory` | — | opens `ClearHistoryDialog` |
| `RecentQuery` | `query: String` | the screen's `onSearch(query)`: commit, panel collapses |
| `RecentGame` | `gameId: Int` | the screen's `onGameClick(gameId)`: open, panel collapses after the delay |
| `HistorySuggestion` | `query: String` | `onSearch(query)` |
| `GameSuggestion` | `gameId: Int` | `onGameClick(gameId)` |
| `SeeAllResults` | — | `onSearch(<current text>)` |

Shift+Delete (research R3) applies only to `RecentQuery` (opens `RemoveHistoryItemDialog`) and `RecentGame`
(`OnRecentGameRemoved`).

Not items: section headers, the loading rows and the empty-state message. They are never highlighted.

## New: the reading order (mapper/SearchDropdownItemMapper.kt)

`searchDropdownItems(query, browseGenres, history, suggestions): List<SearchDropdownItemUiModel>`, a pure function that
returns the items in the order the panel draws them:

- **Query blank** (empty or only spaces):
  `TypeChip × 3 (SearchType.entries order)` → `Genre × browseGenres` → `ClearHistory` (only if `history.queries`
  is not empty) → `RecentQuery × history.queries` → `RecentGame × history.games`.
- **Query not blank**:
  `TypeChip × 3` → `HistorySuggestion × suggestions.historySuggestions` → `GameSuggestion × suggestions.gameSuggestions`
  (none while `isLoadingRemote`) → `SeeAllResults`.

The composables draw the sections in this same order. The function's KDoc and the panel's KDoc each point at the
other, and the order is pinned by `SearchDropdownItemsTest`.

## New: the highlight (components/SearchDropdownNavigation.kt)

| Element | Type | Owner |
|---|---|---|
| highlighted item | `SearchDropdownItemUiModel?` (`null` = the text field) | `remember { mutableStateOf(null) }` in `WideSearchLayout` |

Pure rules, tested by `SearchDropdownNavigationTest`:

- `highlightNext(items, current): SearchDropdownItemUiModel?` (Down)
  - from `null` → `items.first()` (or `null` when `items` is empty);
  - from item *i* → item *i+1*; from the last item → the last item (no wrap).
- `highlightPrevious(items, current): SearchDropdownItemUiModel?` (Up)
  - from item *i* → item *i−1*; from the first item → `null`;
  - from `null` → `null`.
- In both, a `current` not in `items` is treated as `null`.
- `reconcileHighlight(items, current): SearchDropdownItemUiModel?` → `current` if it is still in `items`, else `null`.

State transitions:

```text
            Down (items not empty)                 Down / Up
  [none] ───────────────────────────► [item i] ◄───────────────► [item j]
     ▲            Up from first item        │
     └──────────────────────────────────────┤
     ▲   panel collapses (any reason)       │
     └──────────────────────────────────────┤
     ▲   item i no longer in the list       │
     └──────────────────────────────────────┘
  pointer moves over item k  ──►  [item k]   (from any state)
```

The highlight is not saved. It resets when the window crosses 840dp, which the spec allows.

## Moved: history dialog state

| Field | Type | Before | After |
|---|---|---|---|
| `queryToRemove` | `String?`, saved | inside `ExpandedSearchBar` | `SearchHistoryDialogState`, created in `SearchScreenContent`, passed to both presentations |
| `showClearHistoryDialog` | `Boolean`, saved | inside `ExpandedSearchBar` | `SearchHistoryDialogState`, created in `SearchScreenContent`, passed to both presentations |

Same types, same saver, same dialogs, same events on confirm (`OnHistoryItemRemoved`, `OnClearHistory`). Only the
owner moves (research R6).

## Strings

No new strings. Every visible or announced label already exists: the type labels (`SearchType.toLabelUiText()`), the
genre labels, `browse_by_genre`, `recent_searches`, `clear_all`, `recently_viewed`, `suggestions_section_recent`,
`suggestions_section_games`, `see_all_results_for`, `expanded_search_initial_message`, and the two dialogs' strings.
