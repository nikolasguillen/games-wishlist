# Roadmap

Planned features, in the order they are meant to be built, plus the decisions already taken so they are
not re-litigated in a later session. This file describes **what is not built yet**; the moment a phase
ships, delete it from here — `git log` is the history.

Written 2026-08-20. Phases 1 (Discover feed), 2 (Radar timeline, saved games only) and 3 (release
notifications) have shipped; what's left starts at Phase 4.

## Settings holds only what has a backend

The screen groups its rows by theme, and the only groups that exist are the ones with data behind them.
A genre picker was considered and dropped, since the taste profile infers genres from saved games and the
user does not edit them by hand. Do not add a row before the thing it configures exists.

## Open items in the shipped Discover feed

- `RATING_CONFIDENCE_THRESHOLD` and `NEUTRAL_RATING` in `GetDiscoverFeedUseCase` are the knob — raise the
  prior and thin gems climb, lower it and the shelf fills with established titles. **Both are reasoned
  guesses about IGDB's rating distribution that have never been checked against a real pool.** Validate
  them against live data before treating the shelf's quality as settled.
- Suggestion cap in the search-bar overlay is 4, so `sort hypes desc` is aggressive — an obscure title can
  be squeezed out by hyped ones sharing a substring. If that becomes annoying, sort by name-match quality
  rather than raising the cap.

## Phase 4 — Suggestions lane in Radar

Fold the taste profile into the timeline: saved games get the visual accent, suggestions sit in a minor
tone alongside them.

## iOS follow-ups after the multiplatform migration

Spec `specs/010-kmp-migration` ships iOS without these two features; their entry points are hidden there.
Both already sit behind a contract in shared code, so each is a new iOS implementation and nothing else.

- **Release reminders on iOS**: an iOS implementation of `ReleaseRefreshScheduler` (background refresh),
  local notifications and their permission prompt, and the Settings rows that configure them.
- **On-device description translation on iOS**: an iOS implementation of `GameDescriptionTranslator`.
  Needs a design decision first, since ML Kit's GenAI client has no iOS counterpart (see below).

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
