# Phase 0 Research: Unannounced upcoming games in the anticipated lane

## The load-bearing question

The spec's Assumptions section flags one thing as load-bearing: IGDB must expose a real, reliable signal
that separates "announced, date still pending" from "no date on record at all." If it doesn't, User Story
2's guard (FR-003) cannot be built and the feature has to be reconsidered.

This was not taken on faith. It was checked against the live IGDB API using this project's own
`local.properties` credentials (read-only queries, `/games` and `/games/count`, no data written).

## Decision: the signal exists, and it's `release_dates.date_format = 7`

**Rationale**: Three live queries settle it.

1. **The highest-hype undated game in the catalogue carries the signal.** Querying for `game_type = 0`,
   `first_release_date = null`, sorted by `hypes desc`, the top result is **The Elder Scrolls VI**
   (hypes 502) with a populated `release_dates` array: two entries, one per confirmed platform, both
   `"date_format": 7` and no `date`. Same shape for Deadlock, Judas, Beyond Good & Evil 2, Tides of
   Annihilation, Godforge, Silver Palace, PUBG: Black Budget — every genuinely-anticipated undated title
   in the sample.

2. **Catalogue debris does not carry it.** Querying well-known games (`total_rating_count > 20`) with
   `first_release_date = null` surfaces titles like *Dwarf Fortress* and *Friday Night Funkin'* — but
   these turned out to actually have dated `release_dates` entries (`date_format: 0`), just not mirrored
   to the game-level `first_release_date` scalar. That is a separate, pre-existing IGDB quirk (the
   aggregate field can lag the per-platform one) and out of scope here — FR-002/FR-004 key off
   `first_release_date`, unchanged, so it is unaffected by this feature either way.

3. **The two populations are large and genuinely distinct.** Counting the full catalogue:
   - `first_release_date = null & release_dates = null` (no signal at all — cancelled titles like
     *The Settlers: Kingdoms of Anteria*, abandoned entries like *End of Nations*, or simply never
     recorded): **23,757 games**.
   - `first_release_date = null & release_dates.date_format = 7` (an explicit pending-date signal on at
     least one platform): **20,052 games**.

   Two populations of comparable size that do not overlap by construction (a game with a `date_format = 7`
   row has a non-null `release_dates`) is strong evidence this is a real, IGDB-maintained distinction, not
   an artifact of a handful of well-curated entries.

4. **A genuinely ambiguous middle case exists, and the spec's wording already resolves it correctly.**
   *Half-Life 3* (hypes 94, unmistakably a real, hugely-anticipated title) has **no `release_dates` field
   at all** — IGDB has not confirmed a platform for it yet, so it carries no TBD marker either. Per
   FR-003's exact wording ("no indication that a date is still pending"), it is correctly excluded. This
   is an accepted limitation, not a bug: the shelf now surfaces everything IGDB has confirmed as pending,
   which is a strictly larger set than today, and staying conservative about titles IGDB has not confirmed
   avoids readmitting the "date lost to history" population FR-003 exists to keep out. Recorded here so it
   isn't rediscovered as a surprise later.

**Alternatives considered**:

- *A second query against `/release_dates` to fetch precision for the whole pool, merged in the repository
  layer.* Rejected: doubles the network round-trips for a value that is only needed as an existence check,
  and the two-step rank-then-hydrate shape `fetchPopularityRankedGames` already uses would need a third
  step. Apicalypse can express the existence check directly in the `where` clause of the query that already
  exists (see below), so no extra call is needed.
- *Requesting `release_dates.date_format` in `fields` and carrying precision on the `Game` domain model for
  every pool member.* Rejected: nothing downstream reads it. The date-slot fallback to "unknown release
  date" (`GameUiMapper.toGameItem`) already keys off `Game.releaseDate == null`, which is already true for
  every game this feature admits (see Data Model). Fetching per-platform detail for up to 300 pool members
  just to leave it unused would be a payload cost with no consumer.

## Decision: the filter change is a single `where`-clause branch, upcoming-lane only

**Rationale**: `GameRepositoryImpl.fetchPopularityRankedGames` already takes an `upcomingOnly: Boolean` and
branches the release filter on it. A live test against the real API confirms the exact shape:

```
where id = (81249,28029,7440) & (first_release_date != null | release_dates.date_format = 7)
```

against ids for The Elder Scrolls VI (has a TBD entry), Half-Life 3 (no `release_dates` at all), and The
Settlers: Kingdoms of Anteria (cancelled, no `release_dates` at all) returns **only** The Elder Scrolls VI.
Confirms three things at once: the OR-filter is syntactically valid apicalypse; it can filter on
`release_dates.date_format` without that field being requested in `fields`; and it distinguishes exactly
the cases the spec asks it to.

The production version needs `first_release_date = null` gating the TBD branch — see Data Model for why
that's what makes FR-004's edge case ("a past date but still marked as pending") structurally impossible
rather than something to special-case.

**Alternatives considered**:

- *Applying the relaxed filter to both lanes.* Rejected outright by FR-007 — the popular lane is
  already-released games and has no undated case to begin with; `date_format = 7` never applies to a past
  release.
- *A parallel private method instead of extending the shared one.* Rejected: `fetchPopularityRankedGames`
  is the one place the two lanes' shared shape (rank, hydrate, restore order) lives, and the KDoc already
  frames `upcomingOnly` as exactly this kind of branch point. A second method would duplicate the
  popularity-restore logic FR-009 depends on.

## Non-findings (confirmed no work needed)

- **Display fallback**: `GameUiMapper.toGameItem()` already renders `R.string.unknown_release_date` when
  `Game.releaseDate` is null (`core/ui/mapper/GameUiMapper.kt:125-127`). Every game this feature admits has
  `releaseDate == null` by construction (see Data Model), so User Story 3 / FR-005 / SC-005 need no code
  change — only a test asserting the existing behavior isn't accidentally broken.
- **Hero selection**: `DiscoverMapper.kt` already does `hero = upcoming.firstOrNull()` with no filtering on
  date presence. FR-013 is already the current behavior; admitting more games into `upcoming` is the only
  change needed for an undated game to reach the hero slot.
- **Duplicate rows**: IGDB returns one game object per id regardless of how many `release_dates` rows it
  carries (confirmed in the live probe above — Elder Scrolls VI, with two release-date rows, appeared
  once). FR-011 needs no dedup logic.
- **Pool sizing / latency**: The filter change happens inside the existing single `/games` hydrate call
  already made for the upcoming lane. No new network round-trip, no change to
  `POPULARITY_POOL_LIMIT_UPCOMING`. SC-006 is satisfied by construction.
