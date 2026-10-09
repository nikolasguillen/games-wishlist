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
| `schedulePeriodicRefresh()` | Unique periodic WorkManager job, 24h, unmetered (unchanged) | If `release_dates_last_refresh_epoch_ms` is missing or older than 24h, launch `RefreshReleaseDatesUseCase` in the app-scoped coroutine scope; record the time on success |
| `scheduleImmediateRefresh()` | One-shot WorkManager job (unchanged) | Launch `RefreshReleaseDatesUseCase` now in the app scope; record the time on success. A refresh already running is not duplicated |

The 24h interval is a single constant shared by both implementations.

### `ReleaseNotificationScheduler`, `ReleaseNotifier` — `:core:domain` (unchanged signatures)

| | Android | iOS |
|---|---|---|
| Implementation | WorkManager + `NotificationManagerCompat` (unchanged) | No-op objects. They are unreachable from the UI because `ReleaseRemindersAvailability.isAvailable` is `false`, but bound so that shared use cases resolve |

### `GameDescriptionTranslator` — `:core:domain` (unchanged signature)

| | Android | iOS |
|---|---|---|
| Model status | From ML Kit via `:core:ai` (unchanged) | Always `TranslationModelStatus.UNSUPPORTED` |
| Translate / download | ML Kit (unchanged) | Never called. If called, it returns the failure result the Android implementation uses for an unsupported device |

`UNSUPPORTED` already hides the Settings row and the Game detail action, so iOS needs no UI change for
this.

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
| File name | `UUID` (via `kotlin.uuid.Uuid`) | same |

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
| Existing behaviour | Never composed, because every call site is behind `ReleaseRemindersAvailability.isAvailable` |

To keep the `expect` surface minimal, the iOS `actual`s are inert (no-ops that report "not granted").
They exist only so that `commonMain` compiles.

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
| `runBlockingCompat` | `:shared` | `runBlocking` is not visible from `commonMain`; the default-wishlist seed reads Compose resources, which are suspending, from Room's synchronous creation callback on its own thread |
| `applicationSupportDirectory()` and `TopViewController` (iOS only) | `:core:common`, `:core:ui` | Helpers the iOS `actual`s share: where Room, DataStore and cover files live, and which `UIViewController` presents the photo picker and the share sheet |
| `TranslationPromptBuilder`, `TranslationArtifactSanitizer`, `TranslationMapper` (Android only) | `:core:data` | Parts of the Android translator; the iOS translator is `UnsupportedGameDescriptionTranslator` |

## Platform shell (not contracts)

| Concern | Android (`:androidApp`) | iOS (`iosApp/`) |
|---|---|---|
| Entry point | `MainActivity` → `setContent { QuestLogRoot(pendingDeepLinkGameId, onDeepLinkConsumed, displayCornerRadius) }`; the splash is held by `MainActivity` itself until `RootViewModel` has the onboarding flag | `ContentView` → `MainViewController()` |
| DI start | `QuestLogApp.onCreate` → `initKoin(isDebugBuild = BuildConfig.DEBUG) { androidContext(this) }`; the WorkManager factory is the `Application`'s `Configuration.Provider` | `MainViewController()` → idempotent `initKoin(isDebugBuild = …)`; the iOS bindings are the `expect` platform modules' `iosMain` actuals, which `initKoin` already lists |
| Splash | `core-splashscreen`, held until the start route is known (unchanged) | `LaunchScreen` storyboard with the app logo |
| Periodic refresh kick-off | `QuestLogApp` calls `schedulePeriodicRefresh()` (unchanged) | `MainViewController()` calls `schedulePeriodicRefresh()` once per launch |
| Display corner radius | `RoundedCorner` probe (unchanged) | Fixed default passed by `MainViewController()` |
| Deep link to a game (`questlog://game/{id}`, used by the reminder notification) | `MainActivity` parses the intent (`onCreate` and `onNewIntent`) into a `GameDetailRoute` and hands it to `QuestLogRoot` as a pending route | Not registered (its only producer, reminders, is absent on iOS) |

## Capability exception register

The complete list of behaviour that differs by platform in this feature. **Update this table in the
same commit as any change to it.**

| Capability | Android | iOS | User sees on iOS | Follow-up |
|---|---|---|---|---|
| Release reminders (notifications, permission prompt, per-game toggle, settings rows, onboarding page) | Full | Absent | No reminder controls anywhere | `docs/roadmap.md` — "Release reminders on iOS" |
| Release-date refresh while the app is closed | WorkManager, every 24h | None | Dates refresh at launch (if over 24h old) and when the saved set changes | Same roadmap entry |
| On-device description translation | ML Kit Gemini Nano | Absent (`UNSUPPORTED`) | No translate action, no Settings row | `docs/roadmap.md` — "On-device description translation on iOS" |
| Status-bar icon colour follows the app theme | Yes | Follows the app theme without any code: the walkthrough (force dark under a light system theme and force light under a dark one) showed readable status-bar text both ways | None | None needed |
