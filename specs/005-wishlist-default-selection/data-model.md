# Data Model: Default Wishlist Selection

**Feature**: [spec.md](./spec.md) | **Research**: [research.md](./research.md) | **Date**: 2026-10-01

The database stays at `version = 1`, with `fallbackToDestructiveMigration(true)` and no `Migration`
objects (constitution, Persistence). The exported schema under
`core/database/schemas/com.nikolasguillen.questlog.core.database.QuestLogDatabase/1.json` is regenerated
and committed in the same commit as the entity change.

## Persistence (`:core:database`)

### `DefaultWishlistEntity` — new, table `default_wishlist`

A single-row pointer: "which wishlist is the default".

| Column   | Type    | Constraints                                                                      |
|----------|---------|----------------------------------------------------------------------------------|
| `id`     | INTEGER | Primary key, not auto-generated. Always `0`, which makes this a single-row table. |
| `listId` | INTEGER | NOT NULL. Foreign key → `wishlists.id`, `onDelete = RESTRICT`. Indexed.           |

**Invariants**

- **Exactly one row exists.**
  - It is written once, by the seed in `RoomDatabase.Callback.onCreate`.
  - The fixed primary key rules out a second row.
  - The DAO exposes no insert and no delete for this table, only an `UPDATE`.
- **`listId` always names an existing wishlist.** The foreign key rejects an update to a missing list,
  and `RESTRICT` rejects deleting the list it points at.

**Index**: `@Index("listId")`. Room warns about a foreign-key column without an index, because the
parent-side `RESTRICT` check would otherwise scan the table.

### `ListEntity` — table `wishlists`

No column changes.

Side effect: with the foreign key in place, deleting a row is refused while `default_wishlist.listId`
points at it. `ListDao.deleteListWithGameRefs` then throws and its transaction rolls back, which also
restores the cross-refs it had already deleted. In normal operation `DeleteListUseCase` catches this
case first.

### `GameListCrossRef` — table `game_list_cross_ref`

Not changed. Membership rows are independent of the default designation (FR-009).

### Seed (`DatabaseModule` → `onCreate`)

1. `INSERT INTO wishlists (name, description, icon) VALUES (…)`. There is no explicit id any more.
2. `INSERT INTO default_wishlist (id, listId) VALUES (0, last_insert_rowid())`.

This satisfies FR-010: on first install, the seeded list is the default.

## Domain (`:core:domain`, `:core:model`)

### `WishlistDetail` (`core/domain/model/`) — changed

| Property    | Type           | Change                                                                  |
|-------------|----------------|-------------------------------------------------------------------------|
| `list`      | `WishlistList` | unchanged                                                               |
| `games`     | `List<Game>`   | unchanged                                                               |
| `isDefault` | `Boolean`      | **new.** `true` when `list.id` equals the current default list id.      |

### `WishlistList` (`core/model/`)

No field changes. Its KDoc no longer refers to `WishlistConstants`.

### `WishlistConstants` (`core/model/`) — deleted

The default is now data, not a constant (research R2).

## State transitions

```text
                 setDefaultList(B)               setDefaultList(C)
 default = A  ───────────────────▶  default = B  ───────────────────▶  default = C
     │                                  │
     │ delete(A) → refused              │ delete(A) → allowed (A is no longer default)
     │ delete(B) → allowed              │ delete(B) → refused
```

- `setDefaultList(current default)` is a no-op. The `UPDATE` writes the same value and the UI never
  offers the action in that case.
- Creating a list never moves the default (spec Edge Cases).
- Changing the default never touches `game_list_cross_ref` (FR-009).

## UI state (`:feature:wishlist`)

### `WishlistUiState` — changed

| Property          | Type                   | Default   | Meaning                                                                              |
|-------------------|------------------------|-----------|--------------------------------------------------------------------------------------|
| `listName`        | `UiText`               | `""`      | unchanged                                                                            |
| `isDefaultList`   | `Boolean`              | `false`   | **new.** Shows the "Default" badge in the top bar (FR-001).                          |
| `showListOptions` | `Boolean`              | `false`   | **replaces `canDeleteList`.** Shows the "⋮" menu with "Set as default" and "Delete list" (FR-002, FR-002a). |
| `contentState`    | `WishlistContentState` | `Loading` | unchanged                                                                            |

Once loaded, exactly one of `isDefaultList` and `showListOptions` is `true`. While loading, both are
`false`, so neither the badge nor the menu flashes (research R6).
