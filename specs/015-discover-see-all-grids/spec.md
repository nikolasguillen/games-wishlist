# Feature Specification: See-All Grids for Discover Rows

**Feature Branch**: `015-discover-see-all-grids`

**Created**: 2026-10-13

**Status**: Draft

**Input**: User description: "See-all grids for Discover rows. Today every row on the Discover feed (the personalized shelves such as 'Because you like RPG' / 'More from <studio>', 'Most anticipated' and 'Popular this month') renders every game its lane holds in one horizontal scroll. 'Popular this month' in particular holds dozens of games, so the row is a long swipe and the shelves below it are hard to reach. Cap each row at 6 games, and add an arrow button at the end of the row's title row that opens the full list of that row as a grid."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Shorter rows, so every shelf is within reach (Priority: P1)

A user opens Search and lands on the Discover feed. Each shelf now shows at most six games, in the order it
already had, so a shelf is a short glance rather than a long swipe and the shelves below it are reachable
without scrolling past dozens of cards. The hero card above the shelves is unchanged.

**Why this priority**: This is the problem the feature exists to solve. "Popular this month" holds dozens of
games and pushes everything under the fold of its own row; capping the rows is what fixes that, with or
without the grid.

**Independent Test**: With a feed whose "Popular this month" holds well over six games, open Search and
confirm that shelf shows exactly its first six games in their usual order, that every other shelf with more
than six games does the same, that a shelf with six or fewer shows all of its games, and that the hero card
is unchanged.

**Acceptance Scenarios**:

1. **Given** a Discover shelf that holds more than six games, **When** the feed is shown, **Then** the shelf
   shows only its first six games, in the order it already has.
2. **Given** a Discover shelf that holds six or fewer games, **When** the feed is shown, **Then** the shelf
   shows all of its games.
3. **Given** the feed has personalized shelves, "Most anticipated" and "Popular this month", **When** it is
   shown, **Then** the cap applies to every one of them.
4. **Given** the feed has a hero card, **When** the feed is shown, **Then** the hero card is unchanged and is
   not counted against any shelf's six games.

---

### User Story 2 - See the whole shelf as a grid (Priority: P1)

A user sees a shelf titled "Popular this month" with an arrow at the end of its title row. They tap it. The
Discover feed gives way to a grid of every game in that shelf, on the same cards as a text search result, and
they can open a game, save it, or long-press it to pick a list exactly as they would from a search result.
The grid appears at once, with no loading spinner, because it shows games the shelf already holds.

**Why this priority**: The cap in Story 1 would hide games from the user if there were no way to see the rest.
Together the two stories are the feature; neither ships alone.

**Independent Test**: Tap the arrow on a shelf with more than six games and confirm that the grid shows
exactly the shelf's games in the shelf's order, that it appears with no loading state, that it works with the
device offline, and that opening, saving and long-pressing a card behave as in a text search result.

**Acceptance Scenarios**:

1. **Given** a shelf with more than six games, **When** the feed is shown, **Then** an arrow button appears at
   the end of the shelf's title row and the title stays at the start.
2. **Given** a shelf with six or fewer games, **When** the feed is shown, **Then** no arrow is shown and the
   title row does nothing when tapped, since the grid would show nothing the row does not.
3. **Given** a shelf with more than six games, **When** the user taps its arrow, **Then** the Discover feed is
   replaced by a grid of every game the shelf holds, in the shelf's own order, with no loading state.
4. **Given** the device is offline and the feed is on screen, **When** the user taps a shelf's arrow, **Then**
   the grid opens and shows the shelf's games, with no error.
5. **Given** the grid is showing, **When** the user taps a card, taps its save button or long-presses it,
   **Then** the game opens, is toggled in the default wishlist, or the list selector opens, respectively,
   exactly as for a text-search result.
6. **Given** the grid is showing, **When** a game is saved or unsaved from it, **Then** its saved state is
   also correct on the Discover feed once the user returns to it.
7. **Given** the "Most anticipated" shelf, whose top pick is shown as the hero, **When** the user opens its
   grid, **Then** the grid holds the same games the shelf holds, which do not include the hero.

---

### User Story 3 - Know what the grid is, and get back out (Priority: P1)

After tapping an arrow the search bar text stays empty, so the screen has to say what it is showing and give
a clear way out. The bar's placeholder shows the name of the shelf the user opened, such as "Popular this
month" or "Because you like RPG". The clear (X) button is visible even though the field is empty. Tapping X,
the back arrow, or using the system back gesture returns the user to the Discover feed, scrolled to where they
left it.

**Why this priority**: Without a label and an exit the grid is a dead end. This is the same minimum that the
genre quick-browse (spec 014) required of its grid.

**Independent Test**: Open a shelf's grid and confirm the placeholder names the shelf and the X button is
visible with an empty field; leave via X, via the back arrow and via system back, each time confirming that
the feed returns with the default placeholder, at the scroll position it had before.

**Acceptance Scenarios**:

1. **Given** a shelf grid is showing, **When** the screen is shown, **Then** the search bar text is empty and
   its placeholder reads the shelf's name.
2. **Given** a shelf grid is showing and the field is empty, **When** the screen is shown, **Then** the clear
   (X) button is visible.
3. **Given** a shelf grid is showing, **When** the user taps the clear (X) button, **Then** the Discover feed
   is shown and the default placeholder is restored.
4. **Given** a shelf grid is showing, **When** the user taps the back arrow, **Then** the same thing happens.
5. **Given** a shelf grid is showing and the search bar is collapsed, **When** the user uses the system back
   gesture, **Then** the same thing happens.
6. **Given** the user scrolled the Discover feed down before opening a grid, **When** they return from the
   grid, **Then** the feed is at the scroll position they left.
7. **Given** a shelf grid is showing, **When** the user types a query and submits it, or taps a genre chip or a
   recent search, **Then** that search or browse replaces the grid and the default placeholder (or the genre
   placeholder) is shown.
8. **Given** a shelf grid is showing, **When** the user opens a game and comes back, **Then** the grid is
   still showing, as a search result list would be.

---

### User Story 4 - Sort and filter a shelf grid consistently with the genre browse (Priority: P2)

The sort and filter sheets keep working on a shelf grid, in the same way they do for the genre browse. The
sort option called "Relevance" for a text search is called "Top picks" here too, and it means "the shelf's own
order". The other sorts, and filters by platform, genre and game type, narrow and reorder the games the shelf
holds.

**Why this priority**: The grid is usable without refinements, but offering a sort called "Relevance" with no
query to be relevant to would be wrong, and a grid that cannot be narrowed would be a regression from the
genre browse the user already knows.

**Independent Test**: Open a shelf grid, open the sort sheet and confirm the first option reads "Top picks"
and, selected, leaves the shelf in its own order; pick another sort, leave the grid, and confirm the label is
"Relevance" again with the selection untouched. Open the filter sheet and confirm it offers platforms, genres
and game types drawn from the shelf's games and that selecting some narrows the grid to matching games.

**Acceptance Scenarios**:

1. **Given** a shelf grid is showing, **When** the user opens the sort sheet, **Then** the option normally
   labelled "Relevance" is labelled "Top picks", and with it selected the grid is in the shelf's own order.
2. **Given** a shelf grid is showing and the user picks another sort, **When** they pick "Top picks" again,
   **Then** the shelf's own order returns (descending), or its reverse (ascending).
3. **Given** the user selected a sort and a direction, **When** a shelf grid starts or ends, **Then** the
   selection is unchanged; only the label of the first option changes.
4. **Given** a shelf grid is showing, **When** the user opens the filter sheet, **Then** the platforms,
   genres and game types offered are those present in the shelf's games, and selecting some narrows the grid
   to games that match all of them.
5. **Given** a shelf grid starts, **When** it appears, **Then** no filter is selected, as for any new result
   set.
6. **Given** filters leave no games, **When** the grid is shown, **Then** the same "no matches" state as a text
   search is shown, with the same option to clear the filters.

---

### User Story 5 - A shelf grid leaves no trace in history (Priority: P2)

Opening a shelf is a way of looking around, not a query the user typed. It must not appear among the recent
searches.

**Why this priority**: A correctness guard on the P1 flow, identical to the one the genre browse has.

**Independent Test**: Note the recent searches, open several shelf grids, reopen the expanded search view, and
confirm the recent searches are exactly as before.

**Acceptance Scenarios**:

1. **Given** the user opens a shelf grid, **When** they reopen the expanded search view, **Then** nothing for
   that grid appears among the recent searches.

---

### Edge Cases

- **A shelf with exactly seven games**: it shows six in the row, shows the arrow, and the grid shows all
  seven. A shelf with exactly six shows six and no arrow.
- **Shelf of one**: shows its single game and no arrow, as any shelf with six or fewer.
- **Shelf order**: the order in the row, the first six, and the order in the grid are the same order, so the
  six in the row are always the first six of the grid.
- **Several shelves with the same kind of title**: each arrow opens the grid of its own shelf, never another's
  (two personalized shelves can have similar names; the shelf is identified by its place in the feed and its
  kind, never by its displayed name).
- **Feed refreshes while a grid is open** (the user refreshed from the prompt, or a platform selection
  changed): the grid keeps showing the games of the shelf as it was when opened; it does not reshuffle under
  the user. Returning to the feed shows the refreshed feed.
- **A shelf that disappears on refresh**: the open grid is unaffected; returning shows the refreshed feed
  without it.
- **Offline**: opening and using a grid needs no network. An offline user sees the same grid an online user
  would, from the lane already on screen. Cards whose cover images were never downloaded show the same
  placeholder as elsewhere.
- **Rotating during a grid**: the grid survives rotation: the same shelf, games, placeholder, sort and filters
  are shown, without the Discover feed flashing in between.
- **Backgrounding during a grid**: returning within the same session shows the same grid. After the app
  process was ended, a relaunch starts at the Discover feed; the grid is not persisted (out of scope).
- **Typing without submitting during a grid**: the grid stays on screen until a query is submitted. If the
  user deletes what they typed, the shelf placeholder is shown again.
- **Tapping the arrow quickly twice**: one grid is shown; nothing is left in an inconsistent state.
- **Stale prompt**: the "refresh suggestions" prompt belongs to the feed; it is not shown over the grid, and
  it is still there on return if it was.
- **Accessibility**: the arrow is announced as a button whose spoken label includes the shelf's name (for
  example, "See all: Popular this month"), not as an unlabelled arrow. Its touch target is at least 48dp.
  When the whole title row is the tap target, it is announced once, not once for the title and again for the
  arrow. The arrow mirrors in right-to-left languages.
- **Long shelf names**: a long personalized title keeps the arrow visible at the end of the row; the title
  truncates rather than pushing the arrow away.
- **Both platforms**: Android and iOS show the same cap, the same arrow and the same grid behavior.

## Requirements *(mandatory)*

### Functional Requirements

**The cap**

- **FR-001**: Every Discover shelf, that is each personalized shelf, "Most anticipated" and "Popular this
  month", MUST show at most six games in its row, taken from the start of the shelf's existing order.
- **FR-002**: A shelf with six or fewer games MUST show all of them and MUST NOT show an arrow.
- **FR-003**: The hero card MUST NOT change and MUST NOT count against any shelf's six games.
- **FR-004**: The cap is a display cap only. What the feed fetches, how the lanes are cached, how shelves are
  ranked, and how a game is kept to a single shelf MUST NOT change.

**The arrow**

- **FR-005**: A shelf with more than six games MUST show an arrow button at the end of its title row, with the
  title staying at the start.
- **FR-006**: The arrow MUST have a touch target of at least 48dp and an accessible label that names the
  shelf. The label MUST be localized like the rest of the app, with the shelf's name as an argument rather
  than one hardcoded string per shelf.
- **FR-007**: Tapping the title row anywhere SHOULD act as tapping the arrow, announced to assistive
  technology as a single control. Tapping the arrow itself MUST always work.
- **FR-008**: The arrow MUST use the app's existing "forward" arrow, MUST mirror in right-to-left locales, and
  MUST NOT introduce a new icon style.

**The grid**

- **FR-009**: Tapping a shelf's arrow MUST show every game the shelf holds, as a grid in the search content
  area, replacing the Discover feed.
- **FR-010**: The grid MUST contain exactly the shelf's games in the shelf's order, and MUST NOT trigger any
  network request, a larger fetch or a loading state. It MUST open at once and work with no network.
- **FR-011**: The grid MUST use the same game cards, save button, long-press list selector, "no matches"
  state and filter and sort controls as a text-search result and the genre browse (spec 014). It reuses that
  mode and MUST NOT introduce a second grid mode.
- **FR-012**: Which shelf a grid shows MUST be decided by the shelf's identity (its kind and its place in the
  feed), never by its displayed name.
- **FR-013**: The grid MUST show the shelf as it was when the user opened it, and MUST NOT change if the
  feed refreshes underneath it.
- **FR-014**: Saving, unsaving, opening and long-pressing a game in the grid MUST behave as they do in a
  text-search result, and a game's saved state MUST be consistent between the grid and the feed.

**Search bar and exit**

- **FR-015**: During a shelf grid the search bar text MUST remain empty and its placeholder MUST show the
  shelf's name, localized the way the shelf's title is. Outside a grid, browse or search the default
  placeholder MUST be shown.
- **FR-016**: The clear (X) button MUST be visible during a shelf grid even though the field is empty.
- **FR-017**: Tapping the clear (X) button, tapping the back arrow, or the system back gesture MUST return to
  the Discover feed and restore the default placeholder.
- **FR-018**: Returning to the feed MUST restore its scroll position. *(Nice to have: if it turns out not to
  come for free from how the feed is held, the plan may defer it and record the gap.)*
- **FR-019**: Submitting a query, tapping a genre chip, or tapping a recent search during a shelf grid MUST
  replace the grid with that search or browse, and the placeholder MUST follow.
- **FR-020**: Opening a shelf grid MUST NOT write anything to the search history.

**Sort and filter**

- **FR-021**: During a shelf grid, the sort option normally labelled "Relevance" MUST read "Top picks" and,
  when selected, MUST keep the shelf's own order (descending) or reverse it (ascending). At all other times
  it MUST read "Relevance".
- **FR-022**: Starting or ending a shelf grid MUST NOT change the user's selected sort or direction; only the
  label changes.
- **FR-023**: During a shelf grid the filter sheet MUST offer the platforms, genres and game types present in
  the shelf's games, and selecting them MUST narrow the grid to games that match all of them. The grid MUST
  start with no filter selected.

**Lifecycle**

- **FR-024**: A shelf grid MUST survive device rotation, keeping the same shelf, games, placeholder, sort and
  filters, and MUST NOT flash the feed.
- **FR-025**: The open grid MUST NOT be persisted beyond the running session; a relaunch starts at the
  Discover feed.
- **FR-026**: The feature MUST behave identically on Android and iOS.

**Constraints on how it is built** *(from the project's own rules; the plan must respect them)*

- **FR-027**: The feature MUST stay inside the Search feature, touching the shared UI or model modules only if
  needed. The one exception is the domain module, which may only make the existing shared rule for the filter
  options visible to the Search feature (owner decision during planning, 2026-10-13). It MUST NOT add a
  module, a navigation route or a dependency between modules. The grid is a content state of Search, not a
  screen. If this turns out to be impossible, the plan MUST flag it rather than decide on the side.
- **FR-028**: Every user-visible string, including the arrow's label and the shelf names shown in the search
  bar, MUST come from string resources. No display text in a model, mapper or composable.
- **FR-029**: Which shelf is open, and any filtering or sorting, MUST be decided outside composables, which
  only render and emit events.
- **FR-030**: Layout MUST use the project's spacing tokens, and MUST reuse the existing shared modifiers and
  components before adding new ones.

### Key Entities *(include if feature involves data)*

- **Shelf**: One row of the Discover feed: a personalized shelf (named for the genre or studio it came
  from), "Most anticipated" (without the hero) or "Popular this month". It holds an ordered list of games, of
  which the row shows the first six. It is identified by its kind and its place in the feed.
- **Shelf grid**: The transient mode in which Search shows every game of one shelf as a grid, instead of the
  feed or a query's results. It is the committed-grid mode the genre browse introduced, not a new one. It
  holds a snapshot of the shelf, lives only for the current session, is never stored in history, and ends on
  clear, back arrow, system back or a new search or browse.
- **See-all arrow**: The control at the end of a shelf's title row, present only when the shelf holds more
  than six games.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Every shelf on the Discover feed shows no more than six games, so a user reaches the last shelf
  without swiping through any row of more than six cards.
- **SC-002**: A user can go from the Discover feed to the full list of any capped shelf with a single tap.
- **SC-003**: In 100% of cases the grid shows the same games, in the same order, as the shelf it came from,
  and the row shows the first six of them.
- **SC-004**: The grid appears with no loading state, and with the device offline it opens exactly as it does
  online.
- **SC-005**: No shelf with six or fewer games shows an arrow, and every shelf with more than six does.
- **SC-006**: A user can leave a shelf grid and be back on the Discover feed in one action, via the clear
  button, the back arrow or system back, and the feed is at the scroll position they left.
- **SC-007**: Whenever a shelf grid is showing, the user can tell which shelf it is from the search bar
  alone, without any text typed into it.
- **SC-008**: Opening shelf grids leaves the recent-searches list unchanged in 100% of cases.
- **SC-009**: A screen-reader user hears each arrow as a button that names its shelf.
- **SC-010**: Rotating the device during a shelf grid never drops the user back to the Discover feed.

## Assumptions

- **Cap of six**: six is the product decision and is applied to every shelf alike; it is a single value, not
  one per shelf.
- **"Most anticipated" holds the shelf without the hero**: the hero is split out of that lane before the
  shelf is formed, so its six games and its grid are the shelf's games after the hero was taken out. The
  hero is untouched.
- **Whole title row as the target**: the whole title row acts as the tap target when the shelf has an arrow,
  with the arrow as its visible affordance; a row without an arrow is inert. If the plan finds this conflicts
  with the spoken-label requirement, the arrow alone is the fallback.
- **The label of the default sort**: a shelf grid reuses the label "Top picks" that the genre browse
  introduced for "the order the source ranked the games in", rather than inventing a third wording. For a
  shelf, that order is the shelf's own order.
- **Sort and filter facets**: with no search result to supply the platforms and genres a filter sheet
  offers, they are taken from the shelf's own games, the way the sheet is built for any new result set.
  Unlike the genre browse, no genre is withheld from the genre filter, because a shelf (a studio's games, or
  a mixed-genre recommendation) is not defined by a single genre.
- **No error or loading state**: a shelf grid is built from games the feed already holds, so it has neither.
  The "no matches" state still applies when filters exclude everything.
- **Snapshot, not live**: the grid is a snapshot of the shelf at the moment of the tap; only the saved state
  of its cards follows the user's wishlist live, as it does in the feed and in search results.
- **Scroll position** is expected to hold without extra work, because the feed's scroll state already lives
  above the point where the feed and the results swap places; the plan verifies this on Android and, with a
  Mac, on iOS. If verified not to hold, FR-018 is deferred rather than the design bent around it.
- **Pagination**: none. The grid shows what the shelf holds; it never fetches more.
- **Out of scope**: pagination or fetching more than the lane holds, reordering shelves, changing the hero,
  changing how shelves are ranked or kept to a single shelf, persisting the open grid, and any change to
  Radar or Wishlist.
- **Dependencies**: reuses the Search screen's committed-grid mode and cards (spec 014), the existing sort and
  filter sheets, the list selector, and the Discover feed already held by Search; no new data source.
- **Verification**: Android is checked by build and unit tests. Everything about the iOS side of this shared
  code (compile, behavior, localized strings, the arrow mirroring) can only be verified on a Mac
  and is stated as unverified until it is.
