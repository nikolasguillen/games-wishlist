# Contract: Overview List (`:feature:lists`)

Covers FR-012 and SC-005: the default wishlist's row on the overview list of all wishlists shows the same
"Default" label as its detail screen.

## UI model: `WishlistListUiModel`

See [data-model.md § Overview list](../data-model.md#overview-list-coredomain-featurelists). The contract
is one new field, `isDefault: Boolean`.

## Row rendering: `WishlistRow`

| `isDefault` | The row shows                                                                              |
|-------------|--------------------------------------------------------------------------------------------|
| `true`      | List name, then a `CustomSummaryBadge` reading "Default" beside it, then the existing description and game count. |
| `false`     | Exactly what it shows today.                                                               |

- The badge is informational. It has no click handling, and the row's click target is unchanged.
- The name stays on one line with an ellipsis, and the badge is never pushed out (research R12).
- The row order is unchanged. The default is not moved to the top.
- Spacing uses a `MaterialTheme.spacing` token, with no `dp` literal.

## State and events

| Type                | Change                                                                                  |
|---------------------|-----------------------------------------------------------------------------------------|
| `ListsUiState`      | None. `Success(lists)` now holds rows that carry `isDefault`.                            |
| `ListsUiEvent`      | None. There is no way to change the default from this screen (spec Assumptions).         |
| `ListsUiEffect`     | None.                                                                                    |
| `ListsViewModel`    | Maps `WishlistSummary` to `WishlistListUiModel`. It stays a single `stateIn` pipeline.   |

## Strings

No new string in `feature/lists`. The badge text is `CoreUiR.string.default_list_label`.

## Reactive behavior

After "Set as default" on a detail screen, nothing refreshes by hand. `GetListsUseCase` re-emits with
the new default, so the label is already on the new row when the user goes back (US1 acceptance
scenario 4).
