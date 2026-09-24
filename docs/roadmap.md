# Roadmap

Planned features, in the order they are meant to be built, plus the decisions already taken so they are
not re-litigated in a later session. This file describes **what is not built yet**; the moment a phase
ships, delete it from here — `git log` is the history.

Written 2026-08-20. Phases 1 (Discover feed) and 2 (Radar timeline, saved games only) have shipped; what's
left starts at Phase 3.

## Settings holds only what has a backend

The screen groups its rows by theme, and the only groups that exist are the ones with data behind them.
Notifications belong to Phase 3; a genre picker was considered and dropped, since the taste profile infers
genres from saved games and the user does not edit them by hand; appearance has nothing to switch, because
`:core:designsystem` is dark-only by design. Do not add a row before the thing it configures exists.

## Open items in the shipped Discover feed

- `RATING_CONFIDENCE_THRESHOLD` and `NEUTRAL_RATING` in `GetDiscoverFeedUseCase` are the knob — raise the
  prior and thin gems climb, lower it and the shelf fills with established titles. **Both are reasoned
  guesses about IGDB's rating distribution that have never been checked against a real pool.** Validate
  them against live data before treating the shelf's quality as settled.
- **Cache**: do not dump discovered games into the `games` table unqualified. That table already doubles
  as a cache with ownership flags (`isWishlisted`, `lastViewedAt`); mixing in feed results makes "the
  user's own games" ambiguous. Use a separate entity holding the ordered id list plus a `fetchedAt` stamp.
- **TBA release dates.** The two generic lanes split on the release window (`first_release_date > now` for
  "Most anticipated", `<= now` for "Popular this month"), which drops games with a null
  `first_release_date`. Plenty of genuinely anticipated upcoming titles are still TBA, so they silently
  miss the anticipated shelf. Not just relaxing the filter: a bare `| first_release_date = null` also lets
  in old games whose date was never recorded, so we cannot tell "unannounced upcoming" from "date lost to
  history" without a date-precision signal. `DatePrecision` now exists (fed by `release_dates.date_format`,
  consumed by `ReleaseBucketResolver` for Radar) — the fix is no longer blocked, just not done.
- Suggestion cap in the search-bar overlay is 4, so `sort hypes desc` is aggressive — an obscure title can
  be squeezed out by hyped ones sharing a substring. If that becomes annoying, sort by name-match quality
  rather than raising the cap.

## Phase 3 — Release notifications

Opt-in per game ("Notify me"), driven by the same refreshed dates. This is the payoff Radar was built
for — a timeline the user must open is worth far less than a reminder that arrives on release day.

## Phase 4 — Suggestions lane in Radar

Fold the taste profile into the timeline: saved games get the visual accent, suggestions sit in a minor
tone alongside them. Only after Phase 3 is real.

## Decisions that would be expensive to reverse

Per the KMP section in the root `CLAUDE.md`:

- **Use `kotlinx-datetime`, not `java.time`.** A timeline feature spreads date math everywhere; rewriting
  it after a KMP move is exactly the work the project is trying to avoid.
- Room's destructive migration is deliberate while the app is unpublished — new entities for these phases
  wipe the device, and that is fine. See `docs/tech-debt.md`.
- **ML Kit's GenAI Prompt API (the on-device translation feature's Gemini Nano client) has no
  multiplatform counterpart.** What survives a KMP move is the `GameDescriptionTranslator` port in
  `:core:domain`; `:core:ai` is replaced wholesale, the same shape already used for scheduling —
  `core/domain/radar/ReleaseRefreshScheduler.kt` (contract) with the WorkManager implementation isolated
  in `core/data/scheduler/ReleaseRefreshSchedulerImpl.kt`.
