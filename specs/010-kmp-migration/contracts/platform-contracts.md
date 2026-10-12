# Contract: Platform Capabilities

Shared code owns every contract below, and each platform supplies one implementation (FR-007). The
signatures are the agreed shape, not finished code. KDoc and parameter names follow the house style
when they are written.

## Rules for every contract

- **Where the contract lives**: in `commonMain` of the module that owns the concept today. Domain-level contracts live in `:core:domain`, utility contracts in `:core:common`, and UI-scoped ones in `:core:ui` or `:core:designsystem`.
- **Where implementations live**: in the `androidMain`/`iosMain` of the module that already holds the Android implementation. `:core:domain` never gains a platform source set.
- **How they are bound**: each implementation is bound in that module's platform Koin module, and `:shared` assembles the platform modules.
- **No platform types in signatures.** No platform type (`Context`, `Uri`, `NSURL`, `UIViewController`) appears in any contract signature.
- **Tests use fakes.** A test double for each non-composable contract lives in the test source set of the module that consumes it, never in `commonMain`.

## Existing contracts that gain an iOS implementation

### `ReleaseRefreshScheduler` — `:core:domain` (unchanged signature)

```kotlin
interface ReleaseRefreshScheduler {
    fun schedulePeriodicRefresh()
    fun scheduleImmediateRefresh()
}
```

| | Android | iOS |
|---|---|---|
| `schedulePeriodicRefresh()` | Unique periodic WorkManager job, 24h, unmetered (unchanged) | Submits the next background request, runs `RefreshReleaseDatesUseCase` in the app-scoped coroutine scope if `release_dates_last_refresh_epoch_ms` is missing or older than 24h (recording the time on success), then submits again with the new date |
| `scheduleImmediateRefresh()` | One-shot WorkManager job (unchanged) | Submits the next background request, runs `RefreshReleaseDatesUseCase` now in the app scope (recording the time on success), then submits again with the new date. A refresh already running is joined, not duplicated |
| *(system-launched task)* | WorkManager runs the job (unchanged) | The system launches the app and the `BGAppRefreshTask` handler submits the next request, runs the refresh if due (joining one already in flight), submits again, then reports the outcome through `AppRefreshTaskHandler` |

The 24h interval is a single constant shared by both implementations. On iOS it is a floor, not a schedule: the
system decides when to run the task, so the guarantee is "at most about once per interval, when the system allows".
The next request's earliest date is `lastSuccess + 24h`, or now when there is no success or it is overdue.

### `ReleaseNotificationScheduler`, `ReleaseNotifier` — `:core:domain` (changed in `specs/013-ios-push-notifications`)

```kotlin
data class ReleaseReminder(val gameId: Int, val gameName: String, val releaseDateEpochSeconds: Long, val fireAt: Instant)

interface ReleaseNotificationScheduler {
    suspend fun schedule(reminder: ReleaseReminder)   // arms or re-arms; replaces any pending one
    suspend fun cancel(gameId: Int)
    suspend fun takeFiredReminders(): Map<Int, Long>  // gameId → the release date it was armed for, each reported once
}

interface ReleaseNotifier {
    suspend fun notifyReleased(gameId: Int, gameName: String)   // `canDeliver()` is gone: nothing called it
}
```

Signatures changed from `schedule(gameId, at: Instant)` / `cancel(gameId)` / `notifyReleased(...)` / `canDeliver()`.

| | Android | iOS |
|---|---|---|
| `schedule` | WorkManager one-shot per game, `REPLACE` (unchanged); ignores `gameName` and `releaseDateEpochSeconds` | `LocalNotificationReleaseScheduler`: a local notification through `UserNotifications`. Floating 09:00 calendar trigger on the release day, or a 1 s trigger when `fireAt` has passed. Records an armed-reminder ledger entry, then trims to the 64 soonest |
| `cancel` | `cancelUniqueWork` (unchanged) | Removes the pending request and the ledger entry |
| `takeFiredReminders` | `emptyMap()`: the worker records delivery itself | Ledger entries whose request is no longer pending, removed from the ledger as they are returned |
| `notifyReleased` | `NotificationManagerCompat`, text from `ReleaseReminderText` | Not bound: the system delivers the notification and no app code runs at fire time |

`SyncReleaseNotificationsUseCase` calls `takeFiredReminders()` first and marks each result delivered. It is a single,
serialized instance (`singleOf` plus a `Mutex`), so two syncs never interleave that step with the delivered-date
read.

### `GameDescriptionTranslator` — `:core:domain` (unchanged signature)

| | Android | iOS |
|---|---|---|
| Model status | From ML Kit via `:core:ai` (unchanged) | From Apple's on-device model (FoundationModels, iOS 26+) through `AppleLanguageModelBridge`. An English preferred language → `UNSUPPORTED`; `.available` with the preferred language supported → `READY`; `.unavailable(.modelNotReady)` → `DOWNLOADING`; everything else → `UNSUPPORTED` (iOS below 26, an ineligible device, Apple Intelligence off, an unsupported language, a model reporting a 0-token context window) |
| Translate | ML Kit (unchanged) | A fresh `LanguageModelSession` per call, with permissive-content-transformation guardrails, a token-budget check on iOS 26.4+ and the shared Room cache. `null` on any failure |
| Download | ML Kit (unchanged) | The app cannot start one: the system fetches the model. Polls availability every 15 s, `Completed` when available and `Failed` when it becomes unavailable for any other reason |

The only UI difference is the `TranslationEngineBadge` under a translated description (see the compatibility seams
below). Otherwise iOS needs no UI change: it never reports `DOWNLOADABLE`, so the Settings Download button and the Wi-Fi
dialog are never shown; `UNSUPPORTED` hides the Settings row and the Game detail action.

### `AppleLanguageModelBridge` — `:core:data` (implemented in Swift)

FoundationModels is a Swift-only framework with no Objective-C surface, so Kotlin/Native cannot import it.
Kotlin owns this interface and Swift (`iosApp/iosApp/FoundationModelsBridge.swift`) implements it.
The app delegate passes the implementation to `startQuestLog(languageModelBridge:appRefreshTaskBridge:)`, which
binds it in Koin.

```kotlin
interface AppleLanguageModelBridge {
    fun preferredLanguageTag(): String
    fun preferredLanguageEnglishName(): String
    fun availability(): AppleLanguageModelAvailability   // AVAILABLE, NOT_READY, UNAVAILABLE, LANGUAGE_UNSUPPORTED
    fun generate(instructions: String, prompt: String, onResult: (String?) -> Unit): AppleLanguageModelGeneration
}
interface AppleLanguageModelGeneration { fun cancel() }
```

- Nothing is `suspend`: a `suspend` member implemented in Swift has no cancellation and crashes on a thrown
  Swift error. `generate` reports through a callback and returns a handle that cancels the Swift task.
- The language comes from the bridge, read from the user's preferred languages. The app's locale would always be
  English, because the app ships English resources only.
- `AppleLanguageModelTranslator` lives in `commonMain`, not `iosMain`, so it can be tested on the JVM against a
  fake bridge, like `BackgroundTaskReleaseRefreshScheduler`. It is bound only by the iOS `dataPlatformModule`. This
  is a deliberate exception to "implementations live in `androidMain`/`iosMain`".

### `AppRefreshTaskBridge`, `AppRefreshTaskHandler` — `:core:data` (iOS background refresh)

`BackgroundTasks` is reached from Swift (`iosApp/iosApp/ReleaseRefreshTask.swift`), which registers the task,
submits requests and completes the system task. Kotlin owns both directions and keeps every scheduling decision
(the due check, one run at a time, the timestamp rule, the next request's earliest date), so the JVM tests reach it.

```kotlin
interface AppRefreshTaskBridge {            // Kotlin → Swift
    fun submitRequest(earliestBeginEpochMillis: Long)
}
interface AppRefreshTaskHandler {           // Swift → Kotlin
    fun run(onFinished: (Boolean) -> Unit): AppRefreshTaskRun
}
interface AppRefreshTaskRun { fun cancel() }
```

- `submitRequest` replaces the pending request and may be called from any thread. A rejected submit (background
  refresh off, the Simulator) is logged by Swift and swallowed.
- `onFinished` is called exactly once, after the next request has been submitted: `true` for a refresh that
  succeeded or was not due, `false` for one that failed or was cancelled. `cancel()` is the system's expiration.
- Nothing is `suspend`, for the reasons recorded for `AppleLanguageModelBridge`.
- `BackgroundTaskReleaseRefreshScheduler` and `ReleaseRefreshRunner` live in `commonMain` and are bound only by the
  iOS `dataPlatformModule`, the same exception as `AppleLanguageModelTranslator`.

### `LocalNotificationCenter`, `ReminderSyncTriggers`, `ReleaseReminderText` — `:core:data` (iOS release reminders)

```kotlin
internal interface LocalNotificationCenter {          // the iOS notification centre, as the scheduler sees it
    suspend fun add(request: LocalReminderRequest): Boolean   // false: not authorized, or rejected. Never throws but CancellationException
    suspend fun pendingReminders(): List<PendingReminder>
    suspend fun remove(gameIds: Collection<Int>)
}
interface ReminderSyncTriggers { val events: Flow<Unit> }    // app became active, permission newly granted
interface ReleaseReminderText {                              // implemented in :shared from :core:ui's Compose resources
    suspend fun title(gameName: String): String
    suspend fun body(): String
}
```

- `LocalNotificationReleaseScheduler`, `ReleaseReminderCoordinator` and the armed-reminder ledger live in
  `commonMain` and are bound only by the iOS `dataPlatformModule`, the same exception as `AppRefreshTaskBridge`'s
  `BackgroundTaskReleaseRefreshScheduler`. Only `UserNotificationCenter` (the `UserNotifications` adapter) is `iosMain`.
- `ReminderSyncTriggers` is implemented in `:shared` `iosMain` (`IosReminderSyncTriggers`), because the permission cache
  it listens to lives in `:core:ui`. `ReleaseReminderText` is implemented in `:shared` `commonMain` and used by both
  platforms.
- `ReleaseRefreshRunner` also runs a full reminder sync after a successful refresh, so a date that moved is
  followed while the app is in the background.

### `AppVersionProvider`, `NetworkStatusProvider` — `:core:common` (class → interface)

```kotlin
interface AppVersionProvider { val versionName: String }
interface NetworkStatusProvider { val isUnmeteredNetworkAvailable: Boolean }
```

| | Android | iOS |
|---|---|---|
| `versionName` | `PackageManager` lookup (unchanged logic) | `NSBundle.mainBundle` `CFBundleShortVersionString`, empty on miss |
| `isUnmeteredNetworkAvailable` | `ConnectivityManager` `NET_CAPABILITY_NOT_METERED` (unchanged) | Last `NWPathMonitor` path: `satisfied && !isExpensive && !isConstrained` |

### `WishlistCoverImageStorage` — `:core:data`

```kotlin
interface WishlistCoverImageStorage {
    suspend fun persist(source: String): String?   // absolute path of the stored, downscaled image
    suspend fun delete(path: String)
}
```

| | Android | iOS |
|---|---|---|
| Decode + downscale | `ImageDecoder` from the content `Uri` string, to the existing max dimension (unchanged) | `UIImage(contentsOfFile:)` from the temporary file path, scaled to the same max dimension; the temporary file is deleted afterwards |
| Location | The current internal files subdirectory (unchanged) | `Application Support/<same subdirectory name>` |
| File name | `UUID` (`java.util.UUID`) | same (`NSUUID`) |

It is a concrete class today and becomes an interface so that the repository stays in `commonMain`.

## New contracts

### `ReleaseRemindersAvailability` — `:core:domain`

```kotlin
interface ReleaseRemindersAvailability { val isAvailable: Boolean }
```

| Android | iOS |
|---|---|
| `true` | `false` |

**Consumers**: `SettingsViewModel`, `GameDetailViewModel`, `OnboardingViewModel`, `RadarViewModel`.
`ReleaseNotificationsViewModel` is not a consumer: its screen is only reachable from the Settings row that
the flag already removes. Each one injects it directly, as for any other dependency, and
gets a host test that sets it to `false` and asserts the entry point is absent from the UiState.

### `rememberCoverImagePicker` — `:core:ui`

```kotlin
// `source` is an opaque reference that the same platform's WishlistCoverImageStorage understands. It is
// what CoverImageUpdate.Replace(sourceUri) already carries today, so no domain type changes.
@Composable
expect fun rememberCoverImagePicker(onPicked: (source: String?) -> Unit): CoverImagePickerLauncher

// A class rather than an interface: the launcher is only ever created by the platform's `actual`.
class CoverImagePickerLauncher(private val onLaunch: () -> Unit) { fun launch() = onLaunch() }
```

| Android | iOS |
|---|---|
| `PickVisualMedia(ImageOnly)` launcher; `source` is the picked content `Uri` string (exactly today's value) | `PHPickerViewController` (one image, images only), presented from the current `UIViewController`; the picked image is copied to a temporary file and `source` is that file's absolute path |

**Ownership**: the composable owns only launching and returning the result. The ViewModel decides what
to do with it (Principle III).

### `rememberTextSharer` — `:core:ui`

```kotlin
@Composable
expect fun rememberTextSharer(): TextSharer

// A class, like the picker's launcher: only the platform's `actual` creates one.
class TextSharer(private val onShare: (text: String) -> Unit) { fun share(text: String) = onShare(text) }
```

| Android | iOS |
|---|---|
| `ACTION_SEND` + `Intent.createChooser` (moved verbatim from `GameDetailScreen`) | `UIActivityViewController` with the text, presented from the current `UIViewController` |

### Notification permission UI — `:core:ui`

The existing `NotificationPermission.kt` helpers and `NotificationPermissionDeniedDialog` move to
`androidMain` unchanged. `commonMain` gets the `expect` declarations their callers use:

| Android | iOS |
|---|---|
| Existing behaviour | `rememberNotificationPermissionState` reads the `NotificationAuthorization` cache (`NOT_DETERMINED`, `DENIED`, `AUTHORIZED`). `request()` shows the system prompt when undecided and otherwise opens the app's notification settings. The cache is refreshed on `ON_RESUME` and after a prompt. `NotificationPermissionDeniedDialog` is a `CustomAlertDialog` whose confirm opens the app's notification settings |

The iOS helpers sit in `:core:ui` `iosMain`: `NotificationAuthorization` and `NotificationAuthorizationStatus` (public, so
that `:shared` can observe the transition to `AUTHORIZED`) and `OpenNotificationSettings` (opens
`UIApplicationOpenNotificationSettingsURLString`).

### `SystemBarsAppearance` — `:core:designsystem`

```kotlin
@Composable
internal expect fun SystemBarsAppearance(darkTheme: Boolean)
```

| Android | iOS |
|---|---|
| `WindowCompat.getInsetsController(...).isAppearanceLight*Bars` (moved from `QuestLogTheme`) | No-op in this feature. Compose Multiplatform already keeps the iOS status-bar text readable against the app theme (checked in the walkthrough), so `iosApp/` needs no `preferredStatusBarStyle` |

### `DateUtils` platform rendering — `:core:common`

```kotlin
enum class DateStyle { SHORT, MEDIUM, LONG, FULL }

internal expect fun renderLocalDate(date: LocalDate, pattern: String): String
internal expect fun renderLocalDate(date: LocalDate, style: DateStyle): String
```

| Android | iOS |
|---|---|
| `java.time` `DateTimeFormatter.ofPattern(pattern, Locale.getDefault())` / `ofLocalizedDate(style)`, the same output as today | `NSDateFormatter` with `locale = NSLocale.currentLocale`, `dateFormat = pattern` / `dateStyle` mapped from `DateStyle` |

`DateUtils`'s public functions keep their names and parameters. Only the style parameter's type changes,
from `FormatStyle` to `DateStyle`.

### Connectivity classification — `:core:data` and `:core:network`

```kotlin
// :core:data
internal expect fun Throwable.isConnectivityFailure(): Boolean
internal expect fun Throwable.isTimeoutFailure(): Boolean

// :core:network
internal expect fun Throwable.toPlatformTransportFailure(): Throwable?
```

| | Android | iOS |
|---|---|---|
| `:core:network` `toPlatformTransportFailure()` | `null`: OkHttp's failures are `java.net` types | A `DarwinHttpRequestException` whose `NSError.domain == NSURLErrorDomain` becomes `IgdbTimeoutException` (timed out) or `IgdbConnectivityException` (not connected, cannot find/connect to host, connection lost, DNS lookup failed); anything else is `null` |
| `:core:data` `isConnectivityFailure()` / `isTimeoutFailure()` | `UnknownHostException`, `ConnectException`, `SocketException` / `SocketTimeoutException`, and the network module's two exception types | The network module's two exception types |

The Darwin engine's exception is a Ktor type, and `:core:data` must not depend on Ktor, so the
`NSError` translation is in `:core:network`, which already owns every Ktor-to-own-exception step.
`RepositoryErrorMapper` stays in `commonMain` and rethrows `CancellationException` first.

### Compatibility seams found during implementation

Small `expect`s that are not capabilities but keep one source set free of a platform difference. Each has a
single `actual` per platform and no behaviour of its own to specify.

| `expect` | Module | Why it exists |
|---|---|---|
| `fullScreenDialogProperties()` | `:core:ui` | How a dialog opts out of the platform's own insets so it can fill the screen edge to edge is a per-platform `DialogProperties` option (`decorFitsSystemWindows` on Android, `usePlatformInsets` on iOS) |
| `SearchBarScrollBehaviorCompat` | `:feature:search` | Android is pinned to the androidx Material 3 `1.5.0-beta01` search-bar API and iOS runs JetBrains' `1.12.0-alpha03`, whose scroll-behaviour members differ. Removed when both ship the same stable release |
| `TranslationEngineBadge` | `:feature:game-detail` | Credits the on-device model behind a translated description: Gemini Nano with its logo on Android, Apple Intelligence with a neutral sparkle on iOS. Each `actual` only picks `GeminiNanoBadge` or `AppleIntelligenceBadge`, which live in `commonMain` so the preview can show both and share `TranslationBadge` |
| `runBlockingCompat` | `:shared` | `runBlocking` is not visible from `commonMain`; the default-wishlist seed reads Compose resources, which are suspending, from Room's synchronous creation callback on its own thread |
| `applicationSupportDirectory()` and `TopViewController` (iOS only) | `:core:common`, `:core:ui` | Helpers the iOS `actual`s share: where Room, DataStore and cover files live, and which `UIViewController` presents the photo picker and the share sheet |
| `TranslationMapper` (Android only) | `:core:data` | Part of the Android translator: it maps ML Kit's status and download types, which the iOS translator never sees |
| `databasePlatformModule` | `:core:database` | Binds the Room builder: the database file lives in the app's database directory on Android and in `Application Support` on iOS |
| `networkPlatformModule` | `:core:network` | Binds the HTTP engine (OkHttp, Darwin) and `ElapsedRealtimeSource`, the monotonic clock the token cache expires against |

## Platform shell (not contracts)

| Concern | Android (`:androidApp`) | iOS (`iosApp/`) |
|---|---|---|
| Entry point | `MainActivity` → `setContent { QuestLogRoot(pendingDeepLinkGameId, onDeepLinkConsumed, displayCornerRadius) }`; the splash is held by `MainActivity` itself until `RootViewModel` has the onboarding flag | `AppDelegate` → `startQuestLog(...)`; `ContentView` → `MainViewController()` |
| DI start | `QuestLogApp.onCreate` → `initKoin(isDebugBuild = BuildConfig.DEBUG) { androidContext(this) }`; the WorkManager factory is the `Application`'s `Configuration.Provider` | `startQuestLog(...)` from the app delegate's `didFinishLaunching` → idempotent `initKoin(isDebugBuild = …)`, so a background launch with no UI has the graph too; the iOS bindings are the `expect` platform modules' `iosMain` actuals, which `initKoin` already lists |
| Splash | `core-splashscreen`, held until the start route is known (unchanged) | `LaunchScreen` storyboard with the app logo |
| Swift services handed to Kotlin | None | `FoundationModelsBridge` (FoundationModels) and `ReleaseRefreshTask` (BackgroundTasks), bound in Koin by `startQuestLog` |
| Periodic refresh kick-off | `QuestLogApp` calls `schedulePeriodicRefresh()` (unchanged) | `startQuestLog(...)` calls `schedulePeriodicRefresh()` once per launch; the system also launches the refresh task in the background |
| Display corner radius | `RoundedCorner` probe (unchanged) | Fixed default passed by `MainViewController()` |
| Deep link to a game (`questlog://game/{id}`, used by the reminder notification) | `MainActivity` parses the intent (`onCreate` and `onNewIntent`) into a `GameDetailRoute` and hands it to `QuestLogRoot` as a pending route | No URL scheme. `ReleaseReminderNotificationDelegate` (`:shared` `iosMain`, set by `startQuestLog`) reads `gameId` from the tapped notification's `userInfo` → `ReminderDeepLinks` → `MainViewController` → `QuestLogRoot` as the pending game id |

## Capability exception register

The complete list of behaviour that differs by platform in this feature. **Update this table in the
same commit as any change to it.**

| Capability | Android | iOS | User sees on iOS | Follow-up |
|---|---|---|---|---|
| Release reminders (notifications, permission prompt, per-game toggle, settings rows, onboarding page) | Full | Absent | No reminder controls anywhere | `docs/roadmap.md` — "Release reminders on iOS" |
| Release-date refresh while the app is closed | WorkManager, every 24h | None | Dates refresh at launch (if over 24h old) and when the saved set changes | Same roadmap entry |
| Status-bar icon colour follows the app theme | Yes | Follows the app theme without any code: the walkthrough (force dark under a light system theme and force light under a dark one) showed readable status-bar text both ways | None | None needed |
