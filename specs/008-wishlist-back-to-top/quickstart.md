# Quickstart: Validating the Wishlist Back-to-Top Button

**Feature**: [spec.md](spec.md) | **Plan**: [plan.md](plan.md)

There is no CI and no UI test setup, so validation is compiling, running the existing suites, and walking
the scenarios below on a device. Commands use the Unix wrapper; on Windows use `.\gradlew.bat`.

## 1. Build and automated checks

```bash
./gradlew :core:ui:compileDebugKotlin --console=plain -q
./gradlew :feature:search:compileDebugKotlin --console=plain -q     # the shared button replaces Search's inline one
./gradlew :feature:wishlist:compileDebugKotlin --console=plain -q

# No new tests: nothing here is reachable from a JVM test. The suites that exist must stay green.
./gradlew :feature:search:testDebugUnitTest :feature:wishlist:testDebugUnitTest --console=plain -q

# The change spans modules, so finish with:
./gradlew :app:assembleDebug
./gradlew test
```

**Expected**: everything green. Nothing in the existing tests should change.

## 2. Test data

- A wishlist with **enough games to scroll** (10 or more is plenty in either view): "Test list TEST" works.
- A wishlist with **one game** (fits on one screen).
- An **empty** wishlist.
- A **non-default** wishlist, to trigger the snackbar with "Set as default".

## 3. Manual scenarios

| # | Steps | Expected | Spec |
|---|-------|----------|------|
| B1 | Open the long wishlist (list view). Don't scroll. | No button. | FR-002, US1-4 |
| B2 | Scroll down a little, until the header and the filter/view row are both off screen. | The up-arrow button animates in, bottom-right, with the same look as Search's. | FR-001, FR-005, US1-1 |
| B3 | Scroll back up until the row is partly visible again. | The button hides again. | FR-004 |
| B4 | Scroll far down, then tap the button. | A smooth scroll to the very top; the header is fully visible; the button hides when it arrives. | FR-003, FR-004, SC-003, US1-2/3 |
| B5 | Switch to grid view (toggle) while scrolled down, then scroll and tap the button. | Same appearance point, same result in grid view. | FR-006, US2-4 |
| B6 | Open the one-game wishlist. | The content fits the screen: no button, ever. | US2-1 |
| B7 | Open the empty wishlist; then apply a filter that matches nothing on the long one (change the only game of a status first). | No button in either state. | FR-002, US2-2 |
| B8 | Scroll to the very bottom in grid view. | The last card is fully visible and tappable, with space below it clear of the button. | FR-007, US2-3 |
| B9 | On a non-default wishlist, scroll down so the button shows, then tap "Set as default" in the top bar. | The snackbar and the button are both visible, the snackbar above the button, neither hiding the other's controls. | FR-007, US2-5 |
| B10 | Scroll down; in list view swipe a row open; then tap the button. | Scrolls to the top; nothing is removed. | FR-009 |
| B11 | Scroll down; long-press a card (grid) to open the remove dialog. | The button sits under the dialog's scrim and can't be tapped through it. | Edge cases |
| B12 | While the scroll-to-top animation runs, drag the list. | Your drag takes over and the scroll stops where you leave it. | Edge cases |
| B13 | Scroll down; rotate the device. | The button follows the actual scroll position afterwards; it stays clear of the system bars. | Edge cases |
| B14 | Scroll down, open a game, press back. | The button reflects the restored position (shown if still scrolled down). | Edge cases |
| B15 | Turn TalkBack on and focus the button. | It is announced as "Scroll to top", as a button. | FR-008 |
| B16 | **Search regression**: scroll the Search results or the Discover feed down. | The button still appears at the same point, looks the same, and scrolls to the top (also resetting the collapsing search bar). | R1, R3 |
| B17 | A wishlist of 300+ games: fling-scroll in grid view. | No visible stutter when the button shows and hides. | SC-005 |
| B18 | **Search lists**: scroll the Search results to the very bottom, then the Discover feed to the very bottom. | The last item is fully visible with space below it, clear of the button. | R7 (follow-up) |
