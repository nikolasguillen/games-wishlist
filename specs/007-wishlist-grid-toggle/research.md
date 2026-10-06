# Research: Wishlist Grid View Toggle

**Feature**: [spec.md](spec.md) | **Plan**: [plan.md](plan.md) | **Date**: 2026-10-06

The spec left no `[NEEDS CLARIFICATION]` markers. The open points below came out of reading the code the
feature touches; each is resolved here.

---

## R1. Where the list/grid choice is persisted

**Decision**: A new `WishlistViewModePreferenceStore` interface in `core/domain/settings/`, implemented in
`core/data/settings/` on top of the **existing** `DataStore<Preferences>` (`"settings"`) that
`DataModule.provideSettingsDataStore` already provides, under a new `intPreferencesKey("wishlist_view_mode")`.
The value is a new `WishlistViewMode` enum in `:core:model` that carries an explicit, stable `id`
(`LIST(0)`, `GRID(1)`) with a `fromId` that falls back to `LIST`. Two use cases expose it:
`GetWishlistViewModeUseCase` (Flow) and `SetWishlistViewModeUseCase` (suspend).

**Rationale**:
- It is a copy of a shape that already exists and is already tested: `AppearancePreferenceStore` /
  `AppearancePreferenceStoreImpl` / `AppearanceMode(id)` / `Get|SetAppearanceModeUseCase`. Same file
  layout, same test style (`AppearancePreferenceStoreImplTest` uses a temp-file-backed DataStore, no
  Robolectric).
- FR-009 requires survival across relaunch and across wishlists. Only an app-wide persisted preference
  does that; `rememberSaveable` survives rotation only.
- Persisting the `id` rather than the enum name keeps the stored value stable if the enum is ever
  renamed, which is the convention `AppearanceMode` documents.
- `:core:domain` never imports `androidx.datastore`; the interface sits in `:core:domain`, the
  DataStore code in `:core:data`. The feature module reaches it only through use cases, so no new
  module edge is needed.

**Alternatives considered**:
- *Add the key to `AppearancePreferenceStore`.* Rejected: that interface is named and documented for
  appearance. Folding an unrelated preference into it makes the name wrong and forces every appearance
  consumer to see a wishlist method.
- *A generic `UserPreferencesStore` with many keys.* Rejected for now: a speculative abstraction for two
  values. Worth revisiting if a third preference arrives.
- *A Room column (per list or a settings table).* Rejected: the clarification made the choice app-wide,
  and Room would need a schema change for one int.
- *`rememberSaveable` only.* Rejected: it fails FR-009 and SC-004 (relaunch).

**KMP note**: DataStore Preferences is multiplatform, and the enum lives in `:core:model`, so this
choice is not expensive to carry into a KMP migration.

---

## R2. How the status filter is held, and how it resets

**Decision**: The ViewModel owns a private `MutableStateFlow<WishlistStatusFilter>` (default
`WishlistStatusFilter.All`). `uiState` becomes a `combine` of three sources: the wishlist detail flow
(unchanged, with its existing `distinctUntilChanged` + `NavigateBack` handling kept *before* the
combine), the view-mode flow, and the filter flow. A filter whose status has run out of games is **not**
reset. It yields `WishlistContentState.FilteredEmpty` instead (see R10). The single reset happens on the
detail flow: when the *whole list* becomes empty, the filter is set back to `All`, because the row that
shows it is gone and a stale filter would otherwise hide the next game added.

`WishlistStatusFilter` is a sealed interface: `All` and `Only(status: GameStatus?)`. A `null` status is
the "No status" section. That is why a nullable `GameStatus` alone cannot be the filter: `null` would have
to mean both "All" and "No status".

**Rationale**:
- The screen already uses the `stateIn(WhileSubscribed(5000))` shape. With three sources,
  `combine()`-ing them "is the whole job", which `feature/CLAUDE.md` names as the case for that shape.
- Filtering by the `GameStatus` enum value is filtering by ID, as the project rules require, never by
  label.
- The owner chose a visible "no games match" state over a silent reset (spec FR-016, US4-5/7). The
  user then always sees *why* the list looks empty. If a game later moves back into that status, it
  simply appears under the still-active filter.
- The filter is not persisted, as the Assumptions section of the spec requires. A fresh ViewModel
  starts on `All`.

**Alternatives considered**:
- *Filter in the composable.* Rejected: the constitution (Principle III) forbids filtering in the UI.
- *Switch the ViewModel to `MutableStateFlow` + `update {}`.* Rejected: that is a larger rewrite of a
  tested pipeline, for no gain. Both shapes are sanctioned.

---

## R3. One lazy container for both views, or two

**Decision**: One `LazyVerticalGrid(GridCells.Fixed(2))` renders **both** views. The list header, the
filter/view row and every status header span the full line (`GridItemSpan(maxLineSpan)`). In list view,
each game row also spans the full line. In grid view, each game takes one cell. The existing
`WishlistGamesList` keeps its name and its `SwipeToRevealRow` + `WishlistGameRow` rendering; only the
container changes from `LazyColumn` to `LazyVerticalGrid`.

**Rationale**:
- Header, filter row and section headers are written once and cannot drift between views.
- Odd-count sections need no special handling. A full-span item always starts a new line, so the next
  status header never shares a row with a lone card (spec edge case).
- One hoisted `LazyGridState` serves both modes. Spec 008 (back-to-top) needs exactly one scroll
  state to watch, and its threshold ("first visible item index > 1") then means the same thing in
  both views.
- Lazy items are composed only on screen, which covers SC-005 (500 games).

**Alternatives considered**:
- *Keep the `LazyColumn` and add a separate `LazyVerticalGrid`, swapping them.* Rejected: it duplicates
  the header, row and section-header items. It gives two scroll states for 008 to reconcile, and resets
  the scroll position on every toggle.
- *`LazyColumn` with manual `Row`s of two cards.* Rejected: it reimplements what grid spans already do
  and loses per-card keys and animations.

---

## R4. The search card's heart button inside a wishlist

**Finding**: `VerticalGameCard` always draws `SaveToWishlistButton`. That heart means *membership in the
default list* (`ToggleWishlistUseCase`), and its `isSaved` flag is only computed in Search. The wishlist
mapper calls `toGameItemList()` without saved IDs, so every wishlist card would show an **empty** heart,
including on games that are obviously in the list the user is looking at.

**Decision**: Make `VerticalGameCard.onSaveClick` nullable (`(() -> Unit)? = null`). When it is `null`,
the heart is not drawn. Search keeps passing a callback, so its cards are unchanged. The wishlist grid
passes `null`.

**Rationale**:
- The spec forbids an inconsistent "saved" display (Assumptions). Any heart on this screen would either
  lie, or carry a different meaning ("in *this* list") from the same icon in Search ("in the *default*
  list").
- Removal already has an agreed gesture (long-press, FR-008). A second removal affordance would also
  need the same confirmation and adds nothing.
- `SearchResultGrid` is the only caller of `VerticalGameCard` today, so the change touches one call
  site.

**Alternatives considered**:
- *Pass `isSaved = true` and route a tap to the remove confirmation.* Rejected: same icon, different
  semantics per screen.
- *Wire the heart to `ToggleWishlistUseCase`.* Rejected: on a non-default list, tapping it would add or
  remove the game from a *different* list than the one on screen.

---

## R5. Long-press label on the card

**Finding**: `VerticalGameCard` hardcodes the long-click accessibility label to
`choose_list_content_description`. In the wishlist grid the long-press removes the game, so screen
readers would announce the wrong action.

**Decision**: Add a **required** `onLongClickLabel: String` parameter to `VerticalGameCard`, with no
default (owner's call). The card stops resolving `choose_list_content_description` itself, and every
caller states what its long-press does:
- `SearchResultGrid` passes `stringResource(CoreUiR.string.choose_list_content_description)`, so it's
  behaviourally unchanged.
- The wishlist grid passes its existing `remove_game_action`.

The same label also goes to `SaveToWishlistButton.longClickLabel` when the heart is drawn, because both
long-press targets trigger the same action. The long-press itself only sets the screen's
`gamePendingRemoval`, which opens the existing `RemoveGameDialog`; there is no new dialog.

**Out of scope, noted**: `CompactGameCard` hardcodes the same "choose list" label internally. It is not
used by this feature, so it is left as is rather than changed on the side.

---

## R6. The view toggle control

**Decision**: A single icon toggle at the end of the filter/view row. It shows the icon of the layout
it switches **to**: `Icons.Outlined.GridView` while in list, and `Icons.AutoMirrored.Outlined.ViewList`
while in grid. Its `contentDescription` states the action ("Show as grid" / "Show as list"), and its
`stateDescription` states the current view ("List view" / "Grid view"). The icons come from
`material-icons-extended`, already exposed as `api` by `:core:ui`.

**Rationale**:
- The row also has to host a horizontally scrolling chip strip, so a compact single control leaves
  that strip the most room. `CustomSegmentedButton` uses `spacing.large` vertical padding and is far
  too tall for a "slim row".
- FR-011: the active view is indicated by the content itself and announced through
  `stateDescription`; the action is the label.

**Alternatives considered**: A two-segment list|grid button (it shows the current state more literally,
but it's too wide and too tall next to chips). An overflow menu (fails SC-006 discoverability).

---

## R7. Filter chip row composition

**Decision**: The chip strip is a `LazyRow` that takes `weight(1f)`, with the toggle after it, so the
toggle is never pushed off-screen (spec edge case). Chips are `CustomFilterChip`s: "All" first, then one
per status in the set *{statuses with games} ∪ {the filtered status, if any}*, in section order
(`STATUS_ORDER`, then "No status"). The chip list is empty, and the row shows only the toggle aligned to
the end, when fewer than two statuses have games **and** the filter is `All`.

The retained chip of a filtered-but-empty status needs a label without a section to borrow it from. The
status-to-label logic inside `toWishlistSectionUiModel()` is therefore extracted into one internal
helper (`GameStatus?.toSectionLabel()`) that both sections and chips use.

`FilterChip` exposes the selected state to accessibility services on its own, which covers FR-018.

---

## R8. "Changing the filter while scrolled down" (spec edge case)

**Finding**: The chips scroll with the content, so a chip can only be tapped while the row is on screen.
Then the first sections are directly below. The grid also keeps its position anchored to the key of the
first visible item (the list header or the row), so shrinking the content below it never leaves the
user mid-list.

**Decision**: There is no explicit scroll action on a filter change. The spec's edge case was reworded to
match ("the row stays in place and the filtered content starts directly below it"). The quickstart
verifies it manually (Q7).

**Accepted limitation (owner decision, 2026-10-06)**: the `LazyGridState` is hoisted in
`WishlistContent`, so it survives the `Empty` and `FilteredEmpty` branches. After a list empties and
later refills, the grid may reopen at a stale scroll position (header scrolled off). The owner chose not
to reset the scroll for now. Do not add a reset without asking; revisit if it shows up in use.

---

## R9. Dismissing a revealed swipe row on toggle

**Decision**: `revealedGameId` is already UI-local state in `WishlistContent`. It is reset to `null`
whenever the view mode changes, keyed on `state.viewMode`. Nothing is removed. This is presentation
state only, so it stays out of the ViewModel, like `revealedGameId` does today.

---

## R10. "No games match this filter" state

**Decision**: A new `WishlistContentState.FilteredEmpty` case. The list has games, but none in the
filtered status. It renders like the existing `Empty` branch: a `Column` with the list header, the
filter-and-view row (selected chip and toggle still visible), and `EmptyPage(weight(1f))` with a new
`filtered_empty_message` and `Icons.Outlined.FilterAltOff`.

**Rationale**:
- The owner asked for it to look like the empty-list state, and `EmptyPage` is the shared component
  for that (Principle IV).
- A distinct case keeps the `when` exhaustive. It also keeps `Success(sections)` meaning "something to
  draw", so no composable has to inspect `sections.isEmpty()` (Principle III).
- A non-lazy `Column` mirrors `Empty` exactly. The content fits on screen by definition, so there is
  nothing to scroll. Spec 008's button therefore correctly stays hidden in this state.

**Alternatives considered**: a full-span grid item holding the message. Rejected: lazy grid items cannot
fill the remaining viewport height, so the message would not be centered.

---

## Constitution drift (corrected with the owner's approval, 2026-10-06)

`.specify/memory/constitution.md` said the app is dark-theme only and omitted `feature/wishlist` from
the list of test source sets. Both were out of date: spec 004 added light/dark, and
`WishlistViewModelTest` exists. The `CLAUDE.md` files were already correct, since
`core/designsystem/CLAUDE.md` documents the light/dark theme. The constitution was amended to match
(v1.0.1, PATCH).
