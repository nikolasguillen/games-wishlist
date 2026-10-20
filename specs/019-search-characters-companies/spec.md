# Feature Specification: Search by Character and Developer

**Feature Branch**: `019-search-characters-companies`

**Created**: 2026-10-20

**Status**: Draft

**Input**: User description: "I want to add search by game character and search by developer. Search api accepts character and company params. Find out if we can pass the same query to all three params or we have to make 3 different api calls for games, companies and characters. In search grid, characters and companies layouts should differ from games cards. The image should be inside a circle for characters, and maybe a different shape for companies. If we find out that we can search multiple things with a single api call, we'll have to understand how to reorganize the search results grid in order to show the new results"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Find games through a character (Priority: P1)

A user remembers a character but not the game: "the guy with the blades on his arms", "Kratos", "Link". They
type the character's name in Search and submit. Besides the games whose title matches, the results now include
the characters whose name matches, each shown as a card with the character's portrait inside a circle and the
character's name. The user taps the character and sees the games that character appears in, then opens one of
them like any other game.

**Why this priority**: This is the headline of the feature. Today a character name that is not part of a game
title finds nothing, and the game is simply unreachable from Search. Characters are the smallest slice that
delivers value on its own, and they are also the part where the new card shape matters most.

**Independent Test**: Search for "Kratos". Confirm that a character card with a circular portrait appears,
tap it, and confirm the games Kratos appears in are listed and can be opened and added to a list as usual.

**Acceptance Scenarios**:

1. **Given** the user submits a search for a name that belongs to a character, **When** the results load,
   **Then** that character appears as a character card (circular portrait, name) in the results.
2. **Given** a character card is on screen, **When** the user taps it, **Then** the games that character
   appears in are shown, and each can be opened, saved or long-pressed exactly like a game found by title.
3. **Given** a character has no portrait, **When** its card is shown, **Then** a neutral placeholder fills
   the circle instead of an empty or broken image.
4. **Given** a search that matches no character, **When** the results load, **Then** no character card and
   no empty character section appears.

---

### User Story 2 - Find games through a developer (Priority: P1)

A user wants everything from a studio: "Naughty Dog", "FromSoftware". They type the studio's name and submit.
The results include the companies whose name matches, each shown as a card with the company's logo and name,
in a shape that is visibly different from both game cards and character cards. Tapping a company shows the
games that company developed.

**Why this priority**: It is the second half of the request and as valuable as the first, but it can be
built and tested independently: it needs its own card and its own list of games, and does not depend on the
character work.

**Independent Test**: Search for "Naughty Dog". Confirm a company card shows up with the logo, tap it, and
confirm the studio's games are listed.

**Acceptance Scenarios**:

1. **Given** the user submits a search for a name that belongs to a company, **When** the results load,
   **Then** that company appears as a company card (logo, name) in the results.
2. **Given** a company card is on screen, **When** the user taps it, **Then** the games that company
   developed are shown, and each can be opened, saved or long-pressed like any other game.
3. **Given** a company has no logo, **When** its card is shown, **Then** a neutral placeholder is shown in
   the logo's place.
4. **Given** the three kinds of card in the same results, **When** the user looks at them, **Then** a game, a
   character and a company can be told apart at a glance by their shape alone.

---

### User Story 3 - Read mixed results without confusion (Priority: P2)

A single search can now return games, characters and companies at once ("Link" matches games, several
characters and companies with that word in their name). The user needs to see all three kinds without games being buried, and to look at
only one kind when they know what they want.

**Why this priority**: Without this the first two stories would dump three kinds of card into one unlabelled
grid. It is part of the same release, but it is the organizing layer, not the capability itself.

**Independent Test**: Search for a term that matches all three kinds. Confirm the results are clearly
separated by kind, that games are still the first thing on screen, and that the user can switch to only
characters or only companies and back.

**Acceptance Scenarios**:

1. **Given** a search that matches games, characters and companies, **When** the results load, **Then** each
   kind is clearly labelled and games come first.
2. **Given** a search that matches only one kind, **When** the results load, **Then** only that kind is
   shown, with no empty sections or headers for the others.
3. **Given** mixed results, **When** the user chooses to look at one kind only, **Then** the grid shows only
   that kind and the user can return to the combined view.
4. **Given** a search that matches nothing of any kind, **When** the results load, **Then** the existing
   empty-results message is shown.

---

### User Story 4 - Searching stays fast and forgiving (Priority: P3)

Adding two more kinds of result must not make Search feel slower, and a problem fetching one kind must not
take the others down with it.

**Why this priority**: A quality bar on the first three stories rather than a new capability.

**Independent Test**: Run the same search before and after the feature and compare how soon the games appear;
then cut the connection for one kind of result and confirm the others still show.

**Acceptance Scenarios**:

1. **Given** a submitted search, **When** the games are ready before the characters or companies, **Then** the
   games are shown without waiting for the others, and the other kinds fill in when they arrive.
2. **Given** the fetch for characters or companies fails or times out while games succeed, **When** the
   results are shown, **Then** the games are shown normally and the missing kind is reported as unavailable
   with a way to retry it, rather than turning the whole search into an error.
3. **Given** the fetch for games fails, **When** characters or companies succeed, **Then** those are shown and
   the games section reports its own failure.

---

### Edge Cases

- A name that matches a game, a character and a company at once (a franchise name): all three kinds show,
  games first, none hidden by the others.
- The same character appears in dozens of games, or a studio has hundreds: the list of games behind the card
  must be browsable without loading everything at once, and must say when it is long.
- A character or company with no games in the catalogue: the card still shows, tapping it shows an
  explanatory empty message instead of a blank screen.
- Very long names (a studio with a long legal name) and names with non-Latin characters must stay readable on
  their card without breaking the grid.
- Several entries with the same name (different characters called "Link", a studio and its subsidiaries):
  each is its own card, in a stable order.
- Existing filters and sorting only make sense for games: they must not be offered, or must clearly not
  apply, while the user looks at characters or companies.
- A very short query, or one the existing screen already rejects, behaves exactly as today for all three
  kinds.
- Landscape and two-pane layouts (large windows) keep working: the new cards adapt to the same column rules as
  game cards, and opening a game from the games behind a character or company behaves like opening any game.
- Search history and suggestions keep recording and offering what the user typed; the suggestions themselves
  stay game suggestions.
- The user is offline: the existing offline behavior of Search applies to all three kinds.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: A submitted search MUST look for games, characters and companies whose name matches what the
  user typed, and show every kind that has at least one match.
- **FR-002**: A character result MUST be shown as a card with the character's portrait clipped to a circle and
  the character's name.
- **FR-003**: A company result MUST be shown as a card with the company's logo and name, in a shape different
  from both the game card and the circular character card.
- **FR-004**: When a character or company has no image, the card MUST show a neutral placeholder in the same
  shape.
- **FR-005**: Tapping a character MUST show the games that character appears in; tapping a company MUST show
  the games that company developed.
- **FR-006**: The games shown after tapping a character or company MUST support everything a game found by
  title supports today: opening its detail, saving it to a list, and the long-press action.
- **FR-007**: Going back from the games of a character or company MUST return the user to the search results
  they came from, with the query and scroll position intact.
- **FR-008**: Mixed results MUST be organized so each kind is clearly labelled, games come first, and a kind
  with no matches shows no header and no empty placeholder.
- **FR-009**: The user MUST be able to look at a single kind of result and return to the combined view.
- **FR-010**: Games MUST NOT wait for characters or companies: the games appear as soon as they are ready, and
  the other kinds fill in as they arrive.
- **FR-011**: A failure fetching one kind MUST NOT hide the kinds that succeeded; the failed kind MUST report
  its own failure with a way to retry only that kind.
- **FR-012**: The existing game filters and sorting MUST keep working on games exactly as today, and MUST NOT
  be applied to, or offered for, characters and companies.
- **FR-013**: A search with no match of any kind MUST show the existing empty-results message.
- **FR-014**: All new text (kind labels, empty and error messages, accessibility descriptions of the cards)
  MUST be available in every language the app already supports.
- **FR-015**: Character and company cards MUST be usable with a screen reader: each announces its kind and name
  and exposes the tap action.
- **FR-016**: Search history, suggestions, the Discover feed and the genre browse MUST behave exactly as today.

### Key Entities

- **Game**: Unchanged. A title with a cover, found by name.
- **Character**: A person, creature or avatar that appears in one or more games. Has a name, an optional
  portrait, and the games it appears in.
- **Company**: A studio or publisher. Has a name, an optional logo, and the games it developed.
- **Search results**: The combined outcome of one submitted search: up to three groups (games, characters,
  companies), each of which can be loading, filled, empty or failed on its own.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user who types a character's name finds a game that character appears in within three taps
  after submitting the search (card, then game list, then game).
- **SC-002**: A user who types a studio's name reaches that studio's games within three taps after submitting
  the search.
- **SC-003**: The games of a search appear no later than they do today; adding characters and companies adds
  no perceptible wait before the first game is visible.
- **SC-004**: In a test with mixed results, users identify which cards are games, characters and companies
  correctly every time without reading the labels.
- **SC-005**: A failure or timeout in the character or company lookup never prevents games from showing, in
  100% of tested failure combinations.
- **SC-006**: Existing search behavior (games by title, filters, sort, history, suggestions, Discover, genre
  browse) is unchanged: every existing search test still passes.

## Assumptions

- **Investigation result (the question in the request)**: the catalogue service does not take one shared
  search term for all three kinds. Games and characters each have their own text-search lookup; the
  company text-search lookup returned nothing in testing, and companies are found by a name-contains match
  instead. The service's combined "search everything" lookup returns only names and references, no images,
  and surfaced characters but not companies, so it cannot build the new cards on its own. The service can
  batch several lookups into one request, but in testing the text-search lookup did not work inside a
  batch, only name-contains matches did. The practical consequence is three separate lookups (games,
  characters, companies) per submitted search, which is why the games must not wait for the others
  (FR-010) and why each kind fails independently (FR-011). How the lookups are issued, and whether batching
  is worth the change in ranking it would bring, is a decision for the plan.
- Characters and companies are matched on their name only, the same way a game is matched on its title.
- "Developer" means any company in the catalogue whose name matches; the catalogue does not separate studios
  from publishers in a way that is reliable for name search, so the games behind a company card are the
  games it developed.
- Characters use a circular portrait and companies use a rounded square tile, with the logo fitted inside
  rather than cropped, since logos are not portraits. The exact tokens come from the existing design system.
- The three lookups run when a search is submitted, as game search does today. Live suggestions while typing
  stay game-only.
- Result counts per kind are capped to a first page, with the same paging behavior the game results have
  today; the combined view shows a short preview of characters and companies.
- Existing localisations are extended with the new strings; no new language is added.
- iOS and Android get the same behavior, because the search screen is shared.
- Out of scope: searching by publisher, platform or franchise as separate kinds; character or company detail
  pages with biographies or full filmographies; following a character or company; and any change to the
  Discover feed.
