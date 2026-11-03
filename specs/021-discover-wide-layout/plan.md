# Implementation Plan: Discover Wide Layout

**Branch**: `021-discover-wide-layout` | **Date**: 2026-11-03 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/021-discover-wide-layout/spec.md`

## Summary

On a window at least 840dp wide, the Search screen's idle state (the Discover feed) switches to a layout of its
own, following variant A of the desktop mockup:
- a tall full-width hero banner with the kicker, title, a `developer · date` meta line, and two in-banner actions
  ("Add to wishlist", "View details");
- the estimate footnote under it;
- shelves as fixed rows that hold as many 3:4 cards as fit, up to the six a shelf carries, each with a "See all"
  link;
- cards that show cover, title, developer, heart, and a score or release badge.

Below 840dp nothing changes.

The technical approach is a second, separate set of composables (`WideDiscoverFeed`, `WideDiscoverHero`,
`WideDiscoverShelfCard`) next to the untouched compact ones. One dispatch point in `SearchMainContent` chooses
between them on an `isWideWindow` flag. `:shared` passes that flag from the `twoPaneEnabled` value it already
computes. Both layouts share the hoisted `LazyListState` and the same item keys, so a resize keeps the first visible
shelf. Both render the same `DiscoverContentState`, so nothing reloads.

The only state change is one additive field, `DiscoverContentState.Content.heroReleaseText`, for the estimate mark.
There is no ViewModel, domain, data, network or database change.

## Technical Context

**Language/Version**: Kotlin 2.4.20, Compose Multiplatform 1.12.1 (Material 3, JetBrains 1.12.0-alpha03)

**Primary Dependencies**: Compose foundation/material3 and Coil 3, already in `:feature:search`. Public `:core:ui`
components: `GenericGameCardLayout`, `GameCoverImage`, `SaveToWishlistButton`, `RatingBadge`, `CustomTextButton`,
`ScrollToTopFabDefaults`, `fadingEdge`. **No new library and no new module edge** (research R1).

**Storage**: N/A. No persistence change, and the database stays at `version = 1` untouched.

**Testing**: JUnit4 in `feature/search/src/androidHostTest`: a new `WideDiscoverLayoutTest` for the pure layout
rules and new cases in `DiscoverMapperTest`. Layout is verified by running the desktop app and a resizable Android
emulator ([quickstart.md](./quickstart.md)).

**Target Platform**: Android (minSdk 29) tablets and large windows, desktop (`:desktopApp`, JVM), and iOS 16+ iPads
through the same `commonMain` code. Phones are unaffected.

**Project Type**: Kotlin Multiplatform mobile and desktop app, multi-module.

**Performance Goals**: Scrolling at the platform frame rate with every cover loading, as today. A swap across 840dp
is a recomposition with no fetch (SC-005).

**Constraints**:
- Layout only (spec).
- The owner's constraint: a separate set of wide composables, the size class read once, the same ViewModel and
  state.
- `commonMain` only, with no `android.*` or `java.*`.
- Rows never scroll sideways (clarify Q4).
- The decision follows the window, not the pane (clarify Q3).

**Scale/Scope**:
- 1 feature module (`:feature:search`) plus a one-argument change in `:shared`.
- About 5 new files (3 composable files, 1 helper file, 1 moved composable), 5 new strings and 2 test files
  touched.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Check | Result |
|---|---|---|
| **I. Module boundaries** | `:feature:search` still depends only on the six allowed `:core:*` modules. `:shared` → `:feature:search` already exists; only a `Boolean` argument is added. No new module and no new edge. | ✅ Pass |
| **II. Typed errors** | No error path changes. The error page and its retry are reused as-is. | ✅ Pass (N/A) |
| **III. The UI renders, it does not decide** | The hero's release text and estimate mark are computed in the mapper (`toDiscoverContentState`), not in a composable. The column count and the "See all" rule are pure, tested functions; the composable applies them to its measured width, as `SearchResultGridCells` does. The score-or-release badge choice follows `VerticalGameCard`'s `rating > 0` presentation check. No filtering or sorting is added. New text is in `strings.xml` and reaches state as `UiText`. Flows are still collected with `collectAsStateWithLifecycle()`, unchanged. | ✅ Pass |
| **IV. Reuse the shared layer** | The wide card and hero are built from the public `:core:ui` pieces listed above. Loading, error and placeholder pages are reused. The refresh prompt is moved, not copied. Gaps and paddings use `MaterialTheme.spacing`. The new dimensions are named layout constants beside their code, the existing precedent (`MIN_CARD_WIDTH`, `HERO_MAX_HEIGHT`); none is an inline literal. There is no dialog, and no `AlertDialog` or `TextButton`. Light/dark comes from the theme, and scrim text is fixed white as on every cover card. | ✅ Pass |
| **V. Local verification** | Real commands only (quickstart §1). New logic gets JVM tests in its module. No CI or lint gate is assumed. | ✅ Pass |
| **KMP constraints** | `commonMain` only, with no `expect`/`actual`, no platform contract and no library swap. iOS is not verifiable on Windows and is stated as such. | ✅ Pass |
| **Development workflow** | One composable unit per file in `components/`. One type per file (no new type). `internal` by default. A cross-module `Res` is aliased `CoreUiRes`. Previews use `@QuestLogPreviews` plus a wide `@Preview(widthDp = 1280, heightDp = 800)` where width matters, the precedent of `ExpandedSearchHome`. | ✅ Pass |

**Guidance tension, resolved by the owner**: `feature/CLAUDE.md` says a component choosing a layout by shape reads
the space it was given, not the window. Here the *screen* dispatches on the window's width class. The owner chose
this explicitly (the constraint in the brief, and clarify Q3), so the rule is amended in the same change to cover a
screen with a separate wide layout (research R9). It is not a constitution violation, so there is nothing for
Complexity Tracking.

**Post-design re-check (after Phase 1)**: no change. The design adds one `UiText?` field, five strings, five files
in `components/` and one `Boolean` parameter across the `:shared` boundary. All gates still pass.

## Project Structure

### Documentation (this feature)

```text
specs/021-discover-wide-layout/
├── spec.md              # /speckit-specify + /speckit-clarify
├── plan.md              # This file
├── research.md          # Phase 0: R1–R10
├── data-model.md        # Phase 1: the one state field, constants, keys, strings
├── quickstart.md        # Phase 1: validation guide
├── contracts/
│   └── ui-contract.md   # Phase 1: composable signatures and the :shared boundary
├── checklists/
│   └── requirements.md  # spec quality checklist
└── tasks.md             # Phase 2 (/speckit-tasks, not created here)
```

### Source Code (repository root)

```text
shared/src/commonMain/kotlin/com/nikolasguillen/questlog/shared/
└── QuestLogNavDisplay.kt                 # pass isWideWindow = twoPaneEnabled to SearchScreen

feature/search/src/commonMain/
├── kotlin/com/nikolasguillen/questlog/feature/search/
│   ├── SearchScreen.kt                   # + isWideWindow, threaded to SearchMainContent
│   ├── components/
│   │   ├── SearchMainContent.kt          # + isWideWindow; DiscoverContent dispatches compact | wide
│   │   ├── DiscoverFeed.kt               # compact, unchanged in behaviour: uses the shared item keys and
│   │   │                                 #   the moved refresh prompt; KDoc corrected (overlay, not stickyHeader)
│   │   ├── DiscoverRefreshPrompt.kt      # NEW (moved from DiscoverFeed.kt, private → internal)
│   │   ├── DiscoverFeedItemKeys.kt       # NEW: the item keys both feeds use
│   │   ├── WideDiscoverFeed.kt           # NEW: wide LazyColumn + private WideDiscoverShelf
│   │   ├── WideDiscoverHero.kt           # NEW: tall banner, kicker, title, meta line, two actions
│   │   ├── WideDiscoverShelfCard.kt      # NEW: 3:4 card, heart, score or release badge
│   │   └── WideDiscoverLayout.kt         # NEW: constants + wideShelfColumnCount, wideShelfShowsSeeAll,
│   │                                     #   wideHeroHeight (pure)
│   ├── mapper/DiscoverMapper.kt          # + heroReleaseText (TBA / "<year> (est.)" / formatted date)
│   └── model/DiscoverContentState.kt     # Content + heroReleaseText: UiText? = null
└── composeResources/values/strings.xml   # + 5 strings (data-model.md)

feature/search/src/androidHostTest/kotlin/com/nikolasguillen/questlog/feature/search/
├── components/WideDiscoverLayoutTest.kt  # NEW
└── mapper/DiscoverMapperTest.kt          # + heroReleaseText cases

feature/CLAUDE.md                         # amend the "reads the space it was given" rule (research R9)
```

**Structure Decision**: Everything lives in the existing `:feature:search` module, in its established
`components/`, `mapper/` and `model/` layout. The wide composables sit beside the compact ones and share none of
their size logic. `:shared` changes by one argument, and no other module is touched.

**Build order** (each step compiles and keeps `.\gradlew.bat test` green, so the work can pause anywhere):

1. **Groundwork, no visible change**: move `DiscoverRefreshPrompt` to its own file, and extract the item keys (giving
   the hero its key). Compact behaviour is identical.
2. **State**: add `heroReleaseText` and its mapper rule, with the `DiscoverMapperTest` cases and the new strings.
3. **Layout rules**: add `WideDiscoverLayout.kt` and `WideDiscoverLayoutTest`.
4. **Wide composables**: `WideDiscoverShelfCard`, `WideDiscoverHero`, `WideDiscoverFeed`, with previews. Nothing
   calls them yet.
5. **Dispatch**: `isWideWindow` through `SearchScreen` → `SearchMainContent` → `DiscoverContent`, then
   `QuestLogNavDisplay` passes `twoPaneEnabled`. `:androidApp:assembleDebug` is run here.
6. **Docs and verification**: amend `feature/CLAUDE.md`, run quickstart §2–3, and drop the backlog mouse-wheel item
   only if verified fixed.

## Open points for the owner

1. **Quarter-precision dates are not marked "(est.)"** (research R6). The feed has no date precision, so the plan
   marks only year-only dates and TBA. A "Q4 2026" game shows its placeholder day, as the compact hero already does.
   Marking quarters needs the lane query and cache to carry `date_format`, which is a data change for its own spec.
   **Proposed**: accept for this feature.
2. **Genre**: the spec's reason was inaccurate (research R7). The feed's domain games do carry genres; only the UI
   model drops them. Your decision (no genre) is unchanged. Adding it later would be a `:core:ui` mapper change, not
   a data change.

## Complexity Tracking

No constitution violations. Nothing to justify.
