---

description: "Task list for search by character and developer"
---

# Tasks: Search by Character and Developer

**Input**: Design documents from `specs/019-search-characters-companies/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md) (R1–R15),
[data-model.md](data-model.md), [contracts/repository-contract.md](contracts/repository-contract.md),
[contracts/ui-contract.md](contracts/ui-contract.md), [quickstart.md](quickstart.md)

**Tests**: The spec does not ask for tests, but the constitution (Principle V) requires JVM tests for new mapper, use
case, view model and error-mapping logic, and `KoinGraphTest` must stay green. So every task that adds such logic is
followed by its test task. The project has no Compose UI tests (`docs/tech-debt.md`): visual checks are the
scenarios S1–S18 of [quickstart.md](quickstart.md), and the hands-on tasks below name the scenarios.

**Organization**: Grouped by user story. Phase 2 (Foundational) holds the data layer and navigation plumbing that all
stories share. US1 is the selector, US2 and US3 are the two result types, US4 is the detail page both result types
lead to, and US5 is the failure behavior.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependency on an unfinished task)
- **[Story]**: US1 to US5, from spec.md

## Path Conventions

Multiplatform modules keep sources in `src/commonMain/kotlin/com/nikolasguillen/questlog/...`. Paths below abbreviate
that prefix as follows, and always name the full module directory:

- `core/network/…` = `core/network/src/commonMain/kotlin/com/nikolasguillen/questlog/core/network/…`
- `core/model/…` = `core/model/src/commonMain/kotlin/com/nikolasguillen/questlog/core/model/…`
- `core/domain/…` = `core/domain/src/commonMain/kotlin/com/nikolasguillen/questlog/core/domain/…`
- `core/database/…` = `core/database/src/commonMain/kotlin/com/nikolasguillen/questlog/core/database/…`
- `core/data/…` = `core/data/src/commonMain/kotlin/com/nikolasguillen/questlog/core/data/…`
- `core/ui/…` = `core/ui/src/commonMain/kotlin/com/nikolasguillen/questlog/core/ui/…`
- `core/designsystem/…` = `core/designsystem/src/commonMain/kotlin/com/nikolasguillen/questlog/core/designsystem/…`
- `core/navigation/…` = `core/navigation/src/commonMain/kotlin/com/nikolasguillen/questlog/core/navigation/…`
- `feature/search/…` = `feature/search/src/commonMain/kotlin/com/nikolasguillen/questlog/feature/search/…`
- `feature/game-detail/…` = `feature/game-detail/src/commonMain/kotlin/com/nikolasguillen/questlog/feature/gamedetail/…`
- `feature/wishlist/…` = `feature/wishlist/src/commonMain/kotlin/com/nikolasguillen/questlog/feature/wishlist/…`
- `shared/…` = `shared/src/commonMain/kotlin/com/nikolasguillen/questlog/shared/…`
- Tests: `<module>/src/androidHostTest/kotlin/com/nikolasguillen/questlog/<package>/…`

Every comment and KDoc is in English, and every new type gets class-level KDoc. One `data class`, `sealed interface` or
`class` per file, except the sealed state, event and effect files and `Routes.kt`. Default to `internal`. A new
composable gets a private `@QuestLogPreviews` preview wrapped in `QuestLogTheme { }`. Text is a string resource or
`UiText`, never a literal in a composable or mapper. No new `dp` literal: use `MaterialTheme.spacing`. A cross-module
`Res` import is aliased `CoreUiRes`, and a module's own `Res` is imported bare, with one import per resource. On
Windows use `.\gradlew.bat` in place of `./gradlew`.

**Commit boundaries**: Android must build and pass its tests at every commit. T012 and T013 (the interface and its only
implementation) land in the same commit, as do T053–T056 (the translator signature, the entity key and the callers) and
the moves in T059–T062.

---

## Phase 1: Setup

**Purpose**: Know the starting point, so any later failure is attributable to this feature.

- [X] T001 On branch `019-search-characters-companies`, run `./gradlew :androidApp:assembleDebug --console=plain -q`
  and `./gradlew test --console=plain -q`, and record the result at the end of this task. If either is red before any
  edit, stop and tell the owner which test or module, rather than working on top of a red baseline (the 018 baseline
  had 9 environmental `:core:data` DataStore failures on Windows, so a known red set is possible).
  **Result (2026-10-20, macOS)**: both green before any edit (`assembleDebug` and `test`, exit 0).

---

## Phase 2: Foundational (blocking prerequisites)

**Purpose**: The network, domain and data layer for both new types, plus the routes and the two click lambdas. No story
can start before this phase ends. Nothing here changes what the user sees.

### Network

- [X] T002 [P] Create `core/network/model/IgdbImage.kt`: `@Serializable data class IgdbImage(val id: Int, val url: String?)`
  with class KDoc and `@property` tags ("A character's `mug_shot` or a company's `logo`", url "usually starts with
  `//`"). Same shape as `IgdbCover`, which is a game's cover and stays untouched.
- [X] T003 [P] Create `core/network/model/IgdbCharacter.kt`: `@Serializable data class IgdbCharacter(val id: Int,
  val name: String, val description: String? = null, @SerialName("mug_shot") val mugShot: IgdbImage? = null,
  val games: List<Int>? = null)`. KDoc: a search requests `name, mug_shot.url` only; a profile adds `description, games`
  (an id array).
- [X] T004 [P] Extend `core/network/model/IgdbCompany.kt` with nullable defaulted fields `description: String? = null`,
  `@SerialName("start_date") startDate: Long? = null`, `logo: IgdbImage? = null`,
  `developed: List<Int>? = null`. `id` and `name` stay as they are, so an `involved_companies.company` row (only `id`
  and `name`) still decodes. Update the KDoc `@property` tags.
- [X] T005 In `core/network/IgdbApiService.kt` add `suspend fun getCharacters(query: String): List<IgdbCharacter>` and
  `suspend fun getCompanies(query: String): List<IgdbCompany>`; in `core/network/IgdbApiServiceImpl.kt` implement them as
  `post("characters", query)` and `post("companies", query)`, using the existing private `post`, so timeouts and
  transport failures translate exactly as for games. Do not add a method for a character's or developer's games: they
  reuse `searchGames` (same `/games` path).
- [X] T006 [P] In `core/network/src/androidHostTest/kotlin/com/nikolasguillen/questlog/core/network/IgdbHttpClientTest.kt`
  add a test per new method over `MockEngine`: it posts to `…/characters` or `…/companies` with the query as a
  `text/plain` body and decodes the rows. In `IgdbDtoDecodingTest.kt` add: a character without `mug_shot` decodes it as
  `null`; a company with only `id` and `name` still decodes; a company with `logo`, `developed` and `start_date`
  decodes them.
- [X] T007 [P] Update `core/network/CLAUDE.md`: add the two methods to the interface listing, and change "Four IGDB
  paths, five methods" to six paths and seven methods, keeping the sentence that a new kind of `/games` query needs no
  new method.

### Domain models and mapping

- [X] T008 [P] Create `core/model/GameCharacter.kt` (`id: Int`, `name: String`, `imageUrl: String?`),
  `core/model/CharacterProfile.kt` (`id`, `name`, `imageUrl: String?`, `description: String?`, `gameIds: List<Int>`),
  `core/model/Developer.kt` (`id`, `name`, `logoUrl: String?`, `developedGameCount: Int`) and
  `core/model/DeveloperProfile.kt` (`id`, `name`, `logoUrl: String?`, `description: String?`, `foundedYear: Int?`). One
  file each, KDoc with `@property`, no Android or Compose dependency.
- [X] T009 In `core/data/mapper/GameMapper.kt` change `private fun String.toIgdbImageUrl(size: String = "t_720p")` to
  `internal`, so the new mappers share it. Behavior is unchanged: it prefixes `https:` to a `//` url and replaces
  `t_thumb` with the size.
- [X] T010 [P] Create `core/data/mapper/CharacterMapper.kt` with `internal fun IgdbCharacter.toGameCharacter()` and
  `internal fun IgdbCharacter.toCharacterProfile()`. Portraits use `mugShot?.url?.toIgdbImageUrl("t_cover_big")`. "A
  blank `description` from IGDB maps to `null`" (use `takeIf { it.isNotBlank() }`). `gameIds` is `games.orEmpty()`.
- [X] T011 [P] Create `core/data/mapper/DeveloperMapper.kt` with `internal fun IgdbCompany.toDeveloper()` and
  `internal fun IgdbCompany.toDeveloperProfile()`. Logos use `logo?.url?.toIgdbImageUrl("t_logo_med")`.
  `developedGameCount` is `developed.orEmpty().size`. A blank `description` maps to `null`. `foundedYear` is the UTC
  year of `startDate` (Unix seconds) through `kotlinx-datetime`:
  `Instant.fromEpochSeconds(it).toLocalDateTime(TimeZone.UTC).year`, `null` when `startDate` is `null`.

### Repository

- [X] T012 In `core/domain/repository/GameRepository.kt` add the six methods exactly as written, with their KDoc, in
  [contracts/repository-contract.md](contracts/repository-contract.md): `searchCharacters(query)`,
  `searchDevelopers(query)`, `getCharacterProfile(characterId)`, `getDeveloperProfile(developerId)`,
  `getGamesByIds(gameIds)` and `getDevelopedGames(developerId)`, all returning `AppResult<…>`.
- [X] T013 In `core/data/repository/GameRepositoryImpl.kt` implement the six methods (same commit as T012), each as
  `try { … AppResult.success(…) } catch (e: Exception) { AppResult.failure(e.toRepositoryError()) }`. Add a private
  constant `GAME_FIELDS` holding the exact field list the existing queries send (copy it from `getGamesByDeveloper`) for
  the two games queries only; do not touch the existing six methods. Queries:
  `search "$query"; fields name, mug_shot.url; limit 100;` on `getCharacters`;
  `fields name, logo.url, developed; where name ~ *"$query"* & developed != null; limit 500;` on `getCompanies`;
  profiles with `where id = $id;` and `fields name, description, mug_shot.url, games` (character) or
  `fields name, description, logo.url, start_date` (company); an empty profile response is
  `AppResult.failure(RepositoryError.Unknown())`. Add constants `CREDITED_GAMES_PAGE_SIZE = 500` and
  `CREDITED_GAMES_MAX_PAGES = 10`. A private pager sends `sort id asc; limit 500; offset <n>;` and stops at the first
  page shorter than 500 or after 10 pages, concatenating the pages, and a failure on any page fails the whole call.
  `getGamesByIds` chunks the id list into pieces of at most 500 before paging and returns an empty success for an empty
  list without any request. Both games queries use
  `where … & game_type != ($excludedIds) & version_parent = null` with `excludedIds` built from
  `GameType.noisyTypes` as in the other methods; the developer one is
  `where involved_companies.company = $id & involved_companies.developer = true & …`. Map rows with `toGame()`.
- [X] T014 [P] Create `core/data/src/androidHostTest/kotlin/com/nikolasguillen/questlog/core/data/repository/GameRepositoryImplCharactersTest.kt`
  and `GameRepositoryImplDevelopersTest.kt` (follow `GameRepositoryImplDeveloperGamesTest.kt` for the MockK setup and
  query capture): each method sends the query above; a blank description maps to `null`; 500 then 120 rows give 620
  games over two requests; exactly 500 rows give a third request; 750 ids are chunked; a thrown `IgdbHttpException` on
  page two gives `AppResult.Failure` with the mapped `Http` error; an empty profile gives `Failure(Unknown)`; more than
  ten full pages stop at 5,000 games.
- [X] T015 [P] Create `core/data/src/androidHostTest/kotlin/com/nikolasguillen/questlog/core/data/mapper/CharacterMapperTest.kt`
  and `DeveloperMapperTest.kt`: image urls are rewritten to `t_cover_big` and `t_logo_med` with the `https:` prefix, a
  blank description is `null`, `developedGameCount` counts `developed`, and `start_date` `473299200` (Naughty Dog) maps
  to `1984` (473299200 is 1984-12-31 UTC; the original text said 1985 by mistake).

### Use cases

- [X] T016 [P] Create `core/domain/usecase/search/SearchCharactersUseCase.kt`: a plain class over `GameRepository` with
  `suspend operator fun invoke(query: String): AppResult<List<GameCharacter>>`, a pass-through, like
  `SearchGamesUseCase`.
- [X] T017 Create `core/domain/usecase/search/SearchDevelopersUseCase.kt`: `invoke(query)` returns the repository's list
  ranked and capped at `DEVELOPER_RESULTS_LIMIT = 100`. Order: exact name match (ignoring case) first; then names that
  start with the query; then the rest; within a group, higher `developedGameCount` first; then name ascending.
  Put the ranking in an `internal` top-level function so the test reaches it.
- [X] T018 [P] Create `core/domain/usecase/profile/GetCharacterProfileUseCase.kt` and `GetDeveloperProfileUseCase.kt`:
  pass-throughs over `getCharacterProfile` and `getDeveloperProfile`.
- [X] T019 [P] Create `core/domain/usecase/profile/GetCharacterGamesUseCase.kt` (`invoke(gameIds: List<Int>)`: an empty
  list returns `AppResult.success(emptyList())` with no repository call, otherwise `getGamesByIds`) and
  `GetDeveloperGamesUseCase.kt` (`invoke(developerId: Int)` over `getDevelopedGames`). Both return the games newest
  first by `releaseDate`, with games that have no release date last. Share the ordering through one `internal` function
  in `core/domain/usecase/profile/GameOrdering.kt`.
- [X] T020 Register `factoryOf` for the six new use cases in `core/domain/di/DomainKoin.kt`.
- [X] T021 [P] Create tests in `core/domain/src/androidHostTest/kotlin/com/nikolasguillen/questlog/core/domain/usecase/`:
  `search/SearchDevelopersUseCaseTest.kt` (exact before prefix before contains, ties by count then name, case
  insensitive, cap of 100) and `profile/GetCharacterGamesUseCaseTest.kt` / `GetDeveloperGamesUseCaseTest.kt` (newest
  first, undated last, empty ids make no repository call, a failure passes through). Follow the MockK and
  `runTest` pattern of `SearchResultFacetsTest.kt`.

### Navigation plumbing

- [X] T022 In `core/navigation/Routes.kt` add `@Serializable data class CharacterDetailRoute(val characterId: Int) :
  GameNavKey` and `@Serializable data class DeveloperDetailRoute(val developerId: Int) : GameNavKey`, with KDoc. Add
  `subclass(CharacterDetailRoute::class)` and `subclass(DeveloperDetailRoute::class)` to
  `core/navigation/GameNavSavedStateConfiguration.kt`, and add `CharacterDetailRoute(characterId = 2134)` and
  `DeveloperDetailRoute(developerId = 401)` to `everyRoute` in
  `core/navigation/src/androidHostTest/kotlin/com/nikolasguillen/questlog/core/navigation/RoutesSerializationTest.kt`.
- [X] T023 Add `onCharacterClick: (Int) -> Unit` and `onDeveloperClick: (Int) -> Unit` to `SearchScreen` and
  `SearchScreenContent` in `feature/search/SearchScreen.kt` (after `onGameClick`, before `onProfileClick`; update the
  `@QuestLogPreviews` calls with `{}`). Nothing calls them yet. In `shared/QuestLogNavDisplay.kt`'s `SearchRoute` branch,
  pass `onCharacterClick = { id -> val next = CharacterDetailRoute(id); if (backStack.lastOrNull() != next)
  backStack.add(next) }` and the same for `DeveloperDetailRoute`. Do not add `entryProvider` branches for the new routes
  yet: the existing `else -> NavEntry(key) { }` shows a blank page for them until US4.
- [X] T024 Run `./gradlew :core:network:testAndroidHostTest :core:data:testAndroidHostTest
  :core:domain:testAndroidHostTest :core:navigation:testAndroidHostTest :feature:search:compileAndroidMain
  --console=plain -q` and `./gradlew :androidApp:assembleDebug --console=plain -q`. All must pass.

**Checkpoint**: nothing is visible yet, but every query, mapper, use case and route exists and is tested.

---

## Phase 3: User Story 1 - Choose what to search for (Priority: P1) 🎯 MVP

**Goal**: A row of three chips under the expanded search bar picks the search type. A submitted search looks up only
that type. The hint text follows the chip, nothing below the chips changes, and the remote game suggestions are
silent for the other two types.

**Independent Test**: [quickstart.md](quickstart.md) S1–S3, S10–S12 and S18. Before US2 and US3 land, a submitted
Characters or Developers search reaches `EntityResults` state but the grid area stays empty (a deliberate intermediate
state, T031), so the view model tests (T030) carry the proof of "one lookup, one kind".

- [X] T025 [P] [US1] Create `feature/search/model/SearchType.kt`: `enum class SearchType { GAMES, CHARACTERS,
  DEVELOPERS }` with KDoc.
- [X] T026 [P] [US1] Add to `feature/search/src/commonMain/composeResources/values/strings.xml` (English only):
  `search_type_games` "Games", `search_type_characters` "Characters", `search_type_developers` "Developers",
  `search_placeholder_characters` "Search for a character…", `search_placeholder_developers` "Search for a
  developer…". `search_placeholder` ("Search for a game…") stays for Games.
- [X] T027 [P] [US1] Add `leadingIcon: (@Composable () -> Unit)? = null` as the last parameter of `CustomFilterChip` in
  `core/ui/component/CustomFilterChip.kt`, passed to `FilterChip`'s `leadingIcon`, and a preview with a leading icon.
  Existing callers compile unchanged.
- [X] T028 [US1] Create `feature/search/mapper/SearchTypeMapper.kt` with `internal fun SearchType.toLabelUiText():
  UiText` and `internal fun SearchType.toPlaceholderUiText(): UiText`, using `UiText.StringResource` over the strings
  of T026.
- [X] T029 [US1] In `feature/search/model/`: create `EntityResultUiModel.kt` (`@Immutable data class(val id: Int, val
  name: String, val imageUrl: String?)`); add `data class EntityResults(val items: List<EntityResultUiModel>) :
  SearchContentState` to `SearchContentState.kt` (never empty: an empty lookup is `Empty`); add
  `selectedSearchType: SearchType = SearchType.GAMES` and `resultsType: SearchType = SearchType.GAMES` to
  `SearchUiState.kt` with KDoc saying they differ only between picking a chip and submitting; add `data class
  OnSearchTypeSelected(val type: SearchType)` to `SearchUiEvent.kt`. Create
  `feature/search/mapper/EntityResultMapper.kt` with `GameCharacter.toEntityResultUiModel()` and
  `Developer.toEntityResultUiModel()`. (This mapper file is one more than the plan's tree lists; add it there in T076.)
- [X] T030 [US1] In `feature/search/SearchViewModel.kt` implement the state rules of [data-model.md](data-model.md):
  inject `SearchCharactersUseCase` and `SearchDevelopersUseCase`. `OnSearchTypeSelected(t)` sets
  `selectedSearchType` only, starts no request, and emits the current text on `suggestionsResetTrigger`. Change
  `performSearch(query, type: SearchType = _uiState.value.selectedSearchType)` to set `resultsType = type`, write the
  history for every type, and branch: `GAMES` is the existing code unchanged; `CHARACTERS` and `DEVELOPERS` call their
  use case and produce `SearchContentState.Empty` for an empty list, `EntityResults` otherwise, or
  `SearchContentState.Error(error.toUiText())`. `retrySearch()` passes `resultsType`, not `selectedSearchType`.
  `browseGenre` and `openShelf` set `selectedSearchType = GAMES` and `resultsType = GAMES` in the same `update` that
  sets `activeBrowse`. `initSearchSuggestions` still computes the history suggestions, then returns before the debounce
  and the remote lookup when `_uiState.value.selectedSearchType != SearchType.GAMES`, after clearing `gameSuggestions`
  and `isLoadingRemote`. `clearSearch` leaves both types alone. Register the new constructor parameters: they are plain
  use-case classes, so `viewModelOf(::SearchViewModel)` in `shared/di/ViewModelModule.kt` needs no edit.
- [X] T031 [US1] In `feature/search/components/SearchMainContent.kt` add a `SearchContentState.EntityResults` branch to
  the `when` that renders nothing yet (`Unit`), with a comment naming US2 and US3 as the ones that fill it. This is the
  documented intermediate state.
- [X] T032 [US1] Create `feature/search/components/SearchTypeChipRow.kt`:
  `internal fun SearchTypeChipRow(selected: SearchType, onSelected: (SearchType) -> Unit, modifier: Modifier = Modifier)`
  over `CustomChipRow`, one `CustomFilterChip` per `SearchType.entries` with `label = type.toLabelUiText().asString()`,
  `selected = type == selected`, and a `leadingIcon` that is `Icons.Default.Check` when selected and otherwise
  `Icons.Outlined.SportsEsports`, `Icons.Outlined.Person` or `Icons.Outlined.Business` for Games, Characters and
  Developers. Previews: each type selected.
- [X] T033 [US1] In `feature/search/components/SearchBars.kt`: give `SearchInputField` a `searchType: SearchType =
  SearchType.GAMES` parameter and make the placeholder text `browseText ?: searchType.toPlaceholderUiText().asString()`
  (replacing the direct `Res.string.search_placeholder` read). Give `ExpandedSearchBar` `selectedSearchType` and
  `onSearchTypeSelected`, and in its content lambda draw `SearchTypeChipRow` with a `HorizontalDivider` under it above
  the existing `when`, wrapping the `when` in a `Box(Modifier.weight(1f))` so the suggestions list and the home still
  fill the rest of the column. Thread the values through `SearchTopBar` (`uiState.selectedSearchType`,
  `onEvent(SearchUiEvent.OnSearchTypeSelected(it))`) and update every preview in the file.
- [X] T034 [US1] Extend `feature/search/src/androidHostTest/kotlin/com/nikolasguillen/questlog/feature/search/SearchViewModelTest.kt`
  (new use-case mocks in the setup) with one test per row of the state table in [data-model.md](data-model.md):
  choosing a chip changes only `selectedSearchType` and makes no use-case call; a submit under Characters calls only
  `SearchCharactersUseCase`, sets `resultsType = CHARACTERS` and ends in `EntityResults` (and `Empty` for an empty
  list); the same for Developers; the history records the text for every type; a genre browse and a shelf open reset
  both types to `GAMES`; typing under Characters yields history suggestions and no remote lookup and no
  `isLoadingRemote`; switching back to Games with text typed brings the remote lookup back.
- [X] T035 [US1] Run `./gradlew :feature:search:testAndroidHostTest --console=plain -q`, then
  `./gradlew :androidApp:assembleDebug --console=plain -q`. Hands-on, Android emulator: quickstart S1, S2, S3, S10, S11,
  S12 and S18. S10 shows the grid unchanged after a chip change and no search started; S11 needs the games grid back
  after a genre tap; with Characters or Developers submitted the grid area is empty by design until US2/US3.
  **Result (emulator, Pixel_10_Pro)**: S1, S2, S3 and S10 driven by hand and as expected; the Developers submit ended in the
  empty state (the games message, until US3 adds its own). S11, S12 and S18 were covered by the view-model tests only.

**Checkpoint**: the selector works and the view model looks up exactly one type per search.

---

## Phase 4: User Story 2 - Find a character and see its games (Priority: P1)

**Goal**: A Characters search shows a grid of circular portrait cards, with a placeholder for a missing image and the
right empty message. A tap goes to the character's page (US4).

**Independent Test**: [quickstart.md](quickstart.md) S4 and the characters half of S13. Until US4, tapping a card opens
a blank page and back returns to the results.

- [X] T036 [P] [US2] Add to `feature/search/…/composeResources/values/strings.xml`: `search_no_characters` "No
  characters found for this search", `search_characters_count` "%1$d characters found" and
  `character_card_content_description` "Character, %1$s".
- [X] T037 [P] [US2] Create `feature/search/components/CharacterPortrait.kt`:
  `internal fun CharacterPortrait(imageUrl: String?, modifier: Modifier = Modifier)`: an `AsyncImage` with
  `ContentScale.Crop` clipped to `CircleShape` and `contentDescription = null`; with no image, or on error, a
  `CircleShape` box in `MaterialTheme.colorScheme.surfaceContainerHighest` holding `Icons.Outlined.Person` at half the
  circle's width (use `fillMaxSize(0.5f)` so no `dp` literal is needed). Previews with and without an image.
- [X] T038 [P] [US2] Create `feature/search/components/CharacterResultCard.kt`: a `Column` with a square
  `CharacterPortrait` (`aspectRatio(1f)`, full card width), then the name, `maxLines = 2`, centered, `bodyMedium`, under
  it with `spacing.medium` between. The whole card is `clickable(role = Role.Button)` with `semantics(mergeDescendants
  = true) { contentDescription = stringResource(Res.string.character_card_content_description, item.name) }`.
- [X] T039 [US2] Create `feature/search/components/EntityResultGrid.kt`:
  `internal fun EntityResultGrid(items: List<EntityResultUiModel>, state: LazyGridState, modifier: Modifier = Modifier,
  card: @Composable (EntityResultUiModel, Modifier) -> Unit)`. Copy the geometry of `SearchResultGrid`: a
  `BoxWithConstraints`, `resultGridColumnCount(maxWidth, spacing.large, edge = spacing.large,
  isLandscapeShape(maxWidth, maxHeight))`, `LazyVerticalGrid(columns = SearchResultGridCells(columnCount, edge), state =
  state, horizontalArrangement/verticalArrangement = spacedBy(spacing.large), contentPadding =
  PaddingValues(bottom = ScrollToTopFabDefaults.ContentBottomPadding))`, `itemsIndexed` keyed by `item.id`, each card
  padded `start = edge` in column 0 and `end = edge` in the last column, with `animateItem(fadeOutSpec = null)`.
- [X] T040 [US2] In `feature/search/components/SearchMainContent.kt` pass `resultsType` and the two click lambdas down
  (add parameters), render `EntityResults` for `CHARACTERS` with `EntityResultGrid` and `CharacterResultCard`, and make
  the `Empty` message depend on `resultsType`: `search_no_results` for Games (unchanged), `search_no_characters` for
  Characters. Update the previews.
- [X] T041 [US2] In `feature/search/components/SearchSubHeader.kt` make `onOpenSort` and `onOpenFilters` nullable (the
  buttons are drawn only when non-null), add `resultsType: SearchType = SearchType.GAMES`, and pick the count string by
  type (`search_results_count`, `search_characters_count`, and US3's `search_developers_count`). In
  `SearchBars.kt`'s `bottomContent`, add a `SearchContentState.EntityResults` branch calling it with `items.size`,
  `resultsType`, and `null` callbacks.
- [X] T042 [US2] In `feature/search/SearchScreen.kt` replace `onGameClickWithCollapse` with a helper that takes any
  `(Int) -> Unit` and returns it wrapped with the same `launch { onClick(id); delay(SEARCH_BAR_COLLAPSE_DELAY);
  searchBarState.snapTo(0F) }`, and use it for `onGameClick` and `onCharacterClick` (and `onDeveloperClick` in T049).
  Pass `uiState.resultsType` and the click lambdas to `SearchMainContent`.
- [X] T043 [P] [US2] Add a test to `SearchViewModelTest.kt` pinning that a character result maps to
  `EntityResultUiModel(id, name, imageUrl)` in the API's relevance order, and add `EntityResultMapperTest.kt` in
  `feature/search/src/androidHostTest/kotlin/com/nikolasguillen/questlog/feature/search/mapper/` for both mappers
  (a null image stays null).
- [X] T044 [US2] Run `./gradlew :feature:search:testAndroidHostTest :androidApp:assembleDebug --console=plain -q`.
  Hands-on: quickstart S4 (portraits, Kratos first, placeholder for Young Kratos, "2 characters found", no sort or
  filter buttons, scroll-to-top works) and the characters half of S13; tapping a card shows a blank page and back
  returns with query, chip and scroll position intact.
  **Result (emulator)**: S4 as expected (round portraits, Kratos first, person placeholder for Young Kratos, "2 characters found", no sort or
  filter buttons); a tap opens a blank page and back returns to the results. The empty-state wording was covered by a preview only.

**Checkpoint**: characters can be searched and tapped. With US1 this is the first demonstrable slice.

---

## Phase 5: User Story 3 - Find a developer and see its games (Priority: P1)

**Goal**: A Developers search shows logo tiles in a shape different from game and character cards, with a placeholder
for a missing logo and the right empty message. A tap goes to the developer's page (US4).

**Independent Test**: [quickstart.md](quickstart.md) S8 (the cards part), the developers half of S13.

- [X] T045 [P] [US3] Add `logoTileContainerColor: Color` to `AppColors` in
  `core/designsystem/theme/AppColors.kt`, give it a value in each `AppColors(...)` call (the fallback in `AppColors.kt`
  and the light and dark instances in `core/designsystem/theme/QuestLogTheme.kt`), "light in both themes": define one
  raw constant `LogoTileSurface = Color(0xFFF4F4F4)` in `core/designsystem/theme/Color.kt` and use it in both
  schemes. Update the slot count in `core/designsystem/CLAUDE.md` if it states one.
- [X] T046 [P] [US3] Add to `feature/search/…/composeResources/values/strings.xml`: `search_no_developers` "No
  developers found for this search", `search_developers_count` "%1$d developers found" and
  `developer_card_content_description` "Developer, %1$s".
- [X] T047 [P] [US3] Create `feature/search/components/DeveloperLogo.kt`:
  `internal fun DeveloperLogo(imageUrl: String?, modifier: Modifier = Modifier)`: a box clipped to
  `MaterialTheme.shapes.large` on `MaterialTheme.appColors.logoTileContainerColor`, holding the image with
  `ContentScale.Fit` and `spacing.medium` padding; with no image or on error, `Icons.Outlined.Business` at
  `fillMaxSize(0.5f)` tinted `onSurfaceVariant` on the same tile. Previews with and without an image.
- [X] T048 [P] [US3] Create `feature/search/components/DeveloperResultCard.kt`, the same layout as
  `CharacterResultCard` with `DeveloperLogo` and `developer_card_content_description`.
- [X] T049 [US3] In `feature/search/components/SearchMainContent.kt` render `EntityResults` for `DEVELOPERS` with
  `EntityResultGrid` and `DeveloperResultCard`, and add `search_no_developers` to the `Empty` message selection. In
  `SearchScreen.kt` wrap `onDeveloperClick` with the collapse helper of T042. `SearchSubHeader` already handles the
  developers count (T041).
- [X] T050 [P] [US3] Add a developers case to `EntityResultMapperTest.kt` and a `SearchViewModelTest.kt` case checking
  the ranked order from `SearchDevelopersUseCase` reaches `EntityResults` unchanged.
- [X] T051 [US3] Run `./gradlew :feature:search:testAndroidHostTest :core:designsystem:compileAndroidMain
  :androidApp:assembleDebug --console=plain -q`. Hands-on: quickstart S8's card part ("capcom" gives light logo tiles in
  both themes, Capcom first), the developers half of S13, and S17 for the chips and both cards. Confirm a game card, a
  character card and a developer card differ by shape alone (SC-004).
  **Result (emulator)**: "capcom" gives 18 light logo tiles in both themes with Capcom first, and the building icon only where a company
  has no logo (a first build drew the icon behind every logo; the placeholder is now drawn only when there is no image or it fails).
  S13's developers half and S17 (TalkBack) were not driven by hand.

**Checkpoint**: both result types are searchable and tappable.

---

## Phase 6: User Story 4 - Read about a character or developer and browse their games (Priority: P1)

**Goal**: Both taps open a full-screen page like the wishlist detail: a header, the translatable description, then a
grid of every game, with save and long-press working as in Search.

**Independent Test**: [quickstart.md](quickstart.md) S5–S9, S16 and S7's save and back checks.

### Translation key (the step that wipes dev devices)

- [X] T052 [US4] **Back up first** (Settings, Account & backup, Back up now) on any device you care about: the next
  tasks change a Room entity under the `version = 1` policy, which wipes local data on the next install. Tell the owner
  this is about to happen before starting T053. (A checkpoint with no file edit; see research.md R7.)
- [X] T053 [US4] Create `core/domain/translation/TranslationSubject.kt`: `sealed interface TranslationSubject` with
  `data class Game(val id: Int)`, `data class Character(val id: Int)` and `data class Developer(val id: Int)` (the
  sealed hierarchy keeps its implementations in the one file). Change `GameDescriptionTranslator.translate` in
  `core/domain/translation/GameDescriptionTranslator.kt` to `translate(subject: TranslationSubject, description:
  String): String?` and update its KDoc. Rename `core/domain/usecase/translation/TranslateGameDescriptionUseCase.kt` to
  `TranslateDescriptionUseCase.kt` with `invoke(subject: TranslationSubject, description: String): String?`, and update
  its binding in `core/domain/di/DomainKoin.kt`.
- [X] T054 [US4] In `core/database/entity/TranslatedDescriptionEntity.kt` rename `gameId` to `subjectId`, add
  `subjectType: String`, and make the primary key `["subjectType", "subjectId", "languageTag"]`, updating the KDoc.
  Change `core/database/dao/TranslationDao.kt` to `getTranslation(subjectType: String, subjectId: Int, languageTag:
  String)`. Do not bump `version = 1` in `QuestLogDatabase.kt`. Run `./gradlew :core:database:compileAndroidMain
  --console=plain -q` so Room regenerates `core/database/schemas/com.nikolasguillen.questlog.core.database.QuestLogDatabase/1.json`,
  and stage that file with the entity change.
- [X] T055 [US4] Create `core/data/translation/TranslationSubjectKey.kt` with an `internal` mapping from
  `TranslationSubject` to `(type, id)` using the literals `"game"`, `"character"` and `"developer"`. Change
  `translateWithCache` in `core/data/translation/TranslationCache.kt` to take `subject: TranslationSubject` instead of
  `gameId`, reading and saving through the new DAO signature. Update
  `core/data/translation/AppleLanguageModelTranslator.kt` and
  `core/data/src/androidMain/kotlin/com/nikolasguillen/questlog/core/data/translation/GameDescriptionTranslatorImpl.kt`
  to the new `translate(subject, description)`. The Swift bridge is not touched.
- [X] T056 [US4] In `feature/game-detail/GameDetailViewModel.kt` swap `TranslateGameDescriptionUseCase` for
  `TranslateDescriptionUseCase`, calling it with `TranslationSubject.Game(gameId)`. Update
  `feature/game-detail/src/androidHostTest/kotlin/com/nikolasguillen/questlog/feature/gamedetail/GameDetailViewModelTest.kt`
  and the two translator tests in `core/data/src/androidHostTest/kotlin/com/nikolasguillen/questlog/core/data/translation/`
  to the new signatures, and add one case to `GameDescriptionTranslatorImplTest.kt`: a game and a character with the
  same id keep separate cache rows (the DAO is asked for `"game"` and `"character"` respectively).
- [X] T057 [US4] Update the translation paragraphs of `core/data/CLAUDE.md` (cache key is a subject, not a game id) and
  the heading of "`GameDescriptionTranslator`" in `specs/010-kmp-migration/contracts/platform-contracts.md` ("(unchanged
  signature)" becomes a note that the key is a `TranslationSubject`).
- [X] T058 [US4] Run `./gradlew :core:database:compileAndroidMain :core:data:testAndroidHostTest
  :core:domain:testAndroidHostTest :feature:game-detail:testAndroidHostTest --console=plain -q` and
  `./gradlew :androidApp:assembleDebug --console=plain -q`. All green before the next block.

### Shared UI moves (no behavior change)

- [ ] T059 [US4] Move `DescriptionTranslationState` from `feature/game-detail/model/DescriptionTranslationState.kt` to
  `core/ui/model/DescriptionTranslationState.kt` and make it `public`. Update the imports in `GameDetailUiState.kt`,
  `GameDetailViewModel.kt`, `GameDetailSuccessContent.kt` and `GameDetailViewModelTest.kt`.
- [ ] T060 [US4] Move the translation UI into `:core:ui`, keeping behavior: `GameDescriptionCard.kt` becomes
  `core/ui/component/DescriptionCard.kt` with `DescriptionCard` `public` (rename the composable and its previews);
  `TranslationBadge.kt`, `GeminiNanoBadge.kt`, `AppleIntelligenceBadge.kt` and the `expect` in
  `TranslationEngineBadge.kt` move to `core/ui/component/translation/` and stay `internal` (only `DescriptionCard`
  uses them, inside `:core:ui`); the actuals move to
  `core/ui/src/androidMain/kotlin/com/nikolasguillen/questlog/core/ui/component/translation/TranslationEngineBadge.android.kt`
  and `core/ui/src/iosMain/…/TranslationEngineBadge.ios.kt`. Move the strings `description_translate_action`,
  `description_show_original`, `description_retry_translation_action`, `description_translating`,
  `description_translation_info_action` and the four `powered_by_*` strings from
  `feature/game-detail/src/commonMain/composeResources/values/strings.xml` to
  `core/ui/src/commonMain/composeResources/values/strings.xml`, and `ic_gemini.xml` from
  `feature/game-detail/src/commonMain/composeResources/drawable/` to `core/ui/src/commonMain/composeResources/drawable/`.
  Inside the moved files `Res` is the module's own and imported bare (the old `CoreUiRes.string.description_title` and
  `got_it` become bare). Delete the originals. Update `GameDetailSuccessContent.kt` to call `DescriptionCard` with the
  `core.ui` import. Then `grep` `feature/game-detail/src` for each moved string name to confirm nothing else used it.
- [ ] T061 [US4] Create `core/ui/component/DetailHeader.kt`: `fun DetailHeader(title: String, cover: @Composable () ->
  Unit, modifier: Modifier = Modifier, description: String? = null, titleMaxLines: Int = 1, chips: @Composable
  RowScope.() -> Unit = {})` and `object DetailHeaderDefaults { val CoverWidth: Dp; val CoverHeight: Dp; val
  PlaceholderIconSize: Dp }`, extracted from `WishlistDetailHeader`: a `Row` with `spacedBy(spacing.large)`,
  `background(colorScheme.surfaceVariant)` and `padding(spacing.large)`; the `cover` slot; then a `Column(weight(1f))`
  holding the title (`titleLarge`, `SemiBold`, `maxLines = titleMaxLines`, ellipsis, `onSurface`), the optional
  description (`bodyLarge`, `onSurfaceVariant`, 2 lines, `padding(top = spacing.small)`) and the chips `Row`
  (`CenterVertically`, `spacedBy(spacing.medium)`, `padding(top = spacing.mediumLarge)`). The three defaults hold the
  existing `96.dp`, `128.dp` and `40.dp` literals that `WishlistDetailHeader` uses today. Rewrite
  `feature/wishlist/components/WishlistDetailHeader.kt` as a caller (its private `WishlistCover` becomes the `cover`
  argument, its chips the `chips` argument) with identical pixels, and keep its previews.
- [ ] T062 [US4] Update `core/ui/CLAUDE.md`: add `DetailHeader`, `DescriptionCard` and the translation badge family to the
  component inventory, and the leading icon to `CustomFilterChip`. Update the `TranslationEngineBadge` row of
  `specs/010-kmp-migration/contracts/platform-contracts.md` (module `:core:ui`).
- [ ] T063 [US4] Run `./gradlew :core:ui:testAndroidHostTest :feature:game-detail:testAndroidHostTest
  :feature:wishlist:testAndroidHostTest --console=plain -q` and `./gradlew :androidApp:assembleDebug --console=plain -q`.
  Hands-on (Android emulator): open a game with a description (translate, show original, badge dialog) and a wishlist
  header, and confirm both look exactly as before.

### Detail page

- [ ] T064 [P] [US4] Create in `feature/search/model/`: `EntityDetailSubject.kt` (**public** `sealed interface` with
  public `data class Character(val id: Int)` and `data class Developer(val id: Int)`, KDoc saying `:shared` builds it
  for the route argument); `EntityHeaderUiModel.kt` (`@Immutable data class(name: String, imageUrl: String?,
  foundedYearText: UiText?)`); `EntityProfileContentState.kt` (`@Immutable sealed interface`: `Loading`,
  `Error(message: UiText)`, `Success(header: EntityHeaderUiModel, description: String?)`);
  `EntityGamesContentState.kt` (`Loading`, `Empty`, `Error(message: UiText)`, `Success(games: List<GameItemUiModel>)`);
  `EntityDetailUiState.kt` (`subject`, `profile = Loading`, `games = Loading`, `gameCountText: UiText? = null`,
  `descriptionTranslation: DescriptionTranslationState = Unavailable`, `listSelectorState: ListSelectorState? = null`);
  `EntityDetailUiEvent.kt` (`OnRetry`, `TranslateDescription`, `ShowOriginalDescription`, `OnToggleSave(gameId)`,
  `OnOpenListSelector(gameId)`, `OnToggleListSelection(listId)`, `OnConfirmListSelection`, `OnDismissListSelector`);
  `EntityDetailUiEffect.kt` (`ShowSnackbar(message: UiText)`).
- [ ] T065 [P] [US4] Add to `feature/search/…/composeResources/values/strings.xml`: `entity_games_title` "Games",
  `entity_no_games` "No games to show yet", `entity_founded_year` "Founded %1$d", and the plural
  `entity_game_count` (one "%1$d game", other "%1$d games").
- [ ] T066 [US4] Create `feature/search/mapper/EntityDetailMapper.kt`: `CharacterProfile.toHeader()`,
  `DeveloperProfile.toHeader()` (`foundedYearText = UiText.StringResource(Res.string.entity_founded_year, year)` when
  `foundedYear != null`, otherwise `null`; a character's is always `null`), `Int.toGameCountText(): UiText` using
  `UiText.PluralResource(Res.plurals.entity_game_count, this, this)`, and `EntityDetailSubject.toTranslationSubject()`.
- [ ] T067 [US4] Create `feature/search/EntityDetailViewModel.kt` with `subject: EntityDetailSubject` as its first
  constructor parameter, then `GetCharacterProfileUseCase`, `GetDeveloperProfileUseCase`, `GetCharacterGamesUseCase`,
  `GetDeveloperGamesUseCase`, `TranslateDescriptionUseCase`, `GetTranslationModelStatusUseCase`,
  `GetWishlistedGameIdsUseCase`, `ToggleWishlistUseCase`, `GetWishlistAssignmentsUseCase`, `AddGameToListUseCase` and
  `RemoveGameFromListUseCase`. State is a `MutableStateFlow` with `_uiState.update`, effects a
  `Channel<EntityDetailUiEffect>(Channel.BUFFERED)` + `receiveAsFlow()`, events one exhaustive `onEvent`. Lifecycle as
  in [data-model.md](data-model.md): a character loads the profile, then its games from `profile.gameIds`; a developer
  loads the profile and its games in parallel. Games success non-empty sets `Success` and `gameCountText`, empty sets
  `Empty` and a count of 0, failure sets `Error` and leaves the header. Profile success sets
  `descriptionTranslation` to `Available` when a description exists and `GetTranslationModelStatusUseCase()` is
  `READY`, else `Unavailable`. `TranslateDescription` is ignored while `InProgress`, sets `InProgress`, calls
  `TranslateDescriptionUseCase(subject.toTranslationSubject(), description)` and sets `Ready(text)` or `Failed`;
  `ShowOriginalDescription` sets `Available`. Keep the loaded domain `Game` list so save and the list selector can find
  a game by id; collect `GetWishlistedGameIdsUseCase` for the view model's life and patch `isSaved` in place (the
  pattern of `SearchViewModel.observeWishlistedGameIds`). Save, the list selector and its confirm/toggle/dismiss copy
  the behavior of `SearchViewModel` (including the `added_to_wishlist` and `removed_from_wishlist` snackbar naming the
  default list). `OnRetry` reloads the profile when it is `Error` (and the games after it for a character), otherwise
  the games when they are `Error`.
- [ ] T068 [US4] Create `feature/search/src/androidHostTest/kotlin/com/nikolasguillen/questlog/feature/search/EntityDetailViewModelTest.kt`
  (KDoc header, `StandardTestDispatcher`, `setMain`/`resetMain`, MockK use cases): a character profile then its games
  (the game use case is called with `profile.gameIds`); a developer's two requests both run; the chip text for 1 and 2
  games; an empty list is `Empty`; description `Available` only when a description exists and the model is `READY`, and
  `Unavailable` otherwise; translate goes `InProgress` then `Ready`, `null` goes `Failed`, a second translate while
  `InProgress` is ignored, and show-original goes back to `Available`; save toggles through the use case and sends the
  snackbar naming the default list; a wishlisted-ids emission patches `isSaved`; the list selector opens, toggles and
  confirms with the right add and remove calls.
- [ ] T069 [P] [US4] Create `feature/search/components/EntityDetailTopBar.kt`: a `TopAppBar` with an empty title and a
  back `IconButton` (`Icons.AutoMirrored.Filled.ArrowBack`, `CoreUiRes.string.back_content_description`), following
  `feature/wishlist/components/WishlistTopBar.kt`.
- [ ] T070 [US4] Create `feature/search/EntityDetailScreen.kt` per [contracts/ui-contract.md](contracts/ui-contract.md):
  the public `EntityDetailScreen(viewModel, onGameClick, onBackClick, modifier)` with
  `@Suppress("ParamsComparedByRef")` collecting state with `collectAsStateWithLifecycle()` and effects inside
  `repeatOnLifecycle(STARTED)` (snackbars as in `SearchScreen`), delegating to `internal fun EntityDetailContent(...)`.
  Layout: `Scaffold` with `EntityDetailTopBar`, `ScrollToTopFab`, `SnackbarHost`, and `ListSelectorSheet` while
  `listSelectorState != null`. `profile = Loading` shows `LoadingPage`, `Error` shows `ErrorPage` with `OnRetry`. On
  `Success`, one `LazyVerticalGrid` with `SearchResultGridCells` over its own space: a full-span `DetailHeader`
  (character cover: `CharacterPortrait` in a `CoverWidth` square; developer cover: `DeveloperLogo` in the same square;
  `titleMaxLines = 2`; chips: a `CustomInfoChip` with `gameCountText` once known, and one with `foundedYearText` when
  not null; the chip style of `WishlistDetailHeader`); a full-span `DescriptionCard` only when `description != null`;
  a full-span "Games" title; then `VerticalGameCard` per game (`onSaveClick`, long press to the list selector with the
  `choose_list_content_description` label, as in `SearchResultGrid`). `EntityGamesContentState.Loading` shows a
  full-span `ControllerLoadingAnimation`, `Empty` a full-span `EmptyPage(entity_no_games)`, `Error` a full-span
  `ErrorPage` with `OnRetry`. Previews: character, developer, loading, games error, no description.
- [ ] T071 [US4] In `shared/QuestLogNavDisplay.kt` add the two `entryProvider` branches from the UI contract:
  `is CharacterDetailRoute -> NavEntry(key) { … koinViewModel<EntityDetailViewModel> { parametersOf(
  EntityDetailSubject.Character(key.characterId)) } … }` and the developer one, each calling `EntityDetailScreen` with
  `onGameClick` pushing `GameDetailRoute(gameId)` (plain, not `fromSearch`, guarded by the usual `lastOrNull()` check)
  and `onBackClick = { navigateBack(backStack, twoPaneEnabled) }`. Add `viewModelOf(::EntityDetailViewModel)` to
  `shared/di/ViewModelModule.kt` and `EntityDetailSubject::class` to `extraTypes` in
  `shared/src/androidHostTest/kotlin/com/nikolasguillen/questlog/shared/di/KoinGraphTest.kt`.
- [ ] T072 [US4] Run `./gradlew :feature:search:testAndroidHostTest :shared:testAndroidHostTest --console=plain -q`,
  `./gradlew test --console=plain -q` and `./gradlew :androidApp:assembleDebug --console=plain -q`. Hands-on (Android):
  quickstart S5, S6 (on a device whose language is not English and whose model is ready, translate Kratos's description
  and then open the game with the same id as a character to see its own translation intact), S7, S8's page half
  (Capcom's page shows every game), S9 (Naughty Dog: none of the games is a port, bundle or edition), and S16 (two-pane
  window: the page covers both panes, no rail, and back returns to the panes).

**Checkpoint**: the full feature works for both types on Android.

---

## Phase 7: User Story 5 - Searching stays fast and forgiving (Priority: P3)

**Goal**: A failure of a search or of the games on a page is contained and retryable.

**Independent Test**: [quickstart.md](quickstart.md) S14 and S15.

- [ ] T073 [US5] Extend `SearchViewModelTest.kt`: a Characters search that fails sets `SearchContentState.Error` with the
  mapped message; `OnRetrySearch` after picking a different chip without submitting re-runs the lookup for `resultsType`,
  not `selectedSearchType`; a developers failure followed by a successful retry ends in `EntityResults`; cancelling a
  slow search by submitting a second one lands only the second result.
- [ ] T074 [US5] Extend `EntityDetailViewModelTest.kt`: a games failure keeps `profile = Success` and sets
  `games = Error`, and `OnRetry` reloads only the games; a profile failure sets `profile = Error`, `OnRetry` reloads it
  (and the games after it for a character) and a character's games request is not made before the profile succeeds; a
  `Failed` translation followed by `TranslateDescription` retries.
- [ ] T075 [US5] Hands-on, Android and (Mac) iOS: quickstart S14 (airplane mode on a Characters search, then retry) and
  S15 (cut the network while a developer page loads its games, restore, retry; the header and description stay).
  Confirm the error text comes from `RepositoryError.toUiText()` and not a new string.

**Checkpoint**: every story is verified, including its failure paths.

---

## Phase 8: Polish & cross-cutting concerns

- [ ] T076 [P] Add `specs/019-search-characters-companies/plan.md`'s source tree entry for
  `feature/search/mapper/EntityResultMapper.kt` (created in T029), and tick the finished tasks in this file.
- [ ] T077 [P] Review every new composable and model against the root `CLAUDE.md` rules: one type per file, `internal`
  by default (the public ones are `EntityDetailScreen`, `EntityDetailViewModel`, `EntityDetailSubject`, `DetailHeader`,
  `DescriptionCard`, `DescriptionTranslationState`), no hardcoded display text, no `dp` literal outside
  `DetailHeaderDefaults`, `collectAsStateWithLifecycle()`, KDoc in English, a `@QuestLogPreviews` on each composable.
- [ ] T078 Add a `docs/tech-debt.md` entry (Technical risks) for "a `"` in a search query is interpolated unescaped into
  every IGDB query and returns an HTTP 400" (research R15; the owner approved it). Do not change the existing queries.
- [ ] T079 Run `./gradlew test --console=plain -q` and `./gradlew :androidApp:assembleDebug --console=plain -q` once
  more from a clean state and compare with the T001 baseline: no test deleted or weakened, no new failure.
- [ ] T080 **Mac only**: run `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64 -q` and the `xcodebuild` command of
  the root `CLAUDE.md`, then quickstart S1–S18 on the simulator. The moved `TranslationEngineBadge` `actual` for iOS and
  the reworked `AppleLanguageModelTranslator` are the pieces only a Mac can compile. Say explicitly in the hand-off
  that this was not run if no Mac was available.
- [ ] T081 Work through the whole of [quickstart.md](quickstart.md) S1–S18 once on Android and record anything that
  differs from the expected column at the end of this file.

---

## Dependencies & Execution Order

### Phase dependencies

- **Phase 1 (Setup)**: no dependencies.
- **Phase 2 (Foundational)**: depends on Phase 1 and **blocks every story**.
- **Phase 3 (US1)**: depends on Phase 2.
- **Phase 4 (US2)** and **Phase 5 (US3)**: depend on US1 (they fill the `EntityResults` branch and use the type state).
  They are independent of each other except that both edit `SearchMainContent.kt`, `SearchScreen.kt` and `strings.xml`,
  so do them one after the other.
- **Phase 6 (US4)**: depends on Phase 2 only for its data, but its entry points are the cards of US2 and US3, so it
  follows them. Its translation-key block (T052–T058), shared-UI block (T059–T063) and page block (T064–T072) run in
  that order.
- **Phase 7 (US5)**: depends on US1, US2, US3 and US4.
- **Phase 8 (Polish)**: depends on every story.

### Story dependencies

US1 → US2 → US3 → US4 → US5. Each phase ends in a buildable, test-passing state. After US4, deliver in this order if
trimming is needed: US1 + US2 + US4 is the smallest end-to-end slice (a character search that reaches a page of games),
and US3 adds the developer half.

### Parallel opportunities

- **Phase 2 network and models**: T002, T003, T004 (three DTO files); T008 (four model files) can start with them;
  T006 and T007 after T005.
- **Phase 2 mappers and use cases**: T010 and T011 together; T016, T018, T019 together; T014, T015 and T021 (separate
  test files) together once their subjects exist.
- **US1**: T025, T026 and T027 together; T028 after T025 and T026.
- **US2**: T036, T037 and T038 together; **US3**: T045, T046, T047 and T048 together.
- **US4**: T064, T065 and T069 together; the translation block (T053–T057) and the shared-UI block (T059–T062) touch
  different modules and can be done by two people, but T059 needs T053's rename to be done to avoid conflicts in
  `GameDetailViewModel.kt`.

### Parallel example: Foundational

```text
T002 IgdbImage.kt            T003 IgdbCharacter.kt            T004 IgdbCompany.kt            T008 four core/model files
        └──────────────────────────────┬───────────────────────────────┘
                                       ▼
                         T005 service methods → T006 tests, T007 CLAUDE.md
                                       ▼
        T009 image url helper → T010 CharacterMapper ∥ T011 DeveloperMapper → T012+T013 repository → T014/T015 tests
                                       ▼
        T016 ∥ T018 ∥ T019 use cases → T017 ranking → T020 Koin → T021 tests → T022/T023 routes + lambdas → T024 verify
```

---

## Implementation Strategy

### MVP first (User Story 1, then 2 and 4)

1. Phases 1 and 2: the data layer, with the whole network and domain behind tests.
2. Phase 3 (US1): the selector. Stop and validate S1–S3: it works on its own as a view-model-proven selector.
3. Phase 4 (US2): characters in the grid. Stop and validate S4.
4. Phase 6 (US4) next, ahead of US3, if a character-only demo is wanted: the page, the translation key and the shared
   UI moves are all independent of the developer cards.

### Incremental delivery

Foundational → US1 (selector) → US2 (characters) → US3 (developers) → US4 (page) → US5 (failure verification) → polish.
After each phase `./gradlew test` and `:androidApp:assembleDebug` pass. T052 is the one irreversible step for local
data, so it comes right before the first Room edit and has its own warning.

## Notes

- Verify each hands-on task on a device: the project has no Compose UI tests. The scenario numbers refer to
  [quickstart.md](quickstart.md).
- iOS steps need a Mac. When none is available, say so rather than assuming.
- Do not edit anything under `**/build/generated/**`, and never commit IGDB credentials.
- Do not "fix" a known deviation in `docs/tech-debt.md` while working here (T078 adds the R15 entry, which the owner approved).
