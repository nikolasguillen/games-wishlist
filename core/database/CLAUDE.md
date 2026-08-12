# CLAUDE.md — core:database

Room persistence layer. Room 2.8.4 (the latest release — there is no Room 3) with KSP, on the **legacy
`SupportSQLiteOpenHelper` path**: `Room.databaseBuilder(...)` without `.setDriver(...)`.

The driver-based API (`BundledSQLiteDriver`, `androidx.sqlite`) has been available since Room 2.7.0 and is
deliberately unused. It mainly buys KMP support and a SQLite version pinned by the app rather than by the
device. **The project intends to migrate to KMP eventually** — when that happens, switching to the
driver-based API is part of that work, not a standalone refactor. Until then, do not introduce
`.setDriver(...)` piecemeal.

## No migration strategy — read this first

`GamesWishlistDatabase` is `version = 1` with `exportSchema = false`, and `DatabaseModule` calls
`.fallbackToDestructiveMigration(true)`. There is no `schemas/` directory and no `Migration` object
anywhere in the repo.

**Any change to an entity wipes all data on the device.** Always say so before making a schema change, and
let the user decide whether to accept the wipe or invest in a real migration path.

## Naming

- Entities: `<Name>Entity` with an explicit `@Entity(tableName = "snake_case")` —
  `GameEntity`("games"), `ListEntity`("wishlists"), `SearchHistoryEntity`("search_history"),
  `PlatformEntity`, `GenreEntity`, `CompanyEntity`.
- Junction tables: `<A><B>CrossRef` — **no `Entity` suffix** — with `primaryKeys = [...]`.
  `GameListCrossRef`, `GamePlatformCrossRef`, `GameGenreCrossRef`, `GameCompanyCrossRef`.
- Relation POJOs: `GameWithAllDetails`, `GamePlatformWithDetails`, `GameCompanyWithDetails`,
  `ListWithGameCount`.

Platforms, Genres and Companies are modelled as **many-to-many via CrossRef**. Do not add list-shaped
columns for new relations.

## DAO conventions

- `@Dao interface`; `Flow<T>` for observation, `suspend` for one-shot reads and writes.
- `@Insert(onConflict = OnConflictStrategy.REPLACE)`.
- `@Transaction` is mandatory on any `@Query` returning a relation POJO.
- Multi-table writes are **default `@Transaction suspend fun` bodies declared in the interface itself** —
  see `GameDao.saveGame(...)` and `ListDao.deleteListWithGameRefs(...)`. Follow that pattern instead of
  orchestrating multiple DAO calls from the repository.
- **When persisting a game, always go through `GameDao.saveGame`** so platforms, genres, companies and
  cross-refs are written in one transaction.
- Naming: `getX()` for one-shot, `observeX()` for the Flow variant when a suspend twin exists.

## Other module facts

- Type converters live in a single `util/Converters.kt` (`class Converters`, `@TypeConverter` pairs named
  `fromX`/`toX`), registered via `@TypeConverters(Converters::class)` on the database class.
- This module owns `res/values/strings.xml`: the default wishlist's name and description, inserted with
  `db.execSQL("INSERT INTO wishlists ...")` from `RoomDatabase.Callback.onCreate`. Imported with the alias
  `import com.example.gameswishlist.core.database.R as DatabaseR`.
- `GameDao` interpolates a Kotlin constant into SQL:
  `"... WHERE listId = ${WishlistConstants.DEFAULT_WISHLIST_ID}"`. That constant lives in `core/model`.
- `DATABASE_NAME` is a `const val` in the database class's companion object.

See `docs/tech-debt.md` for the known deviations in this module (comma-joined list columns, missing DAO
tests, no schema export).
