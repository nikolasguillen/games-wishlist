# Phase 1 Data Model: Unannounced upcoming games in the anticipated lane

No new persisted state, no new domain type, no schema change. This feature widens which existing `Game`
rows a query admits; every type involved already exists.

## Entities (reused, not created)

### `Game` (`core/model/Game.kt`)

The spec's "Anticipated game" entity. Relevant existing fields only:

| Field | Type | Role in this feature |
|---|---|---|
| `id` | `Int` | Dedup key; FR-011 is satisfied because IGDB returns one row per id regardless of how many release-date rows it carries (confirmed in research.md). |
| `releaseDate` | `String?` | `null` for every game admitted via the new TBD branch — see "Why `releaseDate` is always null here" below. Drives the existing `unknown_release_date` fallback in `GameUiMapper`. |
| `hypes` | `Int` | Unchanged; ranking still comes from `/popularity_primitives`, not from this field. |
| `platforms` | `List<Platform>` | Unaffected; the platform filter (FR-008) is unchanged, applied to the same hydrate query. |

No field on `Game` needs to change, and no new field is added. In particular, this feature does **not**
put `DatePrecision` on `Game` — nothing downstream would read it (see research.md's "Non-findings").

### `DatePrecision` (`core/model/DatePrecision.kt`)

The spec's "Release date precision" entity. Already exists with the exact value the feature depends on:
`TBD`, mirroring IGDB's `release_dates.date_format = 7`. This feature does not construct a
`DatePrecision` value at runtime — it only relies on the *scalar* `7` inside an apicalypse `where` clause,
which is IGDB-side filtering, not application-side mapping. The enum is referenced here only to document
that the value this feature keys off is not an invented magic number; it is the same constant the codebase
already uses for Radar's `TBA` bucket (`ReleaseBucketResolver.kt:33`).

## Why `Game.releaseDate` is always `null` for a game admitted by the new branch

This is the load-bearing structural fact the whole feature rests on, so it is worth stating as an
invariant rather than leaving it implicit in the query string:

> The new admission branch only fires when `first_release_date = null`. `Game.releaseDate` is derived
> exclusively from `first_release_date` (`GameMapper.kt:40`, `IgdbGame.toGame()`). Therefore every game
> the new branch admits has `Game.releaseDate == null`, unconditionally.

This single fact is what makes three separate requirements true simultaneously without extra code:

- **FR-004 / the "past date but still marked pending" edge case** cannot occur: a game with a past
  `first_release_date` fails the `first_release_date = null` precondition, so it can only be admitted
  through the unchanged `first_release_date > now` branch — which it also fails, being in the past. It is
  excluded by construction, not by a special case checking for contradictory data.
- **FR-005 / SC-005 (unknown-date wording)** needs no new UI code: `GameUiMapper.toGameItem()` already
  maps a `null` `releaseDate` to `R.string.unknown_release_date`.
- **FR-013 (hero)** needs no new selection logic: `DiscoverMapper.kt`'s `hero = upcoming.firstOrNull()`
  already picks whatever the repository ranks first, dated or not.

## Query contract (the actual "shape" this feature changes)

See `contracts/upcoming-lane-release-filter.md` for the precise apicalypse fragment, its truth table, and
the live-API evidence backing each row.

## State transitions

None. Discover results are not persisted (`GameRepositoryImpl`'s existing comment: "Nothing is persisted
-- these are catalogue results, not the user's games"). A game's presence in the shelf is recomputed fresh
on every fetch from IGDB's current data, so there is no stored state to migrate, version, or transition
between.
