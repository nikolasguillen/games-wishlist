# Contract: the upcoming-lane release filter

This project has no external API of its own — `GameRepository`'s interface and every domain model it
returns are explicitly unchanged (FR-010). The one real interface this feature touches is the apicalypse
`where` fragment `GameRepositoryImpl` sends to IGDB's `/games` endpoint for the anticipated lane. That
fragment is frozen here so implementation and tests can be checked against it directly.

## Current fragment (unchanged, popular lane only)

`fetchPopularityRankedGames(upcomingOnly = false, ...)` — **no change in this feature**:

```
first_release_date != null & first_release_date <= $nowSeconds
```

## Current fragment (upcoming lane, before this feature)

```
first_release_date > $nowSeconds
```

## New fragment (upcoming lane, after this feature)

```
(first_release_date > $nowSeconds | (first_release_date = null & release_dates.date_format = 7))
```

Spliced into the existing `where` clause exactly where the old fragment was:

```
where id = ($idList) & game_type != ($excludedIds) & version_parent = null & cover != null & $releaseFilter$platformFilter
```

The outer parentheses are required: the fragment is `&`-joined with the id list, the type exclusion, and
the platform filter, and without them the `|` would bind loosely across the whole clause instead of just
the release condition. Verified live against IGDB (see research.md) with exactly this composition.

`7` is IGDB's `date_format` scalar for "to be announced," the same value `DatePrecision.TBD` maps from in
`GameMapper.kt`'s `fromIgdbDateFormat`. Name it as a constant at the call site (matching the existing
`POPULARITY_TYPE_WANT_TO_PLAY` / `POPULARITY_TYPE_PLAYING` style) rather than an inline magic number.

## Truth table

| `first_release_date` | Has a `release_dates` row with `date_format = 7` | Admitted? | Why |
|---|---|---|---|
| Future timestamp | (irrelevant) | Yes | Unchanged existing branch — FR-002. |
| `null` | Yes | **Yes — new** | FR-001. Confirmed live: The Elder Scrolls VI (hypes 502). |
| `null` | No | No | FR-003. Confirmed live: Half-Life 3 (hypes 94, no `release_dates` at all). |
| Past timestamp | Yes (on a different platform) | No | FR-004. Excluded by construction: the TBD branch requires `first_release_date = null`, and a past timestamp also fails the future-date branch. This is the "contradictory data" edge case from the spec — it cannot reach the admit path at all. |
| Past timestamp | No | No | Unchanged existing behavior. |

## Live evidence (2026-09-25, against the real IGDB API)

- Population size, no signal at all (correctly excluded): **23,757** games matching
  `first_release_date = null & release_dates = null & game_type = 0 & version_parent = null & cover != null`.
- Population size, TBD signal present (newly admitted): **20,052** games matching
  `first_release_date = null & release_dates.date_format = 7 & game_type = 0 & version_parent = null & cover != null`.
- Targeted three-id probe — `where id = (81249,28029,7440) & (first_release_date != null | release_dates.date_format = 7)`
  against The Elder Scrolls VI (81249, has TBD entries), Half-Life 3 (28029, no `release_dates`), and The
  Settlers: Kingdoms of Anteria (7440, cancelled, no `release_dates`) — returned **only** id 81249.

## What this contract does not cover

- `release_dates.date_format` is filtered on but never requested in `fields`. Confirmed live that IGDB
  allows filtering on a relation field that isn't also selected. If a future change needs the precision
  value itself (not just its existence), `fields` must be extended separately — this contract does not
  authorize that.
- This contract governs `upcomingOnly = true` only. `upcomingOnly = false` (the popular lane) is explicitly
  out of scope (FR-007) and its fragment above is documented only to make the "no change here" boundary
  checkable.
