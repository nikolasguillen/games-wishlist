# Data Model: Edit Wishlist

There is no schema change. The `wishlists` table, `ListEntity`, `WishlistList` and `default_wishlist` are
unchanged, so the exported schema is not regenerated.

## Persisted entity (unchanged): `ListEntity` / `WishlistList`

| Field            | Editable | Rule                                                                       |
|------------------|----------|----------------------------------------------------------------------------|
| `id`             | No       | Identity. Never rewritten (FR-008).                                        |
| `name`           | Yes      | Required, non-blank after trim. Enforced by the form, as on creation (FR-007). Not unique. |
| `description`    | Yes      | Trimmed. `""` means no description, and the header then hides it.         |
| `icon`           | Yes      | `WishlistIcon?`. `null` falls back to the default icon.                    |
| `coverImagePath` | Yes      | An absolute path under `filesDir/wishlist_covers/`, or `null`. Changed only through `CoverImageUpdate`. |

Not editable here: the games (`game_list_cross_ref`), their statuses (`games`), and the default pointer
(`default_wishlist`). An `@Update` on `wishlists` cannot touch any of them (FR-008, FR-011).

## New domain type: `CoverImageUpdate` (`:core:domain`, `domain/model/`)

A sealed interface. It says what to do with the cover image when an edit is saved.

| Case                        | Meaning                                  | Resulting `coverImagePath`             | Old file   |
|-----------------------------|------------------------------------------|----------------------------------------|------------|
| `Keep`                      | The user did not touch the cover.        | unchanged                              | kept       |
| `Remove`                    | The user removed the cover.              | `null`                                 | deleted after the row write |
| `Replace(sourceUri: String)`| The user picked a new image (`content://`). | the new persisted path                 | deleted after the row write |
| `Replace` + persist failed  | The copy failed.                         | **unchanged** (the old cover is kept)  | kept, and `FileStorage` failure is returned |

## New UI model: `WishlistFormUiModel` (`:core:ui`, `ui/model/`)

`@Immutable data class`. All fields have defaults, so `WishlistFormUiModel()` is the empty create form.

| Field         | Type            | Default | Notes                                                       |
|---------------|-----------------|---------|-------------------------------------------------------------|
| `name`        | `String`        | `""`    | User text, not a resource → `String`.                       |
| `description` | `String`        | `""`    |                                                             |
| `icon`        | `WishlistIcon?` | `null`  |                                                             |
| `coverImage`  | `String?`       | `null`  | Either a stored file path (pre-fill) or a picked `content://` URI. |

The same type is the sheet's `initialValues` and its `onConfirm` payload. On confirm, `name` and
`description` are already trimmed.

## UI state: `WishlistUiState` (`:feature:wishlist`)

| Field             | Change    | Value                                                                      |
|-------------------|-----------|----------------------------------------------------------------------------|
| `showEditAction`  | **new**   | `false` in the default (loading) state. `true` once a `WishlistDetail` is loaded, **for every list, default included**. |
| `formValues`      | **new**   | `WishlistFormUiModel(list.name, list.description, list.icon, list.coverImagePath)`, or `WishlistFormUiModel()` while loading. |
| `showListOptions` | unchanged | Still `!isDefault`. It gates **Set as default** and **Delete** only.       |
| all other fields  | unchanged |                                                                            |

## State transitions

```
Detail screen ──tap pencil──▶ Edit sheet open (fields = formValues)
Edit sheet ──Cancel / swipe / back──▶ Detail screen, list untouched (FR-012)
Edit sheet ──Save (name non-blank)──▶ sheet closes ──▶ OnListEdited(values)
   └─▶ ViewModel derives CoverImageUpdate ──▶ UpdateListUseCase ──▶ repository.updateList
         ├─ success ─▶ Room re-emits ─▶ header, overview and picker show the new values (FR-009, FR-010)
         └─ FileStorage failure ─▶ other fields are saved, old cover is kept, snackbar is shown
```
