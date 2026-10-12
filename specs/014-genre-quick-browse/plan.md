# Implementation Plan: Genre Quick-Browse in Search

**Branch**: `014-genre-quick-browse` | **Date**: 2026-10-12 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/014-genre-quick-browse/spec.md`

## Summary

The expanded search overlay gains a "Browse by genre" row of five chips above the recent searches. The
chips are the user's strongest taste genres, padded with RPG, Shooter, Adventure, Strategy and Indie.
Tapping one closes the overlay and fills the existing results grid with released games of that genre from
the last two years that have at least 10 ratings, the most rated first. The search bar stays empty and its
placeholder reads "genre: RPG". The clear button, the back arrow and the system back gesture return to
Discover. The sort sheet's "Relevance" reads "Top picks" during a browse. The filter sheet leaves out the
genre being browsed.

Technically:
- **Data.** One new `GameRepository` method, `getPopularGamesByGenre(genreId)`, sends a single `/games`
  query: `sort total_rating_count desc`, with a released-only bound, a 2-year window and a floor of 10
  ratings. It is not cached, like `searchGames`. The Discover `getGamesByGenre` is left alone.
- **Domain.** Two new use cases in `usecase/search/`:
  - `GetBrowseGenresUseCase` builds the five chips from the taste profile. Its code moves out of
    `GetTasteProfileUseCase` into a shared `internal` function.
  - `BrowseGenreUseCase` returns the same `SearchResult` a text search does, without the browsed genre
    among the facets.
- **Model and UI.** `:core:model` gets a `KnownGenre` enum of IGDB's 23 genre ids. `:core:ui` gets
  `Genre.toLabelUiText()` and 23 short, localisable genre strings.
- **ViewModel.** `SearchViewModel` keeps the browse as `activeGenre: BrowseGenreUiModel?` beside the
  existing `contentState`. Every result renders through the existing `SearchContentState` and
  `SearchResultGrid`. Starting any search now cancels the one in flight.

## Technical Context

**Language/Version**: Kotlin 2.4.20, JVM toolchain 21, Java 11 bytecode

**Primary Dependencies**: Compose Multiplatform 1.12.1 (Material 3: JetBrains 1.12.0-alpha03 on iOS, Jetpack
1.5.0-beta01 on Android), Koin 4.2.2, Ktor 3 + kotlinx.serialization (IGDB apicalypse), kotlinx-coroutines
Flow, Compose resources. **No new dependency.**

**Storage**: None. Nothing is persisted (FR-031). No Room, DataStore or schema change.

**Testing**: JUnit4 + MockK + kotlinx-coroutines-test (`StandardTestDispatcher`, `setMain`/`resetMain`) in
`src/androidHostTest`. The repository test uses the captured-query-body style of
`GameRepositoryImplPopularGamesTest`.

**Target Platform**: Android (minSdk 29, compile/targetSdk 37) and iOS 16+. Both are built from
`commonMain`, and nothing in this feature is platform-specific.

**Project Type**: Kotlin Multiplatform mobile app (19 Gradle modules)

**Performance Goals**: A tap on a chip shows the loading state immediately. Results take one IGDB round
trip, the same cost as a text search. The chip row comes from local data and is ready before the overlay
finishes opening.

**Constraints**: No new module edge. `:core:model` stays plain Kotlin. No `SavedStateHandle`. No
`android.*`/`java.*` in `commonMain`. Dates come from `kotlin.time.Clock`, as `GameRepositoryImpl` already
uses. Genres are compared by id only. Every label is a resource (`UiText`).

**Scale/Scope**: One screen. Five modules touched (`core:model`, `core:domain`, `core:data`, `core:ui`,
`feature:search`). About 10 new source files and 12 changed ones, plus 5 test files: 4 new and 1 extended.

No unknowns remain. Every design question is resolved in [research.md](research.md) (R1–R8).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-checked after Phase 1 design. Both pass.*

| Principle | Check | Result |
|-----------|-------|--------|
| **I. Module boundaries** | `:feature:search` reaches the new code only through `:core:domain` use cases, a `:core:model` type (`Genre`) and a `:core:ui` mapper. These are all existing edges. `KnownGenre` is plain Kotlin in `:core:model`. The repository method is declared in `:core:domain` and implemented in `:core:data`. No navigation change. **No new edge.** | ✅ |
| **II. Typed errors** | `getPopularGamesByGenre` catches its exception and maps it through `toRepositoryError()`, which rethrows `CancellationException` (contract G8). It returns `AppResult` because it touches the network. `BrowseGenreUseCase` uses `AppResult.map`. The UI shows errors through `RepositoryError.toUiText()` via the existing `SearchContentState.Error`. | ✅ |
| **III. UI renders, it does not decide** | Chip order (use case), facet exclusion (use case), sort label and "Top picks" order (ViewModel and mapper), placeholder text and clear-button visibility (derived from `activeGenre`) are all decided outside composables. Composables send `OnGenreBrowseTriggered`, `OnClearSearch` and `OnRetrySearch` through the single `onEvent`. Every new text is a `UiText` or a `stringResource`. `BrowseGenreUiModel` is `@Immutable`. State is collected with `collectAsStateWithLifecycle()` as today. | ✅ |
| **IV. Reuse first** | Reused: `SearchContentState`, `SearchResultGrid`, `VerticalGameCard`, `LoadingPage`/`ErrorPage`/`EmptyPage`, `CustomChipRow`, the sort and filter sheets, the chip colour tokens, and `MaterialTheme.spacing` for every gap. One new feature-local composable, `GenreChip`: nothing in `core/ui` is a button styled as a selected chip, and `CustomFilterChip` has the wrong semantics (R7). No new repository: one method on the single `GameRepository`. | ✅ |
| **V. Local verification** | Verification uses `compileAndroidMain`, `testAndroidHostTest` per module, `./gradlew test` and `:androidApp:assembleDebug`. New tests go in the existing source sets of `core/domain`, `core/data`, `core/ui` and `feature/search`. `KoinGraphTest` covers the two new bindings. No CI or lint gate is assumed. | ✅ |
| **Persistence** | Nothing is persisted. The DB version is not touched. | ✅ |
| **Injection** | Two `factoryOf` lines in `domainModule`. `SearchViewModel` gains two constructor parameters, which `viewModelOf(::SearchViewModel)` resolves unchanged. No `SavedStateHandle`. | ✅ |
| **KMP** | All new code is in `commonMain`. No platform contract is needed. iOS behaviour is checked in quickstart §4, and anything that could not be run there is recorded as not run. | ✅ |
| **One type per file** | `KnownGenre.kt`, `BrowseGenreUiModel.kt`, `GetBrowseGenresUseCase.kt` and `BrowseGenreUseCase.kt` each hold one type. The two `internal` functions (`TasteProfileBuilder.kt`, `SearchResultFacets.kt`) are top-level functions, not types. The new event joins the sealed `SearchUiEvent` file, which is the allowed exception. | ✅ |

**Changes to existing behaviour, called out** (none violates a principle, but each touches more than the
new path):
1. Starting a text search now cancels a text search still in flight (R5). This was a latent race, and it
   is fixed because FR-018 runs through the same code.
2. Filter-sheet genre labels use the new short labels for text searches too, such as "RPG" instead of
   "Role-playing (RPG)" (R3). Reverting it is a one-line change.
3. `GetTasteProfileUseCase` and `SearchGamesUseCase` move code into shared `internal` functions. Their
   behaviour does not change, and existing tests cover it.

## Project Structure

### Documentation (this feature)

```text
specs/014-genre-quick-browse/
├── spec.md
├── plan.md               # this file
├── research.md           # R1–R8
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── genre-browse.md       # repository + use cases
│   └── search-screen-ui.md   # what the screen shows and announces
├── checklists/requirements.md
└── tasks.md              # /speckit-tasks (not created here)
```

### Source Code (repository root)

```text
core/model/src/commonMain/kotlin/com/nikolasguillen/questlog/core/model/
└── KnownGenre.kt                                   NEW   enum: IGDB genre id + igdbName, fromId(), BROWSE_DEFAULTS

core/domain/src/commonMain/kotlin/com/nikolasguillen/questlog/core/domain/
├── repository/GameRepository.kt                    EDIT  + getPopularGamesByGenre(genreId)
├── usecase/discover/
│   ├── TasteProfileBuilder.kt                      NEW   internal List<Game>.toTasteProfile() (moved code)
│   └── GetTasteProfileUseCase.kt                   EDIT  delegates to toTasteProfile()
├── usecase/search/
│   ├── SearchResultFacets.kt                       NEW   internal List<Game>.toSearchResult(excludedGenreId)
│   ├── SearchGamesUseCase.kt                       EDIT  delegates to toSearchResult()
│   ├── GetBrowseGenresUseCase.kt                   NEW   Flow<List<Genre>>: 5 chips, taste first
│   └── BrowseGenreUseCase.kt                       NEW   AppResult<SearchResult> for one genre
└── di/DomainKoin.kt                                EDIT  + 2 factoryOf
core/domain/src/androidHostTest/.../usecase/search/
├── GetBrowseGenresUseCaseTest.kt                   NEW
└── BrowseGenreUseCaseTest.kt                       NEW

core/data/src/commonMain/kotlin/com/nikolasguillen/questlog/core/data/repository/
└── GameRepositoryImpl.kt                           EDIT  + getPopularGamesByGenre, 3 GENRE_BROWSE_* constants
core/data/src/androidHostTest/.../repository/
└── GameRepositoryImplGenreBrowseTest.kt            NEW

core/ui/src/commonMain/
├── kotlin/com/nikolasguillen/questlog/core/ui/mapper/GenreMapper.kt   NEW   Genre.toLabelUiText()
└── composeResources/values/strings.xml                               EDIT  + 23 genre_* strings
core/ui/src/androidHostTest/.../mapper/GenreMapperTest.kt              NEW

feature/search/src/commonMain/
├── kotlin/com/nikolasguillen/questlog/feature/search/
│   ├── SearchViewModel.kt                          EDIT  browseGenre(), observeBrowseGenres(), cancel-on-new-search,
│   │                                                     retry/clear/submit aware of activeGenre, "Top picks" order
│   ├── SearchScreen.kt                             EDIT  onBrowseGenre lambda, BackHandler condition
│   ├── model/
│   │   ├── BrowseGenreUiModel.kt                   NEW
│   │   ├── SearchUiState.kt                        EDIT  + browseGenres, activeGenre
│   │   └── SearchUiEvent.kt                        EDIT  + OnGenreBrowseTriggered
│   ├── mapper/SearchUiMapper.kt                    EDIT  toGenreFilters() → toLabelUiText(); toBrowseGenreUiModels();
│   │                                                     withRelevanceLabel(isGenreBrowse)
│   └── components/
│       ├── GenreBrowseSection.kt                   NEW   heading + CustomChipRow of GenreChip (AssistChip)
│       ├── SearchBars.kt                           EDIT  overlay layout, placeholder, X visibility, field semantics
│       ├── SearchMainContent.kt                    EDIT  activeGenre → empty-state message
│       └── SearchPlaceholders.kt                   EDIT  EmptySearchPlaceholder takes its message
└── composeResources/values/strings.xml             EDIT  + browse_by_genre, genre_browse_placeholder,
                                                          sort_top_picks, genre_browse_no_results
feature/search/src/androidHostTest/.../SearchViewModelTest.kt          EDIT  + browse cases (see below)

core/ui/CLAUDE.md                                   EDIT  mapper inventory: + GenreMapper.kt
docs/tech-debt.md                                   EDIT  coverage line: core/ui and core/domain search coverage
```

`:shared` needs no edit: `viewModelOf(::SearchViewModel)` picks up the new constructor parameters, and
`KoinGraphTest` checks they resolve.

**Structure Decision**: The existing multi-module layout is used as it is. Each piece goes in the layer that
already owns its kind: query in `:core:data`, rules in `:core:domain`, the id table in `:core:model`, labels
in `:core:ui`, state and rendering in `:feature:search`.

### `SearchViewModelTest` cases to add

1. A chip tap → `activeGenre` set, `Loading` then `Success`; `AddSearchToHistoryUseCase` **never** called.
2. The browsed genre is absent from `Success.filters` (the use case's facets are passed through).
3. Sort label "Top picks" during a browse and "Relevance" after clear / after a submitted query; `selected`
   and `descending` unchanged throughout.
4. "Top picks" descending = server order; ascending = reversed; still true after a filter is applied.
5. Retry after `Error` calls `BrowseGenreUseCase` again with the same id and not `SearchGamesUseCase`.
6. `OnClearSearch` → `activeGenre == null`, `contentState == Idle`.
7. `OnSearchTriggered` during a browse → `activeGenre == null`; text results shown.
8. Two chips in a row while the first is still suspended → only the second's results land in state.
9. `browseGenres` mirrors `GetBrowseGenresUseCase`'s emissions, mapped by id.

## Follow-ups (not in this feature)

- The Discover "Because you like %1$s" title and the game detail genre list still show IGDB's raw genre
  names. Switching them to `Genre.toLabelUiText()` is a one-line change each. Discover lanes are out of
  scope here.
- The two thresholds (R1) are educated guesses. Quickstart step 1 checks them against live data once; they
  should be revisited if users find a default genre thin.

## Complexity Tracking

No constitution violations. Nothing to justify.
