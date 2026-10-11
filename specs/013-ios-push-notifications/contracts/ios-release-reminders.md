# Contract: iOS Release Reminders

These are the agreed shapes, not finished code. KDoc and parameter names follow the house style when they are
written. The rules in `specs/010-kmp-migration/contracts/platform-contracts.md` apply: no platform type in a
signature, fakes live in the consuming module's test source set, and each implementation is bound in its module's
platform Koin module.

## 1. Domain contracts — `:core:domain/notification/` (changed)

```kotlin
data class ReleaseReminder(
    val gameId: Int,
    val gameName: String,
    val releaseDateEpochSeconds: Long,
    val fireAt: Instant
)

interface ReleaseNotificationScheduler {
    /** Arms (or re-arms) the reminder for [reminder].gameId, replacing any pending one. */
    suspend fun schedule(reminder: ReleaseReminder)

    /** Removes any pending reminder for [gameId]. A no-op when there is none. */
    suspend fun cancel(gameId: Int)

    /**
     * Reminders the system delivered without running app code since the last call, as gameId → the release date
     * each was armed for. Each one is reported once.
     */
    suspend fun takeFiredReminders(): Map<Int, Long>
}

interface ReleaseNotifier {
    /** Posts the reminder for [gameId] now. Only a platform whose reminders run app code at fire time calls it. */
    suspend fun notifyReleased(gameId: Int, gameName: String)
}
```

| | Android | iOS |
|---|---|---|
| `schedule` | Unchanged WorkManager one-shot (`REPLACE`), delay `fireAt - now`. Ignores `gameName` and `releaseDateEpochSeconds` | `LocalNotificationReleaseScheduler`, see §3 |
| `cancel` | Unchanged `cancelUniqueWork` | Removes the request and the ledger entry |
| `takeFiredReminders` | `emptyMap()`: the worker records delivery itself | Ledger entries whose request is no longer pending, removed from the ledger as they are returned |
| `notifyReleased` | Unchanged notification, with text from `ReleaseReminderText` | Not bound. Nothing on iOS calls it, so no implementation and no `NoOp` |

`ReleaseRemindersAvailability` is **deleted** (research §10).

### `SyncReleaseNotificationsUseCase` (changed behaviour, same signature)

1. First, `scheduler.takeFiredReminders()`. For each (id, date) that is still opted in, call
   `repository.markReleaseNotificationDelivered(id, date)`.
2. Then the existing loop, unchanged, except that it calls `schedule(ReleaseReminder(id, game.name, date, instant))`.

Step 1 on Android is a no-op, so the Android behaviour is unchanged. `DeliverReleaseNotificationUseCase` only
gains `suspend` on the notifier call.

## 2. Data-layer ports — `:core:data` `commonMain`

```kotlin
/** The reminder text. Implemented in `:shared` from `:core:ui`'s Compose resources (research §9). Public: :shared implements it. */
interface ReleaseReminderText {
    suspend fun title(gameName: String): String
    suspend fun body(): String
}

/** The iOS notification centre, as the reminder scheduler sees it. Implemented in `iosMain`; faked in tests. */
internal interface LocalNotificationCenter {
    /**
     * `false` when the reminder could not be armed: the app is not authorized to notify (the adapter reads the
     * authorization status first, because the system accepts a request it will never deliver) or the system
     * rejected the request. Never throws except `CancellationException`.
     */
    suspend fun add(request: LocalReminderRequest): Boolean
    suspend fun pendingReminders(): List<PendingReminder>
    suspend fun remove(gameIds: Collection<Int>)
}

internal data class LocalReminderRequest(
    val gameId: Int,
    val title: String,
    val body: String,
    val fireAt: LocalDateTime?   // null = fire in one second; otherwise floating calendar components
)

internal data class PendingReminder(val gameId: Int, val nextTriggerAt: Instant?)

/** What tells the iOS coordinator to resync: app became active, permission newly granted. Implemented in `:shared` iosMain. */
interface ReminderSyncTriggers {
    val events: Flow<Unit>
}
```

Each type gets its own file (the one-type-per-file rule).

## 3. `LocalNotificationReleaseScheduler` — `:core:data` `commonMain`, bound only on iOS

`ReleaseNotificationScheduler` over `LocalNotificationCenter`, `ReleaseReminderText`, the ledger, and a `Clock`.
Every call is serialized behind one `Mutex`, so calls for the same game apply in order.

`schedule(reminder)`:
1. If the ledger has `gameId:releaseDate` and that game is not pending, it already fired. Return.
2. Build the request. `fireAt ≤ now` gives an immediate trigger. Otherwise `fireAt` becomes a
   `LocalDateTime` in `TimeZone.currentSystemDefault()`.
3. `add`. If refused, remove any ledger entry for the game and return.
4. Write the ledger entry `gameId:releaseDate`, replacing any earlier date for that game.
5. `trimToCapacity(pending, capacity = 64)`. Remove what it returns from both the centre and the ledger.

`trimToCapacity(pending: List<PendingReminder>, capacity: Int): List<Int>` is a pure top-level function. It
returns the ids beyond the `capacity` soonest, ordering by `nextTriggerAt` with `null` (immediate) first and the
id as a tie-break.

## 4. `ReleaseReminderCoordinator` — `:core:data` `commonMain`, bound only on iOS

```kotlin
// Public class, internal constructor: `startQuestLog` in :shared calls start(), and Koin builds it inside :core:data.
class ReleaseReminderCoordinator internal constructor(
    private val syncReleaseNotifications: SyncReleaseNotificationsUseCase,
    private val getSavedGameIds: () -> Flow<Set<Int>>,   // from GameRepository.getSavedGames()
    private val triggers: ReminderSyncTriggers,
    private val scope: CoroutineScope
) {
    /** Idempotent. One full sync now, then: a full sync per trigger event; sync(id) for each id that leaves the saved set. */
    fun start()
}
```

- The first emission of the saved ids is a baseline, not a removal.
- Full syncs never overlap. A trigger that arrives during a sync is coalesced into **one** follow-up sync, so it is
  neither dropped nor queued many times over.

### `ReleaseRefreshRunner` (iOS-only, changed)

It takes `SyncReleaseNotificationsUseCase`. After `recordRefresh()` on a `REFRESHED` outcome, it runs a full sync
inside the same refresh. Joiners and the background task therefore wait for it. A failed sync does not turn a
successful refresh into `FAILED`: the dates are stored, and the next trigger resyncs.

## 5. `:shared` — iOS entry points

`startQuestLog(languageModelBridge, appRefreshTaskBridge)` keeps its **signature**. Swift does not change. It gains,
in this order, before it returns:

1. It sets the notification-centre delegate (`ReleaseReminderNotificationDelegate`, held by a top-level property).
2. It seeds the authorization cache.
3. It starts `ReleaseReminderCoordinator`, after `schedulePeriodicRefresh()`.

| New `iosMain` type | Role |
|---|---|
| `ReleaseReminderNotificationDelegate : NSObject, UNUserNotificationCenterDelegateProtocol` | `willPresent` → banner + list + sound. `didReceive` → `ReminderDeepLinks.emit(gameId)`, then `completionHandler()` |
| `ReminderDeepLinks` | `StateFlow<Int?>` plus `consume()`, process-wide |
| `IosReminderSyncTriggers : ReminderSyncTriggers` | Merges `UIApplicationDidBecomeActiveNotification` with the authorization cache's transitions to `AUTHORIZED` |

`MainViewController()` collects `ReminderDeepLinks` with `collectAsStateWithLifecycle()` and passes it to
`QuestLogRoot(pendingDeepLinkGameId = …, onDeepLinkConsumed = ReminderDeepLinks::consume, …)`. `QuestLogRoot` does
not change.

`ReleaseReminderText` is implemented in `:shared` **`commonMain`** with `getString(CoreUiRes.string.release_notification_title_format, gameName)`.
It is bound in `:shared`'s Koin assembly and used by Android's `ReleaseNotifierImpl` and by the iOS scheduler.

## 6. `:core:ui` — iOS `actual`s (changed)

| `expect` | iOS `actual` after this feature |
|---|---|
| `rememberNotificationPermissionState(onResult)` | Reads the authorization cache (research §8 table). `request()` prompts when `NOT_DETERMINED` and otherwise opens `UIApplicationOpenNotificationSettingsURLString`. It refreshes the cache on `ON_RESUME` and after a request, and calls `onResult` with the prompt's outcome |
| `NotificationPermissionDeniedDialog(onDismiss)` | `CustomAlertDialog` with the existing `notification_permission_denied_*` strings. Confirm opens the app's notification settings |

The authorization cache (`NotificationAuthorization`, `iosMain`, public so that `:shared` can observe it) exposes
`status: StateFlow<NotificationAuthorizationStatus>` and `suspend fun refresh()`.

## 7. Koin bindings

| Module | Android | iOS |
|---|---|---|
| `dataPlatformModule` | `ReleaseNotificationSchedulerImpl`, `ReleaseNotifierImpl` (unchanged); availability binding removed | `LocalNotificationReleaseScheduler` as `ReleaseNotificationScheduler`; `UserNotificationCenter` as `LocalNotificationCenter`; `ReleaseReminderCoordinator`; `ReleaseRefreshRunner` gains its sync argument; NoOp and availability bindings removed |
| `:shared` | `ReleaseReminderText` (commonMain) | Same, plus `IosReminderSyncTriggers` as `ReminderSyncTriggers` |

`KoinGraphTest` (Android) has to stay green once the availability binding is removed. The iOS graph is exercised by
launching on the simulator.

## 8. Changes to `specs/010-kmp-migration/contracts/platform-contracts.md`

Made in the same commit as the code that makes each one true:

- `ReleaseNotificationScheduler`, `ReleaseNotifier`: replace the iOS "No-op objects" row with §1 and §3, and record
  the signature change.
- `ReleaseRemindersAvailability`: delete the section.
- Notification permission UI: replace "never composed / inert" with §6.
- Deep link row: iOS gets "delegate → `ReminderDeepLinks` → `QuestLogRoot`; no URL scheme".
- Capability exception register: delete the "Release reminders" row. The "Release-date refresh while the app is
  closed" row already belongs to spec 012.
- Add `LocalNotificationCenter`, `ReminderSyncTriggers` and `ReleaseReminderText` beside `AppRefreshTaskBridge`,
  with the same "`commonMain` logic, iOS-only binding" exception note.
