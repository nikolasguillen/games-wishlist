# Implementation Plan: Edit Wishlist

**Branch**: `006-edit-wishlist` | **Date**: 2026-10-06 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/006-edit-wishlist/spec.md`

## Summary

A pencil button on the list detail screen opens the existing list-creation form. The form is pre-filled
with the list's current name, description, icon and cover, and its title and confirm button are worded for
editing. Saving writes the list in place, and the detail screen, overview and list picker update on their
own through the Room `Flow`s they already observe. Every list, the default included, gets the button.

**The form moves to `:core:ui`.** `CreateWishlistSheet` lives in `:feature:lists`, and the detail screen is
in `:feature:wishlist`. A feature may not depend on a feature, so the sheet becomes the public
`WishlistFormSheet` in `:core:ui`, which both features already depend on. It takes its title, confirm
label and initial values from the caller, and has no create/edit mode. **No new dependency edge.**

**Persistence.** The existing, unused `ListDao.updateList` (`@Update`) writes the row. There is no schema
change and no new table or column. `@Update` cannot touch the games, their statuses or the default
pointer, which gives FR-008 and FR-011 by construction.

**Cover image.** A new domain `CoverImageUpdate` (`Keep` / `Remove` / `Replace(uri)`) tells the
repository what to do with the file. `WishlistViewModel` derives it from the form's result.

- **On success**: the new cover is persisted, the row is written, and only then is the old file deleted
  (the same ordering as `deleteList`).
- **On a failed replace**: every other field is still saved, the old cover is kept, and the screen shows
  the existing `FileStorage` snackbar. That mirrors `createList`'s contract.

## Technical Context

**Language/Version**: Kotlin 2.4.10, JVM toolchain 21, Java 11 target

**Primary Dependencies**: Jetpack Compose (BOM 2026.09.00, Material 3), Hilt 2.60.1, Room 2.8.5 (KSP),
Coil 2, `activity-compose` (Photo Picker), kotlinx-coroutines / Flow

**Storage**: Room `QuestLogDatabase` `version = 1`. No schema change and no exported-schema regeneration.
Cover files live in `filesDir/wishlist_covers/` via `WishlistCoverImageStorage`.

**Testing**: JUnit4 + MockK + `kotlinx-coroutines-test` (`StandardTestDispatcher`,
`Dispatchers.setMain`/`resetMain`). Repository tests mock the DAO and the storage. ViewModel tests mock
the use cases.

**Target Platform**: Android, minSdk 29 / targetSdk 37

**Project Type**: Modular mobile app (17 Gradle modules)

**Performance Goals**: No new targets. One primary-key read plus one primary-key update, plus one image
downscale when the cover is replaced (already bounded at 1440 px by `WishlistCoverImageStorage`).

**Constraints**:

- No new module dependency edges.
- No database `version` bump and no entity change.
- Must not use `insertList` (`REPLACE`) for edits, because the `RESTRICT` FK in `default_wishlist` refuses
  to delete-and-reinsert the default list.
- Feature modules must not see `:core:data` or `:core:database`.

**Scale/Scope**: A local, single-user store with typically fewer than 20 wishlists. About 20 files are
touched across `:core:domain`, `:core:data`, `:core:ui`, `:feature:wishlist`, `:feature:lists` and two
`CLAUDE.md` files.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle / constraint | Assessment | Result |
|---|---|---|
| **I. Module boundaries** | The shared form goes to `:core:ui`, which `:feature:lists` and `:feature:wishlist` already depend on. `:core:ui` already has `:core:model` (for `WishlistIcon`), Coil and `activity-compose`. `CoverImageUpdate` is in `:core:domain`, which the repository interface and `:feature:wishlist` already see. No feature→feature edge and no feature→data edge. `:core:model` is untouched. | ✅ Pass |
| **II. Typed errors** | `updateList` returns `AppResult<Unit>` for the same documented reason as `createList` (only the cover can fail). `core/data/CLAUDE.md`'s exception sentence is extended to name it. `FileStorage` reaches the UI through `RepositoryError.toUiText()`. No exception crosses `:core:data`. | ✅ Pass |
| **III. UI renders, doesn't decide** | `showEditAction`, `formValues` and the `Keep`/`Remove`/`Replace` derivation are in `WishlistViewModel`. The top bar and sheet only read them and emit events. There is one new `UiEvent` and the `when` stays exhaustive. New text is in `strings.xml`. Name, description and cover stay `String`, because they are user data (the `UiText` rule's own test). | ✅ Pass |
| **IV. Reuse the shared layer** | One form for both flows (FR-003), built from `CustomModalBottomSheet`. `save_label`, `cancel` and `error_file_storage` are reused. There is no new dialog. The sheet's `24.dp` becomes `MaterialTheme.spacing.extraLarge`. Its `72.dp` and `48.dp` are component sizes, not spacing, so they stay literals and are recorded in `docs/tech-debt.md`, see the note below. Still a single `GameRepository`. | ✅ Pass, with a recorded deviation |
| **V. Local verification** | The real commands are in [quickstart.md](./quickstart.md). There is a new repository test in `:core:data`, and `WishlistViewModelTest` is extended. The pass-through use case and the composable get no test (research R10). | ✅ Pass |
| **Persistence** | `version` stays 1. No entity change, so no schema regeneration. No list-shaped column. | ✅ Pass |
| **Injection** | `WishlistViewModel` keeps `@AssistedInject` and gains `UpdateListUseCase`. There is no `SavedStateHandle`. | ✅ Pass |
| **KMP** | The moved sheet was already Android-only (Photo Picker, `ActivityResultContracts`), so moving it changes no KMP cost. `CoverImageUpdate` is plain Kotlin. Nothing here is expensive to reverse. | ✅ Pass |

**Post-design re-check (after Phase 1)**: Still all pass. The contracts add no dependency edge, keep every
decision in the ViewModel or repository, and introduce no `SavedStateHandle`.

**Observed, not acted on**:

- **Existing `dp` literals**: `CreateWishlistSheet` already hardcodes `72.dp`, `48.dp` and `24.dp` for the
  cover and icon circles. The `24.dp` has a token (`spacing.extraLarge`) and is switched to it on the move.
  The `72.dp` and `48.dp` are component sizes with no spacing equivalent, so they move verbatim and get an
  entry in `docs/tech-debt.md` (T026), because the constitution's "never inlined" rule has no exemption.
- **`ListsContent`'s create sheet closes on rotation**: it keeps `showCreateSheet` in `remember`. The edit
  sheet uses `rememberSaveable` (research R8). Aligning creation is a one-word change outside this
  feature, so it is the owner's call.
- **`WishlistUiState.listName` is a `UiText.DynamicString`**: that wraps user data, which the `UiText`
  rule says should be a plain `String`. It is not listed in `docs/tech-debt.md`. This feature leaves it
  alone and reads the name for the pre-fill from `WishlistList.name` directly.
- **Constitution IV says "dark-theme only"**, which contradicts spec 004 and `feature/CLAUDE.md`. This was
  already flagged in spec 005's plan.

## Project Structure

### Documentation (this feature)

```text
specs/006-edit-wishlist/
├── plan.md              # This file
├── research.md          # Phase 0: decisions R1–R10
├── data-model.md        # Phase 1: editable fields, CoverImageUpdate, WishlistFormUiModel, UI state
├── quickstart.md        # Phase 1: build/test commands + 15 manual scenarios
├── contracts/
│   ├── data-and-domain.md         # CoverImageUpdate, GameRepository.updateList, UpdateListUseCase
│   ├── wishlist-form-sheet-ui.md  # the shared WishlistFormSheet, string moves, the lists caller
│   └── wishlist-screen-ui.md      # top bar, UiState, UiEvent and wiring on the detail screen
├── checklists/
│   └── requirements.md
└── tasks.md             # Phase 2 (/speckit-tasks; not created here)
```

### Source Code (repository root)

```text
core/domain/src/main/java/com/nikolasguillen/questlog/core/domain/
├── model/CoverImageUpdate.kt                    # NEW: Keep / Remove / Replace(sourceUri)
├── repository/GameRepository.kt                 # + updateList(...): AppResult<Unit>
└── usecase/list/UpdateListUseCase.kt            # NEW: delegation

core/data/
├── src/main/java/com/nikolasguillen/questlog/core/data/repository/GameRepositoryImpl.kt
│                                                # + updateList: read → resolve cover → @Update → delete old file
├── src/test/java/com/nikolasguillen/questlog/core/data/repository/GameRepositoryImplUpdateListTest.kt
│                                                # NEW: every row of the behaviour table in the data contract
└── CLAUDE.md                                    # AppResult exception names updateList alongside createList

core/ui/
├── src/main/java/com/nikolasguillen/questlog/core/ui/
│   ├── component/WishlistFormSheet.kt           # NEW (moved from feature/lists' CreateWishlistSheet):
│   │                                            #   title/confirmLabel/initialValues params + preview
│   └── model/WishlistFormUiModel.kt             # NEW
├── src/main/res/values/strings.xml              # + 6 form-label strings moved from feature/lists
└── CLAUDE.md                                    # component inventory: + WishlistFormSheet

feature/lists/src/main/
├── java/com/nikolasguillen/questlog/feature/lists/
│   ├── components/CreateWishlistSheet.kt        # DELETED
│   └── ListsScreen.kt                           # call WishlistFormSheet; map WishlistFormUiModel → OnListCreated
└── res/values/strings.xml                       # − the 6 moved strings (title + create_action stay)

feature/wishlist/
├── src/main/java/com/nikolasguillen/questlog/feature/wishlist/
│   ├── WishlistViewModel.kt                     # + UpdateListUseCase; showEditAction/formValues;
│   │                                            #   OnListEdited → CoverImageUpdate → use case → snackbar on failure
│   ├── WishlistScreen.kt                        # rememberSaveable showEditSheet; WishlistFormSheet; previews
│   ├── components/WishlistTopBar.kt             # + Edit action (always once loaded); showActions → showListOptions
│   └── model/
│       ├── WishlistUiState.kt                   # + showEditAction, formValues
│       └── WishlistUiEvent.kt                   # + OnListEdited(values)
├── src/main/res/values/strings.xml              # + edit_list_action, edit_wishlist_sheet_title
└── src/test/java/com/nikolasguillen/questlog/feature/wishlist/WishlistViewModelTest.kt
                                                 # + pre-fill, showEditAction, cover derivation, failure snackbar
```

**Structure Decision**: The existing modular layout is kept, following the established ViewModel →
UseCase → `GameRepository` → DAO chain. There are no new modules and no new dependency edges. Three
independently compilable layers give the natural task order:

1. `:core:domain` + `:core:data` (with its test)
2. `:core:ui` sheet extraction + `:feature:lists` switch-over (behaviour-preserving)
3. `:feature:wishlist` edit flow (with its test)

## Implementation Notes for `/speckit-tasks`

- **Do the extraction as one atomic step.** Create `WishlistFormSheet` and the moved strings in `:core:ui`.
  Switch `ListsContent` to it. Delete `CreateWishlistSheet.kt` and the moved strings from `feature/lists`.
  All of this goes in the same commit. It is a pure refactor and needs to be verified by quickstart
  scenario 15 before the edit flow is built on top.
- **Story mapping.**
  - US1 (edit a list): the foundation (domain + data), the extraction, and the detail-screen flow.
  - US2 (the default list is editable): no separate code. It comes from `showEditAction` being independent
    of `showListOptions`. It gets its own ViewModel test case and quickstart scenarios 5 and 6.
  - US3 (back out): `rememberSaveable` seeding plus dismiss removing the sheet. It has no ViewModel logic
    and is verified by quickstart scenario 7.
- **`WishlistTopBar` renames `showActions` → `showListOptions`**, to match the state field it already
  reads. Its two previews change with it.
- **Out of scope** (spec Assumptions): editing from the overview list, a success snackbar, and aligning
  the create sheet's rotation behaviour.

## Complexity Tracking

No constitution violations to justify.
