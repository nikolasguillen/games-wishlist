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
- `IgdbAuthManager` is a `@Singleton` caching the token in memory behind a `Mutex`. Nothing is persisted.
- The DI cycle (client needs auth, auth needs a client) is broken two ways in `di/NetworkModule.kt`:
  `dagger.Lazy<IgdbAuthManager>` in the interceptor, and a **second inline Retrofit instance** built inside
  `provideIgdbAuthService` so the auth call does not go through the auth interceptor. Keep both if you
  touch that module.
- The token is refreshed on two paths: proactively from `expiresIn` (minus a one-minute margin) in
  `getAccessToken()`, and reactively from an OkHttp `Authenticator` that catches a 401 and calls
  `refreshAccessToken(staleToken)`. The authenticator sets the header itself because the retried request
  does **not** re-run the application interceptors, and gives up when `priorResponse` is non-null so a
  permanently rejected token cannot loop.
- **`IgdbAuthManager` fails with `IOException` and never returns null.** Interceptors and authenticators
  may only throw `IOException`; any other exception escapes on the OkHttp dispatcher thread instead of
  reaching the caller, so `fetchToken()` wraps everything that is not already one.

## Build config

Credentials come from `local.properties` via `buildConfigField` in this module's `build.gradle.kts`
(`IGDB_CLIENT_ID`, `IGDB_CLIENT_SECRET`). The module sets
`freeCompilerArgs = listOf("-Xannotation-default-target=param-property")` — note this differs from the flag
used by `core/ui`; do not unify them without checking both compile.
