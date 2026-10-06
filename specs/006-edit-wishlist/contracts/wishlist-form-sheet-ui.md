# Contract: Shared Wishlist Form (`:core:ui`)

Covers FR-003 to FR-007 and FR-012. It replaces `feature/lists/.../components/CreateWishlistSheet.kt`,
which is deleted.

## Component

`core/ui/src/main/java/com/nikolasguillen/questlog/core/ui/component/WishlistFormSheet.kt`

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishlistFormSheet(
    title: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (WishlistFormUiModel) -> Unit,
    initialValues: WishlistFormUiModel = WishlistFormUiModel()
)
```

| Behaviour                | Rule                                                                                     |
|--------------------------|------------------------------------------------------------------------------------------|
| Layout and fields        | Identical to today's `CreateWishlistSheet`: cover picker, name, description, icon row, Cancel and confirm. |
| Pre-fill                 | Each field's `rememberSaveable` is seeded from `initialValues` **once**, on first composition. |
| Cover preview            | `AsyncImage(model = File(coverImage))` when `coverImage` starts with `/` (stored path), `AsyncImage(model = coverImage)` otherwise (picked URI). Research R3. |
| Sizes                    | The icon size uses `MaterialTheme.spacing.extraLarge`. The `72.dp` cover and `48.dp` icon-button sizes stay literals (component sizes, listed in `docs/tech-debt.md`). |
| Title / confirm text     | `title` and `confirmLabel`, verbatim. Cancel stays `CoreUiR.string.cancel`.              |
| Confirm enabled          | `name.isNotBlank()`.                                                                      |
| Confirm payload          | `WishlistFormUiModel(name.trim(), description.trim(), icon, coverImage)`.                 |
| Dismiss                  | `onDismiss()`. The caller removes the sheet from composition, which drops the edited state. |
| Preview                  | A `private` `@QuestLogPreviews` in `QuestLogTheme { }`, pre-filled (edit-like) values.    |

The sheet owns no create/edit concept (research R2).

## Strings

| Key                                   | From                  | To                 |
|---------------------------------------|-----------------------|--------------------|
| `list_name_label`                     | `feature/lists`       | `core/ui`          |
| `description_optional_label`          | `feature/lists`       | `core/ui`          |
| `icon_optional_label`                 | `feature/lists`       | `core/ui`          |
| `cover_image_optional_label`          | `feature/lists`       | `core/ui`          |
| `add_cover_image_content_description` | `feature/lists`       | `core/ui`          |
| `remove_cover_image_action`           | `feature/lists`       | `core/ui`          |
| `new_wishlist_sheet_title`            | stays in `feature/lists` | caller-owned    |
| `create_action`                       | stays in `feature/lists` | caller-owned    |

Before moving a key, check that `core/ui/src/main/res/values/strings.xml` has no key of the same name. A
clash would be resolved silently at merge time. Delete each moved key from `feature/lists` in the same
commit, so the key is never defined twice.

## Caller: `ListsContent` (`:feature:lists`)

```kotlin
WishlistFormSheet(
    title = stringResource(R.string.new_wishlist_sheet_title),
    confirmLabel = stringResource(R.string.create_action),
    onDismiss = { showCreateSheet = false },
    onConfirm = { form ->
        onEvent(ListsUiEvent.OnListCreated(form.name, form.description, form.icon, form.coverImage))
        showCreateSheet = false
    }
)
```

`ListsUiEvent`, `ListsViewModel` and their tests are unchanged.

## Inventory

Add `WishlistFormSheet` (create/edit form for a wishlist, working against `WishlistFormUiModel`) next to
`ListSelectorSheet` in `core/ui/CLAUDE.md`'s component list.
