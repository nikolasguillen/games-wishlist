# Quickstart: verifying Release Notifications

**Feature**: `003-release-notifications` | **Date**: 2026-09-27

How to prove the feature works end to end. There is no CI and no lint gate in this project — verification
is local, and the notification half needs a device or emulator because WorkManager and
`NotificationManagerCompat` do not run in a JVM test.

## Prerequisites

- `local.properties` carries `IGDB_CLIENT_ID` and `IGDB_CLIENT_SECRET` (needed for the saved-game refresh
  that drives rescheduling).
- A device or emulator on **API 33+** for the permission flow, plus one on **API 29–32** to confirm the
  no-runtime-permission path still delivers.
- Remember the database runs `fallbackToDestructiveMigration(true)`: the new table wipes local data on
  first launch after the change. Save a couple of games *after* that first launch.

## Build and unit tests

On macOS/Linux:

```bash
./gradlew :app:assembleDebug        # spans modules and touches DI wiring, so the full build
./gradlew test                      # every JVM suite must stay green
```

On Windows (PowerShell), use `.\gradlew.bat` with the same arguments.

Single-module loops while working:

```bash
./gradlew :core:domain:testDebugUnitTest --console=plain -q
./gradlew :feature:radar:compileDebugKotlin --console=plain -q
```

The suites that must go green, and what they are expected to cover, are tabulated in
[contracts/domain-ports.md](./contracts/domain-ports.md#test-contract).

## Scenario 1 — opt in and see it stick (User Story 1)

1. Save a game with a known future release date (add it to the wishlist, or give it a status).
2. Open Radar. The game appears in a dated bucket with an outlined bell on its row.
3. Tap the bell. On API 33+ the system permission dialog appears on this first opt-in; grant it.
4. The bell fills. Open that game's detail screen — the bell in the release card is filled too.
5. Toggle it off from the detail screen, return to Radar: the row's bell is outlined again.

**Expected**: the flag is identical on both surfaces at every step (FR-003), and one opt-in produces exactly
one row in the management list regardless of how many owned platforms the game releases on (FR-015).

## Scenario 2 — the reminder actually fires

The honest way to check delivery without waiting for a real release day:

1. Opt in to a game, then confirm the work is queued:

   ```bash
   adb shell dumpsys jobscheduler | grep -i questlog
   ```

2. Force the scheduled work to run immediately, rather than editing the clock:

   ```bash
   # list the app's work, then run the release_notification_<gameId> one
   adb shell cmd jobscheduler run -f com.nikolasguillen.questlog <jobId>
   ```

   Alternatively, temporarily save a game whose release date is *today* — the scheduler then enqueues with
   zero delay (09:00 local having passed) and the notification arrives within seconds. This is the path that
   also exercises the today-but-past-09:00 branch.

3. Tap the notification.

**Expected**: one notification naming the game; tapping it opens that game's detail screen (FR-009) whether
the app was dead, backgrounded, or already foregrounded. A second run of the same work posts nothing,
because `notifiedForDate` now matches the resolved date (SC-002).

## Scenario 3 — the schedule follows a shifted date (User Story 2)

This is the requirement most worth verifying by hand, since it is the one a user would never forgive.

1. Opt in to a game with a future date. Note the scheduled work's delay from `dumpsys jobscheduler`.
2. Change that game's stored release date to a different future day — either by editing the row directly:

   ```bash
   adb shell "run-as com.nikolasguillen.questlog sqlite3 \
     /data/data/com.nikolasguillen.questlog/databases/quest_log_database \
     'UPDATE game_platform_cross_ref SET date = <new epoch seconds> WHERE gameId = <id>;'"
   ```

   or by picking a game IGDB has genuinely re-dated and letting the real refresh land.
3. Trigger the refresh worker so the reconcile runs:

   ```bash
   adb shell cmd jobscheduler run -f com.nikolasguillen.questlog <releaseDatesRefreshJobId>
   ```

4. Re-read `dumpsys jobscheduler`.

**Expected**: the reminder's delay now matches the new date, and no notification fires on the old one
(FR-007, SC-003). Moving the date into the past instead must leave no scheduled reminder at all (FR-008).

## Scenario 4 — Settings management and permission (User Story 3)

1. Opt in to two or three games. Open Settings: the **Notifications** group shows the count.
2. Open the management row. All opted-in games are listed, each with the date its reminder is set for.
3. Turn one off from the list — it disappears, and its bell is outlined again back in Radar.
4. Revoke notifications in system settings, return to the app.

**Expected**: the Notifications group now shows a permission row stating that reminders will not arrive,
routing to system settings (FR-011, SC-005). The opt-ins themselves are **not** deleted — re-granting
permission resumes them. The permission row is absent again once notifications work.

## Scenario 5 — the edge cases the spec calls out

| Check | Expected |
|---|---|
| Opt in to a game whose date is `Q2 2027` or TBA | Opt-in is recorded; the management list says no date yet; nothing is scheduled (FR-013, FR-014) |
| That game later gets an exact date, then the refresh runs | A reminder appears with no further user action (FR-014) |
| Unsave an opted-in game (remove from all lists, clear status and priority) | It leaves the management list at once, and no notification is ever delivered for it (FR-005) |
| Deny the permission at the first opt-in | The denial dialog explains it; Settings shows the blocked row; the opt-in is still recorded (FR-012) |
| A game already released (Recently released bucket) | No bell on the row at all |
| API 29–32 device | No permission dialog; reminders deliver normally |

## Confirming the shared-resolver guarantee

Radar's timeline and the reminder must agree on which date applies. After any change to date handling,
check that a multi-platform game's reminder lands on the earliest date Radar shows for it — and that
`core/domain/radar/GetRadarTimelineUseCase.kt` still calls the extracted resolver rather than a private copy.
`GetRadarTimelineUseCaseTest` catches a behavioural regression in the extraction; the agreement itself is a
read of the two call sites.
