# Feature Specification: Wishlist Back-to-Top Button

**Feature Branch**: `008-wishlist-back-to-top`

**Created**: 2026-10-06

**Status**: Draft

**Input**: User description: "we should include a \"back to top\" button like in search screen."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Jump back to the top of a long wishlist (Priority: P1)

A user scrolls down a wishlist with many games, past several status sections. They want to get back to the
list's header and first section (to edit the list, check the game count, or switch view) without dragging
their thumb up through dozens of games. Once they have scrolled a meaningful distance, a round up-arrow
button appears in the bottom corner, the same one they know from the Search screen. They tap it and the
screen scrolls smoothly back to the very top.

**Why this priority**: This is the whole feature. Wishlists can grow to hundreds of games, and the only way
back to the top today is repeated flicking. Search already solves this with a floating button; the
wishlist detail screen should behave the same way.

**Independent Test**: Open a wishlist with enough games to need several screens of scrolling, scroll down
until the button appears, tap it, and confirm the screen ends at the very top with the list header fully
visible.

**Acceptance Scenarios**:

1. **Given** a wishlist long enough to scroll, **When** the user scrolls down until the list header and
   the block right below it (the filter/view row; the first status header on a screen without that
   row) are out of view, **Then** a floating up-arrow button appears in the bottom corner.
2. **Given** the button is visible, **When** the user taps it, **Then** the screen scrolls smoothly to the
   top and the list header is fully visible.
3. **Given** the user has just tapped the button and the screen has reached the top, **When** the scroll
   finishes, **Then** the button disappears.
4. **Given** the user is at or near the top of the wishlist, **When** they look at the screen, **Then** no
   button is shown.

---

### User Story 2 - The button stays out of the way (Priority: P2)

The button must not get in the way of the screen's other content or flows: it should not appear on lists
that do not need it, it should not hide the content it overlays, and it should behave the same way in
whichever layout the wishlist is currently using.

**Why this priority**: A floating control that appears on short lists, covers the last game, or only works
in one layout would be a regression in polish. It is secondary to having the button at all.

**Independent Test**: Open a short wishlist (a handful of games) and confirm the button never appears;
open a long one in each available layout and confirm it appears and works in each; scroll to the bottom
and confirm the last game can still be fully seen and tapped.

**Acceptance Scenarios**:

1. **Given** a wishlist short enough to fit on one screen, **When** the user opens it, **Then** the button
   never appears.
2. **Given** an empty or loading wishlist, **When** the screen is shown, **Then** no button is shown.
3. **Given** a long wishlist, **When** the user scrolls to the very bottom, **Then** the last game is fully
   visible and tappable and is not permanently covered by the button.
4. **Given** the wishlist can be shown in more than one layout (for example list and grid), **When** the
   user scrolls in any of them, **Then** the button appears and returns to the top in each.
5. **Given** the user has scrolled down and the snackbar is shown (for example after a failed action),
   **When** both are on screen, **Then** neither hides the other's controls.

---

### Edge Cases

- **Revealed swipe row**: if a game row has its remove action revealed when the user taps the button, the
  screen scrolls to the top and nothing is removed.
- **Open dialog or sheet**: the button is not tappable while a dialog or bottom sheet (remove
  confirmation, delete list, edit list) is open.
- **Tapping repeatedly or scrolling mid-animation**: tapping again during the scroll has no extra effect;
  if the user touches the list while it is scrolling up, their gesture takes over and the scroll stops
  where they leave it.
- **Games added or removed while scrolled down**: if the list shrinks until the button's threshold is no
  longer met, the button disappears; it never lingers on a list that is back at the top.
- **Leaving and returning**: after navigating to a game's detail and coming back, the scroll position is
  whatever the screen restores, and the button reflects that position (visible if scrolled down, hidden
  otherwise).
- **Rotation / window resize**: the button's visibility follows the actual scroll position after the
  change, and it stays within the screen's safe area, clear of system bars.
- **Accessibility**: the button has a spoken label equivalent to Search's, is reachable by assistive
  technology and meets the minimum touch target size.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The wishlist detail screen MUST show a floating "back to top" button once the first two
  blocks of its content have scrolled out of view: the list header and the block right below it, which
  is the filter/view row (spec 007) or, on a screen without that row, the first status header. This is
  the same point, in item terms, at which Search shows its button: the first visible item is beyond the
  second.
- **FR-002**: The button MUST NOT be shown when the screen is at or near the top, when the list is empty,
  or while it is loading.
- **FR-003**: Tapping the button MUST smoothly scroll the content to the very top, so the list header is
  fully visible.
- **FR-004**: The button MUST hide itself once the user is back at the top, whether they got there via
  the button or by scrolling.
- **FR-005**: The button MUST look and behave like the Search screen's: the same up-arrow icon, the same
  floating style, the same corner, and the same appear/disappear animation, so the app feels consistent.
- **FR-006**: The button MUST work in every layout the wishlist can be displayed in, including a possible
  grid layout, with the same threshold and the same result.
- **FR-007**: The button MUST NOT overlap or block the system bars, the snackbar, or the last game of a
  fully scrolled list.
- **FR-008**: The button MUST have an accessible label (reusing the one Search uses, in every supported
  language) and meet the minimum touch target size.
- **FR-009**: Scrolling to the top MUST NOT change the list's content, its grouping, its selected layout,
  or trigger any removal, navigation or edit.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user on a wishlist of 200 games can get from the bottom back to the top with one tap in
  under 2 seconds.
- **SC-002**: In 100% of wishlists long enough to scroll, the button appears once the list header and the
  block right below it (see FR-001) are out of view, and in 0% of wishlists that fit on one screen.
- **SC-003**: After tapping the button, the list header is fully visible in 100% of attempts, regardless of
  how far down the user was.
- **SC-004**: A first-time user finds and uses the button without instructions in under 10 seconds, because
  it behaves like the one they already know from Search.
- **SC-005**: Showing and hiding the button causes no visible stutter while scrolling a 500-game wishlist
  on a mid-range device.

## Assumptions

- "The search screen" refers to the floating up-arrow button on Search; this spec asks for the same button
  design and behavior on the **wishlist detail screen**. Other long screens (Radar, Lists overview) are
  out of scope.
- The appearance threshold and the animation are the same as in Search rather than being newly
  designed; the visual style is whatever Search uses today.
- The wishlist detail screen does not currently have a floating action button, so the bottom corner is
  free for this one.
- The feature is independent of the wishlist grid-view toggle (spec 007): it applies to the list layout
  today and to the grid layout as soon as that exists, using one shared behavior.
- The existing scroll-to-top label text is reused; no new copy is required unless a language is missing
  it.
- The button is a pure convenience: it is not remembered across sessions and has no setting.
