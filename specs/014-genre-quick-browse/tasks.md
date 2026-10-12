---

description: "Task list for genre quick-browse in Search"
---

# Tasks: Genre Quick-Browse in Search

**Input**: Design documents from `/specs/014-genre-quick-browse/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/genre-browse.md,
contracts/search-screen-ui.md, quickstart.md

**Tests**: Included. The constitution (principle V) requires a test in the module's own `androidHostTest` for new
use-case, ViewModel and mapper logic, and this feature is mostly that logic (chip selection, facet exclusion, sort
order, state transitions). They are JUnit4 + MockK + `kotlinx-coroutines-test` with a `StandardTestDispatcher`. Tests
come before the code they cover; a test that names a type that does not exist yet simply fails to compile until the
code lands, so commit a test and its code together. The composables (`GenreBrowseSection`, the search bar changes)
are checked by previews and by the quickstart walkthrough, not by JVM tests.

**Organization**: Grouped by user story. US2–US5 all build on US1, because every one of them acts on a genre browse
that only US1 can start (see Dependencies). Tasks marked **(Mac only)** cannot be compiled or run on Windows and must
not be reported as verified unless they were run on a Mac.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependency on an unfinished task)
- **[Story]**: Which user story the task belongs to (US1–US5)
- Every path is relative to the repository root

## Rules that apply to every task

- Android must build and pass its tests at every commit: `./gradlew :<module>:compileAndroidMain` after a change,
  `./gradlew :<module>:testAndroidHostTest` for a module with tests, `./gradlew test` and
  `./gradlew :androidApp:assembleDebug` at each checkpoint. Never delete or weaken an existing test.
- One `data class` / `class` / `enum` per file. New types are `internal` unless another module needs them.
- Genres are compared by `id`, never by name (FR-011). Every user-visible string is a resource; nothing is hardcoded.
- Use `MaterialTheme.spacing` tokens, never a `dp` literal. Comments and KDoc are in English and say *why*.
- Commit messages follow `type(scope): subject` (scopes in `.githooks/commit-msg`) and end with the
  `Co-Authored-By` trailer given for this repository.

---

## Phase 1: Setup

**Purpose**: Settle the two data facts the code depends on before they are written into constants and strings.

- [X] T001 Run quickstart §1 against live IGDB using the credentials in `local.properties`. (a) `POST /v4/genres` with
  `fields id, name; limit 50;` and compare every pair with the table in `specs/014-genre-quick-browse/data-model.md`
  (§`KnownGenre`); correct the table where IGDB disagrees. (b) `POST /v4/games/count` with the `where` clause of
  `specs/014-genre-quick-browse/contracts/genre-browse.md` for genres 12, 5, 31, 15, 32 (the defaults) and 30, 26 (niche).
  A default genre must return a full grid's worth; if one is thin, change the window or the rating floor and say why.
  Record the counts and any decision in `specs/014-genre-quick-browse/research.md` (R1) so T011 uses verified values.

---

## Phase 2: Foundational (blocking prerequisites)

**Purpose**: The genre identity table and its localised labels. Every story names genres, so none can start before this.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [X] T002 [P] Create `core/model/src/commonMain/kotlin/com/nikolasguillen/questlog/core/model/KnownGenre.kt`:
  `enum class KnownGenre(val id: Int, val igdbName: String)` with the 23 entries of the table in
  `specs/014-genre-quick-browse/data-model.md` (use the ids and names as corrected by T001). Add
  `companion fun fromId(id: Int): KnownGenre?` returning `null` for an id not listed (no fallback entry) and
  `companion val BROWSE_DEFAULTS: List<KnownGenre>` = `[ROLE_PLAYING, SHOOTER, ADVENTURE, STRATEGY, INDIE]`, in that
  order (FR-007). KDoc: the enum is the app's table of genres it has a label for; `Genre.id` is what code compares.
  No Android or Compose import (`:core:model` stays plain Kotlin).
- [X] T003 [P] Add 23 `genre_*` strings to `core/ui/src/commonMain/composeResources/values/strings.xml`, one per
  `KnownGenre`, using the short labels from the data-model table (`genre_role_playing` = "RPG", `genre_platform` =
  "Platformer", `genre_simulator` = "Simulation", `genre_sport` = "Sports", `genre_real_time_strategy` =
  "Real-time strategy", `genre_hack_and_slash` = "Hack and slash", …). Plain quotes, no escapes (see `core/ui/CLAUDE.md`).
- [X] T004 [P] Create `core/ui/src/androidHostTest/kotlin/com/nikolasguillen/questlog/core/ui/mapper/GenreMapperTest.kt`
  (KDoc header saying what it covers). Cases: every `KnownGenre` maps to a `UiText.StringResource` and no two share a
  resource; `Genre(12, "Role-playing (RPG)")` equals `UiText.StringResource(Res.string.genre_role_playing)`; a genre with
  an id `KnownGenre.fromId` does not list (e.g. `Genre(9999, "Wonder Genre")`) maps to
  `UiText.DynamicString("Wonder Genre")`; the label depends on the id and not the name (`Genre(12, "anything")` still
  resolves to the RPG resource); `KnownGenre` ids are unique, `fromId(12) == ROLE_PLAYING`, `fromId(9999) == null`, and
  `BROWSE_DEFAULTS.map { it.id } == listOf(12, 5, 31, 15, 32)`.
- [X] T005 Create `core/ui/src/commonMain/kotlin/com/nikolasguillen/questlog/core/ui/mapper/GenreMapper.kt` with
  `fun Genre.toLabelUiText(): UiText`: when `KnownGenre.fromId(id)` is non-null, an **exhaustive `when`** over
  `KnownGenre` returns `UiText.StringResource(Res.string.genre_*)`; otherwise `UiText.DynamicString(name)`. Import each
  resource explicitly (`import ...core.ui.resources.genre_role_playing`). Match the shape of `GameTypeMapper.kt`.
  Makes T004 pass.

**Checkpoint**: `./gradlew :core:ui:testAndroidHostTest --console=plain -q` and
`./gradlew :core:model:compileAndroidMain --console=plain -q` are green.

---

## Phase 3: User Story 1 - Browse a genre in one tap (Priority: P1) 🎯 MVP

**Goal**: From the empty expanded search view, one tap on a genre chip closes the overlay and shows a grid of released,
recent, well-rated games of that genre, most rated first, on the usual cards, with the usual loading, empty and error
states and a Retry that re-runs the same browse. The row shows the five default genres.

**Independent Test**: Quickstart §3 steps 1–4, 9, 10: with no saved games the row reads RPG, Shooter, Adventure,
Strategy, Indie above the empty-history message; tapping one shows the grid; save and long-press work; two quick taps
show only the second genre; offline then Retry reloads the same genre.

### Tests for User Story 1 (write first)

- [X] T006 [P] [US1] Create
  `core/data/src/androidHostTest/kotlin/com/nikolasguillen/questlog/core/data/repository/GameRepositoryImplGenreBrowseTest.kt`,
  in the captured-query-body style of `GameRepositoryImplPopularGamesTest.kt`. Cases for `getPopularGamesByGenre(12)`:
  the body contains `genres = (12)`, `cover != null`, `version_parent = null`, `total_rating_count >= 10`,
  `first_release_date <= <now>`, `first_release_date > <now - 2 years>` (drive the clock the way the neighbouring test
  does), `sort total_rating_count desc` and `limit 500`; it contains **no** `platforms =` clause; rows come back in the
  order the service returned them; a thrown `IgdbHttpException` comes back as `AppResult.Failure` with `Http`; a
  `CancellationException` is rethrown, not returned as a failure.
- [X] T007 [P] [US1] Create
  `core/domain/src/androidHostTest/kotlin/com/nikolasguillen/questlog/core/domain/usecase/search/SearchResultFacetsTest.kt`.
  Cases for `List<Game>.toSearchResult(excludedGenreId)`: distinct platforms and genres; platforms and genres ordered by
  how many games carry them (most first); with `excludedGenreId = 12` no genre with id 12 is in `genres` while the games
  list still contains games carrying it; with no exclusion the result equals what `SearchGamesUseCase` produced before the
  move; the games list order is untouched.
- [X] T008 [P] [US1] Create
  `core/domain/src/androidHostTest/kotlin/com/nikolasguillen/questlog/core/domain/usecase/search/BrowseGenreUseCaseTest.kt`
  (MockK on `GameRepository`). Cases: the games are exactly the repository's list in the repository's order (B1); platform
  and genre facets are derived as in a text search (B2); the browsed genre is never in `SearchResult.genres` (B3); a
  repository `Failure` is passed through unchanged (B4); the repository is called with the given id.
- [X] T009 [US1] Extend `feature/search/src/androidHostTest/kotlin/com/nikolasguillen/questlog/feature/search/SearchViewModelTest.kt`:
  add `private val browseGenreUseCase = mockk<BrowseGenreUseCase>()` and pass it as `browseGenreUseCase = ...` in
  `createViewModel()`. Add cases: (1) `OnGenreBrowseTriggered(g)` sets `activeGenre`, moves `contentState` through
  `Loading` to `Success`, and the `Success.filters` hold the facets the use case returned (so the browsed genre is
  absent) — **the existing tests must stay green unchanged**; (2) the initial `browseGenres` is the five defaults
  `[12, 5, 31, 15, 32]` in that order; (3) with "Top picks" order, `Success.allGames` and `games` follow the **server
  order** and are not re-ranked by `calculateGameRelevanceScore`; ascending reverses it; the order survives applying a
  filter; (4) `NAME`, `RATING` and `RELEASE_DATE` sort a browse exactly as they sort a text search; (5) a failed browse
  gives `SearchContentState.Error`, and `OnRetrySearch` calls `browseGenreUseCase` again with the **same** genre id and
  never `searchGamesUseCase`; (6) a browse that returns no games gives `Empty`; (7) tapping genre A and then genre B
  while A's call is still suspended (use a `CompletableDeferred`) leaves only B's results and `activeGenre == B`.

### Implementation for User Story 1 — data and domain

- [X] T010 [US1] Add to `core/domain/src/commonMain/kotlin/com/nikolasguillen/questlog/core/domain/repository/GameRepository.kt`:
  `suspend fun getPopularGamesByGenre(genreId: Int): AppResult<List<Game>>` with the KDoc from
  `specs/014-genre-quick-browse/contracts/genre-browse.md` (released games of the genre, most rated first, inside a
  recent window and above a rating floor; network only, not cached; unlike `getGamesByGenre` it admits no unreleased game
  and orders by rating count).
- [X] T011 [US1] Implement it in
  `core/data/src/commonMain/kotlin/com/nikolasguillen/questlog/core/data/repository/GameRepositoryImpl.kt`, beside
  `getGamesByGenre`. Add three documented constants next to the existing ones: `GENRE_BROWSE_RELEASE_WINDOW_YEARS = 2`,
  `GENRE_BROWSE_MIN_RATING_COUNT = 10`, `GENRE_BROWSE_LIMIT = 500` (use T001's verified values; each KDoc says it is a
  tuning knob and why). Build the query exactly as in the contract: same `fields` list as `searchGames`; `where genres =
  ($genreId) & game_type != ($excludedIds) & version_parent = null & cover != null & total_rating_count >= … &
  first_release_date <= $nowSeconds & first_release_date > $windowStartSeconds`; `sort total_rating_count desc; limit …`.
  No platform clause. Send it with `apiService.searchGames(...)`, map with `toGame()`, wrap in `AppResult.success`, and
  `catch (e: Exception)` → `AppResult.failure(e.toRepositoryError())` (which rethrows `CancellationException`). Compute
  `now` with `Clock.System`, as the file already does. Makes T006 pass.
- [X] T012 [P] [US1] Create
  `core/domain/src/commonMain/kotlin/com/nikolasguillen/questlog/core/domain/usecase/search/SearchResultFacets.kt` with
  `internal fun List<Game>.toSearchResult(excludedGenreId: Int? = null): SearchResult`, holding the facet derivation now
  inline in `SearchGamesUseCase` (distinct platforms and genres, each sorted by occurrence count descending); genres whose
  id equals `excludedGenreId` are left out of the `genres` facet only. Change
  `core/domain/src/commonMain/kotlin/com/nikolasguillen/questlog/core/domain/usecase/search/SearchGamesUseCase.kt` to
  `repository.searchGames(query).map { it.toSearchResult() }`; its public behaviour must not change. Makes T007 pass.
- [X] T013 [US1] Create
  `core/domain/src/commonMain/kotlin/com/nikolasguillen/questlog/core/domain/usecase/search/BrowseGenreUseCase.kt`:
  `class BrowseGenreUseCase(private val repository: GameRepository)` with
  `suspend operator fun invoke(genreId: Int): AppResult<SearchResult>` =
  `repository.getPopularGamesByGenre(genreId).map { it.toSearchResult(excludedGenreId = genreId) }`. KDoc: the games keep
  the order the server returned; the browsed genre is left out of the facets because every result carries it. Depends on
  T010, T012. Makes T008 pass.
- [X] T014 [US1] Register `factoryOf(::BrowseGenreUseCase)` (with its import) in
  `core/domain/src/commonMain/kotlin/com/nikolasguillen/questlog/core/domain/di/DomainKoin.kt`, next to `SearchGamesUseCase`.

### Implementation for User Story 1 — feature models and strings

- [X] T015 [P] [US1] Create
  `feature/search/src/commonMain/kotlin/com/nikolasguillen/questlog/feature/search/model/BrowseGenreUiModel.kt`:
  `@Immutable internal data class BrowseGenreUiModel(val id: Int, val label: UiText)`, with a KDoc line saying the id is
  what code compares and the label is only for display.
- [X] T016 [US1] Add to `feature/search/src/commonMain/composeResources/values/strings.xml`:
  `browse_by_genre` = "Browse by genre" and `genre_browse_no_results` = "No popular %1$s games released recently"
  (positional `%1$s`, plain quotes).
- [X] T017 [P] [US1] In `feature/search/src/commonMain/kotlin/com/nikolasguillen/questlog/feature/search/model/SearchUiState.kt`
  add `val browseGenres: List<BrowseGenreUiModel> = emptyList()` and `val activeGenre: BrowseGenreUiModel? = null`, with
  `@property` KDoc: `browseGenres` is the chip row, `activeGenre` is the genre being browsed or `null`, and
  `activeGenre != null` implies `contentState !is SearchContentState.Idle`. Depends on T015.
- [X] T018 [P] [US1] In `feature/search/src/commonMain/kotlin/com/nikolasguillen/questlog/feature/search/model/SearchUiEvent.kt`
  add `data class OnGenreBrowseTriggered(val genre: BrowseGenreUiModel) : SearchUiEvent` with a KDoc line (the user
  tapped a genre chip). Depends on T015.
- [X] T019 [P] [US1] In `feature/search/src/commonMain/kotlin/com/nikolasguillen/questlog/feature/search/mapper/SearchUiMapper.kt`
  add `internal fun List<Genre>.toBrowseGenreUiModels(): List<BrowseGenreUiModel>` (`id = genre.id`,
  `label = genre.toLabelUiText()`) and
  `internal fun defaultBrowseGenres(): List<BrowseGenreUiModel>` built from `KnownGenre.BROWSE_DEFAULTS` as
  `Genre(it.id, it.igdbName)`. Depends on T005, T015.

### Implementation for User Story 1 — ViewModel

- [X] T020 [US1] In `feature/search/src/commonMain/kotlin/com/nikolasguillen/questlog/feature/search/SearchViewModel.kt`:
  add the constructor parameter `private val browseGenreUseCase: BrowseGenreUseCase`; seed the initial
  `SearchUiState(browseGenres = defaultBrowseGenres(), …)`; route `SearchUiEvent.OnGenreBrowseTriggered` to a new
  `browseGenre(genre)`. `browseGenre` does exactly what `performSearch` does **except** it never calls
  `addSearchToHistoryUseCase`: cancel `searchJob`, clear the text field, clear suggestions (and
  `suggestionsResetTrigger.tryEmit("")`), set `activeGenre = genre` together with `contentState = Loading`, call
  `browseGenreUseCase(genre.id)`, and on success build `Empty` or `Success(games, filters = platforms + genres + game
  types, allGames = games in server order)` with the same builders `performSearch` uses; on failure
  `Error(error.toUiText())`. In `performSearch` also **cancel `searchJob` before launching the new one** (research R5),
  which fixes the latent last-response-wins race between two searches. Do not change what `performSearch` writes to
  history. Depends on T013, T014, T017–T019. Makes T009 cases 1, 2, 5–7 pass once T021–T022 land.
- [X] T021 [US1] In the same file make the sort browse-aware (research R6): give `sortGames` a `keepServerOrder: Boolean`
  parameter; for `SearchSort.RELEVANCE` it returns `games` unchanged when descending and `games.reversed()` when
  ascending, and never calls `calculateGameRelevanceScore`; the other three sorts are untouched. `browseGenre` stores
  `allGames` in server order **without** applying a first sort and builds `games` with
  `sortGames(…, keepServerOrder = true)`; `updateSearchContent` passes
  `keepServerOrder = _uiState.value.activeGenre != null`. Filtering already preserves order. Makes T009 cases 3–4 pass.
- [X] T022 [US1] In the same file make leaving a browse consistent: `clearSearch()` sets `activeGenre = null` together
  with `contentState = Idle`; `performSearch` sets `activeGenre = null` **after** its `isBlank` guard (a blank submit during a browse must leave the
  browse untouched, or genre results would sit under the default placeholder); `retrySearch()` calls
  `browseGenre(activeGenre)` when `activeGenre != null` and `performSearch(textFieldState.text.toString())` otherwise
  (FR-017, FR-023). Makes T009 case 5 pass. Then run `./gradlew :feature:search:testAndroidHostTest`.

### Implementation for User Story 1 — UI

- [X] T023 [P] [US1] Create
  `feature/search/src/commonMain/kotlin/com/nikolasguillen/questlog/feature/search/components/GenreBrowseSection.kt`:
  an `internal` composable `GenreBrowseSection(genres: List<BrowseGenreUiModel>, onGenreClick:
  (BrowseGenreUiModel) -> Unit, modifier: Modifier = Modifier)` — a `titleMedium` "Browse by genre" heading padded like
  the "Recent searches" heading in `SearchBars.kt`, then `CustomChipRow(items = genres, key = { it.id })`. Each chip is a
  `private` `GenreChip` built on Material's **`AssistChip`** (a button: Compose gives it `Role.Button` and no selected
  state, FR-004), with **no icon**, `MaterialTheme.shapes.small`, container `appColors.chipSelectedContainerColor`, label
  `appColors.chipSelectedContentColor`, and a border of `appColors.chipSelectedBorderColor` that is `0.dp` wide in dark
  theme and `1.dp` in light (read `MaterialTheme.isDarkTheme`, copy the split from `CustomFilterChip`). The label is
  `genre.label.asString()`, `maxLines = 1`; a click calls `onGenreClick(genre)` and does **not** call `bringIntoView`
  (the overlay closes). Gaps use `MaterialTheme.spacing`. End the file with `@QuestLogPreviews` previews wrapped in
  `QuestLogTheme { }`: the five defaults, and a long label. Depends on T015, T016.
- [X] T024 [US1] In `feature/search/src/commonMain/kotlin/com/nikolasguillen/questlog/feature/search/components/SearchBars.kt`
  add `browseGenres: List<BrowseGenreUiModel>` and `onBrowseGenre: (BrowseGenreUiModel) -> Unit` to `SearchTopBar` and
  `ExpandedSearchBar` and lay the overlay out as in `specs/014-genre-quick-browse/contracts/search-screen-ui.md`: when the
  query is blank and `browseGenres` is not empty, `GenreBrowseSection` comes first — above the
  `expanded_search_initial_message` when history is empty, and above Recent searches and Recently viewed otherwise (the
  `else` column keeps its `spacedBy(MaterialTheme.spacing.large)`); when `browseGenres` is empty or the query is not
  blank, the overlay is exactly as today (FR-002). Update the existing previews to pass genres and add one
  `ExpandedSearchBar` preview with an empty history and genres. Depends on T023.
- [X] T025 [US1] Make the empty state say which genre it is for. In
  `feature/search/src/commonMain/kotlin/com/nikolasguillen/questlog/feature/search/components/SearchPlaceholders.kt`
  change `EmptySearchPlaceholder` to take `message: String` (keeping `onClearSearchClick`, `modifier`), and in
  `.../components/SearchMainContent.kt` add `activeGenre: BrowseGenreUiModel?` and pass
  `stringResource(Res.string.genre_browse_no_results, genre.label.asString())` when it is non-null and
  `search_no_results` otherwise (FR-019); add a browse-empty preview. Depends on T016, T017.
- [X] T026 [US1] In `feature/search/src/commonMain/kotlin/com/nikolasguillen/questlog/feature/search/SearchScreen.kt` add
  an `onBrowseGenre: (BrowseGenreUiModel) -> Unit` built like `onSearch` — `remember(onResetScroll, searchBarState,
  onEvent, scope)`, then `scope.launch { onResetScroll() }`, `scope.launch { searchBarState.animateToCollapsed() }` and
  `onEvent(SearchUiEvent.OnGenreBrowseTriggered(genre))` — and pass it, with `uiState.browseGenres`, to `SearchTopBar`;
  pass `uiState.activeGenre` to `SearchMainContent`. Update the `SearchScreenContent` previews if the compiler asks.
  Depends on T024, T025.

**Checkpoint**: `./gradlew :feature:search:testAndroidHostTest :core:data:testAndroidHostTest :core:domain:testAndroidHostTest
--console=plain -q`, `./gradlew :shared:testAndroidHostTest` (`KoinGraphTest` must see `BrowseGenreUseCase`) and
`./gradlew :androidApp:assembleDebug` are green. Then run quickstart §3 steps 1–4, 9 and 10 on a device. **User Story 1
is the MVP: stop and validate here.**

---

## Phase 4: User Story 2 - Chips that reflect the user's taste (Priority: P1)

**Goal**: The row leads with the user's strongest taste genres, padded with the defaults, always five distinct chips.

**Independent Test**: Quickstart §3 steps 12–13: with no saved games the row is the defaults; after saving games of
other genres those lead; a genre only dropped games carry does not; no duplicates.

### Tests for User Story 2 (write first)

- [X] T027 [P] [US2] Create
  `core/domain/src/androidHostTest/kotlin/com/nikolasguillen/questlog/core/domain/usecase/search/GetBrowseGenresUseCaseTest.kt`
  (MockK on `getSavedGames()`). Cases: no saved games → ids `[12, 5, 31, 15, 32]` (C2); one saved game with three genres →
  those three first, then defaults skipping duplicates, five total (C3, C4); more than five positive genres → the five
  strongest only, no default; a taste genre equal to a default appears once, at its taste position (C5); a genre whose
  weight is negative (only a `DROPPED` game carries it) is not offered; a tie on weight is broken by `count` and then by
  ascending id, deterministically (run it twice); every emission has exactly 5 distinct ids (C1); a re-emission of the
  saved-games flow that does not change the ranking produces no new list, one that does produces one (C6); a
  `getSavedGames()` flow that throws emits the five defaults instead of failing (C7).
- [X] T028 [US2] Extend `SearchViewModelTest.kt` (`feature/search/src/androidHostTest/kotlin/com/nikolasguillen/questlog/feature/search/`):
  add `getBrowseGenresUseCase = mockk<GetBrowseGenresUseCase>()`, stub `every { getBrowseGenresUseCase() } returns
  flowOf(<the five defaults>)` in `setUp()`, pass it in `createViewModel()`, and add case (9): after the use case emits a
  different list, `uiState.browseGenres` has the same ids in the same order with labels from `toLabelUiText()`, and a
  second emission replaces the first.

### Implementation for User Story 2

- [X] T029 [US2] Create
  `core/domain/src/commonMain/kotlin/com/nikolasguillen/questlog/core/domain/usecase/discover/TasteProfileBuilder.kt` with
  `internal fun List<Game>.toTasteProfile(): TasteProfile`, **moving without changes** `buildProfile`, `gameWeight`,
  `toSignals`, `addScore`, `addCount` and the two `RECENTLY_VIEWED_*` constants out of
  `.../usecase/discover/GetTasteProfileUseCase.kt` (keep their KDoc). `GetTasteProfileUseCase.invoke()` becomes
  `repository.getSavedGames().map { if (it.isEmpty()) TasteProfile.EMPTY else it.toTasteProfile() }`. This is a pure
  move: run `./gradlew :core:domain:testAndroidHostTest` and `GetTasteProfileUseCaseTest` must pass **unchanged**.
- [X] T030 [US2] Create
  `core/domain/src/commonMain/kotlin/com/nikolasguillen/questlog/core/domain/usecase/search/GetBrowseGenresUseCase.kt`:
  `class GetBrowseGenresUseCase(private val repository: GameRepository)` with `operator fun invoke(): Flow<List<Genre>>`
  implementing the five rules of `specs/014-genre-quick-browse/data-model.md` (§`GetBrowseGenresUseCase`): map each
  emission of `repository.getSavedGames()`, build the profile with `toTasteProfile()` (or `TasteProfile.EMPTY` when the
  list is empty), take `names = games.flatMap { it.genres }.associateBy { it.id }`, keep genres with `weight > 0.0`, sort by
  `weight` descending, then `count` descending, then `id` ascending, take at most `BROWSE_GENRE_COUNT = 5` (a named
  private constant), skip an id with no name, then pad from `KnownGenre.BROWSE_DEFAULTS` as `Genre(it.id, it.igdbName)`
  skipping ids already present until there are five; finish with `.distinctUntilChanged()` and `.catch { emit(<the five defaults>) }` after the
  mapping, so a failing saved-games flow still gives a full row (spec Assumptions; the catch ends the
  flow, which is acceptable for a failure that should not happen). KDoc: why positive weights only, why the id is the
  last tie-break, and why the fallback exists. Depends on T029. Makes T027 pass.
- [X] T031 [US2] Register `factoryOf(::GetBrowseGenresUseCase)` in
  `core/domain/src/commonMain/kotlin/com/nikolasguillen/questlog/core/domain/di/DomainKoin.kt`.
- [X] T032 [US2] In `feature/search/src/commonMain/kotlin/com/nikolasguillen/questlog/feature/search/SearchViewModel.kt`
  add the constructor parameter `private val getBrowseGenresUseCase: GetBrowseGenresUseCase` and an
  `observeBrowseGenres()` called from `init`: `viewModelScope.launch { getBrowseGenresUseCase().collect { genres ->
  _uiState.update { it.copy(browseGenres = genres.toBrowseGenreUiModels()) } } }`, same shape as
  `observeDiscoverFeed`. The seeded defaults from T020 stay as the initial value, so the row is never empty if the first
  emission is slow (FR-010). Depends on T030, T031. Makes T028 pass.

**Checkpoint**: `./gradlew :core:domain:testAndroidHostTest :feature:search:testAndroidHostTest --console=plain -q`,
`./gradlew :shared:testAndroidHostTest` and `./gradlew :androidApp:assembleDebug` are green. Run quickstart §3 steps 12–13.

---

## Phase 5: User Story 3 - Know you are browsing, and get back out (Priority: P1)

**Goal**: With the field empty, the bar says which genre is being browsed, shows a clear button, and X, the back arrow and
the system back all return to Discover; a submitted query replaces the browse.

**Independent Test**: Quickstart §3 steps 3, 7, 8, 14: placeholder "genre: RPG" with X visible; X, arrow and system back
each return to Discover with the default placeholder; typing and submitting a query restores it.

### Tests for User Story 3 (write first)

- [X] T033 [US3] Extend `SearchViewModelTest.kt` with: (a) `OnClearSearch` during a browse sets `activeGenre = null` and
  `contentState = Idle`; (b) `OnSearchTriggered("zelda")` during a browse sets `activeGenre = null` and runs
  `searchGamesUseCase`; (c) typing into `textFieldState` without submitting (`setQuery`) leaves `activeGenre` and
  `contentState` unchanged, and clearing the typed text afterwards still leaves the browse active; (d) tapping a chip
  while text-search results are showing replaces them and empties the text field; (e) tapping the genre already being
  browsed ends in the same `Success` state, not an inconsistent one; (f) a blank submit (`OnSearchTriggered("")` or
  whitespace) during a browse leaves `activeGenre`, `contentState` and the sort label unchanged.

### Implementation for User Story 3

- [X] T034 [US3] Add `genre_browse_placeholder` = "genre: %1$s" to
  `feature/search/src/commonMain/composeResources/values/strings.xml`.
- [X] T035 [US3] In `feature/search/src/commonMain/kotlin/com/nikolasguillen/questlog/feature/search/components/SearchBars.kt`
  give `SearchInputField` a parameter `activeGenreLabel: UiText? = null` (a default keeps every existing call and preview
  compiling). When it is non-null: the placeholder text is `stringResource(Res.string.genre_browse_placeholder,
  label.asString())`; the input field gets `Modifier.semantics { stateDescription = <the same text> }` so a screen reader
  announces the genre although the field is empty (the default placeholder stays `clearAndSetSemantics {}` as today); and
  the clear (X) button is shown when `textFieldState.text.isNotEmpty() || activeGenreLabel != null` (FR-021) — it keeps
  sending `onClearSearch`. Pass `uiState.activeGenre?.label` from `SearchTopBar`'s `inputField`. Add a
  `SearchInputField` preview with an empty field and a genre. Depends on T034.
- [X] T036 [P] [US3] In `feature/search/src/commonMain/kotlin/com/nikolasguillen/questlog/feature/search/SearchScreen.kt`
  change the `BackHandler` condition to `searchBarState.currentValue == SearchBarValue.Collapsed &&
  (textFieldState.text.isNotEmpty() || uiState.activeGenre != null)` so the system back gesture ends a browse as the
  arrow does. Update the comment above it. (The back arrow already shows for any non-`Idle` `contentState` and needs no
  change.) Makes T033 meaningful at the UI level; verified in quickstart step 7.

**Checkpoint**: `./gradlew :feature:search:testAndroidHostTest --console=plain -q` and
`./gradlew :androidApp:assembleDebug` are green. Run quickstart §3 steps 3, 7, 8 and 14.

---

## Phase 6: User Story 4 - Sort and filter a genre browse sensibly (Priority: P2)

**Goal**: "Relevance" reads "Top picks" during a browse with the selected sort and direction untouched, and the filter
sheet's genre group never offers the browsed genre while everything else still narrows the grid with AND semantics.

**Independent Test**: Quickstart §3 steps 5–6.

### Tests for User Story 4 (write first)

- [ ] T037 [US4] Extend `SearchViewModelTest.kt` with: (a) during a browse the `RELEVANCE` entry of
  `sortBottomSheetState.sorting` is labelled `UiText.StringResource(Res.string.sort_top_picks)`; after `OnClearSearch`, and
  after a submitted query, it is `UiText.StringResource(CoreUiRes.string.sort_relevance)` again; (b) with `RATING`
  ascending selected, starting and ending a browse leaves `selected`, `descending` and `isSortActive` for every entry
  exactly as they were; (c) starting a browse while a text search's filters are selected resets to no selected filter
  (FR-028) and the genre filters list does not contain the browsed id; (d) with a platform filter and a second genre
  filter applied during a browse, `games` holds only games that carry **all** of them (AND), in "Top picks" order.
- [ ] T038 [US4] Add `sort_top_picks` = "Top picks" to
  `feature/search/src/commonMain/composeResources/values/strings.xml`.

### Implementation for User Story 4

- [ ] T039 [US4] In `feature/search/src/commonMain/kotlin/com/nikolasguillen/questlog/feature/search/mapper/SearchUiMapper.kt`:
  add `internal fun List<SortingUiModel>.withRelevanceLabel(isGenreBrowse: Boolean): List<SortingUiModel>` that rewrites
  **only** the `SearchSort.RELEVANCE` entry's `label` — `UiText.StringResource(Res.string.sort_top_picks)` when
  `isGenreBrowse`, else `UiText.StringResource(CoreUiRes.string.sort_relevance)` — and never touches `selected` or
  `descending`. Also change `toGenreFilters()` to `label = genre.toLabelUiText()` (research R3, so the filter sheet says
  "RPG" like the chip and the placeholder; this also applies to text searches). Depends on T038.
- [ ] T040 [US4] In `feature/search/src/commonMain/kotlin/com/nikolasguillen/questlog/feature/search/SearchViewModel.kt`
  apply the label at every transition: `browseGenre` runs `sorting.withRelevanceLabel(true)` on
  `sortBottomSheetState`; `performSearch` (after its `isBlank` guard) and `clearSearch` run `withRelevanceLabel(false)`. Keep `isSortActive`
  untouched. Makes T037 pass.

**Checkpoint**: `./gradlew :feature:search:testAndroidHostTest --console=plain -q` is green; run quickstart §3 steps 5–6.

---

## Phase 7: User Story 5 - A genre browse leaves no trace in history (Priority: P2)

**Goal**: A genre browse is never written to the search history; a text query submitted afterwards is.

**Independent Test**: Quickstart §3 step 8, and the test below.

- [ ] T041 [US5] Extend `SearchViewModelTest.kt` with: (a) after `OnGenreBrowseTriggered`, `addSearchToHistoryUseCase` was
  never called (`coVerify(exactly = 0)`), including after a Retry of a failed browse; (b) a browse followed by
  `OnSearchTriggered("zelda")` calls `addSearchToHistoryUseCase("zelda")` exactly once and never with an empty string or a
  genre name. No production change is needed: T020 already never calls the use case from `browseGenre`; this test pins it.

**Checkpoint**: `./gradlew :feature:search:testAndroidHostTest --console=plain -q` is green.

---

## Phase 8: Polish & cross-cutting

**Purpose**: Documentation that the rules require, and the verification the constitution calls for.

- [ ] T042 [P] Update `core/ui/CLAUDE.md`: in the Mappers section add `GenreMapper.kt` (`Genre.toLabelUiText()`: a known id
  resolves to a short `genre_*` resource, any other falls back to the data source's name). Instructions, not a changelog:
  do not mention this feature by name.
- [ ] T043 [P] Update `docs/tech-debt.md` (Infrastructure → test coverage gaps): the `:core:domain` sentence says the
  search use cases are not covered; now `BrowseGenreUseCase`, `GetBrowseGenresUseCase` and the facet function are.
  Also update the `:core:ui` sentence ("only `GameUiMapper`, `PlatformPickerMapper` and `HtmlUtils` are covered"): it is
  false once `GenreMapperTest` lands, so add `GenreMapper`. Reword so it names only what is still uncovered, and refresh the "Last audited" date only if the whole file was
  re-checked (it was not, so leave it).
- [ ] T044 Run `./gradlew test` and `./gradlew :androidApp:assembleDebug`; both must pass with no existing test deleted
  or weakened. Fix what they reveal.
- [ ] T045 **(Mac only)** Run `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64 -q` and the `xcodebuild` command from
  the root `CLAUDE.md`. If no Mac is available, record "iOS compile not run" in the final report instead of assuming.
- [ ] T046 Run quickstart §3 (steps 1–14) on an Android emulator or device and record each step's result. Includes TalkBack
  (step 14) and the offline/Retry case (step 10).
- [ ] T047 **(Mac only)** Run quickstart §4 on the iOS simulator (steps 1–9, 11–12, 14 with VoiceOver; step 10 is not
  runnable on a simulator). Record every step not run as not run.

---

## Dependencies & execution order

### Phase dependencies

- **Setup (T001)**: none. T011 and T002 use its verified values.
- **Foundational (T002–T005)**: needs nothing else; **blocks every story** (all of them name genres).
- **US1 (T006–T026)**: needs Foundational. It is the MVP and the base for every other story.
- **US2 (T027–T032)**: needs US1 (it extends `SearchViewModel` and its test and replaces nothing).
- **US3 (T033–T036)**: needs US1 (the state it exposes and the ViewModel paths from T022).
- **US4 (T037–T040)**: needs US1 (a browse to relabel and sort).
- **US5 (T041)**: needs US1.
- **Polish (T042–T047)**: needs the stories you ship.

US2, US3, US4 and US5 do not depend on one another. They edit `SearchViewModel.kt`, `SearchViewModelTest.kt` and
`SearchBars.kt` in places that do not overlap, but those are shared files, so run them one after another unless two
people merge carefully.

### Within a story

- Tests first (they will not compile until their code exists), then data/domain, then models and strings, then the
  ViewModel, then the UI.
- `core:domain` before `core:data` compiles against it (`T010` before `T011`), and both before the feature module.
- `SearchViewModel.kt` is edited by T020, T021, T022, T032 and T040: keep them in that order.

### Parallel opportunities

- Foundational: T002, T003 and T004 are independent files.
- US1 tests T006, T007 and T008 are independent files; T012 can run beside T010–T011; T015, T017, T018, T019 and T023 are
  independent files once T015 exists.
- US2: T027 can be written beside T029.
- US3: T036 (`SearchScreen.kt`) is independent of T034–T035 (`strings.xml`, `SearchBars.kt`).
- Polish: T042 and T043.

### Parallel example: User Story 1

```text
# Tests, together:
T006  core/data/.../GameRepositoryImplGenreBrowseTest.kt
T007  core/domain/.../usecase/search/SearchResultFacetsTest.kt
T008  core/domain/.../usecase/search/BrowseGenreUseCaseTest.kt

# After T015 (BrowseGenreUiModel), together:
T017  SearchUiState.kt     T018  SearchUiEvent.kt     T019  SearchUiMapper.kt     T023  GenreBrowseSection.kt
```

---

## Implementation strategy

### MVP first (User Story 1 only)

1. T001 — verify the data, then T002–T005 — the genre table and labels.
2. T006–T026 — the browse itself, with the default five chips.
3. **Stop and validate** with quickstart §3 steps 1–4, 9, 10. Demo it: it already answers "I just want a good RPG".

### Incremental delivery

1. Foundation + US1 → the one-tap browse (MVP).
2. + US2 → the row becomes personal.
3. + US3 → the bar says what is being browsed and offers the way out (ship with or right after US1: without it, the
   only exits are the back arrow and the system back gesture, and the bar gives no sign of the genre).
4. + US4 → "Top picks" and a filter sheet that makes sense.
5. + US5 → locks in the no-history rule with a test.
6. Polish → docs and the full verification.

Note on US3: US1 alone is usable, since the back arrow and the system back already clear a browse, but the empty bar
with no label and no X is the rough edge the spec calls out. Treat US1 + US3 as the smallest *release*.

## Notes

- [P] tasks touch different files and have no unfinished dependency.
- The two thresholds (2 years, 10 ratings) are tuning knobs; T001 checks them once and T011 documents them.
- `docs/tech-debt.md` lists known deviations; do not fix any of them here.
- Out of scope, do not add: upcoming games, a mode toggle, persisting the chosen genre, changes to Discover lanes, extra
  pagination. The Discover "Because you like %1$s" title and the game-detail genre list keep IGDB's raw names (follow-up).
