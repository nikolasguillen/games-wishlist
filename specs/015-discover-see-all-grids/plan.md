# Implementation Plan: See-All Grids for Discover Rows

**Branch**: `015-discover-see-all-grids` | **Date**: 2026-10-13 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/015-discover-see-all-grids/spec.md`

## Summary

Every Discover shelf (the personalised ones, "Most anticipated" and "Popular this month") now shows at most
six games. A shelf holding more gets an arrow at the end of its title row, and the whole row is the tap
target. Tapping it replaces the feed with the committed results grid that spec 014 introduced:
- The grid holds every game of that shelf in shelf order.
- It opens at once, with no loading state and no network request.
- The search bar is empty and its placeholder is the shelf's title.
- "Top picks" means the shelf's own order, and filters come from the shelf's games.
- X, the back arrow and system back return to the feed at the scroll position it had.

Technically:
- **No new mode.** 014's `activeGenre: BrowseGenreUiModel?` becomes `activeBrowse: SearchBrowseUiModel?`, a
  sealed `Genre | Shelf`. Every site that asked "is a browse active" keeps a single null check (research R3).
- **No new data path.** The ViewModel already keeps the domain `DiscoverFeed` (`discoverFeed`, used by
  `findGame`). One mapper function, `DiscoverFeed.shelfGames(key)`, defines a shelf's games, and the capped row
  and the uncapped grid both read it (R1).
- **Identity.** A new `DiscoverShelfKey`: `Lane(DiscoverLane)` for the generic lanes, and `ByGenre(id)` /
  `ByDeveloper(id)` for the personalised shelves. It uses the signal's id, not the shelf's index, so a feed
  refresh between render and tap cannot open the wrong shelf (R2).
- **One shelf shape.** `DiscoverShelfUiModel(key, title, games ≤ 6, hasMore)` replaces
  `RecommendedShelfUiModel`. `DiscoverContentState.Content.popular` and `upcoming` become nullable shelves.
  The cap and `hasMore` are decided in the mapper (R7).
- **One cross-module touch, by owner decision.** `toSearchResult` (the facet function) goes from `internal`
  to public in `:core:domain`, so the shelf grid's filter sheet uses the same facet rule as a text search
  (R5). Spec FR-027 was amended to say so.

## Technical Context

**Language/Version**: Kotlin 2.4.20, JVM toolchain 21, Java 11 bytecode

**Primary Dependencies**: Compose Multiplatform 1.12.1 (Material 3: JetBrains 1.12.0-alpha03 on iOS, Jetpack
1.5.0-beta01 on Android), Koin 4.2.2, kotlinx-coroutines Flow, Compose resources. **No new dependency.**

**Storage**: None. Nothing is persisted (FR-025). There is no Room, DataStore or schema change. The lane cache
is untouched (FR-004).

**Testing**: JUnit4 + MockK + kotlinx-coroutines-test (`StandardTestDispatcher`, `setMain`/`resetMain`) in
`feature/search/src/androidHostTest`, with use cases mocked as `SearchViewModelTest` does. There are no
Compose UI tests in this project (`docs/tech-debt.md`), so the title row's semantics and size are checked
hands-on (quickstart §3, rows 14–16).

**Target Platform**: Android (minSdk 29, compile/targetSdk 37) and iOS 16+. Everything is in `commonMain`, and
nothing is platform-specific.

**Project Type**: Kotlin Multiplatform mobile app (19 Gradle modules)

**Performance Goals**: The grid appears in the same frame as the tap. It is built synchronously from data in
memory: a facet pass and a sort over at most a few hundred games. No network, no disk.

**Constraints**:
- No new module, nav route or dependency edge.
- `:core:model` stays plain Kotlin. No `SavedStateHandle`. No `android.*`/`java.*` in `commonMain`.
- Shelves are compared by id or enum only.
- Every label is a resource (`UiText`). No new `dp` literal.

**Scale/Scope**:
- One screen, two modules: `:feature:search`, plus a one-word visibility change in `:core:domain`.
- 3 new source files, 1 deleted, about 10 changed, 1 new test file, and 1 test file extended.

No unknowns remain. Every design question is resolved in [research.md](research.md) (R1–R11).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-checked after Phase 1 design. Both pass.*

| Principle | Check | Result |
|-----------|-------|--------|
| **I. Module boundaries** | All the work is in `:feature:search`. The one other touch is `:core:domain`'s `toSearchResult` becoming public, and `:feature:search` → `:core:domain` is an **existing** edge. `DiscoverLane` comes from `:core:model`, also an existing edge. No navigation change: the grid is a `SearchContentState`, not a route. `feature/*` still depends only on `:core:common`, `:core:model`, `:core:domain`, `:core:ui`, `:core:navigation` and `:core:designsystem`. **No new module, route or edge.** | ✅ |
| **II. Typed errors** | There is no new error path. The shelf grid never reaches `Error` (R6). The existing `Error` rendering is unchanged. | ✅ |
| **III. UI renders, it does not decide** | Decided outside composables: the cap and `hasMore` (mapper), shelf identity (`DiscoverShelfKey`, by id/enum), the shelf's games (`shelfGames`, mapper), the placeholder format (`toPlaceholder`, mapper), "Top picks" order and label (ViewModel/mapper), and filters (ViewModel). The composable tests `hasMore`, renders `title.asString()`, and sends `OnShelfSeeAllTriggered` through the single `onEvent`. New UI types are `@Immutable`. State is still collected with `collectAsStateWithLifecycle()`. | ✅ |
| **IV. Reuse first** | Reused: 014's committed-grid mode (`SearchContentState`, `SearchResultGrid`, `VerticalGameCard`), the sort and filter sheets, `withRelevanceLabel` / "Top picks", `findGame`, `discoverFeed`, `toSearchResult`, `DiscoverLane`, `SeeAllResultsRow`'s arrow idiom (`ArrowForwardIos`, `spacing.mediumLarge`), and `MaterialTheme.spacing` for every gap. Nothing in `core/ui/component/` or `util/modifiers/` is a heading with a trailing action, so the title row stays private to `DiscoverFeed.kt` (R7). The 48dp target uses Material's `minimumInteractiveComponentSize()`, not a literal. The cap constant is feature-local, because `UiConstants` is for values several features share (R7). | ✅ |
| **V. Local verification** | `:feature:search:testAndroidHostTest`, `:core:domain:testAndroidHostTest`, `./gradlew test` and `:androidApp:assembleDebug`. No CI or lint gate is assumed. iOS is checked only on a Mac, and quickstart §4 records it as not run until then. | ✅ |
| **Persistence** | Nothing is persisted. The DB version is not touched. | ✅ |
| **Injection** | No constructor change and no new binding. `KoinGraphTest` is unaffected. No `SavedStateHandle`. | ✅ |
| **KMP** | All changes are in `commonMain`. No platform contract is needed, so `contracts/` is not produced. | ✅ |
| **One type per file / naming** | `DiscoverShelfKey.kt`, `DiscoverShelfUiModel.kt` and `SearchBrowseUiModel.kt` each hold one type. The two sealed ones keep their cases in the same file, the allowed exception (precedent: `GameFilterUiModel`). The UI model carries `UiModel`. `DiscoverShelfKey` is an identifier, like `SearchSort`, not a model. The new event joins the sealed `SearchUiEvent` file. | ✅ |
| **Resources** | One new string, `discover_see_all_shelf` ("See all: %1$s"), through the module's own bare `Res`. No `:core:ui` string, so no `CoreUiRes`. Only `values/` ships, so there is nothing to translate (R8). | ✅ |
| **Display-only cap (FR-004)** | `GetDiscoverFeedUseCase`, the lane cache, the shelf ranking and the de-duplication across shelves are not opened. The cap is `take(6)` in `toDiscoverContentState`. | ✅ |

**Changes to existing behaviour or code, called out** (none violates a principle, but each reaches beyond the
new path):
1. `toSearchResult` becomes public in `:core:domain`. This was the owner's decision on 2026-10-13, and spec
   FR-027 was amended. Behaviour does not change.
2. `DiscoverContentState.Content`'s shelf fields change type: `popular` and `upcoming` become
   `DiscoverShelfUiModel?`, and `recommended` becomes `List<DiscoverShelfUiModel>`.
   `RecommendedShelfUiModel` is deleted. The lane titles move from `DiscoverFeed.kt` into the mapper as
   `UiText`.
3. `SearchUiState.activeGenre` is replaced by `activeBrowse`, and every reader changes (research R3, table).
   `keepServerOrder` is renamed `keepSourceOrder`, and `isGenreBrowse` is renamed `isBrowse`.
4. Existing tests that read `content.popular`, `content.upcoming`, `content.recommended` or `activeGenre`
   are updated to the new accessors (for example `content.popular?.games`, or
   `(activeBrowse as SearchBrowseUiModel.Genre).genre`). Each keeps its assertion. None is deleted or
   loosened.

## Project Structure

### Documentation (this feature)

```text
specs/015-discover-see-all-grids/
├── spec.md
├── plan.md               # this file
├── research.md           # R1–R11
├── data-model.md
├── quickstart.md
├── checklists/requirements.md
└── tasks.md              # /speckit-tasks (not created here)
```

No `contracts/`: no platform capability and no new repository or use-case contract. The only cross-module
change is a visibility change on an existing function.

### Source Code (repository root)

```text
core/domain/src/commonMain/kotlin/com/nikolasguillen/questlog/core/domain/usecase/search/
└── SearchResultFacets.kt                           EDIT  internal → public; KDoc names the shelf grid as a caller

feature/search/src/commonMain/
├── kotlin/com/nikolasguillen/questlog/feature/search/
│   ├── SearchViewModel.kt                          EDIT  openShelf(); activeBrowse in performSearch/browseGenre/
│   │                                                     clearSearch; retrySearch exhaustive when; keepSourceOrder;
│   │                                                     observeWishlistedGameIds patches the new shelf shape
│   ├── SearchScreen.kt                             EDIT  onOpenShelf lambda (grid scroll reset); BackHandler on
│   │                                                     activeBrowse; pass activeBrowse + onOpenShelf down
│   ├── model/
│   │   ├── DiscoverShelfKey.kt                     NEW   sealed: Lane(DiscoverLane), ByGenre(genreId), ByDeveloper(developerId)
│   │   ├── DiscoverShelfUiModel.kt                 NEW   key, title, games (≤ 6), hasMore
│   │   ├── RecommendedShelfUiModel.kt              DELETE  replaced by DiscoverShelfUiModel
│   │   ├── SearchBrowseUiModel.kt                  NEW   sealed: Genre(genre), Shelf(key, title)
│   │   ├── DiscoverContentState.kt                 EDIT  popular/upcoming: DiscoverShelfUiModel?; recommended: List<DiscoverShelfUiModel>
│   │   ├── SearchUiState.kt                        EDIT  activeGenre → activeBrowse; KDoc invariants
│   │   └── SearchUiEvent.kt                        EDIT  + OnShelfSeeAllTriggered(shelf)
│   ├── mapper/
│   │   ├── DiscoverMapper.kt                       EDIT  DISCOVER_SHELF_ROW_SIZE; DiscoverFeed.shelfGames(key);
│   │   │                                                 shelves built capped with hasMore and UiText titles
│   │   └── SearchUiMapper.kt                       EDIT  withRelevanceLabel(isBrowse); SearchBrowseUiModel.toPlaceholder()
│   └── components/
│       ├── DiscoverFeed.kt                         EDIT  takes DiscoverShelfUiModel; title row with arrow,
│       │                                                 whole-row target, one merged node; onSeeAllClick; previews
│       │                                                 (> 6, ≤ 6, long title)
│       ├── SearchMainContent.kt                    EDIT  activeBrowse; forwards onSeeAllClick to the feed
│       └── SearchBars.kt                           EDIT  browsePlaceholder: UiText? replaces activeGenreLabel; X visibility
└── composeResources/values/strings.xml             EDIT  + discover_see_all_shelf

feature/search/src/androidHostTest/kotlin/com/nikolasguillen/questlog/feature/search/
├── SearchViewModelTest.kt                          EDIT  + shelf cases (below); accessors updated (change 4 above)
└── mapper/DiscoverMapperTest.kt                    NEW   cap, hasMore, keys, titles, hero split, shelfGames
```

`:shared` needs no edit. `SearchViewModel`'s constructor does not change.

**Structure Decision**: The existing layout is used unchanged. State, mapping and rendering stay in
`:feature:search`, where the genre browse already lives. The facet rule stays in `:core:domain` and is only
made visible.

### `SearchViewModelTest` cases to add

Shelves are built from `DiscoverFeed` emissions of `getDiscoverFeedUseCase`, as the existing Discover tests
build them. The shelf to open is taken from `uiState.discover`, the way the screen gets it.

1. Opening "Popular this month" (eight games) → `activeBrowse` is `Shelf(Lane(POPULAR_THIS_MONTH), title)`.
   `contentState` is `Success` **on the very next state**, with no `Loading` emission in between (collect
   `uiState` into a list). `allGames` holds all eight ids in shelf order.
2. Opening "Most anticipated" → the grid holds `upcoming.drop(1)`: the hero is not in it.
3. No request and no history: after open → clear, `getDiscoverFeedUseCase` was invoked exactly once, and
   `searchGamesUseCase`, `browseGenreUseCase` and `addSearchToHistoryUseCase` were never called.
4. Snapshot: open a shelf, then emit a new feed with different games. `contentState` is unchanged, and
   `discover` shows the new feed.
5. Saved state follows the wishlist in the grid. Save and long-press resolve the game (through `findGame`).
6. Leaving: `OnClearSearch` → `activeBrowse == null`, `Idle`. `OnSearchTriggered("q")` → `activeBrowse ==
   null`, text results. `OnGenreBrowseTriggered(g)` → `activeBrowse is Genre`.
7. Label: "Top picks" while a shelf is open, "Relevance" after clear. `selected` and `descending` are
   unchanged throughout.
8. Order: "Top picks" descending is shelf order and ascending reverses it, still true after a filter is
   applied.
9. Filters: every genre the shelf's games carry is offered (none withheld). Selecting a platform and a genre
   narrows to games with both. A selection nothing matches gives `Success(games = [])`, not `Empty`.
10. Two personalised shelves whose titles resolve to the same text (`ByGenre(12)` and `ByGenre(31)` with the
    same name) each open their own games.
11. A shelf whose key is gone after a refresh: the tap leaves `Idle` and `activeBrowse == null`.
12. Double tap: two `OnShelfSeeAllTriggered` events for the same shelf, with a feed emission in between →
    the grid keeps the first snapshot.
13. Invariant: while a text search is still suspended (a `CompletableDeferred`), open a shelf, then complete
    the search. The shelf's `Success` is still on screen. This shows that `Error`/`Loading` cannot land under
    a shelf, which is why `retrySearch`'s `Shelf` branch is unreachable.

### `DiscoverMapperTest` cases (new)

- Popular with 0, 1, 6 and 7 games → `null`, 1 game with `hasMore = false`, 6 with `false`, and 6 with `true`
  (the first six, in order).
- Upcoming with the hero plus 6, and with the hero plus 7 → the hero is excluded from the row. `hasMore` is
  `false`, then `true`.
- Recommended → keys `ByGenre(genre.id)` and `ByDeveloper(developer.id)`. Titles are
  `UiText.StringResource(discover_because_you_like / discover_more_from, name)`. Lane titles are
  `discover_popular_this_month` and `discover_most_anticipated`.
- `shelfGames(key)` → the whole shelf, of which every row is a prefix. `null` for an unknown key.

## Follow-ups (not in this feature)

- "Because you like %1$s" still formats IGDB's raw genre name ("Role-playing (RPG)") rather than
  `Genre.toLabelUiText()`. This follow-up comes from 014's plan. It is now also the grid's placeholder.
  Switching is a one-line change in `DiscoverMapper.kt`, but shelf titles are out of scope here.
- `feature/lists/.../WishlistRow.kt` uses `Icons.Default.ChevronRight`, which does not mirror in RTL. It was
  noticed while choosing this feature's arrow and was not changed.

## Complexity Tracking

No constitution violations. The `:core:domain` visibility change uses an existing edge. The owner chose it on
2026-10-13 over copying the facet rule into the feature (research R5).
