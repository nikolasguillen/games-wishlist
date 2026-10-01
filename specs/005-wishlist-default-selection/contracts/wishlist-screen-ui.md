# Contract: List Detail Screen (`:feature:wishlist`)

## State: `WishlistUiState`

See [data-model.md § UI state](../data-model.md#ui-state-featurewishlist). The screen's contract is
these two flags:

| `isDefaultList` | `showListOptions` | Top bar renders                                             |
|-----------------|-------------------|-------------------------------------------------------------|
| `false`         | `false`           | Title only. The screen is still loading.                    |
| `true`          | `false`           | Title + "Default" badge (`CustomSummaryBadge`). No menu.    |
| `false`         | `true`            | Title + "⋮" menu → **Set as default**, **Delete list**.     |
| `true`          | `true`            | Never produced.                                             |

## Events: `WishlistUiEvent`

| Event                    | Change     | Effect                                                                     |
|--------------------------|------------|----------------------------------------------------------------------------|
| `OnSetAsDefault`         | **new**    | `SetDefaultListUseCase(listId)`, then `ShowSnackbar(default_list_set_message)` |
| `OnWishlistDeleted`      | unchanged  | Unchanged: `NavigateBack` on success, `ShowSnackbar(unable_to_delete_wishlist)` on refusal |
| `OnGameRemoved(gameId)`  | unchanged  | Unchanged                                                                  |

No confirmation dialog for `OnSetAsDefault` (clarification Q2). The menu item emits the event directly.

## Effects: `WishlistUiEffect`

Unchanged. `ShowSnackbar(message: UiText)` is reused.

## Strings: `feature/wishlist/src/main/res/values/strings.xml`

| Key                        | Value (en)                       |
|----------------------------|----------------------------------|
| `set_as_default_action`    | `Set as default`                 |
| `default_list_set_message` | `This is now your default list`  |

The "Default" label is not in this file. `default_list_label` lives in `:core:ui`, and the top bar reads
it as `CoreUiR.string.default_list_label` (research R11).

## Reactive behavior

After `OnSetAsDefault`, nothing changes the state by hand. `GetWishlistDetailUseCase` re-emits with
`isDefault = true`, the ViewModel maps that to `isDefaultList = true, showListOptions = false`, and the
menu is swapped for the badge. Navigating back to the previous default's screen shows the menu again,
for the same reason.

## Unchanged entry point

`GameDetailActionPill`'s heart (`onFavoriteClick` → `GameDetailUiEvent.ToggleFavorite`) needs no change
to its contract. It now resolves to the user's chosen default through `ToggleWishlistUseCase`.
