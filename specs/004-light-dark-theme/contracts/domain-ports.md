# Contract: `:core:domain` ports and use cases

**Feature**: `004-light-dark-theme` | **Date**: 2026-09-30

Signatures are the contract; bodies belong to implementation. See research.md §2 for why this is a
narrow port rather than a second repository.

## Port

### `settings/AppearancePreferenceStore.kt`

```kotlin
/**
 * Reads and writes the user's [AppearanceMode] choice. Contract only: `:core:domain` never imports
 * `androidx.datastore`.
 */
interface AppearancePreferenceStore {
    /** Emits the current [AppearanceMode] and every subsequent change. Defaults to [AppearanceMode.SYSTEM]
     * when nothing has been persisted yet. */
    fun observeAppearanceMode(): Flow<AppearanceMode>

    /** Persists [mode] as the new selection. Overwrites any previous value. */
    suspend fun setAppearanceMode(mode: AppearanceMode)
}
```

The implementation (`core/data/settings/AppearancePreferenceStoreImpl.kt`) backs this with DataStore
Preferences per data-model.md, and is bound with `@Binds @Singleton` in the existing `core/data/di/DataModule.kt`.

## Use cases

Plain classes with `operator fun invoke(...)`, under `core/domain/usecase/settings/`. Both are thin
pass-throughs — same shape as `GetReleaseNotificationGameIdsUseCase`, and for the same reason: they exist
so `feature/settings` and `:app` depend on `:core:domain`, never on the port or its implementation directly.

### `GetAppearanceModeUseCase`

```kotlin
operator fun invoke(): Flow<AppearanceMode>
```

Straight pass-through to `AppearancePreferenceStore.observeAppearanceMode()`. Two independent consumers
fold this into their own state: `SettingsViewModel` (to show the current selection) and the new
`MainActivity` in `:app`, via a field-injected use case (to resolve the whole app's color scheme —
no dedicated ViewModel, see research.md §3).

### `SetAppearanceModeUseCase`

```kotlin
suspend operator fun invoke(mode: AppearanceMode)
```

Straight pass-through to `AppearancePreferenceStore.setAppearanceMode(mode)`. The single entry point behind
the Settings screen's Appearance selector, so no other surface can write this preference by a different
path.
