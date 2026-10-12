# Research: Genre Quick-Browse in Search

**Feature**: [spec.md](spec.md) | **Plan**: [plan.md](plan.md) | **Date**: 2026-10-12

The spec left no `[NEEDS CLARIFICATION]` markers. The open points below came out of reading the code the
feature touches — `SearchViewModel`, `SearchBars.kt`, `GameRepositoryImpl`, `GetTasteProfileUseCase` and
`SearchUiMapper` — and each is resolved here.

---

## R1. Where the genre results come from, and what "popular first" means

**Decision**: A new repository method, `GameRepository.getPopularGamesByGenre(genreId: Int)`, issues one
`/games` query:

```text
fields <the search field list>;
where genres = (<genreId>) & game_type != (<noisy types>) & version_parent = null & cover != null
    & total_rating_count >= 10 & first_release_date <= <now> & first_release_date > <now - 2 years>;
sort total_rating_count desc;
limit 500;
```

"Popular" is **`total_rating_count`**, sorted server-side. The repository returns the rows in that order and
does not cache them, like `searchGames`.

**Rationale**:
- IGDB has no popularity field on `/games` that can be filtered by genre. The Popularity API used by the
  Discover lanes (`/popularity_primitives`) ranks **game ids only** and knows nothing about genres. Using it
  here would mean pulling a large global pool and keeping only the games of one genre. A niche genre would
  get a handful of survivors out of 500, and two requests would be spent on it.
- `total_rating_count` counts how many people rated a game. It is what IGDB has that comes closest to "how
  many people played it", and it is a field the query can both filter and sort on. The same field already
  provides the rating floor, so one field serves FR-014 and FR-015.
- `hypes` is anticipation before release, and FR-013 rules unreleased games out.
- `first_release_date <= now` gives released games only (FR-013). The lower bound gives the recent window
  (FR-014). A game with no `first_release_date` fails both comparisons and drops out, which is right here:
  an undated game cannot be shown to be released.
- `cover != null`, the noisy game types and `version_parent = null` are the rules every catalogue grid in
  the app already applies (`fetchPopularityRankedGames`, `getGamesByGenre`). The grid is made of covers.
- `limit 500` is what `searchGames` asks for. The spec says pagination stays what a text search does (a
  single page). With the same cap, the filter sheet's match counts cover the same amount of data as for a
  text search. The window and the rating floor keep the real count far below the cap for most genres.
- Initial values: **a 2-year window** and **at least 10 ratings**, as the spec's Assumptions say. They are
  named constants in `GameRepositoryImpl` (`GENRE_BROWSE_RELEASE_WINDOW_YEARS`,
  `GENRE_BROWSE_MIN_RATING_COUNT`), each with a comment saying it is a tuning knob, the same way
  `RECOMMENDED_RELEASE_WINDOW_YEARS` is documented. The quickstart has a step to check them against live
  IGDB data.

**Alternatives considered**:
- *Reuse `getGamesByGenre`.* Rejected. It backs the Discover "Because you like X" shelf: it sorts by
  rating, admits unreleased games, uses a 10-year window and a floor of 3 ratings. Changing it would change
  a Discover lane, which the spec puts out of scope. Adding parameters to it would turn one documented
  contract into two contracts behind flags.
- *Rank with the Popularity API and filter by genre after hydrating.* Rejected for the reasons above: two
  requests, and a pool that thins badly for niche genres.
- *Sort by `total_rating` (the score).* Rejected. That orders by quality, not popularity, and a game scored
  100 by a dozen people would come first. "Popular first" is the requirement, and the "Rating" sort is
  still there for users who want the score.
- *Cache the result in the Discover lane tables.* Rejected. It is a search result, not a lane, and a text
  search is not cached either. Caching would also need a key per genre and an expiry policy that nothing
  asks for.

**Verified against live IGDB (2026-10-12)**:
- `/genres` returns 23 genres and every id/name pair matches the `KnownGenre` table in
  [data-model.md](data-model.md); nothing to correct and no genre is missing.
- `/games/count` with the contract's window and floor (without the `game_type` exclusion, so the real counts are
  slightly lower): RPG 150, Shooter 88, Adventure 289, Strategy 62, Indie 197, Quiz 3, Pinball 0. The five
  defaults each fill a grid, so the 2-year window and the 10-rating floor stay as they are. Pinball and Quiz are
  the thin case the spec's Edge Cases accept (empty state naming the genre).

---

## R2. Which genres the chips show

**Decision**: A new `GetBrowseGenresUseCase` in `core/domain/usecase/search/` returns `Flow<List<Genre>>`
with exactly five genres:

1. Build the taste profile from the saved games.
2. Keep the genres with a **positive** weight, ordered by weight (highest first), then by `count` (highest
   first), then by id (lowest first) so ties always come out in the same order. Take at most five.
3. Pad, in order and skipping ids already present, from the defaults `ROLE_PLAYING, SHOOTER, ADVENTURE,
   STRATEGY, INDIE` until there are five.

The profile is built by the same code `GetTasteProfileUseCase` uses. That code moves out of the class into
an `internal fun List<Game>.toTasteProfile()` in `usecase/discover/TasteProfileBuilder.kt`, and
`GetTasteProfileUseCase.invoke()` becomes `repository.getSavedGames().map { it.toTasteProfile() }` (with the
existing `EMPTY` short-circuit). `GetBrowseGenresUseCase` maps the same saved-games flow once and takes the
profile and each taste genre's `Genre` (id and IGDB name) from the same list of games, so the two cannot get
out of step.

**Rationale**:
- The spec says "ordered by signal strength". The signal's `weight` is the strength. `count` and then the id
  only break ties, and the id keeps the row from reshuffling between openings (spec Assumptions).
- Keeping only positive weights is what `GetDiscoverFeedUseCase.recommendationPlan` already does
  (`filterValues { it.weight > 0.0 }`). A dropped genre is a rejection, not a taste.
- `TasteProfile` holds genre **ids** only, and that is deliberate (its KDoc). The chips need a fallback name
  for a genre the app has no string for (R3), and the saved games carry those names. Reading the profile
  and the names from **one** emission of `getSavedGames()` avoids a `combine` of two Room flows, which can
  briefly pair a new profile with an old list of names.
- Moving `buildProfile` into a top-level function does not change behaviour, and `GetTasteProfileUseCaseTest`
  checks that. `:core:data` mappers already use the `internal` top-level extension function shape.
- The defaults are matched by id (FR-011). The ids come from the new `KnownGenre` enum (R3), so the domain
  and the UI read them from the same place.

**Alternatives considered**:
- *Inject `GetTasteProfileUseCase` and `combine` it with the saved games for the names.* Rejected because of
  the intermediate-emission mismatch described above.
- *Return ids only and let the UI label them.* Rejected. A taste genre that `KnownGenre` does not list would
  have no name to fall back to.
- *Add genre names to `TasteProfile`.* Rejected. The profile is keyed by id so that it survives an upstream
  rename. Names in it would go stale for no gain.
- *Apply a minimum `count` before a taste genre can take a chip.* Rejected. The spec wants a user with any
  saved games to see their own genres first, and one saved RPG is already a signal worth a chip.

---

## R3. Localised genre names

**Decision**:
- `:core:model` gets `enum class KnownGenre(val id: Int, val igdbName: String)` with IGDB's 23 genres and a
  `fromId(id: Int): KnownGenre?` that returns `null` for an id it does not know.
- `:core:ui` gets `mapper/GenreMapper.kt` with `fun Genre.toLabelUiText(): UiText`. A known id resolves
  through an exhaustive `when` to `UiText.StringResource(Res.string.genre_*)`. Any other id falls back to
  `UiText.DynamicString(name)`.
- `core/ui/.../strings.xml` gets 23 `genre_*` strings with **short** labels ("RPG", "Shooter", "Real-time
  strategy", "Hack and slash", …).

**Rationale**:
- The app ships only `values/` (English), so today nothing is translated. "Localised" means the label goes
  through a string resource, and adding a locale is then a resource-only change. That is the root
  `CLAUDE.md` test, *could this string ever be a resource?*, and IGDB's genre list is a fixed list.
- The spec's chips read "RPG", "Shooter", … and the placeholder reads "genre: RPG". IGDB's own names are
  long ("Role-playing (RPG)", "Hack and slash/Beat 'em up", "Real Time Strategy (RTS)") and would not fit a
  chip. Short resource labels solve both problems.
- An exhaustive `when` over an enum makes the compiler catch a genre added without a label. A `Map<Int,
  StringResource>` would not.
- `KnownGenre` lives in `:core:model` because both `:core:domain` (the default ids) and `:core:ui` (the
  labels) need the ids, and both already depend on `:core:model`. It is plain Kotlin, so `:core:model` stays
  free of Android and Compose. `GameType` already sets the precedent of an enum carrying an IGDB `id`.
- All 23 genres, not just the five defaults: a taste genre can be any of them, and the spec wants the chip
  label and the placeholder localised whatever the genre is.

**Alternatives considered**:
- *Strings for the five defaults only.* Rejected. A user whose taste leads with "Platform" would get a chip
  with IGDB's raw English name next to four translated ones.
- *Put the id table in `UiConstants`.* Rejected. The domain needs the default ids too, and `:core:domain`
  cannot see `:core:ui`.

**Related change, called out**: `SearchUiMapper.toGenreFilters()` switches from
`UiText.DynamicString(genre.name)` to `genre.toLabelUiText()`. Otherwise the filter sheet would show
"Role-playing (RPG)" while the chip and the placeholder say "RPG". This also changes the genre labels in
the filter sheet of a **text** search: they become the short labels. Reverting it is a one-line change.
The Discover "Because you like %1$s" title and the game detail screen keep IGDB's names. Discover lanes are
out of scope, and both are listed as follow-ups in the plan.

---

## R4. How a genre browse is held in state

**Decision**:
- `SearchUiState` gains `browseGenres: List<BrowseGenreUiModel>` (the chip row, empty until the first
  emission) and `activeGenre: BrowseGenreUiModel?` (`null` when no browse is active).
- `BrowseGenreUiModel(id: Int, label: UiText)` is `@Immutable`, in `model/BrowseGenreUiModel.kt`.
- A new event, `SearchUiEvent.OnGenreBrowseTriggered(genre: BrowseGenreUiModel)`.
- Results reuse `SearchContentState` unchanged: `Loading`, `Empty`, `Success` and `Error`, as for a text
  search (FR-016).

**Rationale**:
- `feature/CLAUDE.md` says there is one content state per independent content area. A genre browse shows in
  the same content area as a text search and replaces it, so it is not a third area. What differs is where
  the results came from, and only a few UI decisions read that (the placeholder, the clear button, the sort
  label, the empty-state text, retry). One nullable field carries it. `contentState` remains the only thing
  that decides what the content area shows.
- The invariant `activeGenre != null ⇒ contentState != Idle` holds because the only things that clear
  `contentState` (`clearSearch`) also clear `activeGenre`, and the only thing that sets `activeGenre`
  (`browseGenre`) also sets `contentState = Loading`.
- The event carries the whole model, the way `OnFilterClick(filter)` and `OnSortChanged(sort)` do, so the
  ViewModel needs no lookup that could miss.

**Alternatives considered**:
- *Add the genre to each `SearchContentState` case.* Rejected. All four cases would carry it, and every
  `when` would have to pass it through for a field that only the top bar reads.
- *A sealed `SearchSource { Query; Genre }` in the UI state.* Rejected. The text query already lives in the
  `TextFieldState`. Copying it into state would create a second source of truth for the same text.

---

## R5. Superseding a search that is still loading

**Decision**: Both `performSearch` and the new `browseGenre` cancel `searchJob` before they launch the new
one.

**Rationale**:
- Today `performSearch` replaces `searchJob` without cancelling the old job. Two requests in flight both
  write `contentState`, and whichever finishes **last** wins. FR-018 and SC-007 need the **most recent
  request** to win, and the browse makes this likely (tap a chip, change your mind, tap another one).
- Cancelling is enough. The use case suspends inside the Ktor call, and `toRepositoryError()` rethrows
  `CancellationException`, so a cancelled job never reaches `_uiState.update`.
- This also fixes the same race between two **text** searches. That is a change to existing behaviour,
  called out here because it is the shared code path the requirement runs through. It is not an unrelated
  cleanup, and `docs/tech-debt.md` does not list it.

**Alternatives considered**:
- *Tag each request with a sequence number and drop stale results.* Rejected: it does the job of
  cancellation, only worse, and keeps a wasted request running.

---

## R6. "Top picks" ordering and the sort label

**Decision**:
- The label: `SearchUiMapper` gains `List<SortingUiModel>.withRelevanceLabel(isGenreBrowse: Boolean)`. It
  rewrites only the `RELEVANCE` entry's label, to `Res.string.sort_top_picks` (new, in `:feature:search`)
  or to `CoreUiRes.string.sort_relevance`. `selected`, `descending` and `isSortActive` are not touched
  (FR-025, FR-026). The ViewModel applies it when a browse starts and when it ends (`performSearch`,
  `clearSearch`).
- The order: during a browse, `sortGames` with `RELEVANCE` keeps the **server order** when descending and
  reverses it when ascending. `calculateGameRelevanceScore(game, "")` is never called for a browse.
  `NAME`, `RATING` and `RELEASE_DATE` behave exactly as they do for a text search.
- For a browse, `Success.allGames` holds the games in **server order**, not after the first sort, so that
  re-applying "Top picks" after a filter or sort change gets the popularity order back.

**Rationale**:
- With no query, relevance has nothing to measure. The server's popularity order is the "top picks".
  Reversing it for ascending keeps the arrow meaning what it means everywhere else in the sheet.
- For a text search, `allGames` is stored after the first sort, which is harmless because every later sort is
  a total sort on a key. For a browse, the popularity order **is** the key, so the order it arrived in has
  to be kept. Filtering keeps the order, so `filterGames` needs no change.
- Leaving `selected` and `descending` alone is what FR-026 asks for. The `isSortActive` badge stays right
  because "Top picks, descending" is the default in exactly the way "Relevance, descending" is.

---

## R7. The chip's look and its accessibility semantics

**Decision**: A feature-local `GenreChip` (private, in `components/GenreBrowseSection.kt`) built on
Material's **`AssistChip`**. It is given the selected-filter-chip look from the same tokens
`CustomFilterChip` uses for its selected state: `appColors.chipSelectedContainerColor` and
`chipSelectedContentColor`, and a border of `chipSelectedBorderColor` that is 0 dp wide in dark theme and
1 dp in light. It uses `MaterialTheme.shapes.small` and has no icon. The row is `CustomChipRow`. A tap does
not call `bringIntoView`, because the overlay closes.

**Rationale**:
- FR-004 says the chip is announced as a button and **never** as "selected". `FilterChip`, and so
  `CustomFilterChip`, is a toggleable control. It sets a checkbox-like role and a selected state, so a
  screen reader would say "selected". `AssistChip` is a button: Compose gives it `Role.Button` and no
  selected state. So the semantics come out right with no override.
- Wrapping `CustomFilterChip(selected = true)` in `clearAndSetSemantics { role = Role.Button }` would also
  work. It relies on clearing a child's semantics, though, and `CustomFilterChip` takes no `modifier` to
  attach that to, so a wrapper `Box` would need its own click semantics. That is more fragile than using a
  component that is a button to begin with.
- The chip stays in `:feature:search` because it has one caller. Constitution IV says to check the shared
  layer first. Nothing there is a button styled as a selected chip, and a single caller does not justify a
  new shared component.
- `CustomChipRow` is mandatory for a new chip strip (`core/ui/CLAUDE.md`). It brings the fading edges and
  the horizontal scrolling that FR-001 needs.

---

## R8. The search bar: placeholder, clear button, back

**Decision**:
- `SearchInputField` takes an `activeGenreLabel: UiText?` parameter. The placeholder shows
  `stringResource(Res.string.genre_browse_placeholder, label.asString())` ("genre: %1$s") when a genre is
  set, and `search_placeholder` otherwise.
- The clear (X) button is shown when `textFieldState.text.isNotEmpty() || activeGenreLabel != null`
  (FR-021). It still sends `OnClearSearch` (FR-022).
- `SearchScreenContent`'s `BackHandler` is enabled when the bar is collapsed and
  `(text.isNotEmpty() || uiState.activeGenre != null)`, so the system back gesture returns to Discover as
  the on-screen arrow does. The arrow already appears for any `contentState` other than `Idle`, so it needs
  no change.
- Accessibility: today the placeholder `Text` is `clearAndSetSemantics {}`. During a browse, the input field
  gets `Modifier.semantics { stateDescription = <"genre: RPG"> }`, so TalkBack and VoiceOver announce the
  genre even though the field is empty. The default placeholder stays hidden from semantics as it is now.
- Tapping a chip goes through a `onBrowseGenre` lambda in `SearchScreenContent`, built like `onSearch`:
  reset the grid scroll, `searchBarState.animateToCollapsed()`, then send `OnGenreBrowseTriggered`. The
  ViewModel empties the text field (it is already empty, since the section only shows then) and clears the
  suggestions.

**Rationale**:
- SC-006 wants the browse to be readable from the bar alone, and that has to hold for screen-reader users
  too. A state description adds the information without bringing back the placeholder semantics that were
  removed on purpose.
- Without the `BackHandler` change, the system back gesture during a browse would leave Search instead of
  returning to the feed, unlike a text search.

---

## Values fixed by this plan

| Value | Initial | Where |
|-------|---------|-------|
| Release window | 2 years | `GENRE_BROWSE_RELEASE_WINDOW_YEARS`, `GameRepositoryImpl` |
| Minimum ratings | 10 (`total_rating_count`) | `GENRE_BROWSE_MIN_RATING_COUNT`, `GameRepositoryImpl` |
| Result cap | 500 (same as text search) | `GENRE_BROWSE_LIMIT`, `GameRepositoryImpl` |
| Chips | 5 | `BROWSE_GENRE_COUNT`, `GetBrowseGenresUseCase` |
| Defaults | RPG (12), Shooter (5), Adventure (31), Strategy (15), Indie (32) | `KnownGenre.BROWSE_DEFAULTS` |
