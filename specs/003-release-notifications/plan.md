# Implementation Plan: Release Notifications

**Branch**: `003-release-notifications` | **Date**: 2026-09-27 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/003-release-notifications/spec.md`

## Summary

Per-game opt-in release reminders for saved games, delivered on release day. The opt-in is one row per
game in a new Room table; the schedule is one WorkManager one-time request per game, keyed by game id and
re-enqueued with `REPLACE` whenever Radar's existing 24h refresh moves a date. Radar's multi-platform date
resolution is extracted into a shared function so the reminder and the timeline cannot drift apart, and the
notification worker re-verifies eligibility at fire time so a stale schedule can never deliver. The toggle
appears on the Radar row and in the game-detail screen's top app bar, opposite the back button; Settings gains a Notifications group with a
permission row and a management sub-screen.

## Technical Context

**Language/Version**: Kotlin 2.4.10, Java 11 target, JVM toolchain 21

**Primary Dependencies**: Compose BOM 2026.09.00, Hilt 2.60.1, Room 2.8.5 (KSP), WorkManager +
`androidx.hilt.work`, kotlinx-datetime 0.8.0, `androidx.core` (`NotificationManagerCompat`),
activity-compose 1.13.0 (new to `:core:ui`)

**Storage**: Room, `version = 1` with `fallbackToDestructiveMigration(true)` — one new table
(`release_notifications`), schema re-exported without a version bump

**Testing**: JUnit4 + MockK + `kotlinx-coroutines-test` in the existing `src/test` source sets of
`:core:domain`, `:core:data`, `:feature:radar`, `:feature:settings`

**Target Platform**: Android, minSdk 29 / compileSdk & targetSdk 37

**Project Type**: Modular Android app (17 modules)

**Performance Goals**: no added work on Radar's render path (the opt-in set is one more `Flow` folded into
the existing `combine`); reminder delivered within WorkManager's inexact window on the release day

**Constraints**: offline-capable (fire-time content is read from Room, no network); no exact-alarm
permission; `POST_NOTIFICATIONS` requested at runtime on API 33+ only

**Scale/Scope**: tens to low hundreds of saved games, one scheduled work item per opted-in game

## Constitution Check

*GATE: passed before Phase 0 research. Re-checked after Phase 1 design — still passing.*

| Principle | Verdict | How this plan satisfies it |
|---|---|---|
| I. Module boundaries are load-bearing | **Pass** | No new module edge. `feature/*` touches only `:core:{common,model,domain,ui,navigation,designsystem}`; the WorkManager scheduler and the `NotificationManagerCompat` notifier sit in `:core:data` behind ports declared in `:core:domain`, exactly the shape `ReleaseRefreshScheduler` already uses. The new route is a `NavKey` in `core/navigation/Routes.kt` plus one `entryProvider` branch in `:app`. |
| II. Typed errors cross layers | **Pass** | Every new repository method is DB-only, so all of them return a bare `Flow<T>` or `Unit` — no `AppResult`, per the rule that only network-touching methods wrap. Nothing new throws across a layer. |
| III. The UI layer renders, it does not decide | **Pass** | Eligibility, date resolution and precision checks live in use cases; the bell rows render a boolean and emit an event. `RadarViewModel` gains its first `UiEvent`/`onEvent`/effect channel, following the documented shape. The permission read must happen in composition (the launcher lives there), so its result is fed back into the ViewModel as an event and the row text is still produced by a mapper. |
| IV. Reuse the shared layer before adding to it | **Pass** | Reuses `GameListRow`, `CustomContentCard`, `SettingsGroup`/`SettingsRow`, `CustomAlertDialog`, `MaterialTheme.spacing`, and the existing `GameRepository` (no second repository). Radar's date resolution is *extracted and shared*, not copied. Two new shared pieces land in `:core:ui` because three features need them. |
| V. Verification is local, not automated | **Pass** | Verified with `./gradlew :app:assembleDebug` (the change spans modules and touches DI) plus `./gradlew test`. New use-case, mapper and ViewModel logic gets tests in its own module's `src/test`. No lint gate or pipeline is assumed. |

**Deliberate additions that are not violations, recorded so review does not mistake them for drift:**

- `:core:ui` gains two external dependencies (`libs.androidx.activity.compose`, `libs.androidx.core.ktx`).
  These are library deps, not module edges — the constitution's graph is unchanged.
- `:core:data` gains its own `src/main/res/values/strings.xml` for the notification channel and message
  text. A notification has no UI layer to resolve `UiText` in, and `:core:database` already owns a
  `strings.xml` for the same reason.
- One new Room table at `version = 1`, with `schemas/.../1.json` regenerated and committed in the same
  commit. This is what the constitution's persistence section prescribes, not an exception to it.
- `GetRadarTimelineUseCase.kt` (Phase 2 code) is edited to extract its private date resolution. Behaviour
  is unchanged and `GetRadarTimelineUseCaseTest` guards it.

**Resolved during /speckit-analyze**: FR-005 was reworded in spec.md to match this design exactly —
delivery and the management list are guaranteed correct immediately; the WorkManager request and the
opt-in row are cleared on the next reconcile, not synchronously. See [research.md](./research.md) §5 for
why the stricter (immediate) reading was rejected.

## Project Structure

### Documentation (this feature)

```text
specs/003-release-notifications/
├── plan.md              # This file
├── spec.md              # Feature specification
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/
│   ├── domain-ports.md  # Phase 1 output — :core:domain interfaces and use cases
│   └── ui-contracts.md  # Phase 1 output — per-surface state/events, deep link
├── checklists/
│   └── requirements.md  # Spec quality checklist (all items passing)
└── tasks.md             # Phase 2 output (/speckit-tasks — NOT created by /speckit-plan)
```

### Source Code (repository root)

```text
core/model/                       # unchanged — no new domain model is needed
core/database/src/main/java/.../core/database/
├── entity/ReleaseNotificationEntity.kt          # NEW  one row per opted-in game
├── dao/ReleaseNotificationDao.kt                # NEW
└── QuestLogDatabase.kt                          # EDIT register entity + dao
core/database/schemas/                           # EDIT regenerated 1.json (no version bump)

core/domain/src/main/java/.../core/domain/
├── radar/ReleaseDateResolver.kt                 # NEW  extracted from GetRadarTimelineUseCase
├── radar/GetRadarTimelineUseCase.kt             # EDIT call the extracted resolver
├── notification/ReleaseNotificationScheduler.kt # NEW  port: schedule/cancel per game
├── notification/ReleaseNotifier.kt              # NEW  port: post/ensure channel
├── usecase/notification/SetReleaseNotificationEnabledUseCase.kt      # NEW
├── usecase/notification/GetReleaseNotificationGameIdsUseCase.kt      # NEW
├── usecase/notification/GetGamesWithReleaseNotificationsUseCase.kt   # NEW
├── usecase/notification/SyncReleaseNotificationsUseCase.kt           # NEW  the reconciler
├── usecase/notification/DeliverReleaseNotificationUseCase.kt         # NEW  fire-time verify + post
└── repository/GameRepository.kt                 # EDIT four DB-only methods

core/data/src/main/java/.../core/data/
├── scheduler/ReleaseNotificationSchedulerImpl.kt # NEW  WorkManager, unique per game, REPLACE
├── notification/ReleaseNotifierImpl.kt           # NEW  NotificationManagerCompat + channel
├── worker/ReleaseNotificationWorker.kt           # NEW  @HiltWorker, fires the reminder
├── worker/ReleaseDatesRefreshWorker.kt           # EDIT reconcile after a successful refresh
├── repository/GameRepositoryImpl.kt              # EDIT new DAO-backed methods
└── di/DataModule.kt                              # EDIT bind the two new ports
core/data/src/main/res/values/strings.xml         # NEW  channel + notification text

core/ui/src/main/java/.../core/ui/
├── util/NotificationPermission.kt                # NEW  shared permission helper
└── component/NotificationPermissionDeniedDialog.kt # NEW  CustomAlertDialog wrapper
core/ui/build.gradle.kts                          # EDIT activity-compose, core-ktx

core/navigation/src/main/java/.../Routes.kt        # EDIT ReleaseNotificationsRoute

feature/radar/src/main/java/.../feature/radar/
├── RadarViewModel.kt                             # EDIT first onEvent + effect channel
├── RadarScreen.kt                                # EDIT permission helper + effect handling
├── components/RadarGameRow.kt                    # EDIT bell in the trailing slot
├── mapper/RadarUiMapper.kt                       # EDIT carry the opt-in flag
└── model/{RadarEntryUiModel,RadarUiEvent,RadarUiEffect}.kt  # EDIT + 2 NEW

feature/game-detail/src/main/java/.../feature/gamedetail/
├── GameDetailViewModel.kt                        # EDIT handle the new event
├── components/GameDetailSuccessContent.kt        # EDIT populate ImmersiveDetailLayout's actions slot
├── mapper/GameDetailUiMapper.kt                  # EDIT carry the opt-in + availability flags
└── model/{GameDetailUiModel,GameDetailUiEvent,GameDetailUiEffect}.kt  # EDIT

feature/settings/src/main/java/.../feature/settings/
├── SettingsScreen.kt                             # EDIT Notifications group
├── SettingsViewModel.kt                          # EDIT permission state + count
├── ReleaseNotificationsScreen.kt                 # NEW  management sub-screen
├── ReleaseNotificationsViewModel.kt              # NEW
├── mapper/ReleaseNotificationsUiMapper.kt        # NEW
└── model/{ReleaseNotificationsUiState,...}.kt    # NEW  mirrors OwnedPlatforms*

app/src/main/
├── AndroidManifest.xml                           # EDIT POST_NOTIFICATIONS, deep-link filter, singleTop
├── java/.../MainActivity.kt                      # EDIT read intent.data, seed/push GameDetailRoute
└── java/.../QuestLogNavDisplay.kt                # EDIT ReleaseNotificationsRoute branch
```

**Structure Decision**: No new module. The feature spans the existing graph in the same direction Phase 2
already established — ports in `:core:domain`, Android implementations in `:core:data`, UI in the three
feature modules that present a game, wiring in `:app`. The Settings management screen follows
`OwnedPlatformsScreen`/`OwnedPlatformsViewModel` (row → sub-screen → own ViewModel) because an
arbitrary-length list of games does not belong inline in a hub of grouped rows.

## Phase 1 Design Artifacts

- [data-model.md](./data-model.md) — the new table, its lifecycle, and the eligibility predicate
- [contracts/domain-ports.md](./contracts/domain-ports.md) — the two ports, the five use cases, the four
  repository additions
- [contracts/ui-contracts.md](./contracts/ui-contracts.md) — per-surface state/event additions, the
  permission contract, the deep-link contract
- [quickstart.md](./quickstart.md) — how to verify the feature end to end, including forcing a date shift

## Complexity Tracking

> No constitution violations. The deliberate additions and the one FR-005 judgement call are recorded in
> the Constitution Check section above rather than here, because none of them requires a waiver.
