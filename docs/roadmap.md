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
(`:core:model`) of normalised genre and developer weights, and `GetSelectedPlatformIdsUseCase` /
`GetSelectedPlatformsUseCase` / `SetOwnedPlatformsUseCase` / `GetKnownPlatformsUseCase` own the platform
filter, which is the user's explicit selection and nothing else. `TasteProfile.isEmpty`
is the cold-start signal. The "My platforms" picker is built end to end (`OwnedPlatformsRoute`, reached
from the Settings hub) and `SyncPlatformCatalogUseCase` fills the local `platforms` table from IGDB's
`/platforms` endpoint, so the picker offers the whole catalogue instead of only what the user's saved
games happen to cover. `GetDiscoverFeedUseCase` reads the selection and narrows every Discover shelf with
it, so the setting is live. It also reads `GetTasteProfileUseCase` to build the personalised shelf, so
every use case in `usecase/discover/` now has a consumer.

**The feed reads the selection once per load, it does not observe it.** Change the picker and come back
to a Search screen whose ViewModel survived, and the shelves are still the old ones until something
re-triggers `loadDiscoverFeed()`. Making the feed a `Flow` was rejected for now: the ViewModel's
cancel-on-search / restore-on-clear logic is built around a suspend one-shot and the race it guards is
covered by tests written against that shape. Revisit when the ranking work reopens the same code.

**Settings holds only what has a backend.** The screen groups its rows by theme, and the only groups
that exist are the ones with data behind them. Notifications belong to Phase 3; a genre picker was
considered and dropped, since the taste profile infers genres from saved games and the user does not
edit them by hand; appearance has nothing to switch, because `:core:designsystem` is dark-only by
design. Do not add a row before the thing it configures exists.

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

**Personalisation is one extra shelf, not a reshuffle of the generic two.** The generic lanes are the
global popularity top-N; reordering them by taste only reorders what was already globally popular, which
is not what the user's profile says. So the taste profile buys its own query (`where genres = (...)`) and
its own shelf, titled with the reason. Reasons sit on the shelf rather than on each card: a 140dp cover
has no room for a sentence, and every game in the shelf is there for the same reason anyway.

Deliberate limits of the shelf as built, each one a place to extend rather than a bug:

- **One shelf, one signal — the top positive genre.** Every extra shelf is another network call on a
  screen the user opens constantly.
- **Developers are not a signal yet**, though `TasteProfile` documents them as the better predictor.
  Weights are normalised within their own map, so the top developer scores 1.0 whether it came from six
  saved games or one, and the profile carries no count to tell those apart. Giving `TasteProfile` raw
  counts is the prerequisite.
- **The shelf disappears rather than degrading**: below a minimum sample size, with no positive genre, on
  a failed fetch, or when pruning saved games and generic-shelf duplicates leaves too few entries. A
  half-empty personalised row next to two full generic ones reads as a loading bug.

**No rating-count floor — rank by confidence instead.** IGDB can sort by raw score but not by a score
weighted for how many people voted, so `sort total_rating desc` leads with whatever scores 100 across
three votes. Excluding thinly-rated games is the wrong fix: it drops every niche and newly released
title in the genre, which is what the shelf exists to surface, and does nothing about a mediocre game
that clears the floor. So the query keeps only a token floor and the use case re-ranks the whole pool by
a Bayesian average against a neutral prior. That is why the pool is far wider than the shelf: a narrow
one would already be filled by the games the ranking is meant to demote.

`RATING_CONFIDENCE_THRESHOLD` and `NEUTRAL_RATING` in `GetDiscoverFeedUseCase` are the knob — raise the
prior and thin gems climb, lower it and the shelf fills with established titles. **Both are reasoned
guesses about IGDB's rating distribution that have never been checked against a real pool.** Validate
them against live data before treating the shelf's quality as settled.

**An empty platform selection means no platform filter** — the feed omits the `where platforms = (...)`
clause entirely rather than substituting something. An earlier design filled the gap with the platforms
carried by the user's saved games. That was removed: it filtered the feed on a rule the user could neither
see nor explain, which is the opposite of the "every row states its reason" line above, and it did nothing
for the brand-new user it was meant to help, whose library is empty too. Do not reintroduce a fallback
here, and do not seed the selection behind the user's back either — the picker is one tap from every
top-level screen and holds the whole IGDB catalogue.

The filter lands on the `/games` hydrate call, never on `/popularity_primitives`, which returns a game id
and a score and has no platform field. The pool is therefore ranked before it can be filtered, so a
narrow selection thins an already-truncated list — that is what the widened pool limit in
`GameRepositoryImpl` pays for. Any further filter added to a lane inherits the same problem.

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
