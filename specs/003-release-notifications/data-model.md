# Phase 1 Data Model: Release Notifications

**Feature**: `003-release-notifications` | **Date**: 2026-09-27

## New table: `release_notifications`

`core/database/src/main/java/.../core/database/entity/ReleaseNotificationEntity.kt`

| Column | Type | Notes |
|---|---|---|
| `gameId` | `Int` | `@PrimaryKey`. Matches `games.id`. One row per opted-in game — the opt-in is per game, not per platform (FR-015). |
| `enabledAt` | `Long` | Epoch seconds when the user turned the reminder on. Not shown in the UI today; it is what makes the row's existence auditable and gives the management list a stable secondary sort. |
| `notifiedForDate` | `Long?` | The resolved release date a reminder was actually delivered for, `null` until one is. Not a boolean — see below. |

**Presence of a row means "opted in".** Turning the reminder off deletes the row rather than flipping a
flag, so the table only ever holds live opt-ins and the management list is a plain join.

No foreign key to `games`, matching every existing entity in this module (`GameListCrossRef`,
`DiscoverLaneEntryEntity` and the rest declare none). Referential integrity is maintained by the
eligibility predicate below, which joins against the saved set on every read and every reconcile.

**Room constraints**: registered in `QuestLogDatabase.entities` with `version` left at `1`;
`schemas/com.nikolasguillen.questlog.core.database.QuestLogDatabase/1.json` is regenerated and committed in
the same commit. No `Migration` object — the database still runs
`fallbackToDestructiveMigration(true)` while the app is unpublished.

### Why `notifiedForDate` is a date and not a `delivered` flag

A delayed game can release, fire its reminder, and *then* be given a new future date. A boolean would leave
that game permanently silent. Storing the date the reminder was delivered for lets the reconciler re-arm
automatically: if the newly resolved date differs from `notifiedForDate` and is still in the future, the
game is schedulable again. It also guarantees the "exactly one notification" half of SC-002 — a reconcile
that resolves the same date it already notified for schedules nothing.

## Eligibility predicate

A game is **schedulable** when all of the following hold. This is the single rule the reconciler and the
fire-time verification both apply; it is the behavioural centre of the feature.

1. **Opted in** — a `release_notifications` row exists for `gameId`.
2. **Saved** — the game satisfies `GameDao.getSavedGames`'s existing clause: it sits in some list, or
   carries a non-null `status`, or a non-null `priority` (FR-004). Not a separate notion of "saved".
3. **Has a precise date** — the earliest date returned by the shared resolver (see below) carries
   `DatePrecision.EXACT_DATE`. `YEAR_MONTH`, `QUARTER`, `YEAR_ONLY` and `TBD` are recorded opt-ins with no
   schedule (FR-013, FR-014).
4. **Not in the past** — that date's calendar day, in `TimeZone.currentSystemDefault()`, is today or later
   (FR-008).
5. **Not already notified for that date** — `notifiedForDate` is null, or differs from the resolved date.

A game that fails 2–5 keeps its opt-in row and simply has no scheduled work.

## Resolved date

`core/domain/radar/ReleaseDateResolver.kt` (extracted from `GetRadarTimelineUseCase`)

```
resolveReleaseDates(game, ownedPlatformIds) -> List<ReleaseDate>
```

Unchanged Phase 2 behaviour: one date per owned platform the game has a date for (earliest region date
within each), falling back to a single earliest-across-all-platforms date when there is no selection or no
match. The notification path takes the **minimum** of that list (FR-015) — which, in the fallback branch,
is already the only element.

```
resolveNotificationInstant(releaseDate, precision, now, timeZone) -> Instant?
```

New, alongside the existing `resolveBucket` in the same package and following its shape (`now` and
`timeZone` as parameters, so it is testable off-device). Returns 09:00 local on the release calendar day;
`now` when that instant has passed but the day is still today; `null` when the day is past or the precision
is coarser than `EXACT_DATE`. The 09:00 choice is justified in [research.md](./research.md) §2.

## State transitions

```
        (no row)
           │  user toggles on
           ▼
      opted in, unscheduled ──────────────┐
           │  reconcile finds a precise,  │ reconcile finds a coarse / past
           │  future date                 │ date, or the game left the saved set
           ▼                              │
      opted in, scheduled ────────────────┘
           │  worker fires, re-verifies, posts
           ▼
      opted in, notified (notifiedForDate set)
           │  reconcile resolves a *different* future date
           └──────────► opted in, scheduled   (re-armed)

  any state ──ic user toggles off──► (no row)  + work cancelled
```

The `scheduled` state lives in WorkManager, not in the table. There is deliberately no `scheduledFor`
column: WorkManager already owns that value, and `REPLACE` on a per-game unique work name makes
rescheduling idempotent without the app tracking it. Mirroring it in Room would create two copies of one
truth that could disagree.

## Reads the UI needs

| Read | Shape | Consumer |
|---|---|---|
| Opted-in game ids | `Flow<Set<Int>>` | Radar (one flag per row) and game-detail (one flag), folded into their existing state pipelines — the same cheap-membership shape as `getWishlistedGameIds()` |
| Opted-in saved games | `Flow<List<Game>>` | The Settings management sub-screen. Filtered to the saved set, so a game that left it disappears from the list at once |
| Opted-in count | derived from the set | The Settings group row's subtitle |

No new `:core:model` type is introduced: the toggles need a `Set<Int>` and the management list needs
`List<Game>`, both of which already exist.
