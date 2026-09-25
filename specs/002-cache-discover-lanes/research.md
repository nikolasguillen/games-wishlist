# Phase 0 Research: Cache the Discover feed's generic lanes

**Feature**: `002-cache-discover-lanes` | **Date**: 2026-09-25

All unknowns from the Technical Context are resolved below. Every decision is grounded in a file that
was read, not inferred.

## D1 — Where the cache lives in the layer stack

**Decision**: Entirely inside `:core:data` + `:core:database`. `GameRepositoryImpl.getPopularGames` and
`getUpcomingGames` become cache-first; their signatures do not change.

**Rationale**: `GetDiscoverFeedUseCase` only calls `repository.getPopularGames(platformIds)` /
`getUpcomingGames(platformIds)` and then reads `.ids()` off the results
(`GetDiscoverFeedUseCase.kt:138-152`). Nothing above the repository needs to know a result came from
Room. Keeping the change below the domain boundary means `:core:domain`, `:core:ui` and
`feature/search` are untouched, which also satisfies FR-007 mechanically — the recommended shelves and
`DiscoverFeed.hasStaleRecommendations` are computed in the use case and never see this change.

**Alternatives considered**:

- A caching decorator around `GameRepository` in `:core:domain`: rejected — the constitution allows
  exactly one `GameRepository` implementation (Principle IV), and a decorator would put persistence
  policy in the module that is supposed to be persistence-free.
- Caching in `SearchViewModel`: rejected — dies with the process, and Principle III keeps policy out of
  the UI layer.

## D2 — How faithful the cached snapshot must be

**Decision**: Full snapshot. Scalars in a dedicated `cached_games` table, plus platforms, genres and
companies through cached-game cross-refs that point at the **existing shared** `platforms`, `genres`
and `companies` lookup tables.

**Rationale**: This is not a display-only concern. `SearchViewModel.toggleSave` looks the game up in
the in-memory feed and passes the **whole `Game`** into `ToggleWishlistUseCase`
(`SearchViewModel.kt:373-375`), which reaches `GameRepositoryImpl.toggleWishlist` and persists it
verbatim through `saveGameLocal` when no row exists yet (`GameRepositoryImpl.kt:443-445`). A thin
cached `Game` would therefore write a thin row into `games`:

- `genres` and `developers` are exactly what `GetTasteProfileUseCase` builds the recommended shelves
  from, so a game saved from a cache-served lane would contribute no taste signal until its detail
  screen was opened.
- `rating` is rendered on the wishlist's `GameListRow`.

A full snapshot keeps a cache-served `Game` indistinguishable from a network-served one, so no code
path anywhere can tell the difference. Confirmed with the owner before planning.

**Alternatives considered**:

- Lean snapshot (scalars only, ~3 entities): rejected by the owner. Discover itself would render
  identically — `CompactGameCard` reads only cover/name/release, `DiscoverHero` adds `developer`
  (`DiscoverHero.kt:156`) — but it would degrade the taste profile as described above. Platforms would
  have self-healed, since `ToggleWishlistUseCase` schedules an immediate refresh and
  `refreshSavedGameReleaseDates` upserts `game_platform_cross_ref` rows
  (`GameRepositoryImpl.kt:498-500`); genres and companies would not.
- Ordered ids + `fetchedAt` only, re-hydrating `/games` on a cache hit: rejected — it removes only the
  `/popularity_primitives` call and still hits the network on every open, which fails SC-001 and SC-002.

## D3 — Why not the `games` table

**Decision**: A separate `cached_games` table that deliberately omits every user-owned column —
`notes`, `priority`, `status`, `lastViewedAt`, `detailsFetchedAt`.

**Rationale**: FR-006, and the standing decision in `docs/roadmap.md:23-25`. The omission is the point:
`games` doubles as the user's own store, where `lastViewedAt != null` means "recently viewed" and a
`game_list_cross_ref` row means "wishlisted". A catalogue row in that table makes both questions
ambiguous. A table that structurally cannot hold those fields cannot blur them.

**Alternatives considered**: writing catalogue rows into `games` with a discriminator column — rejected
by the feature description and FR-006, independent of technical merit.

## D4 — Storing the order without a list-shaped column

**Decision**: One row per lane entry in `discover_lane_entries`, carrying an explicit `position`.

**Rationale**: `core/database/CLAUDE.md:50-51` forbids list-shaped columns outright ("There is no
`List<String>` converter any more, and reintroducing one is how the comma-joined `artworks`/`engines`
columns happened"). The house precedent is `GameArtworkEntity`, which carries a `position` column
"because `@Relation` cannot sort and the gallery order is user-visible". The lane's popularity order is
user-visible for the same reason — `fetchPopularityRankedGames` goes out of its way to restore it after
the hydrate call loses it (`GameRepositoryImpl.kt:331-333`).

The clarified hero rule falls out of this for free: the hero is `upcoming.firstOrNull()` in
`DiscoverMapper.kt:19`, so it is simply `position = 0` of the `MOST_ANTICIPATED` lane. No hero-specific
storage, no hero-specific code.

## D5 — Keying the cache to the platform selection

**Decision**: No platform key column. `GameRepositoryImpl.setOwnedPlatforms` clears the whole Discover
lane cache **before** applying the new selection.

**Rationale**: `setOwnedPlatforms` is the only write path for the selection — `PlatformDao` exposes it
as the one `@Transaction` that replaces the selection wholesale, and the repository is its only caller
(`GameRepositoryImpl.kt:536-537`). Only one selection is ever active, so per-selection cache variants
have no user value, and invalidating on change satisfies FR-002 and User Story 3 by construction: a
cached lane always belongs to the current selection because a selection change destroys it.

This also avoids encoding a `Set<Int>` into a column, which would be a list-shaped column by another
name. `OwnedPlatformEntity`'s own KDoc makes the same argument for its own shape ("One row per platform
rather than a single multi-value column").

Ordering matters: clear first, then apply. A crash between the two steps then costs at most one
unnecessary refetch, and can never leave a cache built for the previous filter reachable.

**Alternatives considered**:

- A `platformSelectionHash: Int` column compared on read: rejected — correctness by hash equality, where
  a collision silently serves a lane filtered for a different platform set.
- A sorted, comma-joined `platformKey` string: rejected — a list-shaped column (D4).

## D6 — Freshness window and failure behaviour

**Decision**: A single `DISCOVER_LANE_CACHE_TTL` of 6 hours, as a constant in `:core:data` beside the
existing lane constants. On a network failure with any cached copy present — stale or not — the stale
copy is returned as `AppResult.success`.

**Rationale**: Six hours is long enough that no realistic session refetches and short enough that
"Popular this month" does not drift; both lanes rank slow-moving signals, so one window covers both, as
the spec's Assumptions allow. The failure rule is FR-008: a lane that has content should never blank out
because a refresh failed.

This is the one deliberate semantic change worth naming: a lane with a cached copy can no longer surface
a network error to the user. Without a cached copy the behaviour is exactly as today — the failure
propagates. Since `GetDiscoverFeedUseCase` fails the entire feed when either generic lane fails, the
side effect is that a cached feed now survives an outage instead of blanking the screen.

**Alternatives considered**: returning the error alongside stale content — rejected, it would need a
richer result type across `:core:domain` and the UI for a case FR-008 already decides.

## D7 — Lane identity

**Decision**: A `DiscoverLane` enum (`MOST_ANTICIPATED`, `POPULAR_THIS_MONTH`) in `:core:model`, with a
`@TypeConverter` pair added to `core/database/util/Converters.kt`.

**Rationale**: `Converters` already maps `:core:model` enums (`GameStatus`, `WishlistIcon`) and
`core/database/CLAUDE.md:72-73` names that file as the single home for converters. An enum removes the
typo surface a `String` lane key would add to two tables and every query. `:core:model` stays Android-
and Compose-free, so this does not compromise its KMP readiness.

## D8 — Deleting without foreign keys

**Decision**: `DiscoverCacheDao` owns `@Transaction` default-body functions that delete before
inserting, and prunes `cached_games` rows no lane references any more, along with their cross-refs.

**Rationale**: This schema declares no foreign keys anywhere — `PlatformDao`'s KDoc says so explicitly
("`game_platform_cross_ref` declares no foreign key onto this table"), so nothing cascades and cleanup
must be written by hand. The house pattern for exactly this is `GameDao.saveGame`, which deletes the
rows mirroring an API response before re-inserting them, "because a `REPLACE` insert alone would leave
the leftovers of a shorter list behind" (`core/database/CLAUDE.md:61-65`). The shared `platforms`,
`genres` and `companies` lookups are never pruned — they are shared between games and, per the same
document, never cleared.
