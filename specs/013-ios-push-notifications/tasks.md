---

description: "Task list for iOS release notifications"
---

# Tasks: iOS Release Notifications

**Input**: Design documents from `/specs/013-ios-push-notifications/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/ios-release-reminders.md, quickstart.md

**Tests**: Included. The constitution (principle V) requires a test in the module's own `androidHostTest` for new
use-case, ViewModel and mapper logic, and the scheduling decisions here (the armed-reminder ledger, the 64 cap, the
sync triggers, the serialized sync) are exactly that logic. They are JUnit4 + MockK + `kotlinx-coroutines-test` with a
`StandardTestDispatcher`. Tests come before the code they cover. The `UserNotifications` adapter, the notification
delegate and the permission `actual` are `iosMain` code, which the JVM cannot reach: they are verified on the
simulator.

**Organization**: Grouped by user story. Tasks marked **(Mac only)** cannot be compiled or run on Windows and must
not be reported as verified unless they were run on a Mac.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependency on an incomplete task)
- **[Story]**: US1–US5, mapping to the user stories in spec.md

## Path Conventions

Short names used below (all under the repository root; `PKG` = `com/nikolasguillen/questlog`):

- `DOMAIN-NOTIF` = `core/domain/src/commonMain/kotlin/PKG/core/domain/notification`
- `DOMAIN-UC` = `core/domain/src/commonMain/kotlin/PKG/core/domain/usecase/notification`
- `DOMAIN-UC-TEST` = `core/domain/src/androidHostTest/kotlin/PKG/core/domain/usecase/notification`
- `DATA-MAIN` = `core/data/src/commonMain/kotlin/PKG/core/data`
- `DATA-ANDROID` = `core/data/src/androidMain/kotlin/PKG/core/data`
- `DATA-IOS` = `core/data/src/iosMain/kotlin/PKG/core/data`
- `DATA-TEST` = `core/data/src/androidHostTest/kotlin/PKG/core/data`
- `UI-IOS` = `core/ui/src/iosMain/kotlin/PKG/core/ui`
- `SHARED-MAIN` = `shared/src/commonMain/kotlin/PKG/shared`
- `SHARED-IOS` = `shared/src/iosMain/kotlin/PKG/shared`

Rules that apply to every task: one top-level `class`/`interface`/`enum`/`data class` per file; new date code uses
`kotlin.time` (`Clock`, `Instant`) and `kotlinx-datetime`; `commonMain` imports no `android.*`/`java.*`;
implementations are `internal` unless Swift or `:shared` must see them; a cross-module `Res` import is aliased
`CoreUiRes`; KDoc and comments are in English; display text is never hardcoded. Android must build and
`./gradlew test` must pass after every task that touches Kotlin: never delete or weaken a test to get there, except
the flag-only tests named in Phase 7, which cover behaviour that no longer exists.

---

## Phase 1: Setup

**Purpose**: Per `CLAUDE.md`, a platform capability is added to `platform-contracts.md` before the code.

- [X] T001 Update `specs/010-kmp-migration/contracts/platform-contracts.md` per `contracts/ios-release-reminders.md` §8: replace the iOS "No-op objects" row of `ReleaseNotificationScheduler` / `ReleaseNotifier` with the real behaviour and record the `suspend` + `ReleaseReminder` + `takeFiredReminders` change; add `LocalNotificationCenter`, `ReminderSyncTriggers` and `ReleaseReminderText` next to `AppRefreshTaskBridge` with the "`commonMain` logic, iOS-only binding" exception note; replace the "Notification permission UI" iOS text ("never composed / inert") with the real `actual`s. Register **every** `iosMain` helper this feature adds under the capability it implements, so no file is left outside the registry: `NotificationAuthorization` + `NotificationAuthorizationStatus` and `OpenNotificationSettings` (under the permission UI), `IosReminderSyncTriggers` (the iOS implementation of `ReminderSyncTriggers`), and `ReminderDeepLinks` + `ReleaseReminderNotificationDelegate` (under the deep-link row, which for iOS reads "delegate → `ReminderDeepLinks` → `QuestLogRoot`, no URL scheme"). Also record that `SyncReleaseNotificationsUseCase` is a single serialized instance. Leave the `ReleaseRemindersAvailability` section and the capability-register row for Phase 7, which deletes them.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: The changed domain contracts, the serialized sync, and the shared reminder text. Android keeps its
behaviour; iOS keeps its no-ops, adapted to the new signatures. Everything after this phase builds on it.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [X] T002 [P] Create `DOMAIN-NOTIF/ReleaseReminder.kt`: `data class ReleaseReminder(val gameId: Int, val gameName: String, val releaseDateEpochSeconds: Long, val fireAt: Instant)` with `kotlin.time.Instant`. KDoc: the reminder the reconciler hands to a platform scheduler; `gameName` is data-source text (so `String`, not `UiText`); `fireAt` is never `null` here, because a `null` instant means `cancel`; Android ignores `gameName` and `releaseDateEpochSeconds`.
- [X] T003 [P] Edit `DOMAIN-NOTIF/ReleaseNotifier.kt`: make `notifyReleased(gameId: Int, gameName: String)` `suspend` and delete `canDeliver()` (no caller exists: `grep -rn "canDeliver()" --include='*.kt' .` finds only the two implementations). Update the KDoc: only a platform whose reminders run app code at fire time calls it.
- [X] T004 Edit `DOMAIN-NOTIF/ReleaseNotificationScheduler.kt` (after T002): `suspend fun schedule(reminder: ReleaseReminder)`, `suspend fun cancel(gameId: Int)`, and new `suspend fun takeFiredReminders(): Map<Int, Long>`. KDoc as in `contracts/ios-release-reminders.md` §1: gameId → the release date each fired reminder was armed for, each reported once; Android returns an empty map because its worker records delivery itself. Fix the stale "WorkManager-backed implementation" wording so the contract is platform-neutral.
- [X] T005 [P] Edit `DOMAIN-UC-TEST/SyncReleaseNotificationsUseCaseTest.kt` (RED first): convert the `scheduler` stubs and verifications to `coEvery`/`coVerify`, expect `schedule(ReleaseReminder(id, game.name, <resolved date>, <instant>))`, and stub `takeFiredReminders()` to `emptyMap()` by default. Add: (a) a fired `(id → date)` for an opted-in game is passed to `repository.markReleaseNotificationDelivered(id, date)` **before** the loop, so the same date is then cancelled instead of re-armed; (b) a fired entry for a game that is no longer opted in is not marked; (c) a fired date `D1` does not stop the game being armed when its release date has moved to `D2`; (d) `takeFiredReminders()` is called once per full sync and once per single-game sync; (e) **serialization**: with a `schedule` that suspends until released, a second overlapping `invoke` does not call `takeFiredReminders()` until the first has finished, and when the first consumed a fired `(id → date)`, the second does not call `schedule` for that date.
- [X] T006 [P] Edit `DOMAIN-UC-TEST/DeliverReleaseNotificationUseCaseTest.kt`: convert the `notifier` stubs and verifications to `coEvery`/`coVerify`. No new cases; the use case's rules are unchanged.
- [X] T007 Edit `DOMAIN-UC/SyncReleaseNotificationsUseCase.kt` (after T004, T005): call `scheduler.takeFiredReminders()` first and, for each `(id, date)` whose id is still in the opted-in set, call `repository.markReleaseNotificationDelivered(id, date)`; then run the existing loop, building `ReleaseReminder(id, game.name, earliestDate.date, instant)` for `scheduler.schedule`. Keep the early return for an empty scope **after** the fired-reminder step. Wrap the whole body in a `Mutex` field's `withLock`, so two syncs never interleave "take fired reminders" with "read the delivered date": without it a launch from a tapped reminder (coordinator sync, refresh sync and the foreground trigger within moments) can re-arm a reminder that already fired. KDoc: why the lock exists, and that it only serializes within one instance (T008). Update the rest of the KDoc: "single function that decides what is scheduled" still holds; remove the WorkManager-specific wording. `DeliverReleaseNotificationUseCase.kt` needs no source change (the `suspend` call sits in a `suspend` function); confirm with the T006 compile.
- [X] T008 Edit `core/domain/src/commonMain/kotlin/PKG/core/domain/di/DomainKoin.kt` (after T007): change `factoryOf(::SyncReleaseNotificationsUseCase)` to `singleOf(::SyncReleaseNotificationsUseCase)` (add the `singleOf` import if missing). A `factory` hands every caller (`SetReleaseNotificationEnabledUseCase`, `ReleaseDatesRefreshWorker`, `ReleaseRefreshRunner`, the iOS coordinator) its own instance, and each would have its own `Mutex`, which serializes nothing. Add a comment saying so beside the binding.
- [X] T009 [P] Edit `core/ui/src/commonMain/composeResources/values/strings.xml`: add `release_notification_title_format` = `%1$s is out today!` and `release_notification_body` = `Tap to see it in your collection.` (the exact text of `core/data/src/androidMain/res/values/strings.xml`; positional `%1$s` as Compose resources require). Place them beside the other notification strings.
- [X] T010 [P] Create `DATA-MAIN/notification/ReleaseReminderText.kt`: public `interface ReleaseReminderText { suspend fun title(gameName: String): String; suspend fun body(): String }`. KDoc: the reminder's text, supplied by `:shared` from `:core:ui`'s resources so `:core:data` needs no Compose; public because `:shared` implements it.
- [X] T011 Create `SHARED-MAIN/ReleaseReminderTextImpl.kt` (after T009, T010): `internal class ReleaseReminderTextImpl : ReleaseReminderText` using `getString(CoreUiRes.string.release_notification_title_format, gameName)` and `getString(CoreUiRes.string.release_notification_body)`, with `import com.nikolasguillen.questlog.core.ui.resources.Res as CoreUiRes` and one import per string. Bind it in `sharedModule` in `SHARED-MAIN/di/SharedKoin.kt`: `single<ReleaseReminderText> { ReleaseReminderTextImpl() }`.
- [X] T012 Edit `DATA-ANDROID/notification/ReleaseNotifierImpl.kt` (after T003, T010): take `ReleaseReminderText` as a second constructor parameter, make `notifyReleased` `suspend`, build the title and body from it instead of `R.string.release_notification_*`, and delete `canDeliver()`. Keep the channel, the deep link intent, the `gameId` notification id and the small icon. Then delete `release_notification_title_format` and `release_notification_body` from `core/data/src/androidMain/res/values/strings.xml` (the channel name and description stay). `DataPlatformModule.android.kt` needs no edit: `singleOf(::ReleaseNotifierImpl)` resolves the new parameter.
- [X] T013 Edit `DATA-ANDROID/scheduler/ReleaseNotificationSchedulerImpl.kt` (after T004): `override suspend fun schedule(reminder: ReleaseReminder)` using `reminder.gameId` and `reminder.fireAt` (ignore the other two fields), `override suspend fun cancel(gameId: Int)`, and `override suspend fun takeFiredReminders(): Map<Int, Long> = emptyMap()`. Behaviour (`REPLACE`, unique work name, delay) is unchanged.
- [X] T014 [P] Edit `DATA-MAIN/notification/NoOpReleaseNotificationScheduler.kt` and `NoOpReleaseNotifier.kt` to the new signatures (`suspend`, `ReleaseReminder`, `takeFiredReminders() = emptyMap()`, no `canDeliver`). The scheduler no-op is deleted in T035 once iOS has the real one; the notifier no-op stays bound on iOS until T063.
- [X] T015 Run `./gradlew :core:domain:testAndroidHostTest :core:data:testAndroidHostTest --console=plain -q`, then `./gradlew :androidApp:assembleDebug` and `./gradlew test`, and confirm all pass, including `KoinGraphTest` (it must find `ReleaseReminderText` through `sharedModule`) and the tests that build `SyncReleaseNotificationsUseCase` (`SetReleaseNotificationEnabledUseCaseTest`, `ReleaseDatesRefreshWorkerTest`).
- [ ] T016 Manual Android check: on a device or emulator, opt in to a saved game whose exact release day is today, **swipe the app away**, and wait for the reminder. Confirm the title "<Game> is out today!" and the body arrive and that tapping opens the game. This proves the Compose-resource `getString` call works from a worker in a fresh process.

**Checkpoint**: Android behaves exactly as before, on the new contracts. User stories can begin.

---

## Phase 3: User Story 1 - Opt in to a release reminder on iOS (Priority: P1) 🎯 MVP

**Goal**: On iOS, "Notify me" schedules a real local notification that is delivered on the release day, after the
system permission prompt, without duplicates. The reminder is armed as soon as the user taps Allow, with no
relaunch: the first opt-in is made while permission is still undecided, so the scheduler must be told when it is
granted.

**Independent Test**: [quickstart.md](quickstart.md) §4 steps 2–4: on the simulator, opt in to a game releasing
today, allow the prompt, and a notification naming the game arrives once; a future date has a 09:00 floating pending
request.

### Tests for User Story 1 ⚠️ write first, confirm they fail

- [ ] T017 [P] [US1] Create `DATA-TEST/notification/FakeLocalNotificationCenter.kt`: an in-memory `LocalNotificationCenter` with `var accepting = true`, a pending map keyed by `gameId` holding the request, `val added: MutableList<LocalReminderRequest>`, and a `fun fire(gameId: Int)` that removes a pending entry as the system would. `pendingReminders()` derives `nextTriggerAt` from the request (`null` for an immediate request).
- [ ] T018 [P] [US1] Create `DATA-TEST/notification/ArmedReminderLedgerTest.kt` using `InMemoryPreferencesDataStore` from `DATA-TEST/scheduler/`: `put` stores `"<gameId>:<releaseDateEpochSeconds>"` under the key `release_reminders_armed`; a second `put` for the same game replaces its date (at most one entry per game); `remove` drops the given ids; `entries()` returns `Map<Int, Long>`; a malformed stored string is ignored, not thrown.
- [ ] T019 [US1] Create `DATA-TEST/notification/LocalNotificationReleaseSchedulerTest.kt` with `FakeLocalNotificationCenter`, `InMemoryPreferencesDataStore`, `FakeClock` and a mocked `ReleaseReminderText`, and a fixed `TimeZone` supplier. Cover: (a) a future `fireAt` becomes a request whose `fireAt` is the `LocalDateTime` of that instant in the supplied zone, at 09:00, and whose title and body come from the text port; (b) `fireAt <= now` becomes a request with `fireAt = null` (immediate); (c) a ledger entry `gameId:date` is written only after the centre accepted; (d) a refused `add` writes no entry and removes any earlier entry for that game; (e) `schedule` for the same game and date, with a ledger entry and **no** pending request, adds nothing (it fired); (f) `schedule` for the same game with a **different** date arms it and replaces the entry; (g) `cancel` removes the request and the entry; (h) `takeFiredReminders` returns exactly the ledger entries with no pending request, removes them from the ledger, and returns them only once; (i) two overlapping `schedule` calls for one game apply in call order.
- [ ] T020 [P] [US1] Create `DATA-TEST/notification/ReleaseReminderCoordinatorTest.kt` with a `MutableSharedFlow<Unit>` for triggers, a `MutableStateFlow<Set<Int>>` for saved ids and a mocked `SyncReleaseNotificationsUseCase`: `start()` runs one full sync (`invoke(null)`) at once; each trigger event runs another (this is what arms the first reminder after the user taps Allow); the first emission of saved ids is a baseline and syncs nothing; an id that disappears from the set runs `invoke(id)` once; an id that reappears syncs nothing; trigger events arriving **during** a running sync produce exactly one follow-up full sync; `start()` called twice does not double the collectors; an exception from the sync does not stop later syncs, and `CancellationException` propagates.

### Implementation for User Story 1

- [ ] T021 [P] [US1] Create `DATA-MAIN/notification/LocalReminderRequest.kt`: `internal data class LocalReminderRequest(val gameId: Int, val title: String, val body: String, val fireAt: LocalDateTime?)` with KDoc: `null` means "fire in one second", otherwise floating calendar components (no time zone attached).
- [ ] T022 [P] [US1] Create `DATA-MAIN/notification/PendingReminder.kt`: `internal data class PendingReminder(val gameId: Int, val nextTriggerAt: Instant?)` (`null` for an immediate trigger).
- [ ] T023 [US1] Create `DATA-MAIN/notification/LocalNotificationCenter.kt` (after T021, T022): `internal interface LocalNotificationCenter` with `suspend fun add(request: LocalReminderRequest): Boolean`, `suspend fun pendingReminders(): List<PendingReminder>`, `suspend fun remove(gameIds: Collection<Int>)`. Use the `add` KDoc from `contracts/ios-release-reminders.md` §2 (false when not authorized or rejected; never throws except `CancellationException`).
- [ ] T024 [US1] Create `DATA-MAIN/notification/ArmedReminderLedger.kt` (makes T018 pass): `internal class ArmedReminderLedger(private val dataStore: DataStore<Preferences>)` over a `stringSetPreferencesKey("release_reminders_armed")`. Entry format `"<gameId>:<releaseDateEpochSeconds>"`, at most one per `gameId`. Members: `suspend fun entries(): Map<Int, Long>`, `suspend fun put(gameId: Int, releaseDateEpochSeconds: Long)` (replaces any earlier date for that game), `suspend fun remove(gameIds: Collection<Int>)`. A malformed entry is skipped. KDoc: only the iOS scheduler reads or writes it.
- [ ] T025 [US1] Create `DATA-MAIN/notification/LocalNotificationReleaseScheduler.kt` (after T023, T024, T010; makes T019 pass): `internal class LocalNotificationReleaseScheduler(center: LocalNotificationCenter, text: ReleaseReminderText, ledger: ArmedReminderLedger, clock: Clock, timeZone: () -> TimeZone = { TimeZone.currentSystemDefault() }) : ReleaseNotificationScheduler`. Every call runs inside one `Mutex.withLock`. `schedule` follows `contracts/ios-release-reminders.md` §3 steps 1–4 (the "already fired" no-op, build the request with `fireAt = null` when `reminder.fireAt <= clock.now()`, `add`, write or clear the ledger entry). `cancel` removes the request and the ledger entry. `takeFiredReminders` is the ledger entries whose game is not pending, removed as returned. Rethrow `CancellationException` before any other handling. Leave step 5 (trim) for T043.
- [ ] T026 [P] [US1] Create `DATA-MAIN/notification/ReminderSyncTriggers.kt`: public `interface ReminderSyncTriggers { val events: Flow<Unit> }`. KDoc: what tells the iOS coordinator to resync (app became active, permission newly granted); implemented in `:shared` because the permission cache lives in `:core:ui`.
- [ ] T027 [US1] Create `DATA-MAIN/notification/ReleaseReminderCoordinator.kt` (after T026; makes T020 pass): `class ReleaseReminderCoordinator internal constructor(syncReleaseNotifications: SyncReleaseNotificationsUseCase, getSavedGameIds: () -> Flow<Set<Int>>, triggers: ReminderSyncTriggers, scope: CoroutineScope)`, public because `startQuestLog` calls `start()`. `start()` is idempotent: one full sync now, a full sync per trigger event, and `sync(id)` for each id that leaves the saved set (the first emission is the baseline). Use a conflated channel or `conflate()` so a trigger arriving during a sync yields one follow-up sync. Catch non-cancellation exceptions from a sync and log them. Public with an `internal` constructor, as noted in the contract.
- [ ] T028 [US1] **(Mac only)** Create `DATA-IOS/notification/UserNotificationCenter.kt`: `internal class UserNotificationCenter : LocalNotificationCenter` over `UNUserNotificationCenter.currentNotificationCenter()`, bridging each completion handler with `suspendCancellableCoroutine`. `add` first reads `getNotificationSettingsWithCompletionHandler` and returns `false` unless the status is authorized, provisional or ephemeral (the system accepts a request it will never deliver); then builds a `UNMutableNotificationContent` (title, body, `UNNotificationSound.defaultSound`, `userInfo = mapOf("gameId" to gameId)`); trigger is a non-repeating `UNCalendarNotificationTrigger` from `NSDateComponents` with year, month, day, hour and minute set and **no time zone** (floating), or a `UNTimeIntervalNotificationTrigger` of 1 s when `fireAt` is `null`; identifier `release-reminder-<gameId>`. `pendingReminders` lists pending requests with that identifier prefix and maps `trigger.nextTriggerDate()` to an `Instant`. `remove` calls `removePendingNotificationRequestsWithIdentifiers`. Any error other than cancellation returns `false` / an empty list and is logged. Opt in to `BetaInteropApi` only if the compiler asks.
- [ ] T029 [P] [US1] **(Mac only)** Create `UI-IOS/util/NotificationAuthorizationStatus.kt`: `enum class NotificationAuthorizationStatus { NOT_DETERMINED, DENIED, AUTHORIZED }` (public; `:shared` observes it).
- [ ] T030 [US1] **(Mac only)** Create `UI-IOS/util/NotificationAuthorization.kt` (after T029): a public `object` with `val status: StateFlow<NotificationAuthorizationStatus>` (initially `NOT_DETERMINED`), `suspend fun refresh()` (reads `UNNotificationSettings.authorizationStatus`: `notDetermined` → `NOT_DETERMINED`, `denied` → `DENIED`, `authorized`/`provisional`/`ephemeral` → `AUTHORIZED`), and `suspend fun request(): Boolean` (calls `requestAuthorizationWithOptions(alert | sound)`, then `refresh()`, returns whether it was granted). Bridge handlers with `suspendCancellableCoroutine`.
- [ ] T031 [P] [US1] **(Mac only)** Create `UI-IOS/util/OpenNotificationSettings.kt`: `internal fun openAppNotificationSettings()` opening `UIApplicationOpenNotificationSettingsURLString` through `UIApplication.sharedApplication.openURL(...)` (iOS 16+, matching the deployment target).
- [ ] T032 [US1] **(Mac only)** Replace `UI-IOS/util/NotificationPermission.ios.kt` (after T030, T031): the real `actual` per `research.md` §8. `canDeliver = status == AUTHORIZED`; `isPermanentlyDenied = status == DENIED`; `requiresRuntimePermission = true`; `request` launches in `rememberCoroutineScope()`: `request()` the system prompt when `NOT_DETERMINED`, otherwise `openAppNotificationSettings()`; call `onResult(granted)` after a prompt. Refresh the cache in a `LaunchedEffect(Unit)` and on lifecycle `ON_RESUME` (use `LocalLifecycleOwner` as `NotificationPermission.android.kt` does). Replace the "never read on iOS" KDoc.
- [ ] T033 [US1] **(Mac only)** Replace `UI-IOS/component/NotificationPermissionDeniedDialog.ios.kt` (after T031): `CustomAlertDialog` with `notification_permission_denied_title/message/action` and `cancel`, confirm calling `openAppNotificationSettings()` then `onDismiss()`, mirroring `NotificationPermissionDeniedDialog.android.kt`. This module's own `Res` is imported bare. Replace the "never composed on iOS" KDoc.
- [ ] T034 [US1] **(Mac only)** Create `SHARED-IOS/IosReminderSyncTriggers.kt` (after T026, T030): `internal class IosReminderSyncTriggers : ReminderSyncTriggers` whose `events` merges two sources: a `callbackFlow` over `NSNotificationCenter.defaultCenter` for `UIApplicationDidBecomeActiveNotification` (observer removed in `awaitClose`), refreshing `NotificationAuthorization` before each emission; and `NotificationAuthorization.status` changing from anything else to `AUTHORIZED` (FR-012, and the arming of the first opt-in after the user taps Allow). Duplicate events are harmless: the coordinator coalesces them.
- [ ] T035 [US1] Edit `DATA-IOS/di/DataPlatformModule.ios.kt` (after T025, T027, T028): bind `UserNotificationCenter` as `LocalNotificationCenter`, `ArmedReminderLedger(get())` (the settings `DataStore`), `LocalNotificationReleaseScheduler(center = get(), text = get(), ledger = get(), clock = Clock.System)` as `ReleaseNotificationScheduler`, and `ReleaseReminderCoordinator(syncReleaseNotifications = get(), getSavedGameIds = { get<GameRepository>().getSavedGames().map { games -> games.map { it.id }.toSet() } }, triggers = get(), scope = get())` as a `single`; delete the `NoOpReleaseNotificationScheduler` binding and its import, and delete `NoOpReleaseNotificationScheduler.kt`. Flip the flag: `StaticReleaseRemindersAvailability(isAvailable = true)`, and replace the "Release reminders are not on iOS yet" comment (the `NoOpReleaseNotifier` binding stays until T063).
- [ ] T036 [US1] **(Mac only)** Edit `startQuestLog` in `SHARED-IOS/QuestLogIos.kt` (after T034, T035): add `single<ReminderSyncTriggers> { IosReminderSyncTriggers() }` to the module it already passes to `initKoin`; after Koin starts, launch `NotificationAuthorization.refresh()` in `koin.get<CoroutineScope>()` so the permission state is known before the first screen reads it; and call `koin.get<ReleaseReminderCoordinator>().start()` after `schedulePeriodicRefresh()`. Update the KDoc to say it seeds the permission state and starts the reminder coordinator.
- [ ] T037 [US1] Run `./gradlew :core:data:testAndroidHostTest --console=plain -q` (T018, T019 and T020 now pass), `./gradlew :androidApp:assembleDebug` and `./gradlew test`. **(Mac only)** also `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64 -q` and the `xcodebuild` command from `CLAUDE.md`.
- [ ] T038 [US1] **(Mac only)** Simulator walkthrough, [quickstart.md](quickstart.md) §4 steps 2–4: with permission **not yet decided**, opt in to a game releasing today, tap Allow on the prompt, and confirm the notification arrives **without relaunching the app**; confirm a future game has a 09:00 pending request with no time zone; confirm no second notification after backgrounding and returning twice. Also deny the prompt once and confirm the denied dialog appears and the opt-in is not shown as an armed reminder.

**Checkpoint**: User Story 1 works end to end on iOS, including the first opt-in. The reminder set is also reconciled at launch and on foreground (the coordinator), but it does not yet follow a refresh, honour the 64 cap or open the game on tap.

---

## Phase 4: User Story 2 - The reminder follows a shifted release date (Priority: P1)

**Goal**: The pending set stays correct without app code at fire time: dates that move after a refresh, and the 64
cap. (Unsaved games, launches and foreground returns are reconciled by the coordinator delivered in US1.)

**Independent Test**: [quickstart.md](quickstart.md) §4 steps 5 and 8, plus the cap scenario in §2: after a refresh
moves a date the pending trigger is for the new day; unsaving removes the pending request at once; 70 opt-ins
leave the 64 soonest pending.

### Tests for User Story 2 ⚠️ write first, confirm they fail

- [ ] T039 [P] [US2] Create `DATA-TEST/notification/TrimToCapacityTest.kt`: fewer than `capacity` returns nothing; exactly `capacity` returns nothing; 70 reminders in shuffled order return the 6 latest ids; an immediate (`null`) trigger sorts first and is never trimmed while others are later; equal dates tie-break by `gameId`.
- [ ] T040 [US2] Extend `DATA-TEST/notification/LocalNotificationReleaseSchedulerTest.kt` (T019): scheduling 70 reminders in random order leaves the 64 soonest pending, and the 6 trimmed have no ledger entry and are **not** reported by `takeFiredReminders`; a trimmed game is armed again by a later `schedule` once capacity exists.
- [ ] T041 [US2] Edit `DATA-TEST/scheduler/ReleaseRefreshRunnerTest.kt` and `DATA-TEST/scheduler/BackgroundTaskReleaseRefreshSchedulerTest.kt` (RED first): both build the runner, so add the new `syncReleaseNotifications` parameter (a mocked `SyncReleaseNotificationsUseCase`) to their builders. Add to the runner test: a `REFRESHED` outcome runs one full sync after the timestamp is recorded and before `run()` returns; `NOT_DUE` and `FAILED` run none; a sync that throws leaves the outcome `REFRESHED` and the timestamp recorded; a joiner waits for the sync; cancelling the refresh before the sync starts runs none.

### Implementation for User Story 2

- [ ] T042 [P] [US2] Create `DATA-MAIN/notification/TrimToCapacity.kt` (makes T039 pass): `internal fun trimToCapacity(pending: List<PendingReminder>, capacity: Int): List<Int>` returning the ids beyond the `capacity` soonest, ordered by `nextTriggerAt` with `null` first, then by `gameId`. Add `internal const val MAX_PENDING_REMINDERS = 64` with a KDoc naming the iOS limit.
- [ ] T043 [US2] Edit `DATA-MAIN/notification/LocalNotificationReleaseScheduler.kt` (after T025, T042; makes T040 pass): add step 5 of `contracts/ios-release-reminders.md` §3 — after a successful `add`, `trimToCapacity(center.pendingReminders(), MAX_PENDING_REMINDERS)`, then remove those ids from the centre and the ledger.
- [ ] T044 [US2] Edit `DATA-MAIN/scheduler/ReleaseRefreshRunner.kt` (after T041): add `syncReleaseNotifications: SyncReleaseNotificationsUseCase` as a constructor parameter and, in `refresh()`, call it right after `recordRefresh()` on the success path; catch everything but `CancellationException` so a failed sync does not turn a refresh into `FAILED`. Update the class KDoc: the sync runs inside the refresh so joiners, and the background task, wait for it.
- [ ] T045 [US2] Edit `DATA-IOS/di/DataPlatformModule.ios.kt` (after T035, T044): pass `syncReleaseNotifications = get()` to `ReleaseRefreshRunner`.
- [ ] T046 [US2] Run `./gradlew :core:data:testAndroidHostTest --console=plain -q`, `./gradlew :androidApp:assembleDebug` and `./gradlew test`. **(Mac only)** also the iOS framework link.
- [ ] T047 [US2] **(Mac only)** Simulator walkthrough, [quickstart.md](quickstart.md) §4 steps 5 and 8: unsave an opted-in game and confirm its pending request disappears at once; run a refresh that moves a date (`ReleaseRefreshTask.debugRun()` from LLDB, as in spec 012) and confirm the trigger moves; relaunch from a tapped, already-fired reminder and foreground the app and confirm no duplicate and no lost request.

**Checkpoint**: User Stories 1 and 2 both work. The reminder set follows the data.

---

## Phase 5: User Story 3 - Tapping a reminder opens the game (Priority: P2)

**Goal**: A tap on a release notification opens that game's detail screen from a closed, backgrounded or foreground
app, and a reminder due while the app is open is still shown.

**Independent Test**: [quickstart.md](quickstart.md) §4 step 6.

- [ ] T048 [P] [US3] Create `SHARED-IOS/ReminderDeepLinks.kt`: `internal object ReminderDeepLinks { val pending: StateFlow<Int?>; fun emit(gameId: Int); fun consume() }` over a `MutableStateFlow<Int?>`. KDoc: the game a tapped release notification asked to open, handed to `QuestLogRoot` the way `MainActivity` hands over its intent's id; process-wide because the tap can arrive before any scene exists.
- [ ] T049 [US3] **(Mac only)** Create `SHARED-IOS/ReleaseReminderNotificationDelegate.kt` (after T048): `internal class ReleaseReminderNotificationDelegate : NSObject(), UNUserNotificationCenterDelegateProtocol`. `willPresentNotification` calls the completion handler with banner, list and sound options (FR-014). `didReceiveNotificationResponse` reads `gameId` from `response.notification.request.content.userInfo` (an `NSNumber`; an absent or unreadable value is ignored), calls `ReminderDeepLinks.emit(gameId)`, then the completion handler. No game lookup: an unsaved game opens its detail screen like any other.
- [ ] T050 [US3] **(Mac only)** Edit `startQuestLog` in `SHARED-IOS/QuestLogIos.kt` (after T036, T049): set `UNUserNotificationCenter.currentNotificationCenter().delegate` to the delegate **before anything that can return early**, holding the instance in a file-level `private val` because the centre keeps its delegate weakly. It must be set before `didFinishLaunching` returns so the tap that launched the app is delivered. Update the KDoc.
- [ ] T051 [US3] **(Mac only)** Edit `SHARED-IOS/MainViewController.kt`: collect `ReminderDeepLinks.pending` with `collectAsStateWithLifecycle()` and pass it as `pendingDeepLinkGameId`, with `onDeepLinkConsumed = ReminderDeepLinks::consume`. Replace the comment saying iOS has no reminder. `QuestLogRoot` is unchanged.
- [ ] T052 [US3] **(Mac only)** Simulator walkthrough, [quickstart.md](quickstart.md) §4 step 6: tap from the lock screen / notification centre with the app killed and with it backgrounded; confirm a banner appears with the app in the foreground; send `xcrun simctl push` with a `gameId` payload for a repeatable tap; confirm a tap for an unsaved game opens its detail screen without crashing; confirm a tap during the welcome flow is dropped by `QuestLogRoot` rather than queued.

**Checkpoint**: User Stories 1–3 work independently.

---

## Phase 6: User Story 4 - Manage reminders and fix permission from Settings (Priority: P2)

**Goal**: Settings shows every saved game with its toggle and the true permission state, and reminders resume when
permission is granted again.

**Independent Test**: [quickstart.md](quickstart.md) §4 step 7.

**Note**: This story needs no new code of its own. The Settings screens already exist and become live when the flag
flips (T035), the permission `actual` is T032, and the "permission newly granted" resync is part of US1 (T034). This
phase checks that they hold together.

- [ ] T053 [US4] Review, without changing unless a defect shows, the screens that already consume `rememberNotificationPermissionState()` on iOS: `feature/settings/.../SettingsScreen.kt`, `ReleaseNotificationsScreen.kt`, `feature/radar/.../RadarScreen.kt` and `feature/game-detail/.../GameDetailScreen.kt`. Confirm that `isPermanentlyDenied = true` (iOS's only denied state) routes to the denied dialog or settings and that the "request" path is not blocked by the Android-only `shouldShowRationale` assumption. Record any change needed as a new task here before making it.
- [ ] T054 [US4] **(Mac only)** Simulator walkthrough, [quickstart.md](quickstart.md) §4 step 7: deny in iOS Settings, confirm Settings → Release reminders shows the blocked row and its action opens the app's notification settings; opt in to a game while blocked and confirm it is not shown as armed; allow again, return to the app, and confirm that game now has a pending request without re-opting in; confirm every saved game is listed and a flipped toggle does not remove its row.

**Checkpoint**: User Stories 1–4 work.

---

## Phase 7: User Story 5 - The welcome flow stops saying reminders are unavailable (Priority: P3)

**Goal**: With reminders on every platform, the availability flag and the "unavailable" page are deleted, not left
as a constant.

**Independent Test**: [quickstart.md](quickstart.md) §4 step 1; and `grep -rn "RemindersAvailab\|RadarWithoutReminders" --include='*.kt' --include='*.xml' .` finds nothing outside `build/`.

**Note**: The flag is already `true` on iOS after T035, so the page is no longer shown there. This phase removes the
dead configuration. It is separable: dropping it leaves a working feature with a constant `true` flag.

### Tests for User Story 5 ⚠️ update first

- [ ] T055 [P] [US5] Edit `feature/settings/src/androidHostTest/kotlin/PKG/feature/settings/SettingsViewModelTest.kt`: drop the `ReleaseRemindersAvailability` mock and the `remindersAvailable` parameter from `viewModel(...)`, and delete the two tests that exist only for the flag (the `releaseRemindersAvailable` true/false pair near lines 390–399). Keep every other test.
- [ ] T056 [P] [US5] Edit `feature/game-detail/src/androidHostTest/kotlin/PKG/feature/gamedetail/GameDetailViewModelTest.kt`: drop the mock and the `remindersAvailable` parameters of `gameDetailViewModel` / `createViewModel`, and delete the two `remindersAvailable = false` tests (near lines 476 and 484). `GameDetailUiMapperTest` and the `isNotificationAvailable surfaces in uiState` test stay: that field also reflects "saved and not yet released".
- [ ] T057 [P] [US5] Edit `feature/radar/src/androidHostTest/kotlin/PKG/feature/radar/RadarViewModelTest.kt`: drop the mock and the parameters, and delete the two `remindersAvailable = false` tests (near lines 196 and 204).
- [ ] T058 [P] [US5] Edit `feature/onboarding/src/androidHostTest/kotlin/PKG/feature/onboarding/OnboardingViewModelTest.kt`: drop the mock and the parameter of `createViewModel`, and delete the test that maps `Radar` to `RadarWithoutReminders` (near line 341). Keep the pages test that covers `Reminders` appearing only when a runtime permission is needed and not granted.

### Implementation for User Story 5

- [ ] T059 [US5] Edit `feature/settings/.../SettingsViewModel.kt` (constructor parameter `releaseRemindersAvailability`, line 44 usage), `model/SettingsUiState.kt` (`releaseRemindersAvailable`) and `SettingsScreen.kt` (the `if (state.releaseRemindersAvailable)` guard around the notifications group): remove the flag, keep the group unconditional.
- [ ] T060 [US5] Edit `feature/game-detail/.../GameDetailViewModel.kt`: remove the constructor parameter, the `remindersAvailable` field, the `.let { ... copy(isNotificationAvailable = false) }` at line 104 and the early return in `toggleReleaseNotification`. Leave `isNotificationAvailable` itself: `GameDetailUiMapper` still sets it.
- [ ] T061 [US5] Edit `feature/radar/.../RadarViewModel.kt`, `model/RadarUiState.kt` and `RadarScreen.kt`: remove the constructor parameter, `releaseRemindersAvailable` (state, initial value, and the `showNotificationToggle = state.releaseRemindersAvailable && ...` condition keeps only its remaining operands) and the early return in `toggleReleaseNotification`.
- [ ] T062 [US5] Edit `feature/onboarding`: `OnboardingViewModel.kt` (constructor parameter, `remindersAvailable =` argument), `mapper/OnboardingPageUiMapper.kt` (drop `remindersAvailable` from `buildOnboardingPages`, always add `OnboardingPage.Radar`, keep `if (requiresRuntimePermission && !canDeliver) add(OnboardingPage.Reminders)`, delete the `RadarWithoutReminders` branch and its import), `model/OnboardingPage.kt` (delete `RadarWithoutReminders`), and `composeResources/values/strings.xml` (delete `onboarding_radar_without_reminders_body`). Check `OnboardingScreen.kt` for any `when` over the pages and fix it.
- [ ] T063 [US5] Delete `DOMAIN-NOTIF/ReleaseRemindersAvailability.kt`, `DATA-MAIN/notification/StaticReleaseRemindersAvailability.kt`, `DATA-MAIN/notification/NoOpReleaseNotifier.kt`; remove the `ReleaseRemindersAvailability` binding and imports from `DataPlatformModule.android.kt` and `DataPlatformModule.ios.kt`, and the `NoOpReleaseNotifier` binding from the iOS module. Fix the `DataKoin.kt` KDoc that lists the availability binding. Before deleting the notifier binding, run `grep -rn "DeliverReleaseNotificationUseCase" --include='*.kt' . | grep -v /build/` and confirm the only resolver is `ReleaseNotificationWorker` (Android); nothing on iOS may resolve it.
- [ ] T064 [US5] Run `./gradlew :androidApp:assembleDebug` and `./gradlew test` (all four ViewModel suites and `KoinGraphTest` green). **(Mac only)** also the iOS framework link and the `xcodebuild` command.
- [ ] T065 [US5] **(Mac only)** Simulator walkthrough, [quickstart.md](quickstart.md) §4 step 1: reset the welcome flow, walk through it, confirm no page says reminders are unavailable and the reminders page appears with its "Allow notifications" action.

**Checkpoint**: All five user stories are complete.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Documentation that still says iOS has no reminders, and the final verification.

- [ ] T066 [P] Edit `specs/010-kmp-migration/contracts/platform-contracts.md` (after T063): delete the `ReleaseRemindersAvailability` section and the "Release reminders" row of the capability exception register, and remove the "Consumers" paragraph that named the four ViewModels.
- [ ] T067 [P] Edit `docs/roadmap.md`: delete the "Release reminders on iOS" bullet and the `RadarWithoutReminders` sentence in the "iOS follow-ups" section, and reword the section's opening line ("ships iOS without release reminders") so it only describes what is still open (iOS distribution). Delete resolved entries rather than annotating them.
- [ ] T068 [P] Edit `CLAUDE.md` (the "Working in a multiplatform codebase" paragraph that says iOS ships "**without** release reminders") and `core/data/CLAUDE.md` (the sentences listing "no-op reminder classes, `StaticReleaseRemindersAvailability(false)`" and "no WorkManager and no reminders", and the `StaticReleaseRemindersAvailability` mention in the first paragraph) to describe the iOS reminder pieces: `LocalNotificationReleaseScheduler`, `ReleaseReminderCoordinator`, the ledger, and the `UserNotifications` adapter. Check `core/ui/CLAUDE.md` for text about the iOS permission `actual`s being inert.
- [ ] T069 [P] Edit `docs/tech-debt.md` only if a deviation was introduced (for example a `@Suppress` or a documented limit of the 64 cap); otherwise leave it.
- [ ] T070 Run the full local check: `./gradlew :androidApp:assembleDebug`, `./gradlew test`, and **(Mac only)** `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64 -q` and the `xcodebuild` command. Report which of them were run on a Mac and which were not.
- [ ] T071 Run [quickstart.md](quickstart.md) end to end (§1–§5) and note any step not run. Then amend the constitution's KMP paragraph (iOS "ships without release reminders and on-device translation") with `/speckit-constitution`; it is a description, not a gate, so this does not block T070.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: none. T001 first, as `CLAUDE.md` requires.
- **Foundational (Phase 2)**: after Phase 1. **Blocks all user stories.** Android must be green at T015.
- **US1 (Phase 3)**: after Phase 2. The MVP. It includes the reminder coordinator and the permission-granted trigger.
- **US2 (Phase 4)**: after US1 (it extends `LocalNotificationReleaseScheduler` and the iOS Koin module).
- **US3 (Phase 5)**: after US1; its code is independent of US2, but T050 edits `startQuestLog` after T036.
- **US4 (Phase 6)**: after US1 (verification only; it needs T032, T034 and T035).
- **US5 (Phase 7)**: after US1 (the iOS flag is already `true`). It does not depend on US2–US4 and can be dropped.
- **Polish (Phase 8)**: after the phases it documents. T066 needs T063.

### Within Each User Story

- Tests first and failing, then ports and data classes, then implementations, then Koin bindings, then verification.
- In `startQuestLog` (`QuestLogIos.kt`): T036 and T050 edit the same function; apply them in task order.
- In `DataPlatformModule.ios.kt`: T035, T045 and T063 edit the same file; apply them in task order.

### Parallel Opportunities

- Phase 2: T002, T003, T009, T010, T014 are independent files; T005 and T006 are independent tests.
- US1: T017, T018, T020, T021, T022, T026, T029, T031 are independent files.
- US2: T039, T042 are independent files.
- US5: T055–T058 (four test files) in parallel, then T059–T062 (four feature modules) in parallel.
- Polish: T066–T069 in parallel.

### Parallel Example: User Story 1

```text
Task: "FakeLocalNotificationCenter in core/data/src/androidHostTest/.../notification/FakeLocalNotificationCenter.kt"
Task: "ArmedReminderLedgerTest in core/data/src/androidHostTest/.../notification/ArmedReminderLedgerTest.kt"
Task: "ReleaseReminderCoordinatorTest in core/data/src/androidHostTest/.../notification/ReleaseReminderCoordinatorTest.kt"
Task: "LocalReminderRequest.kt and PendingReminder.kt in core/data/src/commonMain/.../notification/"
Task: "NotificationAuthorizationStatus.kt and OpenNotificationSettings.kt in core/ui/src/iosMain/.../util/"
```

---

## Implementation Strategy

### MVP First (User Story 1 only)

1. Phase 1 and Phase 2. Stop at T015/T016: Android unchanged and green.
2. Phase 3 (US1). Stop at T038 and validate on the simulator.
3. The MVP delivers a reminder armed by the opt-in, including the first one after the Allow tap, and reconciled at
   launch and on foreground. It does not yet follow a refresh, honour the 64 cap or open the game on tap, so do not
   ship it alone: those are US2 and US3. From T035 the reminder controls are visible on iOS, so no build between US1
   and US3 should be released.

### Incremental Delivery

1. Setup + Foundational → Android green on the new contracts and a serialized sync.
2. US1 → opt in and get a reminder on iOS, reconciled at launch and foreground.
3. US2 → the reminder follows a refresh and the 64 cap holds (P1, required before release).
4. US3 → tap opens the game.
5. US4 → permission recovery verified.
6. US5 → remove the dead flag and page.

Each phase leaves Android building and `./gradlew test` green, so work can pause after any checkpoint.

---

## Notes

- Release gate: US1 and US2 are both P1, and US3 completes the notification. Do not release with only US1.
- US3 and US5 touch `QuestLogIos.kt` / the feature modules that US2 does not, so they can proceed in parallel with
  it once their prerequisites hold.
- A Windows session can do Phases 1, 2 and the host-test parts of 3–5, 7 and 8. Every **(Mac only)** task, the iOS
  link and the simulator walkthroughs need a Mac. Say so in the report rather than assuming them.
- Commit after each task or logical group, with `type(scope): subject` messages (`feat(data)`, `refactor(domain)`,
  `test(data)`, `docs`, …) and the `Co-Authored-By` trailer this repository requires.
