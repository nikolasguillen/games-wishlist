# Implementation Plan: iOS Release Notifications

**Branch**: `013-ios-push-notifications` | **Date**: 2026-10-11 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/013-ios-push-notifications/spec.md`

## Summary

This brings Android's release reminders (spec 003) to iOS as **local** notifications the app schedules on the
device. There is no server. The reconciler `SyncReleaseNotificationsUseCase`, the opt-in table, the date resolution
and every reminder screen are already shared. What iOS lacks is an implementation of the scheduler contract, the
permission `actual`s, tap handling, and the UI that `ReleaseRemindersAvailability` hides.

One platform difference drives the design. An Android reminder runs app code at fire time and re-checks
eligibility there. An iOS local notification is delivered by the system with no app code at all. iOS therefore
keeps its pending set correct ahead of time:

- It resyncs at launch, on foreground, after permission is granted and after each refresh.
- It cancels a reminder as soon as its game is unsaved.
- It keeps a small ledger of armed reminders, so it can tell afterwards that one fired and never posts it twice.
- It keeps the soonest 64 under the system's pending-notification cap.
- It serializes the sync, so a launch from a tapped reminder (several syncs within moments) cannot re-arm a
  reminder that has just fired.

The decision logic sits in `commonMain` behind ports, so the JVM tests reach it. Only thin adapters touch
`UserNotifications`.

Once iOS has reminders, the availability flag is `true` everywhere and is deleted, along with the
"reminders unavailable" onboarding page.

## Technical Context

**Language/Version**: Kotlin 2.4.20 (Multiplatform; Kotlin/Native for iOS), Swift (no change expected)

**Primary Dependencies**: Existing only. These are Koin 4.2.2, kotlinx-coroutines, kotlinx-datetime, DataStore
Preferences (already on iOS), Compose Multiplatform resources (`:core:ui`), WorkManager (Android, unchanged), and
Apple's `UserNotifications` framework through Kotlin/Native's platform bindings. **No new library and no library
swap.**

**Storage**: Room is unchanged. There is no entity change, it stays at `version = 1`, and there is no schema
export. One new iOS `DataStore` key, `release_reminders_armed`, holds the armed-reminder ledger
([data-model.md](data-model.md)).

**Testing**: JUnit4 + MockK + kotlinx-coroutines-test in `androidHostTest`, reusing `InMemoryPreferencesDataStore`
and `FakeClock` from `core/data`'s test set. The `UserNotifications` adapters, the delegate and the permission
`actual` are verified on the iOS simulator, on a Mac only.

**Target Platform**: Android (minSdk 29, unchanged behaviour) and iOS 16+ (`UIApplicationOpenNotificationSettingsURLString`
is iOS 16+, which matches the minimum).

**Project Type**: Mobile app, Kotlin Multiplatform with Compose Multiplatform, 19 modules.

**Performance Goals**: A full reminder sync is a few DB reads plus one pending-requests query. It has to finish
well inside a `BGAppRefreshTask` budget (about 30 s) together with the refresh. It runs on app foreground, so it
must not block the main thread: it runs in the app scope on `Dispatchers.Default`.

**Constraints**: At most 64 pending local notifications per app. No app code runs at fire time on iOS. The fire
hour is 09:00 local (`NOTIFICATION_HOUR`) on both platforms. Floating triggers keep the reminder on the release
day in the current time zone.

**Scale/Scope**: A handful to a few hundred opted-in games. About 22 new Kotlin files (about 10 of them tests and
fakes), about 25 edited, and one flag removed across 4 ViewModels and their tests.

All Technical Context unknowns were resolved in [research.md](research.md). No NEEDS CLARIFICATION remains.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-checked after Phase 1 design. Result below.*

| Principle | Status | Notes |
|---|---|---|
| I. Module boundaries | ✅ Pass | No new module and no new edge. `:shared` already depends on `:core:ui` and `:core:data`, and implements `ReleaseReminderText` there. `:core:data` and `:core:ui` reach `UserNotifications` only from their own `iosMain`. Feature modules gain no dependency. `:core:ai` is untouched. |
| II. Typed errors | ✅ Pass | `UserNotifications` failures stop in the iOS adapter: `add` returns `false`, other failures are logged and swallowed, and `CancellationException` is always rethrown. Nothing new crosses `:core:data` as an exception. No `AppResult` change. |
| III. UI renders | ✅ Pass | No decision moves into a composable. The flag removal deletes ViewModel branches, the permission `actual`s only report state, and the reminder controls already exist. |
| IV. Reuse | ✅ Pass | The iOS denied dialog is `CustomAlertDialog` with the existing strings. Tap navigation reuses `QuestLogRoot`'s `pendingDeepLinkGameId`. The text moves to `:core:ui`'s resources, so it is not copied. There is still one `GameRepository`. |
| V. Local verification | ✅ Pass | `compileAndroidMain`, `assembleDebug`, `./gradlew test` and the iOS link/`xcodebuild` commands. No CI or lint gate is assumed. Simulator-only checks are named as such in [quickstart.md](quickstart.md). |
| Persistence | ✅ Pass | No entity change and no version bump. |
| Injection | ✅ Pass | Each implementation is bound in its module's platform Koin module, and `:shared` assembles them. No `SavedStateHandle`. `KoinGraphTest` stays green after the availability binding goes. |
| KMP | ✅ Pass | Android builds and passes at every phase. The capability is recorded in `platform-contracts.md` in the same commit (contracts §8). No library swap. The two `commonMain` classes bound only on iOS follow spec 012's recorded exception. |

**Wording that goes stale**: The constitution's KMP paragraph (line 143) says iOS "ships without release reminders
and on-device translation". After this feature, both halves are false: translation already shipped through
FoundationModels. This is a description, not a principle, so no gate fails. It is corrected with
`/speckit-constitution` after implementation. `CLAUDE.md` and `docs/roadmap.md` are edited in the feature's last
phase.

**Post-design re-check**: Still all ✅. The design added three ports (`LocalNotificationCenter`,
`ReminderSyncTriggers`, `ReleaseReminderText`) and changed two domain signatures. None of that adds a module edge
or a library.

## Project Structure

### Documentation (this feature)

```text
specs/013-ios-push-notifications/
├── spec.md
├── plan.md              # This file
├── research.md          # Phase 0: 11 decisions
├── data-model.md        # Phase 1: ReleaseReminder, ledger, request, authorization
├── quickstart.md        # Phase 1: Android, host-test and simulator checks
├── contracts/
│   └── ios-release-reminders.md
├── checklists/
│   └── requirements.md
└── tasks.md             # Phase 2 (/speckit-tasks), not created here
```

### Source Code (repository root)

```text
core/domain/src/commonMain/kotlin/com/nikolasguillen/questlog/core/domain/
├── notification/
│   ├── ReleaseReminder.kt                         # NEW
│   ├── ReleaseNotificationScheduler.kt            # suspend; schedule(ReleaseReminder); takeFiredReminders()
│   ├── ReleaseNotifier.kt                         # suspend notifyReleased; canDeliver() removed
│   └── ReleaseRemindersAvailability.kt            # DELETED (last phase)
└── usecase/notification/
    ├── SyncReleaseNotificationsUseCase.kt         # records fired reminders first; serialized by a Mutex
    └── DeliverReleaseNotificationUseCase.kt       # awaits suspend notifier
core/domain/.../di/DomainKoin.kt                   # SyncReleaseNotificationsUseCase: factoryOf -> singleOf

core/data/src/
├── commonMain/kotlin/.../core/data/
│   ├── notification/
│   │   ├── ReleaseReminderText.kt                 # NEW port (implemented in :shared)
│   │   ├── LocalNotificationCenter.kt             # NEW port (implemented in iosMain)
│   │   ├── LocalReminderRequest.kt, PendingReminder.kt   # NEW
│   │   ├── ArmedReminderLedger.kt                 # NEW (DataStore string set)
│   │   ├── LocalNotificationReleaseScheduler.kt   # NEW (iOS-bound)
│   │   ├── TrimToCapacity.kt                      # NEW pure function
│   │   ├── ReminderSyncTriggers.kt                # NEW port (implemented in :shared iosMain)
│   │   ├── ReleaseReminderCoordinator.kt          # NEW (iOS-bound)
│   │   └── NoOp*, StaticReleaseRemindersAvailability.kt  # DELETED
│   └── scheduler/ReleaseRefreshRunner.kt          # full sync after a successful refresh
├── androidMain/.../notification/ReleaseNotifierImpl.kt          # text via ReleaseReminderText
├── androidMain/.../scheduler/ReleaseNotificationSchedulerImpl.kt # new signature
├── androidMain/res/values/strings.xml             # title/body removed (channel strings stay)
├── iosMain/.../notification/UserNotificationCenter.kt           # NEW adapter over UNUserNotificationCenter
├── iosMain/.../di/DataPlatformModule.ios.kt       # real bindings
└── androidHostTest/.../notification/              # NEW tests + FakeLocalNotificationCenter

core/ui/src/
├── commonMain/composeResources/values/strings.xml # + release_notification_title_format, release_notification_body
├── iosMain/.../util/NotificationAuthorization.kt  # NEW status cache
├── iosMain/.../util/NotificationPermission.ios.kt # real actual
└── iosMain/.../component/NotificationPermissionDeniedDialog.ios.kt  # real actual

shared/src/
├── commonMain/.../shared/ReleaseReminderTextImpl.kt + di binding
└── iosMain/.../shared/
    ├── QuestLogIos.kt                             # delegate, seed, coordinator start
    ├── MainViewController.kt                      # pendingDeepLinkGameId from ReminderDeepLinks
    ├── ReleaseReminderNotificationDelegate.kt     # NEW
    ├── ReminderDeepLinks.kt                       # NEW
    └── IosReminderSyncTriggers.kt                 # NEW

feature/{settings,game-detail,radar,onboarding}/   # flag branches + their flag tests removed (last phase)
iosApp/                                            # no change expected
docs/roadmap.md, CLAUDE.md, specs/010-kmp-migration/contracts/platform-contracts.md
```

**Structure Decision**: No new modules. Each piece goes into the module that already owns the concept: the domain
contract in `:core:domain`, scheduling in `:core:data`, permission UI in `:core:ui`, and entry points and
navigation in `:shared`. Logic bound only on iOS lives in `commonMain` with ports, like spec 012's
`BackgroundTaskReleaseRefreshScheduler`, so the JVM tests reach it.

## Phasing (each phase leaves Android building and green)

1. **Contracts and Android adaptation.** Change the `suspend` signatures and add `ReleaseReminder` and
   `takeFiredReminders`. Make the sync record fired reminders, and serialize it (one `Mutex`, bound as a `single`).
   Move the text to `:core:ui` behind `ReleaseReminderText`. Android behaves identically, and iOS still binds the
   (adapted) no-ops.
2. **iOS opt-in end to end (US1).** Add the ledger, `LocalNotificationReleaseScheduler`, the `UserNotificationCenter`
   adapter, the authorization cache, the permission and dialog `actual`s, the sync triggers and the
   `ReleaseReminderCoordinator`, then **turn the flag on** for iOS. The coordinator belongs here, not later: the
   first opt-in happens while permission is undecided, so the scheduler refuses it, and only the "permission newly
   granted" trigger arms it after the user taps Allow.
3. **iOS pending set stays correct (US2).** Add `trimToCapacity` (the 64 cap) and the post-refresh sync in
   `ReleaseRefreshRunner`.
4. **Taps (US3).** Add the notification delegate, `ReminderDeepLinks` and the `MainViewController` wiring.
5. **Settings verification (US4).** No new code; the screens go live when the flag flips, and the walkthrough checks
   them.
6. **Remove the flag (US5).** Delete `ReleaseRemindersAvailability`, the no-ops, `RadarWithoutReminders` and the
   flag-only tests. Update the docs and `platform-contracts.md`. This phase is separable: dropping it leaves a
   working feature with a constant `true` flag.

The reminder controls are visible on iOS from phase 2, before the cap, the refresh sync and taps exist. The app is
unpublished, so this is a development-time state only: no build between phases 2 and 4 is released.

## Complexity Tracking

No constitution violation needs justifying. These two choices add surface and are recorded here so they are not
mistaken for accidents:

| Choice | Why needed | Simpler alternative rejected because |
|---|---|---|
| Armed-reminder ledger (iOS DataStore key) | iOS runs no code at fire time, so without it a release-day resync posts the reminder twice (research §3) | The delivered list loses notifications the user cleared. A Room column changes the schema for an iOS-only fact |
| `SyncReleaseNotificationsUseCase` bound as a `single` with a `Mutex` | The ledger rule reads state that `takeFiredReminders` has just consumed, so two interleaved syncs can double-post (research §3). A `factory` would give each caller its own lock | A lock in the iOS scheduler only cannot see the repository's delivered date, and a per-instance lock under `factoryOf` serializes nothing |
| `ReleaseReminderCoordinator` watching saved-set removals | There is no fire-time re-check on iOS, so an unsaved game's reminder must be cancelled immediately (FR-004) | Hooking each removal use case means four shared edits that also change Android, and it misses the next removal path |
