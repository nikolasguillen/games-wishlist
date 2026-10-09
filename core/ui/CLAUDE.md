# CLAUDE.md — core:ui

Shared Compose components, UI models, mappers and utilities. Everything here is reusable across features —
**check this inventory before hand-rolling anything.**

## Components (`component/`)

Shared wrappers are prefixed `Custom*`:
`CustomAlertDialog` (3 overloads — always use it instead of Material's `AlertDialog`), `CustomContentCard`,
`CustomFab`, `CustomFilterChip`, `CustomChipRow` (horizontally scrolling chip row with fading edges;
generic over the item, the caller draws each chip and gets a `bringIntoView` callback that scrolls a chip
clear of the fade — call it when the chip stays after a click, skip it when the click removes the chip;
use it for any new chip strip rather than a bare `LazyRow`), `CustomInfoChip`, `CustomModalBottomSheet`, `CustomSegmentedButton`,
`CustomOutlinedIcon` (an `Icon` with an optional border traced from the icon's own path data via
`PathParser`, so it hugs the exact silhouette rather than a bounding shape — pass `outlineColor` to
enable it), `CustomSummaryBadge` (pill-shaped label badge; filled-selected-chip colors, dark/light handled
internally via `MaterialTheme.isDarkTheme` — see `CustomFilterChip` for the same split).

Screen-level and domain components:
`EmptyPage`, `ErrorPage`, `LoadingPage`, `ControllerLoadingAnimation`, `RatingBadge`, `ImageGalleryPager` (+
`CustomPagerIndicator`), `FullScreenImageViewer`, `ImmersiveDetailLayout`, `StatusBarProtection`,
`ProfileIconButton` (entry point to Settings, shared by every top-level screen), `MainScreenHeader` (2
overloads — the title one for a plain heading, the slot one for a search bar or anything else; fixes the
header height and the `ProfileIconButton` slot so top-level screens line up, see
`MainScreenHeaderDefaults.Height`; the slot overload also exposes an optional `leadingContent` to the left
of the main slot — always laid out, width-animated, empty by default — for a caller that needs a
conditional icon there, such as Search's back-to-feed arrow), `ListSelectorSheet` (bottom sheet for adding
a game to one or more lists, working against `ListSelectorItemUiModel`), `WishlistFormSheet` (the form for
creating and editing a wishlist — cover, name, description, icon. It has no mode of its own: the caller
passes the title, the confirm label and a `WishlistFormUiModel` of initial values, and gets one back; Lists
uses it to create, the wishlist detail screen to edit), `GameListRow` (cover + title +
optional subtitle + a `trailingContent` slot for whatever the caller puts at the row's end — Radar's
date/platform pair, Wishlist's rating; check here before hand-rolling another game list row),
`PlatformTile` (32dp colored tile holding a platform's short code — game detail's platform strip, Radar's
per-platform rows; its data comes from `PlatformTileUiModel`), `PlatformRow`, `PlatformSearchField` and
`PlatformPickerList` (the owned-platforms picker shared by Settings and the welcome flow: stateless, driven by
`PlatformPickerContentState` from `toPlatformPickerContentState`, which owns the ordering, search and
entry-pinning rules — each screen supplies its own ViewModel), `ScrollToTopFab` (the floating up-arrow that
returns a long screen to its top, shared by Search and the wishlist: it holds no scroll state, so the caller
owns `visible` and `onClick`, and shows it once its first visible item index is greater than
`UiConstants.SCROLL_TO_TOP_AFTER_ITEM_INDEX`, and gives the list under it
`ScrollToTopFabDefaults.ContentBottomPadding` as bottom content padding so the button never covers the last
item; use it for any new screen that needs one rather than drawing another).

`component/gamecard/` groups the game-card family: `VerticalGameCard`, `RecentGameCard`, `CompactGameCard`,
and the `internal` `GameCoverHeader` they all share; `MiniGameCard` (48x48 bordered cover, used in list
rows) and the bare `GameCoverImage` it wraps (placeholder/crop/clip with no fixed size or chrome — reach
for this directly when a row needs a different aspect ratio, such as the portrait suggestion cover in
Search); `SaveToWishlistButton`, the save toggle both full-size cards draw, also used on its own by
Search's `DiscoverHero`. A helper used by a single card stays private in that card's file —
`GameMetadataRow` in `VerticalGameCard.kt`.

`VerticalGameCard` makes the caller say what its long press does: `onLongClickLabel` is required, with no
default, because the card cannot know whether the press opens the list chooser (Search) or removes the game
(a wishlist grid), and accessibility services announce it. `onSaveClick` is nullable — leave it `null` to
hide the heart where it would mislead: it shows membership of the *default* wishlist, not of the list the
card is shown in.

Every component file ends with a `private fun XPreview()` annotated `@QuestLogPreviews` and wrapped in
`QuestLogTheme { }`. Match that when adding a component.

## Utilities (`util/`)

**`util/modifiers/` already provides a set of visual effect modifiers** — check here before implementing
one by hand:

- `VisualModifiers.kt` — `Modifier.fadingEdge(...)` (vertical gradient fade, caller-driven alphas),
  `Modifier.fadingEdgeHorizontal(state, ...)` (RTL-aware start/end fades of a `LazyRow` that grow as it scrolls),
  `Modifier.dashedBorder(...)`, `rememberCoverBrush()` (the surface gradient behind cover art, painted with
  `Modifier.background(...)` — `GameCoverHeader`, `GameCoverImage` and the wishlist cover all use it).
- `ShimmerModifiers.kt` — `Modifier.shimmerEffect()` for a single shimmering block, and
  `Modifier.lineShimmer(...)` (state via `LineShimmerState.kt`'s `rememberLineShimmerState()`) for a
  skeleton that traces the lines of a `Text` that has already been laid out — prefer it over
  `shimmerEffect()` whenever the shimmer stands in for real text.
- `MetallicModifiers.kt` — `Modifier.brushedMetal(...)`, `Modifier.metallicBorder(...)`,
  `Modifier.metallicBackground(...)`, `Modifier.animatedMetallicBorder(...)` and
  `Modifier.rainbowMetallicBorder(...)`, all wrapping the brushes from `MetallicEffects.kt`.

Also: `ColorUtils.kt`, `HtmlUtils.kt`, `MetallicEffects.kt` (brush factories:
`primaryMetallicGradient()`, `rememberAnimatedMetallicGradient()`, `rainbowMetallicGradient()`),
`PlatformVisuals.kt`, and `Constants.kt` (`object UiConstants` — IGDB platform category/family ids,
`MAX_PLATFORM_NAME_LENGTH`,
`RECENT_GENERATION_THRESHOLD`, `SCROLL_TO_TOP_AFTER_ITEM_INDEX`). Shared UI constants belong in `UiConstants`,
not inline in a composable.

## UiText (`model/UiText.kt`)

The localization abstraction that keeps ViewModels free of any platform handle. `@Immutable sealed class
UiText` with `DynamicString`, `StringResource(res: org.jetbrains.compose.resources.StringResource, vararg args)`,
`PluralResource(res: PluralStringResource, quantity, vararg args)` and `CompoundString(texts, separator)`.
Two resolvers: `@Composable asString()` and `suspend resolve()` for code outside composition, such as an
effect that shows a snackbar. They cannot share a name: a composable and a suspend function with the same
signature are conflicting overloads.

`StringResource` and `PluralResource` have **hand-written `equals`/`hashCode`/`toString`** because of the
`vararg` array — array identity would break state comparison. If you add a case, preserve that discipline.

## Resources and platform code

Strings and drawables live in `src/commonMain/composeResources/` (`values/strings.xml`, `drawable/`), and the
module's `Res` class is public: `com.nikolasguillen.questlog.core.ui.resources.Res`. Compose resources differ
from Android's in ways that fail quietly, so:

- **Every resource used needs its own import** (`import ...core.ui.resources.retry`); `Res.string.retry` alone
  does not resolve.
- **Write quotes plainly** (`won't`, `Delete "%1$s"?`). A backslash-escaped `\'` is printed with its
  backslash. `\n` works.
- **Format arguments are positional only: `%1$s`, `%1$d`.** `%1s` is printed literally.
- **Vector drawables take literal colours** (`#FFFFFFFF`), not `@android:color/...`: the parser throws at the
  first composition. Gradient fills (`aapt:attr`) work.

What cannot be common sits behind `expect` with its Android `actual` in `androidMain`:
`rememberNotificationPermissionState` and `NotificationPermissionDeniedDialog` (the permission and the system
notification settings), `rememberCoverImagePicker` (the image picker; it hands back a source string, a content
URI on Android), `rememberTextSharer` (the share sheet) and `fullScreenDialogProperties`. A common file never
imports `android.*` or `androidx.activity.*`.

## Mappers (`mapper/`)

Extension functions only, no classes. `ErrorMapper.kt` (`RepositoryError.toUiText()`) is the **single**
error-to-text boundary in the app — route all error rendering through it. Also `GameUiMapper.kt`
(`List<Game>.toGameItemList()`, `Game.toGameItem()`, `Platform.getShortLabel()`,
`GameStatus.toLabelUiText()`), `GameTypeMapper.kt`, `WishlistIconMapper.kt`,
`AppearanceModeMapper.kt` (`AppearanceMode.toLabelUiText()`).

## Build notes

`build.gradle.kts` exposes material-icons and `haze` with `api(...)`, so feature modules get them
transitively — do not re-declare them downstream. The module sets
`freeCompilerArgs = listOf("-XXLanguage:+PropertyParamAnnotationDefaultTargetMode")`.

Coil loads over the network only because `:shared` carries `coil-network-ktor3` and the Ktor engine for each
platform (OkHttp, Darwin). A module that shows images depends on `coil-compose` and nothing else; without the
fetcher in the final app every cover silently stays a placeholder.
