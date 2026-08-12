# CLAUDE.md — core:ui

Shared Compose components, UI models, mappers and utilities. Everything here is reusable across features —
**check this inventory before hand-rolling anything.**

## Components (`component/`)

Shared wrappers are prefixed `Custom*`:
`CustomAlertDialog` (3 overloads — always use it instead of Material's `AlertDialog`), `CustomContentCard`,
`CustomFab`, `CustomFilterChip`, `CustomInfoChip`, `CustomModalBottomSheet`, `CustomSegmentedButton`.

Screen-level and domain components:
`EmptyPage`, `ErrorPage`, `LoadingPage`, `ControllerLoadingAnimation`, `GameCard`, `VerticalGameCard`
(also hosts `RecentGameCard` and `GameCompactCard`), `RatingBadge`, `ImageGalleryPager` (+
`CustomPagerIndicator`), `FullScreenImageViewer`, `ImmersiveDetailLayout`, `StatusBarProtection`.

Every component file ends with a `private fun XPreview()` annotated `@Preview(showBackground = true)` and
wrapped in `GamesWishlistTheme { }`. Match that when adding a component.

## Utilities (`util/`)

**`Modifiers.kt` already provides `Modifier.fadingEdge(...)`, `Modifier.dashedBorder(...)` and
`Modifier.shimmerEffect()`.** Look here before implementing a visual effect by hand — a gradient fade, a
dashed outline or a loading shimmer is already solved.

Also: `ColorUtils.kt`, `HtmlUtils.kt`, `MetallicEffects.kt`, `PlatformVisuals.kt`, and `Constants.kt`
(`object UiConstants` — IGDB platform category/family ids, `MAX_PLATFORM_NAME_LENGTH`,
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
