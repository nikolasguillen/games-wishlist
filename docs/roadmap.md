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

## What the taste profile already gives you

`core/domain/usecase/discover/` is built and tested: `GetTasteProfileUseCase` emits a `TasteProfile`
(`:core:model`) of normalised genre and developer weights, and `GetOwnedPlatformsUseCase` /
`SetOwnedPlatformsUseCase` / `GetKnownPlatformsUseCase` own the platform filter, with the user's
explicit selection falling back to the platforms inferred from their saved games. `TasteProfile.isEmpty`
is the cold-start signal. **The "My platforms" picker UI does not exist yet** — the storage and domain
layer do, so the screen is all that is missing.

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

### The return path from results to feed

The results grid stays in the screen body, outside the search bar overlay: it carries the filter chip row,
the filter and sort bottom sheets and the scroll-to-top FAB, all of which live in the `Scaffold` today and
would end up stacked over an overlay and a keyboard.

**There is currently no way back from the grid to the initial state**, for two independent reasons: the
clear (X) button is only rendered while the search bar is expanded (`SearchBars.kt:183`), and clearing the
text does not reset `contentState` anyway — only `performSearch` writes it. There is no `BackHandler` in
`:app` either. Nobody notices today because the initial state is a dead placeholder; it becomes a visible
bug the moment that state holds the feed.

- Show the clear button whenever a query is committed, including with the bar collapsed, and have it reset
  the content state to the feed.
- Add a `BackHandler` so back returns to the feed instead of leaving the tab.
- No "back to recommendations" button is needed: clear-to-browse is the convention users already know, and
  the query left in the collapsed bar is the indicator that they are in results mode.

Implementation consequences:

- Rename `SearchContentState.Initial` to `Discover` — it is no longer initial, it is a state re-entered.
- The feed needs its own scroll state. `SearchScreenContent` has a single `gridState` passed down, and
  sharing it bleeds scroll position between feed and grid; `showScrollToTop` derives from it and must
  follow whichever list is showing.
- Returning to the feed preserves its scroll position and does not refetch. The grid already resets
  correctly via `onResetScroll`.

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
