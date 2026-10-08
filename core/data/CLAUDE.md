# CLAUDE.md — core:data

The single repository implementation plus all mappers, the translation engine and Radar's background
refresh. This is the **error boundary** of the app: exceptions stop here and become typed results.

## Common and Android

The repository, the common mappers, the three preference stores, `RepositoryErrorMapper.kt` and
`StaticReleaseRemindersAvailability` are in `commonMain`. Everything that touches an Android API is in
`androidMain`: `worker/`, `scheduler/`, `notification/ReleaseNotifierImpl`, `translation/` (with
`mapper/TranslationMapper.kt`, which names ML Kit's types), `local/WishlistCoverImageStorageImpl` and this
module's `strings.xml`. `:core:ai` is an `androidMain` dependency only. Each platform's bindings are in its
`dataPlatformModule` (`expect`, with the Android `actual` in `di/DataPlatformModule.android.kt` and the iOS one in
`iosMain`); the common `dataModule` binds only what needs no platform API. A new Android-only class goes in `androidMain` with its
binding in the `actual` module; shared code reaches it through an interface in `:core:domain`.

iOS has no background job runner, no reminders and no on-device translator, so its `dataPlatformModule` binds
`InProcessReleaseRefreshScheduler` (refresh at launch if the last successful one is 24h old, and when the saved
set changes; one run at a time; the timestamp is written only after a success), no-op reminder classes,
`StaticReleaseRemindersAvailability(false)` and `UnsupportedGameDescriptionTranslator`. All of these are in
`commonMain` and unit-tested on the JVM.

## Repository

`GameRepositoryImpl` implements `GameRepository` (the interface lives in `core/domain/repository/`) and is
registered as a `singleOf` bound to the interface in `di/DataKoin.kt` (`commonMain`). There is one repository for search, history, detail,
wishlist, lists, the Discover shelves, the platform catalogue and Radar's release dates — do not add a
second one without discussing it.

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
(`getWishlistedGames()`, `toggleWishlist()`, `deleteList()`). The exceptions are `createList` and
`updateList`, which return `AppResult<Unit>` where a `Failure` means only the cover image failed to persist
— that is documented in the interface KDoc, keep it in sync.

Use `AppResult.map` to transform across layers rather than unwrapping and rewrapping.

## Error mapping

`repository/RepositoryErrorMapper.kt` holds `internal fun Throwable.toRepositoryError()`. Two details that
look like mistakes but are deliberate:

- It **rethrows `CancellationException`** before mapping anything, so coroutine cancellation is not
  swallowed into a `RepositoryError`.
- It matches `IgdbHttpException`, `IgdbTimeoutException` and `IgdbConnectivityException` — **`:core:network`'s
  own types, never the HTTP client's** — so that `:core:data` does not depend on Ktor. Keep it that way: a
  transport failure worth telling apart is translated in `:core:network`. The platform's own connectivity
  and timeout exceptions are named in `isConnectivityFailure()` / `isTimeoutFailure()`, `expect` functions
  whose Android `actual`s (`RepositoryErrorMapper.android.kt`) are the only places here that name a platform
  type; the iOS `actual`s just test for `IgdbConnectivityException` and `IgdbTimeoutException`, because
  `:core:network` has already translated the Darwin engine's `NSError`s. `RepositoryErrorMapperTest.kt` covers the
  behaviour. The network module's exceptions extend
  `IOException`, so their branches have to stay **above** the catch-all.

Mapped cases on Android: `UnknownHostException` / `ConnectException` / `SocketException` → `NoNetwork`,
`SocketTimeoutException` → `RequestTimeout`, `IgdbHttpException` → `Http(code, message)`, everything else
→ `Unknown(cause)`.

## Mappers

Top-level **extension functions**, not mapper classes or interfaces. One file per source type family:

- `mapper/GameMapper.kt` — all three directions: network→domain (`IgdbGame.toGame()`), entity→domain
  (`GameWithAllDetails.toGame()`), domain→entity (`Game.toEntity()`, `Game.toPlatformEntities()`,
  `Game.toGamePlatformCrossRefs()`).
- `mapper/ListMapper.kt` — `ListEntity.toWishlistList()`, `ListWithGameCount.toWishlistList()`.
- `mapper/TranslationMapper.kt` — `GeminiNanoStatus.toTranslationModelStatus()`,
  `GeminiNanoDownload.toTranslationModelDownload()`.

Naming: `toX()` for a single object, `toXEntities()` / `toXCrossRefs()` for collections.

## Caching

Local-first, with no expiry policy. `refreshGameDetail(id)` checks Room first and only hits the network
when the row is absent **or is not yet a detail**: a game saved from search or Discover has a row (no
per-platform dates, no description, no related games) but `GameEntity.detailsFetchedAt` is `null`, so it
still triggers a fetch. A network fetch stamps `detailsFetchedAt`; a row that already carries one is never
re-fetched. Either way `lastViewedAt` is stamped and the result is written through `gameDao.saveGame(...)`.

Reads are reactive `Flow`s off Room. `isWishlisted` is **derived**, not stored: it comes from
`combine(..., gameDao.observeGameIdsInDefaultList())`, which follows the stored default wishlist.

`local/WishlistCoverImageStorage.kt` is the best in-repo example of this codebase's comment style — it
explains *why*, not *what*. Match it when writing non-obvious logic.

## Translation

`GameDescriptionTranslatorImpl` is Gemini Nano only — no second engine. ML Kit's classic Translation API
was weighed and dropped: it downloads a per-language-pair NMT model, the per-app download this feature
exists to avoid, and translates sentence by sentence with no notion of the domain. The seam for a fallback
engine later is this class: a second client in `:core:ai` plus a branch here. Do not build that
abstraction before a second engine actually exists.

Two files sit beside it, `internal` top-level functions as everywhere else in this module and both free of
the client: `TranslationPromptBuilder.kt` (`buildTranslationPromptPrefix()`, `buildTranslationPromptSuffix()`)
owns the prompt text, and `TranslationArtifactSanitizer.kt` (`String.stripTranslationArtifacts()`) strips
what a small on-device model leaves behind. Keep them out of the translator — they are the parts worth
unit-testing without a device.

`dataModule` also registers an application-scoped `CoroutineScope` for this feature only: the model
download has to outlive the `SettingsViewModel` that started it, or leaving the screen would cancel it.

## Background work

Radar's release dates refresh through WorkManager, wired here: `worker/ReleaseDatesRefreshWorker` is a
plain `CoroutineWorker`, registered with `workerOf` in the Android `dataPlatformModule`, that does nothing but call `RefreshReleaseDatesUseCase` and map a failure to `Result.retry()`,
and `scheduler/ReleaseRefreshSchedulerImpl` implements `core/domain/radar/ReleaseRefreshScheduler` over it.

Both enqueues are **unique with a `KEEP` policy**, for two different reasons, and both are load-bearing:
the periodic one is re-requested from `Application.onCreate()` on every launch and must not restart its 24h
window, and the immediate one collapses the burst of requests that several saves in a row produce. Read the
`// KEEP:` comments before changing either policy.

The worker-factory half lives in `:app`, which is the Android-only part of the entry point: `QuestLogApp` implements `Configuration.Provider` and hands WorkManager a
`KoinWorkerFactory`, and it is also where `schedulePeriodicRefresh()` is called from.
