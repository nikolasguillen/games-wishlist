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
    suspend fun persist(source: PickedImage): String?   // absolute path of the stored, downscaled image
    suspend fun delete(path: String)
}
```

| | Android | iOS |
|---|---|---|
| Decode + downscale | `ImageDecoder` to the existing max dimension (unchanged) | `UIImage` from the picked data, scaled to the same max dimension |
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

**Consumers**: `SettingsViewModel`, `ReleaseNotificationsViewModel`, `GameDetailViewModel`,
`OnboardingViewModel`, `RadarViewModel`. Each one injects it directly, as for any other dependency, and
gets a host test that sets it to `false` and asserts the entry point is absent from the UiState.

### `PickedImage` + `rememberCoverImagePicker` — `:core:ui`

```kotlin
// commonMain — value handed from the picker to the ViewModel to the storage
class PickedImage(val bytes: ByteArray)

@Composable
expect fun rememberCoverImagePicker(onPicked: (PickedImage?) -> Unit): CoverImagePickerLauncher

interface CoverImagePickerLauncher { fun launch() }
```

| Android | iOS |
|---|---|
| `PickVisualMedia(ImageOnly)` launcher; reads the picked `Uri` into bytes off the main thread (same user flow as today) | `PHPickerViewController` (one image, images only), presented from the current `UIViewController`; loads `UIImage` data |

**Ownership**: the composable owns only launching and returning the result. The ViewModel decides what
to do with it (Principle III).

### `rememberTextSharer` — `:core:ui`

```kotlin
@Composable
expect fun rememberTextSharer(): TextSharer

fun interface TextSharer { fun share(text: String) }
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
| `WindowCompat.getInsetsController(...).isAppearanceLight*Bars` (moved from `QuestLogTheme`) | No-op in this feature. If the iOS walkthrough shows poor status-bar contrast, the fix goes in `iosApp/` (`UIViewController.preferredStatusBarStyle`), not in shared code |

### `DateUtils` platform rendering — `:core:common`

```kotlin
enum class DateStyle { SHORT, MEDIUM, LONG, FULL }

internal expect fun formatLocalDate(date: LocalDate, pattern: String): String
internal expect fun formatLocalDate(date: LocalDate, style: DateStyle): String
```

| Android | iOS |
|---|---|
| `java.time` `DateTimeFormatter.ofPattern(pattern, Locale.getDefault())` / `ofLocalizedDate(style)`, the same output as today | `NSDateFormatter` with `locale = NSLocale.currentLocale`, `dateFormat = pattern` / `dateStyle` mapped from `DateStyle` |

`DateUtils`'s public functions keep their names and parameters. Only the style parameter's type changes,
from `FormatStyle` to `DateStyle`.

### Connectivity classification — `:core:data`

```kotlin
internal expect fun Throwable.isConnectivityFailure(): Boolean
```

| Android | iOS |
|---|---|
| `UnknownHostException`, `ConnectException`, `SocketException`, `SocketTimeoutException` (moved from `RepositoryErrorMapper`) | `DarwinHttpRequestException` whose `NSError.domain == NSURLErrorDomain` and whose code is one of: not connected, cannot find/connect to host, network connection lost, timed out, DNS lookup failed |

`RepositoryErrorMapper` stays in `commonMain`. Before calling this function, it:

- rethrows `CancellationException` first;
- handles `IgdbHttpException`, `HttpRequestTimeoutException` and `kotlinx.io.IOException` in common code.

## Platform shell (not contracts)

| Concern | Android (`:app`) | iOS (`iosApp/`) |
|---|---|---|
| Entry point | `MainActivity` → `setContent { QuestLogRoot(displayCornerRadius = …, onReady = …) }` | `ContentView` → `MainViewController()` |
| DI start | `QuestLogApp.onCreate` → `startKoin { androidContext(this); workManagerFactory(); modules(sharedModules + androidPlatformModules) }` | `MainViewController()` → idempotent `initKoin(iosPlatformModules)` |
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
| Status-bar icon colour follows the app theme | Yes | System default unless the walkthrough shows a contrast problem | Possibly system-coloured status bar text | Fix in `iosApp/` if needed |
