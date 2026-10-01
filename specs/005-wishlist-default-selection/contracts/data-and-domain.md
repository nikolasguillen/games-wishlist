# Contract: Data & Domain Interfaces

The app exposes no external API, so these contracts are the internal module seams this feature changes.
Each signature is the contract; the body is implementation detail.

## `ListDao` (`:core:database`): additions

```kotlin
@Query("SELECT listId FROM default_wishlist WHERE id = 0")
fun observeDefaultListId(): Flow<Long>

@Query("SELECT listId FROM default_wishlist WHERE id = 0")
suspend fun getDefaultListId(): Long

@Query("UPDATE default_wishlist SET listId = :listId WHERE id = 0")
suspend fun setDefaultList(listId: Long)
```

- There is no insert or delete for `default_wishlist`. The seed is the only writer of the row (data-model
  invariant).

## `GameDao` (`:core:database`): changes

```kotlin
// new
@Query("SELECT gameId FROM game_list_cross_ref WHERE listId = (SELECT listId FROM default_wishlist)")
fun observeGameIdsInDefaultList(): Flow<List<Int>>

// changed: subquery replaces the interpolated WishlistConstants.DEFAULT_WISHLIST_ID
fun getWishlistedGames(): Flow<List<GameWithAllDetails>>

// removed: no callers remain
fun getGameIdsInList(listId: Long): Flow<List<Int>>
```

## `GameRepository` (`:core:domain`): additions

```kotlin
/** Emits the id of the wishlist currently set as the default, and again whenever it changes. */
fun observeDefaultListId(): Flow<Long>

/** One-shot read of the current default wishlist id. */
suspend fun getDefaultListId(): Long

/** Makes [listId] the default wishlist. The previous default loses the designation; no game membership changes. */
suspend fun setDefaultList(listId: Long)
```

The `toggleWishlist` and `getWishlistedGameIds` KDocs already say "the default wishlist". Their signatures
are unchanged; what changes is which list that phrase resolves to.

## Use cases (`:core:domain/usecase/list/`)

| Use case                    | Signature                                       | Behavior                                                                                         |
|-----------------------------|-------------------------------------------------|--------------------------------------------------------------------------------------------------|
| `SetDefaultListUseCase`     | `suspend operator fun invoke(listId: Long)` | **New.** Delegates to `repository.setDefaultList(listId)`.                                        |
| `DeleteListUseCase`         | `suspend operator fun invoke(listId: Long): Boolean` | **Changed.** Returns `false` without deleting if `listId == repository.getDefaultListId()`. Otherwise deletes and returns `true`. |
| `GetWishlistDetailUseCase`  | `operator fun invoke(listId: Long): Flow<WishlistDetail?>` | **Changed.** Also combines `observeDefaultListId()` and fills `WishlistDetail.isDefault`. Still emits `null` when the list is gone. |
| `GetListsUseCase`           | `operator fun invoke(): Flow<List<WishlistSummary>>` | **Changed.** Combines `getAllLists()` with `observeDefaultListId()`. Each row's `isDefault` is `list.id == defaultListId`, so the label moves live when the default changes. Row order is whatever `getAllLists()` emits. |
| `ToggleWishlistUseCase`     | unchanged                                       | Targets whatever `getDefaultListId()` returns at call time (FR-007, FR-008).                        |
| `GetWishlistedGameIdsUseCase` | unchanged                                     | Re-emits when the default changes, through the subquery in `observeGameIdsInDefaultList()`.         |

## Domain model: `WishlistSummary` (`:core:domain/model/`), new

```kotlin
data class WishlistSummary(
    val list: WishlistList,
    val isDefault: Boolean
)
```

Used only by `GetListsUseCase` and the overview screen. `WishlistList` in `:core:model` is unchanged
(research R10).
