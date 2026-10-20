# Contract: Screens, Navigation and Shared UI

## Routes (`:core:navigation`)

```kotlin
@Serializable data class CharacterDetailRoute(val characterId: Int) : GameNavKey
@Serializable data class DeveloperDetailRoute(val developerId: Int) : GameNavKey
```

Each gets a `subclass(...)` line in `GameNavSavedStateConfiguration` and a round trip in `RoutesSerializationTest`.

## `:shared` wiring (`QuestLogNavDisplay`)

```kotlin
is SearchRoute -> SearchScreen(
    viewModel = vm,
    onGameClick = { openSearchDetail(backStack, it, twoPaneEnabled) },              // unchanged
    onCharacterClick = { id -> push(CharacterDetailRoute(id)) },                   // new
    onDeveloperClick = { id -> push(DeveloperDetailRoute(id)) },                   // new
    onProfileClick = …                                                             // unchanged
)

is CharacterDetailRoute -> EntityDetailScreen(
    viewModel = koinViewModel<EntityDetailViewModel> { parametersOf(EntityDetailSubject.Character(key.characterId)) },
    onGameClick = { gameId -> push(GameDetailRoute(gameId)) },                     // full screen, not fromSearch
    onBackClick = { navigateBack(backStack, twoPaneEnabled) }
)
is DeveloperDetailRoute -> // same, with EntityDetailSubject.Developer(key.developerId)
```

`push(route)` stands for the codebase's guarded `if (backStack.lastOrNull() != route) backStack.add(route)`, written
inline as in every other branch.

- `ViewModelModule`: `viewModelOf(::EntityDetailViewModel)`.
- `KoinGraphTest`: `EntityDetailSubject::class` is added to `extraTypes`.
- **Unchanged**: `QuestLogRoot`, `TopLevelDestination`, `SearchDetailPanes` and the scene. The page is not top level,
  so the rail hides, as on a wishlist. A page pushed over a two-pane run covers it, and back returns to the two panes.

## `SearchScreen` (`:feature:search`)

```kotlin
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onGameClick: (Int) -> Unit,
    onCharacterClick: (Int) -> Unit,
    onDeveloperClick: (Int) -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
)
```

What renders where:

- **Chip row** (`components/SearchTypeChipRow.kt`): at the top of `ExpandedSearchBar`'s content, above the existing
  `when`, with a `HorizontalDivider` under it. It is driven by `selectedSearchType` and emits
  `OnSearchTypeSelected`.
- **Hint text**: `SearchInputField`'s placeholder is `browsePlaceholder ?: selectedSearchType.toPlaceholderUiText()`.
- **Suggestions**: `SuggestionsSection` is unchanged. With the remote lookup gated in the view model, its "Games"
  header and rows are simply absent for the other types, and the history rows and "See all results" remain.
- **Results**: `SearchMainContent` renders `EntityResults` with `components/EntityResultGrid.kt`. It uses the same
  `resultGridColumnCount` and `SearchResultGridCells` as the game grid, with `CharacterResultCard` or
  `DeveloperResultCard` chosen by `resultsType`. A tap calls `onCharacterClick` or `onDeveloperClick`, and the search
  bar collapses with the same delay as a game tap.
- **Sub-header**: `SearchSubHeader` gains nullable `onOpenSort`/`onOpenFilters`. With `null` the buttons are hidden.
  For `EntityResults` the count reads "%1$d characters found" or "%1$d developers found".
- **Empty state**: the message is chosen by `resultsType`: `search_no_results` (games, unchanged),
  `search_no_characters` or `search_no_developers`.

## Result cards (`:feature:search/components`)

| Composable | Look | Semantics |
|---|---|---|
| `CharacterPortrait(imageUrl, modifier)` | the image cropped into `CircleShape`, or the `Person` placeholder | decorative (`contentDescription = null`) |
| `DeveloperLogo(imageUrl, modifier)` | a `shapes.large` tile in `appColors.logoTileContainerColor`, logo `ContentScale.Fit` with `spacing.medium` inset, or the `Business` placeholder | decorative |
| `CharacterResultCard(item, onClick, modifier)` | a square `CharacterPortrait` at card width, the name under it (2 lines, centered) | merged node, content description "Character, %1$s", role Button |
| `DeveloperResultCard(item, onClick, modifier)` | a square `DeveloperLogo` at card width, the name under it | merged node, "Developer, %1$s", role Button |

The portrait and logo are shared by the cards and the detail header. All have `@QuestLogPreviews`, with and without an
image.

## `EntityDetailScreen` (`:feature:search`)

```kotlin
@Suppress("ParamsComparedByRef")
@Composable
fun EntityDetailScreen(
    viewModel: EntityDetailViewModel,
    onGameClick: (Int) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
)

@Composable
internal fun EntityDetailContent(
    state: EntityDetailUiState,
    onEvent: (EntityDetailUiEvent) -> Unit,
    onGameClick: (Int) -> Unit,
    onBackClick: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
)
```

Layout follows the wishlist detail screen:

- A `Scaffold` with a top bar holding a back arrow, a `ScrollToTopFab`, a `SnackbarHost`, and the `ListSelectorSheet`
  while `listSelectorState != null`.
- One `LazyVerticalGrid` with `SearchResultGridCells` (columns from `resultGridColumnCount` on its own space). It holds:
  1. `DetailHeader`, full span: portrait or logo, name (up to 2 lines), and chips (`gameCountText` once the games are
     loaded, and `foundedYearText`).
  2. `DescriptionCard`, full span, only when `description != null`, driven by `descriptionTranslation`.
  3. A "Games" section title, full span.
  4. Games: `VerticalGameCard` per game, with `onSaveClick` and long press to the list selector, as in the search grid.
     While the games load, a full-span `ControllerLoadingAnimation`. When there are none, a full-span `EmptyPage`. On
     failure, a full-span `ErrorPage` with retry. None of these hide items 1–3.
- `profile = Loading` shows a full-screen `LoadingPage`, and `profile = Error` a full-screen `ErrorPage` with retry. In
  both cases there is nothing yet to show above the games.

## Shared UI (`:core:ui`)

```kotlin
@Composable
fun DetailHeader(
    title: String,
    cover: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    titleMaxLines: Int = 1,
    chips: @Composable RowScope.() -> Unit = {}
)
object DetailHeaderDefaults { val CoverWidth: Dp; val CoverHeight: Dp; val PlaceholderIconSize: Dp }

@Composable
fun DescriptionCard(
    description: String,
    translation: DescriptionTranslationState,
    onTranslateClick: () -> Unit,
    onShowOriginalClick: () -> Unit,
    modifier: Modifier = Modifier
)

@Composable
fun CustomFilterChip(
    label: String,
    selected: Boolean,
    onFilterClick: () -> Unit,
    trailingIcon: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null    // new, last so existing callers compile unchanged
)
```

`core/ui/CLAUDE.md`'s inventory gains `DetailHeader`, `DescriptionCard` and the translation badge family, and
`CustomFilterChip`'s leading icon. `WishlistDetailHeader` and `GameDetailScreen` are callers of the moved and
extracted pieces, with no visual change.

## Strings

All English, the app's only language (`values/` only, no `values-*`).

`:feature:search` adds:

| Key | Text |
|---|---|
| `search_type_games` / `_characters` / `_developers` | Games / Characters / Developers |
| `search_placeholder_characters` / `_developers` | Search for a character… / Search for a developer… |
| `search_no_characters` / `search_no_developers` | No characters found for this search / No developers found for this search |
| `search_characters_count` / `search_developers_count` | %1$d characters found / %1$d developers found |
| `character_card_content_description` / `developer_card_content_description` | Character, %1$s / Developer, %1$s |
| `entity_games_title` | Games |
| `entity_no_games` | No games to show yet |
| `entity_founded_year` | Founded %1$d |
| plural `entity_game_count` | %1$d game / %1$d games |

`:core:ui` receives the nine moved translation strings and `ic_gemini` (research R8).

`:feature:game-detail` loses those strings and the drawable.
