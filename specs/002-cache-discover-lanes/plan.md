# Implementation Plan: Cache the Discover feed's generic lanes

**Branch**: `002-cache-discover-lanes` | **Date**: 2026-09-25 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/002-cache-discover-lanes/spec.md`

## Summary

Make `GameRepositoryImpl.getPopularGames` and `getUpcomingGames` cache-first, so reopening Discover
within a 6-hour window serves both generic lanes — and the hero that leads "Most anticipated" — from
Room instead of repeating a `/popularity_primitives` rank plus a `/games` hydrate per lane.

The cache is a **full catalogue snapshot held in its own tables**, never in `games`: a lane's ordered ids
live in `discover_lane_entries` with an explicit `position`, the payload in `cached_games`, and
platforms/genres/companies in cached-game cross-refs onto the *existing shared* lookup tables. Full
fidelity is required, not cosmetic: saving a game straight off a Discover card passes the feed's whole
`Game` into `toggleWishlist`, which persists it verbatim, so a thin cached `Game` would quietly starve
the taste profile that drives the recommended shelves (research.md D2, confirmed with the owner).

The platform selection is not part of any cache key — `setOwnedPlatforms` clears the cache before
applying a new selection, so a cached lane always belongs to the current filter by construction.

Nothing above `:core:data` changes: no use case, no UI, no navigation, no new module.

## Technical Context

**Language/Version**: Kotlin 2.4.10, Java 11, JVM toolchain 21

**Primary Dependencies**: Room 2.8.5 (KSP, legacy `SupportSQLiteOpenHelper` path), Hilt 2.60.1,
Retrofit 3 + Moshi, kotlinx-coroutines

**Storage**: Room — `QuestLogDatabase`, `version = 1`, `fallbackToDestructiveMigration(true)`,
`exportSchema = true`

**Testing**: JUnit4 + MockK + `kotlinx-coroutines-test` in `core/data/src/test` (JVM only; no
instrumentation, no Robolectric)

**Target Platform**: Android, minSdk 29 / compile-target SDK 37

**Project Type**: Modular Android app — 17 modules, `feature/*` + `core/*`

**Performance Goals**: zero network round-trips for the two generic lanes on a warm reopen within the
freshness window; cache read must stay well inside a frame budget on the Discover open path

**Constraints**: no list-shaped columns; database version must not be bumped; exported schema
regenerated in the same commit; catalogue results must not enter the `games` table (FR-006); no CI, no
lint gate — verification is local Gradle only

**Scale/Scope**: at most 2 lanes × ~40 (popular) / ~300-pool-filtered (anticipated) entries, so a few
hundred `cached_games` rows at the very most

## Constitution Check

*GATE: passed before Phase 0, re-checked after Phase 1 design. No violations.*

| Principle | Verdict | Evidence |
|---|---|---|
| I — Module boundaries | PASS | Change is confined to `:core:data` + `:core:database` + one enum in `:core:model`. No new dependency edge; `:core:data → :core:database` already exists. No feature module is touched, so no `feature/*` ever sees a persistence type. `:core:model` gains `DiscoverLane`, an enum with no Android or Compose dependency, so it stays KMP-ready. |
| II — Typed errors cross layers | PASS | The lane methods keep their `AppResult` return and their existing `try`/`catch` + `Throwable.toRepositoryError()` boundary; `CancellationException` still rethrows first. The new DAO is DB-only, so it returns bare values, per the rule that only network-touching methods return `AppResult`. One deliberate semantic, documented in research.md D6 and the contract: a network failure with a cached copy resolves to `Success(stale)` (FR-008). |
| III — UI renders, does not decide | PASS | No composable, UiState, mapper or ViewModel is edited. Ordering stays in the data layer and is restored by `position`, by id, never by name. |
| IV — Reuse the shared layer | PASS | Reuses the shared `platforms`/`genres`/`companies` lookup tables rather than duplicating reference data, reuses `GameMapper`'s existing lookup-row helpers, and adds no second repository. Mirrors `GameArtworkEntity`'s `position` precedent and `GameDao.saveGame`'s delete-before-insert discipline instead of inventing new ones. |
| V — Verification is local | PASS | The plan names real commands only (`:core:database:compileDebugKotlin`, `:core:data:testDebugUnitTest`, `./gradlew test`, `:app:assembleDebug`) and adds tests in the module that already has a `src/test` for this exact repository. No CI, lint gate or coverage campaign is assumed. |
| Persistence constraints | PASS | `version` stays `1`; `fallbackToDestructiveMigration(true)` untouched; schema JSON regenerated and committed with the entity change; no list-shaped columns — the ordered id list is rows with a `position`, and the platform selection is a cleared cache rather than an encoded set. |
| Injection | PASS | `DiscoverCacheDao` is provided from `DatabaseModule` exactly like the other DAOs and constructor-injected into `GameRepositoryImpl`. No `SavedStateHandle`, no new DI module. |
| KMP | PASS | No source-set restructuring, no library swap, no move to the driver-based Room API. The one KMP-relevant addition — `DiscoverLane` in `:core:model` — is a plain enum. `System.currentTimeMillis()` is used for stamps, matching `lastViewedAt`/`detailsFetchedAt`, so no `java.time` date math is introduced. |

`GameDao.saveGame` remains the only way a game enters the **`games`** table; `replaceLane` is the
equivalent single transactional entry point for the **cache** tables, so the discipline that rule
protects — cross-refs written in one transaction — is preserved rather than bypassed.

## Project Structure

### Documentation (this feature)

```text
specs/002-cache-discover-lanes/
├── plan.md              # This file
├── spec.md              # Feature specification (+ Session 2026-09-25 clarification)
├── research.md          # Phase 0 — D1..D8 decisions
├── data-model.md        # Phase 1 — entities, relations, mapping, lifecycle
├── quickstart.md        # Phase 1 — how to compile, test and verify by hand
├── contracts/
│   └── discover-cache.md   # Phase 1 — repository + DAO behavioural contracts
├── checklists/
│   └── requirements.md
└── tasks.md             # Phase 2 — created by /speckit-tasks, not by this command
```

### Source Code (repository root)

```text
core/model/src/main/java/com/nikolasguillen/questlog/core/model/
└── DiscoverLane.kt                                   # NEW — MOST_ANTICIPATED, POPULAR_THIS_MONTH

core/database/src/main/java/com/nikolasguillen/questlog/core/database/
├── QuestLogDatabase.kt                               # MODIFIED — register 6 entities + discoverCacheDao()
├── dao/DiscoverCacheDao.kt                           # NEW
├── di/DatabaseModule.kt                              # MODIFIED — @Provides DiscoverCacheDao
├── util/Converters.kt                                # MODIFIED — DiscoverLane converter pair
├── entity/
│   ├── DiscoverLaneCacheEntity.kt                    # NEW
│   ├── DiscoverLaneEntryEntity.kt                    # NEW
│   ├── CachedGameEntity.kt                           # NEW
│   ├── CachedGamePlatformCrossRef.kt                 # NEW
│   ├── CachedGameGenreCrossRef.kt                    # NEW
│   └── CachedGameCompanyCrossRef.kt                  # NEW
└── relation/
    ├── CachedGameWithDetails.kt                      # NEW
    └── CachedGameCompanyWithDetails.kt               # NEW

core/database/schemas/com.nikolasguillen.questlog.core.database.QuestLogDatabase/
└── 1.json                                            # REGENERATED — same commit, still version 1

core/data/src/main/java/com/nikolasguillen/questlog/core/data/
├── mapper/CachedGameMapper.kt                        # NEW — cache entity ↔ domain
└── repository/GameRepositoryImpl.kt                  # MODIFIED — cache-first lanes, TTL, invalidation

core/data/src/test/java/com/nikolasguillen/questlog/core/data/
├── mapper/CachedGameMapperTest.kt                    # NEW
└── repository/GameRepositoryImplDiscoverCacheTest.kt # NEW

core/domain/src/main/java/com/nikolasguillen/questlog/core/domain/repository/
└── GameRepository.kt                                 # MODIFIED — KDoc only, signatures unchanged

docs/roadmap.md                                       # MODIFIED — delete the shipped "Cache" open item
```

**Structure Decision**: No new module and no new layer. The feature lands in the two modules that
already own persistence and the error boundary, which is what keeps `feature/search`, `:core:domain` and
`:core:ui` out of the diff entirely. The `entity/` vs `relation/` split, the `<A><B>CrossRef` naming
without an `Entity` suffix, and one type per file all follow the existing `core/database` conventions.

Per the repository's own rule that the `CLAUDE.md` files are instructions rather than a changelog, the
`docs/roadmap.md` "Cache" bullet under "Open items in the shipped Discover feed" is deleted in the
implementing commit — the thing it described will exist.

## Complexity Tracking

> Filled because the entity count is high enough to deserve an explicit justification, even though no
> constitution principle is violated.

| Addition | Why needed | Simpler alternative rejected because |
|---|---|---|
| 6 new entities instead of reusing `games` + its cross-refs | FR-006 and `docs/roadmap.md:23-25` forbid catalogue rows in `games`; keeping the user-owned columns structurally absent from `cached_games` is what makes that guarantee hold | Reusing `games` with a discriminator column was ruled out by the feature description before planning; reusing the *existing* cross-ref tables for catalogue ids was rejected because cache eviction would then delete cross-refs belonging to a game the user had since saved |
| Cross-refs for platforms/genres/companies rather than scalars only | Saving from a Discover card persists the feed's whole `Game` (`GameRepositoryImpl.kt:443-445`), and `genres`/`developers` are the taste-profile signals behind the recommended shelves | The lean 3-entity variant was put to the owner with this trade-off spelled out and declined; it would have degraded the taste profile whenever a user saved from a cache-served lane without opening the detail screen |
| `CachedGameCompanyWithDetails` relation POJO | The developer/publisher split lives on the cross-ref, and a plain `Junction` drops cross-ref columns | Mirrors the existing `GameCompanyWithDetails`, which exists for exactly this reason |
