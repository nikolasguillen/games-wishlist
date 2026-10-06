---

description: "Task list for Edit Wishlist"
---

# Tasks: Edit Wishlist

**Input**: Design documents from `/specs/006-edit-wishlist/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

**Tests**: Included. The spec does not ask for them, but the constitution (Principle V) requires new
repository and ViewModel logic to get a test in its own module. The pass-through `UpdateListUseCase` gets
none (research R10), and `:core:ui` has no test source set (a known gap in `docs/tech-debt.md`), so the
shared sheet is covered by a preview and the manual scenarios in `quickstart.md`.

**Organization**: Tasks are grouped by user story, after a foundational phase that every story needs
(the repository write and the shared form). US2 and US3 add no production code of their own: US2 falls out
of `showEditAction` being independent of `showListOptions`, and US3 falls out of how the shared sheet
seeds and discards its state. Their phases are the tests and checks that prove those two properties.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependency on an incomplete task)
- **[Story]**: User story the task belongs to (US1, US2, US3)
- Every task names exact file paths.

## Path conventions

All paths are relative to the repository root. Sources live under `src/main/java/` (tests under
`src/test/java/`) in the package `com/nikolasguillen/questlog/...`. Abbreviations used below:

- `DOMAIN/` = `core/domain/src/main/java/com/nikolasguillen/questlog/core/domain/`
- `DATA/` = `core/data/src/main/java/com/nikolasguillen/questlog/core/data/`
- `DATA_TEST/` = `core/data/src/test/java/com/nikolasguillen/questlog/core/data/`
- `UI/` = `core/ui/src/main/java/com/nikolasguillen/questlog/core/ui/`
- `LISTS/` = `feature/lists/src/main/java/com/nikolasguillen/questlog/feature/lists/`
- `WISHLIST/` = `feature/wishlist/src/main/java/com/nikolasguillen/questlog/feature/wishlist/`
- `WISHLIST_TEST/` = `feature/wishlist/src/test/java/com/nikolasguillen/questlog/feature/wishlist/`

On Windows (PowerShell), use `.\gradlew.bat` instead of `./gradlew` in every command below.

---

## Phase 1: Setup

No setup tasks. `feature/wishlist/build.gradle.kts` already carries `junit`, `kotlinx-coroutines-test` and
`mockk`, and the feature adds no module and no dependency edge.

---

## Phase 2: Foundational (blocking prerequisites)

**Purpose**: The repository write, and the shared form that both features will call. There is no schema
change.

**⚠️ CRITICAL**: No user story can start until this phase is complete.

### Repository write (`:core:domain`, `:core:data`)

- [ ] T001 Create `DOMAIN/model/CoverImageUpdate.kt`: `sealed interface CoverImageUpdate` with `data object Keep`, `data object Remove` and `data class Replace(val sourceUri: String)`, all three in this one file (sealed-hierarchy exception). KDoc each case with its meaning from `data-model.md` § "New domain type". Contract: `contracts/data-and-domain.md`.
- [ ] T002 Add `suspend fun updateList(listId: Long, name: String, description: String, icon: WishlistIcon?, coverImage: CoverImageUpdate): AppResult<Unit>` to `DOMAIN/repository/GameRepository.kt`, directly after `createList`. Copy the KDoc from `contracts/data-and-domain.md`: the games, their statuses and the default flag are left alone; an unknown `listId` is a no-op; a `Failure` only means a `Replace` image failed to persist, the other fields are still saved and the previous cover is kept. Depends on T001.
- [ ] T003 [P] Create `DOMAIN/usecase/list/UpdateListUseCase.kt`: `class UpdateListUseCase @Inject constructor(private val repository: GameRepository)` with `suspend operator fun invoke(listId, name, description, icon, coverImage): AppResult<Unit>` delegating to `repository.updateList(...)`. Model it on `CreateListUseCase.kt` and repeat the failure semantics in its KDoc. Depends on T002.
- [ ] T004 [P] Implement `updateList` in `DATA/repository/GameRepositoryImpl.kt`, beside `createList`/`deleteList`, following the behaviour table in `contracts/data-and-domain.md`: (1) `listDao.getListById(listId) ?: return AppResult.success(Unit)`; (2) resolve the new path: `Keep` → old path, `Remove` → `null`, `Replace` → `coverImageStorage.persist(uri)` falling back to the old path when it returns `null`; (3) `listDao.updateList(existing.copy(name = name, description = description, icon = icon, coverImagePath = newPath))`; (4) **after** the row write, `coverImageStorage.delete(old)` only when there was an old path and it differs from `newPath`; (5) return `AppResult.failure(RepositoryError.FileStorage)` only when a `Replace` failed to persist, otherwise `AppResult.success(Unit)`. **Use `listDao.updateList` (`@Update`), never `insertList`**: its `REPLACE` would delete and re-insert the row, which the `RESTRICT` foreign key in `default_wishlist` refuses for the default list. Build results with the `AppResult` factory functions, never the constructors. Depends on T002.
- [ ] T005 Create `DATA_TEST/repository/GameRepositoryImplUpdateListTest.kt`, modelled on `GameRepositoryImplDeleteListTest.kt` in the same folder (same constructor wiring with relaxed mocks, `ListEntity` helper, `runTest`; KDoc header describing what it covers). One test per row of the behaviour table: unknown list is a no-op and returns success; `Keep` updates with the old path and makes no storage call; `Remove` with an old path nulls it and deletes the old file; `Remove` with no old path deletes nothing; `Replace` success stores the new path and deletes the old file; `Replace` with no old path deletes nothing; `Replace` whose `persist` returns `null` keeps the old path, deletes nothing and returns `RepositoryError.FileStorage`. Also assert with `coVerifyOrder` that `listDao.updateList` runs **before** `coverImageStorage.delete`, that the written entity keeps the existing `id`, and that `coVerify(exactly = 0) { listDao.insertList(any()) }` in every case. Depends on T004. Run `./gradlew :core:data:testDebugUnitTest --console=plain -q`.

### Shared form (`:core:ui`, `:feature:lists`)

- [ ] T006 [P] Create `UI/model/WishlistFormUiModel.kt`: `@Immutable data class WishlistFormUiModel(val name: String = "", val description: String = "", val icon: WishlistIcon? = null, val coverImage: String? = null)`. KDoc `coverImage` as "a stored file path when pre-filled, a picked `content://` URI after the user chooses an image, or `null`". `String` rather than `UiText`: user data, never a resource. Contract: `contracts/wishlist-form-sheet-ui.md`.
- [ ] T007 [P] Add six strings to `core/ui/src/main/res/values/strings.xml` with the same names and text as `feature/lists/src/main/res/values/strings.xml`: `list_name_label`, `description_optional_label`, `icon_optional_label`, `cover_image_optional_label`, `add_cover_image_content_description`, `remove_cover_image_action`. First grep `core/ui/src/main/res/values/strings.xml` for each name and stop to report if one already exists with different text.
- [ ] T008 Create `UI/component/WishlistFormSheet.kt` by moving `LISTS/components/CreateWishlistSheet.kt`, including its private `CoverImagePicker` and `IconOption` helpers, and the existing `72.dp` cover and `48.dp` icon-button sizes unchanged (component sizes, recorded in T026), but replace the `24.dp` icon size with `MaterialTheme.spacing.extraLarge`. For the cover preview, pass `File(path)` to `AsyncImage` when the string starts with `/` (a stored file path) and the raw string otherwise (a picked `content://` URI), the same way the detail header and overview rows render covers. The public signature is `fun WishlistFormSheet(title: String, confirmLabel: String, onDismiss: () -> Unit, onConfirm: (WishlistFormUiModel) -> Unit, initialValues: WishlistFormUiModel = WishlistFormUiModel())`. Seed each `rememberSaveable` from `initialValues` (`rememberSaveable { mutableStateOf(initialValues.name) }`, and likewise description, icon and cover). Use the `title` and `confirmLabel` parameters instead of `R.string.new_wishlist_sheet_title`/`create_action`. Keep Cancel as `CoreUiR.string.cancel`, but import this module's own `R` bare, so the `CoreUiR` alias and `CoreUiR.drawable.placeholder` become bare `R` references. Confirm stays `enabled = name.isNotBlank()` and calls `onConfirm(WishlistFormUiModel(name.trim(), description.trim(), selectedIcon, selectedCoverImage))`. End the file with a `private` `@QuestLogPreviews` preview in `QuestLogTheme { }` using pre-filled, edit-like values. Depends on T006, T007.
- [ ] T009 Switch `LISTS/ListsScreen.kt` to the shared sheet: replace the `CreateWishlistSheet(...)` call with `WishlistFormSheet(title = stringResource(R.string.new_wishlist_sheet_title), confirmLabel = stringResource(R.string.create_action), onDismiss = { showCreateSheet = false }, onConfirm = { form -> onEvent(ListsUiEvent.OnListCreated(name = form.name, description = form.description, icon = form.icon, coverImageUri = form.coverImage)); showCreateSheet = false })`, and swap the import. `ListsUiEvent`, `ListsViewModel` and its tests do not change. Depends on T008.
- [ ] T010 Delete `LISTS/components/CreateWishlistSheet.kt`, and remove from `feature/lists/src/main/res/values/strings.xml` the six strings that T007 moved. Before deleting each string, grep `feature/lists` for its remaining use; if one is still read elsewhere, keep it and report. `new_wishlist_sheet_title`, `create_action` and all other strings stay. This must land in the same commit as T007 so that no key is defined in two modules. Depends on T009.
- [ ] T011 Checkpoint: run `./gradlew :core:ui:compileDebugKotlin :feature:lists:testDebugUnitTest --console=plain -q`. It must compile and the existing `ListsViewModelTest`/`ListsUiMapperTest` must stay green. This is a behaviour-preserving refactor, so also run quickstart scenario 15 (create a list) on a device or emulator before building on top.

**Checkpoint**: The repository can edit a list, and the form is shared. User story work can begin.

---

## Phase 3: User Story 1 - Edit a wishlist's details (Priority: P1) 🎯 MVP

**Goal**: A pencil button on the detail screen opens the shared form pre-filled with the list's current
values, worded for editing. Saving updates the list and every screen that shows it, immediately.

**Independent Test**: Quickstart scenarios 1-4, 9-11, 13 and 14. Open a non-default list, tap the pencil,
change the name, tap Save, and see the new name in the header, the overview row and the list picker.

### Implementation for User Story 1

- [ ] T012 [P] [US1] In `WISHLIST/model/WishlistUiState.kt` add two fields after `showListOptions`: `val showEditAction: Boolean = false` and `val formValues: WishlistFormUiModel = WishlistFormUiModel()` (import it from `core.ui.model`). Keep `@Immutable` and the defaults-for-every-field rule. Contract: `contracts/wishlist-screen-ui.md`.
- [ ] T013 [P] [US1] In `WISHLIST/model/WishlistUiEvent.kt` add `data class OnListEdited(val values: WishlistFormUiModel) : WishlistUiEvent`.
- [ ] T014 [P] [US1] Add to `feature/wishlist/src/main/res/values/strings.xml`: `edit_list_action` = `Edit list` and `edit_wishlist_sheet_title` = `Edit Wishlist`.
- [ ] T015 [US1] In `WISHLIST/WishlistViewModel.kt`: (a) inject `private val updateListUseCase: UpdateListUseCase` after `setDefaultListUseCase`; (b) in the `.map { detail -> ... }` success branch set `showEditAction = true` and `formValues = WishlistFormUiModel(name = detail.list.name, description = detail.list.description, icon = detail.list.icon, coverImage = detail.list.coverImagePath)`, leaving `showListOptions = !detail.isDefault` untouched, so **the default list gets `showEditAction = true` too**; (c) add `is WishlistUiEvent.OnListEdited -> editList(event.values)` to the exhaustive `when`; (d) `private fun editList(values: WishlistFormUiModel)`: derive `CoverImageUpdate` by comparing `values.coverImage` with `uiState.value.formValues.coverImage` (equal → `Keep`, `null` → `Remove`, otherwise `Replace(values.coverImage)`), then in `viewModelScope.launch` call `updateListUseCase(listId, values.name, values.description, values.icon, coverImage).onFailure { error -> _uiEffect.send(WishlistUiEffect.ShowSnackbar(error.toUiText())) }`. A success sends no effect. Import `com.nikolasguillen.questlog.core.ui.mapper.toUiText`. Depends on T003, T012, T013.
- [ ] T016 [P] [US1] In `WISHLIST/components/WishlistTopBar.kt`: rename the `showActions` parameter to `showListOptions`; add `showEditAction: Boolean` and `onEditClick: () -> Unit`; as the **first** item in `actions`, render `if (showEditAction)` an `IconButton(onClick = onEditClick, colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.onSurface))` holding `Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit_list_action))`. Keep the existing Set-as-default and Delete buttons under `showListOptions`. Update both previews: the non-default one passes `showEditAction = true, showListOptions = true`; the default one passes `showEditAction = true, showListOptions = false` and so shows the pencil alone. Depends on T014.
- [ ] T017 [US1] In `WISHLIST/WishlistScreen.kt` `WishlistContent`: add `var showEditSheet by rememberSaveable { mutableStateOf(false) }` (import `androidx.compose.runtime.saveable.rememberSaveable`; **not** `remember`, research R8); pass `showEditAction = state.showEditAction`, `showListOptions = state.showListOptions` and `onEditClick = { showEditSheet = true }` to `WishlistTopBar`; and inside the `Scaffold` content, while `showEditSheet` is true, show `WishlistFormSheet(title = stringResource(R.string.edit_wishlist_sheet_title), confirmLabel = stringResource(CoreUiR.string.save_label), initialValues = state.formValues, onDismiss = { showEditSheet = false }, onConfirm = { values -> onEvent(WishlistUiEvent.OnListEdited(values)); showEditSheet = false })`. In `WishlistContentPreview` set `showEditAction = true`. Depends on T008, T012, T013, T014, T016.
- [ ] T018 [US1] Extend `WISHLIST_TEST/WishlistViewModelTest.kt`. Read the file first and add `private val updateListUseCase = mockk<UpdateListUseCase>(relaxed = true)` plus the new constructor argument in its `createViewModel` helper, so the existing tests compile again. Add cases, each for a loaded list: `formValues` carries the list's name, description, icon and cover path; `showEditAction` is `false` before the detail loads; `OnListEdited` with an unchanged `coverImage` calls the use case with `CoverImageUpdate.Keep`; with `coverImage = null` (list had one) → `Remove`; with a new `content://` URI → `Replace(thatUri)`; the use case receives the list's own id and the values' name, description and icon; a `Failure(RepositoryError.FileStorage)` sends `ShowSnackbar` carrying that error's `toUiText()`; a success sends no effect. Update the class KDoc to mention editing. Depends on T015.
- [ ] T019 [US1] Checkpoint: run `./gradlew :feature:wishlist:testDebugUnitTest :app:assembleDebug --console=plain -q`, then walk quickstart scenarios 1-4, 9-11, 13 and 14 on a device or emulator. Depends on T017, T018.

**Checkpoint**: US1 works end to end. Any list that shows the pencil can be edited.

---

## Phase 4: User Story 2 - Edit the default wishlist (Priority: P1)

**Goal**: The default list, and a user's only list, expose the pencil even though Set-as-default and Delete
are hidden for it. Editing never changes which list is the default.

**Independent Test**: Quickstart scenarios 5 and 6.

No new production code: T015 and T016 already keep `showEditAction` independent of `showListOptions`.
These tasks prove it and guard it against regressions.

- [ ] T020 [US2] Add to `WISHLIST_TEST/WishlistViewModelTest.kt`: for a `WishlistDetail` with `isDefault = true`, assert `showEditAction == true`, `showListOptions == false` and `isDefaultList == true`; for `isDefault = false`, assert `showEditAction == true` and `showListOptions == true`; and assert that after `OnListEdited` on the default list `setDefaultListUseCase` is never called (the edit does not touch the default flag). Depends on T018.
- [ ] T021 [US2] Run `./gradlew :feature:wishlist:testDebugUnitTest --console=plain -q`, then quickstart scenarios 5 and 6 on a device or emulator: only the pencil is in the default list's top bar, the "Default" badge stays after saving, and the game-detail heart still saves to that list. T005 already asserts that `insertList` is never called, which is what protects the default pointer. Depends on T020.

**Checkpoint**: US1 and US2 both verified.

---

## Phase 5: User Story 3 - Back out of an edit without changing anything (Priority: P2)

**Goal**: Cancelling or dismissing leaves the list untouched, and reopening shows the saved details, not the
abandoned changes.

**Independent Test**: Quickstart scenarios 7, 8 and 12.

No new production code: the behaviour comes from T008 (seeding once from `initialValues`) and T017 (the
sheet leaves composition on dismiss, which discards its saved state). These tasks verify it.

- [ ] T022 [P] [US3] Review `UI/component/WishlistFormSheet.kt` and confirm by reading it that every exit path reaches `onDismiss` without calling `onConfirm`: the Cancel `TextButton`, and the `CustomModalBottomSheet(onDismiss = ...)` used for swipe-down and Back. Confirm that nothing outside `rememberSaveable` holds field state. Fix it if either does not hold.
- [ ] T023 [US3] On a device or emulator run quickstart scenario 7 (type, Cancel, reopen: the original name is back), scenario 8 (clear the name: Save is disabled) and scenario 12 (type, rotate: the sheet and the text survive). Depends on T019.

**Checkpoint**: All three stories verified.

---

## Phase 6: Polish & Cross-Cutting Concerns

- [ ] T024 [P] Update `core/ui/CLAUDE.md`: add `WishlistFormSheet` to the component inventory beside `ListSelectorSheet` (create/edit form for a wishlist, title, confirm label and initial values supplied by the caller, working against `WishlistFormUiModel`), and add `WishlistFormUiModel` where the models are described, if that file lists them.
- [ ] T025 [P] Update `core/data/CLAUDE.md`: in the `AppResult` section extend the sentence "The exception is `createList`, which returns `AppResult<Unit>` where a `Failure` means only the cover image failed to persist" so it names `updateList` too.
- [ ] T026 [P] Grep `docs/`, `CLAUDE.md`, `feature/CLAUDE.md` and `feature/lists` for `CreateWishlistSheet` and update or delete every stale reference. Per the rule in the root `CLAUDE.md`, delete a rule whose subject is gone instead of rewriting it as a note. Then add one entry to `docs/tech-debt.md`, in that file's existing style: `WishlistFormSheet` keeps two component-size literals with no spacing token, a `72.dp` cover circle and a `48.dp` icon button, which the constitution's `dp` rule does not exempt; resolve it by adding size tokens to `:core:designsystem` or by agreeing an exemption for component sizes.
- [ ] T027 Run the full verification: `./gradlew test --console=plain -q` and `./gradlew :app:assembleDebug`. Every existing suite must stay green. Depends on T019, T021.
- [ ] T028 Run the whole manual table in `quickstart.md` (scenarios 1-15) and the optional `adb shell run-as com.nikolasguillen.questlog ls files/wishlist_covers` check after scenarios 9 and 10. Depends on T027.

---

## Dependencies & Execution Order

### Phase dependencies

- **Setup (Phase 1)**: empty.
- **Foundational (Phase 2)**: blocks every story. Inside it there are two independent tracks:
  - *Repository*: T001 → T002 → (T003, T004) → T005
  - *Shared form*: (T006, T007) → T008 → T009 → T010 → T011
- **US1 (Phase 3)**: needs T003 (use case) and T008 (sheet). Both tracks finish before T015/T017.
- **US2 (Phase 4)** and **US3 (Phase 5)**: need US1's code. They can run in either order.
- **Polish (Phase 6)**: after the stories it documents.

### Within US1

- T012, T013, T014 are independent files and can run in parallel.
- T015 needs T012, T013 and T003. T016 needs T014 only. T017 needs T008, T012, T013, T014, T016.
- T018 needs T015. T019 needs T017 and T018.

### Same-file ordering

`WishlistViewModelTest.kt` is touched by T018 and then T020, so they are sequential. `WishlistTopBar.kt` is
touched only by T016, which also updates both previews.

### Commit grouping (the `type(scope): subject` hook applies)

1. T001-T005: `feat(data): add updateList to the repository`
2. T006-T011: `refactor(ui): move the wishlist sheet to core:ui` (T007 and T010 **must** be in this commit)
3. T012-T019: `feat(wishlist): edit a wishlist from its detail screen`
4. T020-T023: `test(wishlist): cover editing the default list`
5. T024-T026: `docs: document the shared wishlist form`

---

## Parallel Example

```bash
# After T002 (the interface method exists), in parallel:
T003  DOMAIN/usecase/list/UpdateListUseCase.kt
T004  DATA/repository/GameRepositoryImpl.kt

# Alongside the repository track, at any time in Phase 2:
T006  UI/model/WishlistFormUiModel.kt
T007  core/ui/src/main/res/values/strings.xml

# Start of US1, in parallel:
T012  WISHLIST/model/WishlistUiState.kt
T013  WISHLIST/model/WishlistUiEvent.kt
T014  feature/wishlist/src/main/res/values/strings.xml
```

---

## Implementation Strategy

### MVP first (US1 only)

1. Finish Phase 2. T011 proves the extraction did not break creation.
2. Finish Phase 3 and stop at T019. This already delivers the whole user-visible feature, default list
   included, because the pencil is gated independently of the other actions.
3. Do not ship without Phase 4's T020: it is the test that stops a later change from hiding the pencil on
   the default list.

### Incremental delivery

1. Phase 2 → the form is shared and the repository can edit. Nothing user-visible changes.
2. Phase 3 → editing works.
3. Phases 4 and 5 → the default-list and back-out guarantees are tested and checked.
4. Phase 6 → docs and the full run.

---

## Notes

- **Do not use `insertList` for edits.** `REPLACE` deletes and re-inserts the row, and the `RESTRICT`
  foreign key in `default_wishlist` refuses that for the default list.
- **Delete the old cover file only after the row is written**, the same ordering as `deleteList`.
- `WishlistUiState.listName` stays a `UiText.DynamicString`. That is an existing deviation noted in
  `plan.md`, so leave it alone. Pre-fill from `detail.list.name`.
- The create sheet closing on rotation (`showCreateSheet` in `remember`) is observed and out of scope.
- No `SavedStateHandle`, no `dp` literals beyond the three that move with the sheet, and no hardcoded UI
  text.
