# Contract: Desktop Implementations of the Platform Capabilities

This is the desktop column for `specs/010-kmp-migration/contracts/platform-contracts.md`. When this feature is
implemented, that file gains a **Desktop** column in every table below and the new contracts at the end, in the
same commit as the code (FR-032). The rules at the top of that file still apply: no platform type in a signature,
one implementation per platform, bound in the owning module's platform Koin module, fakes in tests.

"Desktop" is one JVM target (`desktopMain`) for both Windows and macOS. Where the two OSes differ, the
implementation picks at runtime through `DesktopNotifications` (below) or `os.name`.

## Existing contracts

| Contract (module) | Desktop implementation |
|---|---|
| `ReleaseRefreshScheduler` (`:core:domain`) | `InAppReleaseRefreshScheduler` over `ReleaseRefreshRunner`: `schedulePeriodicRefresh()` runs if due at launch, then rechecks hourly while the app is open; `scheduleImmediateRefresh()` runs now. Nothing while the app is closed |
| `ReleaseNotificationScheduler` (`:core:domain`) | The shared `LocalNotificationReleaseScheduler` (as on iOS) over `DesktopLocalNotificationCenter` |
| `ReleaseNotifier` (`:core:domain`) | Not bound: the OS delivers the notification and no app code runs at fire time |
| `LocalNotificationCenter` (`:core:data`, internal) | `DesktopLocalNotificationCenter` → `DesktopNotifications` (Windows scheduled toasts, macOS `UserNotifications`); mapping in [data-model.md §3](../data-model.md) |
| `ReminderSyncTriggers` (`:core:data`) | `DesktopReminderSyncTriggers` (`:shared` `desktopMain`): the window gained focus (after refreshing the permission cache), or the permission moved to `AUTHORIZED` |
| `ReleaseReminderText` (`:core:data`) | The shared `ReleaseReminderTextImpl` (unchanged) |
| `ReleaseReminderCoordinator` (`:core:data`) | Shared, with `beforeFirstSync` = the orphan sweep (research R15). iOS passes nothing |
| `GameDescriptionTranslator` (`:core:domain`) | `UnsupportedDescriptionTranslator`: `UNSUPPORTED`, never translates; a download reports failure at once (unreachable) |
| `AppVersionProvider` (`:core:common`) | `System.getProperty("questlog.version")`, set by the packaging and the run task; empty on miss |
| `NetworkStatusProvider` (`:core:common`) | `isUnmeteredNetworkAvailable = true` (only the translation download reads it) |
| `WishlistCoverImageStorage` (`:core:data`) | `javax.imageio` decode + downscale to the existing max dimension, JPEG out, in `appDataDirectory()/covers`; file name `java.util.UUID`; **never deletes the source** |
| `rememberCoverImagePicker` (`:core:ui`) | `java.awt.FileDialog`, one file, JPEG/PNG/GIF/BMP; `source` is the absolute path |
| `rememberTextSharer` (`:core:ui`) | Copies to the system clipboard; `copiesToClipboard = true` (contract change below) |
| `readableBitmaps` (`:core:ui`) | Returns the builder unchanged |
| Notification permission UI (`:core:ui`) | `rememberNotificationPermissionState` over `DesktopNotificationAuthorization` ([data-model.md §2](../data-model.md)); `NotificationPermissionDeniedDialog` is a `CustomAlertDialog` whose confirm calls `openSystemNotificationSettings()` |
| `SystemBarsAppearance` (`:core:designsystem`) | No-op: a desktop window has no system bars the app colours |
| `DateUtils` rendering (`:core:common`) | `java.time` `DateTimeFormatter` with `Locale.getDefault()`, the same as Android |
| Connectivity classification (`:core:data`, `:core:network`) | `:core:network` `toPlatformTransportFailure()` → `null` (OkHttp throws `java.net` types); `:core:data` classifies `UnknownHostException`, `ConnectException`, `SocketException` / `SocketTimeoutException` and the network module's two types, as on Android |
| `fullScreenDialogProperties()` (`:core:ui`) | Desktop `DialogProperties` with `usePlatformDefaultWidth = false` |
| `SearchBarScrollBehaviorCompat` (`:feature:search`) | The iOS `actual`, copied: desktop runs the same JetBrains Material 3 alpha. Both copies go when the API is stable |
| `TranslationEngineBadge` (`:core:ui`) | Draws nothing (unreachable) |
| `runBlockingCompat` (`:shared`) | `kotlinx.coroutines.runBlocking` |
| `databasePlatformModule` (`:core:database`) | `Room.databaseBuilder<QuestLogDatabase>(appDataDirectory()/questlog.db)` with `BundledSQLiteDriver` |
| `networkPlatformModule` (`:core:network`) | OkHttp engine; `ElapsedRealtimeSource` over `System.nanoTime()` |
| `CloudAuth`, `CloudBackupStore` (`:core:cloud`) | `RestCloudAuth`, `RestBackupStore` (research R3–R6, [firestore-rest.md](firestore-rest.md)). `provider = GOOGLE`. `PlatformProviderAuth` is not used on desktop |

### Contract change: `TextSharer`

```kotlin
// :core:ui commonMain. `copiesToClipboard` is false on Android and iOS, which open a share sheet.
class TextSharer(val copiesToClipboard: Boolean = false, private val onShare: (text: String) -> Unit) {
    fun share(text: String) = onShare(text)
}
```

When `copiesToClipboard` is `true`, game detail shows the snackbar `share_copied_to_clipboard` after `share`. The
string is new in `:core:ui`'s resources.

### Contract change: `ReleaseReminderCoordinator`

```kotlin
class ReleaseReminderCoordinator internal constructor(
    syncReleaseNotifications: SyncReleaseNotificationsUseCase,
    getSavedGameIds: () -> Flow<Set<Int>>,
    triggers: ReminderSyncTriggers,
    scope: CoroutineScope,
    beforeFirstSync: suspend () -> Unit = {}   // new; runs once, before the launch sync; a failure is swallowed
)
```

## New contracts

### `DesktopNotifications` — `:core:common` `desktopMain` (public: `:core:ui`, `:core:data`, `:desktopApp` use it)

```kotlin
interface DesktopNotifications {
    suspend fun status(): DesktopNotificationStatus
    suspend fun requestAuthorization(): Boolean          // macOS prompt; Windows returns status() == AUTHORIZED
    suspend fun schedule(request: DesktopNotificationRequest): Boolean   // false: not authorized or rejected
    suspend fun pending(): List<DesktopPendingNotification>
    suspend fun remove(gameIds: Collection<Int>)
    fun setClickHandler(onClick: (gameId: Int) -> Unit)  // the OS reports a click on one of ours
}

class DesktopNotificationRequest(val gameId: Int, val title: String, val body: String, val fireAt: LocalDateTime?)
class DesktopPendingNotification(val gameId: Int, val fireAt: Instant?)

fun desktopNotifications(): DesktopNotifications        // one instance per process, picked from os.name
fun appDataDirectory(): Path                            // research R10
fun openSystemNotificationSettings()                    // ms-settings:notifications / the macOS Notifications pane
```

- Nothing throws except `CancellationException`. A failure becomes `false`, an empty list or `UNSUPPORTED`.
- Every call runs off the main thread (`Dispatchers.IO`). WinRT calls run on one dedicated MTA thread.
- `setClickHandler` is called once, by `:desktopApp`, at the very start of `main`.

### Windows binding (`WindowsToastNotifications`, internal to `:core:common` `desktopMain`)

| Step | Call |
|---|---|
| Process start | `RoInitialize(RO_INIT_MULTITHREADED)`; `SetCurrentProcessExplicitAppUserModelID("com.nikolasguillen.questlog")`; HKCU registration (research R8) |
| Notifier | `RoGetActivationFactory("Windows.UI.Notifications.ToastNotificationManager")` → `CreateToastNotifierWithId(aumid)` |
| Schedule | `XmlDocument.LoadXml(toastXml)` → `IScheduledToastNotificationFactory.CreateScheduledToastNotification(xml, deliveryTime)` → `put_Id("g<id>")` → `AddToSchedule` |
| Immediate | `IToastNotificationFactory.CreateToastNotification(xml)` → `Show` |
| Pending | `GetScheduledToastNotifications` → `get_Id`, `get_DeliveryTime` |
| Remove | `RemoveFromSchedule` on each match |
| Status | `get_Setting` |
| Click | `CoRegisterClassObject(CLSID, factory, CLSCTX_LOCAL_SERVER, REGCLS_MULTIPLEUSE)`; `INotificationActivationCallback.Activate(aumid, "gameId=<id>", …)` → `onClick(id)` |

Toast XML (title and body XML-escaped):

```xml
<toast launch="gameId=123" activationType="foreground">
  <visual><binding template="ToastGeneric"><text>{title}</text><text>{body}</text></binding></visual>
</toast>
```

### macOS binding (`MacUserNotifications`, internal) and the Swift library ABI

`libQuestLogNotifications.dylib`, sources under `desktopApp/native/macos/`, built by `:desktopApp:buildMacNotifications`
(macOS only) and bundled with the macOS app resources:

```c
int32_t ql_authorization_status(void);                       // 0 notDetermined, 1 authorized, 2 denied
void    ql_request_authorization(void (*done)(int32_t granted));
void    ql_add_calendar(int32_t id, const char* title, const char* body,
                        int32_t y, int32_t m, int32_t d, int32_t h, int32_t min,
                        void (*done)(int32_t ok));
void    ql_add_immediate(int32_t id, const char* title, const char* body, void (*done)(int32_t ok));
void    ql_pending(void (*done)(int32_t count, const int32_t* ids, const double* epochSeconds)); // NaN: immediate
void    ql_remove(const int32_t* ids, int32_t count);
void    ql_set_click_handler(void (*onClick)(int32_t gameId)); // installs the UNUserNotificationCenter delegate
```

Every callback is called exactly once, on any thread. Without a bundle identifier, `ql_authorization_status` returns
`-1` and the other calls do nothing (`UNSUPPORTED`).

### Desktop shell (`:desktopApp`, not a contract)

| Concern | Desktop |
|---|---|
| Entry point | `main(args)`: `desktopNotifications().setClickHandler(ReminderDeepLinks::emit)` first; then single-instance check (hand over and exit if another instance runs); then `initKoin(isDebugBuild = …)`; `schedulePeriodicRefresh()`; permission refresh; `ReleaseReminderCoordinator.start()`; `application { Window(onCloseRequest = ::exitApplication) { QuestLogRoot(pending, consume) } }` |
| Arguments | `-ToastActivated` (Windows COM launch: nothing to do beyond the above) |
| Back | Escape and mouse button 4 → back event (research R16) |
| Window | Title "QuestLog", minimum 400 × 640, app icon; not persisted |
| Firebase | Nothing to start: REST only |
| Splash | None: the window opens on the start route once `RootViewModel` knows the onboarding flag, as the iOS root does |

## Capability exception register (rows this feature adds)

| Capability | Android | iOS | Desktop | User sees on desktop | Follow-up |
|---|---|---|---|---|---|
| Release-date refresh while the app is closed | WorkManager | `BGAppRefreshTask` | None | Dates refresh at launch (if over 24 h old), hourly while open, and when the saved set changes | None (owner decision) |
| On-device translation | ML Kit | FoundationModels | None | No translate action, badge or Settings row | None |
| Share sheet | `ACTION_SEND` | `UIActivityViewController` | Clipboard | "Copied to clipboard" | None |
| Sign in with Apple | Firebase web flow | Native | None | Google only | Needs a paid Apple account and an HTTPS redirect domain |
| Reminders from a dev run (macOS) | — | — | None | Notifications shown as blocked | Inherent: needs a bundle |
| Closed-app reminders verified (macOS) | — | — | Built, not accepted | — | Signing and notarization, with an Apple account |
| Reminder cleanup on uninstall (Windows) | System | System | None | A stale toast may appear on its date | MSI uninstall custom action |
| Intel Mac installer | — | — | Not built | — | Build on Intel or a universal runtime |
