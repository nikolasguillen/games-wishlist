# CLAUDE.md — core:database

Room persistence layer. Room 2.8.4 (the latest release — there is no Room 3) with KSP, on the **legacy
`SupportSQLiteOpenHelper` path**: `Room.databaseBuilder(...)` without `.setDriver(...)`.

The driver-based API (`BundledSQLiteDriver`, `androidx.sqlite`) has been available since Room 2.7.0 and is
deliberately unused. It mainly buys KMP support and a SQLite version pinned by the app rather than by the
device. **The project intends to migrate to KMP eventually** — when that happens, switching to the
driver-based API is part of that work, not a standalone refactor. Until then, do not introduce
`.setDriver(...)` piecemeal.

## Schema changes — read this first

**The app is not published. Until the owner says it is, the database stays at `version = 1`, there are no
`Migration` objects, and `DatabaseModule` keeps `.fallbackToDestructiveMigration(true)`.** An entity change
just recreates the database on the next launch, and losing the local data is accepted.

So when you change an entity:

- **Do not bump `version`.** Bumping it is what makes a migration mandatory, and writing migrations
  against a schema that is still moving is wasted work. `docs/tech-debt.md` tracks this as the thing to
  settle before the first release.
- **Do regenerate `schemas/1.json` and commit it in the same commit.** `exportSchema = true` and
  `room.schemaLocation` in this module's build file keep it up to date; it is the diff a reviewer reads to
  see that a column moved. Until release it is a moving snapshot, not a frozen version.

## Naming

- Entities: `<Name>Entity` with an explicit `@Entity(tableName = "snake_case")` —
  `GameEntity`("games"), `ListEntity`("wishlists"), `SearchHistoryEntity`("search_history"),
  `PlatformEntity`, `GenreEntity`, `CompanyEntity`, `EngineEntity`, `GameArtworkEntity`("game_artworks").
- Junction tables: `<A><B>CrossRef` — **no `Entity` suffix** — with `primaryKeys = [...]`.
  `GameListCrossRef`, `GamePlatformCrossRef`, `GameGenreCrossRef`, `GameCompanyCrossRef`,
  `GameEngineCrossRef`.
- Relation POJOs live in **`relation/`, not `entity/`** — `GameWithAllDetails`,
  `GamePlatformWithDetails`, `GameCompanyWithDetails`, `ListWithGameCount`. They are query results, not
  tables: what `entity/` contains is exactly what `schemas/1.json` lists.

Platforms, Genres, Companies and Engines are modelled as **many-to-many via CrossRef**. Artworks belong to
one game and are never shared, so they are a child table instead — with a `position` column, because
`@Relation` cannot sort and the gallery order is user-visible; it is restored in `GameMapper`.

**Do not add list-shaped columns.** There is no `List<String>` converter any more, and reintroducing one
is how the comma-joined `artworks`/`engines` columns happened in the first place.

## DAO conventions

- `@Dao interface`; `Flow<T>` for observation, `suspend` for one-shot reads and writes.
- `@Insert(onConflict = OnConflictStrategy.REPLACE)`.
- `@Transaction` is mandatory on any `@Query` returning a relation POJO.
- Multi-table writes are **default `@Transaction suspend fun` bodies declared in the interface itself** —
  see `GameDao.saveGame(...)` and `ListDao.deleteListWithGameRefs(...)`. Follow that pattern instead of
  orchestrating multiple DAO calls from the repository.
- Rows that mirror what the API returned for one game — the platform, genre, company and engine
  cross-refs, the artworks, the related games — are **deleted before being re-inserted** inside
  `saveGame`. A `REPLACE` insert alone would leave the leftovers of a shorter list behind. The lookup
  tables they point at (`platforms`, `genres`, `companies`, `engines`) are shared between games and are
  never cleared, and `GameListCrossRef` is the user's own data, so `saveGame` does not touch it either.
- **When persisting a game, always go through `GameDao.saveGame`** so platforms, genres, companies and
  cross-refs are written in one transaction.
- Naming: `getX()` for one-shot, `observeX()` for the Flow variant when a suspend twin exists.

## Other module facts

- Type converters live in a single `util/Converters.kt` (`class Converters`, `@TypeConverter` pairs named
  `fromX`/`toX`), registered via `@TypeConverters(Converters::class)` on the database class.
- This module owns `res/values/strings.xml`: the default wishlist's name and description, inserted with
  `db.execSQL("INSERT INTO wishlists ...")` from `RoomDatabase.Callback.onCreate`.
- `GameDao` interpolates a Kotlin constant into SQL:
  `"... WHERE listId = ${WishlistConstants.DEFAULT_WISHLIST_ID}"`. That constant lives in `core/model`.
- `DATABASE_NAME` is a `const val` in the database class's companion object.

See `docs/tech-debt.md` for the known deviations in this module (missing DAO tests).
