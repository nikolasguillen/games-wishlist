# Contract: Two-Pane Navigation and the Component APIs It Touches

**Feature**: [spec.md](../spec.md) | **Plan**: [plan.md](../plan.md) | **Date**: 2026-10-18

This feature exposes nothing outside the app. The contracts here are the internal seams between modules: what
`:shared` promises about the back stack, and the signature changes in `:core:navigation`, `:core:ui` and the two
feature modules. Everything that knows about panes lives in `:shared`.

## 1. Back stack rules (`:shared`, `SearchDetailPanes.kt`, all `internal`)

| Function | Contract |
|---|---|
| `searchDetailRun(keys: List<NavKey>): IntRange?` | The indices of the trailing `GameDetailRoute(fromSearch = true)` run directly on a `SearchRoute`, or `null`. Pure. |
| `openSearchDetail(backStack, gameId, twoPaneEnabled)` | Below the threshold: push `GameDetailRoute(gameId, true)` unless it is already the top (today's guard). At or above it: remove the run (if any), then add the detail unless it is already the sole detail of the run. The stack never grows by more than one, and in two-pane mode the run never exceeds one entry. |
| `navigateBack(backStack, twoPaneEnabled)` | With `twoPaneEnabled` and a run: remove the whole run. Otherwise `removeLastOrNull()`. Never empties a stack of one, as today. |

Callers: Search's `onGameClick`; a `fromSearch` detail's `onGameClick` (a non-`fromSearch` detail keeps today's push
of `GameDetailRoute(id)`); `NavDisplay(onBack = …)`; and the detail's back button, which is only shown when not in
two-pane mode, so it is a plain pop.

## 2. Scene (`:shared`, `SearchDetailSceneStrategy.kt`, `internal`)

- **Input**: `twoPaneEnabled`, plus entry metadata set by the `entryProvider`: one marker on the `SearchRoute` entry,
  another on `fromSearch` detail entries.
- **Output**: when `twoPaneEnabled` and the entries end in a marked run directly on the marked Search entry, a scene
  with
  - `entries = [searchEntry, lastDetailEntry]`
  - `previousEntries = entries up to and including searchEntry` (in agreement with `navigateBack`)
  - `key` = the Search entry's `contentKey`, stable while the detail is replaced
  - content: a `Row` of two equal-weight panes and a `VerticalDivider`. The left pane consumes the `End` display
    cutout inset and the right pane the `Start` one. A `SearchDetailBackHandler` (overlay priority) sends system back
    to the display's `onBack`, so it outranks Search's own handler.
- Otherwise `null`, so `SinglePaneSceneStrategy` renders exactly as today.

## 3. Navigation suite (`:shared`)

`QuestLogContent` passes the suite `isVisible = isTopLevelRoute || (twoPaneEnabled && run != null)` and, in the second
case, `SearchRoute` as the route to highlight.

## 4. Signature changes outside `:shared`

| Module | Declaration | Before | After |
|---|---|---|---|
| `:core:navigation` | `GameDetailRoute` | `(gameId: Int)` | `(gameId: Int, fromSearch: Boolean = false)` |
| `:core:ui` | `ImmersiveDetailLayout(onBackClick)` | `() -> Unit` | `(() -> Unit)?`: `null` draws no navigation icon |
| `:core:ui` | `WindowShape.kt` | `isLandscapeWindow()` | unchanged, now implemented through the new `isLandscapeShape(width: Dp, height: Dp): Boolean` (width > height; a square is portrait) |
| `:feature:game-detail` | `GameDetailScreen(onBackClick)` and the internal content composables down to `ImmersiveDetailLayout` / `DetailErrorLoadingWrapper` | `() -> Unit` | `(() -> Unit)?` |
| `:feature:search` | `SearchResultGrid` (internal) | `isLandscape = isLandscapeWindow()` | `isLandscape = isLandscapeShape(maxWidth, maxHeight)` of its own `BoxWithConstraints` |

No feature module gains a dependency, a route reference or a notion of panes.
