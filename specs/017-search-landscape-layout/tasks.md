---

description: "Task list for the Search landscape layout"
---

# Tasks: Search Landscape Layout

**Input**: Design documents from `specs/017-search-landscape-layout/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md) (R1–R12),
[data-model.md](data-model.md), [quickstart.md](quickstart.md)

**Tests**: One JVM test file for the grid's pure geometry (plan, research R12). The project has no Compose UI
tests (`docs/tech-debt.md`), so every visual check is a row of the hands-on matrix in
[quickstart.md](quickstart.md) §3, and the hands-on tasks below name the rows.

**Organization**: Grouped by user story. US1 (side rail) and US2 (grid columns) are both P1 and touch
different modules, so they can proceed in parallel once Phase 2 is done.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependency on an unfinished task)
- **[Story]**: US1, US2 or US3, from spec.md

## Path Conventions

Multiplatform modules keep sources in `src/commonMain/kotlin/com/nikolasguillen/questlog/...`. Paths below
abbreviate that prefix as follows, and always name the full module directory:

- `core/ui/…` = `core/ui/src/commonMain/kotlin/com/nikolasguillen/questlog/core/ui/…`
- `core/designsystem/…` = `core/designsystem/src/commonMain/kotlin/com/nikolasguillen/questlog/core/designsystem/…`
- `shared/…` = `shared/src/commonMain/kotlin/com/nikolasguillen/questlog/shared/…`
- `feature/search/…` = `feature/search/src/commonMain/kotlin/com/nikolasguillen/questlog/feature/search/…`

Every comment and KDoc is in English. Everything is `commonMain`: no `android.*` or `java.*` import. On
Windows use `.\gradlew.bat` in place of `./gradlew`.

---

## Phase 1: Setup

**Purpose**: Know the starting point, so any later failure is attributable to this feature.

- [ ] T001 On branch `017-search-landscape-layout`, run `./gradlew :androidApp:assembleDebug --console=plain -q`
  and `./gradlew test --console=plain -q`, and confirm both pass before any edit. If either fails, stop and
  report it, because Android must build and pass at every commit.

---

## Phase 2: Foundational (blocks US1 and US2)

**Purpose**: The one landscape rule that `:shared` (rail) and `:feature:search` (grid) both read
(research R1, data-model "Window shape").

**⚠️ CRITICAL**: US1 and US2 cannot start before T002 exists.

- [ ] T002 [P] Create `core/ui/util/WindowShape.kt` (package `com.nikolasguillen.questlog.core.ui.util`) with
  `@Composable fun isLandscapeWindow(): Boolean`. It reads
  `LocalWindowInfo.current.containerDpSize` and returns `width > height`, **strictly greater, so a square
  window is portrait**. It is public (used from `:shared` and `:feature:search`). Add a KDoc saying the
  window's shape decides, not the device, so a split-screen half that is narrow and tall stays portrait and
  a wide tablet window is landscape. No new dependency (`LocalWindowInfo` is already used in
  `core/ui/component/CustomAlertDialog.kt`).
- [ ] T003 [P] In `core/ui/CLAUDE.md`, under "Utilities (`util/`)", add one entry for `WindowShape.kt`
  (`isLandscapeWindow()`: the single place the "wider than tall" rule is written; read it, do not re-derive
  it).
- [ ] T004 Run `./gradlew :core:ui:compileAndroidMain --console=plain -q` and confirm it compiles.

**Checkpoint**: `isLandscapeWindow()` is available to both stories.

---

## Phase 3: User Story 1 - Navigate from a side bar in landscape (Priority: P1) 🎯 MVP

**Goal**: In landscape the three top-level destinations are a vertical rail on the start edge. In portrait
nothing changes. (spec FR-001–FR-006, FR-015)

**Independent Test**: quickstart §3 rows 1–5, 14, 18, 19, 20. Open Search, rotate to landscape: a rail with
Search / Radar / Lists, no bottom bar and no empty strip. Tap through the three. Rotate back: the bar returns.

### Implementation for User Story 1

- [ ] T005 [P] [US1] Create `shared/TopLevelDestination.kt`: `internal enum class TopLevelDestination` with
  entries in this order, which is the on-screen order in both the bar and the rail: `SEARCH`, `RADAR`,
  `LISTS`. Each carries `route: NavKey`, `icon: ImageVector` and `label: StringResource`:
  - `SEARCH`: `SearchRoute`, `Icons.Default.Search`, `Res.string.search_nav_bar_item`
  - `RADAR`: `RadarRoute`, `Icons.Default.Radar`, `Res.string.radar_nav_bar_item`
  - `LISTS`: `ListsRoute`, `Icons.AutoMirrored.Filled.List`, `Res.string.lists_nav_bar_item`

  Add `fun isSelected(currentRoute: NavKey): Boolean`: `currentRoute is SearchRoute` for `SEARCH`,
  `is RadarRoute` for `RADAR`, and **`is ListsRoute || is WishlistRoute`** for `LISTS`. `Res` is imported bare
  (`com.nikolasguillen.questlog.shared.resources.Res`) and each string with its own import, as
  `QuestLogBottomBar.kt` does today. One enum in one file, no `UiModel` suffix (it is an identifier set, like
  `SearchSort`).
- [ ] T006 [P] [US1] In `core/designsystem/theme/AppComponentsColors.kt`, add
  `val navRailItemColors: NavigationRailItemColors`, with the same `@Composable get()` shape as
  `navBarItemColors`, calling `NavigationRailItemDefaults.colors(indicatorColor =
  MaterialTheme.appColors.navBarItemIndicatorColor, selectedIconColor =
  MaterialTheme.appColors.navBarItemSelectedIconColor)`. No new `AppColors` field: the rail reuses the bar's
  colors (`NavigationRailItemColors` is a different type from `NavigationBarItemColors`, which is why a
  second token is needed, research R2).
- [ ] T007 [US1] Create `shared/QuestLogNavigationRail.kt` (depends on T005, T006), containing:
  1. `internal val QuestLogNavigationRailInsets: WindowInsets` (`@Composable get()`), defined as
     `(WindowInsets.systemBars union WindowInsets.displayCutout).only(WindowInsetsSides.Vertical +
     WindowInsetsSides.Start)`. M3's default omits the cutout, and in landscape the camera sits on the start
     edge (research R3).
  2. `@Composable fun QuestLogNavigationRail(currentRoute: NavKey, onNavigateToRoute: (NavKey) -> Unit,
     modifier: Modifier = Modifier)`. It takes the route, not the back stack, so it can be previewed. It is a
     Material 3 `NavigationRail` with `containerColor = MaterialTheme.appColors.navBarContainerColor` and
     `windowInsets = QuestLogNavigationRailInsets`. Its content is `Spacer(Modifier.weight(1f))`, then one
     `NavigationRailItem` per `TopLevelDestination.entries`, then `Spacer(Modifier.weight(1f))`: this
     centers the items vertically, because `NavigationRail` has no arrangement parameter in this version. Each
     item has `selected = destination.isSelected(currentRoute)`, `icon = Icon(destination.icon,
     contentDescription = stringResource(destination.label))`, `label = { Text(stringResource(destination.label)) }`,
     `colors = AppComponentsColors.navRailItemColors`, and an `onClick` that calls
     `onNavigateToRoute(destination.route)` only `if (currentRoute != destination.route)`. The routes are
     `data object`s, so this is the bar's exact guard, and tapping Lists from a wishlist still navigates.
  3. A private `@QuestLogPreviews` preview wrapped in `QuestLogTheme { }`, with Search selected.
- [ ] T008 [US1] Rewrite the body of `QuestLogBottomBar` in `shared/QuestLogBottomBar.kt` to iterate
  `TopLevelDestination.entries` (depends on T005). Keep its public signature, the colors
  (`containerColor = MaterialTheme.appColors.navBarContainerColor`, `AppComponentsColors.navBarItemColors`), the
  icon `contentDescription`, the label, and the same `if (currentRoute != destination.route)` click guard as
  in T007. **Output, order and behavior must be identical to today**: this is the portrait path (SC-007).
- [ ] T009 [US1] Edit `QuestLogContent` in `shared/QuestLogRoot.kt` (depends on T002, T007, T008):
  1. Compute `val isTopLevelRoute = backStack.last() is SearchRoute || backStack.last() is RadarRoute ||
     backStack.last() is ListsRoute` and `val isLandscape = isLandscapeWindow()` (import it from `:core:ui`).
  2. Hoist the existing navigate logic into one local `val onNavigateToRoute: (NavKey) -> Unit` (pop to the
     root, then add the route unless it is `SearchRoute`), used by both the bar and the rail. The logic itself
     is unchanged.
  3. Wrap the layout in a `Row(modifier = modifier)`. First child:
     `AnimatedVisibility(visible = isLandscape && isTopLevelRoute, enter = slideInHorizontally(initialOffsetX =
     { -it }), exit = slideOutHorizontally(targetOffsetX = { -it }))` containing `QuestLogNavigationRail(
     currentRoute = backStack.last(), onNavigateToRoute = onNavigateToRoute)`. Flip the sign of both
     offsets when `LocalLayoutDirection.current == LayoutDirection.Rtl`, so the rail slides from the start
     edge in right-to-left layouts too.
  4. Second child: the existing `Scaffold`, with `Modifier.weight(1f)` (and the `modifier` parameter no longer
     applied to it). While `isLandscape && isTopLevelRoute`, also apply
     `Modifier.consumeWindowInsets(QuestLogNavigationRailInsets)`, so no screen pads the start edge a second
     time. Keep `contentWindowInsets = WindowInsets(0, 0, 0, 0)`.
  5. In the Scaffold's `bottomBar`, change `AnimatedVisibility(visible = …)` to `isTopLevelRoute &&
     !isLandscape`, with the same vertical slide as today (FR-002).

  Portrait must remain exactly as before (no extra padding, same bar).
- [ ] T010 [US1] In the root `CLAUDE.md`, update two places: the "`:shared` is the only module that knows about
  navigation" bullet (it owns "the bottom bar" → "the bottom bar and the landscape side rail"), and the
  "Where things live" row for theme/scaffold/bottom bar so it also lists
  `shared/.../QuestLogNavigationRail.kt` and `shared/.../TopLevelDestination.kt`. Rewrite the existing
  wording in place and do not add a changelog note.
- [ ] T011 [US1] Run `./gradlew :core:designsystem:compileAndroidMain :shared:compileAndroidMain
  --console=plain -q`, then `./gradlew :shared:testAndroidHostTest --console=plain -q` (`KoinGraphTest` and
  `RoutesSerializationTest` must stay green; there is no DI or route change), and fix anything that fails.
- [ ] T012 [US1] Hands-on check on an Android emulator: run quickstart §3 rows 1, 2, 3, 4, 5, 14, 18, 19 and
  20. Record any row that fails and fix it before moving on. Rows 15 and 21 belong to US3.

**Checkpoint**: US1 is complete and demoable on its own (rail in landscape, bar in portrait). This is the MVP.

---

## Phase 4: User Story 2 - See more games per row in landscape (Priority: P1)

**Goal**: The results grid shows more than two portrait-sized cards per row in landscape, and still two in
portrait. (spec FR-007–FR-011)

**Independent Test**: quickstart §3 rows 6–9. Search "zelda", rotate to landscape: at least 3 equal-width cards
per row with even gaps. Rotate back: 2 per row.

### Tests for User Story 2

> Write the test first. It will not compile until T014 adds the declarations, which counts as failing.

- [ ] T013 [US2] Create
  `feature/search/src/androidHostTest/kotlin/com/nikolasguillen/questlog/feature/search/components/SearchResultGridCellsTest.kt`
  (JUnit4; KDoc header saying it covers the results grid's column count and cell sizes; call everything with
  `Density(1f)` so dp equals px). It tests `resultGridColumnCount(availableWidth: Dp, spacing: Dp, edge: Dp,
  isLandscape: Boolean): Int` and `SearchResultGridCells(columnCount: Int, edgePadding: Dp)`
  (`calculateCrossAxisCellSizes(availableSize: Int, spacing: Int): List<Int>`), using spacing = 16 and edge =
  16. The cases are:
  1. Portrait is 2 at widths 320, 360, 412, 800 and 1280 (a portrait tablet stays at two, FR-007).
  2. Landscape worked examples: 560 → 3, 805 → 4, 654 → 3, 918 → 5.
  3. The landscape floor: widths 300 and 400 still give 3.
  4. The boundary: the smallest width that gives 4 columns gives 4, and one dp less gives 3.
  5. For every n from 2 to 6 and widths 300–1200, the cell sizes add up to `available − (n − 1)·spacing`.
  6. The first and last cells exceed every middle cell by exactly `edgePadding`, give or take the one leftover
     pixel, and the visible card widths (cell size minus edge padding on the first and last) differ by at most
     1px.
  7. `n = 2` at 360 gives `[172, 172]` (a 156 card after the 16dp edge padding): today's geometry.
  8. A width that does not divide evenly spreads the leftover pixels one per cell from the start: no pixel is
     lost and no cell grows by more than 1.

### Implementation for User Story 2

- [ ] T014 [US2] Create `feature/search/components/SearchResultGridCells.kt` (depends on T013's signatures):
  - `internal val MIN_CARD_WIDTH = 150.dp`, feature-local, with a KDoc saying it is the narrowest a two-column
    portrait card is on the phones the app targets.
  - `internal fun resultGridColumnCount(availableWidth: Dp, spacing: Dp, edge: Dp, isLandscape: Boolean): Int`:
    **portrait → 2, always; landscape → `max(3, floor((availableWidth − 2·edge + spacing) / (MIN_CARD_WIDTH +
    spacing)))`**.
  - `internal class SearchResultGridCells(private val columnCount: Int, private val edgePadding: Dp) :
    GridCells`, whose `calculateCrossAxisCellSizes` returns `[c + edge, c, …, c, c + edge]`, where
    `c = (W − 2·edge − (n − 1)·spacing) / n` and `edge = edgePadding.roundToPx()`, with any leftover pixels
    handed out one per cell from the start. With n = 2 this is `[c + edge, c + edge]`, today's geometry. The
    sizes must sum to `W − (n − 1)·spacing`. The KDoc explains why the edge cells are wider (every visible
    card ends up the same width at any column count, and the full-span filters row keeps bleeding to the
    screen edge, research R5).
- [ ] T015 [US2] Edit `feature/search/components/SearchResultGrid.kt` (depends on T002, T014). In the
  **non-empty branch only**, wrap the `LazyVerticalGrid` in a `BoxWithConstraints` that carries the incoming
  `modifier`, and read the grid's own `maxWidth` from it (the screen minus the rail and insets, not the window).
  Compute `val columnCount = resultGridColumnCount(maxWidth, MaterialTheme.spacing.large,
  MaterialTheme.spacing.large, isLandscapeWindow())` (import it from `:core:ui`) and pass `columns =
  SearchResultGridCells(columnCount, MaterialTheme.spacing.large)`. Replace the `index % 2` padding with the
  column-aware rule: `val column = index % columnCount`; the card pads `start = MaterialTheme.spacing.large` when
  `column == 0`, `end = MaterialTheme.spacing.large` when `column == columnCount - 1`, and nothing otherwise.
  Keep everything else: the full-span `filtersHeader` item, the `key = game.id`, `animateItem`, the
  `Arrangement.spacedBy(MaterialTheme.spacing.large)` gaps, `contentPadding` and `state`. Leave the empty
  branch (`NoFilteredResultsPlaceholder`) untouched. Add a private `@QuestLogPreviews`-style preview at
  landscape size (check how `QuestLogPreviews` is declared in `:core:designsystem` and add a variant with
  `widthDp = 800, heightDp = 360`) showing at least 9 dummy games.
- [ ] T016 [US2] Confirm FR-011 by reading, and change nothing unless it fails: in
  `feature/search/components/SearchMainContent.kt` and `feature/search/SearchScreen.kt`, verify that text
  results, genre browse and a Discover shelf's "see all" all reach `SearchResultGrid` through
  `SearchContentState.Success`, and that `grep -rn "GridCells" feature/search` shows only
  `SearchResultGrid.kt` and `SearchResultGridCells.kt`. If another grid exists in the search feature, apply
  T015's rule to it.
- [ ] T017 [US2] Run `./gradlew :feature:search:testAndroidHostTest --console=plain -q`. The 8 new cases and the
  entire existing suite (`SearchViewModelTest` and the mapper tests) must pass with no existing test changed.
  Then run `./gradlew :feature:search:compileAndroidMain --console=plain -q`.
- [ ] T018 [US2] Hands-on check on an emulator: run quickstart §3 rows 6, 7, 8 and 9, plus row 1 (portrait is
  still 2 columns, unchanged). Check that the three-column grid has equal-width cards and even gaps.

**Checkpoint**: US2 is complete and works with or without US1.

---

## Phase 5: User Story 3 - The rest of Search fits the landscape screen (Priority: P2)

**Goal**: Every other Search state is fully visible and operable in landscape, with nothing under the camera
cutout. (spec FR-012, FR-013, SC-005, SC-006)

**Independent Test**: quickstart §3 rows 10–13, 15–17, 21 and 22. Walk every Search state in landscape (feed,
bar, results, empty, loading, error, filter sheet, sort sheet) and confirm nothing is clipped.

### Implementation for User Story 3

- [ ] T019 [P] [US3] In `feature/search/components/DiscoverHero.kt`, replace `val heroHeight = 240.dp` with
  `minOf(HERO_MAX_HEIGHT, LocalWindowInfo.current.containerDpSize.height * HERO_MAX_HEIGHT_FRACTION)`.
  Add two file-private constants at the top of the file: `private val HERO_MAX_HEIGHT = 240.dp` and
  `private const val HERO_MAX_HEIGHT_FRACTION = 0.5f`, each with a one-line KDoc (research R7). There is no
  landscape branch, and the hero is 240dp on every portrait phone. Import `LocalWindowInfo` from
  `androidx.compose.ui.platform`.
- [ ] T020 [P] [US3] In `feature/search/SearchScreen.kt`, give the `Scaffold` in `SearchScreenContent` an explicit
  `contentWindowInsets = ScaffoldDefaults.contentWindowInsets.union(WindowInsets.displayCutout.only(
  WindowInsetsSides.Horizontal))`, so the last grid column does not sit under the camera when the phone is
  rotated so that the cutout is on the end edge (research R8). No other change to the screen.
- [ ] T021 [P] [US3] In `core/ui/component/MainScreenHeader.kt`, change the row's
  `.windowInsetsPadding(TopAppBarDefaults.windowInsets)` to `.windowInsetsPadding(
  TopAppBarDefaults.windowInsets.union(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal)))`, so
  the profile button clears the cutout. The horizontal cutout is zero in portrait, so nothing moves there. This
  header is shared, so Radar's and Lists' headers get the same protection (plan, change 1). Update the KDoc only
  if it mentions insets.
- [ ] T022 [P] [US3] In `feature/search/components/SearchSortBottomSheet.kt`, make the options column scroll:
  its modifier chain becomes `Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
  .navigationBarsPadding().padding(bottom = MaterialTheme.spacing.extraLarge)`, so the padding scrolls with
  the content and the last option is always reachable on a short screen or at a large font (research R10).
  Leave the filter sheet alone: its options area already scrolls.
- [ ] T023 [US3] Run `./gradlew :core:ui:compileAndroidMain :feature:search:compileAndroidMain
  --console=plain -q` and `./gradlew :feature:search:testAndroidHostTest :core:ui:testAndroidHostTest
  --console=plain -q`, and confirm they pass.
- [ ] T024 [US3] Hands-on check on an emulator with **Developer options → Display cutout → Punch hole**: run
  quickstart §3 rows 10, 11, 12, 13, 15, 16, 17, 21 and 22. Row 13 is repeated at the largest font size. Row
  21 is the corner-rounding check (research R11): if the rounding next to the rail is noticeable, round only
  the end corners of `cornerClipModifier` in `shared/QuestLogRoot.kt` while the rail is visible, and say so in
  the report.

**Checkpoint**: All three stories work, and the whole Search section is consistent in landscape.

---

## Phase 6: Polish & Cross-Cutting Concerns

- [ ] T025 Run `./gradlew test --console=plain -q` and `./gradlew :androidApp:assembleDebug --console=plain -q`:
  both must pass, with no test deleted or weakened (constitution V).
- [ ] T026 Review the full diff against the rules, and fix what you find: no new `dp` literal at a call site
  (only the named constants `MIN_CARD_WIDTH`, `HERO_MAX_HEIGHT` and `HERO_MAX_HEIGHT_FRACTION`); one type per
  file; `internal` by default (only `isLandscapeWindow()` and `QuestLogBottomBar` stay public); no
  `android.*`/`java.*` in `commonMain`; no new string and no hardcoded text; all comments in English; no edit
  under `**/build/generated/**`.
- [ ] T027 Run the whole quickstart §3 matrix (all 22 rows) once more on the final build, and in
  `quickstart.md` §3 add a line under the table recording the date, the emulator and any row that was not run.
- [ ] T028 iOS, **Mac only**: run `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64 -q` and the
  `xcodebuild` command from quickstart §4, then quickstart §4's rows on a notched simulator. If no Mac is
  available, leave this task unchecked and say in the report that iOS is **not verified**. Do not mark it done.
- [ ] T029 Do not edit `docs/tech-debt.md`. The follow-ups in plan.md (Radar's and Lists' bodies still use
  `WindowInsets.systemBars` in landscape, the hero crops a portrait cover into a wide banner,
  `WishlistGamesList` is still `Fixed(2)`) are listed in the plan, and adding them as debt is the owner's
  call. Mention them in the final report.

---

## Dependencies & Execution Order

### Phase dependencies

- **Phase 1 (T001)**: none.
- **Phase 2 (T002–T004)**: after T001. **Blocks US1 and US2** (both need `isLandscapeWindow()`). US3's tasks
  T019–T022 do not need it.
- **US1 (T005–T012)**, **US2 (T013–T018)**, **US3 (T019–T024)**: all depend on Phase 2 (US3 only for the
  build to be green). They touch different modules and can run in parallel.
- **Polish (T025–T029)**: after every story you intend to ship.

### Within each story

- **US1**: T005 and T006 (parallel) → T007 (needs both) → T008 (needs T005) → T009 (needs T002, T007, T008) →
  T010, T011 → T012.
- **US2**: T013 (test, fails) → T014 → T015 → T016 → T017 → T018.
- **US3**: T019, T020, T021 and T022 are all different files and run in parallel → T023 → T024.

### Story independence

- US1 ships alone (rail, with the grid still at 2 columns). US2 ships alone (grid, with the bottom bar still in
  landscape). US3 refines both but compiles and tests alone. Its hands-on rows 11, 14, 15 and 22 only show the
  full picture once US1 is in.

### Parallel opportunities

- T002 ∥ T003.
- T005 ∥ T006.
- After T004: the whole of US1 ∥ the whole of US2 ∥ T019–T022 of US3.
- T019 ∥ T020 ∥ T021 ∥ T022.

---

## Parallel Example: after Phase 2

```text
# Different modules, no shared files:
Task: "T005 Create shared/TopLevelDestination.kt"                         (US1, :shared)
Task: "T006 Add navRailItemColors in AppComponentsColors.kt"              (US1, :core:designsystem)
Task: "T013 Create SearchResultGridCellsTest.kt"                          (US2, :feature:search tests)
Task: "T019 Cap the hero height in DiscoverHero.kt"                       (US3, :feature:search)
Task: "T021 Add the cutout inset to MainScreenHeader.kt"                  (US3, :core:ui)
```

---

## Implementation Strategy

### MVP first (User Story 1 only)

1. T001 baseline → Phase 2 (T002–T004) → US1 (T005–T012).
2. **Stop and validate**: quickstart rows 1–5, 14, 18–20. Landscape now has a rail and portrait is unchanged.

### Incremental delivery

1. Add US2 (T013–T018): the grid goes multi-column. Validate rows 6–9.
2. Add US3 (T019–T024): cutouts, hero, sort sheet. Validate rows 10–17, 21, 22.
3. Polish (T025–T029). Each story adds value without changing portrait, and Android builds and passes tests
   at every commit.

### Commit rhythm

Commit after each story's checkpoint (or each task group), with `type(scope): subject` messages (for example
`feat(shared): show a navigation rail in landscape`, `feat(search): add columns to the results grid in
landscape`, `fix(search): keep the hero and sheets inside a landscape screen`). Scopes come from `SCOPES` in
`.githooks/commit-msg`.

---

## Notes

- [P] tasks touch different files and have no dependency on an unfinished task.
- Each story is independently completable and testable (quickstart rows listed above).
- T013 is written before T014 and fails until T014 exists.
- iOS can only be verified on a Mac (T028).
- Avoid vague edits: every task names its file, its values and the research item that explains the choice.
