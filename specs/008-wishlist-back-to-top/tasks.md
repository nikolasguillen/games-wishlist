---

description: "Task list for the Wishlist Back-to-Top Button (008)"
---

# Tasks: Wishlist Back-to-Top Button

**Input**: Design documents from `specs/008-wishlist-back-to-top/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md),
[contracts/scroll-to-top-fab.md](contracts/scroll-to-top-fab.md), [quickstart.md](quickstart.md)

**Tests**: **None, by design** (plan.md → Testing, research R9). The behaviour is a boolean derived from a
Compose scroll state plus one scroll call; the project has no Compose UI test setup and no JVM-reachable
logic here. What protects the work instead: the existing suites must stay green (Search is edited), the
modules must compile, and the on-device scenarios in [quickstart.md](quickstart.md) (B1–B17) must pass.
There is no CI, so "run" always means a local Gradle command.

**Organization**: grouped by user story from the spec. The shared component that both stories need is in
Phase 2.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: can run in parallel (different files, no dependency on an unfinished task)
- **[Story]**: US1–US2, matching the user stories in spec.md
- Commands use the Unix wrapper. On Windows use `.\gradlew.bat`.

## Path shorthand (all under the repo root)

| Alias | Directory |
|-------|-----------|
| `COREUI` | `core/ui/src/main/java/com/nikolasguillen/questlog/core/ui` |
| `SEARCH` | `feature/search/src/main/java/com/nikolasguillen/questlog/feature/search` |
| `WL` | `feature/wishlist/src/main/java/com/nikolasguillen/questlog/feature/wishlist` |

Rules that apply to every task: one composable per file; comments and KDoc in English; spacing from
`MaterialTheme.spacing`, never a new `dp` literal; user-visible text through `strings.xml`; every new
composable gets a `private` `@QuestLogPreviews` preview wrapped in `QuestLogTheme { }`; no build file
(`*.gradle.kts`) changes and no new module dependency.

---

## Phase 1: Setup

- [ ] T001 Confirm a green baseline before touching anything: run
  `./gradlew :feature:search:testDebugUnitTest :feature:wishlist:testDebugUnitTest --console=plain -q` on branch
  `008-wishlist-back-to-top`. It is expected to pass (wishlist: 37 ViewModel + 18 mapper tests). If anything
  already fails, stop and report it; do not fix it as part of this feature.
  **Status:** NOT RUN, at the owner's request ("I'll take care of the tests this time"). For reference, the wishlist suite was green (37 + 18) at the start of this branch after the chip-test alignment on develop.

---

## Phase 2: Foundational (blocks both user stories)

**Purpose**: the shared button, its string and the shared threshold, with Search switched over to them. Both
stories need the component, and Search must be proven unchanged before the wishlist adopts it.

- [X] T002 [P] Edit `COREUI/util/Constants.kt`: add to `object UiConstants` a
  `const val SCROLL_TO_TOP_AFTER_ITEM_INDEX = 1` with a one-line KDoc: a screen shows its scroll-to-top button
  once its first visible item index is greater than this.
- [X] T003 [P] Edit `core/ui/src/main/res/values/strings.xml`: add
  `<string name="scroll_to_top_content_description">Scroll to top</string>`, the exact text and name Search has
  today, placed next to the other `*_content_description` strings.
- [X] T004 Create `COREUI/component/ScrollToTopFab.kt` (depends on T003): `fun ScrollToTopFab(visible: Boolean,
  onClick: () -> Unit, modifier: Modifier = Modifier)`, public (two feature modules use it). Body: `CustomFab(onClick =
  onClick, modifier = modifier.animateFloatingActionButton(visible = visible, alignment = Alignment.Center)) {
  Icon(imageVector = Icons.Default.KeyboardArrowUp, contentDescription =
  stringResource(R.string.scroll_to_top_content_description)) }`, with `R` being `:core:ui`'s own
  (`com.nikolasguillen.questlog.core.ui.R`, imported bare). It holds no scroll state and no logic. Search opts in
  to `ExperimentalMaterial3Api` at file level; add `@OptIn(ExperimentalMaterial3Api::class)` on the composable
  only if the compiler asks for it. KDoc: what it is, and that the caller decides `visible` and what `onClick`
  does. End the file with a `private` `@QuestLogPreviews` preview (`visible = true`) wrapped in
  `QuestLogTheme { }`. See `contracts/scroll-to-top-fab.md`.
- [X] T005 Edit `SEARCH/SearchScreen.kt` (depends on T002, T004): (a) replace the whole `floatingActionButton = { CustomFab(...) { Icon(...) } }`
  block (around lines 229–242) with `floatingActionButton = { ScrollToTopFab(visible = showScrollToTop, onClick =
  { scope.launch { onScrollToTop() } }) }`, importing
  `com.nikolasguillen.questlog.core.ui.component.ScrollToTopFab`; (b) in `showScrollToTop`, replace both
  `firstVisibleItemIndex > 1` with `> UiConstants.SCROLL_TO_TOP_AFTER_ITEM_INDEX` (import
  `com.nikolasguillen.questlog.core.ui.util.UiConstants` if it is not there already). Do **not** touch
  `isScrolled` (it is `> 0` on purpose) or `onScrollToTop` (it also resets the collapsing top bar). (c) Remove
  only the imports that are now unused, checking each with a search of the file: `CustomFab`,
  `androidx.compose.material.icons.Icons`, `...icons.filled.KeyboardArrowUp`, `androidx.compose.material3.Icon`,
  `androidx.compose.material3.animateFloatingActionButton`, `androidx.compose.ui.Alignment`,
  `androidx.compose.ui.res.stringResource`. Search's own `R` is the same-package one, so it has no import.
- [X] T006 Edit `feature/search/src/main/res/values/strings.xml` (depends on T005): delete the
  `scroll_to_top_content_description` string. Then
  `grep -rn "scroll_to_top_content_description" --include='*.kt' --include='*.xml' . | grep -v /build/` must
  show only `core/ui` (the string and `ScrollToTopFab.kt`).
- [X] T007 [P] Edit `core/ui/CLAUDE.md` (depends on T004): add `ScrollToTopFab` to the screen-level and domain
  components list: the floating up-arrow button Search and the wishlist screen share; the caller owns
  `visible` and `onClick`, and shows it when its first visible item index is greater than
  `UiConstants.SCROLL_TO_TOP_AFTER_ITEM_INDEX`. Also mention that constant in the `Constants.kt` sentence. Write
  it as an instruction, not a changelog.
- [ ] T008 Checkpoint: run `./gradlew :core:ui:compileDebugKotlin :feature:search:compileDebugKotlin
  :feature:wishlist:compileDebugKotlin --console=plain -q` and `./gradlew :feature:search:testDebugUnitTest
  --console=plain -q`. Both must pass. Then, on a device or emulator, scroll Search's results and the Discover
  feed down and confirm its button looks and behaves exactly as before (quickstart B16). Do not go on if Search
  changed.

**Checkpoint**: the shared button exists, Search uses it, nothing changed for Search users.
  **Status:** Compile DONE (`:core:ui`, `:feature:search`, `:feature:wishlist` all compile). OPEN for the owner: the Search unit tests, and the on-device Search check (quickstart B16).
  **Owner:** reports the on-device scenarios pass. The unit-test run is not confirmed, so this stays open.

---

## Phase 3: User Story 1 — Jump back to the top of a long wishlist (P1) 🎯 MVP

**Goal**: past the header and the filter/view row, a floating up-arrow appears bottom-right; one tap scrolls
smoothly to the very top and the button hides.

**Independent Test**: quickstart B1–B4, B10, B12. Open a long wishlist, scroll down until the button appears,
tap it, and confirm you land at the top with the header fully visible.

- [X] T009 [US1] Edit `WL/WishlistScreen.kt`, inside `WishlistContent`, using the `gridState` that is already
  hoisted there (spec 007): (a) add `val scope = rememberCoroutineScope()` and `val showScrollToTop by remember {
  derivedStateOf { gridState.firstVisibleItemIndex > UiConstants.SCROLL_TO_TOP_AFTER_ITEM_INDEX } }`; (b) pass
  to the existing `Scaffold(...)` a `floatingActionButton = { ScrollToTopFab(visible = state.contentState is
  WishlistContentState.Success && showScrollToTop, onClick = { scope.launch { gridState.animateScrollToItem(0)
  } }) }`; (c) add the imports (`ScrollToTopFab`, `UiConstants`, `derivedStateOf`, `rememberCoroutineScope`,
  `kotlinx.coroutines.launch`; `remember` and `getValue` are already imported). Do not add a ViewModel
  event, a state field or a scroll reset: this is UI-only, and the stale-scroll limitation is the owner's
  decision recorded in research R5. Leave every other branch of the `when` untouched.
- [ ] T010 [US1] Verify: `./gradlew :feature:wishlist:compileDebugKotlin :feature:wishlist:testDebugUnitTest
  --console=plain -q` must pass with the 55 existing tests unchanged. Then on a device or emulator run
  quickstart B1, B2, B3, B4, B10 and B12. Fix anything that fails before moving on.

**Checkpoint**: the button works end to end in the list layout. This is a shippable MVP.
  **Status:** Compile DONE. OPEN for the owner: the 55 wishlist unit tests (expected unchanged) and on-device B1, B2, B3, B4, B10, B12.
  **Owner:** reports the on-device scenarios pass. The unit-test run is not confirmed, so this stays open.

---

## Phase 4: User Story 2 — The button stays out of the way (P2)

**Goal**: never on short, empty, loading or filtered-empty lists; never covering the last game; coexisting
with the snackbar and the system bars; the same in list and grid view.

**Independent Test**: quickstart B5–B9, B11, B13, B14.

- [X] T011 [P] [US2] Edit `WL/components/WishlistGamesList.kt`: give the `LazyVerticalGrid` a
  `contentPadding = PaddingValues(bottom = MaterialTheme.spacing.doubleLarge * 3)` (96dp, built from the
  existing token; add the `PaddingValues` import) with a short comment: it clears the 56dp scroll-to-top button
  plus the Scaffold's margin around it, so the last game is never left covered at the bottom of the list. The
  value is constant and does not depend on whether the button is visible. Do not add a `dp` literal.
- [ ] T012 [US2] Verify: `./gradlew :feature:wishlist:compileDebugKotlin :feature:wishlist:testDebugUnitTest
  --console=plain -q`, then on a device or emulator run quickstart B5, B6, B7, B8, B9, B11, B13 and B14. B9
  (snackbar above the button) is the one that confirms an assumption from research R6: if it fails, fix the
  layout in `WishlistScreen.kt`, not in the shared button.

**Checkpoint**: both stories hold.
  **Status:** Compile DONE. OPEN for the owner: the wishlist unit tests and on-device B5, B6, B7, B8, B9 (confirms the snackbar sits above the button, research R6), B11, B13, B14.
  **Owner:** reports the on-device scenarios pass. The unit-test run is not confirmed, so this stays open.

---

## Phase 5: Polish & cross-cutting

- [X] T013 Review the whole diff against the constitution and the module `CLAUDE.md` files: one composable per
  file; no `dp` literal that has a `MaterialTheme.spacing` token; no hardcoded display text; no unused imports in
  `SearchScreen.kt`, `WishlistScreen.kt`, `WishlistGamesList.kt` or `ScrollToTopFab.kt`; KDoc in English;
  `git diff --stat -- '*.gradle.kts'` is empty (no new module dependency).
- [X] T014 Run `./gradlew :app:assembleDebug --console=plain -q` (the change spans three modules).
- [ ] T015 Run `./gradlew test --console=plain -q`. Everything must pass, including the pre-existing suites.
  **Status:** NOT RUN, at the owner's request. `:app:assembleDebug` (T014) passes.
- [ ] T016 Walk [quickstart.md](quickstart.md) once more on a device, B1–B17. B15 (TalkBack) needs a person and
  B17 needs a 300+ game list; if either cannot be run, say so rather than ticking the box.
  **Status:** NOT RUN, at the owner's request. B15 (TalkBack) and B17 (300+ games) also need a person.
  **Owner:** reports the on-device scenarios pass. The unit-test run is not confirmed, so this stays open.

---

## Phase 6: Follow-up (owner request after the first implementation)

**Purpose**: the wishlist grid got bottom padding for the button but Search's lists did not. Use one shared
value everywhere the button appears.

- [X] T017 Edit `COREUI/component/ScrollToTopFab.kt`: add `object ScrollToTopFabDefaults` with a
  `@Composable` getter `ContentBottomPadding: Dp = MaterialTheme.spacing.doubleLarge * 3`, with a KDoc saying why
  (the 56dp button, the Scaffold margin, breathing room; constant on purpose).
- [X] T018 [P] Edit `WL/components/WishlistGamesList.kt`: pass `ScrollToTopFabDefaults.ContentBottomPadding`
  as the grid's bottom `contentPadding` instead of its own copy of the expression.
- [X] T019 [P] Edit `SEARCH/components/SearchResultGrid.kt` (was `bottom = spacing.medium`) and
  `SEARCH/components/DiscoverFeed.kt` (was `vertical = spacing.large`; now `top = spacing.large` and the shared
  bottom value).
- [X] T020 Compile `:core:ui`, `:feature:search`, `:feature:wishlist`, and update `research.md` R7, `plan.md`,
  `contracts/scroll-to-top-fab.md`, `quickstart.md` (new B18) and `core/ui/CLAUDE.md` to match.
- [X] T021 Verify on a device (owner): quickstart B18, and re-check B8 now that the wishlist reads the shared value.
  **Status:** DONE. The owner ran the on-device scenarios and reports they work.

---

## Dependencies & Execution Order

### Phase dependencies

- **Phase 1 (T001)** → no dependencies.
- **Phase 2 (T002–T008)** → after T001; **blocks both stories**.
- **US1 (T009–T010)** → after Phase 2.
- **US2 (T011–T012)** → T011 can start as soon as Phase 2 is done, in parallel with US1 (different file);
  T012 needs both T009 and T011 (the button must exist to be checked against the last card and the snackbar).
- **Polish (T013–T016)** → after both stories.

### Within-phase ordering

- Phase 2: {T002 ∥ T003} → T004 → T005 → T006; T007 can run once T004 exists; T008 last.
- US1: T009 → T010. US2: T011 → T012 (after T009).

### Files edited by more than one task (do not parallelise across these)

`SEARCH/SearchScreen.kt` (T005 only), `WL/WishlistScreen.kt` (T009 only, and T012 only if B9 forces a layout
fix), `COREUI/util/Constants.kt` (T002 only).

---

## Parallel Opportunities

```text
# Phase 2, after T001:
T002 (constant)  ∥  T003 (string)   ─▶  T004 (component) ─▶ T005 (Search) ─▶ T006 (remove old string)
                                                          └─▶ T007 (core/ui CLAUDE.md)   ─▶ T008 (checkpoint)

# After Phase 2:
T009 (WishlistScreen: button)   ∥   T011 (WishlistGamesList: bottom padding)
        └────────────────────────────────┬───────────────────────────────┘
                                   T010 / T012 (verify)
```

---

## Implementation Strategy

### MVP first (User Story 1 only)

1. Phase 1 and Phase 2, ending with the T008 checkpoint (Search unchanged).
2. US1 (T009–T010). **Stop and validate**: scroll a long wishlist, tap the button, land at the top.
3. This already ships the button. Note that without T011 the button can cover the last card in grid view, so
   do US2 before releasing.

### Incremental delivery

1. Shared component and Search switched over (Phase 2) → wishlist button (US1) → polish of placement (US2).
2. Each phase ends with a verify task. Do not start the next on a red build.
3. Suggested commits, in `type(scope): subject` form: `refactor(ui): share the scroll-to-top button between
   screens` (Phase 2, including Search and the string move), `feat(wishlist): add a back-to-top button`
   (T009, T011), `docs: record back-to-top task progress` (this file).

### Notes

- Search is edited in T005–T006. That is deliberate (plan, "Points for the owner"): if the owner would rather
  not touch Search, stop after T002–T004 and copy the component's body into the wishlist instead, and say so.
- The scroll position is not reset when a list empties and refills (research R5, a standing owner decision from
  spec 007). With this feature a stale position is recoverable by tapping the button. Do not add a reset.
- If a task's instructions and the source disagree, the source wins: re-read the file and adapt, then note it.
