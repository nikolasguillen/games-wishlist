# Contract: Network, Repository and Translation

## `IgdbApiService` (`:core:network`)

Two new methods, one per new IGDB path. Both use the existing private `post(endpoint, query)`, so they decode a bare
list and throw this module's exceptions:

```kotlin
suspend fun getCharacters(query: String): List<IgdbCharacter>   // POST characters
suspend fun getCompanies(query: String): List<IgdbCompany>      // POST companies
```

Games for a character or developer reuse `searchGames(query)`, since the path is the same `/games`
(`core/network/CLAUDE.md`: "a new kind of query needs no new method"). That file's method list and its "Four IGDB
paths, five methods" sentence are updated in the same commit.

**Tests** (`IgdbHttpClientTest`, `IgdbDtoDecodingTest`): each new method posts to the right URL with the query as a
`text/plain` body and decodes the rows. An `IgdbCompany` with only `id` and `name` (an `involved_companies.company`
row) still decodes. A character without `mug_shot`, and a company without `logo`, decode those fields as `null`.

## `GameRepository` additions (`:core:domain`, implemented in `GameRepositoryImpl`)

```kotlin
/** Characters whose name matches [query], in IGDB's relevance order, at most 100. */
suspend fun searchCharacters(query: String): AppResult<List<GameCharacter>>

/**
 * Companies credited as the developer of at least one game whose name contains [query], unranked, at most
 * 500. Ranking is the caller's job (SearchDevelopersUseCase).
 */
suspend fun searchDevelopers(query: String): AppResult<List<Developer>>

/** One character with its description and its game ids. No row → RepositoryError.Unknown. */
suspend fun getCharacterProfile(characterId: Int): AppResult<CharacterProfile>

/** One developer with its description and founding year. No row → RepositoryError.Unknown. */
suspend fun getDeveloperProfile(developerId: Int): AppResult<DeveloperProfile>

/**
 * The games among [gameIds] that Search would also list (no noisy game type, no edition), every one of
 * them, in no particular order. Network only, not cached.
 */
suspend fun getGamesByIds(gameIds: List<Int>): AppResult<List<Game>>

/**
 * Every game [developerId] is credited as developer of, filtered like Search, in no particular order.
 * Network only, not cached. Unlike getGamesByDeveloper (the Discover shelf), it excludes games the
 * company only published and applies no platform filter.
 */
suspend fun getDevelopedGames(developerId: Int): AppResult<List<Game>>
```

### Queries

`GAME_FIELDS` below is the field list the existing game queries already send. A private constant holds it for the new
methods; the six existing methods keep their inline copies and are not touched.

```text
characters  search:   search "<q>"; fields name, mug_shot.url; limit 100;
companies   search:   fields name, logo.url, developed; where name ~ *"<q>"* & developed != null; limit 500;
characters  profile:  fields name, description, mug_shot.url, games; where id = <id>;
companies   profile:  fields name, description, logo.url, start_date; where id = <id>;
games       by ids:   fields <GAME_FIELDS>; where id = (<ids>) & game_type != (<noisy>) & version_parent = null;
                      sort id asc; limit 500; offset <n>;
games       developed: fields <GAME_FIELDS>; where involved_companies.company = <id> & involved_companies.developer = true
                      & game_type != (<noisy>) & version_parent = null; sort id asc; limit 500; offset <n>;
```

The two games queries go through one private pager. It requests offset 0, 500, 1000 and so on, and stops at a page
shorter than 500 or after `CREDITED_GAMES_MAX_PAGES = 10` pages. An id list longer than 500 is chunked before paging,
so no single `where id = (...)` exceeds the page size.

### Errors

Every method is `try { … AppResult.success(…) } catch (e: Exception) { AppResult.failure(e.toRepositoryError()) }`,
as the existing ones are. `CancellationException` is rethrown by `toRepositoryError()`. An empty profile response is
`AppResult.failure(RepositoryError.Unknown())` (research R14). A failure on any page fails the whole call: a partial
list would show a wrong game count.

### Tests (`:core:data`, new `GameRepositoryImplCharactersTest`, `GameRepositoryImplDevelopersTest`)

- Each method sends the query above to the right service method, verified by capturing the query string.
- **Mapping**: images are rewritten to the right size, a blank description becomes `null`, and `start_date` becomes
  its UTC year.
- **Paging**: 500 + 120 rows gives 620 games and two requests; exactly 500 gives a third, empty request; an id list of
  750 is split into chunks.
- **Failure**: a thrown `IgdbHttpException` on page two gives a `Failure` with the mapped `Http` error, and an empty
  profile gives a `Failure` with `Unknown`.

## Use cases (`:core:domain`)

| Use case | Contract | Tests (`androidHostTest`) |
|---|---|---|
| `SearchCharactersUseCase` | `invoke(query): AppResult<List<GameCharacter>>`, pass-through | — (pass-through, like `SearchGamesUseCase`) |
| `SearchDevelopersUseCase` | `invoke(query): AppResult<List<Developer>>`, ranked and capped (research R2) | exact before prefix before contains; ties by count, then name; cap of 100; ignores case |
| `GetCharacterProfileUseCase`, `GetDeveloperProfileUseCase` | pass-through | — |
| `GetCharacterGamesUseCase` | `invoke(gameIds): AppResult<List<Game>>`, newest first with undated last; empty ids returns empty with no repository call | order; undated last; empty ids |
| `GetDeveloperGamesUseCase` | `invoke(developerId): AppResult<List<Game>>`, same ordering | order |
| `TranslateDescriptionUseCase` | `invoke(subject, description): String?` (was `TranslateGameDescriptionUseCase(gameId, description)`) | — (pass-through) |

## Translation (`:core:domain` contract, `:core:data` implementations)

```kotlin
interface GameDescriptionTranslator {
    suspend fun modelStatus(): TranslationModelStatus                            // unchanged
    suspend fun translate(subject: TranslationSubject, description: String): String?   // was (gameId: Int, …)
    fun downloadModel(): Flow<TranslationModelDownload>                          // unchanged
}
```

- `GameDescriptionTranslatorImpl` (Android) and `AppleLanguageModelTranslator` (common, iOS) pass the subject to
  `translateWithCache(subject, languageTag, description, generate)`. That function maps it to `(subjectType,
  subjectId)` for `TranslationDao`. The Swift bridge is untouched: it never sees the key.
- `GameDetailViewModel` calls `TranslateDescriptionUseCase(TranslationSubject.Game(gameId), description)`.
- `specs/010-kmp-migration/contracts/platform-contracts.md`: the heading "`GameDescriptionTranslator` — `:core:domain`
  (unchanged signature)" now says the key is a `TranslationSubject`.
- **Tests**: `GameDescriptionTranslatorImplTest` and `AppleLanguageModelTranslatorTest` follow the new signature. A new
  case shows that a game and a character with the same id keep separate cache rows. `GameDetailViewModelTest`
  follows the use case rename.
