# Implementation Plan: Search by Character and Developer

**Branch**: `019-search-characters-companies` | **Date**: 2026-10-20 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/019-search-characters-companies/spec.md`

## Summary

A row of three chips under the expanded search bar (Games, Characters, Developers) picks what a submitted search looks
up. Every search is one IGDB request and every results grid holds one kind of card. Characters show as circular
portraits and developers as logo tiles. Tapping one opens a full-screen detail page shaped like the wishlist detail: a
header, the translatable description, then a grid of every game the character appears in or the developer made.

Technical approach:

- **Lookups.** Characters use `search` on `/characters`, which ranks by relevance. Developers use a name-contains
  match on `/companies`, restricted to companies with developed games and ranked on the client. `/companies` ignores
  `search`, and IGDB has no single query for all three types ([research R1–R2](./research.md)).
- **Shared code.** The detail page lives in `:feature:search`, which already owns the grid rules, the list selector and
  the save snackbar (R4). The description card and its translation badge move from `:feature:game-detail` to
  `:core:ui` (R8). The wishlist header's container becomes a shared `DetailHeader` (R9).
- **Translation cache.** The cache is keyed by game id, so its key widens to a `TranslationSubject`. That is a Room
  entity change (R7).
- **Detail-page games.** A page loads every credited game in pages of IGDB's 500-row cap, then orders them newest
  first (R5).

No new module, no new module edge and no new library.

## Technical Context

**Language/Version**: Kotlin 2.4.20, Kotlin Multiplatform (`commonMain`; `androidMain`/`iosMain` only for the moved
`TranslationEngineBadge` `actual`s, and for the country name if kept), Compose Multiplatform 1.12.1

**Primary Dependencies**: Ktor 3 + kotlinx.serialization (IGDB), Room 2.8.5 (translation cache), Koin 4.2.2, Navigation
3, Coil 3, kotlinx-datetime (founding year). All already in use.

**Storage**: Room: `translated_descriptions` is re-keyed to `(subjectType, subjectId, languageTag)`. The database stays
at `version = 1`, so the schema JSON is regenerated and dev devices are wiped (accepted policy, `docs/tech-debt.md`).
Search results and detail pages are network only and not cached, like the existing Search.

**Testing**: JUnit4 + MockK + `kotlinx-coroutines-test` on the JVM (`androidHostTest`) in `:core:network` (MockEngine),
`:core:data`, `:core:domain`, `:core:navigation`, `:feature:search`, `:feature:game-detail` and `:shared`
(`KoinGraphTest`). The UI is verified by hand ([quickstart.md](./quickstart.md) S1–S18).

**Target Platform**: Android (minSdk 29, targetSdk 37) and iOS 16+.

**Project Type**: Mobile app (multi-module KMP)

**Performance Goals**: one request per submitted search for every type (SC-003). A detail page shows its header and
description from one small request, and loads its games in one request (two for the largest studios: Konami has 871
games after filtering). No request while typing outside Games.

**Constraints**:

- Module boundaries are unchanged: features never reach `:core:data`, `:core:network` or `:core:database`.
- Text is UiText or a string resource, with English only (`values/`).
- No new `dp` literal: the wishlist header's existing ones move into `DetailHeaderDefaults`.
- No `android.*`/`java.*` in common code.
- Android builds and passes its tests after every step.

**Scale/Scope**:

- **Network and data**: 2 new service methods and 3 DTOs (2 new, 1 extended), 6 repository methods, 4 domain
  models, 7 use cases (6 new, 1 renamed).
- **Navigation**: 2 routes.
- **Feature**: 1 screen and view model plus about 8 components and models in `:feature:search`, and about 6 files
  moved from `:feature:game-detail` to `:core:ui`.
- **Shared UI and data**: 1 extracted shared header, 1 chip parameter, 1 color slot, 1 Room entity change.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Check | Result |
|---|---|---|
| **I. Module boundaries** | Detail page and selector in `:feature:search`, which depends only on its six allowed `:core:*` modules. Translation UI moves to `:core:ui`, already a dependency of both features. The repository, DTOs and Room change stay in `:core:data`/`:core:network`/`:core:database`. `:shared` gains two `entryProvider` branches and constructs the feature's public `EntityDetailSubject`. **No new module, no new edge.** | ✅ PASS |
| **II. Typed errors** | New service methods throw this module's exceptions through the existing `post`. Six repository methods return `AppResult` built with the factories. `toRepositoryError()` rethrows cancellation. An empty profile is `RepositoryError.Unknown` (R14). Errors render through `toUiText()`. | ✅ PASS |
| **III. UI renders, does not decide** | Developer ranking and game ordering live in use cases; the selected-vs-results type rules, suggestion gating and loading states in view models. All are covered by JVM tests. Composables pick a card or an icon by a state value and emit events. New state, event and effect types follow the house pattern. Flows are collected with `collectAsStateWithLifecycle()`. | ✅ PASS |
| **IV. Reuse the shared layer** | Reused: `VerticalGameCard`, `ListSelectorSheet`, `CustomChipRow`, `CustomFilterChip` (gains a leading icon), `CustomInfoChip`, `ErrorPage`, `EmptyPage`, `LoadingPage`, `ScrollToTopFab`, the search grid's column rules. The translation card and badges move to `:core:ui` instead of being copied, and the header container is extracted. One new color slot in `AppColors` instead of a literal. | ✅ PASS |
| **V. Local verification** | Real commands only (quickstart). New mapper, use case and view model logic gets JVM tests in its own module. `KoinGraphTest` stays green with one `extraTypes` entry. | ✅ PASS |
| **KMP constraints** | All shared code in `commonMain`. Moving the `TranslationEngineBadge` `expect`/`actual` updates its row in `platform-contracts.md`. The translator signature change updates that document's heading. A new seam (`localizedCountryName`) appears only if the owner keeps the country (R13), and goes into the same table. iOS verification is Mac-only and stated as such. | ✅ PASS |
| **Persistence** | One entity change. The version is not bumped. The schema JSON is regenerated and committed in the same commit. No list-shaped column. | ✅ PASS (wipe called out) |
| **Injection** | `EntityDetailViewModel` takes its route argument (`EntityDetailSubject`) first, through `parametersOf`, and is registered with `viewModelOf`. No `SavedStateHandle`. Use cases use `factoryOf`. | ✅ PASS |
| **Workflow** | One type per file, except the sealed state, event and effect files and `Routes.kt`. `internal` by default. The public API is the screen, the view model and `EntityDetailSubject`. English KDoc. `CLAUDE.md` files updated in the same commit as what they describe (`core/network`, `core/data`, `core/ui`). | ✅ PASS |

**Post-design re-check** (after Phase 1): unchanged. The contracts keep every feature-to-core edge as it was. The only
cross-module moves are into `:core:ui`. The one storage change is within policy. **Gate passes.**

## Project Structure

### Documentation (this feature)

```text
specs/019-search-characters-companies/
├── plan.md                       # This file
├── research.md                   # Phase 0: R1–R15, including the IGDB findings
├── data-model.md                 # Phase 1: DTOs, domain models, Room key, UI state and lifecycle
├── quickstart.md                 # Phase 1: commands and scenarios S1–S18
├── contracts/
│   ├── repository-contract.md    # service, repository, use cases, translator signature, queries
│   └── ui-contract.md            # routes, wiring, screens, cards, shared UI, strings
├── mockups/search-type-chips.png # the owner's mockup (colors outdated)
├── checklists/requirements.md
└── tasks.md                      # Phase 2 (/speckit-tasks)
```

### Source Code (repository root)

```text
core/network/src/commonMain/.../core/network/
├── IgdbApiService.kt, IgdbApiServiceImpl.kt        # + getCharacters, getCompanies
├── model/IgdbImage.kt, model/IgdbCharacter.kt      # new
└── model/IgdbCompany.kt                            # + description, country, startDate, logo, developed
core/network/CLAUDE.md                              # method list

core/model/src/commonMain/.../core/model/
└── GameCharacter.kt, CharacterProfile.kt, Developer.kt, DeveloperProfile.kt          # new

core/domain/src/commonMain/.../core/domain/
├── repository/GameRepository.kt                   # + 6 methods
├── translation/TranslationSubject.kt               # new
├── translation/GameDescriptionTranslator.kt        # translate(subject, …)
├── usecase/search/SearchCharactersUseCase.kt, SearchDevelopersUseCase.kt             # new
├── usecase/profile/Get{Character,Developer}{Profile,Games}UseCase.kt                 # new (4)
├── usecase/translation/TranslateDescriptionUseCase.kt   # renamed from TranslateGameDescriptionUseCase
└── di/DomainKoin.kt

core/database/src/commonMain/.../core/database/
├── entity/TranslatedDescriptionEntity.kt, dao/TranslationDao.kt                       # subject key
core/database/schemas/…/1.json                      # regenerated

core/data/src/commonMain/.../core/data/
├── repository/GameRepositoryImpl.kt                # + 6 methods, the credited-games pager
├── mapper/CharacterMapper.kt, mapper/DeveloperMapper.kt                               # new
├── mapper/GameMapper.kt                            # toIgdbImageUrl → internal
├── translation/TranslationCache.kt, translation/TranslationSubjectKey.kt (new), AppleLanguageModelTranslator.kt
└── (androidMain) translation/GameDescriptionTranslatorImpl.kt
core/data/CLAUDE.md                                 # mappers list, translation cache key

core/designsystem/.../theme/AppColors.kt (+ light/dark instances)                     # + logoTileContainerColor

core/ui/src/
├── commonMain/.../core/ui/component/DetailHeader.kt                                   # new (extracted)
├── commonMain/.../core/ui/component/DescriptionCard.kt                                # moved
├── commonMain/.../core/ui/component/translation/{TranslationBadge,TranslationEngineBadge,GeminiNanoBadge,AppleIntelligenceBadge}.kt
├── androidMain|iosMain/.../core/ui/component/translation/TranslationEngineBadge.{android,ios}.kt
├── commonMain/.../core/ui/model/DescriptionTranslationState.kt                        # moved
├── commonMain/.../core/ui/component/CustomFilterChip.kt                               # + leadingIcon
└── commonMain/composeResources/{values/strings.xml, drawable/ic_gemini.xml}
core/ui/CLAUDE.md                                   # inventory

core/navigation/src/
├── commonMain/.../Routes.kt, GameNavSavedStateConfiguration.kt                        # + 2 routes
└── androidHostTest/.../RoutesSerializationTest.kt

feature/game-detail/src/commonMain/.../gamedetail/  # imports the moved pieces; VM uses TranslateDescriptionUseCase
feature/wishlist/src/commonMain/.../wishlist/components/WishlistDetailHeader.kt        # wraps DetailHeader

feature/search/src/commonMain/.../feature/search/
├── SearchScreen.kt, SearchViewModel.kt             # types, results type, gating, new lambdas
├── EntityDetailScreen.kt, EntityDetailViewModel.kt # new
├── model/SearchType.kt, EntityResultUiModel.kt, EntityDetailSubject.kt,
│   EntityDetail{UiState,UiEvent,UiEffect}.kt, EntityProfileContentState.kt,
│   EntityGamesContentState.kt, EntityHeaderUiModel.kt                                 # new
├── model/SearchContentState.kt, SearchUiState.kt, SearchUiEvent.kt                    # extended
├── mapper/SearchTypeMapper.kt, mapper/EntityDetailMapper.kt                           # new
├── components/SearchTypeChipRow.kt, EntityResultGrid.kt, CharacterResultCard.kt,
│   DeveloperResultCard.kt, CharacterPortrait.kt, DeveloperLogo.kt, EntityDetailTopBar.kt   # new
├── components/SearchBars.kt, SearchMainContent.kt, SearchSubHeader.kt                 # extended
└── composeResources/values/strings.xml
feature/search/src/androidHostTest/.../ SearchViewModelTest.kt (extended), EntityDetailViewModelTest.kt (new)

shared/src/
├── commonMain/.../shared/QuestLogNavDisplay.kt, di/ViewModelModule.kt
└── androidHostTest/.../di/KoinGraphTest.kt         # + EntityDetailSubject::class

specs/010-kmp-migration/contracts/platform-contracts.md   # translator heading, badge module row
```

**Structure Decision**: The existing multi-module layout is unchanged. The detail page joins `:feature:search` as a
second screen, following `:feature:settings`' precedent. What two features now share moves into `:core:ui`.

## Implementation Order

Each step builds, passes `./gradlew test`, and is a point the work can pause on.

1. **Network.** Add `IgdbImage`, `IgdbCharacter`, the `IgdbCompany` fields, `getCharacters` and `getCompanies`, with
   MockEngine and decoding tests and `core/network/CLAUDE.md`. There is no caller yet.
2. **Domain and data, minus translation.** Add the four models, six repository methods with the credited-games pager,
   `CharacterMapper`/`DeveloperMapper`, the six new use cases with their Koin lines, and repository and use-case tests.
   This adds no UI.
3. **Translation key.** Add `TranslationSubject`, the entity and DAO key with the regenerated schema,
   `translateWithCache` and both translators, the `TranslateDescriptionUseCase` rename with its `GameDetailViewModel`
   call, and test updates. Update the `core/data/CLAUDE.md` translation paragraph and the platform-contracts heading.
   **This is the step that wipes dev devices.**
4. **Shared UI, no behavior change.**
   - Move the description card and badge family to `:core:ui`; game detail imports it.
   - Extract `DetailHeader`; the wishlist uses it.
   - Add `CustomFilterChip.leadingIcon` and `AppColors.logoTileContainerColor`.
   - Update the `core/ui/CLAUDE.md` inventory and the platform-contracts badge row.
   - Check game detail and the wishlist header by eye: they must look exactly as before.
5. **Detail page.** Add `EntityDetailSubject`, its state, events and effects, the view model with its tests, the
   screen, portrait, logo and top bar, and the strings. Add both routes with their saved-state lines and round trips,
   and the `:shared` branches, `viewModelOf` and `KoinGraphTest` `extraTypes`. The page is complete but nothing
   navigates to it yet.
6. **Selector and results.**
   - Add `SearchType`, the two state fields, `OnSearchTypeSelected`, the use-case branches, retry by `resultsType`,
     genre and shelf resetting to Games, and suggestion gating, with `SearchViewModelTest` cases for every row of the
     state table in [data-model.md](./data-model.md).
   - Add the chip row and the hint text.
   - Add `EntityResults`, its grid and cards, the sub-header without sort and filter buttons, and the per-type empty
     messages.
   - Add `SearchScreen`'s new lambdas, wired in `:shared`.
7. **Country chip** (only if the owner keeps it, R13): the ISO table with a sample test, the `localizedCountryName`
   `expect` with both `actual`s, the header chip, and the new row in the platform-contracts table.
8. **Verify.** Run `./gradlew test` and `:androidApp:assembleDebug`, quickstart S1–S18 on Android, then the iOS
   framework link and the scenarios on the simulator on a Mac.

## Spec Updates Made During Planning

Research changed details the spec had guessed. The spec was edited so `/speckit-analyze` finds the documents agreeing:

- **Loading** (FR-014, Story 4 scenario 8, edge case): "loads progressively" became "every game is shown, without
  delaying the header and description" (R5).
- **Order**: "catalogue's default order" became "newest first, undated last" (R5).
- **Developer definition** (assumption, key entity, edge case): a Developers result is a company credited as the
  developer of at least one game (R1).
- **Long description** (edge case): it is shown in full. Game descriptions have no expand or collapse, so there was
  nothing to match.

## Decisions for the Owner

1. **Country in the developer header** ([R13](./research.md#r13-founding-year-and-country)): it is the only header fact
   needing a 249-entry ISO table and new platform code. It is **recommended to drop it** and show the founding year
   and the game count. It is step 7, so cutting it touches nothing else. If dropped, FR-010 and the matching story
   line lose "the country".
2. **Where the detail page lives** ([R4](./research.md#r4-where-the-detail-page-lives)): `:feature:search` is
   recommended. A separate `:feature:entity-detail` module is cleaner if the page ever gets a second entry point, but
   needs a new module, a new `:shared` edge, and the grid rules, selector and strings moved to `:core:ui`. Moving it
   later is a contained refactor.
3. **Translation cache key** ([R7](./research.md#r7-translating-a-characters-or-developers-description)): the
   recommended re-key wipes local data on dev devices once, so **back up first**. The alternative is not caching
   character and developer translations (no schema change, a re-translation every visit).
4. **Logo tiles stay light in the dark theme** ([R3](./research.md#r3-images-on-the-new-cards-and-in-the-header)): IGDB
   logos are opaque JPEGs, mostly on white. A dark tile would frame each one in a white box. This is a visual call,
   reversible by changing one color slot.
5. **Expectation, not a decision** ([R6](./research.md#r6-character-data-in-igdb-is-sparse)): IGDB links characters to
   few games (Mario 2, Link 1), so character pages will often be short.

## Complexity Tracking

| Item | Why Needed | Simpler Alternative Rejected Because |
|---|---|---|
| Room entity change: `translated_descriptions` re-keyed by subject, which wipes dev devices once (within the `version = 1` policy) | The cache is keyed by game id, so a character or developer sharing a game's numeric id would evict that game's translation on every visit (R7) | Namespacing ids by sign is a hack and needs a third namespace. Skipping the cache re-translates on every visit (kept as the owner's fallback). |
| Signature change on a platform contract (`GameDescriptionTranslator.translate`), documented in `platform-contracts.md` | Same reason. Both implementations are Kotlin in `:core:data`, and the Swift bridge never sees the key | Overloading `translate` would keep two cache paths |
| `:core:ui` grows by the description card, the translation badges (one `expect`/`actual` moved, not added) and `DetailHeader` | Two features now show a translatable description and a wishlist-style header (Principle IV) | Copies in `:feature:search` would drift visually |
| `KoinGraphTest.extraTypes` gains `EntityDetailSubject::class` | One view model serves both page kinds through a typed route argument (R12) | Two view models with `Int` arguments would duplicate about 250 lines of translation, save, selector and loading logic |
