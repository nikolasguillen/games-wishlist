# Contract: UI-layer additions

**Feature**: `004-light-dark-theme` | **Date**: 2026-09-30

## `core/designsystem`

### `QuestLogTheme` — changed signature

```kotlin
@Composable
fun QuestLogTheme(darkTheme: Boolean = true, content: @Composable () -> Unit)
```

- `darkTheme = true` is the default so every existing `@Preview { QuestLogTheme { ... } }` call site keeps
  compiling and rendering exactly as before without being touched (research.md §4).
- Internally branches between `darkColorScheme(...)` (today's tokens, unchanged) and a new
  `lightColorScheme(...)` built from new `*Light` constants in `Color.kt`, and between today's `appColors`
  and a new light `AppColors` instance — same field set, light-appropriate values.
- Owns the reactive system-bar icon color update: a `SideEffect` sets
  `WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars` and
  `isAppearanceLightNavigationBars` to `!darkTheme` on every recomposition where `darkTheme` changes
  (research.md §5). Guarded so it no-ops when `LocalView.current`'s context isn't an `Activity` (e.g. some
  preview/host environments).

### `Color.kt` — new tokens

New `*Light` counterparts for every `*Dark` constant currently feeding `darkColorScheme(...)` in
`QuestLogTheme.kt` (`PrimaryLight`, `OnPrimaryLight`, `PrimaryContainerLight`, ... `OutlineVariantLight`),
plus light counterparts for the `Neutral*` ramp used by `AppColors`. `Gold`/`GoldDeep`/`GoldMuted`/
`GoldBright` themselves are reused as-is (same hue, per FR-004) — only where each sits in the scheme (text
vs. container fill) may differ between variants. Exact values: research.md §11.

## `core/ui`

### `mapper/AppearanceModeMapper.kt` (new)

```kotlin
fun AppearanceMode.toLabelUiText(): UiText
```

Same shape as `GameStatus.toLabelUiText()`. Three new strings in `core/ui`'s own `strings.xml`
(`appearance_light`, `appearance_dark`, `appearance_system`).

## `feature/settings`

### `model/SettingsUiState.kt` — new field

```kotlin
val appearanceMode: AppearanceMode = AppearanceMode.SYSTEM
```

### `model/SettingsUiEvent.kt` — new event

```kotlin
data class AppearanceModeChanged(val mode: AppearanceMode) : SettingsUiEvent
```

### `SettingsViewModel.kt` — additions

- Injects `GetAppearanceModeUseCase` and `SetAppearanceModeUseCase`.
- Folds `getAppearanceModeUseCase()` into `_uiState` from `init`, the same "locally driven +
  continuously-observed source" shape `SearchViewModel.observeDiscoverFeed` and
  `GameDetailViewModel.observeContentState` already use — collected for the ViewModel's whole life, not
  gated behind `WhileSubscribed`.
- `onEvent` gains a branch: `is SettingsUiEvent.AppearanceModeChanged ->
  viewModelScope.launch { setAppearanceModeUseCase(event.mode) }`.

### `SettingsScreen.kt` — new group

A new `SettingsGroup` (title: new string `settings_group_appearance`) containing a `CustomSegmentedButton`
over the three `AppearanceMode` values, `label = { Text(it.toLabelUiText().asString()) }`,
`selectedIndex` derived from `state.appearanceMode`, `onOptionSelected` dispatching
`SettingsUiEvent.AppearanceModeChanged`.

## `:app` (new)

### `AppThemeViewModel` (new)

```kotlin
@HiltViewModel
class AppThemeViewModel @Inject constructor(
    getAppearanceModeUseCase: GetAppearanceModeUseCase
) : ViewModel() {
    val appearanceMode: StateFlow<AppearanceMode> = getAppearanceModeUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppearanceMode.SYSTEM)
}
```

`WhileSubscribed(5000)` is safe here (unlike the always-alive shape `SettingsViewModel` needs for its
notification/discover sources) because this VM has exactly one collector — the root composition — which is
never unsubscribed for longer than a configuration change.

### `MainActivity.kt` — `setContent` change

```kotlin
setContent {
    val appThemeViewModel: AppThemeViewModel = hiltViewModel()
    val appearanceMode by appThemeViewModel.appearanceMode.collectAsStateWithLifecycle()
    val darkTheme = when (appearanceMode) {
        AppearanceMode.LIGHT -> false
        AppearanceMode.DARK -> true
        AppearanceMode.SYSTEM -> isSystemInDarkTheme()
    }
    QuestLogTheme(darkTheme = darkTheme) {
        MainContent(...)
    }
}
```

The pre-existing `enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(TRANSPARENT))` call in `onCreate`
stays (it configures edge-to-edge layout, not the reactive icon color, which now lives in `QuestLogTheme`
per research.md §5) but the hardcoded `.dark(...)` style is no longer the only word on icon color — the
`SideEffect` inside `QuestLogTheme` corrects it on the very first composition and every one after.
