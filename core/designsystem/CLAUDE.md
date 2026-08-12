# CLAUDE.md — core:designsystem

Theme tokens only, six files under `theme/`. No composables beyond `GamesWishlistTheme`.

- **Dark theme only.** `Theme.kt` exposes a single `darkColorScheme`. There is no light scheme and no
  dynamic color. Do not assume light-mode support or add `isSystemInDarkTheme()` branches.
- **`MaterialTheme.spacing`** (`Spacing.kt`) — `default 0`, `extraSmall 2`, `small 4`, `smallMedium 6`,
  `medium 8`, `mediumLarge 12`, `large 16`, `extraLarge 24`, `doubleLarge 32` dp.
  Need a value that is not there? Add a token here rather than hardcoding a new `dp` in a composable.
- **`MaterialTheme.appColors`** (`AppColors.kt`) — 17 semantic color slots provided through
  `LocalAppColors`, exposed as a `@Composable @ReadOnlyComposable` extension on `MaterialTheme`.
- Raw palette constants live in `Color.kt`, prebuilt Material 3 `*Colors` objects in
  `AppComponentsColors.kt`, and typography in `Type.kt` (`AppTypography`).
- Every theme holder is an `@Immutable data class` provided via `staticCompositionLocalOf`. Keep that
  pattern when adding a new token group, and provide it inside `GamesWishlistTheme`.
