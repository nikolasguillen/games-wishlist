# Implementation Plan: Unannounced upcoming games in the anticipated lane

**Branch**: `001-tba-anticipated-lane` | **Date**: 2026-09-25 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-tba-anticipated-lane/spec.md`

## Summary

The Discover feed's "Most anticipated" shelf filters unreleased games on `first_release_date > now`,
which silently drops every announced-but-undated title — exactly the games most likely to be genuinely
anticipated. The fix is a single `where`-clause branch in `GameRepositoryImpl.fetchPopularityRankedGames`
(the method shared by both Discover lanes, already parameterized by `upcomingOnly: Boolean`): admit a game
with no `first_release_date` when it carries at least one `release_dates` row with IGDB's
`date_format = 7` ("to be announced") scalar. The popular lane's branch, the method's two other callers,
and every domain model and interface signature are untouched. No UI code changes: `Game.releaseDate` is
`null` for every game this admits by construction, and the existing "unknown release date" fallback
already renders that correctly. Verified against the live IGDB API before committing to this design — see
`research.md`.

## Technical Context

**Language/Version**: Kotlin 2.4.10 (JVM toolchain 21)

**Primary Dependencies**: Retrofit 3 + Moshi (`IgdbApiService`, unchanged — no new endpoint, no new
request/response model). No new dependency added.

**Storage**: N/A. Discover lane results are not persisted (`GameRepositoryImpl`'s own comment: "Nothing is
persisted -- these are catalogue results, not the user's games"). No Room entity, no schema change.

**Testing**: JUnit4 + MockK + `kotlinx-coroutines-test`, extending the existing
`core/data/src/test/.../repository/GameRepositoryImplPopularGamesTest.kt` with the query-string assertion
style already used there (`RequestBody.asText().contains(...)`).

**Target Platform**: Android (minSdk 29) — same as the rest of the app. The change is entirely
server-query-side; no platform-specific behavior is introduced.

**Project Type**: Existing modular Android app. Single module touched: `:core:data`.

**Performance Goals**: No new network round-trip. The filter change lengthens the `where` clause of the
`/games` hydrate call the upcoming lane already makes; the two-call shape (`/popularity_primitives` then
`/games`) is unchanged, and so is `POPULARITY_POOL_LIMIT_UPCOMING`.

**Constraints**: The apicalypse fragment must compose safely with the existing `&`-joined clause
(id list, noisy-type exclusion, `version_parent`, `cover != null`, platform filter) without changing their
meaning. Verified live — see `contracts/upcoming-lane-release-filter.md`.

**Scale/Scope**: One modified private method (`fetchPopularityRankedGames`), one new private constant, one
modified test file. No new files in `:core:data/src/main`. No new module, no new public API, no DI change.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Check | Result |
|---|---|---|
| I. Module Boundaries | Change is entirely inside `:core:data` (`GameRepositoryImpl`, a private method). No new dependency, no new module edge. | PASS |
| II. Typed Errors Cross Layers | `fetchPopularityRankedGames` already wraps the whole fetch in `try`/`catch` → `AppResult.failure(e.toRepositoryError())`. Unchanged. | PASS |
| III. UI Renders, Doesn't Decide | No ViewModel, UseCase, mapper, or composable changes. The admission decision is IGDB-side (a `where` clause), not app logic reachable from the UI layer. | PASS (N/A — no UI touched) |
| IV. Reuse the Shared Layer | No new component, modifier, `dp` literal, or color. N/A. | PASS (N/A) |
| V. Verification Is Local | Plan specifies `./gradlew :core:data:testDebugUnitTest --console=plain -q` as the verification command, extending an existing test file. No CI, lint gate, or formatter assumed. | PASS |

No violations. Complexity Tracking is empty — see below.

*Post-Phase-1 re-check*: Data model and contract design (below) introduce no new entity, no new interface,
and no new module edge. All five gates re-checked against the final design and still PASS.

## Project Structure

### Documentation (this feature)

```text
specs/001-tba-anticipated-lane/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md         # Phase 1 output
├── quickstart.md         # Phase 1 output
├── contracts/
│   └── upcoming-lane-release-filter.md
└── tasks.md              # Phase 2 output (/speckit-tasks — not created by this command)
```

### Source Code (repository root)

Only one module is touched. No new files under `src/main`; the DAO/entity/schema tree, feature modules,
and every other `:core:*` module are untouched.

```text
core/data/
├── src/main/java/com/nikolasguillen/questlog/core/data/repository/
│   └── GameRepositoryImpl.kt          # MODIFIED: fetchPopularityRankedGames's releaseFilter,
│                                       #  one new private constant (IGDB TBD date_format scalar)
└── src/test/java/com/nikolasguillen/questlog/core/data/repository/
    └── GameRepositoryImplPopularGamesTest.kt   # MODIFIED: new cases per the contract's truth table
```

**Structure Decision**: No structural change. This is a single-file behavioral change inside the existing
`:core:data` repository implementation, following the module's established pattern of one shared private
method branching on a boolean flag (`upcomingOnly`) for the two Discover lanes. No new module, no new
package, no new public type.

## Complexity Tracking

*No entries — the Constitution Check above reports no violations.*
