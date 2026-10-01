# Implementation Plan: Default Wishlist Selection

**Branch**: `005-wishlist-default-selection` | **Date**: 2026-10-01 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/005-wishlist-default-selection/spec.md`

## Summary

Today the default wishlist is a compile-time constant (`WishlistConstants.DEFAULT_WISHLIST_ID = 1L`). It
is interpolated into SQL and checked at 9 repository call sites. This plan turns it into persisted,
user-chosen state.

**Storage.** A single-row Room table, `default_wishlist`, points at the chosen list through a
`RESTRICT` foreign key. The fixed key gives "exactly one default", and the database itself refuses to
delete the default.

**Reactivity.** Queries over "the default list" use a `(SELECT listId FROM default_wishlist)` subquery,
so every derived `Flow` re-emits when the default moves. That covers the heart's filled state, the
search grid's save marks and `isWishlisted` everywhere.

**Entry points.**

- **List detail screen**: a **Set as default** item joins **Delete list** in the existing "⋮" menu. It
  applies immediately and confirms with a snackbar. The default list shows a "Default" badge instead of
  the menu.
- **Game detail heart** (`GameDetailActionPill`): no change is needed. It already goes through
  `ToggleWishlistUseCase`, which now resolves the stored default.

## Technical Context

**Language/Version**: Kotlin 2.4.10, JVM toolchain 21, Java 11 target

**Primary Dependencies**: Jetpack Compose (BOM 2026.09.00), Hilt 2.60.1, Room 2.8.5 (KSP, legacy
`SupportSQLiteOpenHelper` path), kotlinx-coroutines / Flow

**Storage**: Room, `QuestLogDatabase` `version = 1`, destructive fallback, schema exported to
`core/database/schemas/`. One new table (`default_wishlist`) and no column changes.

**Testing**: JUnit4 + MockK + `kotlinx-coroutines-test` (`StandardTestDispatcher`,
`Dispatchers.setMain`/`resetMain`). Use-case tests mock the repository; ViewModel tests mock the use
cases.

**Target Platform**: Android, minSdk 29 / targetSdk 37

**Project Type**: Modular mobile app (17 Gradle modules)

**Performance Goals**: No new targets. The added subquery reads one row of a single-row table, and the
heart toggle stays a few indexed lookups.

**Constraints**:

- No database `version` bump and no `Migration`.
- No new module dependency edges.
- Feature modules must not see `:core:database` or `:core:data`.

**Scale/Scope**: A local, single-user store with typically fewer than 20 wishlists. About 20 source files
are touched across `:core:database`, `:core:model`, `:core:domain`, `:core:data`, `:feature:wishlist` and
a comment in `:feature:game-detail`.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle / constraint | Assessment | Result |
|---|---|---|
| **I. Module boundaries** | No new edges. `:feature:wishlist` uses only `:core:domain` (new use case, changed `WishlistDetail`) and `:core:ui` (`CustomSummaryBadge`), which it already depends on. `:core:database` already depends on `:core:model`. `:core:model` loses a file and gains no dependency. | ✅ Pass |
| **II. Typed errors** | The new repository methods are DB-only. They return bare `Flow<Long>`, `Long` and `Unit`, with no `AppResult`, matching `toggleWishlist(): Boolean` and `getFilteredSearchHistory(): List<String>`. No exception mapping is added. | ✅ Pass |
| **III. UI renders, doesn't decide** | `isDefaultList` and `showListOptions` are computed in `WishlistViewModel`; the top bar only reads them. All text is in `strings.xml`, and the snackbar text is `UiText.StringResource`. There is one new `UiEvent` and the `onEvent` `when` stays exhaustive. | ✅ Pass |
| **IV. Reuse the shared layer** | The badge is `CustomSummaryBadge`, the confirmation is the existing `ShowSnackbar` effect, and there is no new dialog. No `dp` literals are added. `GameRepository` stays the single repository. | ✅ Pass |
| **V. Local verification** | The real commands are listed in [quickstart.md](./quickstart.md). New logic gets tests in its own module. `:feature:wishlist` gains a test source set, by copying `feature/lists`'s three `testImplementation` lines. | ✅ Pass |
| **Persistence** | `version` stays 1. The exported schema is regenerated and committed with the entity. There is no list-shaped column. Game writes still go through `GameDao.saveGame`. | ✅ Pass |
| **Injection** | `WishlistViewModel` keeps `@AssistedInject` and gains a constructor-injected `SetDefaultListUseCase`. There is no `SavedStateHandle`. | ✅ Pass |
| **KMP** | A Room table with a foreign key works the same under Room KMP. Nothing here is expensive to reverse. | ✅ Pass |

**Post-design re-check (after Phase 1)**: Still all pass. The contracts add no dependency edge and no
`AppResult`, and the UI contract keeps every decision in the ViewModel.

**Drift observed, not acted on**: Constitution IV says "dark-theme only … no `isSystemInDarkTheme()`
branches". That contradicts `feature/CLAUDE.md` and spec 004 (light/dark theme). Under the
constitution's own precedence rule, `CLAUDE.md` wins and the constitution should be amended. That is an
owner decision outside this feature.

## Project Structure

### Documentation (this feature)

```text
specs/005-wishlist-default-selection/
├── plan.md              # This file
├── research.md          # Phase 0: decisions R1–R9
├── data-model.md        # Phase 1: entity, invariants, state transitions, UI state
├── quickstart.md        # Phase 1: build/test commands + manual scenarios
├── contracts/
│   ├── data-and-domain.md     # DAO / repository / use-case signatures
│   └── wishlist-screen-ui.md  # UiState / UiEvent / strings for the list detail screen
├── checklists/
│   └── requirements.md
└── tasks.md             # Phase 2 (/speckit-tasks; not created here)
```

### Source Code (repository root)

```text
core/database/
├── src/main/java/com/nikolasguillen/questlog/core/database/
│   ├── entity/DefaultWishlistEntity.kt          # NEW: single-row pointer, FK RESTRICT → wishlists
│   ├── QuestLogDatabase.kt                      # register DefaultWishlistEntity
│   ├── dao/ListDao.kt                           # + observeDefaultListId / getDefaultListId / setDefaultList
│   ├── dao/GameDao.kt                           # + observeGameIdsInDefaultList; getWishlistedGames → subquery;
│   │                                            #   − getGameIdsInList (no callers left)
│   └── di/DatabaseModule.kt                     # seed: list without explicit id + pointer via last_insert_rowid()
├── schemas/com.nikolasguillen.questlog.core.database.QuestLogDatabase/1.json   # regenerated
└── CLAUDE.md                                    # drop the "GameDao interpolates a constant" fact; name the new entity

core/model/src/main/java/com/nikolasguillen/questlog/core/model/
├── WishlistConstants.kt                         # DELETED
└── WishlistList.kt                              # KDoc only

core/domain/src/
├── main/java/com/nikolasguillen/questlog/core/domain/
│   ├── repository/GameRepository.kt             # + observeDefaultListId / getDefaultListId / setDefaultList
│   ├── model/WishlistDetail.kt                  # + isDefault
│   └── usecase/list/
│       ├── SetDefaultListUseCase.kt             # NEW
│       ├── DeleteListUseCase.kt                 # compare against repository.getDefaultListId()
│       └── GetWishlistDetailUseCase.kt          # combine observeDefaultListId()
└── test/java/com/nikolasguillen/questlog/core/domain/usecase/list/
    ├── DeleteListUseCaseTest.kt                 # NEW
    └── GetWishlistDetailUseCaseTest.kt          # NEW

core/data/
├── src/main/java/com/nikolasguillen/questlog/core/data/repository/GameRepositoryImpl.kt
│                                                # 9 constant sites → observeGameIdsInDefaultList() / getDefaultListId();
│                                                #   implement the 3 new repository methods
├── src/test/java/com/nikolasguillen/questlog/core/data/repository/GameRepositoryImplToggleWishlistTest.kt
│                                                # stub a non-1 default id, assert the cross-ref uses it
└── CLAUDE.md                                    # isWishlisted derivation sentence

feature/wishlist/
├── build.gradle.kts                             # + testImplementation(junit, coroutines-test, mockk)
├── src/main/java/com/nikolasguillen/questlog/feature/wishlist/
│   ├── WishlistViewModel.kt                     # flags from detail.isDefault; OnSetAsDefault → use case + snackbar
│   ├── WishlistScreen.kt                        # pass new flags/callback to the top bar
│   ├── components/WishlistTopBar.kt             # CustomSummaryBadge when default; "Set as default" menu item
│   └── model/
│       ├── WishlistUiState.kt                   # isDefaultList + showListOptions (replace canDeleteList)
│       └── WishlistUiEvent.kt                   # + OnSetAsDefault
├── src/main/res/values/strings.xml              # + set_as_default_action, default_list_label, default_list_set_message
└── src/test/java/com/nikolasguillen/questlog/feature/wishlist/WishlistViewModelTest.kt   # NEW

feature/game-detail/src/main/java/com/nikolasguillen/questlog/feature/gamedetail/GameDetailViewModel.kt
                                                 # stale comment in confirmListSelection() only

docs/tech-debt.md                                # remove :feature:wishlist from the "no tests at all" entry
```

**Structure Decision**: The existing modular layout is kept. The change follows the established
ViewModel → UseCase → `GameRepository` → DAO chain, with no new modules and no new dependency edges.
Persistence, domain and UI are three independently compilable layers, which also gives the natural task
order:

1. `:core:database` + `:core:model`
2. `:core:domain`
3. `:core:data`
4. `:feature:wishlist`

## Implementation Notes for `/speckit-tasks`

- **Order matters for compilation.** Deleting `WishlistConstants` breaks `:core:data`, `:core:domain` and
  `:feature:wishlist` until they are migrated. Delete it last, or in the same commit as the last
  migrated call site.
- **Story mapping.**
  - US2 (the heart follows the default) comes out of the persistence + repository work, with no UI work.
  - US1 (choose the default) and US3 (the default can't be deleted) both land in `:feature:wishlist` and
    `DeleteListUseCase`.
  - The first two layers are a shared foundation phase.
- **Out of scope** (spec Assumptions): a default badge or swipe action on the Lists overview screen.
  Research R3 records how `GetListsUseCase` would pick it up later.
- **Flag for the owner, not a task**: `GameRepository.getWishlistedGames()` has no callers (research R5).

## Complexity Tracking

No constitution violations to justify.
