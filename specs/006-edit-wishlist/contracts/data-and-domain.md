# Contract: Data & Domain Interfaces

The app exposes no external API, so these contracts are the internal module seams this feature changes.
Each signature is the contract; the body is implementation detail.

## `ListDao` (`:core:database`): no change

The existing `@Update suspend fun updateList(list: ListEntity)` gets its first caller.
`insertList` (`REPLACE`) MUST NOT be used for edits. See [research R5](../research.md#r5-repository-write-cover-file-lifecycle-and-failure-semantics).

## `CoverImageUpdate` (`:core:domain`): new

`core/domain/src/main/java/com/nikolasguillen/questlog/core/domain/model/CoverImageUpdate.kt`

```kotlin
sealed interface CoverImageUpdate {
    data object Keep : CoverImageUpdate
    data object Remove : CoverImageUpdate
    data class Replace(val sourceUri: String) : CoverImageUpdate
}
```

This is a sealed hierarchy, so its cases live in the same file (the one-type-per-file exception).

## `GameRepository` (`:core:domain`): addition

```kotlin
/**
 * Saves new details for [listId]. Its games, their statuses and whether it is the default are left alone.
 * Updating an unknown [listId] is a no-op.
 *
 * Like [createList], a [AppResult.Failure] only means [coverImage] was a [CoverImageUpdate.Replace] whose
 * image failed to persist: the other fields are still saved and the previous cover is kept.
 */
suspend fun updateList(
    listId: Long,
    name: String,
    description: String,
    icon: WishlistIcon?,
    coverImage: CoverImageUpdate
): AppResult<Unit>
```

This is DB- and file-only, but it returns `AppResult` for the same documented reason as `createList`.
`core/data/CLAUDE.md` names `createList` as the sole exception to "DB-only methods return bare types".
Extend that sentence to name `updateList` too.

## `GameRepositoryImpl.updateList` (`:core:data`): behaviour contract

| Given                                      | Then                                                                     |
|--------------------------------------------|--------------------------------------------------------------------------|
| `getListById` returns `null`               | No persist, no update, no delete. Returns success.                       |
| `Keep`                                     | `updateList(copy(..., coverImagePath = old))`. No storage calls. Success.|
| `Remove`, old path `p`                     | `updateList(copy(..., coverImagePath = null))`, **then** `delete(p)`. Success. |
| `Remove`, no old path                      | `updateList(copy(..., null))`. No delete. Success.                       |
| `Replace(u)`, `persist(u)` returns `n`, old `p` | `updateList(copy(..., n))`, **then** `delete(p)`. Success.          |
| `Replace(u)`, `persist(u)` returns `n`, no old | `updateList(copy(..., n))`. No delete. Success.                      |
| `Replace(u)`, `persist(u)` returns `null`  | `updateList(copy(..., old))`. No delete. `failure(FileStorage)`.         |

`id` is always the existing one, and name, description and icon are always the incoming values.

## `UpdateListUseCase` (`:core:domain`): new

`core/domain/src/main/java/com/nikolasguillen/questlog/core/domain/usecase/list/UpdateListUseCase.kt`

```kotlin
class UpdateListUseCase @Inject constructor(private val repository: GameRepository) {
    suspend operator fun invoke(
        listId: Long,
        name: String,
        description: String,
        icon: WishlistIcon?,
        coverImage: CoverImageUpdate
    ): AppResult<Unit>
}
```

A pure delegation, like `CreateListUseCase`. Its KDoc repeats the failure semantics.
