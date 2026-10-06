# Data Model: Wishlist Back-to-Top Button

**Feature**: [spec.md](spec.md) | **Plan**: [plan.md](plan.md) | **Research**: [research.md](research.md)

**There is no data model change.** No entity, no database column, no stored preference, no domain or UI
model, no `UiState` / `UiEvent` / `UiEffect` field. The spec has no Key Entities section, and nothing here
is persisted (the spec's last assumption: "not remembered across sessions and has no setting").

The only state is one derived, in-memory boolean inside the wishlist screen.

## Derived UI state: `showScrollToTop`

| Part | Definition |
|------|------------|
| Source | the hoisted `LazyGridState` of the wishlist grid (spec 007) |
| Scroll test | `gridState.firstVisibleItemIndex > UiConstants.SCROLL_TO_TOP_AFTER_ITEM_INDEX` (the constant is `1`) |
| Content test | `state.contentState is WishlistContentState.Success` |
| Button visible | scroll test **and** content test |
| Held as | `derivedStateOf { ... }` over the scroll test, so the screen recomposes only when it flips |

### Why that index

Item order in the grid is fixed by spec 007: `0` list header, `1` filter/view row, `2` first status header,
then the games. "First visible item is beyond index 1" therefore means the header and the row have both
scrolled out of view.

### Transitions

```text
                 scroll down until firstVisibleItemIndex > 1
   HIDDEN  ─────────────────────────────────────────────────────▶  SHOWN
     ▲                                                               │
     │   scroll up to index <= 1, or tap the button (animates to 0)  │
     └───────────────────────────────────────────────────────────────┘

   content becomes Loading / Empty / FilteredEmpty ──▶ HIDDEN (whatever the scroll state says)
```

The content test is what makes the last line true: the hoisted scroll state survives those branches and can
hold a stale index (research R5), so the scroll test alone is not enough.

## Shared constant

| Name | Where | Value | Used by |
|------|-------|-------|---------|
| `UiConstants.SCROLL_TO_TOP_AFTER_ITEM_INDEX` | `core/ui/util/Constants.kt` | `1` | Search (both its lists) and the wishlist grid |
