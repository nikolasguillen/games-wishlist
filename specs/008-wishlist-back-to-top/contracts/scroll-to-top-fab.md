# Contract: `ScrollToTopFab` and its two callers

The project exposes no external interface for this feature. The contract that matters is the shared
composable and how Search and the wishlist screen use it. Behaviour is defined in [../spec.md](../spec.md),
decisions in [../research.md](../research.md).

## The component: `:core:ui`, `component/ScrollToTopFab.kt`

```kotlin
@Composable
fun ScrollToTopFab(
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
)
```

| Aspect | Contract |
|--------|----------|
| Look | exactly what Search draws today: `CustomFab` with a `Icons.Default.KeyboardArrowUp` icon |
| Animation | `Modifier.animateFloatingActionButton(visible = visible, alignment = Alignment.Center)`, applied inside, with the caller's `modifier` first |
| Label | `stringResource(R.string.scroll_to_top_content_description)`, from `:core:ui`'s own resources |
| Touch target | the `FloatingActionButton` default (56dp), above the 48dp minimum |
| Logic | none: it knows nothing about scroll state, a list or a ViewModel; the caller decides `visible` and what `onClick` does |
| Opt-in | Search opts in to `ExperimentalMaterial3Api` at file level today. Add the same opt-in on the composable only if the compiler asks for it for `animateFloatingActionButton` |
| Preview | a private `@QuestLogPreviews` preview in `QuestLogTheme { }`, as every component file has |

## Shared values

`ScrollToTopFabDefaults.ContentBottomPadding` (`@Composable` getter, `MaterialTheme.spacing.doubleLarge * 3`,
96dp) is the bottom content padding for any list under the button, so the last item is never left covered.
It is constant and independent of whether the button is showing. Declared in the same file as the
composable, like `MainScreenHeaderDefaults`.

`UiConstants.SCROLL_TO_TOP_AFTER_ITEM_INDEX = 1` in `core/ui/util/Constants.kt`. Callers show the button
when their first visible item index is greater than it.

## Caller 1: Search (behaviour unchanged)

`SearchScreen.kt` replaces its inline `CustomFab { Icon(...) }` block with:

```kotlin
floatingActionButton = {
    ScrollToTopFab(
        visible = showScrollToTop,
        onClick = { scope.launch { onScrollToTop() } }
    )
}
```

and its two `firstVisibleItemIndex > 1` tests use `UiConstants.SCROLL_TO_TOP_AFTER_ITEM_INDEX` instead of
the literal. Search keeps its own `onScrollToTop` (it also resets a collapsing top bar). Its
`scroll_to_top_content_description` string is deleted, and the imports the block used (`CustomFab`,
`Icons...KeyboardArrowUp`, `Icon`, `animateFloatingActionButton`, `Alignment` if no longer used) go with it.

## Caller 2: the wishlist screen (new)

In `WishlistScreen.kt`, inside `WishlistContent`, using the `gridState` already hoisted there:

```kotlin
val scope = rememberCoroutineScope()
val showScrollToTop by remember {
    derivedStateOf { gridState.firstVisibleItemIndex > UiConstants.SCROLL_TO_TOP_AFTER_ITEM_INDEX }
}
...
Scaffold(
    ...
    floatingActionButton = {
        ScrollToTopFab(
            visible = state.contentState is WishlistContentState.Success && showScrollToTop,
            onClick = { scope.launch { gridState.animateScrollToItem(0) } }
        )
    },
    ...
)
```

| Behaviour | Where it comes from |
|-----------|---------------------|
| Hidden for `Loading`, `Empty`, `FilteredEmpty` | the `contentState is Success` test |
| Snackbar and button do not overlap | `Scaffold` places the snackbar host above the FAB |
| System bars cleared | `Scaffold` with the existing `contentWindowInsets = WindowInsets.systemBars` |
| Not behind a dialog or sheet | they are modal; the button is under their scrim |
| Same behaviour in list and grid | one `LazyVerticalGrid`, one state |

### The grid: last card not covered

`WishlistGamesList` passes `contentPadding = PaddingValues(bottom = ScrollToTopFabDefaults.ContentBottomPadding)`
to its `LazyVerticalGrid`.

## Search's lists get the same clearance

| List | File | Bottom content padding |
|------|------|------------------------|
| Results grid | `feature/search/.../components/SearchResultGrid.kt` | `ScrollToTopFabDefaults.ContentBottomPadding` (was 8dp) |
| Discover feed | `feature/search/.../components/DiscoverFeed.kt` | `ScrollToTopFabDefaults.ContentBottomPadding`; top stays `spacing.large` (was 16dp vertical) |

## Strings

| Resource | Module | Text |
|----------|--------|------|
| `scroll_to_top_content_description` | `:core:ui` (moved from `:feature:search`) | "Scroll to top" |

No new text. Only `ScrollToTopFab` reads the string, from inside `:core:ui`, so no feature module needs a
`CoreUiR` import for it.
