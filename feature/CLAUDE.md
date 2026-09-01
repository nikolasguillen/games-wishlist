# CLAUDE.md — feature modules

Applies to `feature/search`, `feature/game-detail`, `feature/lists`, `feature/wishlist`, `feature/settings`.
Read the root `CLAUDE.md` first for the module dependency rules.

## Module layout

```
feature/<name>/src/main/java/com/example/gameswishlist/feature/<name>/
  <Name>Screen.kt         public screen + internal stateless content + private @Preview
  <Name>ViewModel.kt
  components/             sub-composables, one logical unit per file
  mapper/                 internal top-level extension functions
  model/                  UiState, UiEvent, UiEffect, ContentState, *UiModel — one type per file
  res/values/strings.xml  per-feature strings
```

The directory `feature/game-detail` maps to the package `feature.gamedetail` (no hyphen). Feature build
files are copy-paste identical — copy `feature/search/build.gradle.kts` when adding a module.

## Screen structure

Split every screen in two. The public composable takes the ViewModel, collects state, and handles effects;
it delegates to a stateless content composable that takes the state and `onEvent`:

```kotlin
@Suppress("ParamsComparedByRef")
@Composable
fun WishlistScreen(viewModel: WishlistViewModel, onGameClick: (Int) -> Unit, modifier: Modifier = Modifier)

@Composable
internal fun WishlistContent(state: WishlistUiState, onEvent: (WishlistUiEvent) -> Unit, modifier: Modifier = Modifier)
```

`modifier: Modifier = Modifier` is always the trailing parameter. The VM-taking overload carries
`@Suppress("ParamsComparedByRef")` — see `feature/wishlist/WishlistScreen.kt:42`.

Extract sub-composables into `components/` as soon as they form a logical unit. Do not let a screen file
grow into a "God UI" file. Give every new composable a `@Preview` (multiple ones — Loading, Success,
Empty, Error — when the state is non-trivial), `private`, wrapped in `GamesWishlistTheme { }`.

## State, events, effects

- **State**: `@Immutable internal data class <Name>UiState(...)` with defaults for every field.
- **Content lifecycle**: a `sealed interface <Name>ContentState` nested in the state (`Loading`, `Success`,
  `Empty`, `Error`) so `when` stays exhaustive and smart-casts. **One per independent content area, not
  one per screen.** Search has two — `SearchContentState` for the results and `DiscoverContentState` for
  the feed — because the feed keeps loading and refreshing while results hold the screen and has to
  still be there when the query is cleared. Sharing one slot forces a shadow copy of whichever is not
  displayed plus a flag saying which that is; both disappear once each area owns its state. Which one
  renders is *derived*, never stored: `SearchContentState.Idle` means "no search running", so the feed
  is what the content area shows.
- **Events**: a `sealed interface <Name>UiEvent` plus a single `internal fun onEvent(event)` with an
  exhaustive `when`. The UI passes `viewModel::onEvent` — never one callback per interaction.
- **One-shot effects**: `Channel<<Name>UiEffect>(Channel.BUFFERED)` + `receiveAsFlow()`, consumed with
  `LaunchedEffect { lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { ... } }`.
  Reference: `feature/wishlist/WishlistScreen.kt`.

`@Immutable` is not only for `UiState`: every `ContentState`, every bottom-sheet or dialog `*State` and
every `*UiModel` carries it too, on the `sealed` interface rather than on its implementations when the
type is a hierarchy. Most of them hold a `List`, which the compiler infers as unstable, and that
instability propagates to whatever composable receives them. **`UiEvent` and `UiEffect` are the exception**
— they travel as lambda parameters, where stability changes nothing. The annotation is a promise, not a
hint: if a property can change under the same instance, Compose will skip a recomposition that should have
run, so check before adding it.

Two state-pipeline shapes coexist; both are fine, pick the one that matches the source:

- Derived from a use-case `Flow` → `.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Default())`
  (wishlist, lists, game-detail).
- Locally driven → `MutableStateFlow` + `_uiState.update { }` (search, which also owns a `TextFieldState`
  and debounces via `snapshotFlow` + `collectLatest` + `delay`).

## ViewModel injection

This is the easiest thing to get wrong.

**Route without arguments** → standard Hilt:

```kotlin
@HiltViewModel
class SearchViewModel @Inject constructor(...) : ViewModel()
```

**Route with arguments** → assisted injection, *not* `SavedStateHandle`:

```kotlin
@HiltViewModel(assistedFactory = WishlistViewModel.Factory::class)
class WishlistViewModel @AssistedInject constructor(
    @Assisted private val listId: Long,
    getWishlistDetailUseCase: GetWishlistDetailUseCase,
    ...
) : ViewModel() {
    @AssistedFactory
    interface Factory { fun create(listId: Long): WishlistViewModel }
}
```

**`SavedStateHandle` is not used anywhere in this project — do not introduce it.**
Reference: `feature/wishlist/WishlistViewModel.kt`.

## Navigation

Screens never navigate. They receive lambdas (`onGameClick: (Int) -> Unit`, `onBack: () -> Unit`).
There are no per-feature DI modules and no per-feature nav entry providers.

Adding a route means two edits, both outside the feature module:

1. A `@Serializable` `data object`/`data class` implementing `GameNavKey : NavKey` in
   `core/navigation/Routes.kt`.
2. A branch in the single `entryProvider` inside `app/.../MainActivity.kt`, obtaining the ViewModel with
   `hiltViewModel<X>()` or, for assisted VMs,
   `hiltViewModel<X, X.Factory>(creationCallback = { it.create(key.someId) })`.

Navigation is plain backstack mutation (`backStack.add(route)` / `backStack.removeLastOrNull()`), always
guarded by `if (backStack.lastOrNull() != nextRoute)`.

## Resources and visibility

- Each feature owns its `strings.xml`, imported as a bare `R`. Cross-module resources are always imported
  with an alias: `import com.example.gameswishlist.core.ui.R as CoreUiR`.
- Default to `internal` for anything local to the feature: UiState, UiEvent, mappers, sub-composables,
  `uiState`, `onEvent`. Only the screen entry point and the ViewModel class need to be public.

## Tests

`src/test/`, JUnit4 + MockK + `kotlinx-coroutines-test`. Mock the use cases directly, never the repository.
Standard setup: `StandardTestDispatcher`, `Dispatchers.setMain(...)` in `@Before` / `resetMain()` in
`@After`, `advanceUntilIdle()` after triggering an event. Give each test class a KDoc header explaining
what it covers. Reference: `feature/search/src/test/.../SearchViewModelTest.kt`.
