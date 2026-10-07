# Contracts: Welcome Onboarding

This is an Android app with no external API, so these are the module-boundary contracts the feature adds
or changes. Signatures are indicative; KDoc and `internal` visibility follow the module CLAUDE.md files.

## `:core:database` — changed

```kotlin
// PlatformDao
@Query("SELECT EXISTS(SELECT 1 FROM owned_platforms WHERE platformId = :platformId)")
suspend fun isOwned(platformId: Int): Boolean

@Query("DELETE FROM owned_platforms WHERE platformId = :platformId")
suspend fun deleteOwnedPlatform(platformId: Int)

/** Flips one platform in a single transaction, so two quick taps can never overwrite each other. */
@Transaction
suspend fun toggleOwnedPlatform(platformId: Int) {
    if (isOwned(platformId)) deleteOwnedPlatform(platformId)
    else insertOwnedPlatform(OwnedPlatformEntity(platformId))
}
```

`setOwnedPlatforms(Set<Int>)` and `clearOwnedPlatforms()` are removed, because nothing else calls them.
There is no entity change, so the database schema and its version stay as they are.

## `:core:domain` — new and changed

```kotlin
// repository/GameRepository.kt — replaces setOwnedPlatforms(platformIds: Set<Int>)
/**
 * Adds [platformId] to the owned platforms, or removes it if already owned. Clears every cached generic
 * Discover lane first, so a cached lane can never be served for a selection it was not fetched under.
 */
suspend fun toggleOwnedPlatform(platformId: Int)

// usecase/discover/ToggleOwnedPlatformUseCase.kt — replaces SetOwnedPlatformsUseCase
class ToggleOwnedPlatformUseCase @Inject constructor(repository: GameRepository) {
    suspend operator fun invoke(platformId: Int)
}

// settings/OnboardingPreferenceStore.kt
interface OnboardingPreferenceStore {
    /** Emits whether the welcome flow was completed or skipped; `false` until it first is. */
    fun observeOnboardingCompleted(): Flow<Boolean>

    /** Marks the welcome flow as completed. Idempotent. */
    suspend fun setOnboardingCompleted()
}

// usecase/settings/GetOnboardingCompletedUseCase.kt
class GetOnboardingCompletedUseCase @Inject constructor(store: OnboardingPreferenceStore) {
    operator fun invoke(): Flow<Boolean>
}

// usecase/settings/CompleteOnboardingUseCase.kt
class CompleteOnboardingUseCase @Inject constructor(store: OnboardingPreferenceStore) {
    suspend operator fun invoke()
}
```

## `:core:data` — new and changed

- `GameRepositoryImpl.toggleOwnedPlatform` calls `discoverCacheDao.clearAll()`, then
  `platformDao.toggleOwnedPlatform(id)`. It replaces `setOwnedPlatforms`, and the KDoc keeps the
  "order matters" explanation.
- `settings/OnboardingPreferenceStoreImpl` works over the injected `DataStore<Preferences>`, with the key
  `booleanPreferencesKey("onboarding_completed")`.
- `DataModule`: `@Binds @Singleton bindOnboardingPreferenceStore(impl): OnboardingPreferenceStore`.

## `:core:navigation` — new

```kotlin
@Serializable
data object OnboardingRoute : GameNavKey
```

## `:core:ui` — new and changed

```kotlin
// model/PlatformPickerItemUiModel.kt  (moved from feature/settings PlatformUiModel)
@Immutable
data class PlatformPickerItemUiModel(val id: Int, val name: String, val abbreviation: String?, val isSelected: Boolean)

// model/PlatformPickerContentState.kt  (moved from feature/settings OwnedPlatformsContentState)
@Immutable
sealed interface PlatformPickerContentState {
    data object Loading : PlatformPickerContentState
    data object Empty : PlatformPickerContentState
    data object NoSearchResults : PlatformPickerContentState
    data class Success(val platforms: List<PlatformPickerItemUiModel>) : PlatformPickerContentState
}

// mapper/PlatformPickerMapper.kt  (moved from feature/settings OwnedPlatformsUiMapper)
fun List<Platform>.toPlatformPickerContentState(
    selectedIds: Set<Int>,
    query: String,
    pinnedIds: Set<Int>?
): PlatformPickerContentState

// component/PlatformRow.kt, component/PlatformSearchField.kt  (moved as-is)

// component/PlatformPickerList.kt  (new: the four-state body formerly inlined in OwnedPlatformsContent)
@Composable
fun PlatformPickerList(
    state: PlatformPickerContentState,
    onToggle: (platformId: Int) -> Unit,
    onClearQuery: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    /** Emitted as the first item of the list in `Success`, so it scrolls away with it. Other states: the caller draws it. */
    header: (@Composable () -> Unit)? = null
)

// util/NotificationPermission.kt — backwards-compatible change
@Composable
fun rememberNotificationPermissionState(
    onResult: (granted: Boolean) -> Unit = {}   // new; invoked from the launcher callback
): NotificationPermissionState
```

The strings the moved components render move to `core/ui/src/main/res/values/strings.xml`. Callers in the
feature modules read them as `CoreUiR`.

## `:feature:settings` — changed

- `OwnedPlatformsViewModel` swaps `SetOwnedPlatformsUseCase` and its mutex for
  `ToggleOwnedPlatformUseCase`, and builds its state with `toPlatformPickerContentState`.
  `OwnedPlatformsUiState.contentState` becomes a `PlatformPickerContentState`.
- `OwnedPlatformsContent` keeps its `Scaffold`, `TopAppBar` and caption, and renders `PlatformSearchField`
  and `PlatformPickerList` from `:core:ui`.
- The `components/PlatformRow.kt`, `components/PlatformSearchField.kt`, `model/PlatformUiModel.kt`,
  `model/OwnedPlatformsContentState.kt` and `mapper/OwnedPlatformsUiMapper.kt` files are deleted, since
  they moved to `:core:ui`.
- `SettingsScreen` gains `onShowWelcomeTourClick: () -> Unit` and a "Show welcome tour" row in the App
  group.

## `:feature:onboarding` — new module

```kotlin
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    getKnownPlatformsUseCase: GetKnownPlatformsUseCase,
    getSelectedPlatformIdsUseCase: GetSelectedPlatformIdsUseCase,
    toggleOwnedPlatformUseCase: ToggleOwnedPlatformUseCase,
    syncPlatformCatalogUseCase: SyncPlatformCatalogUseCase,
    completeOnboardingUseCase: CompleteOnboardingUseCase
) : ViewModel()

@Suppress("ParamsComparedByRef")
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
)
```

Dependencies are only `:core:common`, `:core:model`, `:core:domain`, `:core:ui`, `:core:navigation` and
`:core:designsystem` (Principle I).

## `:app` — changed

- `settings.gradle.kts`: `include(":feature:onboarding")`. `app/build.gradle.kts`:
  `implementation(project(":feature:onboarding"))`.
- `QuestLogNavDisplay` gets an `OnboardingRoute` branch:

  ```kotlin
  is OnboardingRoute -> NavEntry(key) {
      OnboardingScreen(
          viewModel = hiltViewModel<OnboardingViewModel>(),
          onFinish = {
              if (backStack.size == 1) { backStack.add(SearchRoute); backStack.removeAt(0) }
              else backStack.removeLastOrNull()
          }
      )
  }
  ```

- The `SettingsRoute` branch passes `onShowWelcomeTourClick`, which pushes `OnboardingRoute` guarded by
  `lastOrNull() != OnboardingRoute`.
- `MainActivity` and `MainContent`:
  - read the flag once and hold the first draw until it is known (research R5);
  - start the back stack at `OnboardingRoute` or `SearchRoute`;
  - ignore the deep link while the root is `OnboardingRoute`.
