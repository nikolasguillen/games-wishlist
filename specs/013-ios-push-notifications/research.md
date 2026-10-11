# Research: iOS Release Notifications

Every decision below was made against the code on `develop` at `b53b40cd`, which is spec 012's iOS background
refresh. The Android pipeline it mirrors is spec 003.

## 0. What Android does, and what iOS cannot copy

On Android a reminder is a one-shot WorkManager job (`ReleaseNotificationSchedulerImpl`). At fire time it runs
`DeliverReleaseNotificationUseCase` in the app's process. That use case re-checks every eligibility rule: still
opted in, still saved, an exact date, the day has come, not already notified. Then it posts the notification and
records `notifiedForDate`. The fire-time check is what lets `SyncReleaseNotificationsUseCase` be lazy, as spec 003
records: unsaving a game cancels nothing, because a stale job declines to post.

An iOS local notification is handed to the system with its content already built, and the system delivers it
**without running any app code**. That has three consequences, and they shape the rest of this document:

1. Nothing re-checks at fire time, so **the pending set must be correct ahead of time**. A game that is unsaved, or
   turned off, has its request removed at once (§4).
2. Nothing records the delivery, so **iOS has to work out afterwards that a reminder fired**. Otherwise the next
   sync on release day re-arms it (§3).
3. The content (the game's name) has to be known when scheduling, not at fire time (§5).

## 1. Who talks to the notification centre: Kotlin, not Swift

**Decision**: Kotlin `iosMain` calls `UserNotifications` (`platform.UserNotifications.UNUserNotificationCenter`)
directly. That covers scheduling in `:core:data`, the permission state in `:core:ui`, and the notification-centre
delegate in `:shared`. There is no Swift bridge, and `iosApp/` does not change.

**Rationale**: The project rule is that Swift appears only where Kotlin cannot reach an API: FoundationModels is
Swift-only, and the background task is registered in Swift. `UserNotifications` is an Objective-C framework that
Kotlin/Native already binds, and it is no harder to call from Kotlin than from Swift. Keeping it in Kotlin keeps the
logic in the shared, JVM-testable layer and adds no Swift↔Kotlin interface to maintain.

**Alternatives considered**:
- *A Swift `ReminderNotificationBridge` passed through `startQuestLog`, like `AppRefreshTaskBridge`.* Rejected. It
  would add an interface, a Swift class and a parameter only to forward calls Kotlin can make itself.
- *`AppDelegate` as the `UNUserNotificationCenterDelegate`.* Rejected for the same reason. The delegate only has to
  be set before `didFinishLaunching` returns, and `startQuestLog` already runs there (§6).

## 2. Trigger: a floating calendar date, or a one-second interval for "now"

**Decision**: A reminder whose fire instant is in the future gets a non-repeating `UNCalendarNotificationTrigger`.
Its date components are year, month, day, hour and minute, taken from the fire instant in the device's current
time zone, **with no time zone set**. When `resolveNotificationInstant` returns "now" (an exact release day that
is today, with 09:00 already past), the reminder gets a `UNTimeIntervalNotificationTrigger` of one second instead.

**Rationale**: Date components without a time zone are *floating*: the system fires them at 09:00 on the release
day in whatever zone the device is in at the time. That is FR-016, and it holds even if the user travels and does
not open the app. The fire hour stays `NOTIFICATION_HOUR` (09:00), so it matches Android. The one-second interval is
the smallest trigger iOS accepts, and it handles a late opt-in on release day.

**Alternatives considered**:
- *A `UNTimeIntervalNotificationTrigger` of `at - now`.* Rejected. It pins the reminder to an absolute instant, so a
  user who changes time zone without opening the app would be notified at 09:00 of the old zone.
- *Calendar components with an explicit time zone.* Rejected for the same reason.

## 3. Knowing that a reminder fired: the armed-reminder ledger

**Problem**: `SyncReleaseNotificationsUseCase` re-arms a game unless `notifiedForDate` equals its resolved date. On
iOS nobody writes `notifiedForDate`. So a sync on release day after 09:00 would compute "now" and post the
reminder a second time.

**Decision**: The iOS scheduler keeps a persistent **ledger**: for each game it handed to the system, the release
date it was armed for. An entry is written only after the system accepted the request, and it is removed when the
request is cancelled or trimmed (§7). The rule that follows from it:

> A game with a ledger entry for release date *D*, and no pending request, **fired** for *D*.

Only the app adds or removes requests: iOS removes pending requests only when the app is uninstalled, and the ledger
goes with it. So a ledger entry with no pending request can only mean the system delivered it. If permission was
revoked meanwhile, the system dropped it, which is the same outcome Android has when its worker posts while
notifications are blocked. The rule is applied in two places:

- **At the start of every full sync**, the scheduler reports each fired (game, *D*) pair. The sync records it with
  `markReleaseNotificationDelivered`, so `notifiedForDate` means the same on both platforms and the existing
  "already notified for this date" branch cancels instead of re-arming.
- **Inside `schedule()`**, for the same game and the same *D*, the scheduler does nothing if the request already
  fired. This closes the race where the request fires between the start of the sync and the `schedule()` call. If
  the sync computed the instant *before* 09:00 and the request fired just after, the re-added calendar trigger is
  for a minute that has passed. A non-repeating calendar trigger in the past never fires, and the next sync finds
  it gone and records it.

A release date that moves after firing, *D1* → *D2*, does not match the ledger's *D1*, so *D2* is armed. That is
the same re-arming Android does through `notifiedForDate`.

**Syncs must not interleave.** `takeFiredReminders` removes the ledger entry *before* the sync writes
`markReleaseNotificationDelivered`. A second sync that runs in between sees neither a ledger entry nor a delivered
date, so it would arm an immediate request for a reminder that already fired, and the user gets it twice. This is
the normal path, not a corner: tapping a reminder launches the app, and the coordinator's launch sync, the
refresh's post-sync and the foreground trigger all run within moments. So `SyncReleaseNotificationsUseCase` holds a
`Mutex` for its whole body, and its Koin binding changes from `factoryOf` to `singleOf`. The binding matters as
much as the lock: a `factory` gives each caller (the toggle use case, the Android worker, the runner, the
coordinator) its own instance and so its own lock. Android is unaffected, because its sync is idempotent. Nested
locking is safe: the sync lock is always taken before the scheduler's.

**Storage**: The ledger lives in the existing iOS settings `DataStore`, as one string-set key
(`release_reminders_armed`, entries `"<gameId>:<releaseDateEpochSeconds>"`). There is no Room change, so no
schema export and no destructive wipe. It holds at most one entry per opted-in game.

**Alternatives considered**:
- *Read the delivered list (`getDeliveredNotifications`).* Rejected as the source of truth: a notification the
  user cleared from the notification centre is no longer in it.
- *A new Room column (`armedForDate`).* Rejected. It changes the entity and schema to record a fact that only the
  iOS scheduler needs.
- *Treat a passed fire instant as delivered.* Rejected. Floating triggers can fire later than the instant computed
  at scheduling time (a user travelling west), so the request may still be pending.
- *Use the same rule on Android.* Rejected. WorkManager can run the job after its instant (Doze), so a sync in
  between would mark a reminder delivered that was never posted.

## 4. When iOS reconciles

On Android the reconciler runs on toggle and after each background refresh, and the fire-time check covers
everything else. iOS has no fire-time check, so it reconciles more often:

| Moment | What runs | Why |
|---|---|---|
| Opt-in toggled (any surface) | `SyncReleaseNotificationsUseCase(gameId)`, as today | Unchanged |
| Process start (`startQuestLog`) | Full sync | Picks up whatever changed while the app was not running |
| App becomes active (`UIApplicationDidBecomeActiveNotification`) | Full sync | Covers a permission granted in iOS Settings (FR-012), a time zone change, and capacity freed under the 64 cap |
| Permission granted by the in-app prompt | Full sync | Reminders that were refused while permission was undecided are armed at once |
| After a successful release-date refresh (background task **or** in-app) | Full sync, inside the run, before `onFinished` | FR-006 on iOS. Running inside the run means the background task does not report "done" until the reminders have moved |
| A saved game leaves the saved set | `SyncReleaseNotificationsUseCase(gameId)` | FR-004. There is no fire-time check to decline it later |

The "leaves the saved set" trigger watches the saved games for removals and does not hook into each use case that
can remove one. "Saved" means in any list, or with a status or a priority (`getSavedGames`). Removing a game from a
list, deleting a list, clearing a status and toggling the wishlist can all unsave it, and hooking each one would
miss the next path anyone adds. Removals only happen in the app, so watching the flow while the process is alive
is enough.

All of this is a single iOS-only coordinator, `ReleaseReminderCoordinator`, in `:core:data` `commonMain` so the
JVM tests reach it. It is bound only by the iOS `dataPlatformModule` and started by `startQuestLog`. It hears
"app became active" and "permission newly granted" through a `ReminderSyncTriggers` port. `:shared` `iosMain`
implements that port, because the permission cache lives in `:core:ui`, which `:core:data` cannot see. This follows the precedent of `BackgroundTaskReleaseRefreshScheduler` (spec 012). The
post-refresh sync goes into `ReleaseRefreshRunner`, which is already iOS-only.

**Alternative considered**: *Call `SyncReleaseNotificationsUseCase` from `RemoveGameFromListUseCase`,
`DeleteListUseCase`, `SetGameStatusUseCase` and `ToggleWishlistUseCase`.* Rejected. It is four edits in shared use
cases, it changes Android for no Android benefit, and it misses the next removal path.

## 5. The contracts change shape

**Decision**: Both reminder contracts become `suspend`, and `schedule` takes the whole reminder:

```kotlin
interface ReleaseNotificationScheduler {
    suspend fun schedule(reminder: ReleaseReminder)   // gameId, gameName, releaseDateEpochSeconds, fireAt
    suspend fun cancel(gameId: Int)
    suspend fun takeFiredReminders(): Map<Int, Long>  // gameId → release date; Android: always empty
}

interface ReleaseNotifier {
    suspend fun notifyReleased(gameId: Int, gameName: String)
}
```

**Rationale**:
- *`suspend`*: Every `UNUserNotificationCenter` call is asynchronous. A non-suspending `schedule` would have to
  launch and forget. Then a background task could report "done" and be suspended before its reminders were
  rescheduled, and two calls for the same game could be applied out of order. Every caller
  (`SyncReleaseNotificationsUseCase`, `DeliverReleaseNotificationUseCase`) is already `suspend`, so the change is
  local.
- *`ReleaseReminder` carries the name and the date*: iOS builds the content and the ledger entry at scheduling time
  (§0, §3). Android ignores both fields: its worker re-reads them at fire time, unchanged.
- *`takeFiredReminders`*: This is how §3's ledger reaches the domain. Android returns an empty map, because its
  worker records delivery itself.
- *`ReleaseNotifier.canDeliver()` is removed*: Nothing calls it, and the UI reads permission through
  `rememberNotificationPermissionState`. Since this feature edits the interface anyway, the dead member goes in the
  same commit and is not left in place.

`ReleaseReminder` is a domain value in `:core:domain/notification/`, one class per file, used only by this
contract.

## 6. Tapping a reminder

**Decision**: A Kotlin `NSObject` that implements `UNUserNotificationCenterDelegateProtocol` lives in `:shared`
`iosMain`. It is set as the centre's delegate in `startQuestLog`, which runs before `didFinishLaunching` returns.
Apple requires this so that the tap which **launched** the app is delivered. The centre holds its delegate weakly,
so the object is held by a top-level property for the life of the process.

- `didReceiveNotificationResponse` reads `gameId` from the request's `userInfo` and publishes it on a process-wide
  `StateFlow<Int?>` (`ReminderDeepLinks`).
- `willPresentNotification` returns banner + list + sound, so a reminder that comes due in the foreground is
  still shown (FR-014).
- `MainViewController` collects that flow and passes it to `QuestLogRoot(pendingDeepLinkGameId, onDeepLinkConsumed)`.
  Android's `MainActivity` does the same with its intent today. `QuestLogRoot`'s existing handling then applies
  unchanged: it pushes `GameDetailRoute`, avoids a duplicate on top, and drops the link while onboarding is the
  first entry.

A tap on a game that has since been unsaved opens its detail screen as it would from search. Detail does not
require a saved game, so nothing is empty and nothing crashes. The game is never removed from IGDB, so "no longer
exists" cannot happen for a valid id. An id that cannot be read is ignored.

The `questlog://` URL scheme is **not** registered on iOS. Its only producer would be this reminder, and the
delegate reads the id directly.

## 7. The 64-pending cap

**Decision**: After each successful add, the scheduler reads the pending reminders. If there are more than 64, it
removes the ones with the latest trigger dates until 64 remain, and drops their ledger entries. They are not
recorded as fired.

**Rationale**: A full sync calls `schedule` for every eligible game, in any order. Trimming the latest after every
add always leaves the 64 soonest, whatever the order: a reminder is only removed when 64 sooner ones are pending.
A trimmed reminder has no ledger entry and no pending request, so the next full sync tries it again. As soon as
something fires or is cancelled, it fits. The comparison is by the next trigger date. A one-second "now" trigger
always sorts first.

The decision about which reminders to keep is a pure function in `commonMain` (`trimToCapacity`), unit-tested on
the JVM. Only the reads and removals touch `UNUserNotificationCenter`.

## 8. Permission on iOS

**Decision**: The iOS `actual` of `rememberNotificationPermissionState` reads `UNNotificationSettings.authorizationStatus`:

| Status | `canDeliver` | `isPermanentlyDenied` | `request()` |
|---|---|---|---|
| `notDetermined` | false | false | `requestAuthorization(alert, sound)`, then `onResult(granted)` |
| `denied` | false | **true** (iOS never asks twice) | Opens `UIApplicationOpenNotificationSettingsURLString` |
| `authorized`, `provisional`, `ephemeral` | true | false | Opens the notification settings (nothing to request) |

`requiresRuntimePermission` is `true`. The status is read asynchronously, so a process-wide holder in `:core:ui`
`iosMain` caches it. `startQuestLog` seeds it, and it is re-read when the app becomes active and after each request.
The composable starts from the cached value and then follows the holder. The only visible effect of a stale first
read is one frame of "blocked" on the very first screen to read it, and the launch-time seed makes even that
practically unreachable.

The iOS `actual` of `NotificationPermissionDeniedDialog` becomes the Android dialog with the iOS settings URL
(`CustomAlertDialog`, the same strings).

**Alternative considered**: *Provisional authorization*, which delivers quietly to the notification centre without
asking. Rejected. Spec 003 shows a release reminder as a deliberate opt-in with a visible prompt, and a quiet
delivery is not the "heads-up on release day" the onboarding page promises.

## 9. The notification text has one source

**Decision**: The title (`%1$s is out today!`) and body (`Tap to see it in your collection.`) move from
`:core:data`'s Android `strings.xml` to `:core:ui`'s Compose resources. Both platforms read them through a small
port, `ReleaseReminderText`, declared in `:core:data` `commonMain` and implemented in `:shared` `commonMain`
with `getString(CoreUiRes.string…)`. Android's channel name and description stay as Android resources: channels
exist only on Android.

**Rationale**: `:core:data` has no Compose and should not gain it to read two strings. `:shared` already depends on
both `:core:ui` and `:core:data`, so no module edge is added. Keeping one copy of the text is the project's rule for
display text. The project is English-only today, so nothing has to be re-translated.

**Alternatives considered**:
- *Compose resources inside `:core:data`.* Rejected. It applies the Compose plugin and runtime to the data layer.
- *An iOS `Localizable.strings` with `localizedUserNotificationString`.* Rejected. It is a second copy of the text,
  outside `strings.xml`.

## 10. The availability flag goes away

**Decision**: Once iOS has reminders, `ReleaseRemindersAvailability` is `true` on every platform, so it is
deleted. The same goes for `StaticReleaseRemindersAvailability`, the `NoOp` scheduler and notifier, the
`RadarWithoutReminders` onboarding page, its string, and the flag's branches in `SettingsViewModel`,
`GameDetailViewModel`, `RadarViewModel` and `OnboardingViewModel`. The tests that set the flag to `false` and
assert that an entry point is absent are removed with the behaviour they covered. Every test of the reminder
behaviour itself stays.

**Rationale**: A flag with one value is dead configuration. The project rule is to delete what a removed gap
leaves behind, not to annotate it. The capability exception register in `specs/010-kmp-migration` loses its
"Release reminders" row, and the roadmap entry is deleted.

**Reversibility**: This is the one step that is not free to undo. If iOS reminders had to be withdrawn later, the
flag would be re-added in five places. That is acceptable: it is mechanical, and the alternative is keeping a
constant around indefinitely. The removal is the plan's last phase, so it can be dropped without touching anything
before it.

## 11. Verification limits

- The JVM host tests reach all the decision logic: the ledger rules (§3), trimming (§7), the coordinator's
  triggers (§4), the changed sync and deliver use cases, and the ViewModels without the flag.
- The `UNUserNotificationCenter` adapter, the delegate and the permission `actual` are thin, and they are verified
  only on the iOS simulator. Local notifications, the permission prompt and taps all work there. Simulator delivery
  is reproducible with a game whose exact release day is today (the one-second trigger) or a near-future date. A
  tap can also be exercised with `xcrun simctl push` and a payload that carries `gameId`.
- None of this needs a physical device, signing or an entitlement: local notifications need neither the push
  capability nor `aps-environment`.
