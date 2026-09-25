# Phase 1 Contracts: Cache the Discover feed's generic lanes

**Feature**: `002-cache-discover-lanes` | **Date**: 2026-09-25

This app exposes no network or public API surface. The contracts that matter here are the two internal
seams the feature touches: the new `DiscoverCacheDao` in `:core:database`, and the unchanged-signature
but changed-behaviour repository methods in `:core:data`.

## Contract 1 — `GameRepository` (`:core:domain`)

**Signatures do not change.** No feature module, use case or ViewModel is edited by this feature.

```kotlin
suspend fun getPopularGames(platformIds: Set<Int>): AppResult<List<Game>>
suspend fun getUpcomingGames(platformIds: Set<Int>): AppResult<List<Game>>
suspend fun setOwnedPlatforms(platformIds: Set<Int>)
```

### Behavioural contract for the two lane methods

| Precondition | Result | Network | Requirement |
|---|---|---|---|
| Cached lane present, `now - fetchedAt < TTL` | `Success(cached games, cached order)` | none | FR-003, FR-005 |
| No cached lane | fetch, persist, return | rank + hydrate | FR-004 |
| Cached lane present but older than TTL | fetch, replace, return fresh | rank + hydrate | FR-004, FR-009 |
| Fetch fails, **any** cached copy present | `Success(stale cached games)` | attempted | FR-008 |
| Fetch fails, no cached copy | `Failure(RepositoryError)` — unchanged from today | attempted | — |

Invariants:

- Each lane is evaluated independently; one may hit cache while the other refetches (FR-005).
- A cache hit preserves the stored `position` order exactly, so the hero (`position = 0` of
  `MOST_ANTICIPATED`) is restored with it.
- A returned `Game` is field-identical whether it came from cache or network (research.md D2).
- Exceptions never escape: the existing `try`/`catch` + `Throwable.toRepositoryError()` boundary still
  wraps the network path, and `CancellationException` is still rethrown first (Constitution II).
- A successful fetch returning zero games is cached as an empty lane with a fresh stamp, not as "no
  cache".

### Behavioural contract for `setOwnedPlatforms`

Clears the entire Discover lane cache **before** delegating to `PlatformDao.setOwnedPlatforms`. A crash
between the two steps may cost an unnecessary refetch; it can never leave a cache built for the previous
filter reachable (FR-002, research.md D5).

## Contract 2 — `DiscoverCacheDao` (`:core:database`, `dao/`)

`@Dao interface`, `suspend` for one-shot reads and writes, no `Flow` — the repository reads the cache
once per lane fetch. Multi-table writes are default `@Transaction` bodies declared in the interface
itself, the `GameDao.saveGame` pattern (`core/database/CLAUDE.md:58-60`).

```kotlin
@Transaction
@Query(
    """
    SELECT cached_games.* FROM cached_games
    INNER JOIN discover_lane_entries ON cached_games.id = discover_lane_entries.gameId
    WHERE discover_lane_entries.lane = :lane
    ORDER BY discover_lane_entries.position
    """
)
suspend fun getLaneGames(lane: DiscoverLane): List<CachedGameWithDetails>

@Query("SELECT fetchedAt FROM discover_lane_cache WHERE lane = :lane")
suspend fun getLaneFetchedAt(lane: DiscoverLane): Long?

@Transaction
suspend fun replaceLane(
    lane: DiscoverLane,
    fetchedAt: Long,
    games: List<CachedGameEntity>,
    entries: List<DiscoverLaneEntryEntity>,
    platforms: List<PlatformEntity>,
    genres: List<GenreEntity>,
    companies: List<CompanyEntity>,
    platformRefs: List<CachedGamePlatformCrossRef>,
    genreRefs: List<CachedGameGenreCrossRef>,
    companyRefs: List<CachedGameCompanyCrossRef>
)

@Transaction
suspend fun clearAll()
```

Contract of `replaceLane`, in order, one transaction:

1. delete the lane's `discover_lane_entries` rows
2. insert the shared lookup rows (`platforms`, `genres`, `companies`) — insert-if-absent, never pruned
3. `REPLACE`-insert the `cached_games` rows and their three cross-ref sets, deleting each game's
   existing cross-refs first so a shorter list leaves no leftovers (the `saveGame` discipline)
4. insert the new `discover_lane_entries` rows
5. upsert the `discover_lane_cache` row with `fetchedAt`
6. prune `cached_games` rows no `discover_lane_entries` row references any more, and their cross-refs

`clearAll` empties `discover_lane_cache`, `discover_lane_entries`, `cached_games` and the three
cached-game cross-ref tables. It never touches `games`, `game_list_cross_ref`, `owned_platforms`, or the
shared lookups.

## Contract 3 — what this feature must not touch

Enforced by review, and by the fact that the change never leaves `:core:data`/`:core:database`:

- `GetDiscoverFeedUseCase`, `DiscoverFeed.hasStaleRecommendations`, `GetTasteProfileUseCase`,
  `RecommendedShelf` and the `getGamesByGenre` / `getGamesByDeveloper` repository methods — the
  personalized shelves keep their own staleness signal (FR-007).
- `DiscoverMapper.toDiscoverContentState`, `DiscoverHero`, `DiscoverFeed` composable, `SearchViewModel`
  — the hero stays `upcoming.firstOrNull()`.
- `QuestLogDatabase.version`, which stays `1` (Constitution, Persistence).
