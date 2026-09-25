# Phase 1 Data Model: Cache the Discover feed's generic lanes

**Feature**: `002-cache-discover-lanes` | **Date**: 2026-09-25

Six new entities in `:core:database`, one new enum in `:core:model`, one new relation POJO. The database
stays at `version = 1` with `fallbackToDestructiveMigration(true)`; the exported schema under
`core/database/schemas/com.nikolasguillen.questlog.core.database.QuestLogDatabase/1.json` is regenerated
and committed in the same commit (`core/database/CLAUDE.md:14-27`).

No foreign keys — this schema declares none anywhere, so every delete is written by hand (see
`research.md` D8).

## Enum — `:core:model`

### `DiscoverLane`

`core/model/src/main/java/.../core/model/DiscoverLane.kt`

| Constant | Lane |
|---|---|
| `MOST_ANTICIPATED` | unreleased games ranked by anticipation — "Most anticipated" |
| `POPULAR_THIS_MONTH` | already-released games popular now — "Popular this month" |

Persisted through a new `@TypeConverter` pair in `core/database/util/Converters.kt`
(`fromDiscoverLane`/`toDiscoverLane`), matching the `GameStatus` and `WishlistIcon` pairs already there.

## Entities — `:core:database` (`entity/`)

### 1. `DiscoverLaneCacheEntity` → `discover_lane_cache`

The per-lane freshness stamp. At most two rows ever.

| Column | Type | Notes |
|---|---|---|
| `lane` | `DiscoverLane` | `@PrimaryKey` |
| `fetchedAt` | `Long` | epoch millis, `System.currentTimeMillis()` |

**Rules**: a row exists only after a **successful** network fetch. A successful fetch that returned zero
games still writes the row — freshness is decided by the stamp, never by emptiness, so an empty lane is
not re-fetched on every open.

### 2. `DiscoverLaneEntryEntity` → `discover_lane_entries`

The ordered id list (FR-001). The hero is `position = 0` of `MOST_ANTICIPATED` — no hero-specific
storage exists, per the Session 2026-09-25 clarification.

| Column | Type | Notes |
|---|---|---|
| `lane` | `DiscoverLane` | `primaryKeys = ["lane", "gameId"]` |
| `gameId` | `Int` | references `cached_games.id`, no FK |
| `position` | `Int` | 0-based popularity rank; explicit because `@Relation` cannot sort |

### 3. `CachedGameEntity` → `cached_games`

The catalogue snapshot. Mirrors the catalogue-derived columns of `GameEntity` and **deliberately omits
every user-owned one** — `notes`, `priority`, `status`, `lastViewedAt`, `detailsFetchedAt`. That
omission is what makes FR-006 structural rather than conventional.

| Column | Type | Source field on `Game` |
|---|---|---|
| `id` | `Int` `@PrimaryKey` | `id` |
| `name` | `String` | `name` |
| `description` | `String` | `description` (IGDB `summary`) |
| `released` | `String?` | `releaseDate` |
| `backgroundImage` | `String?` | `backgroundImage` (cover url) |
| `rating` | `Double` | `rating` |
| `ratingCount` | `Int` | `ratingCount` |
| `hypes` | `Int` | `hypes` |
| `metacritic` | `Int?` | `metaCritic` |
| `gameTypeId` | `Int` | `gameType` |
| `url` | `String?` | `url` |

A row is shared by both lanes when a game somehow appears in both; the lanes are disjoint by
construction (unreleased vs released), so in practice each row belongs to one lane.

### 4. `CachedGamePlatformCrossRef` → `cached_game_platform_cross_ref`

| Column | Type | Notes |
|---|---|---|
| `gameId` | `Int` | `primaryKeys = ["gameId", "platformId"]` |
| `platformId` | `Int` | points at the shared `platforms` table |

No `releaseDate`/`releaseDatePrecision` columns, unlike `GamePlatformCrossRef`: the lane hydrate query
does not request `release_dates`, so there is nothing to store.

### 5. `CachedGameGenreCrossRef` → `cached_game_genre_cross_ref`

| Column | Type | Notes |
|---|---|---|
| `gameId` | `Int` | `primaryKeys = ["gameId", "genreId"]` |
| `genreId` | `Int` | points at the shared `genres` table |

### 6. `CachedGameCompanyCrossRef` → `cached_game_company_cross_ref`

| Column | Type | Notes |
|---|---|---|
| `gameId` | `Int` | `primaryKeys = ["gameId", "companyId"]` |
| `companyId` | `Int` | points at the shared `companies` table |
| `isDeveloper` | `Boolean` | same role split as `GameCompanyCrossRef` |
| `isPublisher` | `Boolean` | |

## Shared lookup tables — reused, not duplicated

`platforms`, `genres` and `companies` are shared reference data that "are shared between games and are
never cleared" (`core/database/CLAUDE.md:63-65`). Writing the cache inserts any missing rows there
(insert-if-absent) and never prunes them. There is no second copy of reference data.

## Relation POJOs — `:core:database` (`relation/`)

### `CachedGameWithDetails`

Mirrors `GameWithAllDetails`, minus everything the lane hydrate does not fetch (engines, artworks,
related games, per-platform dates):

- `@Embedded val game: CachedGameEntity`
- `platforms: List<PlatformEntity>` via `Junction(CachedGamePlatformCrossRef::class)`
- `genres: List<GenreEntity>` via `Junction(CachedGameGenreCrossRef::class)`
- `companyRefs: List<CachedGameCompanyWithDetails>` via
  `@Relation(entity = CachedGameCompanyCrossRef::class, parentColumn = "id", entityColumn = "gameId")`

### `CachedGameCompanyWithDetails`

Mirrors `GameCompanyWithDetails` exactly — `@Embedded val crossRef: CachedGameCompanyCrossRef` plus
`@Relation(parentColumn = "companyId", entityColumn = "id") val company: CompanyEntity` — because the
developer/publisher split lives on the cross-ref and a plain `Junction` would drop it.

## Mapping

New mapper file `core/data/mapper/CachedGameMapper.kt`, top-level extension functions as everywhere else
in the module:

- `CachedGameWithDetails.toGame(): Game` — entity → domain. Fills `platforms`, `genres`, `developers`
  and `publishers` so the result is field-identical to the network-mapped `Game`; leaves the user-owned
  fields at their `Game` defaults, exactly as `IgdbGame.toGame()` does.
- `Game.toCachedGameEntity()`, `Game.toCachedGamePlatformCrossRefs()`,
  `Game.toCachedGameGenreCrossRefs()`, `Game.toCachedGameCompanyCrossRefs()` — domain → entity.

The existing `GameMapper.kt` is not touched. Its `toPlatformEntities`, `toGenreEntities` and
`toCompanyEntities` helpers already produce the shared-lookup rows and are reused as-is.

## Lifecycle

| Event | Effect |
|---|---|
| Successful lane fetch | `replaceLane(...)`: delete the lane's entries, insert entries + snapshot + cross-refs + missing lookups, write `fetchedAt` — one transaction |
| Lane replaced | `cached_games` rows no longer referenced by any `discover_lane_entries` row are pruned, together with their three cross-ref sets |
| Platform selection changed | `clearAll()` wipes all three cache tables, **before** the new selection is applied (research.md D5) |
| Cache hit within TTL | read-only; nothing is written |
| Refresh fails with a cached copy present | nothing is written; the stale copy is served (FR-008) |
