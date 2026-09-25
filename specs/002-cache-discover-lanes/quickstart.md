# Phase 1 Quickstart: validating the Discover lane cache

**Feature**: `002-cache-discover-lanes` | **Date**: 2026-09-25

How to prove the feature works. Entity shapes are in `data-model.md`, behaviour tables in
`contracts/discover-cache.md` — this file only says what to run and what to expect.

## Prerequisites

- `IGDB_CLIENT_ID` and `IGDB_CLIENT_SECRET` present in `local.properties` (never committed).
- macOS/Linux commands below use `./gradlew`; on Windows use `.\gradlew.bat`.
- The app is unpublished and the database is destructive-migration: the first launch after these
  entities land **wipes the device database**. That is expected and accepted.

## 1. Compile

```bash
./gradlew :core:model:compileDebugKotlin --console=plain -q      # DiscoverLane
./gradlew :core:database:compileDebugKotlin --console=plain -q   # entities, DAO, converters (KSP)
./gradlew :core:data:compileDebugKotlin --console=plain -q       # mapper + repository wiring
```

`:core:database` must be compiled before trusting anything else — Room's KSP processor is what
validates the `@Relation`/`Junction` wiring and the DAO queries.

## 2. Confirm the exported schema was regenerated

After the `:core:database` build, `core/database/schemas/…/1.json` must show the six new tables and
still read `"version": 1`:

```bash
git diff --stat core/database/schemas/
grep -o '"tableName": "[a-z_]*"' core/database/schemas/com.nikolasguillen.questlog.core.database.QuestLogDatabase/1.json
```

Expected new table names: `discover_lane_cache`, `discover_lane_entries`, `cached_games`,
`cached_game_platform_cross_ref`, `cached_game_genre_cross_ref`, `cached_game_company_cross_ref`.
There must still be exactly one directory under `schemas/`. Commit the regenerated file in the same
commit as the entity change.

## 3. Unit tests

```bash
./gradlew :core:data:testDebugUnitTest --console=plain -q
./gradlew test                                                   # full JVM suites must stay green
```

New tests belong in `core/data/src/test/.../repository/`, following
`GameRepositoryImplPopularGamesTest` — `mockk` for `IgdbApiService` and every DAO, `runTest`, no Robolectric.
The existing `GameRepositoryImplPopularGamesTest` and `GameRepositoryImplToggleWishlistTest` must keep
passing unchanged; if either needs editing, the repository's behaviour drifted beyond what this feature
authorizes.

Scenarios to cover, mapped to the spec:

| Scenario | Expectation | Covers |
|---|---|---|
| Fresh stamp + cached entries | no `getPopularityPrimitives` / `searchGames` call; games returned in stored `position` order | FR-003, US1 |
| No cached lane | both calls fire; `replaceLane` called once with the ranked order | FR-004 |
| Stamp older than TTL | both calls fire; cache replaced | FR-009, US2 |
| One lane fresh, the other stale | exactly one lane refetches in the same pass | FR-005 |
| Network throws, cached copy present | `AppResult.Success` holding the stale games; no write | FR-008 |
| Network throws, no cached copy | `AppResult.Failure`, as today | — |
| `setOwnedPlatforms` | `clearAll()` invoked, and invoked **before** `PlatformDao.setOwnedPlatforms` (`coVerifyOrder`) | FR-002, US3 |
| Successful empty fetch | stamp written, lane cached as empty, no refetch on the next call | edge case |
| `CachedGameWithDetails.toGame()` | platforms, genres, developers and publishers all populated; equal to the network-mapped `Game` for the same payload | D2 fidelity |

## 4. Full build (DI + KSP across modules)

```bash
./gradlew :app:assembleDebug
```

Required, not optional: the feature adds a DAO that `DatabaseModule` must provide and that
`GameRepositoryImpl`'s constructor now takes, so single-module compilation cannot catch a broken graph.

## 5. Manual verification on device

There is no CI and no instrumentation coverage for this path, so the acceptance scenarios are checked by
hand:

1. **Cold start** — open Discover, let both lanes and the hero load. Expect network traffic.
2. **Reopen (US1, SC-001)** — switch to another tab and back. Both lanes and the hero appear with no
   loading state and no new requests. Verify with Logcat/OkHttp logging that no `/popularity_primitives`
   or `/games` call fires.
3. **Hero from cache (clarification)** — the hero is present on the cached reopen and is the same game
   that led "Most anticipated" before, and the shelf below it still excludes it.
4. **Platform change (US3, SC-003)** — Settings → change the platform selection → back to Discover.
   Expect a fresh fetch and lanes that match the new filter.
5. **Offline (FR-008, SC-004)** — with a warm cache, enable airplane mode and reopen Discover. The lanes
   still render; no error state.
6. **Offline cold (unchanged)** — clear app data, go offline, open Discover. The feed errors exactly as
   it does today.
7. **Save from a cached lane (D2)** — save a game straight off a cache-served card, then open the
   wishlist. Rating, developer and platforms must look the same as when saving from a freshly fetched
   feed.

## 6. Tuning the window

`DISCOVER_LANE_CACHE_TTL` (6 hours) lives beside the other lane constants in `GameRepositoryImpl`. To
exercise staleness without waiting, override the stamp in a unit test rather than lowering the constant —
the tests inject `fetchedAt` directly.
