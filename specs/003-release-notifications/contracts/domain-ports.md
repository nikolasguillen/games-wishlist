# Contract: `:core:domain` ports and use cases

**Feature**: `003-release-notifications` | **Date**: 2026-09-27

Signatures are the contract; bodies belong to implementation. Every port here is declared in
`:core:domain` and implemented in `:core:data`, the shape `ReleaseRefreshScheduler` already established so
that a KMP move replaces the Android half wholesale.

## Ports

### `notification/ReleaseNotificationScheduler.kt`

```kotlin
/**
 * Schedules per-game release-day reminders. Contract only: `:core:domain` never imports `androidx.work`.
 */
interface ReleaseNotificationScheduler {
    /** Enqueues (or re-enqueues) the reminder for [gameId] to fire at [at]. Replaces any existing one. */
    fun schedule(gameId: Int, at: Instant)

    /** Cancels any pending reminder for [gameId]. A no-op when none is scheduled. */
    fun cancel(gameId: Int)
}
```

`at` is `kotlin.time.Instant`, matching `GetRadarTimelineUseCase` and `resolveBucket`. The implementation
converts to an initial delay and enqueues unique work named `release_notification_<gameId>` with
`ExistingWorkPolicy.REPLACE`; `cancel` calls `cancelUniqueWork` with the same name. No network constraint —
the reminder's content is read from Room.

### `notification/ReleaseNotifier.kt`

```kotlin
/** Posts release-day reminders. Contract only: `:core:domain` never imports the Android notification APIs. */
interface ReleaseNotifier {
    /** Posts the reminder for [gameId], titled with [gameName]. Tapping it opens that game's detail screen. */
    fun notifyReleased(gameId: Int, gameName: String)

    /** True when the app can currently deliver a notification at all (permission granted and not disabled). */
    fun canDeliver(): Boolean
}
```

`gameName` is passed in rather than looked up so the notifier stays a thin platform wrapper with no
repository dependency. Channel creation is the implementation's own concern, done lazily before the first
post.

## Repository additions

`core/domain/repository/GameRepository.kt`. All four are DB-only, so all four return a bare `Flow` or
`Unit` — per the constitution, `AppResult` is only for methods that touch the network.

```kotlin
/** The ids of games with a release reminder enabled, for cheap membership checks against a game list. */
fun getReleaseNotificationGameIds(): Flow<Set<Int>>

/** Turns the reminder for [gameId] on (inserting the opt-in) or off (deleting it). */
suspend fun setReleaseNotificationEnabled(gameId: Int, enabled: Boolean)

/** Records that a reminder was delivered for [releaseDate], so the same date is never notified twice. */
suspend fun markReleaseNotificationDelivered(gameId: Int, releaseDate: Long)

/**
 * The release date a reminder was already delivered for [gameId] ([ReleaseNotificationEntity.notifiedForDate]),
 * or `null` when none has been (or [gameId] is not opted in). Read by [SyncReleaseNotificationsUseCase] and
 * [DeliverReleaseNotificationUseCase] to apply eligibility rule 5 in data-model.md.
 */
suspend fun getReleaseNotificationDeliveredDate(gameId: Int): Long?
```

## Use cases

Plain classes with `operator fun invoke(...)`, one per file under
`core/domain/usecase/notification/`. There is no base `UseCase` type in this project.

### `SetReleaseNotificationEnabledUseCase`

```kotlin
suspend operator fun invoke(gameId: Int, enabled: Boolean)
```

Writes the opt-in, then reconciles **that one game** — scheduling it when it is eligible, cancelling it
otherwise. The single entry point behind every bell toggle, so no surface can record an opt-in without
arming it.

### `GetReleaseNotificationGameIdsUseCase`

```kotlin
operator fun invoke(): Flow<Set<Int>>
```

Straight pass-through, for Radar and game-detail to fold into their existing state pipelines.

### `GetSavedGamesUseCase`

```kotlin
operator fun invoke(): Flow<List<Game>>
```

`core/domain/usecase/` (not the `notification/` subpackage — this is a plain pass-through over
`GameRepository.getSavedGames()`, not specific to this feature). Used by the Settings management screen,
combined there with [GetReleaseNotificationGameIdsUseCase] to know which of *every* saved game currently
has its reminder on — the list is every saved game, not just the opted-in ones (FR-010).

### `SyncReleaseNotificationsUseCase`

```kotlin
suspend operator fun invoke(gameId: Int? = null)
```

The reconciler, and the only thing that decides what is scheduled. Idempotent by construction — every
enqueue is `REPLACE` on a per-game unique name, so running it twice changes nothing. Passing `gameId`
narrows it to one game; omitting it reconciles every opt-in.

For each opt-in in scope it applies the eligibility predicate from [data-model.md](../data-model.md) and
either schedules at `resolveNotificationInstant(...)` or cancels. It also deletes opt-in rows whose game has
left the saved set (FR-005).

Called from exactly two places: `SetReleaseNotificationEnabledUseCase` (one game) and
`ReleaseDatesRefreshWorker` after a successful refresh (all games — this is what satisfies FR-007, and it
must live in the worker because the refresh normally runs with the app's process dead).

### `DeliverReleaseNotificationUseCase`

```kotlin
suspend operator fun invoke(gameId: Int): Boolean
```

The fire-time half, called only by `ReleaseNotificationWorker`. Re-applies the same eligibility predicate
before posting — still opted in, still saved, still precise, release day is today, not already notified for
this date — then posts via `ReleaseNotifier` and calls `markReleaseNotificationDelivered`. Returns whether
it posted, so the worker can distinguish "delivered" from "correctly declined"; both are `Result.success()`,
because a stale schedule is not a failure to retry.

This verification is what makes the whole design safe: reconciliation can lag without a wrong notification
ever reaching the user.

## Shared resolver

`core/domain/radar/ReleaseDateResolver.kt` — top-level functions, following `ReleaseBucketResolver.kt`.

```kotlin
/** Moved verbatim out of GetRadarTimelineUseCase; both callers now share it. */
fun Game.resolveReleaseDates(ownedPlatformIds: Set<Int>): List<ReleaseDate>

/**
 * The instant a reminder for a release on [releaseDateEpochSeconds] should fire: 09:00 local on the
 * release calendar day, [now] when that has passed but the day is still today, `null` when the day is
 * past or [precision] is coarser than EXACT_DATE.
 */
fun resolveNotificationInstant(
    releaseDateEpochSeconds: Long?,
    precision: DatePrecision,
    now: Instant,
    timeZone: TimeZone = TimeZone.currentSystemDefault()
): Instant?
```

`GetRadarTimelineUseCase` is edited to call the extracted `resolveReleaseDates` instead of its own private
copy — the structural guarantee behind FR-006's "no independent source of release dates".

## Test contract

| Unit | Module | Must cover |
|---|---|---|
| `resolveNotificationInstant` | `:core:domain` | each `DatePrecision`; today-but-past-09:00; yesterday; far future; a timezone other than the default |
| `SyncReleaseNotificationsUseCase` | `:core:domain` | schedules an eligible game; cancels on a coarse date; cancels on a past date; re-arms when the date moves; prunes an opt-in whose game left the saved set; picks the earliest date across owned platforms |
| `SetReleaseNotificationEnabledUseCase` | `:core:domain` | on → row written and scheduled; off → row deleted and cancelled |
| `DeliverReleaseNotificationUseCase` | `:core:domain` | posts once and marks; declines when unsaved / opted out / already notified for that date |
| `ReleaseNotificationsViewModel` | `:feature:settings` | list renders; toggling off removes the entry; empty state |
| `RadarViewModel` | `:feature:radar` | the opt-in flag reaches the right rows (including both rows of a multi-platform game); toggle event calls the use case; permission effect emitted on opt-in |
| `SettingsViewModel` | `:feature:settings` | permission state maps to the right row state; count subtitle |

Mock the use cases, never the repository. `StandardTestDispatcher`, `Dispatchers.setMain` in `@Before`,
`advanceUntilIdle()` after events.
