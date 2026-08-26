# Roadmap

Planned features, in the order they are meant to be built, plus the decisions already taken so they are
not re-litigated in a later session. This file describes **what is not built yet**; the moment a phase
ships, delete it from here — `git log` is the history.

Written 2026-08-20.

## The shape that was decided

Two ideas were on the table: a `discover` tab (hyped/upcoming games filtered by the user's taste) and a
`calendar` tab (release dates of saved games). They overlap because they order the same catalogue on two
different axes — relevance and time. The resolution:

- **Discover is not a tab.** It replaces the zero-query state of the Search screen, which today is a dead
  `EmptyPage` placeholder. Search becomes "explore": no query → taste-based feed, query → results.
- **Radar is a tab.** A single chronological timeline that merges the user's saved upcoming games with
  taste-matched suggestions, distinguished visually rather than by living on separate screens.
- **No month-grid calendar.** IGDB release dates carry a precision flag — a large share of interesting
  upcoming titles are only `Q4 2026`, `2027` or `TBD` and cannot be placed on a calendar cell. The
  timeline uses buckets (This week / This month / Next 3 months / Later / TBA), which accommodate partial
  dates natively. Revisit only if the bucket list proves insufficient in practice.
- **Notifications are the payoff of Radar**, not an optional extra. A timeline the user must open is worth
  far less than a reminder that arrives on release day.

Bottom bar goes from 2 tabs to 3: Search · Radar · Lists.

- **Settings is not a tab.** A `SettingsRoute` (the "My platforms" picker lives here first) is reached via
  a profile icon in the top-right corner, present on every top-level screen — Search and Lists today,
  Radar when it exists — and absent from stacked screens (detail, wishlist, settings itself, and anything
  else pushed on the backstack). A fourth bottom-bar tab was considered and
  rejected: Search/Radar/Lists are peer content destinations, Settings is a utility action, not a peer of
  the same kind. The icon itself is one shared composable in `:core:ui` so Search, Lists and (later) Radar
  call the same implementation instead of duplicating it.

## What the taste profile already gives you

`core/domain/usecase/discover/` is built and tested: `GetTasteProfileUseCase` emits a `TasteProfile`
(`:core:model`) of normalised genre and developer weights, and `GetOwnedPlatformsUseCase` /
`SetOwnedPlatformsUseCase` / `GetKnownPlatformsUseCase` own the platform filter, with the user's
explicit selection falling back to the platforms inferred from their saved games. `TasteProfile.isEmpty`
is the cold-start signal. **The "My platforms" picker UI does not exist yet** — the storage and domain
layer do, so the screen is all that is missing. The `SettingsRoute` shell and the profile icon that opens
it are built — the picker's own content is what has to be filled in.

## Phase 1 — Discover feed in Search

Replaces `SearchContentState.Initial` (today `InitialSearchPlaceholder` in
`feature/search/components/SearchPlaceholders.kt`). The search bar overlay keeps owning recent searches and
recently viewed games — the feed lives in the body, behind it.

- Cold start (`TasteProfile.isEmpty`) degrades to generic popular/upcoming. Build that path first — it is
  the first shippable milestone and it needs none of the ranking.
- Coarse filtering server-side in apicalypse (`where genres = (...) & platforms = (...)`), fine ranking
  locally. Personalisation is not expressible in apicalypse, and `limit` caps at 500 — fetch a candidate
  pool, rank in a mapper/use case.
- Every row states its reason ("Because you saved Baldur's Gate 3", "Your genre: RPG"). A recommendation
  the user cannot explain reads as a bug.
- New API surface on `IgdbApiService`: same `@POST("games")` endpoint, different apicalypse bodies.
  Worth checking `popularity_primitives` for real hype signal instead of the raw `hypes` field.

**Cache**: do not dump discovered games into the `games` table unqualified. That table already doubles as
a cache with ownership flags (`isWishlisted`, `lastViewedAt`); mixing in feed results makes "the user's own
games" ambiguous. Use a separate entity holding the ordered id list plus a `fetchedAt` stamp.

**TBA release dates — open problem.** The two lanes split on the release window (`first_release_date > now`
for "Most anticipated", `<= now` for "Popular this month"), which drops games with a null
`first_release_date`. Plenty of genuinely anticipated upcoming titles are still TBA, so they silently miss
the anticipated shelf. Not just relaxing the filter: a bare `| first_release_date = null` also lets in old
games whose date was never recorded, so we cannot tell "unannounced upcoming" from "date lost to history"
without the date-precision flag — the same flag the Radar timeline depends on (see the Phase 2 note on
`release_dates`). Decide this together with that work rather than bolting a heuristic on here.

### Debounced remote suggestions stay

They were considered for removal as redundant with the results grid. They are not: the grid answers "show
me everything matching X", the suggestions answer "I already know the game, take me to it" — a tap goes
straight to the detail screen. In a wishlist app search is predominantly known-item, and the Discover feed
makes it more so, since browsing moves to the feed. Removing them would also leave the expanded search bar
overlay holding three strings of history while covering the feed the user was browsing.

The overlay itself is built. The suggestion cap is 4, so `sort hypes desc` is aggressive — an obscure
title can be squeezed out by hyped ones sharing a substring. If that becomes annoying, sort by name-match
quality rather than raising the cap.

Note the two paths do not use the same matching: suggestions use `where name ~ *"query"*` sorted by hypes,
the full search uses IGDB's `search "query"` full-text relevance with a different `game_type` filter. A
suggestion is therefore a shortcut, not a preview of the grid. That is defensible for an autocomplete —
substring-on-title is less noisy than IGDB full-text — and it is why the "see all results" row commits the
query to the grid.

## Phase 2 — `:feature:radar`, saved games only

- New module (copy `feature/search/build.gradle.kts`, register in `settings.gradle.kts`), `RadarRoute` in
  `core/navigation/Routes.kt`, tab wired in `:app`.
- Chronological bucket timeline over the user's saved games.
- **The release dates are not there yet.** `GamePlatformCrossRef.releaseDate` is only populated by the
  detail fetch (`GameRepositoryImpl.kt:125` requests `release_dates.date`; the search query at line 52 does
  not). A refresh job for saved games' dates is a prerequisite, not a detail.
- Query `@POST("release_dates")`, not `games`: it returns one row per game×platform×region, sortable and
  filterable by date directly, and carries the date-precision field the buckets depend on.
- Dates slip constantly. Without periodic refresh the timeline lies, which is worse than not having one.

## Phase 3 — Release notifications

Opt-in per game ("Notify me"), driven by the same refreshed dates.

## Phase 4 — Suggestions lane in Radar

Fold the taste profile into the timeline: saved games get the visual accent, suggestions sit in a minor
tone alongside them. Only after phases 2 and 3 are real.

## Decisions that would be expensive to reverse

Per the KMP section in the root `CLAUDE.md`:

- **Use `kotlinx-datetime`, not `java.time`.** A timeline feature spreads date math everywhere; rewriting
  it after a KMP move is exactly the work the project is trying to avoid.
- **WorkManager is Android-only.** Put the scheduling contract in `:core:domain` (e.g.
  `ReleaseRefreshScheduler`) with the implementation in `:core:data`, so a KMP move replaces only the impl.
- Room's destructive migration is deliberate while the app is unpublished — new entities for these phases
  wipe the device, and that is fine. See `docs/tech-debt.md`.
