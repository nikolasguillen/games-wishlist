# Data Model: Search Landscape Layout

**Feature**: [spec.md](spec.md) | **Plan**: [plan.md](plan.md)

This feature has **no persisted or domain data**. Nothing reaches `:core:model`, Room, DataStore or the
cloud backup. There is no new `UiState` field or `UiEvent`, and no ViewModel changes. Everything below is
derived during composition from the window and the back stack, and recomputed on every size change.

## Window shape (`:core:ui`)

| Value | Source | Rule |
|---|---|---|
| `isLandscapeWindow()` | `LocalWindowInfo.current.containerDpSize` | `width > height`. A square window is portrait (research R1). |

## Navigation placement (`:shared`)

`TopLevelDestination`, a new enum with one entry per top-level destination. It replaces the three
hand-written items in `QuestLogBottomBar`.

| Entry | Route opened | Icon | Label resource | Selected when the top of the stack is |
|---|---|---|---|---|
| `SEARCH` | `SearchRoute` | `Icons.Default.Search` | `search_nav_bar_item` | `SearchRoute` |
| `RADAR` | `RadarRoute` | `Icons.Default.Radar` | `radar_nav_bar_item` | `RadarRoute` |
| `LISTS` | `ListsRoute` | `Icons.AutoMirrored.Filled.List` | `lists_nav_bar_item` | `ListsRoute` or `WishlistRoute` |

The order of the entries is the order on screen, in both the bar and the rail (FR-003).

Which navigation shows:

| Top of the back stack | Portrait | Landscape |
|---|---|---|
| `SearchRoute`, `RadarRoute`, `ListsRoute` | bottom bar | side rail (start edge) |
| anything else (game detail, wishlist, settings, onboarding…) | none | none |

The transitions are the same as today, by axis: the bar slides vertically and the rail slides
horizontally. A rotation switches between the two instantly, because the activity is recreated on Android
and the layout simply recomposes on iOS.

## Results grid geometry (`:feature:search`)

The inputs are the grid's own width `W` (from `BoxWithConstraints`), `isLandscapeWindow()`, and the
spacing tokens.

| Value | Rule |
|---|---|
| `edge`, `spacing` | `MaterialTheme.spacing.large` (16dp), unchanged |
| `MIN_CARD_WIDTH` | `150.dp`, feature-local |
| column count `n` | portrait: `2`. Landscape: `max(3, ⌊(W − 2·edge + spacing) / (MIN_CARD_WIDTH + spacing)⌋)` |
| card width `c` | `(W − 2·edge − (n − 1)·spacing) / n` |
| cell sizes | `[c + edge, c, …, c, c + edge]`, plus leftover pixels one per cell from the start. With `n = 2`: `[c + edge, c + edge]`, today's geometry |
| card padding | the column-0 card pads `start = edge`, the column-`n−1` card pads `end = edge`, and other cards pad nothing. Column = `index % n` |

Invariants (tested in `SearchResultGridCellsTest`):
- the cell sizes add up to `W − (n − 1)·spacing`;
- every card is the same visible width;
- portrait always yields `n = 2`, and landscape never yields fewer than 3.

## Discover hero (`:feature:search`)

| Value | Rule |
|---|---|
| hero height | `min(240.dp, windowHeight × HERO_MAX_HEIGHT_FRACTION)`, with `HERO_MAX_HEIGHT_FRACTION = 0.5f`. This is 240dp on every portrait phone. |
