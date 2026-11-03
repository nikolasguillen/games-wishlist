# Data Model: Discover Wide Layout

**Feature**: `021-discover-wide-layout` | **Date**: 2026-11-03

This feature changes layout only. **There is no change to UI state, domain models, persistence, the network layer
or the database schema.** The wide layout renders the existing `DiscoverContentState` and reads only fields the
compact layout already reads. This note records what it reads, the new layout constants and rules, the shared
lazy-list keys, and the new strings.

## UI state it renders (unchanged)

`DiscoverContentState` (`feature/search/model/DiscoverContentState.kt`): `Loading`, `Error(message)` and
`Content(popular, upcoming, hero, recommended, isStale, isRefreshing)`, exactly as today.

What the wide layout reads from `GameItemUiModel`, the same fields as today's Discover:

| Where | Fields | Shown as |
|---|---|---|
| Hero banner | `coverImage`, `name`, `developer`, `releaseDateText`, `isSaved`, `id` | Cover, title, meta line `developer · releaseDateText` (developer and separator left out when `null`, as on the compact hero), save action state |
| Shelf card | `coverImage`, `name`, `releaseYear`, `releaseDateText`, `isSaved`, `id` | Cover, title, `releaseYear ?: releaseDateText` (a year, or "TBA"), the heart |
| Shelf | `DiscoverShelfUiModel.title`, `games`, `hasMore`, `key` | Title, the row, "See all" |

The wide card and hero do not add the mockup's genre, developer-on-card, score badge or estimate mark (clarify,
2026-11-03).

## Layout constants (new, private or `internal` to `feature/search/components/`)

They are layout dimensions, not spacing tokens. Like `MIN_CARD_WIDTH` in `SearchResultGridCells.kt` and
`HERO_MAX_HEIGHT` in `DiscoverHero.kt`, they live next to the code that uses them. Gaps and paddings use
`MaterialTheme.spacing`.

| Name | Value | Used by | Source |
|---|---|---|---|
| `WIDE_SHELF_CARD_MIN_WIDTH` | `160.dp` | `wideShelfColumnCount` | Mockup: ~167dp cards, six to ~1080dp |
| `WIDE_SHELF_CARD_ASPECT_RATIO` | `3f / 4f` | `WideDiscoverShelfCard` | Portrait cover, as `ExpandedSearchHome`'s grid |
| `WIDE_HERO_HEIGHT_FRACTION` | `0.45f` | `wideHeroHeight` | research R3 |
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

Reused unchanged: `discover_most_anticipated` (the kicker, uppercased as today), `discover_release_date_disclaimer`
(the footnote), `discover_see_all_shelf`, `CoreUiRes.string.choose_list_content_description`.
