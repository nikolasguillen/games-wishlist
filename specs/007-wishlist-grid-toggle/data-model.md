# Data Model: Wishlist Grid View Toggle

**Feature**: [spec.md](spec.md) | **Plan**: [plan.md](plan.md) | **Research**: [research.md](research.md)

No Room entity changes. The database stays at `version = 1`, and no schema is regenerated.

---

## New domain types

### `WishlistViewMode` — `:core:model`

`core/model/src/main/java/com/nikolasguillen/questlog/core/model/WishlistViewMode.kt`

| Entry | `id` (persisted) | Meaning |
|-------|------------------|---------|
| `LIST` | `0` | Rows grouped by status (today's layout). **Default.** |
| `GRID` | `1` | Two-column vertical cards grouped by status. |

- `companion fun fromId(id: Int): WishlistViewMode`. An unknown id falls back to `LIST`, never throws.
- Ids are stable: never renumber, only append (same rule as `AppearanceMode`).
- Pure Kotlin. No Android or Compose import, so `:core:model` keeps its KMP-ready status.

### Persisted preference

| Store | Key | Type | Absent value |
|-------|-----|------|--------------|
| `DataStore<Preferences>` named `"settings"` (existing) | `wishlist_view_mode` | `Int` (`WishlistViewMode.id`) | `LIST` |

App-wide: one value for every wishlist (FR-009). Written only by `SetWishlistViewModeUseCase`.
See [contracts/wishlist-view-mode-store.md](contracts/wishlist-view-mode-store.md).

---

## New feature-layer types — `feature/wishlist/.../model/`

### `WishlistStatusFilter` (sealed; implementations in the same file)

| Implementation | Meaning |
|----------------|---------|
| `data object All` | No filtering. **Default**, and the value every new ViewModel starts with. |
| `data class Only(val status: GameStatus?)` | Show only the section whose `status` equals this. `null` = the "No status" section. |

This is held by the ViewModel only and is never persisted (see the spec's Assumptions).

### `WishlistFilterChipUiModel` (`@Immutable`)

| Field | Type | Notes |
|-------|------|-------|
| `filter` | `WishlistStatusFilter` | What selecting the chip applies. The chip's identity and LazyRow key. |
| `label` | `UiText` | `"All"` → new `R.string.filter_all`; status chips reuse the section's `label`. |
| `isSelected` | `Boolean` | True for exactly one chip in the list. |

---

## Changed feature-layer types

### `WishlistUiState` — two new fields

| Field | Type | Default | Notes |
|-------|------|---------|-------|
| `viewMode` | `WishlistViewMode` | `LIST` | From `GetWishlistViewModeUseCase`. |
| `filterChips` | `List<WishlistFilterChipUiModel>` | `emptyList()` | Empty means the chip strip is hidden: fewer than two statuses have games and the filter is `All`, or the state is `Loading`/`Empty`. |

All existing fields are unchanged. In particular, `gameCountText` keeps counting **all** games in the
list, whatever the filter (FR-017).

### `WishlistContentState` — one new case, `Success` narrowed

| Case | Meaning |
|------|---------|
| `Loading` | unchanged |
| `Empty` | unchanged: the **list** has no games. |
| `FilteredEmpty` (new `data object`) | The list has games, but none with the filtered status. Rendered as header + row + centered `EmptyPage`. |
| `Success(sections)` | `sections` is now the **filtered**, never empty, list. It holds every section under `All`, and exactly the one matching section under `Only(status)`. |

`WishlistSectionUiModel`: unchanged.

### `WishlistUiEvent` — two new cases

| Event | Payload | Effect |
|-------|---------|--------|
| `OnViewModeToggled` | — | Persists the other mode via `SetWishlistViewModeUseCase`. `uiState` follows through the observed flow, with no local mirror. |
| `OnStatusFilterSelected` | `filter: WishlistStatusFilter` | Sets the in-memory filter. Selecting the already-selected chip is a no-op. |

`WishlistUiEffect`: unchanged. The toggle and the filter need no one-shot effect.

---

## Derivation rules (ViewModel / mapper, never composable)

Given `allSections = detail.games.toWishlistSectionUiModel()` (unchanged mapper):

1. **Chip statuses**: `chipStatuses = allSections.map { it.status } ∪ { s if currentFilter is Only(s) }`,
   ordered by section order (`STATUS_ORDER`, then `null`).
2. **Chips**: if `allSections.isEmpty()` (the list has no games), the result is `emptyList()` **whatever the
   filter is**: the whole row is hidden for an empty list. Else if `allSections.size < 2` and
   `currentFilter == All`, the result is also `emptyList()`. Otherwise
   it's `[All] + chipStatuses.map { Only(it) }`, labelled via `GameStatus?.toSectionLabel()` (extracted
   from the section mapper so a retained, game-less status still has a label), with
   `isSelected = (chip.filter == currentFilter)`.
3. **Visible sections**: `All` gives `allSections`. `Only(s)` gives `allSections.filter { it.status == s }`,
   comparing the enum value (by ID).
4. **Content state**: `allSections.isEmpty()` gives `Empty`. Otherwise, if `visibleSections.isEmpty()` it's
   `FilteredEmpty`. Otherwise it's `Success(visibleSections)`.

Rules 1–3 are new internal mapper functions in `mapper/WishlistUiMapper.kt`, next to
`toWishlistSectionUiModel()`, so they can be unit-tested without the ViewModel.

---

## State transitions

### View mode

```text
           OnViewModeToggled                 OnViewModeToggled
  LIST ───────────────────────────▶ GRID ───────────────────────────▶ LIST
   ▲  (persisted; survives relaunch, applies to every wishlist)
   └── initial value on fresh install / unknown stored id
```

The list becoming empty does **not** change the stored mode (FR-012). The toggle is just not rendered.

### Status filter (per ViewModel instance)

```text
                OnStatusFilterSelected(Only(s))
     All ──────────────────────────────────────▶ Only(s) ──┐ last game of s removed/re-statused:
      ▲  ◀──────────────────────────────────────   │       │ filter KEPT, content = FilteredEmpty,
      │       OnStatusFilterSelected(All)          │  ◀────┘ chip s stays selected; a game gaining s
      │                                            │         brings content back to Success
      └────────────────────────────────────────────┘
          new detail arrives with NO games at all (whole list emptied) → reset to All

  New ViewModel (screen re-entered) → All
```

The one automatic reset happens only when the whole list empties (spec FR-016, edge case "Whole list
emptied while a filter is on").

### Content state

```text
Loading ──detail──▶ Empty            (no games)
                 ├─▶ Success          (games visible under the current filter)
                 └─▶ FilteredEmpty    (games exist, none in the filtered status)
Success ⇄ FilteredEmpty   via filter changes or games changing status
any ──▶ Empty             when the last game is removed (filter reset to All)
```

### Revealed swipe row (UI-local, unchanged owner)

`revealedGameId` resets to `null` whenever `viewMode` changes (spec edge case "Open revealed row when
switching views").
