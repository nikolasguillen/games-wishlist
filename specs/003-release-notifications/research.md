# Phase 0 Research: Release Notifications

**Feature**: `003-release-notifications` | **Date**: 2026-09-27

All unknowns below were resolved by reading the existing Radar (Phase 2) implementation. No open
`NEEDS CLARIFICATION` remains — the two spec-level ambiguities were answered by the owner during
`/speckit-specify` (FR-014, FR-015).

## 1. Scheduling mechanism

**Decision**: One WorkManager `OneTimeWorkRequest` per opted-in game, enqueued as *unique* work named
`release_notification_<gameId>` with `ExistingWorkPolicy.REPLACE` and `setInitialDelay(...)`.

**Rationale**:

- The project already runs WorkManager with a Hilt worker factory (`QuestLogApp` implements
  `Configuration.Provider`; `ReleaseDatesRefreshWorker` is a `@HiltWorker`), so there is no new
  infrastructure to introduce.
- `REPLACE` on a per-game unique name makes rescheduling (FR-007) a single idempotent call: re-enqueueing
  with the new delay silently discards the old request. No bookkeeping of work IDs is needed.
- WorkManager persists its queue across process death and reboot, which is what makes a "fire in 3 months"
  reminder survive without an app-launch re-arm step.

**Alternatives considered**:

- `AlarmManager` with `setExactAndAllowWhileIdle`: rejected. Exact alarms need `SCHEDULE_EXACT_ALARM` /
  `USE_EXACT_ALARM`, which carry a Play policy justification burden, and a release-day reminder does not
  need minute precision. Doze-delayed delivery is acceptable for this feature.
- A single daily "check what releases today" worker: rejected. It replaces a precise per-game schedule with
  a polling job whose behaviour depends on the device waking that day, and it would still need the same
  per-game opt-in table. The per-game schedule is both simpler and more accurate.

## 2. Which instant to fire at

**Decision**: Fire at **09:00 local time on the release calendar day**, resolving the calendar day with
`TimeZone.currentSystemDefault()` — the same conversion `resolveBucket` already uses. If that instant has
already passed but the release day *is* today, schedule with zero delay so it fires promptly. If the
release day is before today, schedule nothing (FR-008).

**Rationale**:

- IGDB stores release dates as a timestamp that is typically midnight UTC. Firing at the raw epoch second
  would deliver the notification the previous evening for anyone west of Greenwich, and at midnight for
  everyone else. A fixed local morning hour is the only reading of "on release day" that is not hostile.
- Reusing `TimeZone.currentSystemDefault()` means the day the notification fires is always the day Radar
  displays the game under, which is the concrete meaning of the spec's "no separate date source".

**Alternatives considered**: firing at local midnight (rejected — a notification at 00:00 is noise);
converting in UTC to match IGDB (rejected — it would disagree with Radar's own bucketing).

## 3. Reusing Radar's date resolution

**Decision**: Extract the multi-platform resolution — currently the `private fun
Game.resolveReleaseDates(ownedPlatformIds)` at the bottom of
`core/domain/radar/GetRadarTimelineUseCase.kt` — into a shared top-level function in
`core/domain/radar/ReleaseDateResolver.kt`, and have both `GetRadarTimelineUseCase` and the new
notification reconciler call it. The notification path then takes the **earliest** of the resolved dates
(FR-015).

**Rationale**: FR-006 ("no independent source of release dates") is only structurally true if there is one
function deciding which date applies to a game. Copying the resolution would let the two drift. The
existing `ReleaseBucketResolver.kt` — a top-level `fun resolveBucket(...)` in the same package, taking
`now` and `timeZone` as parameters so it is testable off-device — is the shape to follow.

**Note**: this edits a Phase 2 file. `GetRadarTimelineUseCaseTest` covers the behaviour being moved, so a
regression shows up in the existing suite.

## 4. Eligibility rule

**Decision**: A game is schedulable when **all** of: it is in the saved set
(`GameDao.getSavedGames`'s clause — in any list, or carrying a status or a priority); its opt-in row
exists; its earliest resolved release date has `DatePrecision.EXACT_DATE`; and that date's calendar day is
today or later.

Per FR-014 the opt-in is **recorded regardless** of precision. A coarse date (`YEAR_MONTH`, `QUARTER`,
`YEAR_ONLY`, `TBD`) simply produces no scheduled work; the next reconcile picks it up once IGDB sharpens
the date. This is why the opt-in row and the schedule are separate concerns rather than one value.

## 5. When reconciliation runs

**Decision**: Two triggers only, both calling the same idempotent `SyncReleaseNotificationsUseCase`:

1. **The opt-in toggle** — reconciles the single game the user just touched.
2. **`ReleaseDatesRefreshWorker`, after a successful refresh** — reconciles every opted-in game. This is
   the trigger that satisfies FR-007, and it has to live in the worker rather than in an in-app observer
   because the refresh normally runs with the app's process dead.

Plus a **fire-time re-verification** inside `ReleaseNotificationWorker`: before posting, it re-checks that
the game is still saved, still opted in, and that today is still its release day. This is the safety net
that lets reconciliation be lazy.

**Alternatives considered**:

- An app-scoped reactive collector over `getSavedGames()` + the opt-in table, started from
  `QuestLogApp.onCreate()`: rejected as YAGNI. It only buys *prompter* cancellation of work that fire-time
  verification already neuters, and it costs an application-lifetime collector plus a second consumer of
  the app-scoped `CoroutineScope` that `DataModule` documents as translation-only.
- Calling the sync from every use case that can change saved-set membership (`ToggleWishlistUseCase`,
  `UpdateGameUseCase`, `AddGameToListUseCase`, `RemoveGameFromListUseCase`): rejected — four call sites
  that are easy to miss, for the same invisible benefit.

**Consequence worth the owner's eye**: FR-005 says unsaving a game "MUST cancel any pending notification
and clear its opt-in state". With this design the *user-observable* half is immediate and total — nothing
is ever delivered for an unsaved game, and it disappears from the Settings management list at once —
but the WorkManager request and the opt-in row may linger until the next reconcile. If the owner wants the
literal wording, the reactive collector above is the way to get it.

## 6. Re-arming after a delivered reminder

**Decision**: The opt-in row stores `notifiedForDate: Long?` — the resolved date a reminder was actually
delivered for, not a bare `delivered` flag.

**Rationale**: A delayed game can release, notify, and *then* get a new future date. A boolean would leave
that game permanently silent; comparing the newly resolved date against `notifiedForDate` re-arms it
automatically while still guaranteeing exactly one notification per date (SC-002).

## 7. Notification tap target

**Decision**: A custom-scheme deep link — `questlog://game/<gameId>` — declared as an `intent-filter` on
`MainActivity` and wrapped in a `PendingIntent` by the notifier. `MainActivity` reads `intent.data`, seeds
or pushes `GameDetailRoute(gameId)` onto the Nav3 back stack, and handles `onNewIntent` with
`launchMode="singleTop"` for the already-running case.

**Rationale**: `:core:data` posts the notification but must not reference `:app`'s `MainActivity` — the
dependency runs the other way. A URI is the only reference that crosses that boundary without inverting it.

**Alternative considered**: `packageManager.getLaunchIntentForPackage(...)` plus an extra. Slightly less
work (no manifest change), but it makes the entry point implicit and gives the app no reusable deep-link
surface. Rejected as the weaker of two similarly sized options.

## 8. Where the notification's own strings live

**Decision**: `:core:data` gains `src/main/res/values/strings.xml` for the channel name/description and
the notification title/body.

**Rationale**: A notification has no UI layer to resolve `UiText` in, and `:core:data` cannot depend on
`:core:ui`. `:core:database` already owns a `strings.xml` for the default wishlist's name for exactly this
reason, so this follows an established precedent rather than inventing one. The constitution's rule is
that display text lives in `strings.xml`, not that it lives in a UI module.

## 9. Permission handling (POST_NOTIFICATIONS)

**Decision**: A shared composable helper in `:core:ui` (`util/NotificationPermission.kt`) exposing whether
notifications can currently be delivered plus a `request()` lambda. `:core:ui` gains
`libs.androidx.activity.compose` and `libs.androidx.core.ktx`.

**Rationale**:

- Three surfaces need it — Radar and game-detail (request on first opt-in, FR-012) and Settings (show
  status and offer a fix, FR-011). `feature/radar` and `feature/settings` already have `activity-compose`;
  `feature/game-detail` does not. One shared helper beats three copies of a version-gated permission dance.
- `NotificationManagerCompat.areNotificationsEnabled()` is the check, not `checkSelfPermission`: minSdk is
  29, so below API 33 there is no runtime permission at all, but the user can still have switched
  notifications off in system settings. `areNotificationsEnabled()` is true-if-deliverable on every
  supported API level; the runtime request is gated to API 33+.
- Permanently-denied recovery is an `Intent` to `Settings.ACTION_APP_NOTIFICATION_SETTINGS`.

**State flow**: the helper lives in composition (where the launcher must be), and the screens feed its
result into their ViewModel as a UiEvent, so the row's text is still produced by a mapper from UiState —
the same shape `TranslationModelRowState` already uses in Settings.

## 10. Surfaces for the toggle

**Decision**:

- **Radar**: a bell `IconButton` appended to `RadarGameRow`'s trailing slot, after the platform tile.
- **Game detail**: a bell `IconButton` inside `GameReleaseInfoCard`, next to the release date.

**Rationale**:

- The Radar row's trailing slot already holds a date label and a platform tile, so a fourth element is
  tight on a narrow screen — but the title column carries `weight(1f)` and ellipsizes, and a 48dp touch
  target fits. The alternative (long-press or swipe) hides the feature's only entry point behind an
  undiscoverable gesture.
- On detail, `GameDetailActionPill` looks like the obvious home but is a fixed-width 220dp pill with
  exactly three slots and a glow treatment; a fourth action means redesigning it. The release card is also
  the better semantic fit: the toggle is about the date, and that is where the date is.
- A multi-platform game shows one Radar row per owned platform, and every one of those rows renders the
  same per-game opt-in (FR-003, FR-015). Toggling either flips both.

**Alternative considered for detail**: `GameDetailPersonalCard`, which holds the user's own fields (notes,
status, priority). Defensible — the opt-in *is* user data — but it separates the control from the date it
acts on. Flagged for the owner in case they prefer it.
