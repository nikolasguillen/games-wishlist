# Research: Edit Wishlist

Phase 0 for [plan.md](./plan.md). The Technical Context had no `NEEDS CLARIFICATION` items. Each entry
below records a design decision the spec left to planning.

## R1. Where the shared form lives

**Decision**: Move `CreateWishlistSheet` out of `:feature:lists` into `:core:ui` as the public
`component/WishlistFormSheet.kt`. Both features call it.

**Rationale**: The spec (FR-003) asks for the same form, not a copy. The two callers live in sibling feature
modules (`:feature:lists` creates, `:feature:wishlist` edits), and a feature may not depend on another
feature (Constitution I). `:core:ui` is already a dependency of both. It already holds the sheet's building
blocks (`CustomModalBottomSheet`, `WishlistIcon.toDrawableRes()`), declares `coil.compose` and
`activity-compose` (needed for `AsyncImage` and the Photo Picker launcher), and already hosts one
domain-shaped sheet, `ListSelectorSheet`. **No new dependency edge.**

**Alternatives considered**:

- *`:feature:wishlist` depends on `:feature:lists`*: rejected, because it breaks Constitution I.
- *Copy the sheet into `:feature:wishlist`*: rejected, because it breaks Constitution IV. The two copies
  would drift, which is exactly what FR-003 forbids.
- *Move list creation into the detail screen's module*: rejected. It changes where creation happens and is
  out of scope.

## R2. How the form is told it is creating or editing

**Decision**: The sheet takes `title: String`, `confirmLabel: String` and
`initialValues: WishlistFormUiModel = WishlistFormUiModel()`. Each caller resolves its own title and
confirm label. The sheet has no create/edit mode.

**Rationale**: The only differences between the modes (FR-004, FR-005) are two labels and the starting
values. Passing them in keeps the shared component ignorant of its callers. The mode-specific strings stay
in the feature that owns the mode: "New Wishlist" / "Create" in `:feature:lists`, "Edit Wishlist" in
`:feature:wishlist`. The edit confirm label reuses the existing `CoreUiR.string.save_label` ("Save").

**Alternatives considered**: A `WishlistSheetMode { CREATE, EDIT }` enum in `:core:ui`. Rejected, because it
would pull both callers' titles into `:core:ui` and couple the component to its two use sites for no gain.

## R3. Shape of the form's data

**Decision**: Add a new `@Immutable data class WishlistFormUiModel(name, description, icon, coverImage)` in
`core/ui/model/`. It is both `initialValues` and the payload of `onConfirm`. `coverImage: String?` holds
whichever the sheet currently shows: the list's stored file path (pre-filled), a freshly picked `content://`
URI, or `null`.

**Rationale**: The existing callback takes four positional parameters, and two callers would now repeat
them. A single model also serves as the pre-fill carried by `WishlistUiState`. `name`, `description` and
`coverImage` are user data or a path, never a resource, so they are `String` per the `UiText` rule.

The sheet's preview branches on the string's shape instead of relying on Coil's string handling: a value
starting with `/` is a stored file path and is passed to `AsyncImage` as `File(path)`, exactly as the detail
header and overview rows already do. Anything else is a picked `content://` URI and is passed as the raw
string. The quickstart verifies the pre-filled cover manually (scenario 2).

## R4. Telling "keep", "remove" and "replace" apart for the cover image

**Decision**: Add a domain type `CoverImageUpdate` in `core/domain/model/`, a sealed interface with
`Keep`, `Remove` and `Replace(sourceUri)`. `WishlistViewModel` derives it when the user confirms, by
comparing the sheet's `coverImage` with the stored path in its current state:

- equal → `Keep`
- `null` → `Remove`
- anything else → `Replace`

**Rationale**: The repository has to know which of the three happened. `Replace` copies a new file in.
`Remove` and `Replace` delete the old file. `Keep` touches no file. A bare `String?` cannot say this
without guessing whether a string is a path or a URI. Deriving it in the ViewModel keeps the decision out
of the composable (Constitution III) and inside the JVM test suite. The type lives next to `WishlistDetail`
because the `GameRepository` interface, which consumes it, is in `:core:domain`.

**Alternatives considered**:

- *The repository compares the incoming string with the stored path*: rejected. It works, but it hides a
  three-way decision behind string comparison in the data layer.
- *Two parameters (`newCoverUri: String?`, `removeCover: Boolean`)*: rejected, because it allows the
  impossible combination of both.

## R5. Repository write, cover-file lifecycle and failure semantics

**Decision**: Add a new `GameRepository.updateList(listId, name, description, icon, coverImage): AppResult<Unit>`,
implemented as follows:

1. `listDao.getListById(listId)`. If it returns `null`, return success and do nothing. The list was
   deleted from under the edit, and the detail screen is already navigating back.
2. Resolve the new path:
   - `Keep`: the old path.
   - `Remove`: `null`.
   - `Replace`: `coverImageStorage.persist(uri)`, falling back to the old path if that returns `null`.
3. `listDao.updateList(existing.copy(name, description, icon, coverImagePath = newPath))`.
4. **After** the row is written, delete the old file if there was one and it differs from `newPath`.
5. Return `AppResult.failure(RepositoryError.FileStorage)` only when a `Replace` failed to persist. Every
   other field is still saved and the previous cover is kept.

**Rationale**: This mirrors `createList`, where the list is always created and a `Failure` means only that
the cover failed, so the UI handles both with the same snackbar. It also mirrors `deleteList`: row first,
then file. An orphaned file is invisible, whereas a row that points at a deleted file renders as a broken
cover. Keeping the old cover on a failed replace means a transient picker or IO error never silently
strips an image the user did not ask to remove.

`ListDao.updateList` (`@Update`) already exists and is unused today. **Do not use `insertList`**: its
`REPLACE` strategy deletes and re-inserts the row, and for the default list the `RESTRICT` foreign key in
`default_wishlist` refuses that delete. `@Update` changes only non-key columns, so the default pointer
(FR-011) and the `game_list_cross_ref` rows (FR-008) are untouched by construction.

**Accepted risk**: Steps 1 and 3 are not one transaction. If the list is deleted in between, `@Update`
affects 0 rows and a just-persisted cover becomes an orphaned file. This is invisible and has the same
trade-off `deleteList` already accepts. A single local user can hardly trigger it.

**Alternatives considered**: A `@Transaction` DAO method. Rejected, because the file IO cannot live inside
a Room transaction, and the only thing it would protect against is the invisible orphan.

## R6. "Immediately see the changes" (FR-009, FR-010, SC-002)

**Decision**: No manual refresh. The detail screen, the overview and the list picker all read reactive
Room `Flow`s that re-emit when the `wishlists` table changes:

- the detail screen: `GetWishlistDetailUseCase` → `observeListById`
- the overview: `GetListsUseCase` → `getAllLists`
- the list picker: `getAllLists`

A replaced cover gets a new UUID filename (`WishlistCoverImageStorage.persist`), so Coil's cache key changes
and no stale image can show.

## R7. Feedback after confirming

**Decision**: A successful edit is silent: the sheet closes and the header updates (spec Assumptions). A
`FileStorage` failure shows `ShowSnackbar(error.toUiText())`, which already resolves to
`error_file_storage`, the same as on creation. This needs no new effect type and no new error string.

## R8. Sheet visibility across rotation and process death

**Decision**: `WishlistContent` holds `var showEditSheet by rememberSaveable { mutableStateOf(false) }`.
The sheet's own fields are already `rememberSaveable` and read `initialValues` only on first composition.

**Rationale**: The activity declares no `configChanges`, so rotation recreates it. With plain `remember`,
the sheet would close and the typed text would be lost. Since the sheet leaves composition when it is
dismissed, its saveable state is discarded at that point, and reopening reads fresh `initialValues` from
the current state (FR-012, US3 scenario 2).

After process death, the restored form fields win over the not-yet-loaded state, which is the point of
restoring them. The `Keep`/`Remove`/`Replace` comparison (R4) runs when the user confirms, against the
state as it is then. By then Room has re-emitted.

**Observed, not acted on**: `ListsContent` keeps `showCreateSheet` in plain `remember`, so the **create**
sheet closes on rotation today. The spec's edge case wording ("matching the creation form's behaviour")
was corrected to drop that comparison. Aligning the create flow is a one-word change, but it is outside
this feature. That is the owner's call.

## R9. The pencil button

**Decision**: `Icons.Default.Edit`, the first action in `WishlistTopBar`, with the content description
`edit_list_action` ("Edit list"), which is new in `:feature:wishlist`. It is gated by a new
`showEditAction` flag that the ViewModel sets to `true` as soon as the list has loaded, for every list.
The existing `showListOptions` gate keeps controlling **Set as default** and **Delete** only (FR-001,
FR-002).

**Rationale**: While the screen is loading there are no values to pre-fill, so the button would open an
empty form. A dedicated flag keeps that decision in the ViewModel instead of a
`contentState != Loading` check in the composable. Material icons come transitively from `:core:ui`'s
`api(...)` declaration.

## R10. Test coverage

**Decision**:

- **`:core:data`**: a new `GameRepositoryImplUpdateListTest`, modelled on
  `GameRepositoryImplDeleteListTest`, covering every R5 branch.
- **`:feature:wishlist`**: extend `WishlistViewModelTest` to cover the pre-fill values, `showEditAction`
  for default and non-default lists, the `Keep`/`Remove`/`Replace` derivation, the snackbar on failure and
  silence on success.
- **`UpdateListUseCase`**: a pure delegation with no logic, like `CreateListUseCase`, which has no test
  either. It gets none.
- **`WishlistFormSheet`**: gets a `@QuestLogPreviews` (the current sheet has none). `:core:ui` has no
  test source set, which is a known gap in `docs/tech-debt.md`.

`ListsViewModelTest` is unchanged: `ListsUiEvent.OnListCreated` keeps its shape, and `ListsContent` maps
the form model onto it.
