# CLAUDE.md — core:network

IGDB client. Ktor 3 + kotlinx.serialization, in `commonMain`; the HTTP engine is per platform (OkHttp on
Android, Darwin on iOS).

## IGDB speaks apicalypse, not REST

Every endpoint takes the apicalypse query as a plain `String`, posted as `text/plain` — the query is built in
the repository, not expressed through client annotations:

```kotlin
interface IgdbApiService {
    suspend fun searchGames(query: String): List<IgdbGame>
    suspend fun getGameDetail(query: String): List<IgdbGame>
    suspend fun getPopularityPrimitives(query: String): List<IgdbPopularityPrimitive>
    suspend fun getPlatforms(query: String): List<IgdbPlatform>
    suspend fun getReleaseDates(query: String): List<IgdbReleaseDateEntry>
}
```

Four IGDB paths, five methods: `searchGames` and `getGameDetail` are the same call under two names, kept
apart only so the repository reads clearly. What distinguishes one `/games` call from another is the query
string, so **a new kind of query needs no new method here** — Discover's shelves, the platform catalogue
sync and the Radar refresh all reuse these.

Return types are bare `List<IgdbGame>` — **no result wrapper**. Failures surface as thrown exceptions and are
converted into `AppResult`/`RepositoryError` in `:core:data`. Do not add error *handling* here — no retries,
no recovery, no result types.

The one thing this module does do is **translate**, so that no HTTP-client type ever leaves it:

- `IgdbHttpException` — a non-2xx response, thrown by the client's response validator
  (`createIgdbHttpClient` in `IgdbHttpClient.kt`). `:core:data` matches it with a plain `is`.
- `IgdbTimeoutException` — the client's `HttpRequestTimeoutException`, `ConnectTimeoutException` and
  `SocketTimeoutException`, caught in `IgdbApiServiceImpl`.
- `IgdbConnectivityException` — for an engine whose "device is offline" failures can only be told apart
  in here. The OkHttp engine reports them as `java.net` exceptions, which `:core:data` matches itself. The Darwin
  engine wraps an `NSError` in Ktor's `DarwinHttpRequestException`, so `toPlatformTransportFailure()` (`expect`, in
  `PlatformTransportFailure.kt`) turns the `NSURLErrorDomain` codes for "not connected", "cannot find or connect to
  host", "connection lost" and "DNS lookup failed" into this type, and "timed out" into `IgdbTimeoutException`.
  `IgdbApiServiceImpl` applies it to anything that is not already one of this module's exceptions.

A new transport failure that `:core:data` should tell apart gets a new exception **here**, not a dependency on
Ktor over there. `CancellationException` is never translated.

## DTOs

- Prefix `Igdb*`, **no `Dto` suffix**: `IgdbGame`, `IgdbCover`, `IgdbPlatform`, `IgdbInvolvedCompany`, …
- `@Serializable` plus `@SerialName("snake_case")` on renamed fields.
- IGDB leaves out any field it has no value for, so every optional field is nullable. `IgdbJson` reads an
  absent nullable field as `null` (`explicitNulls = false`) and ignores unknown keys; `IgdbDtoDecodingTest`
  pins both.
- Class-level KDoc with `@property` tags is mandatory for network models.

## Auth

- `IgdbAuthService` is a separate interface over a client of its own (`createAuthHttpClient`), posting to
  `https://id.twitch.tv/oauth2/token`. It has no auth step, so fetching a token never goes through the step
  that needs one.
- `IgdbAuthManager` is a Koin `single` caching the token in memory behind a `Mutex`, which also serves as
  the single-flight guard: parallel requests on a cold cache mint one token, not one each. The token is
  never persisted.
- The main client's `IgdbAuth` step adds `Client-ID` and the bearer token to every request. The token call is
  a suspend call inside the step, so nothing blocks a thread.
- Expiry comes from `expiresIn` minus a 60 s margin, read through **`ElapsedRealtimeSource`** — a
  `fun interface` bound by each platform's `networkPlatformModule` (`SystemClock.elapsedRealtime()` on Android). Do not inline that call
  back into the manager: it is stubbed in JVM unit tests, and the seam is what `IgdbAuthManagerTest` drives to
  simulate expiry. Do not switch it to the wall clock or to `System.nanoTime()` or `TimeSource.Monotonic`
  either — the first can jump in both directions, the others stop counting while the device sleeps.
- `fetchToken()` swallows non-cancellation failures and returns `null` on purpose — the request then goes
  out unauthorised and the 401 becomes a typed error in `:core:data`. Turning it into a rethrow means
  moving the error boundary, which is a `:core:data` decision. `CancellationException` *is* rethrown.
- There is no recovery from a 401 on a token IGDB rejects early (revoked server-side): it stays cached
  until it expires or the process dies. Refreshing and retrying on a 401 from the auth step is the fix if
  that ever shows up in practice.
- `IgdbAuthManager`, `IgdbAuthService` and `ElapsedRealtimeSource` are `internal`: nothing outside this
  module references them. `networkModule` (`di/NetworkKoin.kt`) and `networkPlatformModule` are public because
  the app root loads them; their definitions may still use the internal types. The engine is a Koin `factory`
  binding in `networkPlatformModule`, so each client owns its own, and adding a platform means adding that
  module's `actual` with its engine and its clock — nothing in `commonMain` changes.
- Logging is `LogLevel.BODY` only when `NetworkConfig.logBodies` is true — the app binds it from its own
  debug flag — with the `Authorization` and `Client-ID` headers masked; otherwise the plugin stays installed but
  silent.

## Tests

`IgdbHttpClientTest` drives the real client and service over Ktor's `MockEngine`: the request that goes out
(URL, method, body, credentials) and what comes back (decoded rows, or this module's exceptions). Use it as
the template for anything new here — there is no need for a network or a mock of the service.

## Build config

Credentials come from `local.properties` and reach the code as `internal object IgdbCredentials`
(`IGDB_CLIENT_ID`, `IGDB_CLIENT_SECRET`), which the `buildconfig` plugin generates into `commonMain` under
`build/` from this module's `build.gradle.kts`. The generated file is never committed; do not copy a value
into source.
