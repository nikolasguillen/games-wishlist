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
Do not add error *handling* here — no retries, no recovery, no result types.

The one thing this module does do is **translate**: `IgdbHttpErrorInterceptor` turns every non-2xx
response into an `IgdbHttpException`, which `:core:data` matches with a plain `is`. That is what keeps
Retrofit out of `:core:data`, and it is the seam that would survive a swap to another HTTP client. The
interceptor is registered **first** in `provideOkHttpClient` so `HttpLoggingInterceptor` still dumps the
failed response before the exception replaces it.

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
- Expiry comes from `expiresIn` minus a 60 s margin, read through **`ElapsedRealtimeSource`** — a
  `fun interface` bound in `NetworkModule` to `SystemClock.elapsedRealtime()`. Do not inline that call back
  into the manager: it is stubbed in JVM unit tests, and the seam is what `IgdbAuthManagerTest` drives to
  simulate expiry. Do not switch it to the wall clock or to `System.nanoTime()` either — the first can jump
  in both directions, the second stops counting while the device sleeps.
- `fetchToken()` swallows non-cancellation failures and returns `null` on purpose — the request then goes
  out unauthorised and the 401 becomes a typed error in `:core:data`. Turning it into a rethrow means
  moving the error boundary, which is a `:core:data` decision. `CancellationException` *is* rethrown.
- There is no recovery from a 401 on a token IGDB rejects early (revoked server-side): it stays cached
  until it expires or the process dies. An OkHttp `Authenticator` calling back into the manager is the fix
  if that ever shows up in practice.
- `NetworkModule`, `IgdbAuthManager` and `ElapsedRealtimeSource` are `internal`: nothing outside this
  module references them. Hilt is fine with an `internal` module — the Java it generates in `:app` does not
  see Kotlin visibility — but a `public` `@Provides` function cannot return an `internal` type, so the
  module has to stay `internal` for as long as those types are.
- The DI cycle (client needs auth, auth needs a client) is broken two ways in `di/NetworkModule.kt`:
  `dagger.Lazy<IgdbAuthManager>` in the interceptor, and a **second inline Retrofit instance** built inside
  `provideIgdbAuthService` so the auth call does not go through the auth interceptor. Keep both if you
  touch that module.

## Build config

Credentials come from `local.properties` via `buildConfigField` in this module's `build.gradle.kts`
(`IGDB_CLIENT_ID`, `IGDB_CLIENT_SECRET`). The module sets
`freeCompilerArgs = listOf("-Xannotation-default-target=param-property")` — note this differs from the flag
used by `core/ui`; do not unify them without checking both compile.
