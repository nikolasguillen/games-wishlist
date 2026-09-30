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

## `:app`

No new ViewModel. `GetAppearanceModeUseCase` is a zero-logic Flow pass-through, and `MainActivity` is
already `@AndroidEntryPoint` — a `ViewModel` whose only job is `Flow.stateIn(viewModelScope, ...)` earns its
keep by surviving configuration changes or combining sources, neither of which applies to a cheap DataStore
re-read with no in-flight state to lose. Hilt field injection directly into the `Activity` is simpler and
was rejected only because it's a "first" for `:app` — replaced by an equally-new-pattern `ViewModel` in the
original draft of this plan, which turned out to be the same tradeoff without the simplicity. See
research.md §3 (updated).

### `MainActivity.kt` — additions

```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var getAppearanceModeUseCase: GetAppearanceModeUseCase

    // ...

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        pendingDeepLinkGameId = intent.toGameDeepLinkId()
        setContent {
            val appearanceMode by getAppearanceModeUseCase()
                .collectAsStateWithLifecycle(initialValue = AppearanceMode.SYSTEM)
            val darkTheme = when (appearanceMode) {
                AppearanceMode.LIGHT -> false
                AppearanceMode.DARK -> true
                AppearanceMode.SYSTEM -> isSystemInDarkTheme()
            }
            QuestLogTheme(darkTheme = darkTheme) {
                MainContent(
                    pendingDeepLinkGameId = pendingDeepLinkGameId,
                    onDeepLinkConsumed = { pendingDeepLinkGameId = null }
                )
            }
        }
    }
}
```

The pre-existing `enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(TRANSPARENT))` call in `onCreate`
stays (it configures edge-to-edge layout, not the reactive icon color, which now lives in `QuestLogTheme`
per research.md §5) but the hardcoded `.dark(...)` style is no longer the only word on icon color — the
`SideEffect` inside `QuestLogTheme` corrects it on the very first composition and every one after.
