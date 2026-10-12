# Contract: Genre browse — repository and use cases

**Feature**: [../spec.md](../spec.md) | **Data model**: [../data-model.md](../data-model.md)

What `:feature:search` can rely on from `:core:domain`, and what `:core:data` has to provide behind it.

---

## `GameRepository.getPopularGamesByGenre`

```kotlin
/**
 * Released games carrying [genreId], most rated first, for Search's genre browse. Bounded to a recent
 * release window and a rating-count floor so obscure and decades-old entries do not surface. Network only and
 * not cached, like [searchGames]; unlike [getGamesByGenre] (the Discover shelf), it admits no unreleased game
 * and orders by how many people rated a game, not by its score.
 */
suspend fun getPopularGamesByGenre(genreId: Int): AppResult<List<Game>>
```

**Guarantees**

| # | Guarantee | Spec |
|---|-----------|------|
| G1 | Every returned game carries `genreId` among its genres. | FR-012 |
| G2 | Every returned game has a `first_release_date` at or before the time of the request. Unreleased and undated games are never returned. | FR-013 |
| G3 | Every returned game's `first_release_date` is later than *now − `GENRE_BROWSE_RELEASE_WINDOW_YEARS`*. | FR-014 |
| G4 | Every returned game has `total_rating_count >= GENRE_BROWSE_MIN_RATING_COUNT`. | FR-014 |
| G5 | The list is ordered by `total_rating_count`, highest first, exactly as IGDB returned it. | FR-015 |
| G6 | At most `GENRE_BROWSE_LIMIT` (500) games; the same cap as `searchGames`. | spec Assumptions |
| G7 | Noisy game types and `version_parent` variants are excluded; every game has a cover. | consistency with every other grid |
| G8 | A transport failure is an `AppResult.Failure` mapped by `toRepositoryError()`. `CancellationException` is rethrown, never returned as a failure. | FR-016, R5 |
| G9 | Nothing is written to Room, the search history included. | FR-024 |

**The query it sends** (`IgdbApiService.searchGames`, `/games`):

```text
fields name, url, game_type, summary, first_release_date, cover.url, total_rating, total_rating_count, aggregated_rating, hypes, platforms.name, platforms.abbreviation, platforms.generation, platforms.category, platforms.platform_family, genres.name, involved_companies.company.name, involved_companies.developer, involved_companies.publisher;
where genres = (<genreId>) & game_type != (<GameType.noisyTypes>) & version_parent = null & cover != null & total_rating_count >= <GENRE_BROWSE_MIN_RATING_COUNT> & first_release_date <= <nowSeconds> & first_release_date > <windowStartSeconds>;
sort total_rating_count desc;
limit <GENRE_BROWSE_LIMIT>;
```

No platform clause: a genre browse is a search, and search does not apply the user's owned platforms. The
platform facet in the filter sheet is how the user narrows by platform.

**Test** (`GameRepositoryImplGenreBrowseTest`, captured-body style as in `GameRepositoryImplPopularGamesTest`):
the body contains the genre clause, both date bounds, the rating floor, `sort total_rating_count desc` and
`limit 500`, and no `platforms =` clause. The rows come back in the order the service returned them. A
thrown `IgdbHttpException` comes back as `Failure(Http)`.

---

## `BrowseGenreUseCase`

```kotlin
class BrowseGenreUseCase(private val repository: GameRepository) {
    suspend operator fun invoke(genreId: Int): AppResult<SearchResult>
}
```

| # | Guarantee |
|---|-----------|
| B1 | `SearchResult.games` is exactly the repository's list, in the repository's order. |
| B2 | `SearchResult.platforms` / `genres` are the distinct facets of `games`, each ordered by how often it occurs, as in `SearchGamesUseCase`. |
| B3 | `SearchResult.genres` never contains `genreId`. |
| B4 | A repository `Failure` is passed through unchanged. |

---

## `GetBrowseGenresUseCase`

```kotlin
class GetBrowseGenresUseCase(private val repository: GameRepository) {
    operator fun invoke(): Flow<List<Genre>>
}
```

| # | Guarantee | Spec |
|---|-----------|------|
| C1 | Every emission has exactly 5 genres with 5 distinct ids. | FR-001, FR-009, FR-010 |
| C2 | No saved games → `[RPG 12, Shooter 5, Adventure 31, Strategy 15, Indie 32]`, in that order. | FR-008 |
| C3 | Taste genres come first: positive weight only, ordered by weight desc, then count desc, then id asc. | FR-006, spec Assumptions |
| C4 | Fewer than 5 taste genres → padded from the defaults in their fixed order, skipping ids already present. | FR-007, FR-009 |
| C5 | A taste genre that is also a default keeps its taste position. | FR-009 |
| C6 | Re-emits when the saved games change, and only when the resulting list changes. | User Story 2 scenario 5 |

**Tests** (`GetBrowseGenresUseCaseTest`, MockK on `GameRepository.getSavedGames()`): empty library; one
saved game with three genres; more than five positive genres; a dropped-only genre (excluded); a taste genre
equal to a default (no duplicate, taste position); a tie (deterministic order); a re-emission that does not
change the ranking (no new list).

---

## `SearchGamesUseCase` (unchanged behaviour)

Its facet derivation moves into `List<Game>.toSearchResult()`. Its output is unchanged for every input.
`SearchViewModelTest` covers it as it does today.
