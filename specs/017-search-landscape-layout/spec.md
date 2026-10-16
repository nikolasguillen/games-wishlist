# Feature Specification: Search Landscape Layout

**Feature Branch**: `017-search-landscape-layout`

**Created**: 2026-10-16

**Status**: Draft

**Input**: User description: "optimize search section landscape layout. Examples in landscape: bottom bar should be on the left side of the screen, vertically. Search grid should have more than 2 items per row."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Navigate from a side bar in landscape (Priority: P1)

A user holds their phone sideways while browsing the app. Today the navigation bar stays across the bottom
of the screen, where it eats into the little vertical space a landscape screen has. With this feature, in
landscape the navigation moves to the left edge of the screen as a vertical bar, and the Search content
uses the full height to the right of it. The user can still jump between Search, Radar and Lists from it
exactly as they do from the bottom bar in portrait.

**Why this priority**: Vertical space is the scarcest thing in landscape, and the bottom bar takes a
fixed slice of it on every top-level screen. The side bar is the change the user asked for first, and it
is what makes everything else in the landscape layout fit. It is independently valuable: even with the
grid unchanged, the screen already feels native to landscape.

**Independent Test**: Open Search, rotate the device to landscape, and confirm the navigation is a
vertical bar on the left with all three destinations; tap Radar, then Lists, then Search, and confirm each
opens and the matching destination shows as selected. Rotate back to portrait and confirm the bar returns
to the bottom.

**Acceptance Scenarios**:

1. **Given** the user is on a top-level screen (Search, Radar or Lists) in portrait, **When** the device is
   rotated to landscape, **Then** the navigation bar is shown vertically along the left edge of the
   screen, not along the bottom, and the screen content fills the rest of the width and the full height.
2. **Given** the side navigation bar is shown, **When** the user taps a destination, **Then** the app
   navigates there exactly as the bottom bar does in portrait, and that destination is marked as selected.
3. **Given** the side navigation bar is shown, **When** the user rotates back to portrait, **Then** the
   bar returns to the bottom of the screen with the same destination selected.
4. **Given** the user is on a screen that shows no navigation bar in portrait (a game's detail, the
   welcome flow), **When** the device is rotated to landscape, **Then** no navigation bar appears there
   either.
5. **Given** the user is in landscape, **When** they navigate from a top-level screen into a game's detail
   and back, **Then** the side bar disappears on the detail screen and is back, in place, on return.

---

### User Story 2 - See more games per row in landscape (Priority: P1)

A user in landscape searches for a game, or opens a "see all" shelf, and gets the results grid. Today it
shows two cards per row, which on a wide screen means two oversized cards and a lot of scrolling. With this
feature, in landscape the grid shows more than two cards per row, so the cards stay about the size they
are in portrait and more of the results are visible at once.

**Why this priority**: This is the second change the user named explicitly and it is the visible payoff of
the wider screen. It is tested independently of the side bar: it only needs a results grid and a rotation.

**Independent Test**: Run a search that returns many results, rotate to landscape, and confirm each row
holds more than two game cards, none of them stretched or cut off, then scroll through and tap a card to
open its game. Rotate back to portrait and confirm the grid returns to two per row.

**Acceptance Scenarios**:

1. **Given** search results are shown in portrait with two cards per row, **When** the device is rotated
   to landscape, **Then** the grid re-flows to more than two cards per row.
2. **Given** results are shown in landscape, **When** the user compares a card to the same card in
   portrait, **Then** its size is similar, not stretched to fill the wider row.
3. **Given** results are shown in landscape, **When** the user scrolls to the end of a result list whose
   length does not fill the last row, **Then** the last row shows its cards aligned like every other row,
   without stretched or misplaced cards.
4. **Given** the results grid has a row of active filter chips above it, **When** the device is rotated to
   landscape, **Then** the chips still span the full width above the grid and the grid sits below them.
5. **Given** the user opens a "see all" shelf or a genre browse, **When** the device is in landscape,
   **Then** that grid uses the same multi-column layout as search results.
6. **Given** the user taps or long-presses a card in the landscape grid, **When** the action fires,
   **Then** it behaves as in portrait (open the game; choose a list).

---

### User Story 3 - The rest of Search fits the landscape screen (Priority: P2)

A user in landscape on the rest of the Search section (the Discover feed shown before a search, the search
bar and its suggestions, the empty, loading and error states, and the filter and sort sheets) finds
everything usable and sensibly laid out: nothing is cut off behind the side bar or the screen's notch,
nothing is stretched edge to edge in a way that looks wrong, and rotating never loses what they were doing.

**Why this priority**: The two requested changes carry the feature, but a landscape layout that leaves
the Discover feed or the search bar looking broken would undermine them. This story is the pass that makes
the whole Search section consistent, and it can be tested screen by screen.

**Independent Test**: In landscape, visit each Search state in turn (Discover feed, search bar focused with
history, results, no results, loading, error, filter sheet, sort sheet) and confirm each is fully
visible, legible and operable with no clipped content.

**Acceptance Scenarios**:

1. **Given** the Discover feed is shown in landscape, **When** the user scrolls it, **Then** every
   section is reachable and its content is fully visible, and the featured game at the top does not take
   the whole visible height, so the start of the next section can be seen without scrolling.
2. **Given** the search bar is expanded in landscape, **When** suggestions and history are shown, **Then**
   they sit beside the side bar rather than under it, and can all be reached.
3. **Given** the empty, loading or error state is shown in landscape, **When** the user looks at it,
   **Then** its message and actions are fully visible and centered in the space to the right of the side bar.
4. **Given** the filter or sort sheet is opened in landscape, **When** it is shown, **Then** all of its
   options can be reached and applied, and it can be dismissed.
5. **Given** the device has a camera cutout or rounded corners on the left, **When** the side bar is
   shown, **Then** none of its icons or labels are hidden behind them.
6. **Given** the user has typed a query, scrolled a long result list, or opened a filter sheet,
   **When** the device is rotated, **Then** the query, the scroll position and the open sheet are all
   still there afterwards.

---

### Edge Cases

- A very short landscape screen (small phone): the side bar must still show all three destinations with
  their icons fully reachable, even if that means labels are shortened or the bar scrolls.
- A wide window that is not a rotated phone (a tablet, a foldable open flat, a resizable window): the
  layout follows the shape of the window, not the physical device, so a window wider than it is tall gets
  the side bar and the multi-column grid.
- A split of the screen where the window is narrow and tall: it keeps the portrait layout.
- Rotating while a search is loading, while the search bar is expanded or while the keyboard is open: the
  state survives and nothing is dropped or restarted.
- Rotating while the bottom bar is animating in or out (navigating to or from a game's detail): the bar
  ends in the right place for the new orientation, with no leftover gap where the old bar was.
- A result list with fewer games than a full row, or a single game: the cards keep their normal size and
  are not stretched to fill the row.
- Right-to-left layouts and the opposite landscape direction (rotated the other way): the side bar sits on
  the edge the platform treats as the start of the screen, and is not placed under the camera cutout.
- Extreme text sizes or display zoom: labels may wrap or shorten, but destinations remain tappable.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: In landscape, the app MUST show its top-level navigation (Search, Radar, Lists) as a vertical
  bar along the left edge of the screen instead of a horizontal bar along the bottom.
- **FR-002**: In portrait, the navigation MUST remain the horizontal bottom bar, unchanged.
- **FR-003**: The side navigation bar MUST offer the same destinations, in the same order, with the same
  icons, labels, selected state and navigation behavior as the bottom bar.
- **FR-004**: The side navigation bar MUST appear on exactly the screens where the bottom bar appears
  today, and MUST NOT appear on screens that show no bar today (game detail, welcome flow).
- **FR-005**: In landscape, screen content MUST use the full screen height and the width not taken by the
  side bar; no area where the bottom bar would have been may be left empty.
- **FR-006**: The side bar MUST NOT be hidden or clipped by the camera cutout, rounded corners or system
  bars, and the content beside it MUST NOT be hidden behind it.
- **FR-007**: The search results grid MUST show more than two game cards per row in landscape, and
  MUST keep showing two per row in portrait.
- **FR-008**: In landscape, game cards in the results grid MUST stay close to their portrait size rather
  than stretching to fill the wider row, and the number of cards per row MUST follow the available width.
- **FR-009**: Spacing between and around cards MUST be even in every row at any number of columns, and an
  incomplete last row MUST align like the rows above it.
- **FR-010**: Everything the results grid shows today MUST remain in landscape, including the active
  filter chips, the no-results placeholder and the scroll-to-top button, and card taps and long-presses
  MUST behave as in portrait.
- **FR-011**: Every grid of games that is shared with Search results (including the "see all" shelves and
  genre browse) MUST adopt the same multi-column landscape layout.
- **FR-012**: The Discover feed, the search bar with its suggestions and history, the empty/loading/error
  states, and the filter and sort sheets MUST be fully visible and operable in landscape, with no clipped
  or unreachable content.
- **FR-013**: Rotating the device MUST preserve the typed query, the search results, the scroll position,
  the expanded or collapsed search bar, any open sheet and the selected destination.
- **FR-014**: The orientation-dependent layout MUST be decided by the shape of the window (wider than tall),
  not the device type, and MUST apply on both Android and iOS.
- **FR-015**: All text the new layout shows (including the side bar's labels and descriptions) MUST be
  localized in the same way as the existing bottom bar text; no new user-facing wording is introduced.

### Key Entities *(include if feature involves data)*

No new data. The feature changes presentation only: the arrangement of the existing navigation bar and
the existing results grid.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: In landscape on a typical phone, the user sees at least 3 game cards per row in the
  results grid (versus 2 today), so a given result list takes at most two thirds as many rows to scroll
  through.
- **SC-002**: A game card in landscape is within 25% of its portrait width, so cover art keeps its
  proportions and is not visibly stretched.
- **SC-003**: In landscape, the vertical space available to Search content is the full screen height:
  the bottom bar's height is no longer subtracted from it.
- **SC-004**: A user can switch between Search, Radar and Lists in landscape in one tap each, the same as
  in portrait, with the correct destination marked as selected 100% of the time.
- **SC-005**: Rotating from portrait to landscape and back on any Search state loses none of the user's
  place: query, results, scroll position and open sheet are intact in 100% of the checks.
- **SC-006**: Across a pass through every Search state in landscape (Discover feed, search bar, results,
  empty, loading, error, filter sheet, sort sheet), no content is cut off, hidden behind the side bar or
  the camera cutout, or unreachable.
- **SC-007**: Portrait behavior is unchanged: the bottom bar and the 2-column grid look and work as they
  did before the feature.

## Assumptions

- "Search section" means the Search tab and the grids it opens (results, "see all" shelves, genre
  browse). The navigation bar, however, is one component shared by Search, Radar and Lists; moving it to
  the side only on Search would make it jump between tabs, so in landscape it moves to the side on all
  three top-level screens. The Radar and Lists screens themselves get no layout change beyond having the
  side bar and the full height.
- "Landscape" is the window being wider than it is tall. This covers a rotated phone and, as a
  consequence, wide tablet and foldable windows; no separate tablet design is part of this feature.
- The exact column count is not fixed: it follows the width so cards keep roughly their portrait size,
  with a minimum of three columns in any landscape window. Portrait keeps two.
- The side bar keeps the current look: same icons, labels, colors and selected indicator as the bottom
  bar, adapted only in orientation. Its bottom-bar slide in/out animation becomes the equivalent for the
  side bar (it appears and disappears under the same conditions).
- The wishlist detail grid and other screens that have their own two-column grid are out of scope; only
  the grid shared by Search results, "see all" shelves and genre browse changes. Making other screens
  landscape-aware is a later, separate piece of work.
- The app already allows rotation on both platforms; no change to which orientations are supported.
- Visual verification on iOS needs a Mac; shared layout code is checked on Android first and on the iOS
  simulator when one is at hand.
