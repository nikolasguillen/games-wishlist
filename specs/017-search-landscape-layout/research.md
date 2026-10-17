# Research: Search Landscape Layout

**Feature**: [spec.md](spec.md) | **Plan**: [plan.md](plan.md) | **Date**: 2026-10-16

Every decision below was checked against the source on `017-search-landscape-layout` and against the
Material 3 sources the build resolves (JetBrains `material3` 1.12.0-alpha03, unpacked from the Gradle cache).

---

## R1 — What "landscape" is, and who decides it

**Decision**: Two separate questions, two separate answers.

- **Search's grid** (columns, hero height) asks "is the window wider than tall?". One `@Composable` helper in
  `:core:ui`, `isLandscapeWindow()` (`core/ui/util/WindowShape.kt`), is the only place that rule is written:
  `LocalWindowInfo.current.containerDpSize`, width **strictly greater** than height. A square window counts as
  portrait.
- **The navigation component** (bar or rail) asks "how big is the window?", through
  `currentWindowAdaptiveInfo().windowSizeClass` (see R2). It does not use `isLandscapeWindow()`.

**Rationale**:
- `containerDpSize` is the window's size, so a split-screen half that is narrow and tall stays portrait.
  It is already read in `commonMain` (`CustomAlertDialog`, `SearchFilterBottomSheet`,
  `GameDetailSuccessContent`), so it costs no dependency, and `:feature:search` already depends on `:core:ui`.
- The grid has to keep two columns on a portrait tablet (FR-007), which a size class cannot express: a
  portrait tablet and a landscape phone share the width class. Shape is the right input there.
- On Android the activity is recreated on rotation, so the value is fresh on the first frame. On iOS the
  window info is snapshot state that `ComposeUIViewController` updates on resize.

**Alternatives considered**:
- Size classes for the grid as well: would give a portrait tablet 3+ columns. Rejected for the grid only.
- `LocalConfiguration.orientation`: Android-only, cannot be in `commonMain`. Rejected.

---

## R2 — The navigation component

**Decision**: Material 3's `NavigationSuiteScaffold` (`material3-adaptive-navigation-suite`, with `adaptive` for
`currentWindowAdaptiveInfo()`), wrapped in `shared/.../QuestLogNavigationSuite.kt`. It wraps `QuestLogNavDisplay`
and takes the place of the old `Scaffold` plus `QuestLogBottomBar`.

- **Which component**: `layoutType` is `NavigationBar` when the window is width-compact (< 600dp) **and not**
  height-compact (>= 480dp), and `NavigationRail` otherwise. So a portrait phone (and a split-screen half of
  one) keeps the bar; a phone on its side, a tablet and an unfolded foldable get the rail. The baseline
  `NavigationBar`/`NavigationRail` are used, not the Expressive short/wide variants, so the look is unchanged.
- **Not the default rule**: `NavigationSuiteScaffoldDefaults.navigationSuiteType` keeps a bar on a
  height-compact window, which is the phone-on-its-side case that needs the rail.
- **Hide and show**: a hoisted `NavigationSuiteScaffoldState`, driven from "the top of the back stack is
  `SearchRoute`, `RadarRoute` or `ListsRoute`". The scaffold animates the slide and resizes its content.
- **Destinations**: one `TopLevelDestination` enum in `:shared` (route, icon, label, which routes count as
  selected), iterated once by `QuestLogNavigationSuite`. The click guard
  (`currentRoute != destination.route`) and the pop-to-root lambda stay in `QuestLogRoot`.
- **Colors**: the container is `MaterialTheme.appColors.navBarContainerColor` for both components; items take
  `AppComponentsColors.navBarItemColors` and `navRailItemColors`.

**Rationale**:
- It is the standard Material answer to "bar or rail", and it owns the layout, the hide animation and the
  insets that the first version of this feature wrote by hand (a `Row`, an `AnimatedVisibility`, and a
  `consumeWindowInsets` on the Scaffold).
- It consumes only the `Bottom` (bar) or `Start` (rail) side of the insets, which is what the hand-written
  version had to arrive at (T009).
- On Android the dependency resolves to Jetpack's `material3-adaptive-navigation-suite` at the same
  `1.5.0-beta01` the rest of Material 3 uses. On iOS it is the JetBrains build at `1.12.0-alpha03`.

**Costs, accepted**:
- The stock rail lays its items out from the top; the first version centered them. The suite gives no slot
  for that.
- A portrait tablet now gets a rail, where the first version kept the bar (shape rule). It follows Material's
  guidance for medium and wider windows.
- Two new dependencies in `:shared`: `material3-adaptive-navigation-suite` and `adaptive`.

**Alternatives considered**:
- Hand-written `Row` + `NavigationRail` beside the `Scaffold`, switched by `isLandscapeWindow()`: the first
  implementation. Replaced by the above.
- `WideNavigationRail`: its look does not match the baseline bar. Rejected for this feature.

---

## R3 — Root layout and window insets

**Decision**: `QuestLogContent` is `QuestLogNavigationSuite { QuestLogNavDisplay(...) }`. There is no app-level
`Scaffold` any more: it existed only to host the bottom bar and hand its height down as `innerPadding`, and the
suite now sizes its content directly, so `QuestLogNavDisplay` takes no `innerPadding`.

**Rationale**:
- The rail's default insets are `systemBarsForVisualComponents.only(Vertical + Start)`. On Android that
  already includes the camera cutout (checked on a punch-hole emulator in both landscape rotations: the rail
  grows to cover it and its items stay clear). On iOS the default is `systemBars` only.
- The suite consumes the side it uses, so Search's Scaffold, Radar's and Lists' (`WindowInsets.systemBars`)
  and `MainScreenHeader` do not pad it twice.
- While the component is hidden (every non-top-level route) it consumes nothing, so a detail screen gets the
  whole window, as before.
- In right-to-left layouts the rail is placed at the start edge by the scaffold.

**Alternatives considered**:
- Keeping the Scaffold under the suite to supply `innerPadding`: it would always be zero. Rejected.

---

## R4 — How many columns the results grid shows

**Decision**: The column count is a pure function, `resultGridColumnCount(availableWidth, spacing,
isLandscape)`, in `feature/search/.../components/SearchResultGridCells.kt`:

- portrait → **2**, always (FR-007);
- landscape → `max(3, floor((availableWidth − 2·edge + spacing) / (MIN_CARD_WIDTH + spacing)))`, where
  `edge` and `spacing` are `MaterialTheme.spacing.large` (16dp), as today, and `MIN_CARD_WIDTH = 150.dp`
  is a feature-local constant (FR-008).

`availableWidth` is the grid's own width, read with `BoxWithConstraints`. That is the screen minus the rail
and any horizontal insets, not the window width.

**Rationale**: 150dp is the smallest width a two-column portrait card has on the phones the app targets
(156dp on a 360dp-wide phone). The rule therefore fits as many portrait-sized cards as the landscape row
holds. Worked examples (card width = `(W − 2·16 − (n−1)·16) / n`):

| Device (portrait dp) | Portrait card | Landscape grid width W | Columns | Landscape card | Ratio |
|---|---|---|---|---|---|
| 360×640 (small Android) | 156 | 640 − 80 rail = 560 | 3 | 165 | 1.06 |
| 412×915 (Pixel 7), 30dp cutout | 182 | 915 − 110 = 805 | 4 | 181 | 0.99 |
| 393×852 (iPhone 15), 59pt safe areas | 172 | 852 − 139 − 59 = 654 | 3 | 197 | 1.14 |
| 448×998 (large Android) | 200 | 998 − 80 = 918 | 5 | 164 | 0.82 |

Every row meets SC-001 (≥ 3 per row) and SC-002 (within 25% of portrait). The floor of 3 only matters for
windows narrower than about 560dp that are still wider than tall (split screen on a tablet). There the
cards get smaller rather than falling back to 2, which is what the spec's assumption asks for.

`VerticalGameCard` keeps its fixed 250dp height, so a landscape phone shows about one full row at a time.
The search header scrolls away (`enterAlwaysSearchBarScrollBehavior`), which gives the grid most of the
height back. Changing the card's height is out of scope: the spec asks for portrait-sized cards.

SC-002 is measured on phones. A tablet's portrait cards are already oversized at two columns. That is
pre-existing, and portrait is out of scope (spec assumption: no separate tablet design).

**Alternatives considered**:
- `GridCells.Adaptive(150.dp)` everywhere: a tablet or foldable in portrait would get 3–5 columns, which
  breaks FR-007 ("portrait keeps two"). Rejected.
- `GridCells.Fixed(3)` in landscape: on a 918dp-wide grid the cards would be 285dp wide, 1.4× portrait,
  which fails SC-002. Rejected.
- A count derived from the window width: wrong by the rail's width and the insets. Rejected in favor of the
  grid's own constraints.

---

## R5 — Keeping every card the same width with more than two columns

**Decision**: A custom `GridCells` implementation, `SearchResultGridCells(columnCount, edgePadding)`, in the
same file. It returns cell sizes in which the **first and last cells are wider by `edgePadding`**, and the
first and last cards pad themselves by that amount on their outer side (`index % columnCount`). Every
visible card is then exactly `(W − 2·edge − (n−1)·spacing) / n` wide. Any leftover pixels go one per cell
from the start, as `GridCells.Fixed` does.

**Rationale**:
- Today's grid insets the cards from the screen edge by padding the left card's start and the right
  card's end inside equal cells (`index % 2`). With two columns both cards lose the same 16dp, so it
  works. With three or more, the middle cards keep their full cell and come out 16dp wider than the edge
  ones. That would break FR-009 (even spacing) and would be visible.
- Moving the inset to the grid's `contentPadding` would also inset the full-span active-filters row, whose
  chips scroll to the screen edge today under `CustomChipRow`'s fading edges. That is a visible portrait
  change, and avoiding it would need a layout hack that lets the header bleed back into the padding.
- With `n = 2` the custom cells produce exactly today's geometry: each cell is `(W − 16) / 2` and each card
  is that minus 16. **Portrait renders pixel-for-pixel as before (SC-007).**
- The count is computed once in `SearchResultGrid` (R4) and handed to both the cells and the items, so they
  cannot disagree. The full-span header always starts its own line, so game `index % n` is the column.

**Alternatives considered**:
- `contentPadding` + a bleeding header modifier: works, but it is a custom layout trick in a shared row for
  no gain over sizing the cells. Rejected.
- `GridCells.Fixed(n)` with per-column padding: unequal card widths for n > 2, as above. Rejected.

---

## R6 — The expanded search overlay covers the rail

**Decision**: No change. `ExpandedFullScreenSearchBar` renders in its own edge-to-edge dialog
(`BasicEdgeToEdgeDialog`, with `windowInsets = SearchBarDefaults.fullScreenWindowInsets`, which is
`WindowInsets.safeDrawing`). It covers the whole screen, rail included, the same way it covers the bottom
bar in portrait today. `safeDrawing` includes the display cutout, so its content already stays clear of the
camera.

**Spec amendment**: User Story 3, acceptance scenario 2 said the suggestions "sit beside the side bar
rather than under it". That contradicts the overlay being a full-screen dialog, and it would make landscape
behave differently from portrait. The scenario now says the overlay covers the whole screen, side bar
included, as it covers the bottom bar in portrait, with nothing under the cutout. This was done in the same
change as this plan.

**Alternatives considered**: `ExpandedDockedSearchBar` in landscape (a popup anchored under the field):
a different interaction from portrait, and the spec asks for the same Search. Rejected.

---

## R7 — The Discover hero on a short screen

**Decision**: The hero's height becomes `min(240.dp, windowHeight × 0.4)`, with the fraction as a
feature-local constant in `DiscoverHero.kt`. There is no landscape branch. On any portrait phone at least 600dp
tall 40% of the window height reaches 240dp, so the hero there is unchanged.

**Rationale**: In landscape the visible feed is the window height minus the status bar and the header
(about 264dp on a 360dp-tall phone, about 316dp on a 412dp-tall one). A fixed 240dp hero plus its 16dp gap
leaves the next shelf's title at or below the fold on small phones. Capping at 40% of the window height (144dp
and 164dp in those two cases) puts the next section's title on screen without scrolling. That is what US3
acceptance scenario 1 requires.

The hero crops the game's cover (a portrait image) into a wide banner, and a shorter, wider banner crops
more. Using a landscape artwork instead would mean a data change, so it is recorded as a follow-up.

**Alternatives considered**: a side-by-side hero (cover left, text right) in landscape: a new layout for one
component that the spec does not ask for. Rejected.

---

## R8 — The camera cutout on the far side of Search

**Decision**: Two insets gain the horizontal display cutout:

- `SearchScreenContent`'s `Scaffold` gets `contentWindowInsets = ScaffoldDefaults.contentWindowInsets
  .union(WindowInsets.displayCutout.only(Horizontal))`.
- `MainScreenHeader`'s row (`:core:ui`) pads by `TopAppBarDefaults.windowInsets
  .union(WindowInsets.displayCutout.only(Horizontal))`.

**Rationale**: When the phone is rotated the other way, the cutout is on the **end** edge, away from the
rail. Today neither the Search content (`systemBarsForVisualComponents`) nor the header
(`TopAppBarDefaults.windowInsets`) includes the cutout, so the last grid column and the profile button would
sit under the camera. SC-006 forbids that for every Search state. The rail already consumes the start side
(R3), so the union adds nothing there and there is no double padding.

`MainScreenHeader` is shared, so Radar's and Lists' headers get the same protection. That is a header-only
improvement. Their bodies keep their current insets, because the spec gives those screens no layout change
beyond the rail. This is listed as a follow-up.

**Alternatives considered**: `WindowInsets.safeDrawing` for Search's content: it includes the IME, which
would shrink the grid whenever the keyboard opens over the collapsed bar. Rejected.

---

## R9 — State across rotation

**Decision**: Nothing to add. FR-013 is already met by what exists, and quickstart §3 verifies it:

| State | Where it lives | Survives Android recreation because |
|---|---|---|
| Query text | `SearchViewModel.textFieldState` | ViewModel |
| Results, filters, sort, open sheets, list selector | `SearchUiState` | ViewModel |
| Expanded/collapsed bar | `rememberContainedSearchBarState()` | `rememberSaveable` with a `Saver` (checked in the M3 source) |
| Header scroll offset | `enterAlwaysSearchBarScrollBehavior()` | `rememberSaveable` (checked in the M3 source) |
| Grid and feed scroll position | `rememberLazyGridState()`, `rememberLazyListState()` | saveable; the first visible **item** is kept, so the re-flowed grid opens on the same game |
| Selected destination | `rememberNavBackStack(...)` | saveable |

On iOS there is no recreation, so the same objects simply carry over.

---

## R10 — The filter and sort sheets in landscape

**Decision**: The filter sheet stays as it is. Its options area is already `verticalScroll` at half the
window height. The sort sheet's `Column` gains `verticalScroll(rememberScrollState())`.

**Rationale**: The sort sheet holds the drag handle, the title, 4 rows of at least 48dp each, the bottom
padding and the navigation-bar inset. That comes to about 310–340dp, which only just fits a 360dp-tall phone
under the status bar. At larger font scales it would not fit, and its last option would be cut off with no
way to reach it (FR-012). A scroll that never activates when the content fits is the cheapest guarantee.
`ModalBottomSheet` already caps its width (640dp), so neither sheet stretches across a landscape screen.

---

## R11 — The display-corner clip next to the rail

**Decision**: The clip is removed. Nothing in the app rounds its content to the device's corner radius any
more: `QuestLogRoot` has no `displayCornerRadius` parameter, `MainActivity` no longer reads `RoundedCorner`, and
the nav entries take no clip modifier.

**Rationale**: The clip existed so the predictive-back card follows the screen's corners. With the rail on
screen, the content's start corners sit against the rail instead of a display corner, so rounding them only
showed the window behind the cut. Rounding only the end corners while the rail shows worked, but it was a
second rule to keep in step with the rail for a cosmetic effect, so the effect was dropped altogether.

---

## R12 — How this is tested

**Decision**:

- **JVM tests** (`feature/search/src/androidHostTest`): a new `SearchResultGridCellsTest` covers
  `resultGridColumnCount` (portrait is always 2, the landscape floor is 3, the worked examples of R4,
  boundary widths) and the cells' sizes (they sum to the available width minus spacing, the edge cells are
  wider by exactly `edgePadding`, every visible card is the same width, leftover pixels are distributed, and
  `n = 2` matches today's geometry). These are pure functions, called with a fixed `Density(1f)`.
- **No Compose UI tests**: the project has none (`docs/tech-debt.md`), and the spec's checks are visual
  (placement, clipping, cutouts). They are run by hand from quickstart §3 on an Android emulator with a
  simulated cutout, in both landscape directions and in RTL.
- **iOS**: `:shared:linkDebugFrameworkIosSimulatorArm64` and a pass of quickstart §4 on a simulator, on a
  Mac. Recorded as not run until then.
- `isLandscapeWindow()` is a single comparison over a composition local and has no test of its own. Its
  behavior is covered by the hand-run matrix.
