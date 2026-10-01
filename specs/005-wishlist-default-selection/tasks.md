---

description: "Task list for Default Wishlist Selection"
---

# Tasks: Default Wishlist Selection

**Input**: Design documents from `/specs/005-wishlist-default-selection/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

**Tests**: Included. The spec does not ask for them, but the constitution (Principle V) requires new
ViewModel and use-case logic to get a test in its own module. There are no DAO tests: that is a known,
listed gap, and the foreign-key and seed behavior is covered by the manual checks in `quickstart.md`.

**Organization**: Tasks are grouped by user story. US2 comes before US1 because it only needs the
foundational layer (no UI work), and US1 and US3 both land in `:feature:wishlist` / `DeleteListUseCase`.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependency on an incomplete task)
- **[Story]**: User story the task belongs to (US1, US2, US3)
- Every task names exact file paths.

## Path conventions

All paths are relative to the repository root. Sources live under `src/main/java/` (tests under
`src/test/java/`) in the package `com/nikolasguillen/questlog/...`. Abbreviations used below:

- `DB/` = `core/database/src/main/java/com/nikolasguillen/questlog/core/database/`
- `DOMAIN/` = `core/domain/src/main/java/com/nikolasguillen/questlog/core/domain/`
- `DOMAIN_TEST/` = `core/domain/src/test/java/com/nikolasguillen/questlog/core/domain/`
- `DATA/` = `core/data/src/main/java/com/nikolasguillen/questlog/core/data/`
- `WISHLIST/` = `feature/wishlist/src/main/java/com/nikolasguillen/questlog/feature/wishlist/`
- `WISHLIST_TEST/` = `feature/wishlist/src/test/java/com/nikolasguillen/questlog/feature/wishlist/`

On Windows (PowerShell), use `.\gradlew.bat` instead of `./gradlew` in every command below.

---

## Phase 1: Setup

**Purpose**: Give `:feature:wishlist` a test source set. It has none today (`docs/tech-debt.md`).

- [ ] T001 [P] In `feature/wishlist/build.gradle.kts`, add the three test dependencies, copied from `feature/lists/build.gradle.kts` lines 48-50: `testImplementation(libs.junit)`, `testImplementation(libs.kotlinx.coroutines.test)`, `testImplementation(libs.mockk)`. Add nothing else.

---

## Phase 2: Foundational (blocking prerequisites)

**Purpose**: The persisted default pointer and the repository seam that all three stories build on. This
phase only *adds* code, so the project keeps compiling throughout. Nothing existing is migrated yet.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [ ] T002 Create `DB/entity/DefaultWishlistEntity.kt`: `@Entity(tableName = "default_wishlist", foreignKeys = [ForeignKey(entity = ListEntity::class, parentColumns = ["id"], childColumns = ["listId"], onDelete = ForeignKey.RESTRICT)], indices = [Index("listId")])` on a `data class` with two properties. `@PrimaryKey val id: Long = 0` ("INTEGER Primary key, not auto-generated. Always `0`, which makes this a single-row table.") and `val listId: Long` ("INTEGER NOT NULL. Foreign key → `wishlists.id`, `onDelete = RESTRICT`. Indexed."). Add a short KDoc saying why: one row means exactly one default, and RESTRICT makes the database itself refuse to delete the default list. Import `ListEntity` from the same package. Follow the `<Name>Entity` convention from `core/database/CLAUDE.md`.
- [ ] T003 Register `DefaultWishlistEntity::class` in the `entities = [...]` array of `DB/QuestLogDatabase.kt` (add the import too). Keep `version = 1`. Do NOT bump it and do NOT add a `Migration`.
- [ ] T004 [P] In `DB/dao/ListDao.kt`, add three methods exactly as in `contracts/data-and-domain.md`. First, `@Query("SELECT listId FROM default_wishlist WHERE id = 0") fun observeDefaultListId(): Flow<Long>`. Second, `@Query("SELECT listId FROM default_wishlist WHERE id = 0") suspend fun getDefaultListId(): Long`. Third, `@Query("UPDATE default_wishlist SET listId = :listId WHERE id = 0") suspend fun setDefaultList(listId: Long)`. Do not add an insert or a delete for this table: the seed is its only writer, which is what keeps "exactly one row" true.
- [ ] T005 [P] In `DB/dao/GameDao.kt`, add `@Query("SELECT gameId FROM game_list_cross_ref WHERE listId = (SELECT listId FROM default_wishlist)") fun observeGameIdsInDefaultList(): Flow<List<Int>>`. The subquery makes Room's invalidation tracker watch `default_wishlist` too, so every derived Flow re-emits when the default moves (research R1). Leave `getWishlistedGames()` and `getGameIdsInList()` alone for now; US2 handles them.
- [ ] T006 [P] In `DB/di/DatabaseModule.kt`, rewrite the `onCreate` seed so it no longer uses an explicit id. First, insert the starter list as `INSERT INTO wishlists (name, description, icon) VALUES (...)`, keeping the existing name/description/icon values. Then, in the next statement, run `INSERT INTO default_wishlist (id, listId) VALUES (0, last_insert_rowid())`. Remove the `WishlistConstants` import from this file. This satisfies FR-010.
- [ ] T007 [P] In `DOMAIN/repository/GameRepository.kt`, add `fun observeDefaultListId(): Flow<Long>`, `suspend fun getDefaultListId(): Long` and `suspend fun setDefaultList(listId: Long)`, next to the other list methods (`getAllLists`, `observeListById`, `deleteList`). Use the KDoc from `contracts/data-and-domain.md`. They are DB-only, so there is no `AppResult` (constitution II).
- [ ] T008 In `DATA/repository/GameRepositoryImpl.kt`, implement the three new methods as one-line delegations: `observeDefaultListId()` → `listDao.observeDefaultListId()`, `getDefaultListId()` → `listDao.getDefaultListId()`, `setDefaultList(listId)` → `listDao.setDefaultList(listId)`. Depends on T004 and T007. Do not touch the existing `WishlistConstants` call sites yet.
- [ ] T009 Verify the foundation. Run `./gradlew :core:database:compileDebugKotlin :core:data:compileDebugKotlin --console=plain -q`. Then confirm `core/database/schemas/com.nikolasguillen.questlog.core.database.QuestLogDatabase/1.json` now contains a `default_wishlist` table with a foreign key to `wishlists` and nothing else new, and that `version` is still 1. Keep the regenerated schema in the change set. Never edit anything under `build/generated/**`.

**Checkpoint**: The pointer table, its DAO and the repository methods exist. Nothing reads them yet.

---

## Phase 3: User Story 2 - Quick-save always targets the chosen default (Priority: P1) 🎯 MVP

**Goal**: The heart button (and every other `isWishlisted` derivation) uses the stored default instead of the hardcoded id. `GameDetailActionPill` itself is unchanged: its heart already goes through `ToggleWishlistUseCase` → `GameRepository.toggleWishlist` (research R8).

**Independent Test**: `GameRepositoryImplToggleWishlistTest` stubs a default id other than 1 and proves the cross-ref is written, and removed, against that id. End to end, `quickstart.md` scenarios 5-7 and 12 prove it once US1 lets you move the default.

### Tests for User Story 2

> Write these first and confirm they fail against the current constant-based code.

- [ ] T010 [P] [US2] In `core/data/src/test/java/com/nikolasguillen/questlog/core/data/repository/GameRepositoryImplToggleWishlistTest.kt`, hoist the inline `listDao = mockk<ListDao>(relaxed = true)` into a field. In each test, stub `coEvery { listDao.getDefaultListId() } returns 7L` (any id that is not 1). Replace the `any()` matchers on `gameDao.isGameInList(...)` with `isGameInList(1, 7L)`, and assert `gameDao.insertGameListCrossRef(GameListCrossRef(1, 7L))` / `gameDao.deleteGameListCrossRef(GameListCrossRef(1, 7L))`. Keep the three existing cases and their names. Today `any()` would hide a regression back to a fixed id.

### Implementation for User Story 2

- [ ] T011 [P] [US2] In `DB/dao/GameDao.kt`, change `getWishlistedGames()` so its `WHERE` clause is `game_list_cross_ref.listId = (SELECT listId FROM default_wishlist)` instead of interpolating `${WishlistConstants.DEFAULT_WISHLIST_ID}`. Remove the now-unused `WishlistConstants` import. Depends on T003.
- [ ] T012 [US2] In `DATA/repository/GameRepositoryImpl.kt`, change `toggleWishlist` and `refreshGameDetail` to call `listDao.getDefaultListId()` once at the top and use that value in place of `WishlistConstants.DEFAULT_WISHLIST_ID` for `gameDao.isGameInList(...)` and for both `GameListCrossRef(...)` constructions. Keep the existing "don't overwrite a stored row" logic and its comment untouched. Depends on T008 and T010.
- [ ] T013 [US2] In the same file, `DATA/repository/GameRepositoryImpl.kt`, replace the remaining `gameDao.getGameIdsInList(WishlistConstants.DEFAULT_WISHLIST_ID)` calls in `observeGameDetail`, `getRecentlyViewedGames`, `getWishlistedGameIds`, `getSavedGames` and `getGamesByList` with `gameDao.observeGameIdsInDefaultList()`. Then remove the unused `WishlistConstants` import. Sequential after T012: same file.
- [ ] T014 [US2] In `DB/dao/GameDao.kt`, delete `getGameIdsInList(listId: Long)`. First run `grep -rn getGameIdsInList --include=*.kt .` and confirm no caller remains outside `build/`. Depends on T013.
- [ ] T015 [P] [US2] In `feature/game-detail/src/main/java/com/nikolasguillen/questlog/feature/gamedetail/GameDetailViewModel.kt`, update the stale comment in `confirmListSelection()` that says toggling `WishlistConstants.DEFAULT_WISHLIST_ID` flows back through `currentGameFlow`. Reword it so it says the same thing about "the default list" with no reference to the constant. Comment only, no behavior change.
- [ ] T016 [US2] Run `./gradlew :core:database:compileDebugKotlin :core:data:testDebugUnitTest :feature:game-detail:testDebugUnitTest :feature:search:testDebugUnitTest --console=plain -q`. All suites must pass, including the updated T010 cases. Then do a fresh-install smoke check. Uninstall the app and run `./gradlew :app:installDebug`. Open any game's detail screen and tap the heart (`quickstart.md` scenarios 5 and 6, minus the badge and menu parts, which don't exist until US1). It must not crash, and the game must appear in the seeded list. This is the first task that reads the default pointer, so a bad seed from T006 shows up here rather than at T038.

**Checkpoint**: Heart, search-grid saves and every `isWishlisted` read follow the stored default. Nothing in the UI can move it yet.

---

## Phase 4: User Story 1 - Choose a default wishlist (Priority: P1)

**Goal**: A non-default list's "⋮" menu offers **Set as default**, which applies immediately with a snackbar. The default list shows a "Default" badge in its top bar and no menu.

**Independent Test**: Open a non-default wishlist, choose **Set as default**, and confirm the snackbar appears and the screen swaps its menu for the badge. The previous default's screen now shows the menu and no badge (`quickstart.md` scenarios 1-4).

> **Compile note**: T018 breaks `:core:domain` until T021. T022 replaces `canDeleteList` in `WishlistUiState`, which breaks `WishlistViewModel`, `WishlistTopBar` and `WishlistScreen` until T025-T027. Don't stop between them.

### Implementation for User Story 1

> Tasks T020 and T024 are tests: write each one before the implementation task that follows it, and expect it to fail until then.

- [ ] T017 [P] [US1] In `feature/wishlist/src/main/res/values/strings.xml`, add `set_as_default_action` = "Set as default", `default_list_label` = "Default" and `default_list_set_message` = "This is now your default list".
- [ ] T018 [P] [US1] In `DOMAIN/model/WishlistDetail.kt`, add `val isDefault: Boolean` as a third constructor property, with a `@property isDefault` KDoc line ("`true` when [list] is the wishlist currently set as the default"). It has no default value, so every construction site has to say it. `GetWishlistDetailUseCase.kt:22` is the only place that constructs `WishlistDetail`, so `:core:domain` will not compile between T018 and T021. That is expected, so do not patch the call site here.
- [ ] T019 [P] [US1] Create `DOMAIN/usecase/list/SetDefaultListUseCase.kt`: `class SetDefaultListUseCase @Inject constructor(private val repository: GameRepository)` with `suspend operator fun invoke(listId: Long) { repository.setDefaultList(listId) }` and a short KDoc. There is no test for it: it is a pass-through. Depends on T007.
- [ ] T020 [US1] Create `DOMAIN_TEST/usecase/list/GetWishlistDetailUseCaseTest.kt` with a KDoc header, JUnit4 + MockK, mocking `GameRepository`. Cover three cases. First, `isDefault` is `true` when `observeListById` emits a list whose id equals `observeDefaultListId()`, and `false` when it does not. Second, `isDefault` flips when `observeDefaultListId()` emits a new id for the same list. Third, the flow still emits `null` when `observeListById` emits `null`. Depends on T018. It cannot fail at runtime: `:core:domain` already fails to compile after T018, so it passes only once T021 lands.
- [ ] T021 [US1] In `DOMAIN/usecase/list/GetWishlistDetailUseCase.kt`, combine a third flow, `repository.observeDefaultListId()`, with the existing two, and fill `WishlistDetail.isDefault = list.id == defaultListId`. Keep emitting `null` when the list is gone, and update the class KDoc. Depends on T018 and T020.
- [ ] T022 [P] [US1] In `WISHLIST/model/WishlistUiState.kt`, remove `canDeleteList` and add `val isDefaultList: Boolean = false` and `val showListOptions: Boolean = false`. Keep `listName` and `contentState` unchanged, and keep the type `@Immutable internal data class`.
- [ ] T023 [P] [US1] In `WISHLIST/model/WishlistUiEvent.kt`, add `data object OnSetAsDefault : WishlistUiEvent` to the sealed interface.
- [ ] T024 [US1] Create `WISHLIST_TEST/WishlistViewModelTest.kt` with a KDoc header. Use JUnit4 + MockK + `kotlinx-coroutines-test` with `StandardTestDispatcher`, `Dispatchers.setMain` in `@Before` / `resetMain` in `@After`, and `advanceUntilIdle()` after each event. Mock the use cases (`GetWishlistDetailUseCase`, `DeleteListUseCase`, `RemoveGameFromListUseCase`, `SetDefaultListUseCase`), never the repository. Construct the ViewModel directly with an explicit `listId`. Cover four cases. First, initial state has `isDefaultList == false` and `showListOptions == false`. Second, a loaded detail with `isDefault = true` gives `isDefaultList == true` and `showListOptions == false`. Third, a loaded detail with `isDefault = false` gives the opposite. Fourth, `OnSetAsDefault` calls `setDefaultListUseCase(listId)` once and emits `WishlistUiEffect.ShowSnackbar(UiText.StringResource(R.string.default_list_set_message))`. Depends on T001, T017, T018, T019, T022, T023. It fails until T025.
- [ ] T025 [US1] In `WISHLIST/WishlistViewModel.kt`, add `private val setDefaultListUseCase: SetDefaultListUseCase` to the `@AssistedInject` constructor. Remove the `canDeleteList` property and the `WishlistConstants` import. When mapping a loaded detail, set `isDefaultList = detail.isDefault` and `showListOptions = !detail.isDefault`. Add the `OnSetAsDefault` branch to the exhaustive `when` in `onEvent`, calling a new private `setAsDefault()`. That function does `viewModelScope.launch { setDefaultListUseCase(listId); _uiEffect.send(WishlistUiEffect.ShowSnackbar(UiText.StringResource(R.string.default_list_set_message))) }`. Leave the delete and remove-game paths alone. Depends on T017, T018, T019, T022, T023.
- [ ] T026 [US1] In `WISHLIST/components/WishlistTopBar.kt`, replace the `canDeleteList` parameter with `isDefaultList: Boolean`, `showListOptions: Boolean` and `onSetAsDefaultClick: () -> Unit`, keeping `modifier` as the trailing parameter. Render the `title` slot as a `Row` holding the title `Text` plus, only when `isDefaultList`, `CustomSummaryBadge(text = stringResource(R.string.default_list_label))` from `:core:ui`, separated by a `MaterialTheme.spacing` token (no `dp` literal). In `actions`, show `ListOptionsMenu` only when `showListOptions`. Give it a `DropdownMenuItem` "Set as default" (`R.string.set_as_default_action`) above the existing "Delete list" item. The "Set as default" item collapses the menu and calls `onSetAsDefaultClick`, with no dialog. Update `WishlistTopBarPreview` for the new parameters and add a second `@QuestLogPreviews` preview for the default state (`isDefaultList = true, showListOptions = false`), both `private` and wrapped in `QuestLogTheme(darkTheme = isSystemInDarkTheme())`. Depends on T017 and T022.
- [ ] T027 [US1] In `WISHLIST/WishlistScreen.kt`, pass `isDefaultList = state.isDefaultList`, `showListOptions = state.showListOptions` and `onSetAsDefaultClick = { onEvent(WishlistUiEvent.OnSetAsDefault) }` to `WishlistTopBar`. Fix any preview that still constructs `WishlistUiState(canDeleteList = ...)`. Depends on T023, T026.
- [ ] T028 [US1] Run `./gradlew :core:domain:testDebugUnitTest :feature:wishlist:testDebugUnitTest :feature:wishlist:compileDebugKotlin --console=plain -q`. T020 and T024 must pass. Both the badge and the menu previews should render in Android Studio.

**Checkpoint**: A user can pick a default, see it, and have the heart follow it (US1 + US2 together are the MVP).

---

## Phase 5: User Story 3 - The default wishlist can never be deleted (Priority: P2)

**Goal**: `DeleteListUseCase` protects whichever list is the *current* default, not the hardcoded id 1. The RESTRICT foreign key from T002 is the database backstop.

**Independent Test**: With the repository returning default id 7, `DeleteListUseCase(7)` returns `false` and deletes nothing, while `DeleteListUseCase(1)` deletes. Manually, `quickstart.md` scenarios 8-10.

> **Ship US3 with US1.** Until T030 lands, the old id-1 check refuses to delete list A after the user moves the default to B, which breaks US3 acceptance scenario 4 and the delete flow in scenario 8.

### Tests for User Story 3

- [ ] T029 [P] [US3] Create `DOMAIN_TEST/usecase/list/DeleteListUseCaseTest.kt` with a KDoc header, JUnit4 + MockK, mocking `GameRepository`. Cover three cases. First, with `getDefaultListId()` returning 7, `invoke(7)` returns `false` and `repository.deleteList` is never called. Second, `invoke(3)` returns `true` and calls `deleteList(3)` once. Third, with `getDefaultListId()` returning 3, `invoke(1)` returns `true`, proving the old built-in id is no longer special. Fails until T030.

### Implementation for User Story 3

- [ ] T030 [US3] In `DOMAIN/usecase/list/DeleteListUseCase.kt`, replace `if (listId == WishlistConstants.DEFAULT_WISHLIST_ID) return false` with `if (listId == repository.getDefaultListId()) return false`. Remove the `WishlistConstants` import. Rewrite the KDoc so it says the *current default* wishlist is never deleted: the heart button and every `isWishlisted` read resolve to it, and the database also refuses the delete. Depends on T007 and T029.
- [ ] T031 [US3] In `WISHLIST_TEST/WishlistViewModelTest.kt` (created in T024), add two cases. First, `OnWishlistDeleted` with `deleteListUseCase` returning `false` emits `ShowSnackbar(UiText.StringResource(R.string.unable_to_delete_wishlist))` and does not emit `NavigateBack`. Second, with it returning `true` it emits `NavigateBack`. Depends on T024 and T025.
- [ ] T032 [US3] Run `./gradlew :core:domain:testDebugUnitTest :feature:wishlist:testDebugUnitTest --console=plain -q`. T029 and T031 must pass.

**Checkpoint**: All three stories work. The default list is protected wherever deletion can be triggered.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Remove the old constant, bring the docs in line, and verify end to end.

- [ ] T033 Delete `core/model/src/main/java/com/nikolasguillen/questlog/core/model/WishlistConstants.kt`. In `core/model/src/main/java/com/nikolasguillen/questlog/core/model/WishlistList.kt`, reword the class KDoc, which today says the default wishlist's id is "fixed by [WishlistConstants.DEFAULT_WISHLIST_ID]", so it no longer names the constant. Before deleting, run `grep -rn "WishlistConstants\|DEFAULT_WISHLIST_ID" --include=*.kt .` and confirm no reference remains outside `build/`. Depends on T006, T011, T013, T015, T025, T030.
- [ ] T034 [P] In `core/database/CLAUDE.md`, delete the bullet saying `GameDao` interpolates `WishlistConstants.DEFAULT_WISHLIST_ID` into SQL. Do not turn it into a note saying it is gone. Add `DefaultWishlistEntity`("default_wishlist") to the entity naming list, as a single-row pointer that makes the default wishlist a stored value. Keep the existing sentence about `onCreate` seeding the default wishlist's name and description, and extend it to mention the pointer row.
- [ ] T035 [P] In `core/data/CLAUDE.md`, update the "Caching" sentence saying `isWishlisted` comes from `combine(..., gameDao.getGameIdsInList(DEFAULT_WISHLIST_ID))` so it names `gameDao.observeGameIdsInDefaultList()` and the stored default.
- [ ] T036 [P] In `docs/tech-debt.md` (line ~57, "Test coverage gaps"), remove `:feature:wishlist` from the list of modules with no tests. Do not annotate it as fixed. Keep `:core:database` DAOs and `:core:ui`.
- [ ] T037 Run the full verification from `quickstart.md` § 1: `./gradlew test` and `./gradlew :app:assembleDebug`. Then confirm `grep -rn DEFAULT_WISHLIST_ID --include=*.kt .` is empty outside `build/`, and that the generated `QuestLogDatabase_Impl` under `core/database/build/generated/` runs `PRAGMA foreign_keys = ON` (research R1). Read it only; never edit it.
- [ ] T038 Run the manual scenarios 1-12 in `quickstart.md` § 2 on an emulator or device. Uninstall the app first (or clear its data) so the seed creates the pointer row.

---

## Dependencies & Execution Order

### Phase dependencies

- **Setup (Phase 1)**: No dependencies. It can start immediately.
- **Foundational (Phase 2)**: Does not need Phase 1. T001 only matters for T024. It blocks all user stories.
- **US2 (Phase 3)**: Needs Foundational only.
- **US1 (Phase 4)**: Needs Foundational and T001. It does not need US2 at the code level, but its value is only visible together with US2.
- **US3 (Phase 5)**: Needs Foundational (T007). Its T031 extends the test file US1 creates (T024).
- **Polish (Phase 6)**: Needs all three stories. T033 specifically needs every call site migrated.

### Within the foundation

```text
T002 → T003 → { T004, T005, T006, T007 in parallel } → T008 → T009
```

### Within each story

- Tests first, then models and state, then use cases, then ViewModel, then UI.
- US2: `T010 ∥ T011 → T012 → T013 → T014`, with `T015` in parallel and `T016` last.
- US1: `{T017, T018, T019, T022, T023 in parallel} → T020 → T021`; then `T024 → T025 → T026 → T027 → T028`.
- US3: `T029 → T030 → T031 → T032`.

### Parallel opportunities

- After T003: T004, T005, T006 and T007 touch four different files.
- After the foundation: US2 (T010-T016) and US1's independent tasks (T017, T018, T019, T022, T023) can run side by side.
- T029 (US3 test) can be written any time after T007.
- T034, T035 and T036 are docs-only and fully parallel.

### Parallel example: foundation

```text
# Once T003 is done, launch together:
Task: "Add observeDefaultListId / getDefaultListId / setDefaultList to DB/dao/ListDao.kt"
Task: "Add observeGameIdsInDefaultList to DB/dao/GameDao.kt"
Task: "Rewrite the onCreate seed in DB/di/DatabaseModule.kt"
Task: "Add the 3 repository methods to DOMAIN/repository/GameRepository.kt"
```

---

## Implementation Strategy

### MVP: US2 + US1, shipped with US3

1. Phase 2 (foundation), then Phase 3 (US2). The heart now reads the stored default, but nothing in the UI can change it yet, so the behavior users see is the same as today.
2. Phase 4 (US1). This is the first user-visible change. **Stop and validate** with `quickstart.md` scenarios 1-7 and 12.
3. Phase 5 (US3). It is one use-case file plus tests, and it should land in the same release as US1: without it the old id-1 check wrongly blocks deleting the previous default (see the note in Phase 5).
4. Phase 6 (polish), including the constant's deletion and the doc updates.

### Notes

- **Delete `WishlistConstants` last (T033).** The foundation adds code only, and each story migrates its own call sites. Deleting the constant earlier would leave `:core:data`, `:core:domain` and `:feature:wishlist` uncompilable.
- The database stays at `version = 1` with destructive fallback and no migrations. Commit the regenerated `1.json` schema together with the entity (T002/T009).
- Do not touch `GameRepository.getWishlistedGames()` beyond T011: it has no callers, and whether to delete it is the owner's call (research R5).
- Out of scope (spec Assumptions): a default badge or swipe action on the Lists overview screen.
- A task is done when its stated command passes. Verification tasks (T009, T016, T028, T032, T037) are real steps, not formalities.
