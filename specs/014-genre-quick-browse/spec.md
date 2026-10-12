# Feature Specification: Genre Quick-Browse in Search

**Feature Branch**: `014-genre-quick-browse`

**Created**: 2026-10-12

**Status**: Draft

**Input**: User description: "Genre quick-browse in Search. Today, finding games of a given genre requires typing a text query first, and the genre filter only narrows results that already exist. Users who just want 'a good RPG' have no entry point. Add one-tap genre browsing to the expanded search view: a 'Browse by genre' row of five genre chips (personalized from the user's taste profile, padded with fixed defaults) that, when tapped, shows a grid of recent, released, well-rated games of that genre, popular first."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Browse a genre in one tap (Priority: P1)

A user opens Search, taps the search bar, and — before typing anything — sees a "Browse by genre" row above
their recent searches. They tap "RPG". The overlay closes and the screen shows a grid of released RPGs, the
most popular first, on the same cards they already know from a text search. They can open a game, save it
to their default wishlist or long-press it to pick a list, exactly as they would from a text search result.

**Why this priority**: This is the whole feature. Today the only way to see "a good RPG" is to guess a
text query and then narrow it with a filter. One tap from the empty search bar is the entry point that was
missing.

**Independent Test**: Open the expanded search view with an empty field, confirm the "Browse by genre" row
is above the recent searches, tap a chip, and confirm the overlay closes and a grid of released games of
that genre appears, with loading, then results, using the same cards and the same save and long-press
actions as a text search.

**Acceptance Scenarios**:

1. **Given** the expanded search view with an empty field, **When** it opens, **Then** a "Browse by genre"
   section with five genre chips is shown above the recent searches.
2. **Given** the "Browse by genre" row is shown, **When** the user taps a chip, **Then** the overlay closes,
   a loading state is shown, and then a grid of games of that genre replaces the Discover feed.
3. **Given** a genre browse is showing results, **When** the user looks at them, **Then** every game is
   already released, none is unreleased or "to be announced", and the most popular games come first.
4. **Given** a genre browse is showing results, **When** the user taps a card, taps its save button or
   long-presses it, **Then** the game opens, is toggled in the default wishlist, or the list selector opens,
   respectively, exactly as for a text-search result.
5. **Given** the user is typing in the field (the field is not empty), **When** the suggestions are shown,
   **Then** the "Browse by genre" section is hidden, like the other empty-field sections.

---

### User Story 2 - Chips that reflect the user's taste (Priority: P1)

The five chips are not a fixed list for everyone. A user whose saved games lean towards strategy and
roguelikes sees those genres first; a brand-new user, with nothing saved yet, sees a sensible default set.
Either way the row always offers five genres, so it never looks broken or half empty.

**Why this priority**: Personalization is what makes the row more useful than a static menu, and the
fallback is what makes it work on day one. Both are required for the row to be shippable.

**Independent Test**: With no saved games, confirm the chips are exactly RPG, Shooter, Adventure, Strategy,
Indie in that order. Save games of a few other genres and confirm those genres lead the row, strongest first,
followed by the remaining defaults, with no duplicates and five chips in total.

**Acceptance Scenarios**:

1. **Given** a user with no saved games, **When** the row is shown, **Then** the chips are RPG, Shooter,
   Adventure, Strategy, Indie, in that order.
2. **Given** a user whose taste profile has at least five genres, **When** the row is shown, **Then** the
   chips are the five strongest taste genres, strongest first, and no default is shown.
3. **Given** a user whose taste profile has fewer than five genres, **When** the row is shown, **Then** the
   taste genres come first, strongest first, followed by defaults in their fixed order until there are
   five chips.
4. **Given** a taste genre that is also one of the defaults, **When** the row is shown, **Then** that genre
   appears once, in its taste position, and the defaults that pad the row skip it.
5. **Given** the user saves or removes games, **When** they next open the expanded search view, **Then**
   the row reflects their updated taste.

---

### User Story 3 - Know you are browsing, and get back out (Priority: P1)

After tapping a chip the search bar text stays empty, so the screen has to say what it is showing and give a
clear way out. The bar's placeholder switches from its default to "genre: RPG" (with the genre's localized
name). The clear (X) button is visible even though the field is empty. Tapping X, or the back arrow, returns
the user to the Discover feed and restores the default placeholder.

**Why this priority**: Without this the grid has no label and no exit — the user would be looking at results
for a search they cannot see. It is part of the minimum shippable feature.

**Independent Test**: Tap a genre chip and confirm the placeholder reads "genre: <name>" and the X button is
visible with an empty field; tap X and confirm the Discover feed and the default placeholder return; repeat
with the back arrow.

**Acceptance Scenarios**:

1. **Given** a genre browse is active, **When** the screen is shown, **Then** the search bar text is empty
   and its placeholder reads "genre: <genre name>" with the genre name localized.
2. **Given** a genre browse is active and the field is empty, **When** the screen is shown, **Then** the
   clear (X) button is visible.
3. **Given** the field has text and no genre browse is active, **When** the screen is shown, **Then** the
   clear (X) button is visible, as today.
4. **Given** the field is empty and no genre browse is active, **When** the screen is shown, **Then** the
   clear (X) button is not visible, as today.
5. **Given** a genre browse is active, **When** the user taps the clear (X) button, **Then** the Discover
   feed is shown and the default placeholder is restored.
6. **Given** a genre browse is active, **When** the user taps the existing back arrow, **Then** the same
   thing happens: the Discover feed and the default placeholder return.
7. **Given** a genre browse is active, **When** the user types a new query and submits it, **Then** the
   text-search results replace the genre browse and the default placeholder is restored.
8. **Given** a genre browse is active, **When** the user expands the search view again and taps a different
   genre chip (or a recent search), **Then** that new browse or search replaces the current one.

---

### User Story 4 - Sort and filter a genre browse sensibly (Priority: P2)

The sort and filter sheets keep working during a genre browse, adjusted so they make sense. The sort option
that is called "Relevance" for a text search is called "Top picks" while browsing a genre, because there is
no query to be relevant to. Filters no longer offer the genre being browsed, since every result already has
it, but everything else still narrows the grid.

**Why this priority**: The browse is usable without these refinements, but leaving a "Relevance" label with
no query, or a genre filter that is already satisfied, would feel wrong. It refines the P1 flow rather than
enabling it.

**Independent Test**: During a genre browse open the sort sheet and confirm the first option reads "Top
picks"; pick another sort and direction, leave the browse, and confirm the label reads "Relevance" again with
the sort and direction untouched. Open the filter sheet and confirm the browsed genre is missing from the
genre group while other genres, platforms and game types are offered and narrow the grid.

**Acceptance Scenarios**:

1. **Given** a genre browse is active, **When** the user opens the sort sheet, **Then** the option normally
   labelled "Relevance" is labelled "Top picks".
2. **Given** no genre browse is active, **When** the user opens the sort sheet, **Then** that option is
   labelled "Relevance".
3. **Given** the user selected a sort and a direction, **When** a genre browse starts or ends, **Then** the
   selection is unchanged; only the label of the first option changes.
4. **Given** a genre browse is active, **When** the user opens the filter sheet, **Then** the browsed genre
   is not offered in the genre group, while the remaining genres, platforms and game types are.
5. **Given** a genre browse is active, **When** the user selects further genres, platforms or game types and
   applies them, **Then** the grid narrows to games that match the browsed genre **and** all the selected
   filters (the genre filter stays an AND).
6. **Given** a genre browse is active, **When** it ends (clear, back arrow or a new query), **Then** the
   genre group in the filter sheet offers every genre again.

---

### User Story 5 - A genre browse leaves no trace in history (Priority: P2)

A genre browse is a way of looking around, not a query the user typed. It must not show up among the recent
searches, so those stay a record of what the user actually searched for.

**Why this priority**: Without this the recent-searches list would fill with a placeholder-like entry or an
empty string. It is a correctness guard on the P1 flow.

**Independent Test**: Note the recent searches, run several genre browses, reopen the expanded search view,
and confirm the recent searches are exactly as before.

**Acceptance Scenarios**:

1. **Given** the user starts a genre browse, **When** they reopen the expanded search view, **Then** no
   entry for that browse appears in the recent searches.
2. **Given** a genre browse is active, **When** the user types and submits a text query, **Then** that query
   is recorded in the recent searches as any text search would be.

---

### Edge Cases

- **A genre with no results in the window**: the grid shows the same empty state a text search shows when it
  finds nothing, worded so it makes sense without a typed query (it names the genre, not an empty string).
  If filters are active it offers to clear them, as a text search does.
- **Offline or HTTP failure**: the same error state as a text search is shown, with a Retry action. Retry
  re-runs the same genre browse (same genre, same sort; filters start unselected, as for any new result set),
  not a text search of an empty query.
- **Taste profile with fewer than five genres**: padded with defaults until there are five (User Story 2).
- **A taste genre that is also a default**: appears once, in its taste position; padding skips it (User
  Story 2, scenario 4).
- **A taste genre with no usable signal**: a genre the user has only dropped games from (negative signal)
  is not treated as something they like and is not offered on the strength of that signal.
- **Tapping a chip while a previous search is still loading**: the earlier request is superseded. Only the
  results for the most recently tapped genre are ever shown; the user never sees the earlier results flash
  in, and never sees results that do not match the placeholder.
- **Tapping a chip while a text search is showing**: the genre browse replaces the text search, and the
  default placeholder is replaced by the genre placeholder.
- **Tapping the chip for the genre already being browsed**: the overlay closes and the same genre browse is
  shown; nothing is left in an inconsistent state.
- **Rotating during a genre browse**: the browse survives: the same genre, results, placeholder, sort and
  filters are shown after the rotation, without the Discover feed flashing in between.
- **Backgrounding during a genre browse**: returning to the app within the same session shows the same
  browse. Relaunching the app after the process was ended starts at the Discover feed; the genre choice is
  not persisted (out of scope).
- **Typing without submitting while a browse is active**: the browse stays on screen until a query is
  submitted. If the user deletes what they typed, the genre placeholder is shown again.
- **Active filter on the browsed genre**: a browse starts with no filter selected (see Assumptions), so a
  filter on the browsed genre can never be carried into it; and since that genre is not offered in the filter
  sheet, it cannot be selected during the browse either.
- **Recent window or minimum ratings leaves a thin pool**: a niche genre may show few results or none; this
  is an accepted consequence of keeping obscure and ancient entries out.
- **Accessibility**: each chip is announced by screen readers as a button carrying the genre name, and is
  never announced as "selected", even though it is drawn like a selected filter chip. The clear (X) button,
  when shown for a browse, keeps its usual spoken label.
- **Long genre names and narrow screens**: the chip row scrolls horizontally rather than wrapping or
  truncating a name; every chip keeps a comfortable touch target.
- **Both platforms**: Android and iOS offer the same row, the same ordering and the same behavior.

## Requirements *(mandatory)*

### Functional Requirements

**Genre row**

- **FR-001**: The expanded search view, when the field is empty, MUST show a "Browse by genre" section above
  the recent searches, containing a single horizontally scrolling row of exactly five genre chips.
- **FR-002**: The section MUST be hidden whenever the field contains text, like the other empty-field
  sections.
- **FR-003**: The genre chips MUST be visually distinct from the recent-search chips: drawn like a selected
  filter chip and carrying no icon.
- **FR-004**: Assistive technology MUST announce each genre chip as a button with the genre name. It MUST
  NOT announce a chip as "selected".
- **FR-005**: The section title and every user-visible string of the feature MUST be localized like the rest
  of the app; the genre names MUST be shown in the app's language.

**Which genres**

- **FR-006**: The row MUST take the user's strongest genres from their taste profile (derived from their
  saved games), ordered by signal strength, strongest first, up to five.
- **FR-007**: When the taste profile yields fewer than five genres, the row MUST be padded, in order and
  without duplicates, from the fixed defaults RPG, Shooter, Adventure, Strategy, Indie until it has five
  chips.
- **FR-008**: A user with no saved games MUST see exactly the five defaults, in that order.
- **FR-009**: A genre MUST appear at most once in the row. When a taste genre is also a default, it keeps its
  taste position and the padding skips it.
- **FR-010**: The row MUST always contain five chips; it MUST NOT be empty or partially filled because the
  taste profile is small or unavailable.
- **FR-011**: Genres MUST be identified by a stable identifier, never by their displayed name, wherever they
  are selected, compared, de-duplicated or excluded, so a genre is recognized in any language.

**Browsing**

- **FR-012**: Tapping a genre chip MUST close the overlay and show a grid of games of that genre in the
  search content area, replacing the Discover feed or any text-search results.
- **FR-013**: The results MUST contain only games that are already released and MUST NOT contain unreleased
  or unannounced-date games.
- **FR-014**: The results MUST be limited to a recent release window and to games with at least a minimum
  number of user ratings, so obscure or ancient entries do not appear. The initial values are in
  Assumptions.
- **FR-015**: The results MUST be ordered with the most popular games first by default.
- **FR-016**: The results MUST use the same game cards, save button, long-press list selector, loading
  state, empty state and error state as a text search, including the same pagination behavior.
- **FR-017**: The Retry action on a failed genre browse MUST re-run the same genre browse, with the same
  genre and sort. Filters start unselected, as for any new result set (FR-028).
- **FR-018**: When a chip is tapped while an earlier search or browse is still loading, only the results of
  the most recent request MUST ever be shown.
- **FR-019**: The empty state of a genre browse MUST read sensibly without a typed query.

**Search bar**

- **FR-020**: During a genre browse the search bar text MUST remain empty and its placeholder MUST read
  "genre: <genre name>" with the genre name localized. Outside a genre browse the default placeholder MUST
  be shown.
- **FR-021**: The clear (X) button MUST be visible whenever the field has text **or** a genre browse is
  active, and hidden otherwise.
- **FR-022**: Tapping the clear (X) button, or the existing back arrow, during a genre browse MUST return to
  the Discover feed and restore the default placeholder.
- **FR-023**: Submitting a typed query during a genre browse MUST replace the genre browse with the
  text-search results and restore the default placeholder.
- **FR-024**: Starting a genre browse MUST NOT write anything to the search history.

**Sort and filter sheets**

- **FR-025**: During a genre browse the sort option normally labelled "Relevance" MUST be labelled "Top
  picks"; at all other times it MUST be labelled "Relevance".
- **FR-026**: Starting or ending a genre browse MUST NOT change the user's selected sort or direction; only
  the label changes.
- **FR-027**: During a genre browse the filter sheet MUST NOT offer the browsed genre in its genre group.
  The remaining genres, platforms and game types MUST be offered and MUST narrow the results, with the
  genre filter remaining an AND.
- **FR-028**: A genre browse MUST start with no filter selected, as a new text search does, so no filter on
  the browsed genre can be active during it.
- **FR-029**: When the genre browse ends, the filter sheet MUST offer every genre again.

**Lifecycle**

- **FR-030**: A genre browse MUST survive device rotation, keeping the same genre, results, placeholder,
  sort and filters.
- **FR-031**: The chosen genre MUST NOT be persisted beyond the running session; a relaunch starts at the
  Discover feed.
- **FR-032**: The feature MUST behave identically on Android and iOS.

### Key Entities *(include if feature involves data)*

- **Genre browse**: The transient mode in which Search shows games of one genre instead of a text query's
  results. It holds the browsed genre and lives only for the current session; it is not a search query and is
  never stored in history. It ends on clear, back arrow, or a submitted text query, and is replaced by a new
  chip tap.
- **Genre chip**: One entry of the "Browse by genre" row — a genre identified by its ID, with a localized
  display name. The row holds exactly five, ordered taste-first, then defaults.
- **Taste profile**: The existing inference of the user's preferred genres from their saved games, with a
  signal strength per genre. This feature only reads it; it does not change how it is built.
- **Default genres**: The fixed fallback set — RPG, Shooter, Adventure, Strategy, Indie — used to pad the row.
- **Browse window**: The pair of limits that keeps results fresh and credible: a recent release window and a
  minimum number of ratings.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can go from the empty expanded search view to a grid of games in a chosen genre with a
  single tap.
- **SC-002**: With no saved games, 100% of users see exactly the five default genres in the stated order;
  with any saved games, the row always has five distinct genres, led by their strongest ones.
- **SC-003**: In 100% of genre browses, every game shown is already released and meets the rating minimum, and
  none is outside the release window.
- **SC-004**: A genre browse leaves the recent-searches list unchanged in 100% of cases.
- **SC-005**: A user can leave a genre browse and be back on the Discover feed with the default placeholder
  in one tap, via either the clear button or the back arrow.
- **SC-006**: Whenever a genre browse is active, the user can tell which genre it is from the search bar alone,
  without any text typed into it.
- **SC-007**: Rapidly tapping several different chips never leaves the screen showing results that do not
  match the genre in the placeholder.
- **SC-008**: A screen-reader user hears every genre chip as a button with its name and never hears
  "selected" for it.
- **SC-009**: A failed genre browse can be retried in one tap and the retry reloads the same genre, not a
  text search.
- **SC-010**: Rotating the device during a genre browse never drops the user back to the Discover feed.

## Assumptions

- **Release window and rating minimum**: Initial values are a release window of the last 2 years and a
  minimum of 10 user ratings. They are tunable knobs, not product decisions, and should be validated against
  real catalogue data before the quality of the grid is treated as settled (the same caveat the roadmap notes
  for the Discover thresholds). The plan fixes the final values.
- **"Strongest" genres**: Signal strength is the existing taste signal. Only genres with a positive signal are
  taken from the taste profile; ties are broken deterministically so the row does not reorder between
  openings.
- **Defaults are matched by stable identifier**, not by name, so the five defaults stay the right genres if
  upstream names change or a locale renders them differently.
- **Genre names**: The five defaults have app-provided localized names. A taste genre the app has no
  translation for falls back to the name the game data source provides.
- **Popular-first** means ordered by the data source's popularity measure, not by rating. The "Top picks"
  label is the user-facing name for that default order. The other sorts (name, rating, release date) work on
  a genre browse as they do on a text search.
- **Filters reset when a browse starts**, exactly as they do when a new text query is submitted: every new
  result set starts with no filter selected, built from the facets of its own results. The user then narrows
  the browse in the filter sheet. (Corrected during planning: the earlier wording said active filters carry
  over, which is not what a text search does.)
- **Pagination** is whatever a text search already does; no new paging behavior is introduced.
- **Taste profile availability**: If the taste profile cannot be read, the row falls back to the defaults
  rather than hiding or failing.
- **Out of scope**: upcoming and unreleased games, a mode toggle between released and upcoming, persisting the
  chosen genre, any change to the Discover lanes, and pagination beyond what a text search does.
- **Dependencies**: Reuses the existing Search screen, game cards, list selector, sort and filter sheets, the
  taste profile built from saved games, and the existing game data source; no new data source is introduced.
  Nothing outside Search changes.
