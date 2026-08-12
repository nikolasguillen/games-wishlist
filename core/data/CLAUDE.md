# CLAUDE.md — core:data

The single repository implementation plus all mappers. This is the **error boundary** of the app:
exceptions stop here and become typed results.

## Repository

`GameRepositoryImpl` implements `GameRepository` (the interface lives in `core/domain/repository/`) and is
bound with `@Binds @Singleton` in `di/DataModule.kt`. There is one repository for search, history, detail,
wishlist and lists — do not add a second one without discussing it.

## AppResult

`AppResult` and `RepositoryError` live in **`core:model`**, not here. Build them with the factory functions,
never the constructors:

```kotlin
override suspend fun searchGames(query: String): AppResult<List<Game>> = try {
    AppResult.success(response.map { it.toGame() })
} catch (e: Exception) {
    AppResult.failure(e.toRepositoryError())
}
```

Only methods that touch the network return `AppResult`. **DB-only methods return bare `Flow<T>` or `Unit`**
(`getWishlistedGames()`, `toggleWishlist()`, `deleteList()`). The exception is `createList`, which returns
`AppResult<Unit>` where a `Failure` means only the cover image failed to persist — that is documented in
the interface KDoc, keep it in sync.

Use `AppResult.map` to transform across layers rather than unwrapping and rewrapping.

## Error mapping

`repository/RepositoryErrorMapper.kt` holds `internal fun Throwable.toRepositoryError()`. Two details that
look like mistakes but are deliberate:

- It **rethrows `CancellationException`** before mapping anything, so coroutine cancellation is not
  swallowed into a `RepositoryError`.
- It detects Retrofit's `HttpException` **by class name via reflection**
  (`javaClass.name != "retrofit2.HttpException"`, then `getMethod("code")`) specifically so that
  `:core:data` does not depend on Retrofit. **Retrofit is a `testImplementation` on purpose** — do not
  promote it to `implementation` to "clean this up". `core/data/build.gradle.kts` carries a comment
  explaining why, and `RepositoryErrorMapperTest.kt` covers the behaviour.

Mapped cases: `UnknownHostException` / `ConnectException` / `SocketException` → `NoNetwork`,
`SocketTimeoutException` → `RequestTimeout`, Retrofit HTTP → `Http(code, message)`, everything else →
`Unknown(cause)`.

## Mappers

Top-level **extension functions**, not mapper classes or interfaces. Two files only:

- `mapper/GameMapper.kt` — all three directions: network→domain (`IgdbGame.toGame()`), entity→domain
  (`GameWithAllDetails.toGame()`), domain→entity (`Game.toEntity()`, `Game.toPlatformEntities()`,
  `Game.toGamePlatformCrossRefs()`).
- `mapper/ListMapper.kt` — `ListEntity.toWishlistList()`, `ListWithGameCount.toWishlistList()`.

Naming: `toX()` for a single object, `toXEntities()` / `toXCrossRefs()` for collections.

## Caching

Local-first, with no expiry policy. `refreshGameDetail(id)` checks Room first and **only hits the network
when the row is absent** — a cached game is never re-fetched. It then stamps `lastViewedAt` and writes
through `gameDao.saveGame(...)`.

Reads are reactive `Flow`s off Room. `isWishlisted` is **derived**, not stored: it comes from
`combine(..., gameDao.getGameIdsInList(DEFAULT_WISHLIST_ID))`.

`local/WishlistCoverImageStorage.kt` is the best in-repo example of this codebase's comment style — it
explains *why*, not *what*. Match it when writing non-obvious logic.
