# Data Model: Genre Quick-Browse in Search

**Feature**: [spec.md](spec.md) | **Plan**: [plan.md](plan.md) | **Research**: [research.md](research.md)

Nothing is persisted. There is no Room entity, no DataStore key and no schema change: the browse lives in
the ViewModel for the session only (FR-031). Below are the new and changed types, layer by layer.

---

## `:core:model`

### `KnownGenre` (new enum, `KnownGenre.kt`)

The IGDB genres the app has a label for. Plain Kotlin: no Android, no Compose.

| Field | Type | Meaning |
|-------|------|---------|
| `id` | `Int` | IGDB genre id. Every comparison uses it (FR-011). |
| `igdbName` | `String` | IGDB's own name. A data value, used only as the `Genre.name` of a default genre; never displayed directly (the UI resolves labels by id). |

`companion fun fromId(id: Int): KnownGenre?` returns `null` for an id the enum does not list. There is no
fallback entry: the caller decides what an unknown genre becomes.

`companion val BROWSE_DEFAULTS: List<KnownGenre>` is `[ROLE_PLAYING, SHOOTER, ADVENTURE, STRATEGY, INDIE]`,
in that order (FR-007). It is the single source for the fixed fallback: the ViewModel seeds the row with it
and `GetBrowseGenresUseCase` pads from it.

| Entry | `id` | `igdbName` | Label (`core/ui` `genre_*`) |
|-------|-----:|------------|------------------------------|
| `POINT_AND_CLICK` | 2 | Point-and-click | Point-and-click |
| `FIGHTING` | 4 | Fighting | Fighting |
| `SHOOTER` | 5 | Shooter | Shooter |
| `MUSIC` | 7 | Music | Music |
| `PLATFORM` | 8 | Platform | Platformer |
| `PUZZLE` | 9 | Puzzle | Puzzle |
| `RACING` | 10 | Racing | Racing |
| `REAL_TIME_STRATEGY` | 11 | Real Time Strategy (RTS) | Real-time strategy |
| `ROLE_PLAYING` | 12 | Role-playing (RPG) | RPG |
| `SIMULATOR` | 13 | Simulator | Simulation |
| `SPORT` | 14 | Sport | Sports |
| `STRATEGY` | 15 | Strategy | Strategy |
| `TURN_BASED_STRATEGY` | 16 | Turn-based strategy (TBS) | Turn-based strategy |
| `TACTICAL` | 24 | Tactical | Tactical |
| `HACK_AND_SLASH` | 25 | Hack and slash/Beat 'em up | Hack and slash |
| `QUIZ_TRIVIA` | 26 | Quiz/Trivia | Quiz |
| `PINBALL` | 30 | Pinball | Pinball |
| `ADVENTURE` | 31 | Adventure | Adventure |
| `INDIE` | 32 | Indie | Indie |
| `ARCADE` | 33 | Arcade | Arcade |
| `VISUAL_NOVEL` | 34 | Visual Novel | Visual novel |
| `CARD_AND_BOARD_GAME` | 35 | Card & Board Game | Card & board game |
| `MOBA` | 36 | MOBA | MOBA |

The ids are verified against IGDB's live `/genres` endpoint before the strings are written (see
[quickstart.md](quickstart.md), step 1).

### Unchanged, but relied on

- `Genre(id, name)` — what the chip use case returns and what `toLabelUiText()` maps.
- `TasteProfile` / `TasteSignal` — read for ordering, unchanged (keys stay ids).
- `SearchResult(games, platforms, genres)` — the browse returns the same type as a text search.

---

## `:core:domain`

### `GameRepository.getPopularGamesByGenre(genreId: Int): AppResult<List<Game>>` (new)

Released games of one genre, most rated first, inside the recent window and above the rating floor. Network
only, not cached. Full contract in [contracts/genre-browse.md](contracts/genre-browse.md).

### `List<Game>.toTasteProfile(): TasteProfile` (new `internal` function, `usecase/discover/TasteProfileBuilder.kt`)

The body of today's `GetTasteProfileUseCase.buildProfile` plus its helpers (`gameWeight`, `toSignals`,
`addScore`, `addCount`, and the two recency constants), moved out unchanged. `GetTasteProfileUseCase` calls
it. Behaviour is identical, and `GetTasteProfileUseCaseTest` shows it.

### `GetBrowseGenresUseCase` (new, `usecase/search/`)

`operator fun invoke(): Flow<List<Genre>>`, which always emits exactly five distinct genres.

Rules, applied to every emission of `repository.getSavedGames()`:

1. `profile = games.toTasteProfile()` (or `TasteProfile.EMPTY` when `games` is empty).
2. `names = games.flatMap { it.genres }.associateBy { it.id }`.
3. Taste genres = `profile.genres` with `weight > 0.0`, sorted by `weight` desc, then `count` desc, then `id`
   asc, limited to `BROWSE_GENRE_COUNT` (5), each mapped to `names[id]`. An id with no name in `names` is
   skipped. That cannot happen with a consistent emission; the check is only a guard.
4. Pad from `KnownGenre.BROWSE_DEFAULTS` (as `Genre(k.id, k.igdbName)`), skipping ids already present, until
   there are five.
5. `distinctUntilChanged()`, so a library change that does not move the ranking produces no new list.
6. If the saved-games flow throws, the flow emits the five defaults and ends (spec Assumptions: an unreadable
   taste profile falls back to the defaults rather than failing).

### `BrowseGenreUseCase` (new, `usecase/search/`)

`suspend operator fun invoke(genreId: Int): AppResult<SearchResult>`

- `repository.getPopularGamesByGenre(genreId).map { games -> games.toSearchResult(excludedGenreId = genreId) }`.
- The games keep the **order the server returned them in** (the popularity ranking). The facets are derived
  as in a text search, except that the `genres` facet leaves out `genreId`: every result has that genre, so
  filtering on it would narrow nothing (FR-027).

### `List<Game>.toSearchResult(excludedGenreId: Int? = null): SearchResult` (new `internal` function, `usecase/search/SearchResultFacets.kt`)

The facet derivation that `SearchGamesUseCase` does inline today: distinct platforms and genres, each ordered
by how often it occurs. Moved out so both use cases share it. `SearchGamesUseCase` calls it with no
exclusion, so its behaviour does not change.

---

## `:core:ui`

### `Genre.toLabelUiText(): UiText` (new, `mapper/GenreMapper.kt`)

- If `KnownGenre.fromId(id)` is not null, an exhaustive `when` returns `UiText.StringResource(Res.string.genre_*)`.
- Otherwise `UiText.DynamicString(name)`.

### Strings (`core/ui/.../values/strings.xml`)

23 `genre_*` entries with the labels in the table above.

---

## `:feature:search`

### `BrowseGenreUiModel` (new, `model/BrowseGenreUiModel.kt`)

```text
@Immutable data class BrowseGenreUiModel(val id: Int, val label: UiText)
```

Built by `List<Genre>.toBrowseGenreUiModels()` in `SearchUiMapper.kt` (`label = genre.toLabelUiText()`).

### `SearchUiState` (changed)

| New field | Type | Default | Meaning |
|-----------|------|---------|---------|
| `browseGenres` | `List<BrowseGenreUiModel>` | `emptyList()` | The chip row. The ViewModel's initial state seeds it with the five `KnownGenre.BROWSE_DEFAULTS`, so the row is never missing (FR-010); the use case's first emission, one local read later, replaces it. A bare `SearchUiState()` (previews, tests) has no row, and the section is not drawn while the list is empty. |
| `activeGenre` | `BrowseGenreUiModel?` | `null` | The genre being browsed, or `null` when no browse is active. |

Invariant: `activeGenre != null` ⇒ `contentState !is SearchContentState.Idle`.

### `SearchUiEvent` (changed)

`data class OnGenreBrowseTriggered(val genre: BrowseGenreUiModel) : SearchUiEvent`

### `SearchContentState` (unchanged)

A browse goes through `Loading → Success | Empty | Error`, as a text search does. In `Success`, a browse's
`allGames` keeps the **server order** (R6). `filters` = platforms + genres (without the browsed one) + the
initial game types, all unselected (FR-028).

### `SortBottomSheetState.sorting` (shape unchanged)

During a browse the `RELEVANCE` entry's `label` is `sort_top_picks`; otherwise it is `sort_relevance`.
Nothing else in the list changes when the label is swapped.

### Strings (`feature/search/.../values/strings.xml`)

| Name | Value |
|------|-------|
| `browse_by_genre` | Browse by genre |
| `genre_browse_placeholder` | genre: %1$s |
| `sort_top_picks` | Top picks |
| `genre_browse_no_results` | No popular %1$s games released recently |

---

## State transitions

`S` = `SearchUiState`; `text` = the `TextFieldState`'s content.

| From | Trigger | Effect on `activeGenre` | Effect on `contentState` | Other effects |
|------|---------|-------------------------|--------------------------|---------------|
| any | `OnGenreBrowseTriggered(g)` | `g` | `Loading`, then `Success`/`Empty`/`Error` | cancel `searchJob`; empty `text`; clear suggestions; sort label → "Top picks"; **no history write** |
| browse active | `OnSearchTriggered(q)` (submit, or a recent-search chip) | `null` | `Loading` → … | cancel `searchJob`; sort label → "Relevance"; `q` written to history |
| browse active | `OnClearSearch` (X, back arrow, system back) | `null` | `Idle` (Discover) | cancel `searchJob`; sort label → "Relevance" |
| browse `Error` | `OnRetrySearch` | unchanged | `Loading` → … | re-runs `browseGenre(activeGenre)`, not a text search (FR-017) |
| browse `Success` | `OnSortChanged`, filter apply/clear | unchanged | `Success` (re-filtered, re-sorted) | "Top picks" = server order (desc) or reversed (asc) |
| browse active | typing without submitting | unchanged | unchanged | the browse stays on screen; the genre placeholder shows again if the text is deleted |
| any | rotation | unchanged | unchanged | ViewModel retained |
| any | process death | — | — | ViewModel recreated: `null` / `Idle` (FR-031) |
