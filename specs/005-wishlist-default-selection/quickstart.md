# Quickstart: Validating Default Wishlist Selection

**Feature**: [spec.md](./spec.md) | **Contracts**: [contracts/](./contracts/) | **Data model**: [data-model.md](./data-model.md)

## Prerequisites

- `local.properties` contains the IGDB credentials. You need them to open game details.
- Use an emulator or device on API 29+.
- The schema changes, and the database stays at `version = 1`. Uninstall the app first, or clear its
  data, so `onCreate` runs again and seeds the default pointer. `fallbackToDestructiveMigration` only
  fires on a version bump, which this feature does not make.

On Windows (PowerShell), use `.\gradlew.bat` instead of `./gradlew`.

## 1. Build & automated checks

```bash
./gradlew :core:database:compileDebugKotlin --console=plain -q   # entity, DAO, schema export
./gradlew :core:domain:testDebugUnitTest --console=plain -q      # DeleteListUseCaseTest, GetWishlistDetailUseCaseTest, GetListsUseCaseTest
./gradlew :core:data:testDebugUnitTest --console=plain -q        # GameRepositoryImplToggleWishlistTest
./gradlew :feature:wishlist:testDebugUnitTest --console=plain -q # WishlistViewModelTest
./gradlew :feature:lists:testDebugUnitTest --console=plain -q    # ListsViewModelTest, ListsUiMapperTest
./gradlew :app:assembleDebug                                     # spans modules
./gradlew test                                                   # all existing suites stay green
```

**Expected results**

- Everything compiles and passes.
- `git status` shows a modified exported schema (`core/database/schemas/.../1.json`) that contains a
  `default_wishlist` table with a foreign key to `wishlists`.
- `grep -rn DEFAULT_WISHLIST_ID --include=*.kt .` returns nothing outside `build/`.
- The generated `QuestLogDatabase_Impl` (under `core/database/build/generated/`) runs
  `PRAGMA foreign_keys = ON` (research R1). Read it only; do not edit it.

## 2. Manual scenarios

Start from a fresh install, then create two extra lists, **B** and **C**, from the Lists tab. The seeded
list is **A**.

| # | Steps | Expected | Covers |
|---|-------|----------|--------|
| 1 | Open A's detail screen | "Default" badge next to the title; no "⋮" menu | FR-001, FR-002a, FR-010 |
| 2 | Open B's detail screen | No badge; "⋮" menu with **Set as default** and **Delete list** | FR-002, FR-002a |
| 3 | On B: ⋮ → Set as default | No dialog; snackbar "This is now your default list"; the menu is replaced by the badge | FR-002b, FR-003, SC-001 |
| 4 | Back → open A | No badge; "⋮" menu now present | FR-003 |
| 5 | Open any game's detail → tap the heart | The heart fills; the game appears in **B**, not in A | FR-007, SC-003 |
| 6 | Tap the heart again on the same game | The heart empties; the game leaves B | FR-008 |
| 7 | Save a game to A with the heart *before* step 3, then do step 3 | The game is still in A; its heart now shows empty (it is not in B) | FR-009 |
| 8 | On A (now non-default): ⋮ → Delete list → confirm | A is deleted; you navigate back | FR-005, US3-AS4 |
| 9 | Open B (default) | No menu, so delete can't be reached | FR-004, SC-002 |
| 10 | Delete C, so only B remains | B still shows the badge; there is no menu anywhere | FR-006, SC-004, US3-AS2 |
| 11 | Kill and relaunch the app | B is still the default; the heart still targets B | persistence |
| 12 | Search tab → tap a result's save button | The game lands in the current default | FR-011 |
| 13 | Open the Lists tab (the overview) | Exactly one row, A's, shows a "Default" badge beside its name. Rows keep their order, and no row has a new action. Read the row once with TalkBack. | FR-012, SC-005, US1-AS4 |
| 14 | Do scenario 3 (set B as default), then go back to the overview | The badge is on B's row and gone from A's, without leaving and re-entering the tab | FR-012, US1-AS4 |
