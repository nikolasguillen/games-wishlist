# Research: Discover Wide Layout

**Feature**: `021-discover-wide-layout` | **Date**: 2026-11-03 | **Spec**: [spec.md](./spec.md)

Every unknown from the plan's Technical Context is resolved here. Each entry gives the decision, why, and what
else was weighed. File references are to the code as it is on `develop` at `d19b40a6`.

## R1. Where the wide/compact decision is read, and how it reaches Discover

**Decision**: `:shared` passes the window's width class to the screen as a plain `Boolean`. `QuestLogNavDisplay`
already holds `twoPaneEnabled`, computed once in `QuestLogRoot` as
`currentWindowAdaptiveInfo().windowSizeClass.isWidthAtLeastBreakpoint(WIDTH_DP_EXPANDED_LOWER_BOUND)`, the 840dp
rule with 840 included. It is handed to `SearchScreen(isWideWindow = twoPaneEnabled)`. From there it flows to
`SearchScreenContent` and `SearchMainContent`. `DiscoverContent` reads it once and calls either the existing
`DiscoverFeed` or the new `WideDiscoverFeed`.

**Rationale**:
- It is the owner's constraint: the size class is read once and dispatches to one of two separate layouts.
- It is the same value that switches the two-pane scene, the rail and the back handling. So the feed changes layout
  at exactly the moment the rest of the app treats the window as wide, never one frame apart.
- `:feature:search` has no window-size-class dependency (`material3-adaptive` lives in `:shared`), and needs none.

**Alternatives considered**:
- *Add `jetbrains-compose-adaptive` to `:feature:search` and call `currentWindowAdaptiveInfo()` there.* Rejected.
  It adds a library to a feature for one boolean, and the rule would then be read in two places that could
  disagree.
- *Compare `LocalWindowInfo.current.containerDpSize.width` with a hand-written `840.dp`.* Rejected. It is a second
  definition of the threshold, and on Android the container size and the window metrics `WindowSizeClass` reads are
  not guaranteed to round the same way at the boundary.
- *Decide by the pane's own width (`BoxWithConstraints`).* This is what `feature/CLAUDE.md` currently prescribes for
  a component choosing a layout by shape, but the owner ruled it out in clarify (Q3: follow the window). The rule in
  `feature/CLAUDE.md` is amended in the same change so the instructions and the code agree; see R9.

## R2. Keeping the scroll position and the feed across a layout swap (FR-012, SC-005)

**Decision**:
- **One list state for both layouts.** Both feeds take the `LazyListState` that `SearchScreenContent` already
  hoists (`discoverListState`) and is passed down through `SearchMainContent`. The swap replaces the composable
  under the same state object, so the first visible item index and its key survive.
- **Same item keys in both feeds.** Both lazy columns emit the same items in the same order: the hero, the
  personalised shelves, then "Most anticipated", then "Popular this month". They also share the same keys. The
  literals now inline in `DiscoverFeed` (`"recommended_$index"`, `"upcoming"`, `"popular"`) move to a small shared
  helper, used by both feeds, and the hero gets a key too. Since `LazyListState` restores by the first visible
  item's key, the same shelf is at the top after the swap. Only the pixel offset inside that item can differ,
  because item heights differ between layouts.
- **No reload.** The feed lives in `SearchViewModel` (`DiscoverContentState`), which neither layout touches. A swap
  is recomposition only, with no event, so there is no fetch and no `Loading`.

**Rationale**: This needs no new state and no ViewModel change. It is also the reason for the owner's "both over the
same ViewModel and state" constraint. A two-pane change also moves Search's composition rather than recreating it
(`SearchDetailScene` keeps the same entry), so the hoisted state survives that switch too.

**Alternatives considered**: Saving and restoring the index manually across the swap was rejected. It is redundant
with what `LazyListState` already does when keys match.

## R3. Hero height (FR-003, SC-002)

**Decision**: The wide hero's height is the window height × 0.45, clamped between 280dp and 560dp, and never more
than 0.8 × the banner's own width. The rule is one pure function, `wideHeroHeight(windowHeight, availableWidth)`,
next to the other wide-layout helpers so it can be unit-tested. The window height comes from
`LocalWindowInfo.current.containerDpSize.height`, as the compact hero reads it today; the width comes from the
banner's own constraints.

| Window (w × h, dp) | Banner width (approx.) | Wide hero | Compact hero today |
|---|---|---|---|
| 840 × 600 (short) | ~730 | 280 (floor) | 240 |
| 1280 × 800 | ~1150 | 360 | 240 (≥1.3× met: 1.5×) |
| 1920 × 1080 | ~1800 | 486 | 240 |
| 2560 × 1440 | ~2440 | 560 (ceiling) | 240 |
| 1080 × 900, two panes | ~470 | 376 (width cap) | 240 |

On a window 800dp tall, the header (~72dp), top padding (16), hero (360), footnote (~28), spacing (24) and the first
shelf's title row (48) add up to ~548dp. That leaves the title and the top of the first row of cards on screen, as
SC-002 requires.

**Rationale**: Height drives the dominance on a large display. The ceiling stops a 4K window from showing only a
banner, and the width cap stops a half-width pane from turning the banner into a portrait poster. The mockup's
banner, at roughly 1:3 of its width on a ~1080dp content area, falls inside these bounds.

**Alternatives considered**:
- *Fixed aspect ratio from width (for example 3:1).* Rejected: on an ultra-wide, short window it would push the
  shelves off screen.
- *Keep the compact rule with a higher cap.* Rejected: 0.4 of the height capped at, say, 480 looks the same at
  800dp tall and fails the "grows with the window" intent no differently, but gives no protection in a pane.

## R4. How many cards a wide shelf row shows, and when "See all" appears (FR-008, FR-009, SC-003)

**Decision**: The row is a fixed-geometry `Row`, not a `LazyRow`, made of `columns` equal-width slots.

- `columns = max(1, floor((availableWidth − 2·edge + spacing) / (WIDE_SHELF_CARD_MIN_WIDTH + spacing)))`, with
  `WIDE_SHELF_CARD_MIN_WIDTH = 160.dp`, `spacing = MaterialTheme.spacing.large` (16) and
  `edge = MaterialTheme.spacing.large`, the feed's horizontal padding. This is the same shape as
  `resultGridColumnCount` in `SearchResultGridCells.kt`.
- The row fills the first `min(games.size, columns)` slots; the rest are empty spacers. So cards keep the slot's
  width and are never stretched to fill a row they do not have the games for (spec edge case).
- "See all" is shown when `shelf.hasMore || shelf.games.size > columns`. That is the compact arrow's condition, plus
  the case the clarify answer added: cards left out of the row.
- Card height comes from a 3:4 aspect ratio on the slot's width, the ratio `ExpandedSearchHome` already uses for
  portrait covers. The mockup's cards are about 0.71.

At ~1080dp of content: floor((1080 − 32 + 16) / 176) = 6 columns, so a full shelf shows six, as SC-003 requires. At
a 2560dp window the slots stay ~180dp wide and a six-game shelf leaves the rest of the row empty instead of
stretching to ~390dp cards.

Both rules are pure functions (`wideShelfColumnCount`, `wideShelfShowsSeeAll`) in
`components/WideDiscoverLayout.kt` with a JVM test, following `SearchResultGridCells.kt` and its test.

**Rationale**:
- Clarify Q4: rows never scroll sideways.
- A `Row` also sidesteps the open backlog bug where the mouse wheel does not scroll Discover on desktop: there is no
  horizontally scrolling child left to catch the wheel. Whether that alone fixes the bug is to be checked on
  desktop (see quickstart), not assumed.
- `DiscoverShelfUiModel.games` is already capped at `DISCOVER_SHELF_ROW_SIZE = 6`, so no row can hold more than six
  whatever the width.

**Alternatives considered**:
- *`LazyRow` with `fillParentMaxWidth(1f / columns)`.* Rejected: it keeps a sideways-scrollable child for no
  benefit when nothing scrolls.
- *A `LazyVerticalGrid` per shelf.* Rejected: nested lazy lists in the same direction need a fixed height, and a
  single row gains nothing from a grid.
- *Stretching cards to fill the row when the shelf has fewer games.* Rejected by the spec's edge case.

**Verified**: "See all" for a shelf without `hasMore` already works. `SearchViewModel.openShelf` resolves the
shelf's games with `DiscoverFeed.shelfGames(key)` and never checks `hasMore`
(`SearchViewModel.kt`, `openShelf`), so the grid opens with the shelf's full list.

## R5. The wide shelf card (FR-010)

**Decision**: A new Discover-only card, `WideDiscoverShelfCard`, in `feature/search/components/`. It shows exactly
what today's shelf card (`CompactGameCard`) shows, re-proportioned for a wide row:
- the cover, filling a 3:4 card;
- the heart at the top end;
- over the bottom scrim, the title (one line) and `releaseYear ?: releaseDateText` (a year, or "TBA") on one line,
  the same expression `CompactGameCard` renders.

It is built from public `:core:ui` pieces:
- `GenericGameCardLayout`: the shell and the tap, with no long press on the card itself, as on `CompactGameCard`.
- `GameCoverImage`: the cover, cropped to fill.
- `SaveToWishlistButton`: tap toggles the save, long press opens the list chooser.
- The `fadingEdge` dark scrim, with text in fixed white like every cover card.

**Rationale**:
- `CompactGameCard` cannot be reused for a 3:4 slot. It forces `height(150.dp)` and `aspectRatio(1f)`, and its cover
  header is drawn at a fixed 150dp. Giving it size parameters would change a shared component that the compact feed
  depends on, which the owner's constraint (separate wide composables) and SC-006 argue against.
- `VerticalGameCard` shows rating · developer · year and long-presses the whole card. The owner chose today's
  Discover information (title and year), not the results grid's.
- A card used only by Discover stays in the feature, like `DiscoverHero`.

**Alternatives considered**:
- *The mockup's card (developer line plus a score-or-release badge).* Rejected by the owner (clarify 2026-11-03,
  "keep the same rows that we already use").
- *A new `:core:ui` card.* Rejected until a second screen needs it.

## R6. The release date in the hero's meta line (FR-004, FR-005)

**Decision**: The wide hero's meta line is the compact hero's, unchanged: `developer · releaseDateText`, with the
developer and its separator left out when `developer` is `null`. No estimate mark ("(est.)") is added: the owner
asked not to follow the mockup that strictly (clarify 2026-11-03). So there is no new state, no mapper change and
no new string for it.

**Finding kept for later**: The Discover lanes request only `first_release_date`, not `release_dates.date_format`,
and the lane cache stores the scalar date only. If an estimate mark is ever wanted, a year-only date can be told
from IGDB's 31 December placeholder (`DateUtils.isYearOnlyPlaceholder`), but a quarter needs the lane query and
cache to carry the precision. That would be a data change.

## R7. Genre

**Finding**: The domain `Game` the feed holds carries genres (the lane queries request `genres.name` and the cache
keeps a genre cross-reference). `GameItemUiModel` has no genre field.

**Decision**: No genre is shown, per the owner. The hero and the cards keep the information rows today's Discover
uses. The spec's assumption is worded accordingly.

## R8. The hero's two actions, the refresh prompt, and keyboard reach (FR-006, FR-013, FR-014, SC-008)

**Decision**:
- **"Add to wishlist"** is a pill-shaped, primary-coloured control built on `Surface` + `combinedClickable`. A tap
  sends `OnToggleSave(id)`, a long press sends `OnOpenListSelector(id)` with the existing
  `choose_list_content_description` as its long-click label, and `Role.Button` is set. Material's `Button` has no
  long press, which is why it is not used. The label and icon follow `isSaved`: "Add to wishlist" with an outlined
  heart, or "In wishlist" with a filled heart.
- **"View details"** is Material's `OutlinedButton` in fixed white, so it stays legible on the permanently dark
  scrim, as every cover overlay does. It calls `onGameClick(id)`.
- **The banner itself is not clickable** (spec assumption): the cover is a plain image, with no
  `GenericGameCardLayout` `onClick`.
- **Layout inside the banner**: the kicker pill at top start, as on the compact hero. At the bottom, the title (two
  lines max) and the meta line sit at the start, with the two actions at the end of the same row. When the banner
  is narrower than 600dp (a half-width pane), the actions move below the title. That is a width check inside the
  wide hero only, which the owner's constraint allows: it forbids size conditionals inside the compact composables.
- **Refresh prompt**: `DiscoverRefreshPrompt` is today a `private` composable in `DiscoverFeed.kt`. It moves
  unchanged to its own `internal` file, `components/DiscoverRefreshPrompt.kt`, and both feeds draw it in the same
  `AnimatedVisibility` overlay. Its visible/refreshing state comes from `DiscoverContentState.Content`, so it
  survives a swap untouched. The move also lets the `DiscoverFeed` KDoc paragraph that still calls the prompt a
  `stickyHeader` be corrected: the code draws an overlay.
- **Keyboard**: every interactive element is a clickable or a button, so it is focusable and activated with Enter
  or Space on desktop, in composition order: search bar, the banner's two actions, then each shelf's "See all"
  and its cards left to right. Nothing needs a custom focus order. The list chooser stays a long press, as on every
  other card; no keyboard shortcut is added for it.

**Alternatives considered**: Reusing `SaveToWishlistButton` as the hero's save action was rejected. It is a 48dp
icon target, and the mockup asks for a labelled button.

## R9. Documentation that has to move with the code

**Decision**:
- `feature/CLAUDE.md` currently says "A component that has to choose a layout by shape reads the space it was
  given, not the window." It gets one sentence for the case this feature introduces: a screen with a separate
  wide layout receives the window's width class from `:shared` as a `Boolean` (`isWideWindow`) and dispatches on
  it once. Components inside either layout still size by the space they are given.
- No tech-debt entry is resolved or added. The backlog's mouse-wheel item stays until it is verified on desktop;
  it may then be deleted.

## R10. Verification on this machine

**Decision**: Windows, so the batch wrapper: `.\gradlew.bat :feature:search:testAndroidHostTest`,
`.\gradlew.bat :feature:search:compileAndroidMain`, `.\gradlew.bat test`, `.\gradlew.bat :androidApp:assembleDebug`.
The layout is checked by running `:desktopApp:run` (a resizable window, the main wide target) and on a resizable
Android tablet emulator. iOS gets the same `commonMain` code; its visual check needs a Mac and is stated as not
done rather than assumed (see quickstart).
