# Data Model: Discover Wide Layout

**Feature**: `021-discover-wide-layout` | **Date**: 2026-11-03

This feature changes layout only. There are no changes to persistence, the network layer, the domain models or the
database schema. It makes one additive change to UI state and adds a few layout constants. Everything else the wide
layout renders already exists.

## UI state

### `DiscoverContentState.Content` (`feature/search/model/DiscoverContentState.kt`), one field added

| Field | Type | Status | Meaning |
|---|---|---|---|
| `popular` | `DiscoverShelfUiModel?` | unchanged | "Popular this month", or `null` when the lane is empty |
| `upcoming` | `DiscoverShelfUiModel?` | unchanged | "Most anticipated" without the hero |
| `hero` | `GameItemUiModel?` | unchanged | The featured game |
| **`heroReleaseText`** | **`UiText?`** | **new**, default `null` | The hero's release date as the wide meta line shows it, with the estimate mark applied (research R6). `null` exactly when `hero` is `null`. |
| `recommended` | `List<DiscoverShelfUiModel>` | unchanged | Personalised shelves |
| `isStale` | `Boolean` | unchanged | Drives the refresh prompt |
| `isRefreshing` | `Boolean` | unchanged | The prompt's spinner |

**Rules for `heroReleaseText`**, set in `DiscoverFeed.toDiscoverContentState` from the domain hero's `releaseDate`:

| Hero `releaseDate` | `heroReleaseText` |
|---|---|
| `null` | `UiText.StringResource(CoreUiRes.string.release_date_tba)` ("TBA") |
| a year-only placeholder (`DateUtils.isYearOnlyPlaceholder`, 31 December) | `UiText.StringResource(Res.string.discover_release_estimate, "<year>")` ("2026 (est.)") |
| any other ISO date | `UiText.DynamicString(DateUtils.formatIsoDate(date))` (a localised medium date) |
| no hero at all | `null` |

**Invariants**:
- `heroReleaseText == null` ⇔ `hero == null`.
- The field does not depend on saved state, so `SearchViewModel`'s update of `hero.isSaved` (`hero.copy(isSaved = …)`)
  leaves it untouched and needs no change.
- The compact layout never reads it.

`DiscoverShelfUiModel`, `DiscoverShelfKey`, `GameItemUiModel`, `SearchUiState`, `SearchUiEvent` and `SearchUiEffect`
are unchanged.

## Layout constants (new, private or `internal` to `feature/search/components/`)

They are layout dimensions, not spacing tokens. Like `MIN_CARD_WIDTH` in `SearchResultGridCells.kt` and
`HERO_MAX_HEIGHT` in `DiscoverHero.kt`, they live next to the code that uses them. Gaps and paddings use
`MaterialTheme.spacing`.

| Name | Value | Used by | Source |
|---|---|---|---|
| `WIDE_SHELF_CARD_MIN_WIDTH` | `160.dp` | `wideShelfColumnCount` | Mockup: ~167dp cards, six to ~1080dp |
| `WIDE_SHELF_CARD_ASPECT_RATIO` | `3f / 4f` | `WideDiscoverShelfCard` | Portrait cover, as `ExpandedSearchHome`'s grid |
| `WIDE_HERO_HEIGHT_FRACTION` | `0.45f` | `wideHeroHeight` | R3 |
| `WIDE_HERO_MIN_HEIGHT` | `280.dp` | `wideHeroHeight` | R3, keeps ≥ compact at any height |
| `WIDE_HERO_MAX_HEIGHT` | `560.dp` | `wideHeroHeight` | R3 |
| `WIDE_HERO_MAX_ASPECT` | `0.8f` (height ÷ width) | `wideHeroHeight` | R3, a half-width pane |
| `WIDE_HERO_STACKED_BELOW` | `600.dp` | `WideDiscoverHero` | R8, actions move below the title |

## Pure layout rules (new, `components/WideDiscoverLayout.kt`, unit-tested)

| Function | Input | Output | Rule |
|---|---|---|---|
| `wideShelfColumnCount(availableWidth, spacing, edge)` | `Dp`, `Dp`, `Dp` | `Int` ≥ 1 | `floor((availableWidth − 2·edge + spacing) / (WIDE_SHELF_CARD_MIN_WIDTH + spacing))`, at least 1 |
| `wideShelfShowsSeeAll(hasMore, gameCount, columns)` | `Boolean`, `Int`, `Int` | `Boolean` | `hasMore \|\| gameCount > columns` |
| `wideHeroHeight(windowHeight, availableWidth)` | `Dp`, `Dp` | `Dp` | `min((windowHeight × 0.45).coerceIn(280, 560), availableWidth × 0.8)` |

## Lazy-list item keys (shared by both feeds)

Both feeds emit the same items in the same order with the same keys, which is what keeps the first visible shelf
across a swap (research R2):

| Item | Key | Order |
|---|---|---|
| Hero (with the estimate footnote) | `"hero"` | 1st, when there is a hero |
| Personalised shelf *i* | `"recommended_$i"` | then, strongest first |
| Most anticipated | `"upcoming"` | then |
| Popular this month | `"popular"` | last |

The keys move from literals inside `DiscoverFeed` into one `internal` helper both feeds call. That replaces the
compact feed's literals with the same values, so its behaviour does not change; the hero, unkeyed today, gains its
key in both.

## Strings (new, `feature/search/src/commonMain/composeResources/values/strings.xml`)

| Name | English | Where |
|---|---|---|
| `discover_add_to_wishlist` | Add to wishlist | Hero save action, not saved |
| `discover_in_wishlist` | In wishlist | Hero save action, saved |
| `discover_view_details` | View details | Hero details action |
| `discover_see_all` | See all | Shelf link, visible text (announced with the existing `discover_see_all_shelf`, "See all: %1$s") |
| `discover_release_estimate` | %1$s (est.) | Hero meta line, year-only date |

Reused unchanged: `discover_most_anticipated` (the kicker, uppercased as today), `discover_release_date_disclaimer`
(the footnote), `discover_see_all_shelf`, `CoreUiRes.string.release_date_tba`,
`CoreUiRes.string.choose_list_content_description`.
