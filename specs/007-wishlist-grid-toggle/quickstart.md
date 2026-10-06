# Quickstart: Validating the Wishlist Grid View Toggle

**Feature**: [spec.md](spec.md) | **Plan**: [plan.md](plan.md)

There is no CI. Validation means compiling, running the JVM suites locally, and walking the scenarios
below on a device or emulator. Commands use the Unix wrapper; on Windows use `.\gradlew.bat`.

## 1. Build and automated checks

```bash
# Fast per-module feedback while working
./gradlew :core:model:compileDebugKotlin --console=plain -q
./gradlew :core:domain:compileDebugKotlin --console=plain -q
./gradlew :core:data:compileDebugKotlin --console=plain -q
./gradlew :core:ui:compileDebugKotlin --console=plain -q
./gradlew :feature:wishlist:compileDebugKotlin --console=plain -q
./gradlew :feature:search:compileDebugKotlin --console=plain -q   # VerticalGameCard's only other caller

# Unit tests touched by this feature
./gradlew :core:data:testDebugUnitTest :feature:wishlist:testDebugUnitTest :feature:search:testDebugUnitTest --console=plain -q

# The change spans modules and adds a Hilt binding, so finish with:
./gradlew :app:assembleDebug
./gradlew test
```

**Expected**: everything green. The new tests (see [plan.md](plan.md) → Testing) pass, and every existing
`WishlistViewModelTest` case still passes after its `newViewModel()` helper gains the two new use
cases.

## 2. Test data

You need one wishlist with **at least 5 games across at least 3 statuses**, including one with no status
and an odd count in at least one status. You also need one wishlist whose games **all share one status**,
and one **empty** wishlist. Set statuses from each game's detail screen.

## 3. Manual scenarios

| # | Steps | Expected | Spec |
|---|-------|----------|------|
| Q1 | Fresh install (or clear app data). Open the mixed wishlist. | List view. The row under the header shows chips on the left, "All" selected, and a grid icon on the right. | FR-010, US2-2, US4-1 |
| Q2 | Tap the grid icon. | Two-column vertical cards, grouped under the same status headers and counts. No heart on any card. Header unchanged. | US1-1, US3-1, FR-006, R4 |
| Q3 | In grid, check a status with an odd count. | The last card keeps its width. The next status header starts on a new full-width line. | Edge "odd number" |
| Q4 | Tap a card, then go back. | The game detail opens; on return, still grid. | FR-007 |
| Q5 | Long-press a card. Cancel. Long-press again. Confirm. | The remove confirmation appears both times. Cancel keeps the game; confirm removes it. TalkBack announces the long-press action as "Remove game". | FR-008, R5 |
| Q6 | Tap a status chip, then toggle list and grid. | Only that section is shown in both views. The header's game count still shows the full total. | FR-014, FR-015, FR-017 |
| Q7 | Scroll so the row is near the top edge, then tap a different chip. | No scroll jump: the row stays where it was and the filtered content starts directly below it. | Edge "changing the filter", R8 |
| Q8 | With a filter on a status that has 1 game, open that game and change its status (or long-press and remove it). Return. | The filter stays on, its chip is still selected, the toggle is still visible, and a centered "No games match this filter" message is shown below the row (in both views). No blank area. | FR-016, US4-5, SC-009 |
| Q8b | From Q8, move a game back into that status. Separately, tap "All". | The game appears under the still-active filter. Tapping "All" shows every section again. | US4-7 |
| Q8c | With a filter on, remove every game in the list. Then add one game with a different status. | The empty-list state with no row. After adding, the game is visible with "All" selected (no stale filter). | Edge "whole list emptied" |
| Q9 | Open the single-status wishlist. | No chips. The toggle sits alone at the end of the row and works. | FR-013, US4-6 |
| Q10 | In grid, open another wishlist. Then kill the app and relaunch. Rotate. | Grid every time. | FR-009, US5 |
| Q11 | Open the empty wishlist (in grid mode). | The existing empty state. No row, chips or toggle. | FR-003, US2-1 |
| Q12 | In the mixed list, in grid, remove games until it's empty. Then add one game from Search. | The empty state replaces the grid. After adding a game, grid again. | FR-012, US2-3 |
| Q13 | In list view, swipe a row to reveal Remove, then tap the toggle. | Grid shown, nothing removed. Back in list, no row is revealed. | Edge "revealed row", R9 |
| Q14 | Many statuses on a narrow device (or large font). | The chips scroll horizontally. The toggle never leaves the screen. | Edge "many statuses" |
| Q15 | TalkBack on the toggle and the chips. | Toggle: "Show as grid", state "List view" (and the reverse in grid). Chips announce selected or not selected. | FR-011, FR-018 |
| Q16 | Search screen regression: results grid. | Cards still show the heart. Tap saves, long-press opens the list chooser with the same announcement as before. | R4/R5 back-compat |
| Q17 | A wishlist with 300+ games in grid, fling scroll. | No visible stutter. | SC-005 |
