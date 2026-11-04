# Implementation Plan: Search Dropdown on Wide Windows

**Branch**: `022-search-dropdown-wide` | **Date**: 2026-11-04 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/022-search-dropdown-wide/spec.md`

## Summary

On a window at least 840dp wide, the search bar opens a panel directly under itself instead of the full-screen
overlay. The panel is as wide as the bar, floats over the page, and leaves the rail and the page visible. It shows
what the overlay shows, in the overlay's order: the search-type chips, then the genre chips, then recent searches and
recently viewed games before typing; or the type chips, then matching recent searches, game suggestions and "See
all results" while typing. The mockup's expanded game card is not built. It is driven by the keyboard (a virtual
highlight moved by the arrow keys, Enter, Escape, Tab, Shift+Delete) and by the mouse (hover highlights, click
activates). Below 840dp nothing changes.

The technical approach:
- **One dispatch point for all of Search** (the owner's open question): `SearchScreenContent` reads `isWideWindow`
  once and renders either `CompactSearchLayout` (today's Scaffold, moved verbatim) or `WideSearchLayout`. Spec 021's
  Discover switch moves up into that choice, and `SearchMainContent` takes the feed as a slot instead of a flag.
- **The panel is Material 3's `ExpandedDockedSearchBarWithGap`**, with no scrim. It runs on the same `SearchBarState`,
  input field and `TextFieldState` as the compact overlay, so crossing 840dp swaps the presentation without losing
  the text, the caret or the type.
- **The panel reuses the overlay's own pieces**: the chips, the genre section, the recent searches, the recent game
  cards and the suggestion rows, given optional per-item decorations and a `wrap` mode.
- **The highlight is UI state** in `WideSearchLayout`, ruled by pure, JVM-tested functions.

There is no ViewModel, UI state, event, domain, data, network or database change, no new string, and no change in
`:shared`.

## Technical Context

**Language/Version**: Kotlin 2.4.20, Compose Multiplatform 1.12.1 (Material 3: JetBrains 1.12.0-alpha03 in
`commonMain`, Jetpack 1.5.0-beta01 on Android)

**Primary Dependencies**: Compose foundation and Material 3, already in `:feature:search`.
`ExpandedDockedSearchBarWithGap` has the same signature in both Material 3 artifacts (verified in their sources,
research R2), so no `expect`/`actual` is needed; the call opts into `ExperimentalMaterial3ExpressiveApi`. Reused
pieces: `CustomFilterChip`, `RecentGameCard`, `CustomAlertDialog` through the existing history dialogs, and the
overlay's own section and row composables. **No new library and no new module edge.**

**Storage**: N/A. The database stays at `version = 1`, untouched.

**Testing**: JUnit4 in `feature/search/src/androidHostTest`: new `SearchDropdownItemsTest` (the reading order) and
`SearchDropdownNavigationTest` (moves and reconciliation). Existing suites untouched and green. Presentation,
keyboard, mouse and focus are checked by hand on desktop, a resizable Android emulator and an iPad simulator
([quickstart.md](./quickstart.md)).

**Target Platform**: Desktop (`:desktopApp`, JVM), Android tablets and large windows (minSdk 29), and iPads (iOS 16+),
all through the same `commonMain` code. Phones are unaffected.

**Project Type**: Kotlin Multiplatform mobile and desktop app, multi-module.

**Performance Goals**: The panel opens with Material's expand animation at the platform frame rate. Typing stays
as responsive as in the overlay, because the suggestions pipeline is the same (debounced in the ViewModel). Swapping
across 840dp is a recomposition with no fetch.

**Constraints**:
- Presentation only (spec).
- The owner's constraints: the wide dropdown is its own composable, the window size is read once, the compact overlay
  is unchanged, the same ViewModel and state are used.
- `commonMain` only.
- Keyboard focus stays in the text field (FR-015, FR-019; research R3).
- The panel never covers the rail (no scrim).

**Scale/Scope**:
- 1 feature module (`:feature:search`), no `:shared` edit.
- 10 new source files: 2 layouts, 4 wide panel composables, the item modifier, the item model, the item mapper and
  the navigation rules.
- 1 file split out of an existing one (`RecentSearchesSection.kt`) and 2 new test files.
- 0 new strings.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Check | Result |
|---|---|---|
| **I. Module boundaries** | Only `:feature:search` changes; it keeps depending on the six allowed `:core:*` modules. `:shared` already passes `isWideWindow`. No new module, no new edge, no `:core:ui` change (the chips are decorated through a wrapping `Box`, not a new `CustomFilterChip` parameter). | ✅ Pass |
| **II. Typed errors** | No error path changes. The suggestions' loading state is reused as is. | ✅ Pass (N/A) |
| **III. The UI renders, it does not decide** | The item order, the moves and the reconciliation are pure functions with JVM tests (R7). The composables apply them and route activations to the callbacks the overlay already uses. No filtering or sorting. No new text: every label is an existing resource. Flows are still collected with `collectAsStateWithLifecycle()`. The highlight is ephemeral composable state, like a scroll position; the history dialogs stay saveable composable state, as `feature/CLAUDE.md` prescribes for confirmations that emit one event. | ✅ Pass |
| **IV. Reuse the shared layer** | The panel is built from the overlay's composables and `:core:ui`'s `CustomFilterChip` and `RecentGameCard`. The dialogs are the existing `CustomAlertDialog`-based ones. Gaps and paddings use `MaterialTheme.spacing`, and colors come from the theme (`appColors` / `colorScheme`). There is no inline `dp` literal and no Material `AlertDialog`. The highlight modifier lives in the feature, its only user; it moves to `core/ui/util/modifiers/` when a second feature needs it. | ✅ Pass |
| **V. Local verification** | Real commands only (quickstart §1). The new logic gets tests in its module. No CI or lint gate is assumed. | ✅ Pass |
| **KMP constraints** | `commonMain` only. No `android.*` or `java.*`, no `expect`/`actual` (same Material 3 API on both artifacts, R2), no platform contract, no library swap. iOS is verifiable on this Mac and is listed in the quickstart. | ✅ Pass |
| **Development workflow** | One composable unit per file in `components/`. `SearchDropdownItemUiModel` is one sealed hierarchy in one file, the exception the rules allow. No other new type (two functions instead of a direction enum). `internal` by default, and the own `Res` is imported bare. Previews use `@QuestLogPreviews` plus `@Preview(widthDp = 720)` for the panel. | ✅ Pass |

**Guidance check, `feature/CLAUDE.md`**: "A screen with a separate wide layout … receives `isWideWindow` … and
dispatches on it once, in one place." The current code reads it inside `SearchMainContent` (spec 021). After this
plan it is read in `SearchScreenContent` only, which is what the rule describes, so the rule needs no amendment.
`SearchMainContent`'s KDoc ("the one switch between the two Discover layouts") is rewritten in the same change.

**Post-design re-check (after Phase 1)**: no change. The design adds no state to `SearchUiState`, no string, no
module edge, and one sealed UI type. All gates still pass.

## Project Structure

### Documentation (this feature)

```text
specs/022-search-dropdown-wide/
├── spec.md              # /speckit-specify, owner answers of 2026-11-04, planning refinements
├── plan.md              # This file
├── research.md          # Phase 0: R1–R11
├── data-model.md        # Phase 1: SearchDropdownItemUiModel, reading order, highlight rules, moved dialog state
├── quickstart.md        # Phase 1: validation guide
├── contracts/
│   └── ui-contract.md   # Phase 1: composable signatures, keyboard and pointer contracts, :shared boundary
├── checklists/
│   └── requirements.md  # spec quality checklist
└── tasks.md             # Phase 2 (/speckit-tasks, not created here)
```

### Source Code (repository root)

```text
feature/search/src/commonMain/kotlin/com/nikolasguillen/questlog/feature/search/
├── SearchScreen.kt                    # SearchScreenContent: hoists the history dialog state; the single
│                                      #   `if (isWideWindow) WideSearchLayout else CompactSearchLayout`
├── components/
│   ├── CompactSearchLayout.kt         # NEW: today's Scaffold, moved verbatim (ExpandedSearchBar + DiscoverFeed)
│   ├── WideSearchLayout.kt            # NEW: Scaffold with WideSearchDropdown + WideDiscoverFeed; owns the
│   │                                  #   highlight and the key handler
│   ├── SearchBars.kt                  # SearchTopBar: expandedContent slot, inputFieldModifier,
│   │                                  #   readOnlyWhenCollapsed; SearchInputField: modifier, readOnlyWhenCollapsed;
│   │                                  #   ExpandedSearchBar: takes the hoisted dialog state
│   ├── SearchMainContent.kt           # isWideWindow removed; discoverFeed slot; KDoc rewritten
│   ├── WideSearchDropdown.kt          # NEW: ExpandedDockedSearchBarWithGap + panel + the two history dialogs
│   ├── WideSearchPanel.kt             # NEW: scrolling column, empty-query vs typing, live region
│   ├── WideSearchHome.kt              # NEW: chips, genres, recent searches, recent games (wrapping)
│   ├── WideSearchSuggestions.kt       # NEW: chips, history suggestions, game suggestions, see-all
│   ├── SearchPanelItem.kt             # NEW: Modifier.searchPanelItem (mark, hover, bring into view)
│   ├── SearchDropdownNavigation.kt    # NEW: highlightNext, highlightPrevious, reconcileHighlight (pure)
│   ├── RecentSearchesSection.kt       # NEW file, moved out of ExpandedSearchHome.kt (+ RecentSearchChip),
│   │                                  #   private → internal, + clearAllModifier / chipModifier
│   ├── ExpandedSearchHome.kt          # uses the moved section; otherwise unchanged
│   ├── SearchTypeChipRow.kt           # + wrap, chipModifier
│   └── GenreBrowseSection.kt          # + wrap, chipModifier
├── mapper/
│   └── SearchDropdownItemMapper.kt    # NEW: searchDropdownItems (pure)
└── model/
    └── SearchDropdownItemUiModel.kt          # NEW: sealed interface + its variants

feature/search/src/androidHostTest/kotlin/com/nikolasguillen/questlog/feature/search/
├── mapper/SearchDropdownItemsTest.kt           # NEW
└── components/SearchDropdownNavigationTest.kt  # NEW
```

**Structure Decision**: Everything lives in `:feature:search`, in its established `components/`, `mapper/` and
`model/` layout. The wide composables sit beside the compact ones, and the compact ones gain only optional parameters
whose defaults reproduce today's behaviour. `:shared` and every other module are untouched.

**Build order** (each step compiles and keeps `./gradlew test` green, so the work can pause anywhere):

1. **Groundwork, no visible change**:
   - hoist the history dialog state into `SearchScreenContent`;
   - move `RecentSearchesSection` to its own file;
   - add the optional parameters (`wrap`, `chipModifier`, `clearAllModifier`, the input field's `modifier` and
     `readOnlyWhenCollapsed`, `SearchTopBar`'s `expandedContent` slot);
   - give `SearchMainContent` the `discoverFeed` slot in place of `isWideWindow`.

   Compact and wide behave exactly as today.
2. **Single dispatch, no visible change**: `CompactSearchLayout` (the moved Scaffold) and `WideSearchLayout`, which at
   this step still passes `ExpandedSearchBar` and `WideDiscoverFeed`, so the wide window looks as it does today.
   `SearchScreenContent` dispatches. Run `:androidApp:assembleDebug` here.
3. **Pure rules and tests**: `SearchDropdownItemUiModel`, `searchDropdownItems`, the navigation functions,
   `SearchDropdownItemsTest`, `SearchDropdownNavigationTest`.
4. **Wide panel composables with previews**: `SearchPanelItem`, `WideSearchHome`, `WideSearchSuggestions`,
   `WideSearchPanel`, `WideSearchDropdown`. Nothing calls them yet.
5. **Wire it**: `WideSearchLayout` passes `WideSearchDropdown`, the highlight state, the key handler and
   `readOnlyWhenCollapsed = false`. This is the first visible change. Run `:androidApp:assembleDebug` and
   `:desktopApp:run`.
6. **Verification and docs**: quickstart §2–8 on desktop, the Android emulator and the iPad simulator. Record anything
   unverified in `docs/tech-debt.md`. Add the desktop keyboard checks to the existing open-checks entry rather than
   a new one if they are still open.

## Spec refinements made during planning

The spec was edited in the same change. Each edit follows a planning finding:

- **FR-001 and US1-1**: "focusing the bar opens the panel" becomes "a click or tap, typing, or Down opens it; tabbing
  onto the bar alone does not" (research R4).
- **US2-8**: "reachable through Tab and the arrow keys" becomes "reachable with the arrow keys". The spec also said
  Tab closes the panel (US3-3), so both could not hold (research R3).
- **US3-4**: "focuses the bar again" becomes "clicks the bar again (or types, or presses Down)", following FR-001.
- **FR-003**: the height cap is "a maximum that keeps the lower part of the window visible", Material's half-window
  cap, instead of "the space available under the bar".
- **FR-015**: the "Clear all" action is listed among the items the arrow keys reach.
- **FR-016**: Shift+Delete removes a highlighted recent search (through its dialog) or recently viewed game (R3).

## Owner decisions applied (2026-11-04, after the analysis)

- **Panel items are non-focusable**, so a mouse click or Tab never moves keyboard focus off the text field (research R3, T017).
- **The 240dp minimum height of the Material panel is accepted** and stated in FR-003 (research R2).
- **The item type is named `SearchDropdownItemUiModel`**, following the `UiModel` suffix rule literally.
- **"The bar loses focus" means Tab and Shift+Tab**; the window losing focus keeps the panel open (FR-021, research R5).

## Decisions the owner may want to revisit

- **Tab-focus does not open the panel** (R4). Flipping it is one `onFocusChanged` on the wide field.
- **The gap variant**: `ExpandedDockedSearchBarWithGap` matches the mockup's separate pill and card, but JetBrains
  still marks it experimental (Expressive). The non-gap `ExpandedDockedSearchBar` is a one-line swap (R2).
- **A click on the rail while the panel is open only closes the panel** (R5). A focusable popup consumes the click
  that dismisses it, which is what FR-022 asks for, but it also means the first click on another tab is spent
  closing the panel.

If a platform lets the closing click through to what is under the panel (T029), the options are a transparent full-window click catcher (it would also catch clicks on the rail), a custom overlay (more code), or accepting it on that platform. The owner decides; nothing is chosen alone.

None of these is hard to undo.

## Complexity Tracking

No constitution violations. Nothing to justify.
