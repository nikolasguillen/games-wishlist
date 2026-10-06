# Contract: List Detail Screen (`:feature:wishlist`)

Covers FR-001, FR-002, FR-009, FR-011 and FR-014, plus US1 to US3 at the screen level.

## State: `WishlistUiState`

See [data-model.md § UI state](../data-model.md#ui-state-wishlistuistate-featurewishlist). The top bar
renders from two independent flags:

| State                  | `showEditAction` | `showListOptions` | Top-bar actions                          |
|------------------------|------------------|-------------------|------------------------------------------|
| Loading                | `false`          | `false`           | none                                     |
| Default list           | `true`           | `false`           | ✏️ Edit                                   |
| Non-default list       | `true`           | `true`            | ✏️ Edit, ✓ Set as default, 🗑 Delete        |

## Top bar: `WishlistTopBar`

```kotlin
@Composable
internal fun WishlistTopBar(
    showEditAction: Boolean,
    showListOptions: Boolean,   // renamed from showActions: it no longer covers every action
    onBackClick: () -> Unit,
    onEditClick: () -> Unit,
    onSetAsDefaultClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
)
```

- The Edit `IconButton` uses `Icons.Default.Edit`, the same `iconButtonColors` as its siblings, and
  `contentDescription = stringResource(R.string.edit_list_action)`.
- The two existing previews (non-default, default) are updated: the default preview shows the pencil alone.

## Events: `WishlistUiEvent`

| Event                               | Change    | Handling in `WishlistViewModel`                                        |
|-------------------------------------|-----------|------------------------------------------------------------------------|
| `OnListEdited(values: WishlistFormUiModel)` | **new** | Derive `CoverImageUpdate` from `values.coverImage` vs `uiState.value.formValues.coverImage` (equal → `Keep`, `null` → `Remove`, else → `Replace`). Call `UpdateListUseCase`. On `Failure` → `ShowSnackbar(error.toUiText())`. On success, no effect. |
| `OnSetAsDefault`, `OnWishlistDeleted`, `OnGameRemoved` | unchanged | unchanged                                         |

`WishlistUiEffect` is unchanged. `ShowSnackbar` already exists.

## Screen wiring: `WishlistContent`

- `var showEditSheet by rememberSaveable { mutableStateOf(false) }` (research R8).
- Top bar `onEditClick = { showEditSheet = true }`.
- While `showEditSheet` is true:

```kotlin
WishlistFormSheet(
    title = stringResource(R.string.edit_wishlist_sheet_title),
    confirmLabel = stringResource(CoreUiR.string.save_label),
    initialValues = state.formValues,
    onDismiss = { showEditSheet = false },
    onConfirm = { values ->
        onEvent(WishlistUiEvent.OnListEdited(values))
        showEditSheet = false
    }
)
```

- The previews in `WishlistScreen.kt` set `showEditAction = true`.

## ViewModel injection

`WishlistViewModel` keeps `@AssistedInject` and gains a constructor-injected `UpdateListUseCase`. There is
no `SavedStateHandle`.

## Strings (`feature/wishlist/src/main/res/values/strings.xml`)

| Key                         | Value           |
|-----------------------------|-----------------|
| `edit_list_action`          | `Edit list`     |
| `edit_wishlist_sheet_title` | `Edit Wishlist` |

The confirm label reuses `CoreUiR.string.save_label` ("Save"), and the cover-failure text reuses
`error_file_storage` through `RepositoryError.toUiText()`.
