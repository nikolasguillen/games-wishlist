# UI Contract: Search Dropdown on Wide Windows

Phase 1 of [plan.md](./plan.md). The internal composable surface of `:feature:search` after the change, and the one
boundary with `:shared`. Signatures show parameters, not bodies; everything is `internal` unless marked public.
Parameter order follows the module's convention: required first, `modifier` as the first optional.

## Boundary with `:shared` (unchanged)

```kotlin
// public, feature/search/SearchScreen.kt: signature unchanged since spec 021
fun SearchScreen(
    viewModel: SearchViewModel,
    onGameClick: (Int) -> Unit,
    onCharacterClick: (Int) -> Unit,
    onDeveloperClick: (Int) -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
    isWideWindow: Boolean = false
)
```

`QuestLogNavDisplay` already passes `isWideWindow = twoPaneEnabled` (the 840dp rule `QuestLogRoot` computes). No
edit in `:shared`.

## The single dispatch (research R1)

```kotlin
// SearchScreen.kt: the only place in Search that reads isWideWindow
internal fun SearchScreenContent(/* unchanged parameters */) {
    // shared state: searchBarState, isScrolled, onSearch, onBrowseGenre, onOpenShelf, on*ClickWithCollapse,
    // NavigationBackHandler, backgroundColor, and two holders the layouts receive whole:
    //   scrollState: SearchScrollState (scrollBehavior, gridState, discoverListState, showScrollToTop, onScrollToTop)
    //   historyDialogs: SearchHistoryDialogState (rememberSaveable, moved here: research R6)
    if (isWideWindow) WideSearchLayout(/* shared state and callbacks */)
    else CompactSearchLayout(/* shared state and callbacks */)
}
```

```kotlin
// components/CompactSearchLayout.kt: today's Scaffold, moved verbatim
internal fun CompactSearchLayout(
    uiState: SearchUiState,
    textFieldState: TextFieldState,
    searchBarState: SearchBarState,
    scrollState: SearchScrollState,             // see below
    snackbarHostState: SnackbarHostState,
    backgroundColor: Color,
    historyDialogs: SearchHistoryDialogState,   // see below
    onEvent: (SearchUiEvent) -> Unit,
    onSearch: (String) -> Unit,
    onBrowseGenre: (BrowseGenreUiModel) -> Unit,
    onOpenShelf: (DiscoverShelfUiModel) -> Unit,
    onGameClick: (Int) -> Unit,                 // already wrapped with the collapse
    onCharacterClick: (Int) -> Unit,
    onDeveloperClick: (Int) -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
)

// components/WideSearchLayout.kt: same parameters; owns the highlight (research R7) and the key handler (R3)
internal fun WideSearchLayout(/* same parameters as CompactSearchLayout */)
```

Two small `@Stable` holders keep the layouts' parameter lists short; each is one class in its own file under
`components/`, and the screen owns both:

- `SearchHistoryDialogState` (`queryToRemove: String?` and `showClearHistoryDialog: Boolean` as mutable properties,
  `isAnyOpen`, a `Saver`; created by `rememberSearchHistoryDialogState()`, saved across rotation).
- `SearchScrollState` (`scrollBehavior`, `gridState`, `discoverListState`, `showScrollToTop`, `onScrollToTop`).

Both layouts render, in the same order: `Scaffold { topBar = SearchTopBar(...), floatingActionButton =
ScrollToTopFab, content = SearchMainContent(...) + SearchFilterBottomSheet + SearchSortBottomSheet + ListSelectorSheet
+ StatusBarProtection }`. They differ in two slots only, today:

| Slot | `CompactSearchLayout` | `WideSearchLayout` |
|---|---|---|
| `SearchTopBar.expandedContent` | `ExpandedSearchBar` (full-screen, unchanged) | `WideSearchDropdown` |
| `SearchMainContent.discoverFeed` | `DiscoverFeed` | `WideDiscoverFeed` |

## Changed shared pieces (defaults keep the compact behaviour identical)

```kotlin
// components/SearchBars.kt
internal fun SearchTopBar(
    uiState: SearchUiState,
    searchBarState: SearchBarState,
    textFieldState: TextFieldState,
    scrollBehavior: SearchBarScrollBehavior,
    onSearch: (String) -> Unit,
    onEvent: (SearchUiEvent) -> Unit,
    onProfileClick: () -> Unit,
    backgroundColor: Color,
    expandedContent: @Composable (inputField: @Composable () -> Unit, colors: SearchBarColors, shape: Shape) -> Unit,
    inputFieldModifier: Modifier = Modifier,       // wide: the key handler
    readOnlyWhenCollapsed: Boolean = true          // wide: false (research R4)
)
// onGameClick and onBrowseGenre leave SearchTopBar: they are captured by the expandedContent each layout passes.

internal fun SearchInputField(
    textFieldState: TextFieldState,
    searchBarState: SearchBarState,
    onSearch: () -> Unit,
    onClearSearch: () -> Unit,
    modifier: Modifier = Modifier,                 // NEW, passed to SearchBarDefaults.InputField
    shape: Shape = SearchBarDefaults.inputFieldShape,
    browsePlaceholder: UiText? = null,
    searchType: SearchType = SearchType.GAMES,
    readOnlyWhenCollapsed: Boolean = true          // NEW
)

internal fun ExpandedSearchBar(
    /* unchanged parameters */,
    historyDialogs: SearchHistoryDialogState                                             // NEW (hoisted, R6)
)

// components/SearchMainContent.kt: isWideWindow removed; the Discover feed becomes a slot
internal fun SearchMainContent(
    /* unchanged parameters except isWideWindow, which is removed */,
    discoverFeed: @Composable (content: DiscoverContentState.Content, listState: LazyListState) -> Unit
)
// DiscoverContent keeps Loading and Error; its Content branch calls discoverFeed.

// components/SearchTypeChipRow.kt
internal fun SearchTypeChipRow(
    selected: SearchType,
    onSelected: (SearchType) -> Unit,
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.SpaceEvenly,  // NEW: the dropdown start-aligns
    chipModifier: (SearchType) -> Modifier = { Modifier }   // NEW: applied to a Box around each chip
// The type chips are always a single row (three chips; it scrolls if the space is narrower), in the overlay and in
// the dropdown alike. Only the genres and the recent searches wrap.
)

// components/GenreBrowseSection.kt
internal fun GenreBrowseSection(
    genres: List<BrowseGenreUiModel>,
    onGenreClick: (BrowseGenreUiModel) -> Unit,
    modifier: Modifier = Modifier,
    wrap: Boolean = false,                                          // NEW
    chipModifier: (BrowseGenreUiModel) -> Modifier = { Modifier }   // NEW
)

// components/RecentSearchesSection.kt: MOVED out of ExpandedSearchHome.kt (private → internal), with RecentSearchChip
internal fun RecentSearchesSection(
    recentSearches: List<String>,
    wrap: Boolean,
    onClearRecentSearches: () -> Unit,
    onHistoryItemClicked: (String) -> Unit,
    onShowRemovalDialog: (String) -> Unit,
    clearAllModifier: Modifier = Modifier,                  // NEW
    chipModifier: (String) -> Modifier = { Modifier }       // NEW
)
```

`SuggestionRow.kt` (`HistorySuggestionRow`, `GameSuggestionRow`, `SeeAllResultsRow`, `LoadingSuggestionRow`,
`SuggestionSectionHeader`) is reused as is: each already takes a `modifier`.

## New composables

```kotlin
// components/WideSearchDropdown.kt
// ExpandedDockedSearchBarWithGap(state, inputField, shape = <bar pill>, dropdownScrimColor = Color.Unspecified,
//   dropdownGapSize = <spacing token>, colors, properties = PopupProperties(focusable = true,
//   clippingEnabled = false, dismissOnClickOutside = !historyDialogs.isAnyOpen))
// containing WideSearchPanel, RemoveHistoryItemDialog and ClearHistoryDialog.
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
internal fun WideSearchDropdown(
    searchBarState: SearchBarState,
    inputField: @Composable () -> Unit,
    searchBarShape: Shape,
    searchBarColors: SearchBarColors,
    uiState: SearchUiState,
    query: String,
    onActivate: (SearchDropdownItemUiModel) -> Unit,
    onRemoveRecentGame: (Int) -> Unit,
    historyDialogs: SearchHistoryDialogState,
    onConfirmRemoveQuery: (String) -> Unit,
    onConfirmClearHistory: () -> Unit,
    highlighted: SearchDropdownItemUiModel? = null,                      // only for the screen-reader announcement
    itemModifier: (SearchDropdownItemUiModel) -> Modifier = { Modifier } // hover, mark and bring-into-view; the layout builds it
)

// components/WideSearchPanel.kt: chooses the typing or the empty-query content (by the query, not by size),
// a vertically scrolling Column, plus the invisible live region that announces the highlight (research R9).
internal fun WideSearchPanel(
    uiState: SearchUiState,
    query: String,
    onActivate: (SearchDropdownItemUiModel) -> Unit,
    onRemoveRecentGame: (Int) -> Unit,
    onShowRemovalDialog: (String) -> Unit,
    modifier: Modifier = Modifier,
    highlighted: SearchDropdownItemUiModel? = null,
    itemModifier: (SearchDropdownItemUiModel) -> Modifier = { Modifier }
)

// components/WideSearchHome.kt: before typing. Type chips (one row), genres (wrap), recent searches (wrap) with
// "Clear all", recently viewed games as a FlowRow of RecentGameCard, or InitialMessage when there is no history.
internal fun WideSearchHome(/* the WideSearchPanel parameters it needs */)

// components/WideSearchSuggestions.kt: while typing. Type chips (one row), then the overlay's suggestion rows under
// the same headers, then SeeAllResultsRow.
internal fun WideSearchSuggestions(/* the WideSearchPanel parameters it needs */)

// components/SearchPanelItem.kt: the one modifier every actionable panel item wears (research R9)
// The mark's shape follows the item's own: MaterialTheme.shapes.small for the chips, "Clear all" and the recent
// game cards, MaterialTheme.shapes.medium for a row.
internal fun Modifier.searchPanelItem(
    item: SearchDropdownItemUiModel,
    highlighted: SearchDropdownItemUiModel?,
    onHighlight: (SearchDropdownItemUiModel) -> Unit
): Modifier
```

Each new composable file gets `@QuestLogPreviews` previews wrapped in `QuestLogTheme`. The panel previews also get
a `@Preview(widthDp = 720)` for the empty-query, typing, loading and first-time states, and one with a highlighted
item.

## New pure functions

```kotlin
// mapper/SearchDropdownItemMapper.kt
internal fun searchDropdownItems(
    query: String,
    browseGenres: List<BrowseGenreUiModel>,
    history: SearchHistoryUiModel,
    suggestions: SearchSuggestionsUiModel
): List<SearchDropdownItemUiModel>

// components/SearchDropdownNavigation.kt
internal fun highlightNext(items: List<SearchDropdownItemUiModel>, current: SearchDropdownItemUiModel?): SearchDropdownItemUiModel?
internal fun highlightPrevious(items: List<SearchDropdownItemUiModel>, current: SearchDropdownItemUiModel?): SearchDropdownItemUiModel?
internal fun reconcileHighlight(items: List<SearchDropdownItemUiModel>, current: SearchDropdownItemUiModel?): SearchDropdownItemUiModel?
```

## Keyboard contract (wide panel open; focus in the text field)

| Key | Highlight | Result | Consumed |
|---|---|---|---|
| Down | none → first item; *i* → *i+1*; last → last | highlight moves | yes |
| Up | *i* → *i−1*; first → none; none → none | highlight moves | yes |
| Enter | an item | the item's activation (data-model table) | yes |
| Enter | none | — | no: the field's search action commits the text |
| Escape | any | panel collapses, text kept | yes |
| Tab, Shift+Tab | any | panel collapses, text kept, focus moves on | no |
| Shift+Delete | `RecentQuery` / `RecentGame` | removal dialog / removed | yes |
| Shift+Delete | anything else | — | no |
| any other key | any | the text field handles it | no |

Panel closed (wide): Down on the focused bar opens it; typing into the focused bar opens it; a click or tap on the
bar opens it.

## Pointer contract

| Pointer | Result |
|---|---|
| enters or moves over an item | that item becomes highlighted |
| rests still while the keyboard moves the highlight | nothing (the keyboard's highlight stays) |
| click or tap on an item | the item's activation |
| long press on a recent search | `RemoveHistoryItemDialog`, as today |
| click on a recent game's remove button | removed, as today |
| click or tap outside the bar and the panel | panel collapses; the click does nothing else |
