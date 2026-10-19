# Implementation Plan: Two-Pane Search and Game Detail

**Branch**: `018-two-pane-search-detail` | **Date**: 2026-10-18 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/018-two-pane-search-detail/spec.md`

## Summary

On windows at least 840dp wide, a game opened from the Search flow is shown in a right-hand pane while Search stays
on the left with its state intact. Narrower windows and every other entry point behave exactly as today.

Technical approach: a hand-written Navigation 3 `SceneStrategy` in `:shared` renders `[Search | detail]` when the
window is expanded and the back stack ends in a run of Search-opened details sitting on `SearchRoute`. In every
other case it defers to the default single-pane strategy. `GameDetailRoute` gains a defaulted `fromSearch` flag so
that a Search-opened detail can be told apart from a Radar, wishlist or reminder-link one. A few pure back stack
functions in `:shared` replace the run instead of stacking it while the window is expanded, and close the whole run
on back. Navigation 3 keeps entry content movable between scenes, so the Search entry's whole composition (query,
filters, scroll) survives. Three fixes make the panes behave:

- the scene registers a back handler that outranks the Search pane's own (it would otherwise clear the search);
- the results grid decides "landscape" from its own space rather than the window's;
- the detail's back button becomes optional.

No new library and no new module edge. One existing catalog library becomes a direct dependency of `:shared`.

## Technical Context

**Language/Version**: Kotlin 2.4.20, Kotlin Multiplatform (`commonMain` only), Compose Multiplatform 1.12.1

**Primary Dependencies**: Navigation 3 (runtime 1.1.7, JetBrains UI 1.1.2: `SceneStrategy`/`Scene`); JetBrains
Material 3 adaptive 1.2.0 (`currentWindowAdaptiveInfo`, `WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND`); JetBrains
navigationevent-compose (`rememberNavigationEventDispatcherOwner`); Koin (unchanged)

**Storage**: N/A. The back stack is already saved through `GameNavSavedStateConfiguration`, and the new route field
has a default.

**Testing**: JUnit4 on the JVM (`androidHostTest`): `:core:navigation` (route round trip), `:shared` (back stack rules
and `KoinGraphTest`), `:core:ui` (`isLandscapeShape`). Layout and gesture behavior is verified by hand
([quickstart.md](./quickstart.md)).

**Target Platform**: Android (minSdk 29, targetSdk 37) and iOS 16+. Tablets, foldables, freeform and split-screen
windows, and large phones in landscape that reach 840dp.

**Project Type**: Mobile app (multi-module KMP)

**Performance Goals**: Opening, replacing and closing the detail is immediate, with no blink of the Search pane. No
extra recomposition of Search when the detail changes, because the scene key stays stable.

**Constraints**: Navigation logic only in `:shared`; feature modules stay unaware of panes; all text from string
resources (the feature adds none); no new module edge; Android must build and pass its tests at every commit.

**Scale/Scope**: About 2 new files and 3 edited files in `:shared`, 1 route field, 2 small `:core:ui` changes, a
nullable parameter threaded through 4 game-detail composables, and 1 line in the search grid. No change to any
ViewModel, use case or data layer.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Check | Result |
|---|---|---|
| **I. Module boundaries** | The scene, the back stack rules, the window-size read and the rail logic are all in `:shared`, which "is the only module that knows about navigation". The route change is in `Routes.kt`. Feature modules get a nullable `onBackClick` (game-detail) and a different landscape input (search). Neither references a route, a pane or another feature. **No new module edge.** `:shared` declares `navigationevent-compose` directly; it is already in the catalog and in the graph through `navigation3-ui`, `:feature:search` and `:feature:onboarding`. This is called out in Complexity Tracking. | ✅ PASS (one declaration called out) |
| **II. Typed errors** | No data-layer or error-path change. The detail's error state renders in the pane through the existing `ErrorPage`. | ✅ N/A |
| **III. UI renders, does not decide** | The back stack decisions are pure functions in `:shared`, not composables, and are unit-tested. Feature composables only drop a back button when handed `null`, and pick columns from a size they measure. No new UiState, no new text. | ✅ PASS |
| **IV. Reuse shared layer** | `isLandscapeShape` sits beside `isLandscapeWindow` in `:core:ui/WindowShape.kt`, so the landscape rule is still written once. Material's `VerticalDivider`, no new `dp` literal, no new component. `ImmersiveDetailLayout` is extended, not copied. | ✅ PASS |
| **V. Local verification** | Real commands only: `:core:navigation`, `:shared` and `:core:ui` `testAndroidHostTest`, `test`, `:androidApp:assembleDebug`, plus the iOS framework link on a Mac. New pure logic gets JVM tests. `KoinGraphTest` is unaffected (no new ViewModel). | ✅ PASS |
| **KMP constraints** | All `commonMain`; no `android.*`/`java.*`; no `expect`/`actual`; no new platform capability; no library swap. iOS verification is Mac-only and stated as such. | ✅ PASS |
| **Persistence / Injection** | No database or Koin change. `SavedStateHandle` is not introduced. The route argument still reaches `GameDetailViewModel` through `parametersOf(key.gameId)`. | ✅ N/A |
| **Workflow** | One declaration per file (`SearchDetailSceneStrategy.kt`, the scene class in its own file, `SearchDetailPanes.kt` with top-level functions). `internal` by default. English KDoc. `core/ui/CLAUDE.md`'s `WindowShape.kt` line updated in the same commit as `isLandscapeShape`. | ✅ PASS |

**Post-design re-check** (after Phase 1): unchanged. The contracts confirm that no feature module learns about panes
and that the only build change is the single declaration already listed. **Gate passes.**

## Project Structure

### Documentation (this feature)

```text
specs/018-two-pane-search-detail/
├── plan.md                          # This file
├── research.md                      # Phase 0: decisions R1–R10
├── data-model.md                    # Phase 1: route field, run, layout states
├── quickstart.md                    # Phase 1: build/test commands, scenarios S1–S9
├── contracts/
│   └── navigation-contract.md       # Phase 1: back stack rules, scene, signature changes
├── checklists/requirements.md       # from /speckit-specify
└── tasks.md                         # Phase 2 (/speckit-tasks, not created here)
```

### Source Code (repository root)

```text
core/navigation/src/
├── commonMain/.../core/navigation/Routes.kt                     # GameDetailRoute(gameId, fromSearch = false)
└── androidHostTest/.../RoutesSerializationTest.kt               # round trip with fromSearch = true

core/ui/src/
├── commonMain/.../core/ui/util/WindowShape.kt                   # + isLandscapeShape(width, height)
├── commonMain/.../core/ui/component/ImmersiveDetailLayout.kt    # onBackClick: (() -> Unit)?
├── androidHostTest/.../util/WindowShapeTest.kt                  # new: shape rule (square = portrait)
└── CLAUDE.md                                                    # WindowShape.kt line mentions both helpers

feature/game-detail/src/commonMain/.../feature/gamedetail/
├── GameDetailScreen.kt                                          # onBackClick: (() -> Unit)?
└── components/{GameDetailMainContent, DetailErrorLoadingWrapper, GameDetailSuccessContent}.kt

feature/search/src/commonMain/.../feature/search/components/
└── SearchResultGrid.kt                                          # isLandscapeShape(maxWidth, maxHeight)

shared/
├── build.gradle.kts                                             # + libs.jetbrains.navigationevent.compose
├── src/commonMain/.../shared/
│   ├── SearchDetailPanes.kt            # new: searchDetailRun, openSearchDetail, navigateBack (pure)
│   ├── SearchDetailSceneStrategy.kt    # new: the SceneStrategy + entry metadata markers
│   ├── SearchDetailScene.kt            # new: the two-pane Scene (panes, divider, insets, transitions)
│   ├── SearchDetailBackHandler.kt      # new: overlay-priority back handler for the scene
│   ├── QuestLogRoot.kt                 # twoPaneEnabled; rail visibility and highlighted route
│   └── QuestLogNavDisplay.kt           # sceneStrategies, onBack, Search/detail click rules, metadata, nullable back
└── src/androidHostTest/.../shared/SearchDetailPanesTest.kt      # new
```

**Structure Decision**: The existing multi-module layout, unchanged. All navigation and layout logic is added to
`:shared` next to `QuestLogNavDisplay`/`QuestLogRoot`. The other modules receive only the minimal, pane-agnostic API
changes listed in [contracts/navigation-contract.md](./contracts/navigation-contract.md).

## Implementation Order

Each step builds and passes `./gradlew test` on its own (constitution: every step must be one the work can pause on).

1. **Route**: add `fromSearch` to `GameDetailRoute` and extend `RoutesSerializationTest`. No behavior change yet.
2. **Pure rules**: add `SearchDetailPanes.kt` with `SearchDetailPanesTest`. It covers the run detection cases in
   [data-model.md](./data-model.md), replace versus push by `twoPaneEnabled`, the same-game guard, whole-run back,
   and a deep link on top of a run.
3. **Pane-agnostic component changes**: nullable `onBackClick` through game detail and `ImmersiveDetailLayout`;
   `isLandscapeShape` with its test; the search grid uses it; `core/ui/CLAUDE.md` updated. Every caller still passes
   a lambda, so there is no visible change except the grid in non-full-width spaces, which do not exist yet.
4. **Wire the rules without the scene**: Search and detail clicks create `fromSearch` routes and go through
   `openSearchDetail`/`navigateBack`, with `twoPaneEnabled` read in `QuestLogContent`. With no scene yet, an expanded
   window shows the detail full-screen with replace semantics. That is an intermediate state, still buildable.
5. **Scene**: `SearchDetailSceneStrategy` and `SearchDetailScene` (panes, divider, inset consumption, disabled
   back dispatcher on the left pane, stable key, immediate transitions); entry metadata markers; nullable back
   for `fromSearch` details in two-pane mode; rail visibility and highlight. Add the `navigationevent-compose`
   declaration to `:shared`.
6. **Verify**: `./gradlew test`, `:androidApp:assembleDebug`, quickstart S1–S8 on Android, and S9 plus the iOS link on
   a Mac when one is at hand.

## Decisions for the Owner

- **Large phones in landscape** ([research.md R3](./research.md#r3-the-840dp-threshold-and-where-it-is-read)): a
  phone that is at least 840dp wide on its side gets two panes under the rule as written. The plan follows the
  request's width-only threshold. Excluding those phones is one added height check in one place. This is easy to
  reverse either way.
- **Spec 017's grid rule** ([research.md R7](./research.md#r7-making-both-screens-fit-a-half-width-pane)): the results
  grid now takes its landscape cue from its own space instead of the window. That is identical whenever Search is
  full width, and it is what makes the half-width pane look right.

## Complexity Tracking

| Item | Why Needed | Simpler Alternative Rejected Because |
|---|---|---|
| `:shared` declares `libs.jetbrains.navigationevent.compose` directly (existing catalog library, already in the dependency graph; **no new module edge**) | The two-pane scene must outrank the Search pane's own back handler while the detail is showing ([research.md R5](./research.md#r5-the-system-back-gesture-while-both-panes-are-composed)). Otherwise back clears the search instead of closing the detail. | Passing a "pane" flag into `SearchScreen` would make `:feature:search` aware of navigation layout (Principle I). Relying on the transitive dependency without declaring it would hide the use. |
| New argument on an existing route (`GameDetailRoute.fromSearch`) | A reminder link opened over Search produces the same back stack shape as a Search-opened game but must stay full-screen. The origin has to be in the key ([research.md R2](./research.md#r2-telling-a-search-opened-detail-apart-from-the-others)). | A separate route type would need a second saved-state registration and a duplicated `entryProvider` branch. Inferring the origin from stack structure is wrong for the deep link. |
