# Research: Wishlist Back-to-Top Button

**Feature**: [spec.md](spec.md) | **Plan**: [plan.md](plan.md) | **Date**: 2026-10-06

The spec has no `[NEEDS CLARIFICATION]` markers. The open points below came from reading Search's button
and the wishlist screen as `develop` has them now (including the 007 grid); each is resolved here.

---

## R1. One shared button, or a copy in the wishlist

**Finding**: Search builds its button inline in `SearchScreen.kt`: a `CustomFab` with a
`Modifier.animateFloatingActionButton(visible, alignment = Center)` and a `KeyboardArrowUp` icon, labelled
with Search's own string. FR-005 asks for the same icon, style, corner and animation.

**Decision**: Extract that block into a `ScrollToTopFab(visible, onClick, modifier)` composable in
`:core:ui` (`component/ScrollToTopFab.kt`), and make both Search and the wishlist screen use it.

**Rationale**:
- Constitution Principle IV: check the shared layer before adding to it, and don't leave two copies of a
  component. Two copies would drift, which FR-005 ("looks and behaves like Search's") is explicitly against.
- It is about ten lines, with no behaviour of its own to risk. Search's call site shrinks to one line.
- Both feature modules already depend on `:core:ui`, so there is **no new module edge**.

**Alternatives considered**:
- *Copy the block into the wishlist screen.* Rejected: duplication, with a second copy of the icon, the
  label and the animation to keep in step.
- *Leave Search alone and add a second, parallel shared composable.* Rejected: it is the same as
  duplicating, with extra indirection.

**Owner check**: this edits Search (one call site and one string). It is a deliberate extraction for reuse,
not a drive-by cleanup, but it is the one place this feature touches another screen.

---

## R2. The accessibility label

**Finding**: `scroll_to_top_content_description` ("Scroll to top") exists only in
`feature/search/src/main/res/values/strings.xml`, and is read through Search's own `R`. The wishlist
module cannot see another feature's resources. The repo has only a default `values/` folder in every
module (no translated `values-xx`), so "every supported language" today means the one English string.

**Decision**: Move the string to `core/ui/src/main/res/values/strings.xml` and read it inside
`ScrollToTopFab`. Remove it from Search's `strings.xml`; Search no longer references it directly.

**Rationale**: FR-008 says reuse Search's label. Moving it to the module that owns the shared button keeps
exactly one definition, and any later translation covers both screens.

**Alternatives considered**: duplicate the string in the wishlist module (two sources of truth).

---

## R3. When the button appears

**Decision**: Show it when `gridState.firstVisibleItemIndex > 1`, held as a shared constant
`UiConstants.SCROLL_TO_TOP_AFTER_ITEM_INDEX = 1` that Search uses too.

**Rationale**:
- It is the rule Search uses (`firstVisibleItemIndex > 1`), so FR-005 holds and the two screens cannot
  disagree.
- In the wishlist grid the item order is fixed by spec 007: index 0 is the list header, index 1 the
  filter/view row, index 2 the first status header. "First visible item is beyond index 1" therefore means
  the header and the row have both scrolled out of view, which is exactly FR-001 and SC-002. This holds in
  list and grid view alike, because both live in the same lazy grid.
- A `derivedStateOf` over a boolean means the screen recomposes only when the button should appear or
  disappear, not on every scroll frame (SC-005).

**Alternatives considered**: a pixel offset threshold. Rejected: different from Search, and it would not
match the "header and row gone" wording.

---

## R4. The scroll action

**Decision**: `gridState.animateScrollToItem(0)` launched from a `rememberCoroutineScope()`.

**Rationale**: Search does the same on its grid. Search additionally zeroes a collapsing-top-bar offset;
the wishlist's `TopAppBar` has no scroll behaviour, so there is nothing to reset. Item 0 is the header, so
the header ends fully visible (FR-003, SC-003). A touch during the animation takes over, which is what
`animateScrollToItem` does by default (spec edge case).

---

## R5. Where the button is allowed to show

**Finding**: The `LazyGridState` is hoisted in `WishlistContent` (007) so it outlives the `Empty` and
`FilteredEmpty` branches, which don't contain the grid. After a list empties, the state can still hold a
stale index greater than 1.

**Decision**: Show the button only while the content is `Success`, i.e.
`visible = state.contentState is WishlistContentState.Success && showScrollToTop`.

**Rationale**: FR-002 requires no button for an empty or loading list. Gating on the content state, not
on the scroll state alone, makes that true even with a stale index.

**Known limitation (owner decision in spec 007, unchanged)**: the scroll position is not reset when a
list empties and refills. With this feature the stale position is at least recoverable: the button
shows and one tap returns to the top. Do not add a reset without asking.

---

## R6. Placement, the snackbar and the system bars

**Decision**: Put it in the `Scaffold`'s `floatingActionButton` slot, which defaults to the end corner.

**Rationale**:
- It is the same slot and corner Search uses (FR-005).
- Material 3's `Scaffold` is expected to lay out the snackbar host above the FAB, so a snackbar (for
  example "This is now your default list") and the button do not overlap (FR-007). This is from how the
  Scaffold is documented to behave, not something checked in this repo yet: quickstart B9 confirms it on a
  device, and if it fails the fix belongs in the screen's layout, not in the button.
- The screen already sets `contentWindowInsets = WindowInsets.systemBars`, so the Scaffold keeps the
  button clear of the system bars.

---

## R7. Not covering the last game

**Finding**: The button floats over the bottom-end corner. In grid view the last card of a section sits in
exactly that corner, and at the very bottom the list cannot scroll further, so the card would stay partly
covered. Search had the same gap: its results grid ended with 8dp of padding and its Discover feed with 16dp.

**Decision**: One shared value, `ScrollToTopFabDefaults.ContentBottomPadding` in `:core:ui`, defined as
`MaterialTheme.spacing.doubleLarge * 3` (96dp), which clears the 56dp button, the Scaffold's 16dp margin
around it, and a little breathing room. It is the bottom content padding of the wishlist grid **and**, after
a follow-up the owner asked for, of Search's results grid and Discover feed, so the one button clears the last
item on every screen that shows it.

**Rationale**:
- FR-007 and User Story 2, scenario 3: the last game must be fully visible and tappable.
- It is built from an existing spacing token, so no new `dp` literal (Principle IV).
- The padding is constant. It does not depend on the button being visible, so the layout does not jump when
  the button appears. The cost is a little empty space at the end of the list.

**Alternatives considered**: measure the button and pad by its height (more state, for a fixed-size
component); a taller trailing spacer item (same constant, in a worse place).

---

## R8. No ViewModel or state change

**Decision**: Keep the scroll logic in the composable, like Search.

**Rationale**: Whether a button shows is a function of scroll position, not a business decision: nothing to
filter, sort or persist, no event or effect. Principle III is about decisions, and the project's own
precedent (Search) keeps scroll-derived UI state in the composable. No `UiState`, `UiEvent` or `UiEffect`
change.

---

## R9. Testing

**Finding**: There is no logic here that a JVM test can reach: a boolean derived from a Compose scroll
state, and one scroll call. The project has no Compose UI test setup (`androidTest` does not exist), and
constitution Principle V asks for tests on ViewModel, mapper and use-case logic, which this feature does
not add.

**Decision**: Verify by compiling, running the existing suites (they must stay green, since Search is
touched), and walking [quickstart.md](quickstart.md) on a device. Do not invent a test that asserts
nothing about the behaviour.

---

## Observed, not acted on

- `CompactGameCard` still hardcodes the "choose list" long-press label (noted in spec 007).
