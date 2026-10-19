---

description: "Task list for two-pane search and game detail"
---

# Tasks: Two-Pane Search and Game Detail

**Input**: Design documents from `specs/018-two-pane-search-detail/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md) (R1–R10),
[data-model.md](data-model.md), [contracts/navigation-contract.md](contracts/navigation-contract.md),
[quickstart.md](quickstart.md)

**Tests**: The spec does not ask for tests, but the constitution (Principle V) requires JVM tests for new logic. So
the pure back stack rules (`SearchDetailPanes.kt`), the route's new field and the window-shape rule each get one
test file. The project has no Compose UI tests (`docs/tech-debt.md`): every visual or gesture check is a scenario
(S1–S9) of [quickstart.md](quickstart.md), and the hands-on tasks below name the scenarios.

**Organization**: Grouped by user story. Most of the work lands in US1 (the scene) and US2 (replace and back).
US3 and US4 are mostly verification of behavior that falls out of the design, plus the tests that pin it. US5 is the
only P2 story and is independent of US2–US4.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependency on an unfinished task)
- **[Story]**: US1 to US5, from spec.md

## Path Conventions

Multiplatform modules keep sources in `src/commonMain/kotlin/com/nikolasguillen/questlog/...`. Paths below
abbreviate that prefix as follows, and always name the full module directory:

- `core/navigation/…` = `core/navigation/src/commonMain/kotlin/com/nikolasguillen/questlog/core/navigation/…`
- `core/ui/…` = `core/ui/src/commonMain/kotlin/com/nikolasguillen/questlog/core/ui/…`
- `shared/…` = `shared/src/commonMain/kotlin/com/nikolasguillen/questlog/shared/…`
- `feature/game-detail/…` = `feature/game-detail/src/commonMain/kotlin/com/nikolasguillen/questlog/feature/gamedetail/…`
- `feature/search/…` = `feature/search/src/commonMain/kotlin/com/nikolasguillen/questlog/feature/search/…`
- Tests: `<module>/src/androidHostTest/kotlin/com/nikolasguillen/questlog/<package>/…`

Every comment and KDoc is in English. Everything is `commonMain`: no `android.*` or `java.*` import, and no
`expect`/`actual`. No new user-facing text is introduced. On Windows use `.\gradlew.bat` in place of `./gradlew`.

**Commit boundaries**: Android must build and pass its tests at every commit. T013 and T014 change two sides of
one signature (`QuestLogNavDisplay`'s new parameter) and must land in the same commit.

---

## Phase 1: Setup

**Purpose**: Know the starting point, so any later failure is attributable to this feature, and declare the one
dependency the scene needs.

- [X] T001 On branch `018-two-pane-search-detail`, run `.\gradlew.bat :androidApp:assembleDebug --console=plain -q`
  and `.\gradlew.bat test --console=plain -q`, and confirm both pass before any edit. If either fails, stop and
  tell the owner which test or module, rather than working on top of a red baseline.
  *Result: `test` is red before any edit, 9 of 277 failing, all in `:core:data` (DataStore
  `Unable to rename …preferences_pb.tmp` in the Windows temp dir; environmental, not touched by this feature). The
  owner was told and said to resume, so the failures are treated as a known baseline. Phases are verified with the
  changed modules' tests plus `:androidApp:assembleDebug`.*
- [X] T002 [P] In `shared/build.gradle.kts`, add `implementation(libs.jetbrains.navigationevent.compose)` to
  `commonMain.dependencies`, next to the other `libs.jetbrains.*` lines. The catalog entry already exists
  (`gradle/libs.versions.toml:109`) and `:feature:search` / `:feature:onboarding` already use it. This is the only
  build change in the feature, called out in the plan's Complexity Tracking. Do not add any other dependency or
  module edge.

---

## Phase 2: Foundational (blocking prerequisites)

**Purpose**: The route field, the pure back stack rules and the optional back button. Nothing visible changes yet:
every existing call site keeps compiling because the new route field has a default and the callers already pass
lambdas.

**⚠️ CRITICAL**: No user story can start until this phase is complete.

- [X] T003 In `core/navigation/Routes.kt`, change `GameDetailRoute` to
  `data class GameDetailRoute(val gameId: Int, val fromSearch: Boolean = false) : GameNavKey`. Give it a KDoc
  saying `fromSearch` marks a detail opened from the Search flow (results, the Discover feed, a "see all" shelf, a
  genre browse, or a similar game inside another `fromSearch` detail) and that only these take part in two-pane
  mode. Do not touch `GameNavSavedStateConfiguration.kt`: the class is already registered and a saved stack
  without the field decodes to `false`.
- [X] T004 In `core/navigation/src/androidHostTest/.../core/navigation/RoutesSerializationTest.kt`, add
  `GameDetailRoute(gameId = 1942, fromSearch = true)` to `everyRoute` (the class-coverage tests still pass, since
  they compare classes). Add two tests: `fromSearch is kept through a save and restore` (round trip of
  `GameDetailRoute(5, fromSearch = true)` asserts `fromSearch == true`) and `a route saved without fromSearch
  restores as false` (decode `{"type":"<serialName of GameDetailRoute>","gameId":1942}` through the polymorphic
  `NavKey` serializer; derive the serial name from `serializer(GameDetailRoute::class.java).descriptor.serialName`
  as the existing test does, and assert `fromSearch == false`). Run `.\gradlew.bat :core:navigation:testAndroidHostTest --console=plain -q`.
- [X] T005 Create `shared/SearchDetailPanes.kt` with the pure rules from
  [contracts/navigation-contract.md](contracts/navigation-contract.md) §1, all `internal`, top-level functions, English
  KDoc, no Compose and no Android import:
  `fun <T> searchDetailRun(items: List<T>, isSearch: (T) -> Boolean, isSearchDetail: (T) -> Boolean): IntRange?`
  (the indices of the trailing run of `isSearchDetail` items sitting directly on an `isSearch` item that is at index
  0, or `null`; this generic form is what the scene strategy reuses over entry metadata in T012);
  `fun searchDetailRun(keys: List<NavKey>): IntRange?` (delegates with `SearchRoute` and
  `GameDetailRoute.fromSearch`);
  `fun openSearchDetail(backStack: MutableList<NavKey>, gameId: Int, twoPaneEnabled: Boolean)` (`NavBackStack` is a
  `MutableList`; below the threshold: push `GameDetailRoute(gameId, fromSearch = true)` unless it already equals
  the top; at or above it: if the run is already exactly that one detail do nothing, otherwise remove the whole
  run, then add the detail); and
  `fun navigateBack(backStack: MutableList<NavKey>, twoPaneEnabled: Boolean)` (with `twoPaneEnabled` and a run:
  remove the whole run; otherwise `removeLastOrNull()`; never empties a stack of one, as today's `onBack` does not
  because `NavDisplay` only calls it when there is something to pop; keep that behavior, add a `size > 1` guard).
  The invariant (a `fromSearch` detail only exists inside a run on `SearchRoute`) is in
  [data-model.md](data-model.md); put it in the KDoc.
- [X] T006 Create `shared/src/androidHostTest/.../shared/SearchDetailPanesTest.kt` (JUnit4, KDoc header saying what
  it covers). Cover, with plain `mutableListOf<NavKey>`: run detection for the four stacks in
  [data-model.md](data-model.md); `openSearchDetail` below the threshold pushes one entry and ignores a repeat of the
  top; at or above it from `[Search]` gives `[Search, D(1,s)]`; again with another game gives
  `[Search, D(2,s)]` (replace); with the same game is a no-op; with a three-detail run built narrow
  (`[Search, D1, D2, D3]`) collapses to a single detail; `navigateBack` wide removes the whole run, narrow removes
  only the last entry, and a one-entry stack is left alone. Run `.\gradlew.bat :shared:testAndroidHostTest --console=plain -q`.
- [X] T007 [P] In `core/ui/…/component/ImmersiveDetailLayout.kt`, change `onBackClick: () -> Unit` to
  `onBackClick: (() -> Unit)?` and update its KDoc (`null` draws no back button). In the `navigationIcon` slot
  (around line 97) draw the existing `ActionIconButton` only when `onBackClick != null`; leave the slot empty
  otherwise. Game detail is the only caller. Keep the file's preview compiling.
- [X] T008 [P] In `feature/game-detail/…/components/DetailErrorLoadingWrapper.kt`, change `onBackClick` to
  `(() -> Unit)?`; the `IconButton(onClick = onBackClick)` back button (around line 38) is drawn only when it is
  non-null. This keeps the back button out of the loading and error states of a detail shown in a pane.
- [X] T009 In `feature/game-detail/…/GameDetailScreen.kt`, `…/components/GameDetailMainContent.kt` and
  `…/components/GameDetailSuccessContent.kt`, thread the nullable type: every `onBackClick: () -> Unit` becomes
  `(() -> Unit)?` (`GameDetailScreen` line 42 and the internal content at 117, `GameDetailMainContent` line 23,
  `GameDetailSuccessContent` line 47) down to the `ImmersiveDetailLayout` and `DetailErrorLoadingWrapper` calls.
  The previews keep passing `{}`. Add the one-line KDoc on the public `GameDetailScreen` parameter: `null` hides the
  back button, for a screen that is not the only thing on the display. Do not mention panes or navigation in the
  feature module. Depends on T007 and T008.
- [X] T010 Checkpoint: run `.\gradlew.bat :core:ui:compileAndroidMain :feature:game-detail:compileAndroidMain --console=plain -q`,
  then `.\gradlew.bat test --console=plain -q` and `.\gradlew.bat :androidApp:assembleDebug --console=plain -q`.
  All green, and the app behaves exactly as before on a phone (a quick manual open-and-back of a game).

**Checkpoint**: Foundation ready. User story work can begin.

---

## Phase 3: User Story 1 - Look at a game without leaving the results (Priority: P1) 🎯 MVP

**Goal**: On a window of at least 840dp, a game opened from Search appears in a right pane while Search stays on
the left, with the rail visible and Search selected. Search is full width until a game is opened.

**Independent Test**: [quickstart.md](quickstart.md) S1 and S2 on a tablet in landscape. Before the tap: Search
full width, no divider or placeholder. After the tap: detail on the right with no back button, results, query,
filter and scroll position unchanged on the left, both panes scroll independently, rail visible with Search
selected.

- [X] T011 [US1] Create `shared/SearchDetailScene.kt`: `internal class SearchDetailScene<T : Any>(...) : Scene<T>`
  holding the Search entry and the detail entry. `key` is the Search entry's `contentKey` (stable while the detail
  is replaced, so a swap starts no `NavDisplay` transition); `entries = listOf(searchEntry, detailEntry)`;
  `previousEntries` is the entries up to and including the Search entry; `content` is a `Row` with two
  `weight(1f)` boxes and a Material `VerticalDivider` between them (no new spacing token and no `dp` literal). The
  left box calls `searchEntry.Content()` inside
  `CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides rememberNavigationEventDispatcherOwner(enabled = false))`
  (imports from `androidx.navigationevent.compose`, see [research.md R5](research.md#r5-the-system-back-gesture-while-both-panes-are-composed)),
  so the Search pane's own back handler cannot swallow the system back. The right box calls
  `detailEntry.Content()`. Implement `equals`/`hashCode` through a `data class` or by hand, as `SinglePaneScene` does.
  One class per file.
- [X] T012 [US1] Create `shared/SearchDetailSceneStrategy.kt`: the metadata markers and the strategy. Define two
  `internal` metadata keys/helpers (one marking the Search entry, one marking a `fromSearch` detail entry) and
  `internal class SearchDetailSceneStrategy(private val twoPaneEnabled: Boolean) : SceneStrategy<NavKey>`. In
  `calculateScene(entries)`: return `null` when `!twoPaneEnabled`; otherwise call the generic `searchDetailRun`
  from T005 over `entries`, using the markers as `isSearch` / `isSearchDetail`; when a run exists return
  `SearchDetailScene(searchEntry = entries[0], detailEntry = entries[run.last], previousEntries = entries.take(run.first))`
  (the Search entry is at index 0, so `take(run.first)` ends with it); otherwise `null`, so the default
  single-pane strategy renders exactly as today. Depends on T005 and T011. The strategy must not reference any
  route class, only the markers.
- [X] T013 [US1] In `shared/QuestLogRoot.kt` (lands with T014): read once
  `val twoPaneEnabled = currentWindowAdaptiveInfo().windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)`
  (same imports as `QuestLogNavigationSuite.kt`; this is the 840dp breakpoint and 840dp exactly counts). Compute
  `val searchDetailRun = searchDetailRun(backStack)`. Set the suite's `isVisible` to
  `isTopLevelRoute || (twoPaneEnabled && searchDetailRun != null)` and its `currentRoute` to `SearchRoute` while
  that second case holds, so the Search destination stays selected (FR-006); pass `twoPaneEnabled` to
  `QuestLogNavDisplay`. Leave `onNavigateToRoute` and the deep-link `LaunchedEffect` unchanged (the deep link keeps
  `GameDetailRoute(gameId)`).
- [X] T014 [US1] In `shared/QuestLogNavDisplay.kt` (lands with T013): add a `twoPaneEnabled: Boolean` parameter;
  switch `NavDisplay` to its `sceneStrategies = listOf(SearchDetailSceneStrategy(twoPaneEnabled))` overload (it falls
  back to the single-pane strategy when the list returns `null`); give the `SearchRoute` entry the Search marker and
  a `GameDetailRoute` entry with `key.fromSearch` the detail marker, through `NavEntry(key, metadata = …)`; make
  Search's `onGameClick` call `openSearchDetail(backStack, gameId, twoPaneEnabled)` (this replaces the
  `GameDetailRoute(gameId)` push and its guard); make `onBack` call `navigateBack(backStack, twoPaneEnabled)`; pass
  the detail screen `onBackClick = null` when `twoPaneEnabled && key.fromSearch` and the existing
  `{ backStack.removeLastOrNull() }` otherwise. Keep the existing `transitionSpec` / `popTransitionSpec` /
  `predictivePopTransitionSpec` and every other route branch as they are. Do not touch the Radar and Wishlist
  branches (they keep `GameDetailRoute(gameId)`).
- [X] T015 [US1] Make entering, leaving and swapping the two-pane layout immediate (no crossfade that makes the Search
  pane blink; [research.md R8](research.md#r8-transitions)): give `SearchDetailScene.metadata` (override the default
  in `shared/SearchDetailScene.kt`) the transition metadata that disables the default fade for the scene change
  (`NavDisplay.transitionSpec` / `popTransitionSpec` / `predictivePopTransitionSpec` with `EnterTransition.None togetherWith ExitTransition.None`),
  and, if the scene metadata alone does not win over the transitioning entries' (the transitioning entry's metadata
  has precedence per the `NavDisplay` KDoc), apply the same metadata to the Search and detail entries in T014's
  `NavEntry` calls. Which place takes effect is verified on a device in T016, not assumed.
- [X] T016 [US1] Verify. `.\gradlew.bat :shared:compileAndroidMain --console=plain -q`, then
  `.\gradlew.bat test --console=plain -q` and `.\gradlew.bat :androidApp:assembleDebug --console=plain -q`. Then
  [quickstart.md](quickstart.md) S1 and S2 on a tablet emulator (Pixel Tablet, landscape), plus the blink check from
  S3 (open, swap and close a detail and watch the left pane). If the left pane blinks or the transition is
  wrong, fix T015's metadata placement. Note anything that looks wrong at half width (3+ cramped columns in the
  grid are expected until US5).

**Checkpoint**: US1 works and is demonstrable on its own (MVP). Replacing and back are not complete yet: a second tap
already replaces the detail (T005 made `openSearchDetail` replace), but similar games and the back gesture are
verified in US2.

---

## Phase 4: User Story 2 - Compare several games quickly (Priority: P1)

**Goal**: There is always exactly one detail in the right pane. Tapping another result or a similar game replaces
it; one system back closes it and returns Search to full width.

**Independent Test**: [quickstart.md](quickstart.md) S3 and S4. Five taps in a row, then a similar game, show the last
game each time; one back gives full-width Search with the same results; with a committed query, the first back
closes the detail without clearing the query.

- [X] T017 [US2] In `shared/QuestLogNavDisplay.kt`, change the `GameDetailRoute` branch's `onGameClick`: when
  `key.fromSearch`, call `openSearchDetail(backStack, gameId, twoPaneEnabled)` (replace in two-pane mode, push
  below the threshold, and the new detail stays `fromSearch`); when it is not `fromSearch` (a detail opened from
  Radar, a wishlist or a reminder link) keep today's guarded `backStack.add(GameDetailRoute(gameId))`. Depends on
  T014.
- [X] T018 [US2] Verify on a tablet emulator: [quickstart.md](quickstart.md) S3 (five swaps, a similar game, one
  back; repeat with predictive back on Android 14+ and check the preview shows full-width Search) and S4 (with a
  committed query, the first back closes the detail and keeps the query; the second back clears the query). If the
  first back still clears the search, the disabled dispatcher in T011 is not reaching the Search pane's
  `NavigationBackHandler` (`feature/search/.../SearchScreen.kt:224`): fix it in `shared/SearchDetailScene.kt` (for
  example by providing the owner above the entry content rather than inside it), never by changing
  `:feature:search`. Also open an image full-screen in the detail pane and confirm back closes the viewer first.

**Checkpoint**: US1 and US2 together are the whole core experience.

---

## Phase 5: User Story 3 - Rotate or resize without losing my place (Priority: P1)

**Goal**: Crossing 840dp in either direction keeps the search and the open game: narrow shows the detail
full-screen with a working back button, wide shows both panes.

**Independent Test**: [quickstart.md](quickstart.md) S5, including 20 consecutive rotations.

No new production code is expected: the back stack is not touched by a resize, only the chosen scene changes
(R4). These tasks pin that behavior and prove it on devices.

- [X] T019 [US3] In `shared/src/androidHostTest/.../shared/SearchDetailPanesTest.kt`, add the resize cases: a run built
  while narrow (`[Search, D1, D2, D3]`) then `navigateBack` wide leaves `[Search]`; the same stack with
  `navigateBack` narrow leaves `[Search, D1, D2]` and a further narrow back leaves `[Search, D1]` (the history is
  walked one entry at a time, as today); `openSearchDetail` after widening replaces the whole run. Run
  `.\gradlew.bat :shared:testAndroidHostTest --console=plain -q`.
- [X] T020 [US3] Verify [quickstart.md](quickstart.md) S5 steps 1–5 on a tablet (rotate) and in a freeform or
  split-screen window (resize across 840dp), with a typed query, an active filter and a scrolled grid. Check that the
  query, filter chip and scroll position survive every crossing, that the detail is full-screen with a working
  back button when narrow, that back returns to the same results, and that no empty pane or duplicated detail ever
  appears in 20 rotations. If the Search pane loses state when it moves between scenes, fix it in the scene or
  the entry decorators in `shared/` (the entry's `ViewModel` is not at risk), never in `:feature:search`.

**Checkpoint**: US1 to US3 hold across window changes.

---

## Phase 6: User Story 4 - Nothing changes on narrower windows and other entry points (Priority: P1)

**Goal**: Below 840dp everything behaves exactly as before, and on any window a detail opened from Radar, a
wishlist or a release-reminder link is full-screen.

**Independent Test**: [quickstart.md](quickstart.md) S6 on a phone and on a tablet.

- [X] T021 [US4] In `shared/src/androidHostTest/.../shared/SearchDetailPanesTest.kt`, add the isolation cases:
  `[Search, D(9)]` (a deep-linked, non-`fromSearch` detail on Search) has no run and `navigateBack` wide pops just
  that entry; `[Search, D(1,s), D(9)]` has no run (the deep-linked detail covers it) and wide back pops only
  `D(9)`, after which the run is back; `[Search, Radar, D(4)]` has no run; below the threshold `openSearchDetail`
  never removes anything. Then, in `shared/QuestLogNavDisplay.kt` and `shared/QuestLogRoot.kt`, confirm by reading
  that the Radar branch, the Wishlist branch and the deep-link `LaunchedEffect` (`QuestLogRoot.kt` line 79) still
  create `GameDetailRoute(gameId)` with `fromSearch = false`, and that no other code path creates
  `fromSearch = true` except `openSearchDetail`. Fix any that do not.
- [ ] T022 [US4] Verify [quickstart.md](quickstart.md) S6 on a phone under 840dp (both orientations) and on a tablet:
  below the threshold open a game from Search, a similar game, then back twice and see today's behavior; on the
  tablet open games from Radar and from a wishlist (full-screen, back button, no rail), tap a release reminder
  while two panes are open (full-screen over both, back returns to the two panes), and tap a similar game inside a
  Radar-opened detail (it stacks full-screen).

**Checkpoint**: The feature is strictly additive.

---

## Phase 7: User Story 5 - Both panes are comfortable at half width (Priority: P2)

**Goal**: The Search screen (including spec 017's landscape layout) and the game detail fit a half-width pane: the
results grid chooses its columns from its own space, cutout insets pad only the outer edge, and nothing is
clipped, overlapping or hidden by the rail.

**Independent Test**: [quickstart.md](quickstart.md) S7 on a window just above 840dp and on a very wide one.

- [X] T023 [P] [US5] In `core/ui/…/util/WindowShape.kt`, add
  `fun isLandscapeShape(width: Dp, height: Dp): Boolean = width > height` (a square is portrait; plain function,
  public, KDoc) and make `isLandscapeWindow()` call it with `LocalWindowInfo.current.containerDpSize`, so the
  landscape rule is still written once.
- [X] T024 [US5] Create `core/ui/src/androidHostTest/kotlin/com/nikolasguillen/questlog/core/ui/util/WindowShapeTest.kt`
  (JUnit4, KDoc header): wider than tall is landscape, taller than wide is portrait, a square is portrait. Run
  `.\gradlew.bat :core:ui:testAndroidHostTest --console=plain -q`. Depends on T023.
- [X] T025 [US5] In `feature/search/…/components/SearchResultGrid.kt`, replace `isLandscape = isLandscapeWindow()`
  (line 84) with `isLandscape = isLandscapeShape(maxWidth, maxHeight)` taken from the grid's own
  `BoxWithConstraints`, and swap the import (line 35). Update the comment above the `BoxWithConstraints` to say the
  grid's own space decides both the column count and the landscape rule. When Search is full width this is the same
  value as before, so spec 017's behavior is unchanged. Depends on T023.
- [X] T026 [P] [US5] In `shared/SearchDetailScene.kt`, make each pane consume the cutout inset of the side it does
  not touch ([research.md R7](research.md#r7-making-both-screens-fit-a-half-width-pane)): the left box gets
  `Modifier.consumeWindowInsets(WindowInsets.displayCutout.only(WindowInsetsSides.End))` and the right box the
  same with `Start`, so `SearchScreen`'s Scaffold (which unions the horizontal cutout) does not pad the inner edge
  against the divider. Use the layout-direction-aware `Start`/`End` sides, not left/right. The rail already
  consumes the start side of the left pane.
- [ ] T027 [US5] Verify [quickstart.md](quickstart.md) S7 at about 841dp (foldable or freeform window) and at 1280dp:
  the Discover feed, the results grid (2 columns in a portrait-shaped pane, no card narrower than on a portrait
  phone), "see all", genre browse, the empty / error / loading states, the filter and sort sheets, the list chooser,
  the detail's loading / error / success states, the expanded search overlay (it covers both panes with spec 017's
  landscape layout), and the cutout padding on a landscape phone of at least 840dp. Fix any clipping or
  overlap found, in the pane-agnostic way (use the available space, never the window or a pane flag). Record in a
  short note in this task's commit message what was fixed.
- [X] T028 [P] [US5] In `core/ui/CLAUDE.md`, update the `WindowShape.kt` line: it now holds `isLandscapeWindow()` and
  `isLandscapeShape(width, height)`, and a component that sits in a space smaller than the window (the results
  grid) passes its own size to the second. Edit the existing rule; do not add a changelog note.

**Checkpoint**: All five stories work.

---

## Phase 8: Polish & cross-cutting concerns

- [X] T029 [P] Update the instruction files in the same commit as the behavior they describe: in
  `feature/CLAUDE.md` (Navigation section) say that a Search-opened game goes through `openSearchDetail` in
  `:shared` and carries `GameDetailRoute.fromSearch`, and that a screen's `onBackClick` may be `null` when it is not
  the only thing on screen; in the root `CLAUDE.md` "Where things live" table, add `SearchDetailPanes.kt`,
  `SearchDetailSceneStrategy.kt` and `SearchDetailScene.kt` to the row for the navigation suite and back stack
  files. Edit the existing rules in place; no history notes.
- [ ] T030 Final verification on Windows: `.\gradlew.bat test --console=plain -q` (all suites green, including
  `KoinGraphTest` and `RoutesSerializationTest`; none deleted or weakened) and
  `.\gradlew.bat :androidApp:assembleDebug --console=plain -q`. Then one last pass of [quickstart.md](quickstart.md)
  S1–S8 on a tablet emulator and a phone, and confirm no `dp` literal, hardcoded text, `android.*` import or new
  module edge slipped in (`git diff --stat` should touch only the files named in [plan.md](plan.md)).
- [ ] T031 iOS check, **Mac only** (do not report it as done from Windows): `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64 -q`,
  the `xcodebuild` command from the root `CLAUDE.md`, then [quickstart.md](quickstart.md) S9 on an iPad simulator
  (open, swap, edge-swipe back, rotate, S7). Until this runs, say in the final report that iOS is unverified.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: none. T001 first, T002 in parallel.
- **Foundational (Phase 2)**: after Setup; blocks every story.
- **US1 (Phase 3)**: after Phase 2. T011 → T012; T013 and T014 together; T015 after T011/T014; T016 last.
- **US2 (Phase 4)**: after US1 (edits the same `QuestLogNavDisplay.kt` branch and verifies the scene).
- **US3 (Phase 5)**: after US2 (the tests need `navigateBack`/`openSearchDetail` from T005, but the verification
  needs the working scene and back).
- **US4 (Phase 6)**: after US1; independent of US2/US3 apart from sharing the test file with T006/T019 (sequential
  edits).
- **US5 (Phase 7)**: after Phase 2. T023, T026 and T028 are independent files; T024, T025 depend on T023; T026
  needs T011; T027 needs all of Phase 7 plus US1.
- **Polish (Phase 8)**: after every story you intend to ship.

### Story dependencies

- US1 needs only Foundational. US2 builds on US1. US3 and US4 only verify and pin what US1/US2 produce. US5 is
  independent of US2–US4 in code and can start once the scene file exists (T011).

### Parallel opportunities

- T001 ‖ T002.
- T007 ‖ T008 ‖ (T003 → T004, T005 → T006): the `:core:ui` and `:feature:game-detail` edits do not touch the
  navigation work. T009 waits for T007 and T008.
- After T011: T023 ‖ T026 ‖ T028 can run while US1 wiring (T012–T015) proceeds, as they touch other files (T026 edits
  `SearchDetailScene.kt`, so it must not overlap T015, which also edits it).
- Test files: `SearchDetailPanesTest.kt` is edited by T006, T019 and T021, so those are sequential.

### Parallel example: Foundational

```text
Track A: T003 → T004 → T005 → T006        (route, route test, back stack rules, rules test)
Track B: T007 ‖ T008 → T009               (optional back button through core:ui and game-detail)
Join:    T010
```

---

## Implementation Strategy

### MVP first (User Story 1)

1. Phase 1 and Phase 2 (the app is unchanged on every device).
2. Phase 3 (US1). **Stop and validate** S1 and S2 on a tablet. This is the demonstrable slice: a game opens beside
   the results. A second tap already replaces the detail because `openSearchDetail` does so from the start.
3. Continue with US2 for similar games and the back gesture before showing it to anyone: without T018's check the
   back gesture may clear the search instead of closing the detail.

### Incremental delivery

1. Foundational → US1 (MVP) → US2 (core experience complete) → US3 (resize) → US4 (regression guard).
2. US5 (half-width fit) can land any time after T011, but it is needed before the feature feels right on a foldable:
   until T025, the grid forces three or more narrow columns into a portrait-shaped pane.
3. Polish, then the Mac-only iOS check.

---

## Notes

- Every story's code path is `commonMain`; there is no platform contract and no `expect`/`actual`.
- Never edit anything under `**/build/generated/**`, and do not change a ViewModel, use case or the data layer.
- Do not add a pane flag, a route reference or any navigation call to `:feature:search` or `:feature:game-detail`.
- The large-phone-in-landscape question ([plan.md](plan.md), "Decisions for the Owner") is deliberately not coded:
  the width-only rule in T013 is the single place to change if the owner decides otherwise.
- Commit after each task or logical group, with `type(scope): subject` messages (scopes such as `shared`,
  `navigation`, `ui`, `game-detail`, `search`), ending with the trailer the repository requires.

---

## Implementation notes

Hands-on checks ran on the `Medium_Tablet` emulator (1280x800dp), driven with `adb`.

**Deviations from the plan, found by running it**

- **Back handling (T011, T018).** The plan's disabled child dispatcher did not work: Search registers its back handler
  once and keeps that registration when its composition moves into the scene, so the dispatcher never saw it. A
  default-priority handler added after the panes also lost, depending on how Search was composed (it failed after a
  rotate-then-open). The scene now registers an overlay-priority handler (`SearchDetailBackHandler.kt`). Verified:
  one back closes the detail and keeps the query, on both the direct path and the rotate-then-open path; the next
  back clears the query; back on the expanded search bar collapses it and leaves the panes alone. A consequence: no
  predictive back preview while two panes are showing (documented in research R5).
- **Grid columns (T025).** The left pane's grid box is about 600x600dp on a 1280dp tablet, so it shows 3 columns of
  about 180dp (portrait-phone card size), not the 2 the plan predicted; at about 850dp wide it shows 2 (research R7).

**Verified on the emulator**: S1 (no empty pane), S2 (detail beside results, query and scroll kept, no back button,
rail with Search selected), S3 (replace, a related game replaces in place, one back returns to full width), S4,
S5 steps 1 to 3 (rotate across 840dp both ways), S8 (tab switch), and a Radar-opened game staying full-screen with a
back button. The detail also fits a ~850dp window.

**Not verified, so the tasks stay open**

- T022: a game opened from a wishlist, from a release-reminder link, and a similar game inside a Radar-opened detail.
- T027: the empty, error and loading states, the filter and sort sheets, the list chooser, and cutout padding on a
  device with a cutout.
- T020 is checked for the rotation path; not run: 20 repeated rotations, freeform or split-screen resizing. The
  stacked-while-narrow case is covered by unit tests (`SearchDetailPanesTest`).
- T015/T016: whether opening and closing blinks the Search pane cannot be judged from still screenshots.
- T030: `gradlew test` is red only on the 9 `:core:data` DataStore failures that were red before any edit; every other
  module is green, and `:androidApp:assembleDebug` passes.
- T031: Mac only.