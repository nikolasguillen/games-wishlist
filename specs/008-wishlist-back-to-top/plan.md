# Implementation Plan: Wishlist Back-to-Top Button

**Branch**: `008-wishlist-back-to-top` | **Date**: 2026-10-06 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/008-wishlist-back-to-top/spec.md`

## Summary

The wishlist detail screen gets the same floating "back to top" button Search has. It appears once the
user has scrolled past the list header and the filter/view row, and one tap scrolls smoothly back to the
very top. It is hidden at the top, on short lists, and for loading, empty and filtered-empty content.

Technically:
- Search's inline button is extracted into one shared `ScrollToTopFab` in `:core:ui`, and both screens use
  it, so the icon, style, animation and label stay identical.
- The wishlist screen shows it from the `Scaffold`'s FAB slot, driven by the hoisted `LazyGridState`
  (spec 007) and gated on the `Success` content state.
- The lists under the button (the wishlist grid, and Search's results grid and Discover feed) get one shared
  constant bottom padding, `ScrollToTopFabDefaults.ContentBottomPadding`, so the button never leaves the last
  item covered.
- No ViewModel, state, event, data or dependency changes.

## Technical Context

**Language/Version**: Kotlin 2.4.10, JVM toolchain 21, Java 11 bytecode

**Primary Dependencies**: Jetpack Compose (BOM 2026.09.00, Material 3 `FloatingActionButton` and
`animateFloatingActionButton`), kotlinx-coroutines (the scroll animation)

**Storage**: N/A. Nothing is persisted; the button has no setting.

**Testing**: No new automated tests: the behaviour is a boolean derived from a Compose scroll state, and
the project has no Compose UI test setup (research R9). The existing JUnit4 + MockK suites must stay green
(Search is edited). Verification is compile + the on-device scenarios in [quickstart.md](quickstart.md).

**Target Platform**: Android, minSdk 29, compile/targetSdk 37

**Project Type**: Modular Android app (17 Gradle modules)

**Performance Goals**: no visible stutter while scrolling a 500-game wishlist (SC-005). A `derivedStateOf`
boolean recomposes the screen only when the button should appear or disappear.

**Constraints**: no new module edge; text from `strings.xml`; spacing from `MaterialTheme.spacing`; same
look and behaviour as Search's button (FR-005).

**Scale/Scope**: 3 modules touched (`core:ui`, `feature:search`, `feature:wishlist`); 1 new source file, about
9 edited files, 1 string moved.

No unknowns remain. All design questions are resolved in [research.md](research.md).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-checked after Phase 1 design. Both pass.*

| Principle | Check | Result |
|-----------|-------|--------|
| **I. Module boundaries** | `:feature:search` and `:feature:wishlist` already depend on `:core:ui`; the shared button lives there. No feature depends on another feature, no new edge, no build-file change. | ✅ |
| **II. Typed errors** | No data, repository or error path involved. | ✅ (n/a) |
| **III. UI renders, doesn't decide** | The button's visibility is derived from scroll position, not a business decision, and there is nothing to filter, sort or persist. Search keeps the same logic in its composable. No new `UiState` / `UiEvent` / `UiEffect`. No hardcoded text. | ✅ |
| **IV. Reuse first** | Reuses `CustomFab` and Search's existing button design by *extracting* it, instead of copying it. Spacing from existing tokens (`doubleLarge * 3`), no `dp` literal. The string moves to `:core:ui`, one definition. | ✅ |
| **V. Local verification** | Names real Gradle commands only. No test is invented for logic that has none; the existing suites are kept green (research R9). | ✅ |
| **Constraints** | No DB, no `SavedStateHandle`, no KMP restructuring. | ✅ |
| **Workflow** | One composable per file; `internal` where local (`ScrollToTopFab` is public because two feature modules use it); English KDoc; component file ends with a `@QuestLogPreviews` preview. | ✅ |

## Project Structure

### Documentation (this feature)

```text
specs/008-wishlist-back-to-top/
├── spec.md
├── plan.md                                  # this file
├── research.md                              # R1–R9 decisions
├── data-model.md                            # no data change; the one derived boolean
├── quickstart.md                            # build commands + manual scenarios B1–B17
├── contracts/scroll-to-top-fab.md           # shared component, its two callers, strings
├── checklists/requirements.md
└── tasks.md                                 # /speckit-tasks (not created here)
```

### Source Code (repository root)

```text
core/ui/src/main/java/com/nikolasguillen/questlog/core/ui/
├── component/ScrollToTopFab.kt              # NEW  shared button (CustomFab + animated + label) + ScrollToTopFabDefaults + preview
└── util/Constants.kt                        # EDIT + SCROLL_TO_TOP_AFTER_ITEM_INDEX = 1
core/ui/src/main/res/values/strings.xml      # EDIT + scroll_to_top_content_description (moved from Search)
core/ui/CLAUDE.md                            # EDIT list ScrollToTopFab in the component inventory

feature/search/src/main/java/com/nikolasguillen/questlog/feature/search/
├── SearchScreen.kt                          # EDIT adopt ScrollToTopFab; use the shared constant
└── components/
    ├── SearchResultGrid.kt                  # EDIT bottom contentPadding = ScrollToTopFabDefaults.ContentBottomPadding
    └── DiscoverFeed.kt                      # EDIT bottom contentPadding = ScrollToTopFabDefaults.ContentBottomPadding
feature/search/src/main/res/values/strings.xml   # EDIT remove scroll_to_top_content_description

feature/wishlist/src/main/java/com/nikolasguillen/questlog/feature/wishlist/
├── WishlistScreen.kt                        # EDIT floatingActionButton slot + derived visibility + scroll action
└── components/WishlistGamesList.kt          # EDIT bottom contentPadding on the grid
```

**Structure Decision**: No new module and no new layer. One shared component in `:core:ui` (where the
project keeps shared composables), two thin callers. The wishlist change stays inside `feature/wishlist`'s
existing screen and list component.

## Design Notes

- **Visibility**: `ScrollToTopFab(visible = state.contentState is Success && showScrollToTop, ...)`, where
  `showScrollToTop` is a `derivedStateOf { gridState.firstVisibleItemIndex > 1 }`. Item `1` is the filter/view
  row from spec 007, so the test means "header and row both off screen" (research R3).
- **Scroll**: `scope.launch { gridState.animateScrollToItem(0) }`.
- **Last item**: bottom `contentPadding` of `ScrollToTopFabDefaults.ContentBottomPadding` (`spacing.doubleLarge
  * 3`) on the wishlist grid and on both of Search's lists (research R7).
- **Order of work matters for Search**: extract the component and move the string first, switch Search over,
  confirm Search still compiles and behaves, then add the wishlist caller.

## Testing

| What | How |
|------|-----|
| Shared component, both callers | compile `:core:ui`, `:feature:search`, `:feature:wishlist` |
| Nothing regresses | `./gradlew :feature:search:testDebugUnitTest :feature:wishlist:testDebugUnitTest`, then `./gradlew test` |
| DI / full build | `./gradlew :app:assembleDebug` |
| Behaviour (appearance, tap, hiding, snackbar, last card, TalkBack, Search regression) | on-device scenarios B1–B17 in [quickstart.md](quickstart.md) |

No new unit or UI tests, and why: research R9.

## Points for the owner

1. **Search is edited.** Its inline button becomes the shared component and its string moves to `:core:ui`.
   Behaviour should not change (quickstart B16). If you would rather not touch Search, the alternative is a
   copy in the wishlist, which research R1 recommends against.
2. **Search's lists also get the clearance** (results grid and Discover feed), at the owner's request after
   the first implementation, so the button clears the last item there too. Quickstart B18 checks it.

## Complexity Tracking

No constitution violations. Nothing to justify.
