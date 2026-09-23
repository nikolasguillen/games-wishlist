# CLAUDE.md — core:ui

Shared Compose components, UI models, mappers and utilities. Everything here is reusable across features —
**check this inventory before hand-rolling anything.**

## Components (`component/`)

Shared wrappers are prefixed `Custom*`:
`CustomAlertDialog` (3 overloads — always use it instead of Material's `AlertDialog`), `CustomContentCard`,
`CustomFab`, `CustomFilterChip`, `CustomInfoChip`, `CustomModalBottomSheet`, `CustomSegmentedButton`.

Screen-level and domain components:
`EmptyPage`, `ErrorPage`, `LoadingPage`, `ControllerLoadingAnimation`, `RatingBadge`, `ImageGalleryPager` (+
`CustomPagerIndicator`), `FullScreenImageViewer`, `ImmersiveDetailLayout`, `StatusBarProtection`,
`ProfileIconButton` (entry point to Settings, shared by every top-level screen), `MainScreenHeader` (2
overloads — the title one for a plain heading, the slot one for a search bar or anything else; fixes the
header height and the `ProfileIconButton` slot so top-level screens line up, see
`MainScreenHeaderDefaults.Height`; the slot overload also exposes an optional `leadingContent` to the left
of the main slot — always laid out, width-animated, empty by default — for a caller that needs a
conditional icon there, such as Search's back-to-feed arrow), `ListSelectorSheet` (bottom sheet for adding
a game to one or more lists, working against `ListSelectorItemUiModel`), `GameListRow` (cover + title +
optional subtitle + a `trailingContent` slot for whatever the caller puts at the row's end — Radar's
date/platform pair, Wishlist's rating; check here before hand-rolling another game list row).

`component/gamecard/` groups the game-card family: `VerticalGameCard`, `RecentGameCard`, `GameCompactCard`,
and the `internal` `GameCoverHeader` they all share; `MiniGameCard` (48x48 bordered cover, used in list
rows) and the bare `GameCoverImage` it wraps (placeholder/crop/clip with no fixed size or chrome — reach
for this directly when a row needs a different aspect ratio, such as the portrait suggestion cover in
Search). Card-specific private helpers (`SaveToWishlistButton`, `GameMetadataRow`) stay in the file of the
single card that uses them.

Every component file ends with a `private fun XPreview()` annotated `@Preview(showBackground = true)` and
wrapped in `GamesWishlistTheme { }`. Match that when adding a component.

## Utilities (`util/`)

**`util/modifiers/` already provides a set of visual effect modifiers** — check here before implementing
one by hand:

- `VisualModifiers.kt` — `Modifier.fadingEdge(...)` (gradient fade for scrollable edges),
  `Modifier.dashedBorder(...)`.
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
`RECENT_GENERATION_THRESHOLD`). Shared UI constants belong in `UiConstants`, not inline in a composable.

## UiText (`model/UiText.kt`)

The localization abstraction that keeps ViewModels free of `Context`. `@Immutable sealed class UiText` with
`DynamicString`, `StringResource(@StringRes resId, vararg args)` and `CompoundString(texts, separator)`.
Two resolvers: `@Composable asString()` and `asString(context: Context)`.

`StringResource` has **hand-written `equals`/`hashCode`/`toString`** because of the `vararg` array — array
identity would break state comparison. If you add a case, preserve that discipline.

## Mappers (`mapper/`)

Extension functions only, no classes. `ErrorMapper.kt` (`RepositoryError.toUiText()`) is the **single**
error-to-text boundary in the app — route all error rendering through it. Also `GameUiMapper.kt`
(`List<Game>.toGameItemList()`, `Game.toGameItem()`, `Platform.getShortLabel()`,
`GameStatus.toLabelUiText()`), `GameTypeMapper.kt`, `WishlistIconMapper.kt`.

## Build notes

`build.gradle.kts` exposes material-icons and `haze` with `api(...)`, so feature modules get them
transitively — do not re-declare them downstream. The module sets
`freeCompilerArgs = listOf("-XXLanguage:+PropertyParamAnnotationDefaultTargetMode")`, which differs from the
flag used by `core/network`.
