# Research: Default Wishlist Selection

**Feature**: [spec.md](./spec.md) | **Plan**: [plan.md](./plan.md) | **Date**: 2026-10-01

The Technical Context had no open `NEEDS CLARIFICATION` items: the stack is fixed by the constitution, and
the spec's UX questions were settled in `/speckit-clarify`. What remained were design decisions about how
the current hardcoded default turns into user-chosen state. Each one is recorded below.

## Starting point

The default wishlist is not data today. It is `WishlistConstants.DEFAULT_WISHLIST_ID = 1L` in
`:core:model`, and it is used in four places:

- **SQL**: interpolated at compile time into `GameDao.getWishlistedGames()`.
- **Repository**: 9 call sites in `GameRepositoryImpl` (`observeGameDetail`, `refreshGameDetail`,
  `getRecentlyViewedGames`, `getWishlistedGameIds`, `toggleWishlist` ×3, `getSavedGames`,
  `getGamesByList`), mostly as `gameDao.getGameIdsInList(DEFAULT_WISHLIST_ID)` to derive `isWishlisted`.
- **Deletion guard**: `DeleteListUseCase` refuses that id, and `WishlistViewModel` hides the options menu
  for it.
- **Seed**: `DatabaseModule`'s `onCreate` callback inserts the starter list with that explicit id.

Every one of these has to read a stored value instead. The value has to be reactive, so that
`isWishlisted` (the heart's filled state) re-derives when the default moves.

---

## R1 — Where the default designation is stored

**Decision**: A new single-row Room table, `default_wishlist` (`DefaultWishlistEntity`). It has a fixed
primary key and a `listId` column. `listId` is a foreign key to `wishlists.id` with
`onDelete = ForeignKey.RESTRICT`.

**Rationale**:

- **Exactly one default (FR-003).** The primary key is fixed, so there can be at most one row. The seed
  writes the row and the DAO exposes only an `UPDATE`, so there is never zero.
- **The default can't be deleted (FR-004, SC-002).** The database enforces this through the `RESTRICT`
  foreign key, whichever entry point issues the delete. The use-case check (R4) is what users hit. The
  constraint is the backstop that makes the 100% claim in SC-002 true even if a future code path skips
  the use case.
- **The designation belongs to the set of wishlists, not to one list (spec Assumptions).** A pointer
  models that directly. `ListEntity` and the `WishlistList` domain model stay unchanged.
- **Flows react on their own.** A query written as
  `... WHERE listId = (SELECT listId FROM default_wishlist)` makes Room's invalidation tracker observe
  `default_wishlist` as well. Every derived `Flow` (the heart state, the search grid's saved marks) then
  re-emits when the default changes, with no `flatMapLatest` plumbing in the repository.
- **One store.** It lives in the same database as the lists it points at, so a destructive schema
  recreate re-seeds both together. There is precedent: user settings such as `owned_platforms` are
  already Room tables, and the project has no DataStore.

**Alternatives considered**:

- **An `isDefault` column on `wishlists`.** Rejected. Keeping exactly one `true` needs either a partial
  unique index, which Room's `@Index` cannot declare, or code discipline in every writer. It also does
  nothing for the delete guard at the database level.
- **DataStore or SharedPreferences holding the list id.** Rejected. It splits one fact across two
  stores, the delete can't be checked atomically against it, and it is not in the project. A destructive
  Room recreate would also leave the preference pointing at a list that no longer exists.

**Side effect to verify**: Room turns on `PRAGMA foreign_keys = ON` once any entity declares a foreign
key, and this is the first one in the schema. Only tables that declare foreign keys are affected, and
`default_wishlist` is the only one. Check this after the first build: the generated
`QuestLogDatabase_Impl` should run the pragma. Read it only; never edit `build/generated/**`.

**Risk noted**: `ListDao.insertList` uses `OnConflictStrategy.REPLACE`. In SQLite, REPLACE deletes the
conflicting row first, so with the foreign key on, re-inserting a list with the default's id would fail.
`insertList` is only called with `id = 0` (autogenerate), so no conflict is possible today. The plan
leaves it alone.

---

## R2 — Seeding without a magic id

**Decision**: `onCreate` inserts the starter list without an explicit id. It then runs
`INSERT INTO default_wishlist (id, listId) VALUES (0, last_insert_rowid())`.
`WishlistConstants` is deleted from `:core:model`.

**Rationale**: Once the pointer is stored, nothing reads the constant except the seed, and the seed
doesn't need it. Leaving the constant in would invite new code to compare against `1L` and quietly bring
the hardcoded default back. The `WishlistList` KDoc and the `DeleteListUseCase` KDoc mention it and are
updated in the same change.

**Alternatives considered**: Keep the constant, private to `DatabaseModule`. Workable, but it is still a
second source of truth about which list is the starter one. `last_insert_rowid()` is exact inside the
same callback.

---

## R3 — How the default reaches the domain and the UI

**Decision**:

- `GameRepository` gains three methods: `observeDefaultListId(): Flow<Long>`,
  `suspend getDefaultListId(): Long` and `suspend setDefaultList(listId: Long)`. They are DB-only, so
  they return a bare `Flow`, a value and `Unit` (constitution II).
- `WishlistDetail` gains `isDefault: Boolean`. `GetWishlistDetailUseCase` combines it from
  `observeDefaultListId()`.
- `WishlistList` in `:core:model` is not changed.

**Rationale**: The only screen that needs "is this list the default?" in this scope is the list detail
screen, which already consumes `WishlistDetail`. Putting the flag on the composite domain model keeps
`:core:model`, `ListEntity`, `ListWithGameCount` and the list mappers untouched. When the Lists overview
later needs a per-row badge (the deferred swipe-action discussion), `GetListsUseCase` can combine the
same `observeDefaultListId()`.

**Alternatives considered**: Add `isDefault` to `WishlistList` as a computed SQL column. It touches four
more files (entity relation, both list queries, the mapper, the model) to serve a screen that is out of
scope. Deferred until the overview needs it.

---

## R4 — Where the "can't delete the default" rule lives

**Decision**: It stays in `DeleteListUseCase`. The use case now compares against
`repository.getDefaultListId()` instead of the constant, and still returns `false` for the default. The
`RESTRICT` foreign key (R1) is the database backstop.

**Rationale**: This keeps the current layering, where the rule is in the domain and testable by mocking
the repository. The read and the delete are not in one transaction. Racing them would need a concurrent
set-default and delete on the same list from two screens in a single-user app. Even then the foreign key
aborts the delete rather than corrupting state.

**Alternatives considered**: A `@Transaction` DAO method `deleteListIfNotDefault(): Boolean`, with
`GameRepository.deleteList` returning `Boolean`. It is atomic, but it moves a domain rule into SQL and
changes a repository signature, to close a race the foreign key already makes harmless.

---

## R5 — Queries that mean "games in the default list"

**Decision**:

- `GameDao` gains `observeGameIdsInDefaultList(): Flow<List<Int>>`, using the subquery form from R1.
- `GameDao.getWishlistedGames()` switches from the interpolated constant to the same subquery.
- `toggleWishlist` and `refreshGameDetail` resolve the id once through `listDao.getDefaultListId()` and
  keep using `isGameInList(gameId, listId)` and the cross-ref insert/delete.
- `GameDao.getGameIdsInList(listId)` loses its last caller, so it is deleted.

**Rationale**: The subquery keeps the reactive observe-paths free of `flatMapLatest`. The one-shot
write-paths need the concrete id anyway, to build a `GameListCrossRef`.

**Out-of-scope observation**: `GameRepository.getWishlistedGames()` has no callers outside the interface.
Its query is updated only because the constant it interpolates is going away. Whether to delete it is
left to the owner, per the "don't silently fix" rule.

---

## R6 — List-detail screen state while loading

**Decision**: `WishlistUiState.canDeleteList` is replaced by two flags, both defaulting to `false`:

- `isDefaultList` drives the "Default" badge.
- `showListOptions` drives the "⋮" menu, which now holds both "Set as default" and "Delete list".

The ViewModel sets exactly one of them once the detail has loaded.

**Rationale**: A single `isDefaultList = false` default would flash the menu during `Loading` for a list
that turns out to be the default. With two flags that both start `false`, nothing shows until the answer
is known. The composable only reads the flags (constitution III).

**Alternatives considered**: Make `isDefaultList` nullable (`Boolean?`). That pushes a
three-state check into the composable. Rejected.

---

## R7 — Reusing the shared layer

**Decision**:

- The badge is `CustomSummaryBadge` from `:core:ui`. It is already theme-aware for light and dark.
- The confirmation is the existing `WishlistUiEffect.ShowSnackbar`.
- There is no new dialog (clarification Q2).

**Rationale**: Constitution IV. Both pieces already exist and match the visual language of the other
small status chips.

---

## R8 — Who else follows the default

**Decision**: No code changes are needed in `:feature:game-detail` or `:feature:search`. Only a stale
comment in `GameDetailViewModel.confirmListSelection()` is updated.

**Rationale**:

- The heart calls `ToggleWishlistUseCase`, which calls `repository.toggleWishlist`, which now resolves the
  stored default (FR-007, FR-008).
- Its filled state comes from `observeGameDetail`, which now combines with
  `observeGameIdsInDefaultList()`, so it updates the moment the default changes.
- The search grid's save button uses the same use cases, so it follows the default too. This is
  consistent behavior and worth stating in the PR description.

---

## R9 — Tests

**Decision**:

- **`:core:domain`**: new `DeleteListUseCaseTest` (default refused, non-default deleted) and
  `GetWishlistDetailUseCaseTest` (`isDefault` derived; it flips when the default id emits a new value).
- **`:core:data`**: `GameRepositoryImplToggleWishlistTest` stubs `listDao.getDefaultListId()` with a
  non-1 id, and asserts the cross-ref is built with that id. Today it matches `any()`, which would hide a
  regression back to a fixed id.
- **`:feature:wishlist`**: new `WishlistViewModelTest`. It covers badge vs. menu flags,
  `OnSetAsDefault` → use case + snackbar, and that the delete path is unchanged. This needs the three
  `testImplementation` lines copied from `feature/lists/build.gradle.kts`.
- **`docs/tech-debt.md`**: `:feature:wishlist` is removed from the "no tests at all" entry, per the
  remove-don't-annotate rule.
- **No `SetDefaultListUseCase` test.** It is a pass-through with no logic of its own.
- **No DAO tests.** They are a known, listed gap (`:core:database`). The foreign key and seed behavior are
  covered by the manual checks in [quickstart.md](./quickstart.md).
