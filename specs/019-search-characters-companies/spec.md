# Feature Specification: Search by Character and Developer

**Feature Branch**: `019-search-characters-companies`

**Created**: 2026-10-20

**Status**: Draft

**Input**: User description: "I want to add search by game character and search by developer. Search api accepts character and company params. Find out if we can pass the same query to all three params or we have to make 3 different api calls for games, companies and characters. In search grid, characters and companies layouts should differ from games cards. The image should be inside a circle for characters, and maybe a different shape for companies. If we find out that we can search multiple things with a single api call, we'll have to understand how to reorganize the search results grid in order to show the new results"

**Follow-up direction (clarification)**: "Tapping on a character or a dev should lead to a new detail page, designed like the wishlist detail screen, with a header with the info about the dev. In the page's body we should have the description (translatable like in game detail) and then the grid view (not switchable to list view) with all the games where the character appears, or all the games created by the selected developer. We won't call 3 apis for each search, we will have a search type selector in the expanded search view, so every search result grid will have just one type of result." A mockup of the selector is in [mockups/search-type-chips.png](mockups/search-type-chips.png) (its colors are outdated; only the layout applies).

## Clarifications

### Session 2026-10-20

- Q: What happens when the user taps a character or a developer card? → A: A new detail page opens, designed like the wishlist detail screen: a header with the entity's info, then in the body its description (translatable as in game detail) and a grid of its games.
- Q: Does one search look up games, characters and developers together? → A: No. A search type selector in the expanded search view picks Games, Characters or Developers, and every results grid holds exactly one kind of result, so each search is a single lookup.
- Q: How does the selector look, and what does the expanded search view show below the bar while Characters or Developers is selected? → A: The selector is a row of three chips (Games, Characters, Developers) directly under the search bar, with a check mark on the selected one. The content below the row (genre browse, recent searches, recently viewed) is the same for every type; selecting a type changes nothing there.
- Q: Can the games on the detail page be shown as a list? → A: No. They are a grid only, with no list/grid toggle.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Choose what to search for (Priority: P1)

When the user opens the expanded search view, a row of three chips sits directly under the search bar, above
the genre browse: **Games**, **Characters** and **Developers**. Each chip has an icon and a title, and the
selected one swaps its icon for a check mark and is highlighted. Games is selected by default. Tapping a chip
selects that type and updates the hint text in the bar ("Search for a game…", "Search for a character…",
"Search for a developer…"). Nothing below the row changes with the type: genre browse, recent searches and
recently viewed are the same for all three. A submitted search looks up only the selected type, so the
results grid holds one kind of result.

**Why this priority**: Everything else hangs from it. Without the selector there is no way to ask for a
character or a developer, and it keeps every search to a single lookup and every grid to a single kind of
card.

**Independent Test**: Open the expanded search view, confirm the three chips under the bar with a check mark
on Games, tap Characters, and confirm the check mark moves and the hint text changes while everything below
the row stays the same. Submit a search and confirm every result is a character.

**Acceptance Scenarios**:

1. **Given** the expanded search view, **When** the user looks under the search bar, **Then** a row of three
   chips (Games, Characters, Developers) is shown, Games is selected with a check mark, and the hint text
   matches it.
2. **Given** the chip row, **When** the user taps another chip, **Then** it becomes the only selected chip and
   the hint text shows the new type, and anything already typed in the bar is kept.
3. **Given** any type is selected, **When** the user looks below the chip row, **Then** the genre browse,
   recent searches and recently viewed are the same as for the other two types.
4. **Given** any type is selected, **When** a search is submitted, **Then** the results grid contains only
   that type.
5. **Given** a search is showing results, **When** the user reopens the expanded view, picks a different type
   and submits, **Then** the grid is replaced by that type's results for the submitted text. Picking a chip
   never starts a search by itself.
6. **Given** a recent search is tapped, **When** a type is selected, **Then** that text is searched for the
   selected type.

---

### User Story 2 - Find a character and see its games (Priority: P1)

A user remembers a character but not the game: "Kratos", "Link". They pick Characters, type the name and
submit. The results are a grid of character cards, each with the character's portrait inside a circle and the
character's name. Tapping a card opens that character's detail page, which shows who the character is and the
games they appear in (User Story 4), and from there the user opens a game like any other.

**Why this priority**: It is the headline of the feature. Today a character name that is not part of a game
title finds nothing, and the game is unreachable from Search.

**Independent Test**: Pick Characters, search "Kratos", confirm the grid shows circular portrait cards, tap
Kratos, and confirm the detail page opens.

**Acceptance Scenarios**:

1. **Given** Characters is selected, **When** the user submits a name that belongs to a character, **Then**
   the grid shows character cards, each with a circular portrait and the name.
2. **Given** a character has no portrait, **When** its card is shown, **Then** a neutral placeholder fills the
   circle instead of an empty or broken image.
3. **Given** a character card, **When** the user taps it, **Then** that character's detail page opens.
4. **Given** Characters is selected, **When** the search matches no character, **Then** the existing
   empty-results message is shown, worded for characters.

---

### User Story 3 - Find a developer and see its games (Priority: P1)

A user wants everything from a studio: "Naughty Dog", "FromSoftware". They pick Developers, type the name and
submit. The results are a grid of developer cards, each with the company's logo on a tile whose shape is
visibly different from both game cards and the circular character cards, and the company's name. Tapping a
card opens that developer's detail page.

**Why this priority**: It is the second half of the request and as valuable as the first. It is independent:
it needs its own card and its own detail page and shares nothing with the character slice but the selector
and the page's layout.

**Independent Test**: Pick Developers, search "Naughty Dog", confirm a logo-tile card appears, tap it, and
confirm the developer's detail page opens.

**Acceptance Scenarios**:

1. **Given** Developers is selected, **When** the user submits a name that belongs to a company, **Then** the
   grid shows developer cards, each with the logo on its tile and the name.
2. **Given** a company has no logo, **When** its card is shown, **Then** a neutral placeholder is shown on the
   tile.
3. **Given** a developer card, **When** the user taps it, **Then** that developer's detail page opens.
4. **Given** a game card, a character card and a developer card (seen across the three types), **When** the
   user compares them, **Then** each can be told apart by its shape alone.
5. **Given** Developers is selected, **When** the search matches no company, **Then** the existing
   empty-results message is shown, worded for developers.

---

### User Story 4 - Read about a character or developer and browse their games (Priority: P1)

The detail page of a character or a developer looks like the wishlist detail screen. A header at the top
carries the entity's image (the portrait for a character, the logo for a developer), its name and its key
facts as small chips: the number of games, plus the country and the founding year for a developer when
known. Below the header, the body has the entity's description, which the user can translate the same way as
a game's description, and under it a grid of every game the character appears in or that the developer
created. The grid is always a grid, with no switch to a list. Tapping a game opens its detail; saving and the
long-press action work as they do on any game card. Back returns to the results the user came from.

**Why this priority**: It is where the user lands after every tap on the new cards and where the games are
finally reached, so the first three stories are incomplete without it. It is the same slice for characters and
developers, so it is built once.

**Independent Test**: Open the detail of a character and of a developer from the results. Confirm the header,
the description with its translate action, and a grid of games with no list switch. Open a game from the
grid, then go back twice and confirm the search query and results are intact.

**Acceptance Scenarios**:

1. **Given** the user taps a character or developer card, **When** the page opens, **Then** the header shows
   its image, name and game count, and for a developer also the country and founding year when the catalogue
   has them.
2. **Given** the page has a description, **When** the user looks at the body, **Then** the description sits
   above the grid, and the user can translate it, view the translation info, retry a failed translation and
   see the same "translated on your device" badge as in game detail, with the same behavior when the device
   cannot translate.
3. **Given** the entity has no description, **When** the page opens, **Then** no description block and no
   empty placeholder is shown, and the grid follows the header directly.
4. **Given** the page is open, **When** the user looks at the games, **Then** they are shown as a grid of the
   same game cards as in search results, with no control to switch to a list.
5. **Given** a game in the grid, **When** the user taps, saves or long-presses it, **Then** it opens its
   detail, saves to a list or shows the long-press action exactly as a game found by title does, and the
   card reflects the saved state.
6. **Given** the user opened the page from search results, **When** they go back, **Then** they return to the
   results with the query, selected type and scroll position intact.
7. **Given** the entity has no games in the catalogue, **When** the page opens, **Then** an explanatory empty
   message replaces the grid.
8. **Given** the entity has many games (some studios have several hundred), **When** the page opens, **Then**
   every one of them is shown once they arrive, and the header and description are readable meanwhile.

---

### User Story 5 - Searching stays fast and forgiving (Priority: P3)

Adding two more types must not make Search feel slower or more fragile, and a problem on the detail page must
not strand the user.

**Why this priority**: A quality bar on the first four stories rather than a new capability.

**Independent Test**: Search each type with the connection cut and then restored, and open a detail page
whose games fail to load.

**Acceptance Scenarios**:

1. **Given** a submitted search of any type, **When** the lookup fails or times out, **Then** the results area
   shows the existing error state with a retry, and retry repeats only that search.
2. **Given** a detail page whose games fail to load, **When** the header and description are already shown,
   **Then** they stay visible, and the games area shows its own error with a retry.
3. **Given** the games of a detail page are slow, **When** the page opens, **Then** the header and description
   appear first and the games area shows a loading state until they arrive.

---

### Edge Cases

- A name that matches a game, a character and a company at once (a franchise): each type returns its own
  results when selected; nothing is merged or hidden.
- The same character appears in dozens of games, or a studio has hundreds: every game is shown, and the header
  and description do not wait for them.
- A character with no games, or a developer whose credited games are all editions or extras the app does not
  list: the card still shows and its page explains why the grid is empty.
- Very long names (a studio with a long legal name) and names in non-Latin scripts stay readable on both the
  card and the header without breaking the layout.
- Several entries with the same name (different characters called "Link", a studio and its subsidiaries): each
  is its own card in a stable order.
- A very long description is shown in full, as a game's description is, and scrolls with the page.
- Existing filters and sorting only make sense for games: they are offered on game results and are not
  offered on character or developer results, nor on the detail page.
- Changing type while a game filter is active: the game filters are kept for when the user returns to Games.
- A genre chosen from the genre browse while Characters or Developers is selected: the results are games, and
  the Games chip becomes selected so the chip row never contradicts the grid.
- A recent search tapped while Characters or Developers is selected: the text is searched for that type.
  History records the text only, not the type it was searched under.
- A recently viewed game tapped while Characters or Developers is selected: it opens that game's detail as
  today, and the selected type is unchanged.
- Typing in the bar while Characters or Developers is selected: the remote suggestions of matching games are
  not shown, because they would lead to games rather than to the type being searched; the user's own history
  suggestions still appear.
- A very short query, or one the existing screen already rejects, behaves as today for every type.
- Landscape and large windows keep working: the new cards and the detail page's grid follow the same column
  rules as game cards. The detail page is a full-screen page; opening a game from it behaves as opening a game
  from anywhere else.
- Suggestions and search history keep recording and offering what the user typed, as today.
- The user is offline: the existing offline behavior of Search applies to every type, and a detail page that
  cannot load its games shows the retry state.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The expanded search view MUST offer a search type selector as a row of three chips directly
  under the search bar: Games, Characters and Developers. Exactly one is selected at a time, and Games is the
  default.
- **FR-002**: Each chip MUST show an icon and its title, and the selected chip MUST show a check mark in place
  of its icon and be visibly highlighted.
- **FR-003**: The search bar's hint text MUST reflect the selected type.
- **FR-004**: A submitted search MUST look up only the selected type, and the results grid MUST contain only
  that type.
- **FR-005**: Selecting a type MUST keep the text in the bar and MUST NOT start a search by itself; the next
  submitted search, including one started from a recent search, uses the selected type.
- **FR-006**: Character results MUST be shown as cards with the portrait clipped to a circle and the name;
  developer results as cards with the logo on a tile and the name, in a shape different from both the game
  card and the circular character card.
- **FR-007**: When a character or company has no image, its card MUST show a neutral placeholder in the same
  shape.
- **FR-008**: Tapping a character card or a developer card MUST open a detail page for that entity.
- **FR-009**: The detail page MUST follow the wishlist detail screen's design: a header at the top, then a
  scrolling body.
- **FR-010**: The header MUST show the entity's image, its name and a game-count chip; for a developer it MUST
  also show the country and the founding year when the catalogue has them.
- **FR-011**: The body MUST show the entity's description above the games when one exists, and nothing in its
  place when none does.
- **FR-012**: The description MUST be translatable with the same actions, states and badge as a game's
  description, including the same behavior on a device that cannot translate.
- **FR-013**: The body MUST show the games the character appears in, or the games the developer created, as a
  grid of game cards. It MUST NOT offer a list view or a toggle to one.
- **FR-014**: The games in that grid MUST support opening the detail, saving to a list and the long-press
  action exactly as game cards do elsewhere. Every game MUST be shown however many there are, without
  delaying the header and description.
- **FR-015**: Going back from the detail page MUST return to the results with the query, selected type and
  scroll position intact.
- **FR-016**: The existing game filters and sorting MUST keep working exactly as today on game results, and
  MUST NOT be offered on character results, developer results or the detail page.
- **FR-017**: A failed lookup MUST show the existing error state with a retry; a failed games load on the
  detail page MUST keep the header and description visible and offer its own retry.
- **FR-018**: A search or a detail page with nothing to show MUST show an empty message worded for what is
  missing (no characters, no developers, no games).
- **FR-019**: All new text (type titles and descriptions, hint texts, empty and error messages, chip labels,
  accessibility descriptions) MUST be available in every language the app already supports.
- **FR-020**: The chip row, the new cards and the detail page MUST be usable with a screen reader: each chip
  announces its title and whether it is selected, and each card announces its kind and name and exposes its
  tap action.
- **FR-021**: The genre browse, recent searches, recently viewed games and the Discover feed MUST look and
  behave exactly as today whichever type is selected.
- **FR-022**: Choosing a genre from the genre browse always shows game results, so it MUST select the Games
  chip.

### Key Entities

- **Search type**: One of Games, Characters, Developers. Decides what a submitted search looks up and which
  card the grid shows. Has a title and an icon, shown on its chip.
- **Game**: Unchanged. A title with a cover, found by name.
- **Character**: A person, creature or avatar that appears in one or more games. Has a name, an optional
  portrait, an optional description and the games it appears in.
- **Developer (company)**: A company credited as the developer of at least one game. Has a name, an optional
  logo, an optional description, an optional country and founding year, and the games it created.
- **Entity detail page**: The page of one character or developer: header, description, grid of games.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user who knows a character's name finds a game that character appears in within four taps from
  the expanded search view (type chip, card, game), plus typing the name.
- **SC-002**: A user who knows a studio's name reaches that studio's games within four taps by the same path.
- **SC-003**: Results of a Characters or Developers search appear as fast as a Games search does today: one
  lookup per search for every type.
- **SC-004**: In a test with one grid of each type, users identify which cards are games, characters and
  developers correctly every time without reading the names.
- **SC-005**: On the detail page, the header and description are visible before the games finish loading, and
  a failure loading the games never hides them, in 100% of tested failure cases.
- **SC-006**: A user who translates a character's description gets the same experience as translating a game's,
  with no new controls to learn.
- **SC-007**: Existing search behavior with Games selected (title search, filters, sort, history, suggestions,
  Discover, genre browse) is unchanged: every existing search test still passes.

## Assumptions

- **Investigation result (the question in the original request)**: the catalogue service does not take one
  shared search term for all three types. Games and characters each have their own text-search lookup; the
  company text-search lookup returned nothing in testing, and companies are found by a name-contains match
  instead. The service's combined "search everything" lookup returns only names and references with no
  images, and surfaced characters but not companies. Batching several lookups into one request only worked with
  name-contains matches, not text search. The type selector makes this moot for the user: one lookup per
  search, so nothing needs to be batched or merged. The catalogue holds a description for characters and
  companies, a country and founding date for companies, and the list of games for each.
- Characters and developers are matched on their name only, the same way a game is matched on its title.
- "Developer" is a company in the catalogue credited as the developer of at least one game, matched on its
  name; a company that only published games is not a Developers result. The games on a developer's page are the
  games it developed.
- Characters use a circular portrait and developers use a rounded square tile with the logo fitted inside
  rather than cropped, since logos are not portraits. The exact tokens come from the existing design system.
- The detail page's header reuses the look of the wishlist detail header (tinted card, image on the left, name
  and chips beside it); the entity's description is not repeated in the header because it has its own block in
  the body.
- The games on the detail page are shown newest first, with games that have no release date last, and with no
  filter or sort controls.
- Descriptions come from the catalogue in its source language and are translated with the same on-device
  translation game detail already uses; no new translation engine or setting is introduced.
- The expanded view's content below the chip row is deliberately type-independent (see the clarifications):
  the genre browse and recently viewed games stay game-only content, and recent searches are shared by the
  three types because history stores only the text.
- Live suggestions while typing stay game-only, so they are the one thing the type changes below the bar:
  they are not shown for Characters or Developers. Suggestions for characters and developers are not part of
  this feature.
- The selected type is kept for the duration of the app session and Games is selected again on a fresh
  launch.
- iOS and Android get the same behavior, because the search screen and the new page are shared.
- Out of scope: searching publishers, platforms or franchises as their own types; following or favouriting a
  character or developer; showing a character's or developer's other facts (species, gender, websites);
  searching several types at once; and any change to the Discover feed.
