# Contract: Wishlist view-mode preference

Layering mirrors `AppearancePreferenceStore` exactly. The feature module only ever sees the two use cases.

```text
:feature:wishlist ──▶ :core:domain (use cases + interface) ◀── :core:data (DataStore impl, Hilt @Binds)
```

## `:core:domain` — `core/domain/settings/WishlistViewModePreferenceStore.kt`

```kotlin
interface WishlistViewModePreferenceStore {
    /** Emits the current mode and every subsequent change. Emits LIST when nothing is persisted yet. */
    fun observeWishlistViewMode(): Flow<WishlistViewMode>

    /** Persists [mode]. Overwrites any previous value. */
    suspend fun setWishlistViewMode(mode: WishlistViewMode)
}
```

The interface lives in `:core:domain` and never imports `androidx.datastore`.

## `:core:domain` — use cases, `core/domain/usecase/list/`

| Class | Signature | Behaviour |
|-------|-----------|-----------|
| `GetWishlistViewModeUseCase` | `operator fun invoke(): Flow<WishlistViewMode>` | Pass-through to `observeWishlistViewMode()`. |
| `SetWishlistViewModeUseCase` | `suspend operator fun invoke(mode: WishlistViewMode)` | Pass-through to `setWishlistViewMode(mode)`. |

These are plain classes with `@Inject constructor`, with no base type (project convention).

## `:core:data` — `core/data/settings/WishlistViewModePreferenceStoreImpl.kt`

- `@Inject constructor(settingsDataStore: DataStore<Preferences>)`: the **same** singleton the appearance
  store receives from `DataModule.provideSettingsDataStore`. No second DataStore file.
- Key: `intPreferencesKey("wishlist_view_mode")`. It stores `WishlistViewMode.id` and reads through
  `WishlistViewMode.fromId(...)`, with `LIST.id` when absent.
- Bound in `DataModule` with `@Binds @Singleton`, next to `bindAppearancePreferenceStore`.
- DB-only/local, so it returns a bare `Flow` / `Unit`, never `AppResult` (constitution Principle II).

## Guarantees the tests pin down

1. A fresh store emits `LIST`.
2. `set(GRID)` makes the next emission `GRID`.
3. A second store instance over the same file reads back the persisted value (the relaunch case).
4. An unknown stored id reads as `LIST`. This is covered by `WishlistViewMode.fromId`.
