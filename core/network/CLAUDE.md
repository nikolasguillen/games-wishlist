# CLAUDE.md — core:network

IGDB client. Retrofit 3 + OkHttp 5 + Moshi (KSP codegen).

## IGDB speaks apicalypse, not REST

Endpoints are `@POST` with a raw `okhttp3.RequestBody` sent as `text/plain` — the apicalypse query is built
as a string in the repository, not expressed through Retrofit annotations:

```kotlin
interface IgdbApiService {
    @POST("games") suspend fun searchGames(@Body body: RequestBody): List<IgdbGame>
    @POST("games") suspend fun getGameDetail(@Body body: RequestBody): List<IgdbGame>
}
```

Return types are bare `List<IgdbGame>` — **no `Response<T>`, no `Call`, no network-level result wrapper**.
Failures surface as thrown exceptions and are converted into `AppResult`/`RepositoryError` in `:core:data`.
Do not add error handling here.

## DTOs

- Prefix `Igdb*`, **no `Dto` suffix**: `IgdbGame`, `IgdbCover`, `IgdbPlatform`, `IgdbInvolvedCompany`, …
- `@JsonClass(generateAdapter = true)` plus `@Json(name = "snake_case")` on renamed fields.
- Class-level KDoc with `@property` tags is mandatory for network models.

## Auth

- `IgdbAuthService` is a separate Retrofit interface with a hardcoded absolute URL
  (`@POST("https://id.twitch.tv/oauth2/token")`).
- `IgdbAuthManager` is a `@Singleton` caching the token in memory behind a `Mutex`, which also serves as
  the single-flight guard: parallel requests on a cold cache mint one token, not one each. The token is
  never persisted.
- Expiry comes from `expiresIn` minus a 60 s margin, measured against **`SystemClock.elapsedRealtime()`**.
  Do not switch that to the wall clock or to `System.nanoTime()`: the first can jump in either direction,
  the second stops counting while the device sleeps.
- `fetchToken()` swallows non-cancellation failures and returns `null` on purpose — the request then goes
  out unauthorised and the 401 becomes a typed error in `:core:data`. Turning it into a rethrow means
  moving the error boundary, which is a `:core:data` decision. `CancellationException` *is* rethrown.
- There is no recovery from a 401 on a token IGDB rejects early (revoked server-side): it stays cached
  until it expires or the process dies. An OkHttp `Authenticator` calling back into the manager is the fix
  if that ever shows up in practice.
- The DI cycle (client needs auth, auth needs a client) is broken two ways in `di/NetworkModule.kt`:
  `dagger.Lazy<IgdbAuthManager>` in the interceptor, and a **second inline Retrofit instance** built inside
  `provideIgdbAuthService` so the auth call does not go through the auth interceptor. Keep both if you
  touch that module.

## Build config

Credentials come from `local.properties` via `buildConfigField` in this module's `build.gradle.kts`
(`IGDB_CLIENT_ID`, `IGDB_CLIENT_SECRET`). The module sets
`freeCompilerArgs = listOf("-Xannotation-default-target=param-property")` — note this differs from the flag
used by `core/ui`; do not unify them without checking both compile.
