---

description: "Task list for Discover Wide Layout"
---

# Tasks: Discover Wide Layout

**Input**: Design documents from `specs/021-discover-wide-layout/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/ui-contract.md](./contracts/ui-contract.md), [quickstart.md](./quickstart.md)

**Tests**: One new JVM test file, for the pure layout rules (the constitution asks for a test on new logic). Nothing else is tested automatically: there is no state, mapper or ViewModel change, and the existing suites must stay green untouched. Visual behaviour is verified with the quickstart.

**Organization**: Tasks are grouped by user story. US1 and US2 are both P1 and together are the MVP: the wide layout is switched on for real users only at the end of US2 (T016), because switching it on earlier would show a wide window a hero with no shelves.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: can run in parallel (different files, no dependency on an unfinished task)
- **[Story]**: US1 (tall hero with its own actions), US2 (full-width shelves), US3 (switching layouts never disturbs the feed)

## Path conventions

All paths are relative to the repository root. `FS` below stands for
`feature/search/src/commonMain/kotlin/com/nikolasguillen/questlog/feature/search`, so `FS/components/DiscoverFeed.kt`
is `feature/search/src/commonMain/kotlin/com/nikolasguillen/questlog/feature/search/components/DiscoverFeed.kt`. The
commands use the Windows batch wrapper (`.\gradlew.bat`); on macOS use `./gradlew`.

Project rules that apply to every task: one composable unit per file in `components/`; `internal` by default; spacing
from `MaterialTheme.spacing`, never a `dp` literal for a gap (layout dimensions are the named constants in
`WideDiscoverLayout.kt`); a cross-module `Res` is imported aliased (`import ...core.ui.resources.Res as CoreUiRes`), the
module's own `Res` bare, and each resource needs its own import; every new composable gets a `private` `@QuestLogPreviews`
preview wrapped in `QuestLogTheme { }`; comments and KDoc in English; never edit anything under `build/generated/`.

---

## Phase 1: Setup

**Purpose**: Know the starting point before changing anything.

- [X] T001 Run `.\gradlew.bat :feature:search:compileAndroidMain --console=plain -q` and `.\gradlew.bat :feature:search:testAndroidHostTest --console=plain -q` on the branch as it is, and note any failure that exists before this work, so it is not mistaken for a regression later.

---

## Phase 2: Foundational (blocking prerequisites)

**Purpose**: Behaviour-neutral groundwork that the wide layout and the compact one both stand on: a shared refresh prompt, shared lazy-list item keys, the new strings, and the tested layout rules.

**⚠️ CRITICAL**: no user story work can begin until this phase is complete.

- [X] T002 Move `DiscoverRefreshPrompt` out of `FS/components/DiscoverFeed.kt` into a new `FS/components/DiscoverRefreshPrompt.kt`, changing it from `private` to `internal` and keeping its body, signature (`isRefreshing: Boolean, onClick: () -> Unit, onDismissClick: () -> Unit, modifier: Modifier = Modifier`) and KDoc otherwise identical; give it a `@QuestLogPreviews` preview (idle and refreshing); remove the function and its now-unused imports from `DiscoverFeed.kt`. In the `DiscoverFeed` KDoc, replace the paragraph that says the prompt is a `stickyHeader` with what the code does (it is drawn in an `AnimatedVisibility` overlay at the top of the feed). The compact feed must look and behave exactly as before.
- [X] T003 Create `FS/components/DiscoverFeedItemKeys.kt` holding the lazy-list item keys both feeds use, as `internal` top-level declarations with the values from data-model.md: `DISCOVER_HERO_KEY = "hero"`, `DISCOVER_UPCOMING_KEY = "upcoming"`, `DISCOVER_POPULAR_KEY = "popular"` and `fun discoverRecommendedKey(index: Int): String = "recommended_$index"`. Keep the existing comment's reasoning (keys stay strings: on Android a lazy item key has to be storable in a Bundle). Then update `DiscoverFeed.kt` to use them in place of its literals, and give the hero `item` the key `DISCOVER_HERO_KEY` (it has none today). The order of the items in `DiscoverFeed` must not change. Depends on T002 (same file).
- [X] T004 [P] Add the four new strings to `feature/search/src/commonMain/composeResources/values/strings.xml`, next to the other `discover_*` entries, with these names and English values: `discover_add_to_wishlist` = `Add to wishlist`, `discover_in_wishlist` = `In wishlist`, `discover_view_details` = `View details`, `discover_see_all` = `See all`. Write quotes plainly; add no other string. Reused unchanged and not to be duplicated: `discover_most_anticipated`, `discover_release_date_disclaimer`, `discover_see_all_shelf`, and `CoreUiRes.string.choose_list_content_description`.
- [X] T005 [P] Create `FS/components/WideDiscoverLayout.kt` with the layout constants and the three pure functions from data-model.md, all `internal`, with KDoc saying where each value comes from. Constants: `WIDE_SHELF_CARD_MIN_WIDTH = 160.dp`, `WIDE_SHELF_CARD_ASPECT_RATIO = 3f / 4f`, `WIDE_HERO_HEIGHT_FRACTION = 0.45f`, `WIDE_HERO_MIN_HEIGHT = 280.dp`, `WIDE_HERO_MAX_HEIGHT = 560.dp`, `WIDE_HERO_MAX_ASPECT = 0.8f` (height ÷ width), `WIDE_HERO_STACKED_BELOW = 600.dp`. Functions: `wideShelfColumnCount(availableWidth: Dp, spacing: Dp, edge: Dp): Int` = `floor((availableWidth − 2·edge + spacing) / (WIDE_SHELF_CARD_MIN_WIDTH + spacing))`, at least 1; `wideShelfShowsSeeAll(hasMore: Boolean, gameCount: Int, columns: Int): Boolean` = `hasMore || gameCount > columns`; `wideHeroHeight(windowHeight: Dp, availableWidth: Dp): Dp` = `min((windowHeight × 0.45).coerceIn(280.dp, 560.dp), availableWidth × 0.8)`. Model the file on `FS/components/SearchResultGridCells.kt` (`resultGridColumnCount`): no Compose runtime, no `@Composable`.
- [X] T006 Create `feature/search/src/androidHostTest/kotlin/com/nikolasguillen/questlog/feature/search/components/WideDiscoverLayoutTest.kt` (JUnit4, a KDoc header saying what it covers, modelled on `SearchResultGridCellsTest.kt`) covering: `wideShelfColumnCount` with `spacing = 16.dp` and `edge = 16.dp` gives 6 at 1072dp and 1080dp, 5 at 1071dp, 4 at 760dp, and never less than 1 (for example at 100dp); `wideShelfShowsSeeAll` is true when `hasMore`, true when `gameCount > columns` (for example 6 games, 4 columns), and false when `hasMore` is false and `gameCount <= columns`; `wideHeroHeight` is 360dp at (800dp tall, 1150dp wide), is at least 1.3 × 240dp at any height from 800dp up, stays within [280dp, 560dp] when the width cap does not bite (for example 600dp and 1440dp tall at 2440dp wide), is capped by 0.8 × the width at (900dp tall, 470dp wide) giving 376dp, and lets the width cap win below the floor (300dp wide gives 240dp). Depends on T005.
- [X] T007 Checkpoint: run `.\gradlew.bat :feature:search:compileAndroidMain --console=plain -q` and `.\gradlew.bat :feature:search:testAndroidHostTest --console=plain -q`; both must pass, and `WideDiscoverLayoutTest` must be among the tests that ran. Open the compact Discover previews in `DiscoverFeed.kt` and confirm they render as before.

**Checkpoint**: foundation ready; the compact feed is unchanged and the wide layout's building blocks exist.

---

## Phase 3: User Story 1 - A tall hero banner with its own actions (Priority: P1) 🎯 MVP

**Goal**: On a wide window the top of Discover is a full-width banner, clearly taller than the phone hero, with the kicker, the title, the `developer · release date` meta line and two in-banner buttons, followed by the estimate footnote. The compact hero is not touched.

**Independent Test**: The wide feed is not switched on yet (that is T016), so check it in the previews at `@Preview(widthDp = 1280, heightDp = 800)` and `widthDp = 560`: the banner is taller than the compact hero would be, both buttons are inside it, and the footnote sits under it. The running-app check is quickstart §2's first rows, done after T016.

### Implementation for User Story 1

- [X] T008 [US1] In `FS/components/DiscoverHero.kt`, change `HeroImageFallback` from `private` to `internal` (visibility only: no change to its body or to anything the compact hero does), so the wide hero shows the same fallback artwork for a missing or failed cover (spec edge case on a missing cover).
- [X] T009 [US1] Create `FS/components/WideDiscoverHero.kt` with `internal fun WideDiscoverHero(game: GameItemUiModel, onDetailsClick: () -> Unit, onSaveClick: () -> Unit, onSaveLongClick: () -> Unit, modifier: Modifier = Modifier)` per contracts/ui-contract.md §3. Height is `wideHeroHeight(LocalWindowInfo.current.containerDpSize.height, <the banner's own max width, from BoxWithConstraints>)`. The cover fills the banner and is **not clickable**: use `SubcomposeAsyncImage` with the same loading box as `DiscoverHero` and `HeroImageFallback()` for a null cover and on error. The kicker is the compact hero's pill (`stringResource(Res.string.discover_most_anticipated).uppercase()`, same colours and shape) at the top start. At the bottom, over a dark `fadingEdge` scrim with fixed white text: the title (`maxLines = 2`, ellipsis) and the meta line `developer · game.releaseDateText.asString()`, built exactly as `DiscoverHero` builds it (developer and its `UiConstants.METADATA_SEPARATOR` omitted when `game.developer` is `null`); no genre and no estimate mark. The two actions sit at the end of the title row, or below the title when the banner is narrower than `WIDE_HERO_STACKED_BELOW`: (1) "add to wishlist", a pill-shaped primary-coloured `Surface` with `combinedClickable(role = Role.Button, onClick = onSaveClick, onLongClick = onSaveLongClick, onLongClickLabel = stringResource(CoreUiRes.string.choose_list_content_description))`, showing `discover_add_to_wishlist` with an outlined heart when `!game.isSaved` and `discover_in_wishlist` with a filled heart when `game.isSaved`; (2) "view details", a Material `OutlinedButton` in fixed white (legible on the dark scrim in both themes) with `discover_view_details`, calling `onDetailsClick`. Both must be focusable and activatable by keyboard. Add `@QuestLogPreviews` previews: not saved, saved, no cover, no developer, a very long title; plus a `@Preview(widthDp = 1280, heightDp = 800)` and a narrow (`widthDp = 560`) preview, the precedent of `ExpandedSearchHome.kt`. Depends on T005, T008.
- [X] T010 [US1] Create `FS/components/WideDiscoverFeed.kt` with `internal fun WideDiscoverFeed(...)` using exactly the signature in contracts/ui-contract.md §3 (`hero, popular, upcoming, onGameClick, onSaveClick, onLongClick, onSeeAllClick, modifier, recommended = emptyList(), isStale = false, isRefreshing = false, onRefreshClick = {}, onDismissRefreshClick = {}, state = rememberLazyListState()`). When `hero`, `popular` and `upcoming` are all `null` and `recommended` is empty, render `DiscoverPlaceholder(modifier)` and return, as `DiscoverFeed` does. Otherwise a `Box` holding a `LazyColumn(state = state, contentPadding = PaddingValues(top = spacing.large, bottom = ScrollToTopFabDefaults.ContentBottomPadding), verticalArrangement = Arrangement.spacedBy(spacing.extraLarge), modifier = modifier)` and, overlaid, the refresh prompt in the same `AnimatedVisibility` (`fadeIn() + slideInVertically { -it }` / `slideOutVertically { -it } + fadeOut()`, aligned top centre, `fillMaxWidth()`, the same paddings) as the compact feed, drawing `DiscoverRefreshPrompt(isRefreshing, onRefreshClick, onDismissRefreshClick)`. The first lazy item, only when `hero != null`, has `key = DISCOVER_HERO_KEY` and holds `WideDiscoverHero` (`onDetailsClick = { onGameClick(hero.id) }`, `onSaveClick = { onSaveClick(hero.id) }`, `onSaveLongClick = { onLongClick(hero.id) }`, horizontal padding `spacing.large`) followed by the footnote `stringResource(Res.string.discover_release_date_disclaimer)` styled as in `DiscoverHero` (`labelSmall`, italic, `onSurfaceVariant` at 0.7 alpha, top and start padding `spacing.small`). The shelf items are added in T013. Add a `@QuestLogPreviews` preview and a `@Preview(widthDp = 1280, heightDp = 800)` preview with a hero. Depends on T003, T009.
- [X] T011 [US1] Checkpoint: run `.\gradlew.bat :feature:search:compileAndroidMain --console=plain -q`, and check the T009 and T010 previews: banner taller than the compact hero at 800dp tall (about 360dp), both buttons inside it (stacked under the title in the 560dp preview), footnote directly under it, saved state changing the first button, light and dark.

**Checkpoint**: the wide hero exists and renders correctly; nothing calls it yet.

---

## Phase 4: User Story 2 - Full-width shelves that use the space (Priority: P1)

**Goal**: Below the hero every shelf is one row of cards that fills the width (six at about 1080dp, fewer when narrower, never cut off, never stretched), with its title and a "See all" link; each card shows cover, title, release year (or TBA) and the heart. At the end of this phase the wide layout is switched on.

**Independent Test**: Quickstart §2 rows "Count the cards", the card click/heart/long-press rows, "See all", and "Narrow the window step by step": on desktop at a window of at least 840dp, every shelf is a single row, six whole cards at about 1080dp, "See all" opens the same grid as the phone arrow, and narrowing the window reduces the cards and shows "See all" on rows that now hide games.

### Implementation for User Story 2

- [X] T012 [P] [US2] Create `FS/components/WideDiscoverShelfCard.kt` with `internal fun WideDiscoverShelfCard(game: GameItemUiModel, onClick: () -> Unit, onSaveClick: () -> Unit, onSaveLongClick: () -> Unit, modifier: Modifier = Modifier)` per contracts/ui-contract.md §3. It fills its slot's width at `WIDE_SHELF_CARD_ASPECT_RATIO` (3:4) and shows exactly what `CompactGameCard` shows, in the same way: `GenericGameCardLayout(onClick = onClick)` with no long press on the card itself, `GameCoverImage` filling the card, `SaveToWishlistButton(isSaved = game.isSaved, onSaveClick, onLongClick = onSaveLongClick, longClickLabel = stringResource(CoreUiRes.string.choose_list_content_description))` aligned top end, and over the bottom `fadingEdge(topAlpha = 1f, ...)` scrim with `Color.Black.copy(alpha = 0.7f)` and fixed-white text: the title (`labelLarge`, bold, one line, ellipsis) and `game.releaseYear ?: game.releaseDateText.asString()` (`labelSmall`, white at 0.8 alpha, one line, ellipsis). No developer, no score, no genre. Do not widen or edit `CompactGameCard`. Add `@QuestLogPreviews` previews: saved, unsaved, a very long title, and no cover.
- [X] T013 [US2] In `FS/components/WideDiscoverFeed.kt`, add a `private fun WideDiscoverShelf(shelf: DiscoverShelfUiModel, onGameClick: (Int) -> Unit, onSaveClick: (Int) -> Unit, onLongClick: (Int) -> Unit, onSeeAllClick: (DiscoverShelfUiModel) -> Unit, modifier: Modifier = Modifier)` and emit the shelf items after the hero, in the same order and with the same keys as the compact feed: each `recommended` shelf `item(key = discoverRecommendedKey(index))`, then `upcoming` with `DISCOVER_UPCOMING_KEY`, then `popular` with `DISCOVER_POPULAR_KEY`, each only when non-null. The shelf is a `Column` spaced by `spacing.medium`. Its **title row**: the shelf title (`titleMedium`, bold, `maxLines = 1`, ellipsis, weight 1, marked as a heading for accessibility) and, at the end, a `CustomTextButton` with the visible text `stringResource(Res.string.discover_see_all)`, shown only when `wideShelfShowsSeeAll(shelf.hasMore, shelf.games.size, columns)`, calling `onSeeAllClick(shelf)` and announced as `stringResource(Res.string.discover_see_all_shelf, shelf.title.asString())` through the semantics (as `DiscoverShelfTitleRow` does); keep the row at the minimum interactive height so every shelf has one rhythm. Its **card row**: a `BoxWithConstraints` whose `columns = wideShelfColumnCount(maxWidth, spacing.large, spacing.large)`, laying out a `Row` with `horizontalArrangement = Arrangement.spacedBy(spacing.large)`, horizontal padding `spacing.large`, and `columns` equal-weight slots; the first `min(shelf.games.size, columns)` slots hold `WideDiscoverShelfCard(game, onClick = { onGameClick(game.id) }, onSaveClick = { onSaveClick(game.id) }, onSaveLongClick = { onLongClick(game.id) })` and the rest are empty spacers of the same weight, so a card never stretches. The row is a plain `Row`, never a `LazyRow` or a horizontal scroll. Add previews: a full shelf, a shelf with fewer games than columns, a shelf with `hasMore = true`, a very long shelf title, and a `@Preview(widthDp = 1280, heightDp = 800)` and a narrow one. Depends on T005, T010, T012.
- [X] T014 [US2] In `FS/components/SearchMainContent.kt`, add `isWideWindow: Boolean = false` as the last parameter of `SearchMainContent` (after `discoverListState`), pass it to the private `DiscoverContent`, and there dispatch the `DiscoverContentState.Content` branch: `if (isWideWindow) WideDiscoverFeed(...) else DiscoverFeed(...)`, giving both the same arguments (`hero`, `popular`, `upcoming`, `onGameClick`, `onSaveClick = { onEvent(SearchUiEvent.OnToggleSave(it)) }`, `onLongClick = { onEvent(SearchUiEvent.OnOpenListSelector(it)) }`, `onSeeAllClick`, `recommended`, `isStale`, `isRefreshing`, the refresh and dismiss events, and `state = listState`). The `Loading` and `Error` branches stay identical for both. This is the only place the flag is read. The existing `DiscoverFeed` call must be byte-for-byte the same arguments as today. Add a wide-window Discover preview next to `SearchMainContentDiscoverPreview`. Depends on T013.
- [X] T015 [US2] In `FS/SearchScreen.kt`, add `isWideWindow: Boolean = false` to the public `SearchScreen` (after `modifier`, per contracts/ui-contract.md §1) and to `SearchScreenContent`, and pass it to `SearchMainContent(isWideWindow = isWideWindow, ...)`. Nothing else in `SearchScreenContent` reads it: the top bar, the FAB, the sheets and the snackbar are shared by both layouts. Depends on T014.
- [X] T016 [US2] In `shared/src/commonMain/kotlin/com/nikolasguillen/questlog/shared/QuestLogNavDisplay.kt`, in the `SearchRoute` branch of the `entryProvider`, pass `isWideWindow = twoPaneEnabled` to `SearchScreen(...)`. `twoPaneEnabled` is the value `QuestLogRoot` computes once from the window size class (840dp included); do not compute a second one and do not add a dependency. This switches the wide layout on. Depends on T015.
- [ ] T017 [US2] Checkpoint: run `.\gradlew.bat :feature:search:testAndroidHostTest --console=plain -q`, `.\gradlew.bat test` and `.\gradlew.bat :androidApp:assembleDebug`; all must pass (`KoinGraphTest` included). Then run `.\gradlew.bat :desktopApp:run` and go through quickstart §2 down to "Narrow the window step by step", recording anything that differs.

**Checkpoint**: US1 and US2 work together on desktop and tablets; this is the MVP.

---

## Phase 5: User Story 3 - Switching between layouts never disturbs the feed (Priority: P2)

**Goal**: Crossing 840dp in either direction swaps the layout without a reload and without losing the user's place, and any window narrower than 840dp is exactly as before. This is delivered by the shared state design (one `LazyListState`, the same item keys, the same ViewModel state) and switched on in T016; this phase proves it.

**Independent Test**: Quickstart §2 "Scroll to the second shelf, then drag the width across 840dp both ways, several times", and §3 "Phone, portrait and landscape".

### Verification for User Story 3

- [ ] T018 [US3] On desktop (`.\gradlew.bat :desktopApp:run`), with the feed loaded and scrolled to the second shelf, drag the window width across 840dp (including to exactly 840dp, which must be the wide layout, and 839dp, which must be the compact one) at least 20 times in both directions, per SC-005. Confirm: the layout swaps at 840dp; the same shelf stays at the top; no loading state appears; the feed is not fetched again. If the first visible shelf is lost, find the cause in `SearchScreenContent` / `SearchMainContent` state hoisting or in the item keys of T003, T010 and T013, fix it there, and record what it was.
- [ ] T019 [US3] On an Android resizable emulator or a tablet (`.\gradlew.bat :androidApp:installDebug`), check quickstart §3 rows: the wide layout at 840dp or more; resize or rotate across 840dp while scrolled keeps the shelf and does not reload; with a game open beside the feed in two panes, Discover is in the narrow left pane with the hero's buttons below the title and fewer cards per row, nothing cut off; and with a stale feed (change a saved game's status, come back) the refresh prompt works over the wide layout and keeps its state (shown, refreshing or dismissed) across a width swap.
- [ ] T020 [P] [US3] On a phone or phone emulator in portrait and landscape (below 840dp), confirm Discover is exactly as before: the short hero with its heart and whole-banner tap, the square cards, the sideways-scrolling shelves and arrows (SC-006). Also confirm the loading page (slow network), the error page and its retry (airplane mode, then retry), and the empty-feed placeholder look and behave the same in both layouts at a wide width.

**Checkpoint**: all three stories hold.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Documentation, accessibility checks and the final gates.

- [ ] T021 [P] Amend `feature/CLAUDE.md` in the "Navigation" section, where it says "A component that has to choose a layout by shape reads the space it was given, not the window.": add one sentence for the case this feature introduces — a screen with a separate wide layout receives the window's width class from `:shared` as a `Boolean` (`isWideWindow`, the 840dp rule `QuestLogRoot` already computes) and dispatches on it once, while components inside either layout still size by the space they are given. Write it as an instruction, not a changelog.
- [ ] T022 [P] On desktop, check SC-008 and FR-014: Tab moves focus through the search bar, the hero's two buttons, then each shelf's "See all" and each card, in screen order, and Enter activates them; the hero image is not a click target; and the new labels are announced sensibly (the "See all" link as "See all: <shelf title>", the save action by its label and state). Check light and dark: banner and card text stay legible on every cover.
- [ ] T023 [P] On desktop, scroll the feed with the mouse wheel and trackpad over the shelf rows. If it now scrolls vertically, delete the backlog item "Mouse wheel does not scroll in Discover (and maybe other screens)" from `docs/backlog.md` (no "fixed in" note); if it does not, leave the item and add what was observed to it.
- [ ] T024 Run the final gates: `.\gradlew.bat test` and `.\gradlew.bat :androidApp:assembleDebug`, both green with no test deleted or weakened; then walk quickstart §1 and tick off each expected result.
- [ ] T025 iOS: the code is `commonMain` only, so no iOS-specific work is expected. If a Mac is at hand, run `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64 -q`, then the iOS app on an iPad simulator, and check quickstart §2 and §3's wide steps. If no Mac is at hand, say in the final report that iOS was not verified; do not assume it.

---

## Dependencies & Execution Order

### Phase dependencies

- **Setup (Phase 1)**: none.
- **Foundational (Phase 2)**: after Setup; **blocks every user story**.
- **US1 (Phase 3)** and **US2 (Phase 4)**: both need Phase 2. They are both P1 and ship together; T012 can start with Phase 3, but T013 needs the feed file from T010 and the dispatch tasks (T014–T016) need T013.
- **US3 (Phase 5)**: needs T016, because it verifies the switched-on layout.
- **Polish (Phase 6)**: after the stories it checks; T021 can start any time.

### Task dependencies

- T002 → T003 (same file, `DiscoverFeed.kt`).
- T005 → T006; T005 → T009, T013.
- T003 → T010; T008 → T009 → T010 → T013 → T014 → T015 → T016 → T017.
- T012 → T013.
- T016 → T018, T019, T020.

### Story completion order

Phase 2 → US1 (hero) → US2 (shelves, then switched on) → US3 (verification) → Polish. US1 and US2 do not depend on each other's code except that the wide feed file is shared (T010 creates it, T013 extends it).

### Parallel opportunities

- After T001: T002, T004 and T005 touch different files and can run together (T003 follows T002).
- T006 (test) can run beside T008 once T005 is done.
- T012 (the card) can run beside T008–T010, since it is a new file.
- In Polish: T021, T022, T023 are independent.
- T020 can run beside T018 and T019 (different devices).

### Parallel example

```text
# After T001, together:
Task: "T002 Move DiscoverRefreshPrompt to FS/components/DiscoverRefreshPrompt.kt"
Task: "T004 Add the four new strings to feature/search/.../strings.xml"
Task: "T005 Create FS/components/WideDiscoverLayout.kt"

# Beside the hero work, together:
Task: "T009 Create FS/components/WideDiscoverHero.kt"
Task: "T012 Create FS/components/WideDiscoverShelfCard.kt"
```

---

## Implementation Strategy

### MVP first (US1 + US2)

1. Phase 1 and Phase 2: the groundwork, with the compact feed unchanged and green at every step.
2. Phase 3 (US1): the hero, checked in previews.
3. Phase 4 (US2): the shelves, then the three small edits (T014–T016) that switch the wide layout on.
4. **Stop and validate** on desktop (T017) before anything else.

### Incremental delivery

- Every task leaves the app building and the suites green, so the work can stop at any checkpoint. Wide windows keep showing the compact feed until T016, and only then see the new layout, complete.
- US3 adds no new code: it proves the resize behaviour, and its failures are fixed at the state hoisting or the item keys.

### Notes

- One new test file, no ViewModel, mapper, state, domain, data or database change, and no new module edge or library.
- Commit after each task or small group, with `type(scope): subject` messages (scope `search`, or `shared` for T016, or `docs` for T021), a body that explains why when it is not obvious, and the `Co-Authored-By` trailer this repository requires.
- If a task turns out to need a change outside the files it names, stop and say so before widening it: the owner asked for layout only, and the compact feed must not change.
