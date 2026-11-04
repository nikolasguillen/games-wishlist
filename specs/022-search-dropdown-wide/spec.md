# Feature Specification: Search Dropdown on Wide Windows

**Feature Branch**: `022-search-dropdown-wide`

**Created**: 2026-11-04

**Status**: Draft

**Input**: User description: "Replace the full-screen search overlay with a dropdown under the search bar on wide windows, such as desktop and tablets of 840dp or more. Compact windows keep the current full-screen overlay unchanged. Reference: docs/design/05-search-overlay-preview.png (its expanded game card beside the panel is not wanted; its copy is Italian placeholder text, so follow its layout, not its wording or data)."

## Clarifications

### Session 2026-11-04

- Q: Does the dropdown keep the search-type chips (games, characters, developers)? → A: Yes. They stay as a
  compact row at the top of the panel, above the lists, so characters and developers remain searchable on wide
  windows. This departs from the mockup, which omits them.
- Q: Does the dropdown keep the genre quick-browse shown before the first keystroke? → A: Yes. It sits right
  below the search-type chips and above the recent searches and the recently viewed games, the same order as
  the full-screen overlay. This departs from the mockup, which omits it.
- Q: Does the dropdown get the mockup's expanded game card beside it? → A: No, and not later either. The owner
  does not want it: the panel is only the list, nothing opens beside it.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Search from a dropdown that leaves the page visible (Priority: P1)

A user on a desktop window or a tablet clicks into the search bar. Today the whole window is taken over by a
full-screen overlay: phone content stretched across a wide window, with the screen they were on hidden behind
it. With this feature, on a wide window a panel opens directly under the bar instead. It is as wide as the bar
and floats over the page; the navigation rail, the rest of the window and the page behind stay visible. Before
the user types, the panel lists their recent searches and recently viewed games. As they type, it lists
matching suggestions (recent searches that match, then games with title, year and developer) and ends with a
row to see all results for what they typed.

**Why this priority**: This is the feature. Everything else (keyboard use, closing rules, the boundary
crossing) refines how this panel behaves, and the panel alone already replaces the stretched overlay with
something that fits a wide window.

**Independent Test**: On a window at least 840dp wide, click the search bar with some history present: confirm a
panel opens under the bar, no wider than the bar, with the page and the navigation rail still visible around
it, showing the recent searches and recently viewed games. Type a few letters: confirm it switches to matching
suggestions ending with the "see all results" row.

**Acceptance Scenarios**:

1. **Given** a window at least 840dp wide, **When** the user clicks or taps the search bar, types into it, or
   presses Down while it has keyboard focus, **Then** a panel opens
   directly under the bar, as wide as the bar, floating over the page, and the navigation rail and the page
   around the panel stay visible.
2. **Given** the panel is open and the query is empty, **When** the user looks at it, **Then** it shows the
   search-type chips at the top, then the genre quick-browse below them, then the same recent searches and
   recently viewed games the full-screen overlay shows today, in the same order as the full-screen overlay.
3. **Given** the panel is open, **When** the user types, **Then** it shows the same suggestions the
   full-screen overlay shows for that text: matching recent searches, matching games (title, year, developer)
   and a final row to see all results for the typed text, with the same loading state while games are fetched.
4. **Given** the user has no history and has typed nothing, **When** the panel opens, **Then** it shows the
   search-type chips, the genre quick-browse and the same empty-state message the full-screen overlay shows
   today.
5. **Given** the panel is open, **When** the user picks the characters or developers chip, **Then** the chip
   shows as selected and the typed search looks up that type, as in the full-screen overlay today.
6. **Given** the panel is open with an empty query, **When** the user chooses a genre, **Then** the genre's
   results open as they do from the full-screen overlay today.
7. **Given** the user chooses a game suggestion, **When** the choice fires, **Then** that game opens, as it
   does from the full-screen overlay today (beside the results when two panes are shown).
8. **Given** the user chooses the "see all results" row or a recent search, **When** the choice fires, **Then**
   the search is committed and the results appear, as they do from the full-screen overlay today.
9. **Given** a window narrower than 840dp, **When** the user focuses the search bar, **Then** the existing
   full-screen overlay opens, unchanged in appearance and behaviour.

---

### User Story 2 - Use the dropdown entirely from the keyboard or the mouse (Priority: P1)

A desktop user searches without touching the mouse, or with only the mouse. With the keyboard, the arrow keys
move through the panel's rows, the highlighted row is clearly marked, and Enter activates it. With the mouse,
hovering a row highlights it and a click activates it. Escape closes the panel.

**Why this priority**: A dropdown on desktop that cannot be driven by the keyboard is worse than the overlay it
replaces. Keyboard and mouse use is the reason a dropdown fits a desktop window, so it ships with the panel,
not after it.

**Independent Test**: Open the panel with several rows showing. Without the mouse, press Down repeatedly and
confirm the highlight moves row by row through every row, press Up to go back, press Enter on a game and
confirm it opens. Reopen the panel, hover rows with the mouse and confirm the highlight follows the pointer,
then click one and confirm it activates.

**Acceptance Scenarios**:

1. **Given** the panel is open, **When** the user presses Down, **Then** the highlight moves to the next row,
   and from the last row it does not run off the end of the panel.
2. **Given** a row is highlighted, **When** the user presses Up, **Then** the highlight moves to the previous
   row, and from the first row focus returns to the text field with its text and caret intact.
3. **Given** a row is highlighted, **When** the user presses Enter, **Then** that row is activated exactly as a
   click on it would be.
4. **Given** no row is highlighted and there is a typed query, **When** the user presses Enter, **Then** the
   search is committed for the typed text, as it is from the full-screen overlay today.
5. **Given** the highlight is on a row, **When** the user types or edits text, **Then** typing still goes to the
   text field and the suggestions update.
6. **Given** the panel is open, **When** the user moves the mouse over a row, **Then** that row shows the same
   highlight as a keyboard-selected row, and a click on it activates it.
7. **Given** a list taller than the panel, **When** the highlight moves with the keyboard, **Then** the
   highlighted row is always scrolled into view.
8. **Given** a screen reader or assistive tool is in use, **When** the highlight moves, **Then** the
   highlighted row is announced, and every item of the panel is reachable with the arrow keys while focus
   stays in the text field.

---

### User Story 3 - The panel closes without losing what was typed (Priority: P1)

The user can dismiss the panel at any time and come back to it. It closes on Escape, on a click outside it, and
when the search bar loses focus. Closing it never clears the text in the bar, so refocusing the bar reopens the
panel on the same query.

**Why this priority**: A dropdown that eats the user's query when it is dismissed would make every accidental
click costly. It is also the half of "dropdown behaviour" that users expect without being told.

**Independent Test**: Type "ember", press Escape: the panel closes and "ember" is still in the bar. Focus the
bar again: the panel reopens on "ember". Repeat closing by clicking the page outside the panel, and by moving
focus away with Tab.

**Acceptance Scenarios**:

1. **Given** the panel is open, **When** the user presses Escape, **Then** the panel closes and the typed text
   stays in the bar.
2. **Given** the panel is open, **When** the user clicks or taps anywhere outside the panel and the bar,
   **Then** the panel closes, the typed text stays in the bar, and the click does not also activate whatever
   was beneath the panel.
3. **Given** the panel is open, **When** the search bar loses focus (for example Tab moves focus away),
   **Then** the panel closes and the typed text stays in the bar.
4. **Given** the panel was closed with text in the bar, **When** the user clicks the bar again (or types, or
   presses Down in it), **Then** the panel reopens showing suggestions for that same text.
5. **Given** the user clicks inside the bar to move the caret while the panel is open, **When** the click
   lands, **Then** the panel stays open.
6. **Given** a row's remove button or the "clear all" action is used, **When** it fires, **Then** the same
   confirmation dialog the full-screen overlay shows appears, the panel stays open behind it, and confirming
   or cancelling returns the user to the panel (confirming a removal updates the list in place).

---

### User Story 4 - Resizing across the 840dp boundary never loses the query (Priority: P2)

A user resizes the window, rotates a tablet, or enters or leaves split screen, so the window crosses 840dp in
either direction while the search bar is focused and a query is typed. The presentation swaps between the
dropdown and the full-screen overlay, and the typed text, the caret position and the focus stay where they
were.

**Why this priority**: Both presentations exist side by side from now on, so crossing the boundary is a new way
to lose input. It is verified once both presentations exist, so it ranks behind them.

**Independent Test**: On a window of 1000dp, type "ember" with the panel open. Narrow the window to 700dp: the
full-screen overlay is showing with "ember" in the bar. Widen it back to 1000dp: the dropdown is showing with
"ember" in the bar and suggestions for it.

**Acceptance Scenarios**:

1. **Given** the panel is open with typed text, **When** the window narrows below 840dp, **Then** the
   full-screen overlay is shown with the same text, and suggestions for it.
2. **Given** the full-screen overlay is open with typed text, **When** the window widens to 840dp or more,
   **Then** the dropdown is shown with the same text, and suggestions for it.
3. **Given** either direction of the swap, **When** it happens, **Then** the search type already chosen is kept
   and no search is committed or cleared by the swap itself.
4. **Given** a confirmation dialog is open (remove a recent search, clear history), **When** the window crosses
   the boundary, **Then** the dialog is still open afterwards.

---

### Edge Cases

- The window is exactly 840dp wide: the dropdown is used.
- The bar is already focused when the search screen is shown, or the screen returns from a game opened from a
  suggestion: the panel follows the same open and close rules as any other focus change; it does not reopen on
  its own once closed, until the bar is focused again.
- Two panes are shown (Search beside a game's detail): the panel stays as wide as the bar and does not extend
  over the detail pane.
- The page behind the panel is a results grid, a character or developer grid, or the Discover feed: the panel
  floats over it in the same way, and the page does not scroll, reflow or reload when the panel opens or closes.
- The panel has more rows than fit under the bar in a short window: the panel is capped to the space available,
  scrolls inside itself, and never runs off the bottom of the window.
- The empty-query panel is at its tallest (chips, genres, recent searches and recently viewed games all
  present): it is capped to the space available under the bar and scrolls inside itself, never past the bottom
  of the window.
- The user presses Down with no rows to highlight (empty state, or only the loading rows): nothing happens and
  the text field keeps focus.
- Suggestions arrive or change while a row is highlighted: the highlight does not jump to a different row
  silently; if the highlighted row disappears, the highlight returns to the text field.
- A game suggestion has no year or no developer: the row shows what exists, without a stray separator, as the
  full-screen overlay does today.
- The query is blank (only spaces): the panel behaves as it does for an empty query, as the overlay does today.
- A very long query or a very long game title: the text is cut with an ellipsis and never widens the panel.
- The mouse pointer rests over a row while the keyboard moves the highlight: the keyboard's highlight wins until
  the pointer actually moves.
- The user uses select-all, copy or paste in the text field while the panel is open: they work on the text as
  normal and do not close the panel or move the highlight.

## Requirements *(mandatory)*

### Functional Requirements

**Presentation**

- **FR-001**: On a window at least 840dp wide, a click or tap on the search bar, typing into it, or pressing Down
  while it has keyboard focus MUST open a panel directly under the bar instead of the full-screen overlay. Moving
  keyboard focus onto the bar with Tab alone shows the bar's focus but does not open the panel (the convention for
  a search field with suggestions, and what keeps a panel closed with Escape from reopening by itself).
- **FR-002**: The panel MUST be as wide as the search bar, MUST float over the page, and MUST NOT cover the
  navigation rail, the area beside the bar or the whole window.
- **FR-003**: The panel's height MUST fit its content up to a maximum that keeps the lower part of the window
  visible, scrolling inside itself beyond that, and MUST NOT extend past the bottom of the window.
- **FR-004**: On a window narrower than 840dp, the search bar MUST open the existing full-screen overlay,
  unchanged in appearance, content and behaviour.
- **FR-005**: The page behind the panel MUST NOT scroll, reflow, reload or lose its state when the panel opens
  or closes.
- **FR-006**: The panel's layout MUST follow `docs/design/05-search-overlay-preview.png` (a titled list of recent
  searches, a titled list of games with title, year and developer, and a final row to see all results for the
  typed text), without its wording or placeholder data and without its expanded game card.

**Content**

- **FR-007**: With an empty or blank query, the panel MUST show the same recent searches and recently viewed
  games as the full-screen overlay, each list with the same ordering, the same limits and the same empty-state
  message (the chips and the genre quick-browse above them are FR-009 and FR-010),
  and with the same ways to remove one recent search and to clear the history.
- **FR-008**: With a non-blank query, the panel MUST show the same suggestions as the full-screen overlay for
  that query: matching recent searches, then matching games (title, year, developer, with the same loading state
  and the same cap), then a final row to see all results for the typed text.
- **FR-009**: The panel MUST keep the search-type chips (games, characters, developers) as a compact row at the
  top of the panel, above the lists, shown with an empty query and with a typed one. Picking a type MUST behave
  as it does in the full-screen overlay today (it changes the type of the search and nothing else in the
  panel), and the chosen type MUST be the one the suggestions and the committed search use.
- **FR-010**: With an empty or blank query, the panel MUST keep the genre quick-browse as a section directly
  below the search-type chips and above the recent searches and the recently viewed games, so the order from the
  top is chips, genres, recent searches, recently viewed games, as in the full-screen overlay, with the same genres, the same order and the same result
  when one is chosen as in the full-screen overlay. It is not shown while a query is typed, as today. When there
  is no history, the section still shows, as it does today for a first-time user.
- **FR-011**: All text in the panel MUST be the app's own localized strings, not the mockup's copy.

**Choosing**

- **FR-012**: Choosing a game suggestion MUST open that game exactly as the full-screen overlay does.
- **FR-013**: Choosing the final row, or a recent search, MUST commit the search exactly as the full-screen
  overlay does.
- **FR-014**: Removing a recent search and clearing the history MUST ask for confirmation with the same dialogs
  as today, and the data behind recents, suggestions and history MUST be the same as today's.

**Keyboard and mouse**

- **FR-015**: The Up and Down arrow keys MUST move the highlight through every row of the panel (the search-type
  chips, the genres, the "clear all" action, the recent searches, the games and the final row) in visual order, without wrapping past
  either end, and Up from the first row MUST return focus to the text field with its text
  and caret unchanged.
- **FR-016**: Enter MUST activate the highlighted row exactly as a click does; with no row highlighted, Enter
  MUST commit the typed query as it does from the full-screen overlay today. Shift+Delete on a highlighted recent
  search MUST ask to remove it with the same confirmation as today, and on a highlighted recently viewed game MUST
  remove it as its remove button does.
- **FR-017**: The highlighted row MUST be visibly marked, distinct from the other rows, and the same mark MUST
  be used for the keyboard's highlight and the mouse's hover.
- **FR-018**: Hovering a row with the mouse MUST highlight it and clicking it MUST activate it.
- **FR-019**: Typing, editing and the usual text-field shortcuts MUST keep working while a row is highlighted,
  and MUST NOT be swallowed by the panel.
- **FR-020**: The highlighted row MUST always be scrolled into view, and MUST be exposed to assistive tools,
  which MUST announce it when it changes.

**Closing**

- **FR-021**: The panel MUST close on Escape, on a click or tap outside the panel and the bar, and when the
  search bar loses focus.
- **FR-022**: A click or tap that closes the panel MUST NOT also activate whatever was beneath it.
- **FR-023**: Closing the panel MUST NOT clear or alter the typed text, and focusing the bar again MUST reopen
  the panel on the same text.
- **FR-024**: A click inside the search bar while the panel is open MUST NOT close it.

**Structure and boundary**

- **FR-025**: The dropdown MUST be its own presentation, separate from the full-screen overlay, rather than
  size conditions inside the overlay; the compact overlay MUST keep working exactly as it does today.
- **FR-026**: The Search screen MUST be told by the app whether the window is wide (the 840dp rule the app
  already uses) and choose between the two presentations once, in one place, following the feature-module rule
  for screens with a separate wide layout. Whether that is one choice for
  all of Search or one per area is a decision for planning, since results and filters will get wide variants
  of their own.
- **FR-027**: Crossing 840dp in either direction while the bar is focused MUST swap the presentation without
  clearing the typed text, moving the caret, changing the chosen search type or committing a search, and MUST
  keep an open confirmation dialog open.
- **FR-028**: The panel MUST NOT show the mockup's expanded game card (or any preview of the highlighted game)
  beside or inside it: the owner does not want it. This feature MUST NOT add the side filter panel on results
  either, or make any change to the compact layout.

### Key Entities *(include if feature involves data)*

- **Search panel row**: One selectable line of the panel. It is a recent search, a recently viewed game, a
  matching recent search, a matching game (title, year, developer) or the final "see all results" row. Each row
  has one action when chosen, and exactly one row at a time can be highlighted.
- **Highlight**: Which row, if any, the arrow keys or the mouse have marked as the one Enter will activate. It
  belongs to the panel, resets when the panel closes, and is separate from the text field's focus.
- **Panel open state**: Whether the dropdown is showing. It follows the search bar's focus and is closed by
  Escape or an outside click; it never owns the typed text.

The recents, suggestions and history themselves are not new: this feature reads exactly the data the
full-screen overlay reads.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: On a window of 840dp or more, 100% of attempts to open it (a click, typing or Down in the bar) show a panel under the bar whose
  width equals the bar's width, with the navigation rail and the page visible around it.
- **SC-002**: A user can go from an empty bar to an opened game, using only the keyboard (type, Down, Enter), in
  under 5 seconds, without touching the mouse.
- **SC-003**: Every row of the panel can be highlighted by the arrow keys and by the mouse hover, and
  activating it by Enter or by click has the same result, in 100% of the rows checked.
- **SC-004**: After Escape, an outside click, or a loss of focus, the typed text in the bar is unchanged in
  100% of attempts, and refocusing the bar reopens the panel on that text.
- **SC-005**: Resizing a window across 840dp in both directions with a typed query and the bar focused leaves the
  query, caret and search type unchanged in 100% of attempts, and no search is committed by the resize.
- **SC-006**: On a window narrower than 840dp, the search overlay is indistinguishable from today's in
  appearance and behaviour, and every existing compact-overlay check still passes.
- **SC-007**: The content shown in the panel for a given history and query matches the full-screen overlay's
  content for the same history and query, row for row.
- **SC-008**: Opening and closing the panel causes no visible reflow, scroll jump or reload of the page behind it.

## Assumptions

- The wide threshold is the app's existing 840dp rule, with exactly 840dp counting as wide; this feature
  introduces no second threshold.
- "Wide window" covers desktop windows and tablets alike; the same panel serves both, and touch input works on
  it as ordinary taps, though the keyboard and mouse behaviour is designed first for the desktop.
- Only the dropdown's presentation changes. The data behind recents, suggestions and history, the confirmation
  dialogs and the actions chosen from rows are reused as they are.
- The mockup is followed for layout (a bar-width panel under the bar, titled lists, the final "see all" row, a
  clearly marked highlighted row), not for its Italian wording, placeholder games or its expanded game card.
- Where the mockup and the current overlay differ in what a row shows (the mockup's games show year and
  developer, with a quarter for unreleased games), the panel shows what the current suggestions already carry.
- The panel's row heights and hit targets follow the app's existing sizing tokens, comfortable for a mouse and
  at least as large as a tap target.
- The "back" arrow in the mockup's bar is the bar's existing back behaviour and is not part of this feature.
- Escape closes only the panel; a second Escape with the panel already closed does whatever it does today.
- Keyboard and focus behaviour can only be fully verified on a desktop window, which has not been tried yet for
  Search; those checks are recorded as open in the technical-debt list if they cannot be done before the feature
  ships, as was done for the Discover wide layout.
- The mockup's expanded game card beside the panel is not wanted, now or later, and is not a future spec. Out
  of scope, the side filter panel on results has its own spec, and so would any compact layout change.
