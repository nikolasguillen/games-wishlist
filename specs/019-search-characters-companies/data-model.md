# Phase 1 Data Model: Search by Character and Developer

One type per file, as everywhere in the project. All new types are `commonMain`.

## Network (`:core:network`, `model/`)

| Type | Fields | Notes |
|---|---|---|
| `IgdbImage` (new) | `id: Int`, `url: String?` | A character's `mug_shot` or a company's `logo`. Same shape as `IgdbCover`, but a separate type, because `IgdbCover` is documented as a game's cover. |
| `IgdbCharacter` (new) | `id: Int`, `name: String`, `description: String?`, `@SerialName("mug_shot") mugShot: IgdbImage?`, `games: List<Int>?` | `games` is an id array. A search requests `name, mug_shot.url` only; a profile adds `description, games`. |
| `IgdbCompany` (extended) | existing `id`, `name`, plus nullable `description: String?`, `country: Int?`, `@SerialName("start_date") startDate: Long?`, `logo: IgdbImage?`, `developed: List<Int>?` | Still decodes `involved_companies.company` rows, which carry only `id` and `name`: every new field is nullable and `IgdbJson` reads absent as `null`. |

KDoc with `@property` tags on each, as `core/network/CLAUDE.md` requires.

## Domain models (`:core:model`)

| Type | Fields | Built by |
|---|---|---|
| `GameCharacter` | `id: Int`, `name: String`, `imageUrl: String?` | `IgdbCharacter.toGameCharacter()` |
| `CharacterProfile` | `id: Int`, `name: String`, `imageUrl: String?`, `description: String?`, `gameIds: List<Int>` | `IgdbCharacter.toCharacterProfile()` (`gameIds` empty when absent) |
| `Developer` | `id: Int`, `name: String`, `logoUrl: String?`, `developedGameCount: Int` | `IgdbCompany.toDeveloper()`. The count is `developed.size`, raw from IGDB, and is used only for ranking (research R2). |
| `DeveloperProfile` | `id: Int`, `name: String`, `logoUrl: String?`, `description: String?`, `foundedYear: Int?`, `countryCode: String?` | `IgdbCompany.toDeveloperProfile()`. `foundedYear` is the UTC year of `start_date`. `countryCode` is ISO alpha-2 and exists only if the country is kept (R13); otherwise the field is not added. |

A blank `description` from IGDB maps to `null`, so "no description" has one representation.

Image URLs go through the shared `toIgdbImageUrl(size)`: `t_cover_big` for portraits and `t_logo_med` for logos
(R3).

## Domain contract (`:core:domain`)

### `TranslationSubject` (new, `translation/`)

```text
sealed interface TranslationSubject
  data class Game(val id: Int)
  data class Character(val id: Int)
  data class Developer(val id: Int)
```

`GameDescriptionTranslator.translate(subject: TranslationSubject, description: String): String?` replaces
`translate(gameId, description)`. Everything else in the contract is unchanged.

### `GameRepository` additions

See [contracts/repository-contract.md](./contracts/repository-contract.md). There are six methods, all returning
`AppResult` because all touch the network.

### Use cases

| Use case | Package | Does |
|---|---|---|
| `SearchCharactersUseCase(query)` | `usecase/search/` | Passes the repository's relevance order through. |
| `SearchDevelopersUseCase(query)` | `usecase/search/` | Ranks exact, then prefix, then contains, then `developedGameCount` desc, then name, and caps at 100 (R2). |
| `GetCharacterProfileUseCase(id)` | `usecase/profile/` | Pass-through. |
| `GetDeveloperProfileUseCase(id)` | `usecase/profile/` | Pass-through. |
| `GetCharacterGamesUseCase(gameIds)` | `usecase/profile/` | Fetches, then orders newest first with undated games last. An empty `gameIds` is an empty success with no request. |
| `GetDeveloperGamesUseCase(developerId)` | `usecase/profile/` | The same ordering. |
| `TranslateDescriptionUseCase(subject, description)` | `usecase/translation/` | Renamed from `TranslateGameDescriptionUseCase`, with the key widened. |

All are plain classes with `operator fun invoke`, registered with `factoryOf` in `domainModule`.

## Persistence (`:core:database`)

`TranslatedDescriptionEntity`, table `translated_descriptions`:

| Column | Before | After |
|---|---|---|
| `gameId: Int` | PK part | renamed `subjectId: Int`, PK part |
| `subjectType: String` | — | new, PK part: `"game"`, `"character"` or `"developer"` |
| `languageTag: String` | PK part | unchanged |
| `sourceHash: Int`, `translatedText: String` | | unchanged |

Primary key `(subjectType, subjectId, languageTag)`. `TranslationDao.getTranslation(subjectType, subjectId,
languageTag)`. The mapping from `TranslationSubject` to `(type, id)` lives in `:core:data`
(`translation/TranslationSubjectKey.kt`), so `:core:database` stays free of the domain type. The database stays at
`version = 1`. The schema JSON under `core/database/schemas/` is regenerated and committed in the same commit, and dev
devices are wiped (research R7). The backup is unaffected: `UserDataDao` never reads translations.

## Feature UI models (`:feature:search`, `model/`)

### Search

| Type | Shape | Notes |
|---|---|---|
| `SearchType` | `enum { GAMES, CHARACTERS, DEVELOPERS }` | Labels and hint texts come from `mapper/SearchTypeMapper.kt` (`toLabelUiText()`, `toPlaceholderUiText()`); icons are picked in the chip composable. |
| `EntityResultUiModel` | `@Immutable data class(id: Int, name: String, imageUrl: String?)` | One result card. Whether it is a portrait or a logo comes from `SearchUiState.resultsType`. |
| `SearchContentState` | + `data class EntityResults(val items: List<EntityResultUiModel>)` | The existing cases are unchanged. `EntityResults` is never empty: an empty lookup is `Empty`. |
| `SearchUiState` | + `selectedSearchType: SearchType = GAMES`, + `resultsType: SearchType = GAMES` | research R10 |
| `SearchUiEvent` | + `data class OnSearchTypeSelected(val type: SearchType)` | |

State rules (all in `SearchViewModel`, all covered by tests):

| Trigger | `selectedSearchType` | `resultsType` | Content |
|---|---|---|---|
| `OnSearchTypeSelected(t)` | `t` | unchanged | unchanged, and no request |
| Submit (`OnSearchTriggered`, a recent search, "See all results") | unchanged | `= selectedSearchType` | `Loading`, then `Success` (games), `EntityResults`, `Empty` or `Error` |
| `OnGenreBrowseTriggered`, `OnShelfSeeAllTriggered` | `GAMES` | `GAMES` | as today |
| `OnRetrySearch` | unchanged | unchanged | re-runs the query for `resultsType` |
| `OnClearSearch` | unchanged | unchanged | `Idle` |
| Typing with `selectedSearchType != GAMES` | — | — | history suggestions only; `gameSuggestions` empty and `isLoadingRemote` false |

The filters sheet, the sort sheet, `OnToggleSave` and `OnOpenListSelector` already act only on
`SearchContentState.Success` or the Discover feed, so `EntityResults` needs no change to them.

### Entity detail page

| Type | Shape |
|---|---|
| `EntityDetailSubject` (public) | `sealed interface { data class Character(val id: Int); data class Developer(val id: Int) }` |
| `EntityDetailUiState` | `@Immutable data class(subject: EntityDetailSubject, profile: EntityProfileContentState = Loading, games: EntityGamesContentState = Loading, gameCountText: UiText? = null, descriptionTranslation: DescriptionTranslationState = Unavailable, listSelectorState: ListSelectorState? = null)` |
| `EntityProfileContentState` | `Loading` · `Error(message: UiText)` · `Success(header: EntityHeaderUiModel, description: String?)` |
| `EntityHeaderUiModel` | `@Immutable data class(name: String, imageUrl: String?, foundedYearText: UiText?, countryCode: String?)`. The last two are always `null` for a character, and `countryCode` exists only if kept (R13). |
| `EntityGamesContentState` | `Loading` · `Empty` · `Error(message: UiText)` · `Success(games: List<GameItemUiModel>)` |
| `EntityDetailUiEvent` | `OnRetry`, `TranslateDescription`, `ShowOriginalDescription`, `OnToggleSave(gameId)`, `OnOpenListSelector(gameId)`, `OnToggleListSelection(listId)`, `OnConfirmListSelection`, `OnDismissListSelector` |
| `EntityDetailUiEffect` | `ShowSnackbar(message: UiText)` |

`DescriptionTranslationState` and `ListSelectorState` are reused, from `:core:ui` (moved, R8) and `:feature:search`
(as is). `OnRetry` reloads whichever of the two areas failed.

Lifecycle (`EntityDetailViewModel.init`):

```text
Character: profile ─ success ─▶ games(profile.gameIds)
                    └ failure ─▶ profile = Error           (games stays Loading, not shown)
Developer: profile ∥ games(developerId)                     (parallel)

games success, non-empty ─▶ Success(games), gameCountText = plural entity_game_count(n)
games success, empty     ─▶ Empty,          gameCountText = plural entity_game_count(0)
games failure            ─▶ Error(message)  (header and description stay)
profile success          ─▶ descriptionTranslation = Available if a description exists and the
                            model status is READY, else Unavailable (same rule as game detail)
```

Membership: `GetWishlistedGameIdsUseCase` is collected for the view model's lifetime and patches `isSaved` on the
`Success` games in place, the same pattern as `SearchViewModel.observeWishlistedGameIds`.

## Shared UI (`:core:ui`)

| Item | Change |
|---|---|
| `DescriptionTranslationState`, `DescriptionCard`, `TranslationBadge`, `TranslationEngineBadge` (`expect` with 2 `actual`s), `GeminiNanoBadge`, `AppleIntelligenceBadge` | moved from `:feature:game-detail` (R8) |
| `DetailHeader` + `DetailHeaderDefaults` | new, extracted from `WishlistDetailHeader` (R9) |
| `CustomFilterChip` | + `leadingIcon: (@Composable () -> Unit)? = null` (R11) |
| `localizedCountryName(isoAlpha2)` | `expect`, only if the country is kept (R13) |

## Design system (`:core:designsystem`)

`AppColors` gains `logoTileContainerColor`, light in both schemes (R3).
