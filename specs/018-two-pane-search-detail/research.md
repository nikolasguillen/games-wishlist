# Research: Two-Pane Search and Game Detail

**Feature**: [spec.md](./spec.md) | **Plan**: [plan.md](./plan.md) | **Date**: 2026-10-18

Everything below was checked against the source and against the library sources resolved in the Gradle cache
(Navigation 3 UI 1.1.2 from JetBrains, Navigation 3 runtime 1.1.7, navigationevent-compose 1.1.2, JetBrains adaptive
1.2.0). No unknown is left open.

---

## R1. How the two panes are drawn

**Decision**: A hand-written Navigation 3 `SceneStrategy` in `:shared` (`SearchDetailSceneStrategy`) that returns a
two-pane `Scene` when the window is at least 840dp wide and the back stack ends with game details opened from
Search, sitting directly on `SearchRoute`. Every other case returns `null` and falls through to the default
`SinglePaneSceneStrategy`, so nothing else changes.

**Rationale**:

- Navigation 3's scene API (`SceneStrategyScope.calculateScene(entries)`, `Scene.entries`, `Scene.previousEntries`)
  is exactly the hook for "render more than one entry at once", and it is already on `:shared`'s classpath through
  `navigation3-ui`. No new dependency.
- `NavDisplay` wraps each entry in `movableContentOf` (`SceneSetupNavEntryDecorator`), and an entry shared by two
  scenes is rendered only by the target scene. So when the Search entry moves from the one-pane scene into the
  two-pane scene, its composition moves with it: the query field, the active filters, the grid's scroll state and
  even non-saveable `remember`ed state survive (FR-003). Its `ViewModel` is scoped to the `SearchRoute` entry and
  was never at risk.
- The strategy decides only from entry metadata (the `NavEntry` key is private), which the single `entryProvider`
  sets. Feature modules see nothing of it.

**Alternatives considered**:

- **Material 3 adaptive `ListDetailSceneStrategy`** (`material3-adaptive-navigation3`). It would add a new
  dependency (the JetBrains multiplatform artifact is a separate coordinate from the `adaptive` one already used),
  and it is built around a list pane that is always present on wide windows, with a detail placeholder. FR-002
  forbids both the empty pane and the placeholder. It also pops details one at a time, which conflicts with FR-008.
  Rejected.
- **A layout switch inside `SearchScreen`**, with the detail embedded in the feature. It would make
  `:feature:search` know about game detail and about navigation, which Principle I forbids. Rejected.
- **A second `NavDisplay` for the right pane.** That would mean two back stacks, two back handlers and state
  syncing across a resize. Rejected as far more machinery than one scene.

---

## R2. Telling a Search-opened detail apart from the others

**Decision**: `GameDetailRoute` gains an argument, `fromSearch: Boolean = false`. Search's `onGameClick` and the
"similar game" click inside a detail that is itself `fromSearch` create `GameDetailRoute(id, fromSearch = true)`.
Every other entry point (Radar, a wishlist, the release-reminder deep link) keeps calling `GameDetailRoute(id)` and
is therefore never part of two-pane mode (FR-013).

**Rationale**: The back stack alone cannot tell the difference. A reminder link opened while Search is showing
produces `[SearchRoute, GameDetailRoute(id)]`, the same shape as a game opened from the results, yet the spec says it
must stay full-screen. The origin has to travel with the route. A defaulted argument keeps every existing call site
and the saved-state format compatible (a missing field decodes to its default). It also keeps the route list at its
current length, so no fourth edit to the "three edits per route" rule is needed.

**Alternatives considered**:

- **A separate `SearchGameDetailRoute`.** It needs a new route, a new saved-state `subclass` line and a second
  `entryProvider` branch that would duplicate the detail branch. It is equally correct but costs more. Rejected.
- **Infer from structure (`[SearchRoute, GameDetail…]`).** Wrong for the deep-link case above. Rejected.

**Consequence**: `GameDetailRoute(7)` and `GameDetailRoute(7, fromSearch = true)` are different keys, so each gets its
own `ViewModel` (the entry's `contentKey` is the key's `toString()`). This is harmless: the two are never the same
on-screen detail.

---

## R3. The 840dp threshold and where it is read

**Decision**: `QuestLogContent` reads
`currentWindowAdaptiveInfo().windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)`
once and passes the resulting `twoPaneEnabled` down to `QuestLogNavDisplay`, to the scene strategy and to the
navigation-suite visibility. `QuestLogNavigationSuite` already reads the same `currentWindowAdaptiveInfo()` for its
bar-or-rail decision, from the `adaptive` artifact `:shared` already has.

**Rationale**: One reading, one boolean, every consumer in agreement. `WIDTH_DP_EXPANDED_LOWER_BOUND` is the 840dp
breakpoint, and it is part of the default breakpoint set, so "exactly 840dp counts" (spec edge case) is the
library's own `isWidthAtLeastBreakpoint` semantics. It is the window's width, so split-screen and freeform windows
follow the same rule (spec assumption).

**Flagged for the owner, not hard to undo**: some large phones are at least 840dp wide in landscape (a 412×915dp
phone on its side is 915dp wide). Under the rule as written, those get two panes in landscape. That follows the
request's own example ("a landscape window of that width") and Material's canonical list-detail guidance. If the
owner reads "no change to phone layouts" as excluding them, the fix is one extra condition, also requiring
`isHeightAtLeastBreakpoint(HEIGHT_DP_MEDIUM_LOWER_BOUND)`, in the one place `twoPaneEnabled` is computed. The plan
implements the width-only rule.

---

## R4. Back stack rules: replace, don't stack

**Decision**: A small set of pure functions in `:shared` (`SearchDetailPanes.kt`) owns the rules, and both the
click lambdas and `NavDisplay.onBack` call them:

- `searchDetailRun(keys)`: the index range of the trailing run of `fromSearch` details that sits directly on
  `SearchRoute`, or `null`.
- **Opening from Search or from a `fromSearch` detail** while `twoPaneEnabled`: drop the whole run, then add the new
  detail. A detail never stacks in two-pane mode (FR-004, FR-005). Below the threshold it is a plain push, exactly as
  today (FR-012).
- **Back** while `twoPaneEnabled` and a run exists: remove the whole run, so Search returns to full width in one
  step, however many games were opened before the window grew (FR-008, spec edge case). Otherwise
  `removeLastOrNull()`, as today.

The two-pane scene's `previousEntries` is the stack up to and including `SearchRoute`, which keeps predictive back
in agreement with what `onBack` does.

**Rationale**: Keeping the rule in plain functions over `List<NavKey>` makes it testable on the JVM without Compose
(Principle V), and it guarantees that the click path, the back path, the rail and the scene all use the same
definition of "a Search-opened detail is showing". The scene strategy applies the same function to entry metadata
instead of keys.

**Resize behavior** (FR-009 to FR-011) needs no code of its own. The back stack is not touched on a resize; only the
scene chosen for it changes. A run stacked while narrow (`[Search, D1, D2, D3]`) shows `D3` in the right pane when
the window grows. If the window shrinks again before any back, `D3` is full-screen and back walks `D2`, then `D1`,
exactly as today.

**Alternatives considered**: Collapsing the run to its last detail whenever the window grows. That mutates the back
stack in reaction to a configuration change, a side effect in composition, and it loses the narrow-mode history for
no user-visible gain. Rejected.

---

## R5. The system back gesture while both panes are composed

**Problem found**: `SearchScreen` registers its own `NavigationBackHandler`, which clears the query when a search or
browse is committed (`SearchScreen.kt:224`). Today it is never composed together with a detail. In a two-pane scene
it is, and because it is registered after `NavDisplay`'s own handler it would take priority: back would clear the
search instead of closing the detail. That breaks FR-003 and FR-008.

**Decision**: The two-pane scene registers its own back handler at the library's **overlay** priority
(`SearchDetailBackHandler`, a `NavigationEventHandler` added with `PRIORITY_OVERLAY`), which hands the event to the
display's `onBack`. Overlay handlers are asked before every default-priority one, so while the detail is showing, back
closes it. Once it closes, the scene is gone and Search's own handler works as before.

**What was tried first, and why it was dropped** (found on a tablet emulator): a child dispatcher with
`enabled = false`, and a default-priority `NavigationBackHandler` added after the panes. Neither is reliable. A
handler registers once, when its composition is created, and the Search entry keeps that registration as it moves
between scenes, so a child dispatcher never sees it. Among default-priority handlers the one added last wins, and the
order depends on how the Search entry came to be composed (it lost after a rotate-then-open). The overlay priority
does not depend on order.

**Rationale**: It outranks Search's handler without telling `:feature:search` anything (Principle I). The other ways
around the problem were to pass a flag into `SearchScreen` (the feature would learn about panes) or to reorder
handlers (fragile, as above).

**Known limit**: the handler sees a gesture only when it completes, so there is no predictive back preview while two
panes are showing. Back itself is unaffected. A window-level surface (the expanded search bar, a sheet, a dialog,
the full-screen image viewer) handles its own back and is not intercepted; verified for the expanded search bar.

**Dependency note**: this needs `navigationevent-compose` as a **direct** dependency of `:shared`
(`libs.jetbrains.navigationevent.compose`, already in the catalog and already used by `:feature:search` and
`:feature:onboarding`). It adds no new module edge and no new library to the build, only a new declaration. It is
called out in the plan's Complexity Tracking table.

---

## R6. The detail's back button and close button

**Decision**: `GameDetailScreen`'s `onBackClick` becomes `(() -> Unit)?`, and a sibling `onCloseClick: (() -> Unit)?`
(default `null`) is added. `null` back means no back button; a non-null close draws a close (X) button in the same
place when there is no back button. Both are threaded through `GameDetailMainContent`, `DetailErrorLoadingWrapper` and
`GameDetailSuccessContent` to `ImmersiveDetailLayout` in `:core:ui`, which takes the same two parameters. Game detail
is that component's only caller. For a detail with `twoPaneEnabled && key.fromSearch` the `entryProvider` passes no
back and a close that calls `navigateBack`, the same function system back uses; otherwise the usual back and no close.

**Rationale**: The feature module learns only that its navigation button can be a back arrow or a close X, not why
(FR-007, Principle I). A back arrow suggests a history to walk back through; in two-pane mode there is none, because
opening another game replaces the pane, so the button says "close this pane", and it is visible where system back is
not (mouse, keyboard, stylus). Both buttons also cover the loading and error states, so the X is there while a detail
loads or fails in the right pane. The X needs one new string, `close_content_description`, in `:core:ui`.

**Alternative considered**: a single sealed "navigation action" parameter. Cleaner on paper, but it would rewrite
every call site for no behavior gain. The two nullable callbacks keep the existing back path untouched.

**Why `twoPaneEnabled && key.fromSearch` is exactly "rendered in the right pane"**: a `fromSearch` detail can only be
created by Search (the stack root) or by another `fromSearch` detail, and switching tabs pops back to the root.
`fromSearch` details therefore always form a run directly on `SearchRoute`. When one is rendered at all, it is the
top of that run, which is the two-pane case whenever `twoPaneEnabled` holds. A deep-linked detail pushed on top of
the run is `fromSearch = false` and covers it. While it is showing, the run is not rendered.

**Alternatives considered**: A `CompositionLocal` provided by the scene ("you are in the detail pane"). It is just
as correct, but it adds a concept, and the derivation above already holds. Rejected for now. It would be the fallback
if a future entry point broke the invariant.

---

## R7. Making both screens fit a half-width pane

The audit of every place the two screens read the window rather than their own space:

| Place | Reads | In a pane | Action |
|---|---|---|---|
| `SearchResultGrid` columns | `isLandscapeWindow()` (window shape) + the grid's own `maxWidth` | **Wrong.** An unfolded foldable's pane (about 410×700dp) is portrait-shaped, yet the window is landscape, so the grid would force at least three columns into it, about 110dp each. | Decide landscape from the grid's own box (`maxWidth > maxHeight`, through a new `isLandscapeShape(width, height)` in `:core:ui/WindowShape.kt`, so the rule is still written once). |
| `ExpandedSearchHome` (the expanded search bar's suggestions) | `isLandscapeWindow()` | Correct. It lives in `ExpandedFullScreenSearchBar`, a full-window overlay, so the window *is* its space. | None. Opening the search bar in two-pane mode covers both panes, as a full-screen search should. |
| `DiscoverHero` height, `GameDetailSheetContent` min height, `SearchFilterBottomSheet` height | window height | Correct. The panes are full height. | None. |
| `CustomAlertDialog` width, modal bottom sheets | window | Correct. They are window-level surfaces. | None. |
| Horizontal insets (`SearchScreen`'s Scaffold unions `displayCutout` horizontally) | window insets | **Wrong at the inner edge.** A cutout on the window's end side would also pad the left pane's inner edge, and one on the start side the right pane's. | Each pane consumes the insets of the side it does not touch (left pane: `End`, right pane: `Start`) with `consumeWindowInsets`. Verified on device (quickstart S7). |

With the grid change, the grid decides from the space it was given. Measured on a 1280x800dp tablet: the left pane's
grid box is about 600x600dp (it sits under the search header), so it counts as landscape and shows 3 columns of
roughly 180dp, about the size of a portrait phone's cards; at about 850dp wide the pane is clearly portrait-shaped
and shows 2 columns. The landscape column logic from spec 017 is unchanged for the full-width layout (US5-1, US5-3,
FR-015). This changes spec 017's rule for the grid only, from "the window's shape" to "the shape of the space the grid
is given". Search at full width reads the same either way, except in a nearly square portrait window (within about
36dp), where the grid's box can read as landscape. The cut-off sits where the pane is close to square, so the column
count there is a judgement call, not a guarantee of 2.

The detail has no width-dependent logic. Its hero, sheet and action pill are all `fillMaxWidth`/centered (FR-016).

**Pane split**: two equal-weight panes with a Material `VerticalDivider` between them. No new spacing token is
needed. The spec leaves the ratio to the plan, and equal halves is what it assumes.

---

## R8. Transitions

**Decision**: Entering, leaving and swapping within the two-pane layout are immediate, with no crossfade. The
two-pane scene's `key` is the Search entry's `contentKey`, so replacing the detail does not change the scene and
starts no `NavDisplay` transition. Entering and leaving the scene override the transition through the scene's own
metadata (`NavDisplay.transitionSpec` and `popTransitionSpec`, both `None togetherWith None`). `NavDisplay` reads the
metadata of the scene on top: the target scene when opening and the departing one when popping, and both are the
two-pane scene. The predictive pop spec is left at the app's default; it is not reached while two panes show
(see R5, known limit).

**Rationale**: `NavDisplay` animates between scenes, and an entry shared by both scenes is drawn only by the target.
The app's default `fadeIn togetherWith fadeOut` would therefore fade the Search pane in from nothing every time a
game opens or closes, which reads as the list blinking. Which of the two scenes' metadata ends up applied is
verified on device (quickstart S3) rather than assumed. A detail-pane animation is polish that this spec does not
ask for.

---

## R9. Navigation rail

**Decision**: In `QuestLogContent`, the suite is visible when the current route is top-level **or** when
`twoPaneEnabled` and a Search-detail run is showing (FR-006). In that case the suite is handed `SearchRoute` as the
route to highlight, so `TopLevelDestination.SEARCH.isSelected` stays true. Tapping Search then does nothing (it is
already selected). Tapping Radar or Lists runs the existing `onNavigateToRoute`, which pops to the root, closing the
detail, and opens the tab (spec assumption: existing tab-switching behavior).

---

## R10. iOS

Everything above is `commonMain`: the scene, the window size class (JetBrains `adaptive`), navigationevent (iOS's
edge-swipe back goes through the same dispatcher) and the composables. No `expect`/`actual` and no platform contract
are added. iOS can only be verified on a Mac (`:shared:linkDebugFrameworkIosSimulatorArm64`, then an iPad simulator).
