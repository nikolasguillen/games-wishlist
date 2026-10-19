# Feature Specification: Two-Pane Search and Game Detail

**Feature Branch**: `018-two-pane-search-detail`

**Created**: 2026-10-18

**Status**: Draft

**Input**: User description: "Two-pane search and game detail on large screens. On a large window (at least 840dp wide, such as a tablet, an unfolded foldable, or a landscape window of that width), opening a game's detail from the Search tab shows the detail on the right side of the screen while the search stays visible on the left. On narrower windows the app behaves exactly as it does today."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Look at a game without leaving the results (Priority: P1)

A user on a tablet (or any window at least 840dp wide) searches for a game and taps one of the results.
Today the detail replaces the whole Search screen, and they must go back to the results to look at the next
game. With this feature the detail opens on the right side of the screen while the search stays visible on
the left, with its query, filters, results and scroll position untouched. Until a game is opened, Search
takes the full width, with no empty pane and no placeholder; the two panes appear only when a detail is
opened.

**Why this priority**: This is the whole point of the feature: reading a game's detail without losing the
place in the results. It is also the smallest slice that delivers value on its own.

**Independent Test**: On a window at least 840dp wide, run a search, scroll down the results and tap a
game. Confirm the detail appears on the right, the results stay on the left at the same scroll position
with the same query and filters, and the navigation rail stays visible with Search selected. Before the tap,
confirm Search used the full width.

**Acceptance Scenarios**:

1. **Given** a window at least 840dp wide showing Search with no game opened, **When** the user looks at
   the screen, **Then** Search fills the full width next to the navigation rail, with no empty right pane
   and no placeholder.
2. **Given** the same window and a list of search results scrolled partway down, **When** the user taps a
   game, **Then** its detail appears on the right, and the search on the left keeps its query, active
   filters, results and scroll position.
3. **Given** two panes are shown, **When** the user looks at the navigation rail, **Then** it is still
   visible and the Search destination is still selected.
4. **Given** two panes are shown, **When** the user scrolls the results or the detail, **Then** each pane
   scrolls on its own without moving the other.
5. **Given** two panes are shown, **When** the user adds the open game to a list or changes its state from
   the detail, **Then** it works exactly as in the full-screen detail and the result card on the left
   reflects the change as it does today when the user comes back.

---

### User Story 2 - Compare several games quickly (Priority: P1)

With two panes shown, the user taps another game in the results and the right pane switches to that game.
They keep tapping through results to compare games, and the detail never piles up: there is always exactly
one detail, in the right pane. Opening another game from inside a detail (for example a similar game) also
swaps the right pane's content rather than adding to it.

**Why this priority**: Comparing games is the reason the user wants side-by-side panes. Without the
replace behavior, every tap would add to a history the user would then have to unwind.

**Independent Test**: With two panes shown, tap five different results in a row, then open a similar game
from inside the detail. Confirm the right pane shows the last game opened each time and a single use of the
system back action, or a single tap on the close button, closes the detail and returns Search to full width.

**Acceptance Scenarios**:

1. **Given** the detail of game A is shown in the right pane, **When** the user taps game B in the results,
   **Then** the right pane shows game B and game A's detail is gone.
2. **Given** the detail of game A is shown in the right pane, **When** the user opens a similar game from
   inside that detail, **Then** the right pane shows the similar game, still in the right pane.
3. **Given** the user has opened several games one after another in two-pane mode, **When** they use the
   system back action once, **Then** the detail closes and Search returns to full width, not to a
   previously opened game.
4. **Given** two panes are shown, **When** the user looks at the detail, **Then** it has a close (X) button
   and no back button, because the search is already visible and there is nothing to go back to.
5. **Given** two panes are shown, **When** the user taps the close button, **Then** the detail closes and Search
   returns to full width, exactly as with the system back action.

---

### User Story 3 - Rotate or resize without losing my place (Priority: P1)

A user resizes or rotates the window (rotating a tablet, folding or unfolding a foldable, resizing a
split-screen window) across the 840dp threshold. Their search and the game they have open are kept. Going
below the threshold shows the open game's detail full-screen with a working back button; going above it
shows both panes.

**Why this priority**: Window size changes without warning on the devices this feature targets. A feature
that loses the query or the open game on every fold or rotation would feel broken, so this is part of the
core experience, not polish.

**Independent Test**: In two-pane mode with a query, filters and an open game, shrink the window below
840dp: the detail goes full-screen and back returns to the same results. Grow it above 840dp again: both
panes return with the same query and the same game.

**Acceptance Scenarios**:

1. **Given** two panes are shown, **When** the window becomes narrower than 840dp, **Then** the open game's
   detail is shown full-screen with a back button, and the search query, filters and results are intact
   behind it.
2. **Given** that full-screen detail, **When** the user uses the back button or the system back action,
   **Then** the search results are shown with the same query, filters and scroll position.
3. **Given** a detail opened from Search on a window narrower than 840dp, **When** the window becomes at
   least 840dp wide, **Then** both panes are shown: the search on the left and that same game on the right.
4. **Given** a window narrower than 840dp showing Search with no game opened, **When** it becomes at least
   840dp wide, **Then** Search simply takes the wider space and no pane or placeholder appears.

---

### User Story 4 - Nothing changes on narrower windows and other entry points (Priority: P1)

A user on a phone, or in any window narrower than 840dp, sees no difference: tapping a game opens its detail
full-screen and back returns to the results. On large windows, game details opened from anywhere other than
Search (Radar, a wishlist, a release-reminder link) still open full-screen as they do today.

**Why this priority**: Not regressing the phone experience, which is the main one, is a hard requirement.
Two-pane mode must be strictly additive.

**Independent Test**: On a phone-sized window, open a game from Search and go back; open games from Radar
and a wishlist. On a large window, open a game from Radar, from a wishlist and from a release-reminder link,
and confirm each opens full-screen with no left pane.

**Acceptance Scenarios**:

1. **Given** a window narrower than 840dp, **When** the user taps a game in the search results, **Then** its
   detail opens full-screen and back returns to the results, exactly as today.
2. **Given** a window at least 840dp wide, **When** the user opens a game from Radar, a wishlist or a
   release-reminder link, **Then** the detail opens full-screen as today, with no search shown beside it.
3. **Given** a window narrower than 840dp, **When** the user opens a game, then opens a similar game from
   inside its detail, **Then** the second detail opens on top of the first and back returns through them one
   at a time, exactly as today.

---

### User Story 5 - Both panes are comfortable at half width (Priority: P2)

Each pane is only about half of a large window (roughly phone-sized once the rail is accounted for). The
user finds the search screen and the game detail laid out properly at that width: nothing cut off,
overlapping or stretched, and the search grid chooses its number of columns from the width of its own pane,
not from the width of the whole window.

**Why this priority**: The two-pane layout is only usable if both screens fit their pane, but it builds on
the P1 stories and can be verified screen by screen.

**Independent Test**: In two-pane mode on a window just above 840dp and on a much wider one, go through the
search screen (Discover feed, results grid, filter chips, suggestions, empty, loading and error states) and
the game detail (header, media, description, actions, similar games) in the half-width panes, in portrait
and in landscape.

**Acceptance Scenarios**:

1. **Given** two panes are shown, **When** the user views the search results grid in the left pane, **Then**
   its columns follow the width of that pane, with cards about the size they have in portrait and no
   stretched or cut-off card.
2. **Given** two panes are shown, **When** the user views the game detail in the right pane, **Then** all
   its content and actions are reachable and none is clipped, overlapping or hidden by the navigation rail.
3. **Given** two panes are shown on a window that is also in the landscape layout introduced by the search
   landscape feature, **When** the user views Search, **Then** the left pane follows the same rules as in
   full width, adjusted to the pane's width.
4. **Given** two panes are shown, **When** the user opens a sheet or dialog from either pane (filters, sort,
   choose a list), **Then** it is usable and does not leave the panes in a broken state.

---

### Edge Cases

- The window is at exactly 840dp: two-pane mode applies (the threshold is "at least 840dp").
- The user is in two-pane mode and closes the detail with the system back action, then taps a different game:
  the panes reappear with the new game and the search state unchanged.
- The user stacked several details on a narrow window (game, then a similar game, then another), then
  widens the window: the most recently opened game is shown in the right pane, and the system back action
  closes the detail and returns Search to full width.
- The user is in two-pane mode and switches to Radar or Lists from the rail: the other destination fills the
  full width, as it does today. Returning to Search follows the app's existing behavior when switching tabs.
- A game opened from Radar on a narrow window is open full-screen when the window becomes at least 840dp
  wide: it stays full-screen, because it was not opened from Search.
- A game's detail is loading or fails to load in the right pane: the pane shows the same loading and error
  states as the full-screen detail, and the search on the left stays usable.
- A search result tapped is the same game already shown in the right pane: the pane keeps showing it, with
  nothing stacked and no flicker or reset of its scroll position beyond what a normal open does.
- The search has no results, or is still loading, when a game is opened from the Discover feed: the left
  pane shows whatever Search shows at that moment, unchanged.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: On a window at least 840dp wide, when the user opens a game's detail from the Search tab, the
  system MUST show the detail on the right side of the screen and keep the Search screen visible on the left.
- **FR-002**: Until a game's detail is opened, the Search screen MUST take the full width of the window next
  to the navigation rail, with no empty right pane and no placeholder.
- **FR-003**: While two panes are shown, the Search screen on the left MUST keep its query, filters,
  results, loaded content and scroll position, and the user MUST be able to keep interacting with it.
- **FR-004**: Tapping a different game in the Search screen while a detail is shown MUST replace the detail
  in the right pane; the system MUST NOT stack additional details.
- **FR-005**: Opening another game from inside a game's detail (for example a similar game) while two panes
  are shown MUST replace the right pane's content.
- **FR-006**: The navigation rail MUST remain visible while two panes are shown, and the Search destination
  MUST remain selected.
- **FR-007**: While two panes are shown, the game detail MUST NOT show a back button and MUST show a close (X)
  button in its place, including while it loads and when it fails to load. The close button MUST do what the
  system back action does (FR-008). Its label MUST be a localizable string resource (FR-017).
- **FR-008**: While two panes are shown, the system back action MUST close the detail and return the Search
  screen to full width, regardless of how many games were opened one after another.
- **FR-009**: Resizing or rotating the window across the 840dp threshold MUST keep the current search (query,
  filters, results) and the open game.
- **FR-010**: When the window becomes narrower than 840dp with a detail opened from Search, the system MUST
  show that detail full-screen with a working back button, and back MUST return to the same search state.
- **FR-011**: When the window becomes at least 840dp wide with a detail opened from Search, the system MUST
  show both panes with the same search state and the same game.
- **FR-012**: On windows narrower than 840dp, behavior MUST be identical to the app before this feature:
  details open full-screen, back returns to the results, and opening a game from inside a detail stacks as
  it does today.
- **FR-013**: Game details opened from entry points other than the Search tab (Radar, a wishlist, a
  release-reminder link) MUST open full-screen on every window size, as they do today.
- **FR-014**: The game detail shown in the right pane MUST support the same content and actions as the
  full-screen detail (including adding to a list and changing its state).
- **FR-015**: The Search screen, including its landscape layout, MUST lay out correctly in a pane about half
  the window width: columns and spacing MUST follow the width of the pane the screen is in, not the width of
  the window.
- **FR-016**: The game detail MUST lay out correctly in a pane about half the window width, with no clipped,
  overlapping or unreachable content.
- **FR-017**: Any text introduced by this feature MUST be a localizable string resource, not hardcoded.
- **FR-018**: The feature MUST behave the same on Android and iOS, and MUST NOT change the layout or behavior
  of Radar, Lists or Wishlist.

### Key Entities

- **Search flow**: The Search tab and everything the user opens from it (results, the Discover feed, "see
  all" shelves, genre browsing). Only game details opened from here take part in two-pane mode.
- **Open game (detail)**: The single game currently shown in the detail, either in the right pane or
  full-screen. At most one is shown at a time in two-pane mode.
- **Window width class**: Whether the window is at least 840dp wide (two-pane eligible) or narrower
  (unchanged behavior). It can change at any time through rotation, folding or resizing.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: On a window at least 840dp wide, a user can view the detail of 5 different games from one set
  of search results with zero back actions between them.
- **SC-002**: Opening a game in two-pane mode preserves the search query, filters, results and scroll position
  in 100% of cases.
- **SC-003**: Across 20 consecutive rotations or resizes over the 840dp threshold, the search state and the
  open game are preserved every time, and no step shows an empty pane or a duplicated detail.
- **SC-004**: Using the system back action once from two-pane mode always returns Search to full width,
  however many games were opened beforehand.
- **SC-005**: On windows narrower than 840dp, every navigation flow involving Search and game details behaves
  identically to before the feature, with zero visual or behavioral differences.
- **SC-006**: Game details opened from Radar, a wishlist or a release-reminder link open full-screen on every
  window size, in 100% of cases.
- **SC-007**: With two panes on windows from 840dp up to a very wide tablet, every screen of the Search flow
  and the game detail can be fully used with no clipped, overlapping or unreachable content.

## Assumptions

- The 840dp threshold applies to the width of the app's window, not the physical screen, so split-screen and
  freeform windows follow the same rule as full-screen ones.
- The two panes share the space next to the navigation rail in roughly equal halves; the exact ratio is a
  design detail for the plan, as long as FR-015 and FR-016 hold.
- "Search flow" covers every game opened from the Search tab, including the Discover feed, "see all" shelves
  and genre browsing, not only the results of a typed query.
- Game details opened from a release-reminder link while the Search tab is already showing still open
  full-screen, because the link is not part of the Search flow.
- Switching tabs from the rail while two panes are shown follows the app's existing tab-switching behavior;
  this feature does not add a rule for it.
- The result card for the open game is not specially highlighted in the list; that is a change to search
  content and is out of scope.
- Out of scope: two-pane layouts for Radar, Lists or Wishlist; changing the search or detail content itself
  beyond making it fit its pane; any change to phone layouts.
- Project constraints carried into planning: shared code for Android and iOS only; navigation logic stays in
  the shared app module and feature modules stay unaware of it; all user-facing text comes from string
  resources; any new dependency or module edge must be called out explicitly in the plan.
- Verifying the layout (including the landscape layout from spec 017) in a half-width pane is part of this
  feature's acceptance, and the iOS side can only be verified on a Mac.
