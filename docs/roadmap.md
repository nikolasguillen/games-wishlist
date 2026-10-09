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

- **Release reminders on iOS**: a background refresh for `ReleaseRefreshScheduler` (today it only runs at launch),
  local notifications and their permission prompt, and the Settings rows that configure them. The welcome flow's
  `RadarWithoutReminders` page goes away when reminders arrive.
- **On-device description translation on iOS**: an iOS implementation of `GameDescriptionTranslator`.
  Needs a design decision first, since ML Kit's GenAI client has no iOS counterpart (see below).
- **iOS distribution**: installing on a physical device (development team), signing and provisioning, and
  TestFlight. Spec 010 verifies on the simulator only.

## Decisions that would be expensive to reverse

- Room's destructive migration is deliberate while the app is unpublished — new entities for these phases
  wipe the device, and that is fine. See `docs/tech-debt.md`.
- **ML Kit's GenAI Prompt API (the on-device translation feature's Gemini Nano client) has no
  multiplatform counterpart.** `:core:ai` stays Android-only and reachable only from `:core:data`; what is shared is
  the `GameDescriptionTranslator` port in `:core:domain`, and iOS binds `UnsupportedGameDescriptionTranslator`
  until it gets a translator of its own. Scheduling has the same shape: `ReleaseRefreshScheduler` is the
  contract, WorkManager implements it on Android and `InProcessReleaseRefreshScheduler` (once per 24 h, at launch)
  on iOS.
