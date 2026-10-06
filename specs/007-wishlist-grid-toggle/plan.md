# Implementation Plan: Wishlist Grid View Toggle

**Branch**: `007-wishlist-grid-toggle` | **Date**: 2026-10-06 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/007-wishlist-grid-toggle/spec.md`

## Summary

The wishlist detail screen gains a slim **filter-and-view row** between the list header and the first
status section. On the left are status filter chips ("All" + one per non-empty status, hidden when there
are fewer than two and no filter is on). On the right is a list/grid toggle. A filter that matches no
games stays applied and shows a centered message, like the empty-list state. Grid view renders the same status sections, with
each section's games as two-column `VerticalGameCard`s, the Search card. A long-press on a card opens
the existing remove confirmation.

Technically:
- The `LazyColumn` becomes one `LazyVerticalGrid` that serves both views through item spans.
- The ViewModel `combine`s the wishlist detail with a persisted, app-wide `WishlistViewMode` and an
  in-memory `WishlistStatusFilter`.
- The view mode is stored in the existing settings DataStore through a new domain store and two use
  cases. These are cloned from the `AppearancePreferenceStore` shape.
- `VerticalGameCard` gets a nullable save callback, which hides the heart, and a **required** long-press
  label. Its one existing caller, `SearchResultGrid`, passes the "choose list" label explicitly.

## Technical Context

**Language/Version**: Kotlin 2.4.10, JVM toolchain 21, Java 11 bytecode

**Primary Dependencies**: Jetpack Compose (BOM 2026.09.00, Material 3, `material-icons-extended` via
`:core:ui`), Hilt 2.60.1 (assisted-inject ViewModel), DataStore Preferences (already in `:core:data`),
kotlinx-coroutines Flow

**Storage**: Existing `DataStore<Preferences>` named `"settings"`, one new int key. No Room change.

**Testing**: JUnit4 + MockK + kotlinx-coroutines-test (`StandardTestDispatcher`, `setMain`/`resetMain`).
A temp-file-backed DataStore is used for the store test, as in `AppearancePreferenceStoreImplTest`.

**Target Platform**: Android, minSdk 29, compile/targetSdk 37

**Project Type**: Modular Android app (17 Gradle modules)

**Performance Goals**: Toggle and filter apply within 1 s (SC-001, SC-007). Smooth scrolling of a
500-game grid (SC-005). Lazy composition covers this.

**Constraints**: No new module edges. `:core:model` stays free of Android and Compose. No
`SavedStateHandle`. The DB version is not bumped.

**Scale/Scope**: One screen, about 6 modules touched (`core:model`, `core:domain`, `core:data`,
`core:ui`, `feature:wishlist`, plus a one-line call-site check in `feature:search`). Roughly 12 new or
changed source files and 3 test files.

No unknowns remain. All design questions are resolved in [research.md](research.md).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-checked after Phase 1 design. Both pass.*

| Principle | Check | Result |
|-----------|-------|--------|
| **I. Module boundaries** | `:feature:wishlist` uses only `:core:domain` use cases, `:core:model`, `:core:ui` and `:core:designsystem`, all existing edges. The store interface is in `:core:domain` and its DataStore impl in `:core:data`. `:core:model` gets a pure-Kotlin enum. No navigation change. **No new edge.** | ✅ |
| **II. Typed errors** | The preference store is local: it returns a bare `Flow` and `Unit`, never `AppResult`. No exception crosses `:core:data`. | ✅ |
| **III. UI renders, doesn't decide** | Chip derivation, filtering (by `GameStatus` enum, not label) and filter reset live in the ViewModel and mapper. The composable only emits `OnViewModeToggled` / `OnStatusFilterSelected`. All text is `UiText` or string resources. State is collected with `collectAsStateWithLifecycle()` (unchanged). New UiModels are `@Immutable`. | ✅ |
| **IV. Reuse first** | Reuses `VerticalGameCard`, `CustomFilterChip`, `StatusSectionHeader`, `SwipeToRevealRow`, `RemoveGameDialog`, the settings DataStore, and `MaterialTheme.spacing` tokens (card padding copied from `SearchResultGrid`). `CustomSegmentedButton` was considered and rejected on height (R6). The `VerticalGameCard` parameter changes extend the shared card instead of forking it. The filtered-empty state reuses `EmptyPage` (R10). | ✅ |
| **V. Local verification** | Plan names real Gradle commands only ([quickstart.md](quickstart.md)). New logic gets tests in its own module's `src/test`. | ✅ |
| **Constraints** | No DB bump, no `SavedStateHandle`, no KMP restructuring. DataStore and the enum are KMP-friendly (R1). | ✅ |
| **Workflow** | One type per file, with the sealed `WishlistStatusFilter` keeping its two implementations in its file (sealed-hierarchy exception). `UiModel` suffix. `internal` by default. Own `R` bare, `CoreUiR` aliased. English KDoc. | ✅ |

**Drift corrected**: the constitution said the app is dark-only, and its test-suite list omitted
`feature/wishlist`. It was amended to v1.0.1 (PATCH) with the owner's approval. The `CLAUDE.md` files
already matched the source.

## Project Structure

### Documentation (this feature)

```text
specs/007-wishlist-grid-toggle/
├── spec.md
├── plan.md                              # this file
├── research.md                          # R1–R10 decisions
├── data-model.md                        # types, derivation rules, state transitions
├── quickstart.md                        # build/test commands + manual scenarios Q1–Q17
├── contracts/
│   ├── wishlist-view-mode-store.md      # domain store + use cases + DataStore impl
│   └── wishlist-screen-ui.md            # layout, events, VerticalGameCard params, strings
├── checklists/requirements.md
└── tasks.md                             # /speckit-tasks (not created here)
```

### Source Code (repository root)

```text
core/model/src/main/java/com/nikolasguillen/questlog/core/model/
└── WishlistViewMode.kt                                   # NEW  enum LIST(0)/GRID(1) + fromId

core/domain/src/main/java/com/nikolasguillen/questlog/core/domain/
├── settings/WishlistViewModePreferenceStore.kt           # NEW  interface
└── usecase/list/
    ├── GetWishlistViewModeUseCase.kt                     # NEW
    └── SetWishlistViewModeUseCase.kt                     # NEW

core/data/src/main/java/com/nikolasguillen/questlog/core/data/
├── settings/WishlistViewModePreferenceStoreImpl.kt       # NEW  DataStore impl, key "wishlist_view_mode"
└── di/DataModule.kt                                      # EDIT @Binds for the new store
core/data/src/test/java/com/nikolasguillen/questlog/core/data/settings/
└── WishlistViewModePreferenceStoreImplTest.kt            # NEW

core/ui/src/main/java/com/nikolasguillen/questlog/core/ui/component/gamecard/
└── VerticalGameCard.kt                                   # EDIT nullable onSaveClick, required onLongClickLabel
core/ui/CLAUDE.md                                         # EDIT document both card params

feature/search/.../components/SearchResultGrid.kt         # EDIT pass choose-list label explicitly (CoreUiR)

feature/wishlist/src/main/java/com/nikolasguillen/questlog/feature/wishlist/
├── WishlistViewModel.kt                                  # EDIT combine(detail, viewMode, filter); 2 events; reset only on empty list
├── WishlistScreen.kt                                     # EDIT hoist LazyGridState; FilteredEmpty branch; reset revealedGameId on mode
├── components/
│   ├── WishlistGamesList.kt                              # EDIT LazyColumn → LazyVerticalGrid, LIST/GRID items
│   ├── WishlistViewOptionsRow.kt                         # NEW  chip LazyRow + toggle
│   └── WishlistViewModeToggle.kt                         # NEW  icon toggle with content/state descriptions
├── mapper/WishlistUiMapper.kt                            # EDIT + toSectionLabel() (extracted), + toFilterChips(), + filteredBy()
└── model/
    ├── WishlistUiState.kt                                # EDIT + viewMode, + filterChips
    ├── WishlistContentState.kt                           # EDIT + FilteredEmpty
    ├── WishlistUiEvent.kt                                # EDIT + OnViewModeToggled, + OnStatusFilterSelected
    ├── WishlistStatusFilter.kt                           # NEW  sealed All / Only(status)
    └── WishlistFilterChipUiModel.kt                      # NEW
feature/wishlist/src/main/res/values/strings.xml          # EDIT + 6 strings

.specify/memory/constitution.md                           # EDIT v1.0.1: light/dark, wishlist test suite
feature/wishlist/src/test/java/com/nikolasguillen/questlog/feature/wishlist/
├── WishlistViewModelTest.kt                              # EDIT helper gets new use cases; new cases
└── mapper/WishlistUiMapperTest.kt                        # NEW  chips + filtering rules
```

**Structure Decision**: This follows the existing module layout; no new module. Persistence follows the
`AppearancePreferenceStore` slice through `core:model` → `core:domain` → `core:data`. The UI stays
inside `feature/wishlist` following `feature/CLAUDE.md` (`components/`, `mapper/`, `model/`). The only
shared-layer edit is the `VerticalGameCard` change and its one call site in Search.

## Design Notes

**ViewModel pipeline** (shape kept: `stateIn(WhileSubscribed(5000))`):

```text
getWishlistDetailUseCase(listId)
  .distinctUntilChanged { old, new -> old == null && new == null }   # unchanged
  .onEach { NavigateBack if null; reset filter to All if the list has no games at all }   # + reset
combine(detailFlow, getWishlistViewModeUseCase(), selectedFilter) { detail, mode, filter -> WishlistUiState }
  .stateIn(...)
```

Event handlers: `OnViewModeToggled` launches `setWishlistViewModeUseCase(other(uiState.value.viewMode))`.
`OnStatusFilterSelected(f)` sets `selectedFilter.value = f`.

**Grid rendering**: see [contracts/wishlist-screen-ui.md](contracts/wishlist-screen-ui.md). Both views
share one `LazyGridState`, hoisted in `WishlistContent` so that spec 008's back-to-top button can
observe it without further restructuring.

**Spec 008 interaction**: the filter/view row is item index 1. Spec 008's threshold ("first visible
item index > 1") therefore means "header and row scrolled away" in both views, which matches its
updated wording.

## Testing

| Suite | New / changed cases |
|-------|---------------------|
| `core/data` `WishlistViewModePreferenceStoreImplTest` (new) | defaults to LIST; set → observe; survives a second store instance over the same file. |
| `feature/wishlist` `WishlistUiMapperTest` (new) | no chips for 0 or 1 sections under `All`; chips kept under `Only(s)` even with 1 section; "All" + per-section chips in section order with the right label; a filtered status with no games keeps its (selected) chip in its ordered slot with the right label; exactly one selected; `Only(status)` keeps one section; `Only(null)` keeps "No status"; `All` keeps every section; `Only(s)` with no matching section yields no sections. |
| `feature/wishlist` `WishlistViewModelTest` (edit) | `newViewModel()` gains mocked `GetWishlistViewModeUseCase` (default `flowOf(LIST)`) and `SetWishlistViewModeUseCase`. New: state reflects the stored mode; `OnViewModeToggled` persists the opposite mode; `OnStatusFilterSelected` narrows sections and marks the chip; the filter survives a mode change; the header count ignores the filter; when the filtered status empties, the state is `FilteredEmpty`, the filter and chip are kept, and a game regaining the status gives `Success` again; a list going entirely empty yields `Empty`, no chips, and the filter back at `All`. |
| `feature/search` | existing suite stays green. `SearchResultGrid` is updated for the required label; no Search ViewModel change. |

`WishlistViewMode.fromId` fallback is asserted in the store test (unknown id gives LIST). It isn't worth
a separate `:core:model` test source set.

## Complexity Tracking

No constitution violations. Nothing to justify.
