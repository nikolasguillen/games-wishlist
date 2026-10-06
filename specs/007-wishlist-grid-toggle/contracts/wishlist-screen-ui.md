# Contract: Wishlist detail screen UI

The stateless `WishlistContent(state, onEvent, …)` renders from `WishlistUiState` and emits
`WishlistUiEvent`s. It makes no decisions (constitution Principle III). Types are defined in
[../data-model.md](../data-model.md).

## Layout (one `LazyVerticalGrid`, `GridCells.Fixed(2)`, one hoisted `LazyGridState`)

| # | Item | Span | Present when |
|---|------|------|--------------|
| 0 | `WishlistDetailHeader` (unchanged) | full | always in `Success` |
| 1 | `WishlistViewOptionsRow`: chip strip (`weight(1f)`, `LazyRow`) + view toggle (end) | full | always in `Success` |
| … | per visible section: `StatusSectionHeader` | full | each section in `Success.sections` |
| … | per game, **LIST**: `SwipeToRevealRow { WishlistGameRow }` | full | `viewMode == LIST` |
| … | per game, **GRID**: `VerticalGameCard` | 1 cell | `viewMode == GRID` |
| … | per section: trailing spacer | full | each section |

- In `Loading` and `Empty`, the screen renders exactly what it renders today. There's no row, toggle or
  chips (FR-003, FR-012).
- In `FilteredEmpty`, the screen renders a non-lazy `Column`, mirroring today's `Empty` branch:
  `WishlistDetailHeader`, then `WishlistViewOptionsRow` (selected chip and toggle visible), then
  `EmptyPage(message = filtered_empty_message, icon = Icons.Outlined.FilterAltOff, Modifier.weight(1f))`
  (FR-016, SC-009).
- Item keys stay unique across modes: `"list_header"`, `"view_options"`, `"header_<status>"`,
  `"game_<status>_<id>"`, `"spacer_<status>"`.
- Grid card padding follows `SearchResultGrid`. The left cell gets `start = spacing.large` and the right
  cell gets `end = spacing.large`, with parity taken from the card's index *within its section*, because
  each section starts a new line.

## Interactions → events

| User action | Emitted | Notes |
|-------------|---------|-------|
| Tap the view toggle | `OnViewModeToggled` | Also resets the UI-local `revealedGameId` (keyed on `viewMode`). |
| Tap a chip | `OnStatusFilterSelected(chip.filter)` | |
| Tap a card or row | `onGameClick(id)` lambda (unchanged) | Navigation stays in `:app`. |
| Long-press a card (GRID) | none. Sets the UI-local `gamePendingRemoval`, which shows the existing `RemoveGameDialog` | Confirm emits the existing `OnGameRemoved(id)`. |
| Swipe a row → Remove (LIST) | unchanged | |

## Shared-component changes (`:core:ui`)

`VerticalGameCard` changes two parameters:

| Parameter | Before | After | Search | Wishlist |
|-----------|--------|-------|--------|----------|
| `onSaveClick` | `() -> Unit = {}` | `(() -> Unit)? = null`, where `null` hides the heart | passes a callback (unchanged look) | `null` |
| `onLongClickLabel` | none (hardcoded "choose list" inside the card) | **required** `String`, no default | `stringResource(CoreUiR.string.choose_list_content_description)` (same announcement as today) | `stringResource(R.string.remove_game_action)` |

The label goes to both the card's long-press and, when drawn, the heart's `longClickLabel`.
`SearchResultGrid` is the only existing caller. It must be edited to pass the label, which is a
source-breaking change caught at compile time.

## Accessibility and strings

| Element | Text | Resource (new unless noted) |
|---------|------|-----------------------------|
| Toggle in LIST | contentDescription "Show as grid", stateDescription "List view" | `show_as_grid_action`, `list_view_state` |
| Toggle in GRID | contentDescription "Show as list", stateDescription "Grid view" | `show_as_list_action`, `grid_view_state` |
| "All" chip | "All" | `filter_all` |
| Filtered-empty message | "No games match this filter" | `filtered_empty_message` |
| Status chips | the section label | existing (`GameStatus.toLabelUiText()`, `no_status_section_label`) |
| Card long-press label | "Remove game" | existing `remove_game_action` |

All new strings go in `feature/wishlist/src/main/res/values/strings.xml`, imported as the module's own
bare `R`.
