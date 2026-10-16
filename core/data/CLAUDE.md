# CLAUDE.md — core:data

The single repository implementation plus all mappers, the translation engine and Radar's background
refresh. This is the **error boundary** of the app: exceptions stop here and become typed results.

## Common and Android

The repository, the common mappers, the three preference stores and `RepositoryErrorMapper.kt` are in
`commonMain`, and so are the translation prompt builder and the
artifact sanitizer, which every translation engine shares. Everything that touches an Android API is in
`androidMain`: `worker/`, `scheduler/`, `notification/ReleaseNotifierImpl`, `translation/GameDescriptionTranslatorImpl`
(with `mapper/TranslationMapper.kt`, which names ML Kit's types), `local/WishlistCoverImageStorageImpl` and this
module's `strings.xml`. `:core:ai` is an `androidMain` dependency only. Each platform's bindings are in its
`dataPlatformModule` (`expect`, with the Android `actual` in `di/DataPlatformModule.android.kt` and the iOS one in
`iosMain`); the common `dataModule` binds only what needs no platform API. A new Android-only class goes in `androidMain` with its
binding in the `actual` module; shared code reaches it through an interface in `:core:domain`.

iOS has no WorkManager, so its `dataPlatformModule` binds `BackgroundTaskReleaseRefreshScheduler` with its
`ReleaseRefreshRunner`, `AppleLanguageModelTranslator`, and the release reminder pieces:
`LocalNotificationReleaseScheduler` (with its `ArmedReminderLedger`), `ReleaseReminderCoordinator` and
`UserNotificationCenter`, the one `iosMain` class, which adapts `UNUserNotificationCenter` to
`LocalNotificationCenter`. Everything else is in `commonMain` and unit-tested on the JVM. The translator needs an
`AppleLanguageModelBridge` and the scheduler an `AppRefreshTaskBridge`, which Swift supplies because
FoundationModels and BackgroundTasks cannot be imported from Kotlin/Native.

## Repository

`GameRepositoryImpl` implements `GameRepository` (the interface lives in `core/domain/repository/`) and is
registered as a `singleOf` bound to the interface in `di/DataKoin.kt` (`commonMain`). There is one repository for search, history, detail,
wishlist, lists, the Discover shelves, the platform catalogue and Radar's release dates — do not add a
second one for that without discussing it. `AccountRepositoryImpl` and `BackupRepositoryImpl` are the two
owner-approved additions: the cloud account and the cloud backup, over `:core:cloud`. They share no data with
`GameRepositoryImpl` and are bound the same way in `di/DataKoin.kt`.

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

The `Cloud*Exception` types from `:core:cloud` map to `NoNetwork`, `RequestTimeout`, `NotSignedIn`,
`SignInCancelled`, `AccountExistsWithOtherMethod` and `MethodLinkedToOtherAccount`, and `BackupUnreadableException`
(a stream that cannot be put back together) maps to `BackupUnreadable`. `CloudRecentLoginRequiredException` is
never mapped: `AccountRepositoryImpl.deleteAccount` answers it with one more sign-in and one retry, and a second
refusal becomes `NotSignedIn`. A new `Cloud*` branch goes **above** the catch-all.

## Backup

`BackupRepositoryImpl` turns `UserDataDao`'s rows into a `BackupSnapshot` (JSON, in `backup/model/`, mapped by
`mapper/BackupMapper.kt`) plus one stream per list cover, and cuts each stream into base64 parts of at most
`PART_MAX_CHARS`, at most `MAX_BACKUP_PARTS` in all. The order is what makes it safe:

- **A backup writes every part first and the manifest last.** The manifest is the commit point: until it is written the
  previous manifest still points at the previous, untouched parts. Stale parts are swept afterwards, best effort.
- **A restore downloads and checks everything before it changes anything**, then replaces the device's data in one
  Room transaction (`UserDataDao.replaceUserData`). Cover files it imported are deleted on any exit before that
  commits, a cancellation included.
- **Deleting reverses the backup**: parts first, then the manifest.
- One `Mutex` keeps backing up, restoring and deleting from overlapping. Keep a single instance of the repository.
- **Changing `BACKUP_FORMAT_VERSION` is a compatibility decision.** An older app refuses a newer backup with
  `BackupFromNewerVersion`; a field added with a default needs no bump, since the JSON ignores unknown keys.

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

There is one engine per platform: `GameDescriptionTranslatorImpl` runs Gemini Nano on Android, and
`AppleLanguageModelTranslator` runs Apple's on-device model on iOS. They share the prompt builder, the sanitizer
and `translateWithCache`; each supplies only its generate step and its status and download handling. Do not add a
second engine to a platform that has one. ML Kit's classic Translation API and Apple's Translation framework were
both weighed and dropped for the same reason: they download a per-language-pair model, the per-app download this
feature exists to avoid, and translate sentence by sentence with no notion of the domain.

The iOS translator reaches FoundationModels through `AppleLanguageModelBridge`, implemented in Swift under
`iosApp/` and bound by `startQuestLog`. The bridge is callback-based on purpose (a `suspend` member
implemented in Swift cannot be cancelled), and the translator never reports `DOWNLOADABLE`: the system owns that
model, so `downloadModel()` only polls for it to become available.

Three files sit beside it in `commonMain`, `internal` top-level functions as everywhere else in this module and
all free of the client: `TranslationPromptBuilder.kt` (`buildTranslationInstructions(targetLanguage)`,
`buildTranslationSource(description)`) owns the prompt text, `TranslationArtifactSanitizer.kt`
(`String.stripTranslationArtifacts()`) strips what a small on-device model leaves behind, and
`TranslationCache.kt` (`TranslationDao.translateWithCache(...)`) holds the length guard, the Room cache and its
staleness check, taking only the engine's generate step. All three are common so every engine shares one prompt
and one cache. Keep them out of the translator — they are the parts worth unit-testing without a device.

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

The worker-factory half lives in `:androidApp`, which is the Android-only part of the entry point: `QuestLogApp` implements `Configuration.Provider` and hands WorkManager a
`KoinWorkerFactory`, and it is also where `schedulePeriodicRefresh()` is called from.

On iOS the same contract is `BackgroundTaskReleaseRefreshScheduler`: the refresh runs in the app at launch and when
the saved set changes, and the system also launches it through a `BGAppRefreshTask`. The guarantee is "at most about
once per interval, when the system allows", never an exact cadence, so the in-app refresh stays as the fallback. All
the rules are in `ReleaseRefreshRunner`, which both paths share:

- A refresh is due when the last *successful* one is missing or at least `RELEASE_DATES_REFRESH_INTERVAL` old. The
  time is written only after the use case succeeds, never on a failure or a cancellation.
- One refresh runs at a time, and a request that arrives during one **joins** it rather than being dropped. A dropped
  background run would tell the system it is done and let the app be suspended mid-refresh.
- The next background request is submitted **before** every run and again **after** it, whatever the outcome, with
  `earliestBegin` counted from the last success. Do not move the first submit after the work: a suspended or killed
  app would never reach it, and the chain would end.
- Swift (`ReleaseRefreshTask`) only registers the task, submits, and passes the outcome and the expiration across
  through `AppRefreshTaskBridge` and `AppRefreshTaskHandler`. Koin is started by `startQuestLog` from the app
  delegate, not by `MainViewController`, because a background launch builds no UI.

## Release reminders on iOS

The system delivers a local notification with no app code running, so the pending set has to be right ahead of time
and a reminder that fired can only be recognised afterwards. `LocalNotificationReleaseScheduler` arms one request per
game (identifier `release-reminder-<gameId>`, a floating 09:00 calendar trigger, or a one-second trigger when the
time has passed) and records it in `ArmedReminderLedger`; an entry whose request is no longer pending fired, and
`takeFiredReminders()` reports it once so `SyncReleaseNotificationsUseCase` marks it delivered instead of arming the
same date again. Three rules are easy to break:

- **`add` checks the permission itself.** The system accepts a request from an app that may not notify and never
  delivers it, so `UserNotificationCenter.add` returns `false` unless authorized, and the scheduler then writes no
  ledger entry. The next sync after the permission is granted arms it.
- **At most 64 are pending.** After every `add` the scheduler trims to the 64 soonest and drops the rest from the
  ledger too, so a trimmed game is armed again once there is room and is never reported as fired.
- **`SyncReleaseNotificationsUseCase` is a `single` with a `Mutex`.** `takeFiredReminders()` removes the ledger entry
  before the delivered date is written, so a second sync in between would re-arm a fired reminder. A `factory` gives
  every caller its own lock.

`ReleaseReminderCoordinator` runs the sync at launch, on every `ReminderSyncTriggers` event (the app became active,
permission newly granted) and for a game that leaves the saved set; a trigger during a sync becomes one follow-up
sync. `ReleaseRefreshRunner` also syncs after a successful refresh. The reminder text comes from `ReleaseReminderText`,
implemented in `:shared` from `:core:ui`'s Compose resources for both platforms.
