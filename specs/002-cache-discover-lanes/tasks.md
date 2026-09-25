---

description: "Task list for caching the Discover feed's generic lanes"
---

# Tasks: Cache the Discover feed's generic lanes

**Input**: Design documents from `/specs/002-cache-discover-lanes/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/discover-cache.md, quickstart.md

**Tests**: Included. The project constitution (Principle V) requires a test for new repository/mapper
logic in its own module's `src/test`, and `quickstart.md` already enumerates the exact scenarios to cover.

**Organization**: Tasks are grouped by user story (spec.md: US1 P1, US2 P2, US3 P2) so each can be
implemented and verified independently once Foundational is done.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependency on an incomplete task)
- **[Story]**: US1 / US2 / US3, per spec.md

## Path Conventions

Existing 17-module Android project (`:core:*`, `:feature:*`, `:app`). No new module is created — every
path below is inside `:core:model`, `:core:database`, `:core:data` or `:core:domain`, per plan.md's
Project Structure section.

---

## Phase 1: Setup

**Purpose**: The one type every later phase (converters, entities, mapper) needs to exist first.

- [X] T001 [P] Create `DiscoverLane` enum with constants `MOST_ANTICIPATED` and `POPULAR_THIS_MONTH` in
  `core/model/src/main/java/com/nikolasguillen/questlog/core/model/DiscoverLane.kt` (data-model.md
  "Enum — `:core:model`"). No Android or Compose dependency, matching every other type in this module.

**Checkpoint**: `DiscoverLane` compiles standalone (`./gradlew :core:model:compileDebugKotlin`).

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Schema, DAO, DI wiring and the entity↔domain mapper. No user story is testable until this
phase is done, because all three read/write the same cache tables through the same DAO.

**⚠️ CRITICAL**: Do not start Phase 3+ before this phase's checkpoint passes.

- [X] T002 [P] Add a `DiscoverLane` `@TypeConverter` pair (`fromDiscoverLane`/`toDiscoverLane`, storing
  the enum name as `String`) to `core/database/src/main/java/com/nikolasguillen/questlog/core/database/util/Converters.kt`,
  matching the existing `GameStatus`/`WishlistIcon` pairs in that file. Depends on T001.
- [X] T003 [P] Create `DiscoverLaneCacheEntity` in
  `core/database/src/main/java/com/nikolasguillen/questlog/core/database/entity/DiscoverLaneCacheEntity.kt`:
  `@Entity(tableName = "discover_lane_cache")`, `@PrimaryKey val lane: DiscoverLane`, `val fetchedAt: Long`
  (data-model.md entity 1). A row exists only after a successful fetch; an empty-but-successful fetch
  still writes it. Depends on T001.
- [X] T004 [P] Create `DiscoverLaneEntryEntity` in
  `core/database/src/main/java/com/nikolasguillen/questlog/core/database/entity/DiscoverLaneEntryEntity.kt`:
  `@Entity(tableName = "discover_lane_entries", primaryKeys = ["lane", "gameId"])` with `val lane: DiscoverLane`,
  `val gameId: Int`, `val position: Int` (data-model.md entity 2). `position = 0` on `MOST_ANTICIPATED`
  is the hero, per the spec.md Session 2026-09-25 clarification — no separate hero storage. Depends on T001.
- [X] T005 [P] Create `CachedGameEntity` in
  `core/database/src/main/java/com/nikolasguillen/questlog/core/database/entity/CachedGameEntity.kt`:
  `@Entity(tableName = "cached_games")` with `@PrimaryKey val id: Int` plus `name: String`,
  `description: String`, `released: String?`, `backgroundImage: String?`, `rating: Double`,
  `ratingCount: Int`, `hypes: Int`, `metacritic: Int?`, `gameTypeId: Int`, `url: String?`
  (data-model.md entity 3). Deliberately has no `notes`, `priority`, `status`, `lastViewedAt` or
  `detailsFetchedAt` columns — that omission is what makes FR-006 structural.
- [X] T006 [P] Create `CachedGamePlatformCrossRef` in
  `core/database/src/main/java/com/nikolasguillen/questlog/core/database/entity/CachedGamePlatformCrossRef.kt`:
  `@Entity(tableName = "cached_game_platform_cross_ref", primaryKeys = ["gameId", "platformId"])` with
  `val gameId: Int`, `val platformId: Int` (data-model.md entity 4; no release-date columns — the lane
  hydrate query never requests `release_dates`).
- [X] T007 [P] Create `CachedGameGenreCrossRef` in
  `core/database/src/main/java/com/nikolasguillen/questlog/core/database/entity/CachedGameGenreCrossRef.kt`:
  `@Entity(tableName = "cached_game_genre_cross_ref", primaryKeys = ["gameId", "genreId"])` with
  `val gameId: Int`, `val genreId: Int` (data-model.md entity 5).
- [X] T008 [P] Create `CachedGameCompanyCrossRef` in
  `core/database/src/main/java/com/nikolasguillen/questlog/core/database/entity/CachedGameCompanyCrossRef.kt`:
  `@Entity(tableName = "cached_game_company_cross_ref", primaryKeys = ["gameId", "companyId"])` with
  `val gameId: Int`, `val companyId: Int`, `val isDeveloper: Boolean`, `val isPublisher: Boolean`
  (data-model.md entity 6 — full snapshot fidelity is required, not cosmetic: research.md D2).
- [X] T009 Create `CachedGameCompanyWithDetails` relation POJO in
  `core/database/src/main/java/com/nikolasguillen/questlog/core/database/relation/CachedGameCompanyWithDetails.kt`,
  mirroring `GameCompanyWithDetails` exactly: `@Embedded val crossRef: CachedGameCompanyCrossRef` plus
  `@Relation(parentColumn = "companyId", entityColumn = "id") val company: CompanyEntity` (data-model.md
  "Relation POJOs"). A plain `Junction` would drop the `isDeveloper`/`isPublisher` split, which is why
  this POJO exists. Depends on T008.
- [X] T010 Create `CachedGameWithDetails` relation POJO in
  `core/database/src/main/java/com/nikolasguillen/questlog/core/database/relation/CachedGameWithDetails.kt`:
  `@Embedded val game: CachedGameEntity`, `platforms: List<PlatformEntity>` via
  `Junction(CachedGamePlatformCrossRef::class)`, `genres: List<GenreEntity>` via
  `Junction(CachedGameGenreCrossRef::class)`, `companyRefs: List<CachedGameCompanyWithDetails>` via
  `@Relation(entity = CachedGameCompanyCrossRef::class, parentColumn = "id", entityColumn = "gameId")`
  (data-model.md "Relation POJOs"). Depends on T005, T006, T007, T009.
- [X] T011 Create `DiscoverCacheDao` in
  `core/database/src/main/java/com/nikolasguillen/questlog/core/database/dao/DiscoverCacheDao.kt` per
  contracts/discover-cache.md Contract 2: `suspend fun getLaneGames(lane: DiscoverLane): List<CachedGameWithDetails>`
  (the `@Transaction @Query` joining `cached_games`/`discover_lane_entries` ordered by `position`),
  `suspend fun getLaneFetchedAt(lane: DiscoverLane): Long?`, a `@Transaction suspend fun replaceLane(...)`
  default body following the exact 6-step order in the contract (delete the lane's entries; insert
  missing shared lookup rows; `REPLACE`-insert `cached_games` + cross-refs, deleting each game's existing
  cross-refs first; insert new entries; upsert the `discover_lane_cache` stamp; prune orphaned
  `cached_games` rows and their cross-refs), and a `@Transaction suspend fun clearAll()` that empties all
  five cache tables (`discover_lane_cache`, `discover_lane_entries`, `cached_games`, and the three
  cached-game cross-ref tables) without touching `games`, `game_list_cross_ref`, `owned_platforms` or the
  shared lookups. Follow the `GameDao.saveGame` delete-before-insert discipline
  (`core/database/CLAUDE.md`). Depends on T002, T003, T004, T010.
- [X] T012 Register the six new entities (`DiscoverLaneCacheEntity`, `DiscoverLaneEntryEntity`,
  `CachedGameEntity`, `CachedGamePlatformCrossRef`, `CachedGameGenreCrossRef`, `CachedGameCompanyCrossRef`)
  in `QuestLogDatabase.kt`'s `@Database(entities = [...])` list and add
  `abstract fun discoverCacheDao(): DiscoverCacheDao` in
  `core/database/src/main/java/com/nikolasguillen/questlog/core/database/QuestLogDatabase.kt`. Leave
  `version = 1` unchanged (constitution: Persistence). Depends on T011.
- [X] T013 [P] Add a `@Provides` method for `DiscoverCacheDao` off the existing `QuestLogDatabase`
  instance in `core/database/src/main/java/com/nikolasguillen/questlog/core/database/di/DatabaseModule.kt`,
  matching how the other DAOs are provided. Depends on T012.
- [X] T014 [P] Build `:core:database` to regenerate the exported schema at
  `core/database/schemas/com.nikolasguillen.questlog.core.database.QuestLogDatabase/1.json` (per
  `core/database/CLAUDE.md`: commit the regenerated file in the same commit as the entity change).
  Confirm it lists the six new table names and still reads `"version": 1`; confirm there is still exactly
  one directory under `schemas/`. Depends on T012.
- [X] T015 [P] Create `CachedGameMapper.kt` in
  `core/data/src/main/java/com/nikolasguillen/questlog/core/data/mapper/CachedGameMapper.kt` with
  `CachedGameWithDetails.toGame(): Game` (fills `platforms`, `genres`, `developers` and `publishers` so
  the result is field-identical to `IgdbGame.toGame()`'s output, leaving every user-owned `Game` field at
  its default) and `Game.toCachedGameEntity()`, `Game.toCachedGamePlatformCrossRefs()`,
  `Game.toCachedGameGenreCrossRefs()`, `Game.toCachedGameCompanyCrossRefs()`. Reuse
  `GameMapper.kt`'s existing `toPlatformEntities`/`toGenreEntities`/`toCompanyEntities` for the shared
  lookup rows rather than duplicating them (data-model.md "Mapping"). Depends on T010.
- [X] T016 [P] Add `CachedGameMapperTest` in
  `core/data/src/test/java/com/nikolasguillen/questlog/core/data/mapper/CachedGameMapperTest.kt`
  asserting a `Game` with non-empty `platforms`, `genres`, `developers` and `publishers` round-trips
  through `toCachedGameEntity()`/`toCached*CrossRefs()` → `CachedGameWithDetails.toGame()` with all four
  collections populated identically to the source — the fidelity requirement research.md D2 exists for.
  Depends on T015.

**Checkpoint**: `./gradlew :core:database:compileDebugKotlin :core:data:compileDebugKotlin --console=plain -q`
succeeds and `CachedGameMapperTest` passes. User story implementation can now begin.

---

## Phase 3: User Story 1 - Reopening Discover shows the same lanes instantly (Priority: P1) 🎯 MVP

**Goal**: `GameRepositoryImpl.getPopularGames`/`getUpcomingGames` read from the cache when a valid entry
exists, and persist the result after any successful network fetch — so a second Discover open in the
same freshness window serves both lanes, hero included, with zero network calls.

**Independent Test**: Open Discover, let both lanes load (first-ever open, cache empty, so this still
hits the network), navigate away, navigate back — the second open must render both lanes and the hero
identically with no loading state and no `/popularity_primitives` or `/games` call.

### Implementation for User Story 1

- [X] T017 [US1] Add a `DISCOVER_LANE_CACHE_TTL` constant (6 hours, expressed in the same millis units as
  `fetchedAt`) beside the existing `POPULARITY_TYPE_*`/`POPULARITY_POOL_LIMIT*` constants near the top of
  `core/data/src/main/java/com/nikolasguillen/questlog/core/data/repository/GameRepositoryImpl.kt`
  (research.md D6).
- [X] T018 [US1] Add a `discoverCacheDao: DiscoverCacheDao` constructor parameter to `GameRepositoryImpl`
  in the same file, and update all six existing direct-instantiation call sites in
  `core/data/src/test/java/com/nikolasguillen/questlog/core/data/repository/` —
  `GameRepositoryImplDeleteListTest.kt`, `GameRepositoryImplDeveloperGamesTest.kt`,
  `GameRepositoryImplPopularGamesTest.kt`, `GameRepositoryImplRefreshGameDetailTest.kt`,
  `GameRepositoryImplRefreshReleaseDatesTest.kt`, `GameRepositoryImplToggleWishlistTest.kt` — to pass
  `mockk<DiscoverCacheDao>(relaxed = true)`. No behavioral change to any of those tests.
- [X] T019 [US1] Add a private `GameRepositoryImpl` helper that persists a lane after a successful fetch:
  maps the ranked `List<Game>` to a `CachedGameEntity` + cross-ref lists via `CachedGameMapper` (T015),
  builds the matching `DiscoverLaneEntryEntity` list with each game's index as `position`, and calls
  `discoverCacheDao.replaceLane(...)` with `fetchedAt = System.currentTimeMillis()`. Reused by both lane
  methods in T020.
- [X] T020 [US1] Rewrite `GameRepositoryImpl.getPopularGames` and `getUpcomingGames` to be cache-first:
  call `discoverCacheDao.getLaneFetchedAt(lane)`; if non-null and
  `System.currentTimeMillis() - fetchedAt < DISCOVER_LANE_CACHE_TTL`, return
  `AppResult.success(discoverCacheDao.getLaneGames(lane).map { it.toGame() })` (order preserved by the
  `ORDER BY position` query, so `MOST_ANTICIPATED`'s `position = 0` is the hero); otherwise fall through
  to the existing `fetchPopularityRankedGames` call and, on success, persist via the T019 helper before
  returning. `POPULAR_THIS_MONTH` ↔ `getPopularGames`, `MOST_ANTICIPATED` ↔ `getUpcomingGames` (contracts
  Contract 1 table; FR-001, FR-003, FR-004).
- [X] T021 [P] [US1] Add `GameRepositoryImplDiscoverCacheTest` in
  `core/data/src/test/java/com/nikolasguillen/questlog/core/data/repository/GameRepositoryImplDiscoverCacheTest.kt`
  covering: a fresh cached lane (`getLaneFetchedAt` returns a recent stamp) results in zero calls to
  `apiService.getPopularityPrimitives`/`apiService.searchGames` and games returned in the stored
  `position` order; no cached lane (`getLaneFetchedAt` returns `null`) results in both calls firing and
  `discoverCacheDao.replaceLane` being called exactly once with the ranked order; a successful fetch that
  returns zero games still calls `replaceLane` with an empty entry list and a fresh stamp (so it is not
  re-fetched on the next call). Follow the mocking style of `GameRepositoryImplPopularGamesTest`.

**Checkpoint**: User Story 1 is independently functional and testable — `./gradlew :core:data:testDebugUnitTest --console=plain -q`.

---

## Phase 4: User Story 2 - Cached lanes refresh once they go stale (Priority: P2)

**Goal**: A cached lane older than `DISCOVER_LANE_CACHE_TTL` is refetched and replaced rather than served
indefinitely; a refresh that fails while a cached copy (fresh or stale) exists serves that stale copy
instead of failing the lane.

**Independent Test**: Seed a cached lane with a `fetchedAt` older than the TTL, open Discover, confirm a
fresh network fetch occurs and the lane updates; then repeat with the network mocked to throw and confirm
the previous cached copy is still shown rather than an error.

### Implementation for User Story 2

- [X] T022 [US2] In the cache-first branch added in T020, wrap the fallback network fetch so that if it
  throws (after `Throwable.toRepositoryError()` would normally produce a `Failure`) **and**
  `discoverCacheDao.getLaneFetchedAt(lane)` is non-null (a cached copy exists, stale or not), the method
  returns `AppResult.success(discoverCacheDao.getLaneGames(lane).map { it.toGame() })` instead of
  `AppResult.failure(...)`; `CancellationException` still rethrows before this check, unchanged
  (Constitution II; contracts Contract 1 row "Fetch fails, any cached copy present"; FR-008).
- [X] T023 [P] [US2] Extend `GameRepositoryImplDiscoverCacheTest` (T021) with: a cached lane whose
  `fetchedAt` is older than `DISCOVER_LANE_CACHE_TTL` causes both API calls to fire and `replaceLane` to
  run, replacing the stored order (FR-009); one lane fresh and the other stale in the same test instance
  results in exactly one lane's API calls firing (FR-005); a thrown network exception with a stale cached
  copy present resolves to `AppResult.Success` holding the stale games and **no** `replaceLane` call
  (FR-008); a thrown network exception with no cached copy at all still resolves to `AppResult.Failure`,
  unchanged from today.

**Checkpoint**: User Stories 1 AND 2 both work independently — `./gradlew :core:data:testDebugUnitTest --console=plain -q`.

---

## Phase 5: User Story 3 - Platform selection changes are respected even when cached (Priority: P2)

**Goal**: Changing the platform filter in Settings invalidates the entire Discover lane cache before the
new selection takes effect, so a cached lane can never be served for a platform selection it wasn't
fetched under.

**Independent Test**: Load Discover with one platform selection (populating the cache), change the
platform filter, reopen Discover, and confirm both lanes refetch rather than serving the old cache.

### Implementation for User Story 3

- [X] T024 [US3] In `GameRepositoryImpl.setOwnedPlatforms` (`core/data/src/main/java/.../repository/GameRepositoryImpl.kt`),
  call `discoverCacheDao.clearAll()` **before** delegating to `platformDao.setOwnedPlatforms(platformIds)`
  — order matters: a crash between the two calls must never leave a cache built for the previous
  selection reachable (research.md D5; contracts Contract 1 "Behavioural contract for
  `setOwnedPlatforms`"; FR-002).
- [X] T025 [P] [US3] Add `GameRepositoryImplSetOwnedPlatformsTest` in
  `core/data/src/test/java/com/nikolasguillen/questlog/core/data/repository/GameRepositoryImplSetOwnedPlatformsTest.kt`
  asserting `discoverCacheDao.clearAll()` and `platformDao.setOwnedPlatforms(...)` are both invoked, in
  that order, via MockK's `coVerifyOrder { ... }`.

**Checkpoint**: All three user stories are independently functional — `./gradlew :core:data:testDebugUnitTest --console=plain -q`.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Documentation and whole-project verification once all three stories are done.

- [ ] T026 [P] Delete the resolved "Cache" bullet (the one starting "do not dump discovered games into
  the `games` table unqualified") under "Open items in the shipped Discover feed" in `docs/roadmap.md`,
  per that file's own rule that a resolved entry is deleted, not annotated.
- [ ] T027 [P] Update the KDoc on `getPopularGames`, `getUpcomingGames` and `setOwnedPlatforms` in
  `core/domain/src/main/java/com/nikolasguillen/questlog/core/domain/repository/GameRepository.kt` to
  note the cache-first behavior and the cache-clearing side effect of a platform-selection change.
  Signatures are unchanged (contracts Contract 1).
- [ ] T028 Run the full verification sequence from quickstart.md §1, §3 and §4: `:core:model`,
  `:core:database` and `:core:data` `compileDebugKotlin`; `./gradlew :core:data:testDebugUnitTest`;
  `./gradlew test` (all existing JVM suites, including the six updated in T018, must stay green); and
  `./gradlew :app:assembleDebug` to catch any DI graph break `DatabaseModule`/`GameRepositoryImpl`'s new
  dependency could cause across module boundaries.
- [ ] T029 Manually walk quickstart.md §5 scenarios 1–7 on a device or emulator: cold start, cache-hit
  reopen (US1/SC-001), hero present on the cached reopen, platform-change refetch (US3/SC-003), offline
  reopen with a warm cache (US2/FR-008/SC-004), offline cold start (unchanged baseline), and saving a game
  straight off a cache-served card to confirm wishlist fidelity (research.md D2).

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies.
- **Foundational (Phase 2)**: Depends on T001. Blocks every user story — the DAO, entities and mapper are
  shared by all three.
- **User Story 1 (Phase 3)**: Depends on Foundational (Phase 2) checkpoint. No dependency on US2/US3.
- **User Story 2 (Phase 4)**: Depends on Foundational, and specifically on T020 (the cache-first branch it
  extends) from US1. Cannot be implemented before US1's T020 lands, though it remains independently
  *testable* once it does.
- **User Story 3 (Phase 5)**: Depends on Foundational only (T024 touches `setOwnedPlatforms`, a method
  neither US1 nor US2 touches). Can proceed in parallel with US2 once Foundational and T018 (the
  constructor change) are done.
- **Polish (Phase 6)**: Depends on all three user stories being complete.

### Within Each Phase

- Entities (T003–T008) before the relation POJOs that embed them (T009, T010).
- Relation POJOs before the DAO that returns them (T011).
- DAO before database registration (T012), which precedes DI wiring (T013) and schema regeneration (T014).
- Mapper (T015) before its test (T016), and before any repository code that calls it (T019).
- Within US1: constant (T017) and constructor change (T018) before the persistence helper (T019), which
  precedes the cache-first rewrite (T020), which precedes its test (T021).

### Parallel Opportunities

- T002–T008 (converters + 5 independent entity files) can run in parallel once T001 lands.
- T013 and T014 can run in parallel once T012 lands (DI wiring vs. schema regeneration touch different
  files/outputs).
- T015 and T016 are sequential (test needs the mapper), but T015 can run in parallel with T011–T014 once
  T010 exists, since the mapper file is independent of the DAO/DI/schema work.
- Once Foundational is done and T018 has landed, US2 (Phase 4) and US3 (Phase 5) touch disjoint code paths
  (`getPopularGames`/`getUpcomingGames` vs. `setOwnedPlatforms`) and their test files are new and separate
  — they can be implemented in parallel by different people, even though US2 is listed after US1 for
  priority reasons.
- T026 and T027 (docs) can run in parallel with each other and with T028's build commands.

---

## Parallel Example: Phase 2 (Foundational)

```bash
# After T001 (DiscoverLane) lands, launch these together — six independent files:
Task: "Add DiscoverLane TypeConverter pair in core/database/.../util/Converters.kt"
Task: "Create DiscoverLaneCacheEntity in core/database/.../entity/DiscoverLaneCacheEntity.kt"
Task: "Create DiscoverLaneEntryEntity in core/database/.../entity/DiscoverLaneEntryEntity.kt"
Task: "Create CachedGameEntity in core/database/.../entity/CachedGameEntity.kt"
Task: "Create CachedGamePlatformCrossRef in core/database/.../entity/CachedGamePlatformCrossRef.kt"
Task: "Create CachedGameGenreCrossRef in core/database/.../entity/CachedGameGenreCrossRef.kt"
Task: "Create CachedGameCompanyCrossRef in core/database/.../entity/CachedGameCompanyCrossRef.kt"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Phase 1: Setup (T001).
2. Phase 2: Foundational (T002–T016) — **critical path**, blocks everything else.
3. Phase 3: User Story 1 (T017–T021).
4. **STOP and VALIDATE**: run quickstart.md §5 scenarios 1–3 by hand. A warm reopen with zero network
   calls and the hero intact is the entire value proposition (SC-001, SC-002).

### Incremental Delivery

1. Setup + Foundational → schema and mapper exist, nothing observable changes yet.
2. + User Story 1 → reopening Discover is instant (MVP).
3. + User Story 2 → stale caches self-heal and a flaky network no longer blanks a warm lane.
4. + User Story 3 → the platform picker can no longer show a cache built for the wrong filter.
5. + Polish → docs cleaned up, full build/test/manual pass.

### Suggested Team Split

With more than one person: one takes Foundational solo (it is the blocking path), then US1 and US3 can
run in parallel once it lands (disjoint methods), with US2 following US1 by one task (T020) rather than
starting from zero.
