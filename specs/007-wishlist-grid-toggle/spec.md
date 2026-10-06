# Feature Specification: Wishlist Grid View Toggle

**Feature Branch**: `007-wishlist-grid-toggle`

**Created**: 2026-10-06

**Status**: Draft

**Input**: User description: "in wishlist detail screen we should add a toggle that let's the user choose between the current list view and an alternative grid view (vertical game card, same as search screen). The toggle should be visible only when the list is not empty. We should discuss about where we should position the button and how to handle the status split in grid mode"

## Clarifications

### Session 2026-10-06

- Q: Where should the view toggle be positioned? → A: In the content, on a slim row between the list
  header and the first status section (not in the top bar, not pinned).
- Q: How should the status split be handled in grid mode? → A: Keep the sections: a full-width status
  header with its count, followed by a two-column grid of that status's games.
- Q: How does the user remove a game in grid mode? → A: Long-press the card to open the same removal
  confirmation the list view uses.
- Q: Is the view choice global or per wishlist? → A: One app-wide choice, persisted across relaunches.
- Q: What should sit on the left of the row that holds the view toggle? → A: Status filter chips, so the
  row is a filter-and-view bar rather than a lone button. (This brings status filtering into scope.)

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Browse a wishlist as a grid of covers (Priority: P1)

A user opens one of their wishlists and, instead of the compact rows they see today, wants to browse it
the way they browse Search results: large vertical cards with cover art, two to a row. They tap a view
toggle, the games re-lay out as a grid, and they can tap any card to open that game, exactly as they
would from a row. They can tap the toggle again to return to the row layout.

**Why this priority**: This is the whole feature. Rows are dense and good for scanning titles, but a
cover-led grid is better for "what do I feel like playing?" browsing, and it matches how the user
already sees games in Search. Every other story refines this one.

**Independent Test**: Open a wishlist containing several games, switch to grid view, confirm the games
appear as vertical cards two per row, tap a card and land on that game's detail screen, go back, switch
to list view and confirm the original row layout returns with the same games.

**Acceptance Scenarios**:

1. **Given** a wishlist with at least one game shown in list view, **When** the user activates the view
   toggle, **Then** the games are shown as vertical game cards in a grid, using the same card design as
   the Search results.
2. **Given** a wishlist shown in grid view, **When** the user activates the view toggle again, **Then**
   the original list layout is shown with the same games and the same grouping.
3. **Given** a wishlist shown in grid view, **When** the user taps a game card, **Then** that game's
   detail screen opens, and going back returns to the wishlist still in grid view.
4. **Given** a wishlist shown in either view, **When** the user scrolls, **Then** the list header
   (name, description, icon, cover, game count) scrolls with the content in both views, as it does today.

---

### User Story 2 - The toggle only appears when there is something to lay out (Priority: P1)

The view toggle is only meaningful when the list has games. A user who opens a brand-new or emptied list
sees the existing empty state with no toggle; as soon as the first game is added the toggle appears, and
if the last game is removed it disappears again.

**Why this priority**: An always-visible toggle on an empty list is a control that does nothing and
clutters the empty state. It is stated explicitly as a requirement, so it is part of the minimum shippable
feature.

**Independent Test**: Open an empty wishlist and confirm no toggle is visible; add a game from Search and
return, confirm the toggle is visible; remove that game and confirm the toggle is gone again.

**Acceptance Scenarios**:

1. **Given** an empty wishlist, **When** the user opens it, **Then** the empty state is shown and no view
   toggle is visible.
2. **Given** a wishlist with at least one game, **When** the user opens it, **Then** the view toggle is
   visible.
3. **Given** a wishlist whose last game has just been removed while in grid view, **When** the list
   becomes empty, **Then** the empty state is shown and the toggle is hidden.
4. **Given** the list is still loading, **When** the loading page is shown, **Then** no view toggle is
   visible.

---

### User Story 3 - Games stay grouped by status in grid view (Priority: P2)

Today a wishlist is split into status sections ("Playing", "Want to buy", "No status", …), each with a
header and a count. The user wants the grid view to keep helping them tell games apart by status, in a
way that suits a grid.

**Why this priority**: Without a decision here the grid would either lose information the list view
shows or look inconsistent between the two modes. It is secondary to having a grid at all, but it
determines how the grid actually looks, so it must be settled before planning.

**Independent Test**: Open a wishlist with games in at least two different statuses, switch to grid view,
and confirm the status of each game can still be told apart in the way chosen for this feature.

**Acceptance Scenarios**:

1. **Given** a wishlist with games in several statuses, **When** the user switches to grid view,
   **Then** each status keeps its own section: a full-width header showing the status label and game
   count, followed by that status's games as a two-column grid of vertical cards.
2. **Given** a wishlist where every game has the same status, **When** the user switches to grid view,
   **Then** the grid is shown without any visual artefact from the grouping (no empty sections, no
   orphaned header).

---

### User Story 4 - Filter the wishlist by status (Priority: P2)

On the left of the same row that holds the view toggle, the user sees a row of status chips ("All",
"Playing", "Want to buy", …). Tapping a status narrows the wishlist to the games with that status, in
either view, so a user with a long list can look at just what they are playing right now. Tapping "All"
(or clearing the selection) shows everything again.

**Why this priority**: It gives the row a purpose beyond hosting one button and pairs naturally with the
status sections, but the grid toggle works without it. It is a distinct, independently testable slice.

**Independent Test**: Open a wishlist with games in at least three different statuses, tap one status chip
and confirm only that status's games are shown, switch between list and grid and confirm the filter
holds, then clear the filter and confirm all games return.

**Acceptance Scenarios**:

1. **Given** a wishlist with games in several statuses, **When** the user opens it, **Then** the row shows
   the view toggle on the right and one chip per status that has at least one game, plus "All", on the
   left, with "All" selected.
2. **Given** the "All" chip is selected, **When** the user taps a status chip, **Then** only that
   status's section is shown, with its header and count, in the current view.
3. **Given** a status filter is active, **When** the user switches between list and grid view, **Then**
   the filter stays applied.
4. **Given** a status filter is active, **When** the user taps "All", **Then** every status section is
   shown again.
5. **Given** a status filter is active and the last game of that status is removed or changes status,
   **When** the list updates, **Then** the filter returns to "All" and that status's chip disappears.
6. **Given** a wishlist whose games all share one status (or have no status at all), **When** the user
   opens it, **Then** no status chips are shown, because there is nothing to filter, and the view toggle
   remains on the row.

---

### User Story 5 - The chosen view is remembered (Priority: P3)

A user who prefers the grid should not have to re-select it every time they open a wishlist. The chosen
view stays in effect when they leave the screen, open another wishlist, rotate the device, or relaunch
the app.

**Why this priority**: A convenience on top of a working toggle. The feature is still useful if the
choice reset on every visit, but it would feel like a chore for anyone who prefers the grid.

**Independent Test**: Switch a wishlist to grid view, leave the screen, reopen the same wishlist and then
a different one, and confirm both open in grid view; fully close and relaunch the app and confirm the
choice persists.

**Acceptance Scenarios**:

1. **Given** the user selected grid view, **When** they navigate away and reopen the same wishlist,
   **Then** it opens in grid view.
2. **Given** the user selected grid view in one wishlist, **When** they open a different wishlist,
   **Then** it also opens in grid view.
3. **Given** the user selected grid view, **When** the device is rotated or the app is relaunched,
   **Then** grid view is still selected.
4. **Given** a fresh install, **When** the user opens a wishlist, **Then** it opens in list view.

---

### Edge Cases

- **Last game removed while in grid view**: the list becomes empty, the empty state replaces the grid and
  the toggle disappears (User Story 2). The stored view choice is not reset, so adding a game later
  brings the grid back.
- **Removing a game**: removal remains possible in grid view, with the same confirmation step the list
  view has today. List rows reveal a remove action by swiping; in the grid, a long-press on a card opens
  that same confirmation. Cancelling the confirmation leaves the game in place.
- **Many statuses on a narrow screen**: the chips scroll horizontally; the view toggle stays pinned at the
  end of the row and is never pushed off-screen.
- **Changing the filter while scrolled down**: the content returns to the top of the filtered result so
  the user does not land in the middle of a shorter list.
- **Filter matches nothing**: cannot normally happen, since chips exist only for statuses that have games;
  if the last matching game disappears the filter resets to "All" (User Story 4, scenario 5).
- **Odd number of games in a status**: the last row of a section may hold a single card, which keeps the
  same width as the others rather than stretching; the next status header always starts on a new
  full-width row.
- **Open "revealed" row when switching views**: if a row's remove action is revealed in list view when
  the user toggles to grid, it is dismissed; nothing is removed by switching views.
- **Very long game names**: cards truncate titles the same way Search cards do and never break the grid
  alignment.
- **Games added or removed elsewhere while the screen is open**: the visible layout updates in place
  without changing the selected view.
- **Large lists**: a wishlist with hundreds of games scrolls smoothly in grid view; cards and covers are
  only produced as they come into view.
- **Narrow or wide screens and landscape**: the grid stays readable; the number of columns stays at two
  unless planning decides otherwise for wider windows.
- **Accessibility**: the toggle has a spoken label that says which view it will switch to, and the
  current view is announced.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The wishlist detail screen MUST offer a control that switches between the existing list
  view and a grid view of the same games.
- **FR-002**: Grid view MUST present each game as the same vertical game card used by the Search results
  screen, arranged in two columns.
- **FR-003**: The view control MUST be visible only when the wishlist contains at least one game, and
  MUST NOT be visible while the list is loading or empty.
- **FR-004**: Switching views MUST NOT change which games are shown, their order, or the list header
  (name, description, icon, cover, game count).
- **FR-005**: The view control MUST be placed in the screen content, on a slim row between the list
  header and the first status section, pinned to the end of the row, with the status filter chips
  (FR-013) occupying the rest of the row. The row scrolls with the content and MUST NOT be added to the
  top bar.
- **FR-006**: In grid view, games MUST remain grouped by status: each status MUST have a full-width
  header (same label and count as in list view) followed by that status's games in a two-column grid.
  Statuses with no games MUST NOT produce a header.
- **FR-007**: Tapping a game card in grid view MUST open that game's detail screen, and returning MUST
  restore the wishlist in the same view.
- **FR-008**: The user MUST be able to remove a game from the wishlist while in grid view by long-pressing
  its card, which opens the same confirmation step as in list view.
- **FR-009**: The selected view MUST persist across leaving the screen, opening other wishlists, device
  rotation and app relaunch, and MUST apply to every wishlist rather than being stored per list.
- **FR-010**: On first use (no stored choice), the wishlist detail screen MUST open in list view.
- **FR-011**: The view control MUST indicate the currently active view and expose an accessible label
  describing the action it performs, with all user-visible text localized like the rest of the app.
- **FR-012**: When the wishlist becomes empty (last game removed), the screen MUST show the existing empty
  state and hide the view control, without discarding the stored view choice.
- **FR-013**: The row MUST show an "All" chip plus one chip per status that has at least one game in the
  list (including a chip for games with no status when there are any), in the same order as the status
  sections. The chip row MUST be hidden when the list contains fewer than two distinct statuses.
- **FR-014**: Selecting a status chip MUST show only that status's section (header, count and games) in
  the current view; selecting "All" MUST show every section. Exactly one chip is selected at a time, and
  "All" is selected by default.
- **FR-015**: The active filter MUST apply identically in list and grid view and MUST survive switching
  between them.
- **FR-016**: If the filtered status stops having games (removed or re-statused), the filter MUST reset
  to "All" automatically.
- **FR-017**: Changing the filter MUST NOT change the stored view choice, the game count in the header,
  or any game's data; it only changes which games are displayed.
- **FR-018**: The chips MUST be selectable by assistive technology, announce their selected state, and
  use the same localized status labels as the section headers.

### Key Entities *(include if feature involves data)*

- **Wishlist view mode**: The user's chosen way of presenting a wishlist's games — list or grid. A single
  app-wide preference, not attached to any individual wishlist, defaulting to list.
- **Status filter**: The currently selected status chip, or "All". Transient screen state: not stored per
  wishlist, not remembered across visits (see Assumptions), and independent of the view mode.
- **Wishlist game group**: The existing grouping of a wishlist's games by personal status (including a
  "no status" group), each with a label and a count. Its content is identical in both views; only its
  presentation (rows vs. a two-column grid under the same header) differs.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can switch a wishlist between list and grid view with a single tap, and the new
  layout is fully shown within 1 second on a typical device.
- **SC-002**: In 100% of wishlists with at least one game the toggle is visible, and in 100% of empty or
  loading wishlists it is not.
- **SC-003**: Every game reachable in list view is reachable in grid view: opening its detail and removing
  it from the list (swipe in list view, long-press in grid view) both work.
- **SC-004**: After choosing grid view, 100% of subsequent wishlist openings (including after an app
  relaunch) start in grid view until the user switches back.
- **SC-005**: A wishlist of 500 games scrolls in grid view without visible stutter on a mid-range device.
- **SC-006**: A first-time tester can find and use the toggle without instructions in under 10 seconds.
- **SC-007**: A user can narrow a wishlist to a single status with one tap and see the filtered result
  within 1 second, in both views.
- **SC-008**: With a status filter active, 100% of the visible games have that status and 100% of the
  games with that status are visible.

## Assumptions

- The grid uses the exact card design Search uses today; no new card variant is introduced. If that card
  carries a save-to-wishlist control, how it behaves inside a wishlist is a planning detail, but it MUST
  NOT let the user end up in an inconsistent state (a game shown as saved while it has been removed from
  this list).
- The view choice is a single app-wide preference rather than per-wishlist; this keeps the control
  simple and matches the expectation that "I like grids" is a personal preference, not a list property.
- The default is list view, so existing users see no change until they opt in.
- Only the wishlist detail screen is in scope. Other places that list games (Search, Radar, the Lists
  overview) are unchanged.
- Filtering by status is part of this feature (it fills the left side of the toggle row); sorting is
  not. The order inside each group is whatever the list view uses today.
- The status filter is a single selection and is not remembered: every visit to a wishlist starts on
  "All", and the filter is not stored per list. Only the list/grid choice persists.
- Two grid columns is the default, matching Search; adaptive column counts for tablets are out of scope
  for this feature.
- Switching views is instant and unanimated beyond a standard content transition; no custom animation is
  required.
