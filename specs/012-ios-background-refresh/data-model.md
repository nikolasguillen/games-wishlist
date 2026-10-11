# Data Model: iOS Background Refresh of Release Dates

No Room entity changes, and no database version change. The only persisted value is already there.

## Last successful refresh time

| | |
|---|---|
| Storage | Settings Preferences DataStore, key `release_dates_last_refresh_epoch_ms` (`Long`, epoch ms). The **same key** `InProcessReleaseRefreshScheduler` writes today, so existing installs keep their state |
| Written | Only when `RefreshReleaseDatesUseCase` returns `AppResult.Success`, with `clock.now()` at that moment |
| Never written | On `Failure`, on cancellation, or on a run that was not due |
| Read by | The due check, and the next-begin-date computation |
| Shared by | Background and in-app runs (one runner, one key) |

## Refresh run (`ReleaseRefreshRunner`, in memory)

At most one exists per process. It is held as a shared deferred result in the app-scoped `CoroutineScope`.

| Field | Meaning |
|---|---|
| `onlyIfDue` | `true` for launch and background; `false` for a wishlist change |
| outcome | `ReleaseRefreshOutcome`: `REFRESHED`, `NOT_DUE` or `FAILED`; or cancelled (`CancellationException`) |

State transitions:

```
idle ──request──▶ running ──use case Success──▶ REFRESHED (timestamp written) ──▶ idle
                     │  ├─use case Failure───▶ FAILED                         ──▶ idle
                     │  └─not due (onlyIfDue)▶ NOT_DUE (no network)           ──▶ idle
                     └──cancel()───────────▶ cancelled (nothing written)      ──▶ idle
running ──another request──▶ joins the same run, gets the same outcome
```

Validation rules:

- **Due** ⇔ the timestamp is missing, or `now - timestamp >= RELEASE_DATES_REFRESH_INTERVAL` (24 h, the
  single constant in `ReleaseRefreshInterval.kt`). This is the same boundary as today: exactly 24 h is due,
  23 h 59 min is not.
- An empty wishlist goes through the use case, which returns `Success` with no network call, so it counts as
  `REFRESHED` (FR-010).

## Pending background request (owned by iOS)

| | |
|---|---|
| Identifier | `com.nikolasguillen.questlog.release-dates-refresh`, listed in `Info.plist`'s `BGTaskSchedulerPermittedIdentifiers`; Swift-only |
| Kind | `BGAppRefreshTaskRequest` |
| `earliestBeginDate` | `lastSuccess == null ? now : max(now, lastSuccess + interval)` (research R5) |
| Cardinality | At most one: a new submit replaces the pending one |
| Submitted | Before every runner call starts and again after it completes, whatever the origin or outcome. Every launch makes one runner call. The second submit replaces the first |
| Submit rejected | Logged by Swift and ignored: background refresh switched off, Simulator, too many requests. The in-app fallback still runs |

## Outcome reported to the system

| Run outcome | `setTaskCompleted(success:)` |
|---|---|
| `REFRESHED` | `true` |
| `NOT_DUE` | `true` |
| `FAILED` | `false` |
| cancelled (expiration) | `false` |
