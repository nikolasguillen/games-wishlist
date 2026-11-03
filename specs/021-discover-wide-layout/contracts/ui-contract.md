# UI Contract: Discover Wide Layout

**Feature**: `021-discover-wide-layout` | **Date**: 2026-11-03

The app exposes no API. Its contract is the boundary between `:shared` and `:feature:search`, plus the internal
composables that make up the two layouts. Signatures are given as the shape they must have; parameter order follows
the project rule (required first, `modifier: Modifier = Modifier` last among the defaulted ones, as the existing
code does).

## 1. `:shared` → `:feature:search` (the only cross-module change)

```kotlin
// feature/search/.../SearchScreen.kt — public entry point, one parameter added
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

- **Caller**: the `SearchRoute` branch of the `entryProvider` in `shared/.../QuestLogNavDisplay.kt` passes
  `isWideWindow = twoPaneEnabled`, the value `QuestLogRoot` computes once from the window size class (840dp included).
- **Meaning**: whether the *window* is at least 840dp wide. It is not "two panes are showing" (a game may or may not
  be open) and not the pane's width (clarify Q3).
- **Default `false`**: previews, and any caller that does not pass it, get today's compact layout.
- No new module edge: `:shared` already depends on `:feature:search`, and `:feature:search` gains no dependency.

## 2. Inside `:feature:search` — the single dispatch point

```kotlin
// components/SearchMainContent.kt — internal, one parameter added and threaded to DiscoverContent
internal fun SearchMainContent(
    /* existing parameters unchanged */,
    modifier: Modifier = Modifier,
    discoverListState: LazyListState = rememberLazyListState(),
    isWideWindow: Boolean = false
)
```

`DiscoverContent` (private, same file) is the one place the flag is read:

| `DiscoverContentState` | `isWideWindow = false` | `isWideWindow = true` |
|---|---|---|
| `Loading` | `LoadingPage()` | `LoadingPage()` (same) |
| `Error` | `ErrorPage(message, retry → OnRetryDiscover)` | same |
| `Content` | `DiscoverFeed(…)` (**unchanged**) | `WideDiscoverFeed(…)` |

Both feeds get the same `state = discoverListState`, the same `onEvent`-derived callbacks and the same
`onSeeAllClick`. `SearchScreenContent` passes `isWideWindow` through and does nothing else with it: the top bar, the
FAB, the sheets and the snackbar are shared by both layouts.

## 3. The wide feed

```kotlin
// components/WideDiscoverFeed.kt
@Composable
internal fun WideDiscoverFeed(
    hero: GameItemUiModel?,
    heroReleaseText: UiText?,
    popular: DiscoverShelfUiModel?,
    upcoming: DiscoverShelfUiModel?,
    onGameClick: (Int) -> Unit,
    onSaveClick: (Int) -> Unit,
    onLongClick: (Int) -> Unit,
    onSeeAllClick: (DiscoverShelfUiModel) -> Unit,
    modifier: Modifier = Modifier,
    recommended: List<DiscoverShelfUiModel> = emptyList(),
    isStale: Boolean = false,
    isRefreshing: Boolean = false,
    onRefreshClick: () -> Unit = {},
    onDismissRefreshClick: () -> Unit = {},
    state: LazyListState = rememberLazyListState()
)
```

**Behaviour it must have**:
- With nothing to show (`hero`, `popular` and `upcoming` all `null`, `recommended` empty), it renders
  `DiscoverPlaceholder`, exactly as `DiscoverFeed` does.
- It is a `LazyColumn` on `state`, with the items, order and keys of `data-model.md` ("Lazy-list item keys"),
  `contentPadding` top `spacing.large` and bottom `ScrollToTopFabDefaults.ContentBottomPadding`, and items spaced by
  `spacing.extraLarge`.
- The hero item is `WideDiscoverHero` followed by the `discover_release_date_disclaimer` footnote.
- Each shelf item is a `WideDiscoverShelf`.
- `isStale` shows `DiscoverRefreshPrompt` in the same top overlay as the compact feed.

```kotlin
// components/WideDiscoverHero.kt
@Composable
internal fun WideDiscoverHero(
    game: GameItemUiModel,
    releaseText: UiText?,
    onDetailsClick: () -> Unit,
    onSaveClick: () -> Unit,
    onSaveLongClick: () -> Unit,
    modifier: Modifier = Modifier
)
```

- Height: `wideHeroHeight(window height, own max width)`.
- The cover fills the banner and is not clickable. A missing or failed cover falls back to the compact hero's
  fallback artwork.
- The kicker is `discover_most_anticipated`, uppercased, in the compact hero's pill.
- The bottom block holds the title (≤ 2 lines) and the meta line: `developer · releaseText`, with the developer and
  its separator left out when `null`.
- The actions are "Add to wishlist" / "In wishlist" (tap → `onSaveClick`, long press → `onSaveLongClick`, long-click
  label `choose_list_content_description`) and "View details" (→ `onDetailsClick`). They sit at the end of the title
  row, or below it when the banner is narrower than `WIDE_HERO_STACKED_BELOW`.

```kotlin
// components/WideDiscoverFeed.kt (private) — one shelf
@Composable
private fun WideDiscoverShelf(
    shelf: DiscoverShelfUiModel,
    onGameClick: (Int) -> Unit,
    onSaveClick: (Int) -> Unit,
    onLongClick: (Int) -> Unit,
    onSeeAllClick: (DiscoverShelfUiModel) -> Unit,
    modifier: Modifier = Modifier
)
```

- The title row holds the shelf title (one line, ellipsised, a heading for accessibility). When
  `wideShelfShowsSeeAll(shelf.hasMore, shelf.games.size, columns)` is true, a `CustomTextButton` with the visible
  text `discover_see_all` sits at the row's end, announced as `discover_see_all_shelf(title)` and calling
  `onSeeAllClick(shelf)`.
- The card row is a `Row` of `columns = wideShelfColumnCount(maxWidth, spacing.large, spacing.large)` equal slots,
  the first `min(games.size, columns)` holding a `WideDiscoverShelfCard`. It never scrolls sideways.

```kotlin
// components/WideDiscoverShelfCard.kt
@Composable
internal fun WideDiscoverShelfCard(
    game: GameItemUiModel,
    onClick: () -> Unit,
    onSaveClick: () -> Unit,
    onSaveLongClick: () -> Unit,
    modifier: Modifier = Modifier
)
```

- It fills its slot's width, with a 3:4 height.
- A tap on the card → `onClick`. The heart (`SaveToWishlistButton`, top end): tap → `onSaveClick`, long press →
  `onSaveLongClick`. The card has no long press of its own, as on `CompactGameCard`.
- The scrim holds the title (one line), the developer (one line, omitted when `null`), and at the end of that line
  `RatingBadge(rating)` when `rating > 0`, otherwise the release badge with `releaseYear ?: releaseDateText`.

## 4. Moved, unchanged in behaviour

```kotlin
// components/DiscoverRefreshPrompt.kt — moved out of DiscoverFeed.kt, private → internal
@Composable
internal fun DiscoverRefreshPrompt(
    isRefreshing: Boolean,
    onClick: () -> Unit,
    onDismissClick: () -> Unit,
    modifier: Modifier = Modifier
)
```

`DiscoverFeed`'s own signature does not change. It only stops defining the prompt and the item-key literals, and
uses the shared ones instead.

## 5. Events

No new `SearchUiEvent`, no new `SearchUiEffect`. The wide layout emits only events the compact one already emits:

| Interaction | Event / callback |
|---|---|
| Hero "Add to wishlist", card heart | `SearchUiEvent.OnToggleSave(id)` |
| Long press on the hero's save action or a card's heart | `SearchUiEvent.OnOpenListSelector(id)` |
| Hero "View details", card tap | `onGameClick(id)` (the screen's collapse-then-open lambda) |
| "See all" | `onSeeAllClick(shelf)` → `SearchUiEvent.OnShelfSeeAllTriggered(shelf)` |
| Refresh prompt / its dismissal | `OnRefreshDiscover` / `OnDismissDiscoverRefresh` |
| Error retry | `OnRetryDiscover` |
