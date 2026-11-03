# Feature Specification: Discover Wide Layout

**Feature Branch**: `021-discover-wide-layout`

**Created**: 2026-11-03

**Status**: Draft

**Input**: User description: "Redesign the Discover feed (the Search screen's idle state, before any query is typed) for wide windows, such as desktop and tablets of 840dp or more. Compact windows keep their current layout unchanged. Reference: page 1 of docs/design/quest-log-desktop-variants.pdf ("Search home", variant A), also exported as docs/design/01-search-home-a-hero-banner.png."

## Clarifications

### Session 2026-11-03

- Q: Which wording does the wide hero's kicker use: the compact hero's "Most anticipated", or the mockup's "coming out for you"? → A: "Most anticipated", unchanged.
- Q: Should the wide layout add genre to the hero's meta line and the cards, which the feed's data does not carry? → A: No. No genre is shown and no genre data is added; the existing rows of information stay (developer and release date).
- Q: When a game is open beside the feed (spec 018), does the wide Discover follow the window or the narrower pane? → A: The window: the wide layout stays and adapts to the pane through its fitting rules.
- Q: Do shelf rows scroll sideways when more games exist than fit? → A: No. The row shows what fits and "show all" reaches the rest.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - A tall hero banner with its own actions (Priority: P1)

A user opens Search on a desktop window or a tablet and, before typing anything, sees the Discover feed. Today
the top of it is a short strip: the phone hero stretched across the window, capped at 240dp, and tapping it
anywhere opens the game. With this feature, on a wide window the top of Discover is a full-width banner that
is clearly taller and grows with the window. It carries a kicker, the featured game's title, one line of
meta (developer and release date, the date marked as an estimate when it is one) and two buttons placed inside the
banner: one to add the game to the wishlist, one to open its details.

**Why this priority**: The hero is the first thing the eye lands on and the main reason today's wide
Discover feels like a stretched phone screen. It also introduces the only new interaction in the feature
(two explicit actions instead of one big tap target), so it is the part that most needs to be right.

**Independent Test**: Open Search on a window at least 840dp wide with the feed loaded. Confirm the banner
spans the content width, is visibly taller than the phone hero, shows the kicker, title, meta line and both
buttons; tap "add to wishlist" and confirm the game is saved and the button shows it; tap "view details" and
confirm the game opens.

**Acceptance Scenarios**:

1. **Given** the feed is loaded on a window at least 840dp wide, **When** Discover is shown, **Then** the
   hero is a full-width banner showing the cover art, the kicker, the game's title, one meta line and two
   buttons: add to wishlist and view details.
2. **Given** the hero is shown, **When** the user taps "add to wishlist", **Then** the game is saved exactly
   as the heart on the phone hero saves it, and the button changes to show the game is now saved.
3. **Given** the featured game is already saved, **When** Discover is shown, **Then** the button already
   shows the saved state, and tapping it removes the game exactly as the phone heart does.
4. **Given** the user long-presses "add to wishlist", **When** the press fires, **Then** the list chooser
   opens, as a long press on the phone hero's heart does today.
5. **Given** the hero is shown, **When** the user taps "view details", **Then** the game's detail opens as it
   does today from the phone hero (beside the feed when two panes are shown).
6. **Given** the featured game has no developer or no cover, **When** the hero is shown, **Then** the meta
   line leaves out the developer without leaving a stray separator, and a missing cover shows the same
   fallback artwork the phone hero uses.
7. **Given** the featured game's release date is an estimate (a quarter, a year, or to be announced),
   **When** the meta line is shown, **Then** the date is marked as an estimate, as the mockup shows ("Q4
   2026 (est.)"), and a firm date is shown without that mark.
8. **Given** the hero is shown, **When** the user looks directly beneath it, **Then** the footnote that
   release dates are publisher estimates is there, as on the phone.

---

### User Story 2 - Full-width shelves that use the space (Priority: P1)

Below the hero, each shelf of Discover is a row of cards that fills the available width: as many cards as fit
side by side, six in the mockup at about 1080dp, instead of a short strip of phone-sized cards with empty
space beside it. Each shelf has its title on the left and a "show all" link on the right. Each card shows the
cover, the title, the developer, a wishlist heart, and a badge: the game's score when it has one, its release
timing otherwise.

**Why this priority**: Shelves are most of what Discover is. Filling the row is what makes the screen feel
designed for a large window, and it is independently testable on any feed that has at least one shelf.

**Independent Test**: On a window at least 840dp wide, open Discover with a feed that has the generic shelves
and at least one personalised one. Confirm every shelf is a single row filling the content width, each with a
title and a "show all" link, each card showing cover, title, developer, heart and badge. Widen and
narrow the window and confirm the number of cards per row follows the width.

**Acceptance Scenarios**:

1. **Given** Discover is shown on a window at least 840dp wide, **When** the user looks at any shelf,
   **Then** the shelf is one row of cards that fills the content width, with its title at the start of the
   row and a "show all" link at the end of it.
2. **Given** a window about 1080dp wide of content, **When** a shelf with at least six games is shown,
   **Then** the row shows six cards side by side, none cut off at the edge.
3. **Given** the window is widened or narrowed, **When** the available width changes, **Then** the number of
   cards in each row changes with it and cards keep a similar size instead of stretching, and no card is ever
   shown cut off.
4. **Given** a card is shown, **When** the user looks at it, **Then** it shows the cover, the game's title,
   the developer (left out when unknown), a heart reflecting whether the game is saved, and a badge showing
   the game's score when it has one and its release timing (year, date, or to be announced) otherwise.
5. **Given** the user taps a card, **When** the tap fires, **Then** the game opens; **Given** the user taps
   the heart, **Then** the game is saved or unsaved without opening it; **Given** the user long-presses the
   heart, **Then** the list chooser opens: all as on the phone card today.
6. **Given** a shelf's "show all" link is shown, **When** the user taps it, **Then** the same see-all grid
   opens that the phone feed opens for that shelf today.
7. **Given** the feed has personalised shelves (by genre or developer) and the generic ones, **When**
   Discover is shown, **Then** the shelves appear in the same order and with the same titles and contents as
   on the phone.

---

### User Story 3 - Switching between layouts never disturbs the feed (Priority: P2)

A user resizes the window, rotates the tablet, enters or leaves split screen, or opens a game beside the
feed, so that the window crosses the 840dp boundary in either direction while Discover is on screen. The
screen swaps between the compact and the wide layout without reloading the feed and without losing the user's
place. A user on a phone, or in any window narrower than 840dp, sees no change at all.

**Why this priority**: It protects the two things that must not regress: the existing phone experience and
the user's place in the feed. It is verified screen by screen once both layouts exist, so it ranks behind
them.

**Independent Test**: With the feed loaded and scrolled down past the hero, resize the window from 700dp to
1000dp wide and back. Confirm the layout swaps at 840dp, the feed does not show a loading state or refetch,
and the same shelf stays at the top of the view. On a phone, confirm Discover looks and behaves exactly as
before.

**Acceptance Scenarios**:

1. **Given** Discover is shown on a window narrower than 840dp, **When** the user looks at it, **Then** it is
   the existing compact layout, unchanged in appearance and behaviour.
2. **Given** the feed is scrolled to some shelf, **When** the window grows across 840dp (or shrinks back
   across it), **Then** the layout swaps and the same shelf is still at the top of the view, not back at the
   hero.
3. **Given** the feed is loaded, **When** the layout swaps, **Then** no loading state appears and the feed's
   data is not fetched again.
4. **Given** the window is exactly 840dp wide, **When** Discover is shown, **Then** the wide layout is used.
5. **Given** the feed is stale and the refresh prompt is shown, **When** the layout swaps in either
   direction, **Then** the prompt keeps its state (shown, refreshing or dismissed).

---

### Edge Cases

- The feed has no featured game (the hero slot is empty): the wide layout shows the shelves without a banner
  and without a gap where it would have been, as the compact layout does today.
- The feed has no shelves, only a hero, or nothing at all: the same placeholder or reduced content the
  compact layout shows today appears in the wide one.
- The feed is loading, has failed, or is empty: the wide layout shows the same loading, error (with its
  retry) and empty states as the compact one. The redesign applies only to a loaded feed.
- A shelf holds fewer games than fit in a row: its cards sit at the start of the row at their normal size,
  never stretched to fill it, and its "show all" link is shown only when "show all" does something today.
- A shelf holds more games than fit in the row at the current width (a narrow wide window, or a half-width
  pane): the cards that do not fit are left out of the row rather than cut off or scrolled sideways, and the
  shelf offers "show all" so every game stays reachable.
- A very long game title, developer or shelf title: it is truncated to one line (the hero title may use two)
  and never pushes a button, the heart, the badge or the "show all" link out of view.
- A very tall or very short window: the hero's height has a floor and a ceiling, so it neither collapses nor
  swallows the screen; on a short window the first shelf's title still shows without scrolling.
- The stale-recommendations prompt is showing: it still pins to the top of the feed and does not cover the
  hero's buttons once the user scrolls.
- Discover is shown in the left pane while a game is open on the right (spec 018): it follows the same
  fitting rules inside the narrower pane (see Assumptions).
- The mouse wheel or trackpad over the feed scrolls it vertically; no content may depend on sideways
  scrolling to be reached.
- Light and dark themes: the banner text stays legible on any cover art in both.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: On a window at least 840dp wide (840dp included), the Discover feed MUST be shown in the wide
  layout; on a narrower window it MUST be shown in the existing compact layout, with no visible or behavioural
  change from today.
- **FR-002**: The wide layout MUST show the same content as the compact one, from the same feed data: the
  same featured game, the same shelves in the same order with the same titles, the same loading, empty and
  error states, and the same stale-recommendations prompt. This feature MUST change layout only.
- **FR-003**: The hero MUST be a full-width banner whose height grows with the window's height, with a floor
  and a ceiling well above today's 240dp cap, and MUST NOT push the first shelf's title off the screen on a
  window at least 800dp tall.
- **FR-004**: The hero MUST show the cover art, a kicker label, the game's title, and one meta line holding
  the developer and the release date, in that order, leaving out the developer when it is unknown. No genre
  is shown.
- **FR-005**: The meta line MUST mark a release date as an estimate whenever it is one (a quarter, a year, or
  to be announced) and MUST NOT mark a firm date.
- **FR-006**: The hero MUST contain two actions inside the banner: "add to wishlist" and "view details". The
  first MUST save or remove the game exactly as the phone hero's heart does, reflect the saved state, and
  open the list chooser on a long press. The second MUST open the game exactly as the phone hero's tap does.
- **FR-007**: The footnote that release dates are publisher estimates MUST appear directly under the hero.
- **FR-008**: Each shelf MUST be a single row of cards that fills the available width, showing as many cards
  as fit at a roughly constant card size and never showing a card cut off or stretched.
- **FR-009**: Each shelf MUST show its title at the start of its row and a "show all" link at the end of it;
  the link MUST open the same see-all grid the compact feed opens for that shelf, and MUST appear under the
  same conditions as the compact feed's arrow, plus whenever the row is showing fewer games than the shelf
  holds.
- **FR-010**: Each card MUST show the cover, the title, the developer (left out when unknown), a heart
  reflecting the saved state, and a badge: the game's score when it has one, otherwise its release timing
  (year, date, or to be announced). Taps MUST do what they do on the compact card: a tap opens the game, the
  heart toggles the save, a long press on the heart opens the list chooser.
- **FR-011**: The search bar at the top and the navigation on the left MUST be unchanged.
- **FR-012**: Resizing the window across the 840dp boundary in either direction MUST swap the layout without
  reloading the feed, without a loading state, and without losing the scroll position: the same shelf stays
  at the top of the view.
- **FR-013**: The stale-recommendations prompt MUST keep its current behaviour, including its state across a
  layout swap, and MUST remain reachable and legible over the wide layout.
- **FR-014**: Every piece of new text (the kicker, "add to wishlist", its saved state, "view details", "show
  all", the estimate mark) MUST come from the app's string resources, none hardcoded, and MUST be announced
  sensibly by screen readers; the banner's two buttons and the "show all" link MUST be reachable and
  activatable by keyboard, since desktop is the main wide target.
- **FR-015**: The wide layout MUST work in the light and the dark theme, and every cover-art overlay MUST keep
  its text legible.

### Key Entities

- **Featured game (hero)**: the one game the feed highlights. Attributes used: cover, title, developer,
  release date and whether that date is an estimate, saved state.
- **Shelf**: a titled, ordered group of games (generic lanes, or personalised by genre or developer), with
  whether it holds more games than it shows. Unchanged by this feature.
- **Shelf card**: one game in a shelf: cover, title, developer, saved state, and the badge (the
  score when the game has one, its release timing otherwise).
- **Wide window**: a window at least 840dp wide, the same threshold the two-pane Search layout uses.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: On a window at least 840dp wide, a side-by-side comparison of Discover with variant A of the
  mockup (cover art and wording aside) finds the same structure: full-width banner with kicker, title, meta
  line and two buttons, the estimate footnote, then titled rows with a "show all" link and cards with cover,
  title, developer, heart and badge (genre aside, which is not shown).
- **SC-002**: On a window 800dp tall or taller, the hero is at least 1.3 times as tall as the compact hero
  would be in the same window, and the first shelf's title is visible without scrolling.
- **SC-003**: On a window with about 1080dp of content width, every shelf with six or more games shows exactly
  six cards in one row; across widths from 840dp up to 2560dp, 100% of the rows show whole cards with no
  card cut off, stretched or hidden under the edge.
- **SC-004**: A user can add the featured game to the wishlist or open its details with a single click or tap
  on a button inside the banner, and can do the same for any shelf game from its card.
- **SC-005**: Crossing the 840dp boundary in either direction, 20 times in a row, never shows a loading
  state, never triggers a new fetch of the feed, and keeps the same shelf at the top of the view every time.
- **SC-006**: On any window narrower than 840dp, Discover is indistinguishable from before the feature, in
  appearance and in what every tap does.
- **SC-007**: Every tap target that existed on the wide Discover before the feature (open a game, save a
  game, long-press for the list chooser, "see all", retry, refresh prompt and its dismissal) still works and
  does the same thing, 100% of them.
- **SC-008**: The banner's two buttons, every card and every "show all" link can be reached and activated
  using only the keyboard, in the order they appear on screen.

## Assumptions

- **Scope**: "Discover" is the Search screen's idle state, the feed shown before any query is typed. Search
  results, the see-all grids themselves, game detail, and every other screen are not changed. This is
  layout only: no change to where shelves come from, how they are chosen, the stale-recommendations rules,
  or what any tap does.
- **Out of scope, each its own spec**: the search-results filter sidebar, the dropdown search overlay, the
  "most popular" and "recently viewed" side panel (mockup variant B), the full-detail expand button, and
  everything price-related in the mockup (offers, price sources).
- **The mockup is a layout reference only**: its copy is Italian placeholder text and its images are grey
  placeholders. The app's own English copy and real cover art are used. The reference is variant A,
  `docs/design/01-search-home-a-hero-banner.png`.
- **The kicker keeps the hero's current wording** ("Most anticipated", the label the compact hero already
  shows), not the mockup's "coming out for you". Confirmed by the owner (see Clarifications).
- **No genre is shown**: the mockup's meta line and card subtitle show a genre, but the featured game and the
  shelf cards carry none today. This feature does not widen what the feed fetches or stores, so the meta line
  is developer and release date, and the card subtitle is the developer. Confirmed by the owner. The
  personalised "by genre" shelves are unaffected: they are shelves, not a genre label on a card.
- **The card badge is built from existing parts**: today's shelf card shows a title and a release-year line,
  with no score. The wide card's badge reuses the existing score badge and the existing release text, per
  the mockup: the score for a game that has one, the release timing (year, date, or to be announced) for one
  that does not.
- **The hero banner itself is not a tap target**: unlike today's phone hero, only its two buttons act, as in
  the mockup. The long-press list chooser moves to the "add to wishlist" button.
- **Shelf rows never scroll sideways**: shelves already hold at most six games, so a wide window shows up to
  six cards, fewer when the width allows fewer, and the rest are reached through "show all", since a mouse
  wheel does not scroll a row sideways (see the open backlog item on wheel scrolling in Discover). A shelf
  that holds more than the row shows therefore always offers "show all", even where the compact feed would
  not. Confirmed by the owner. Whether the see-all grid already works for a shelf the compact feed gives no
  arrow is for planning to verify.
- **"Wide" follows the window, not the pane**: the layout is chosen once from the window width at the same
  840dp threshold as two-pane Search. When a game is open beside the feed, Discover sits in a narrower left
  pane in the wide layout and adapts through its own fitting rules (fewer cards per row, the banner's
  buttons wrapping below the title when the width is short). Confirmed by the owner.
- **Both layouts are separate sets of screen parts**: the owner requires the wide layout to be its own
  set of screen parts rather than size conditions inside the compact ones, with the window size read once
  to pick between them, both over the same feed data and state. Planning carries this as a constraint; it is also
  what makes SC-005 (no reload, kept position) achievable.
- **Hero height**: the exact proportion and ceiling are tuned at planning against the mockup; this spec fixes
  only the observable bounds in FR-003 and SC-002.
- **New text** is added to the app's string resources in English, the only language it ships today.
- **Targets**: desktop and tablets, on Android first; iOS gets the shared layout, and its visual check needs
  a Mac and is done when one is at hand.
- **Dependencies**: the existing feed data and its see-all grid; the 840dp wide-window rule used by two-pane
  Search (spec 018); the shared game card and save button components.
