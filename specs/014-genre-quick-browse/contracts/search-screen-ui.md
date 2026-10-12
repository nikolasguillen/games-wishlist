# Contract: Search screen — genre browse UI

**Feature**: [../spec.md](../spec.md) | **Data model**: [../data-model.md](../data-model.md)

What the Search screen shows and announces, as a function of `SearchUiState` and the `TextFieldState`.
Composables only render this and send events (Constitution III). Every decision below is already made in
the state.

Shorthand: `browsing = uiState.activeGenre != null`, `typed = textFieldState.text.isNotEmpty()`.

---

## Expanded overlay (`ExpandedSearchBar`)

| Condition | Content, top to bottom |
|-----------|------------------------|
| `typed` (non-blank) | Suggestions, unchanged. **No genre section** (FR-002). |
| not `typed`, `browseGenres` not empty, history empty | "Browse by genre" section, then `expanded_search_initial_message`. |
| not `typed`, `browseGenres` not empty, history not empty | "Browse by genre" section, then Recent searches, then Recently viewed. |
| not `typed`, `browseGenres` empty | As today (no genre section). |

**Genre section** (`components/GenreBrowseSection.kt`):
- A `titleMedium` "Browse by genre" heading, padded the way the "Recent searches" heading is.
- `CustomChipRow` with the five `GenreChip`s, keyed by `id`.
- `GenreChip` is an `AssistChip` showing `label`, with the selected-filter-chip colours and border, and **no
  icon** (FR-003).
- Semantics: role **Button**, text = the genre label, **no selected / toggle state** (FR-004).
- Tap: `onBrowseGenre(genre)`, defined in `SearchScreenContent`, which resets the grid scroll, collapses the
  bar and sends `OnGenreBrowseTriggered(genre)` (FR-012).

---

## Search input field (`SearchInputField`)

| | `browsing` | not `browsing` |
|-|------------|----------------|
| Placeholder (visible only when not `typed`) | `genre_browse_placeholder` with the genre label: "genre: RPG" (FR-020) | `search_placeholder` |
| Field semantics | `stateDescription` = the same "genre: RPG" text | unchanged (placeholder hidden from semantics) |
| Clear (X) visible | always (FR-021) | when `typed` |
| Clear (X) action | `OnClearSearch` | `OnClearSearch` |

---

## Collapsed bar and back

- The back-to-Discover arrow is shown when `contentState !is Idle`, unchanged, so it is shown during any
  browse. Action: `OnClearSearch` (FR-022).
- `BackHandler` is enabled when the bar is collapsed and (`typed` || `browsing`). Action: `OnClearSearch`.
- The sub-header (result count, sort, filter) appears for a browse's `Success`, as for a text search.

---

## Content area (`SearchMainContent`)

| `contentState` | During a browse |
|----------------|-----------------|
| `Loading` | `LoadingPage`, unchanged |
| `Success` | `SearchResultGrid`, unchanged: same cards, save, long-press, active-filter chips (FR-016) |
| `Empty` | `EmptySearchPlaceholder` with `genre_browse_no_results` formatted with the genre label; action "Clear search" → `OnClearSearch` (FR-019) |
| `Error` | `ErrorPage`; Retry → `OnRetrySearch`, which re-runs the browse (FR-017) |

`SearchMainContent` gets `activeGenre: BrowseGenreUiModel?` only to pick the empty-state message.

---

## Sheets

- **Sort**: unchanged composable. It renders `sorting[i].label`, which the ViewModel has already set to
  "Top picks" or "Relevance" (FR-025, FR-026).
- **Filter**: unchanged composable. The genre group lists `GameFilterUiModel.Genre` entries, which never
  include the browsed genre (FR-027). When no genre is left, the section is not drawn: that is the existing
  `if (filters.isEmpty()) return`. Genre labels now come from `toLabelUiText()`, for a text search too.

---

## Previews to add

- `GenreBrowseSection`: five chips.
- `ExpandedSearchBar`: with history and genres; empty history with genres.
- `SearchInputField`: browsing with an empty field (genre placeholder and X).
- `SearchMainContent`: a browse's empty state.
