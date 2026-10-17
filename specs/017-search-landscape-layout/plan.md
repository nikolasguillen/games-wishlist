# Implementation Plan: Search Landscape Layout

**Branch**: `017-search-landscape-layout` | **Date**: 2026-10-16 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/017-search-landscape-layout/spec.md`

## Summary

When the window is wider than it is tall:
- the app's top-level navigation moves from the bottom bar to a vertical rail on the start edge;
- Search's results grid shows as many portrait-sized cards per row as fit, at least three;
- the rest of Search (Discover hero, the sheets, cutouts) is adjusted so nothing is clipped.

Portrait is unchanged, down to the pixel.

Technically:
- **Two inputs, two questions.** Search's grid asks "wider than tall?" through `isLandscapeWindow()` in
  `:core:ui` (`LocalWindowInfo`'s container size, `width > height`). The navigation component asks "how big?"
  through `currentWindowAdaptiveInfo().windowSizeClass` (R1, R2).
- **A navigation suite wraps the nav display.** `QuestLogRoot` becomes `QuestLogNavigationSuite { QuestLogNavDisplay }`:
  Material's `NavigationSuiteScaffold` with a bar on a portrait phone and a rail on every other window, hidden on
  non-top-level routes. It replaces the old app-level `Scaffold`, so `QuestLogNavDisplay` takes no `innerPadding`
  (R2, R3). Adds two dependencies to `:shared`: `material3-adaptive-navigation-suite` and `adaptive`.
- **One destination list.** A `TopLevelDestination` enum in `:shared` holds the three destinations, iterated once
  by the suite; items take `navBarItemColors` and a new `navRailItemColors` token (R2).
- **A grid that counts its columns.** `SearchResultGrid` reads its own width. A pure
  `resultGridColumnCount` gives 2 in portrait, and in landscape `max(3, as many 150dp cards as fit)`. A custom
  `GridCells` widens the edge cells by the edge inset, so every card is the same width at any count and
  portrait geometry is exactly today's (R4, R5).
- **Small fixes so nothing is clipped.**
  - The hero is capped at 40% of the window height (R7).
  - Search's content and the shared header add the horizontal cutout to their insets (R8).
  - The sort sheet can scroll (R10).
  - The expanded search overlay already covers the rail, and the spec now says so (R6).

## Technical Context

**Language/Version**: Kotlin 2.4.20, JVM toolchain 21, Java 11 bytecode

**Primary Dependencies**: Compose Multiplatform 1.12.1 (Material 3: JetBrains 1.12.0-alpha03 on iOS,
Jetpack 1.5.0-beta01 on Android; `NavigationRail`, `NavigationRailItem` and `NavigationRailItemDefaults`
exist in both), Compose foundation (`LazyVerticalGrid`, `GridCells`, `BoxWithConstraints`, `WindowInsets`),
Navigation 3. **No new dependency.**

**Storage**: None. Nothing is persisted, and there is no schema change.

**Testing**: JUnit4 in `feature/search/src/androidHostTest` for the pure grid geometry (research R12). No
Compose UI tests exist in the project (`docs/tech-debt.md`), so the visual checks are the hands-on matrix
in [quickstart.md](quickstart.md) §3. iOS is checked on a Mac (§4).

**Target Platform**: Android (minSdk 29, compile/targetSdk 37) and iOS 16+. All of the change is in
`commonMain`, with no platform-specific code.

**Project Type**: Kotlin Multiplatform mobile app (20 Gradle modules)

**Performance Goals**: No extra frame on rotation. Column count and hero height are arithmetic on values
already in composition. `BoxWithConstraints` adds one subcomposition around the results grid only.

**Constraints**:
- No new module, route, dependency or dependency edge.
- No ViewModel, `UiState` or `UiEvent` change.
- No `android.*`/`java.*` in `commonMain`.
- No new `dp` literal at a call site: the 150dp minimum card width and the 0.4 hero fraction are named
  constants next to the code that uses them, as `heroHeight` and `MIN_TOUCH_TARGET_HEIGHT` already are.
- No new user-facing text: the rail reuses the bar's three strings (FR-015).

**Scale/Scope**:
- 4 modules: `:core:ui`, `:core:designsystem`, `:shared`, `:feature:search`.
- 4 new source files, about 9 changed, 1 new test file, 2 `CLAUDE.md` inventory lines, and a 1-scenario
  spec amendment.

No unknowns remain. Every design question is answered in [research.md](research.md) (R1–R12).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-checked after Phase 1 design. Both pass.*

| Principle | Check | Result |
|-----------|-------|--------|
| **I. Module boundaries** | Edges used: `:shared` → `:core:ui`, `:core:designsystem`, `:core:navigation`; `:feature:search` → `:core:ui`, `:core:designsystem`. All of them exist already. Navigation stays in `:shared`: the rail, the destination enum and the bar/rail switch live there, and no feature module learns about the rail. `:core:model` is untouched. **No new module, route or edge.** | ✅ |
| **II. Typed errors** | No error path is added or changed. | ✅ |
| **III. UI renders, it does not decide** | Nothing here filters or sorts data. Column count, hero height and bar-vs-rail are presentation geometry, derived during composition from the window, which is what the UI layer owns. The column-count rule is a pure function with JVM tests, not logic buried in a composable. State is still collected with `collectAsStateWithLifecycle()`. No text is hardcoded: the rail reuses `search_nav_bar_item`, `radar_nav_bar_item` and `lists_nav_bar_item`. | ✅ |
| **IV. Reuse first** | Reused: `MainScreenHeader`, `VerticalGameCard`, `CustomChipRow`, `ScrollToTopFab`, `CustomModalBottomSheet`, `MaterialTheme.spacing.large` for every gap and inset, `appColors.navBarContainerColor` and the bar's indicator and icon colors. Nothing in `core/ui/component/` or `util/modifiers/` covers a rail or a window-shape check, so `isLandscapeWindow()` is added to `core/ui/util/` because two modules need it. The rail's colors are added to `AppComponentsColors`, not inlined. No `isSystemInDarkTheme()` is added. | ✅ |
| **V. Local verification** | `:feature:search:testAndroidHostTest`, `:shared:compileAndroidMain`, `./gradlew test`, `:androidApp:assembleDebug`. iOS: `:shared:linkDebugFrameworkIosSimulatorArm64` + quickstart §4 on a Mac, recorded as not run until then. No CI or lint gate is assumed. | ✅ |
| **Persistence** | None. The database version is not touched. | ✅ |
| **Injection** | No Koin change. `KoinGraphTest` is unaffected. No `SavedStateHandle`. | ✅ |
| **KMP** | Everything is in `commonMain`. No platform capability is involved, so there is no `contracts/` and no change to `platform-contracts.md`. | ✅ |
| **One type per file / naming** | `TopLevelDestination.kt` (enum), `QuestLogNavigationSuite.kt` (composable), `SearchResultGridCells.kt` (one class plus the top-level pure functions it uses), `WindowShape.kt` (one function). `TopLevelDestination` is an identifier set, like `SearchSort`, so it takes no `UiModel` suffix. | ✅ |
| **Previews** | None for `QuestLogNavigationSuite`: it wraps the whole nav display and needs a back stack and Koin. |  ✅ |

**Changes to existing behavior or code, called out** (none violates a principle, but each reaches beyond the
new path):
1. `MainScreenHeader` (`:core:ui`, shared by Search, Radar and Lists) adds the horizontal display cutout to
   its insets (R8). In portrait the horizontal cutout is zero, so nothing moves. In landscape Radar's and
   Lists' headers also keep the profile button clear of the camera.
2. `QuestLogBottomBar` and the app-level `Scaffold` are removed in favor of `QuestLogNavigationSuite`. The bar's
   look, order and click behavior are unchanged; the rail is the same component family at 80dp.
3. The results grid's per-item padding changes from `index % 2` to the column-aware rule of R5. With two
   columns the geometry is identical.
4. `DiscoverHero`'s height becomes `min(240.dp, windowHeight × 0.4)`. It is unchanged on any portrait phone.
5. Spec US3 acceptance scenario 2 was amended. The expanded search overlay is a full-screen dialog and
   covers the rail, as it covers the bar in portrait (R6).

## Project Structure

### Documentation (this feature)

```text
specs/017-search-landscape-layout/
├── spec.md               # amended: US3 scenario 2 (R6)
├── plan.md               # this file
├── research.md           # R1–R12
├── data-model.md         # derived layout values; no persisted data
├── quickstart.md         # automated checks + 22-row hands-on matrix + iOS pass
├── checklists/requirements.md
└── tasks.md              # /speckit-tasks (not created here)
```

There is no `contracts/`: no platform capability, no repository or use-case contract, and no external
interface. The only cross-module surface is `isLandscapeWindow()`, specified in data-model.md.

### Source Code (repository root)

```text
core/ui/src/commonMain/kotlin/com/nikolasguillen/questlog/core/ui/
├── util/WindowShape.kt                          NEW   isLandscapeWindow(): width > height of LocalWindowInfo's container
└── component/MainScreenHeader.kt                EDIT  row insets ∪ displayCutout.only(Horizontal)

core/designsystem/src/commonMain/kotlin/com/nikolasguillen/questlog/core/designsystem/theme/
└── AppComponentsColors.kt                       EDIT  + navRailItemColors (same indicator / selected icon as the bar)

shared/src/commonMain/kotlin/com/nikolasguillen/questlog/shared/
├── TopLevelDestination.kt                       NEW   enum SEARCH / RADAR / LISTS: route, icon, label, isSelected(route)
├── QuestLogNavigationSuite.kt                   NEW   NavigationSuiteScaffold: bar on a portrait phone, rail otherwise;
│                                                      hidden on non-top-level routes
└── QuestLogRoot.kt                              EDIT  QuestLogNavigationSuite { QuestLogNavDisplay }; the shared "is top-level
                                                       route" check and navigate lambda
(QuestLogBottomBar.kt                         DELETED)

feature/search/src/commonMain/kotlin/com/nikolasguillen/questlog/feature/search/
├── SearchScreen.kt                              EDIT  Scaffold contentWindowInsets ∪ displayCutout.only(Horizontal)
└── components/
    ├── SearchResultGridCells.kt                 NEW   MIN_CARD_WIDTH; resultGridColumnCount(); SearchResultGridCells
    │                                                  (edge-aware GridCells)
    ├── SearchResultGrid.kt                      EDIT  BoxWithConstraints → column count; SearchResultGridCells;
    │                                                  column-aware edge padding; landscape preview
    ├── DiscoverHero.kt                          EDIT  height = min(240.dp, window height × HERO_MAX_HEIGHT_FRACTION)
    └── SearchSortBottomSheet.kt                 EDIT  options Column scrolls

feature/search/src/androidHostTest/kotlin/com/nikolasguillen/questlog/feature/search/components/
└── SearchResultGridCellsTest.kt                 NEW   cases below

CLAUDE.md                                        EDIT  "Where things live": the rail and TopLevelDestination next to the bar
core/ui/CLAUDE.md                                EDIT  utilities: isLandscapeWindow() in util/WindowShape.kt
```

**Structure Decision**: The existing layout is used unchanged. Navigation chrome stays in `:shared`, which
owns the bar today. The grid geometry stays in `:feature:search` beside the only grid that uses it. The one
rule two modules share, the window shape, goes to `:core:ui`, which both already depend on.

### `SearchResultGridCellsTest` cases

`resultGridColumnCount(availableWidth, spacing = 16, edge = 16, isLandscape)`, all in dp at `Density(1f)`:

1. Portrait is 2 at 320, 360, 412, 800 and 1280 widths, so a portrait tablet stays at two (FR-007).
2. Landscape at the worked examples of research R4 gives 560 → 3, 805 → 4, 654 → 3 and 918 → 5.
3. The landscape floor holds: 300 and 400 still give 3.
4. The boundary: the smallest width that gives 4 columns gives 4, and one dp less gives 3.

`SearchResultGridCells(columnCount, edgePadding).calculateCrossAxisCellSizes(available, spacing)`:

5. For every n from 2 to 6 and widths 300–1200, the sizes add up to `available − (n − 1)·spacing`.
6. The first and last cells exceed every middle cell by exactly `edgePadding`, give or take the one
   leftover pixel, and the visible card widths differ by at most 1px.
7. `n = 2` at 360 gives `[172, 172]` (card 156 after the 16dp edge padding), which is today's geometry.
8. A width that does not divide evenly spreads the leftover pixels one per cell from the start, so no pixel
   is lost and no cell grows by more than 1.

## Follow-ups (not in this feature)

- **Radar and Lists bodies in landscape**: they get the rail and the full height, but their content keeps
  `WindowInsets.systemBars`, so with the phone rotated the other way the end side can sit under the cutout.
  Only their headers are protected (R8).
- **Hero art in landscape**: the hero crops a portrait cover into a wide banner, and the landscape cap makes
  the crop tighter. A landscape artwork would fit better, but it would need a data change (R7).
- **Other two-column grids**: `WishlistGamesList` still uses `GridCells.Fixed(2)` with `index % 2` padding.
  `SearchResultGridCells` could serve it later, but the spec keeps it out of scope.
- **Game detail and the other full-screen routes in landscape**: untouched by this feature.

## Complexity Tracking

No constitution violations: no new module, dependency, edge, repository or route.
