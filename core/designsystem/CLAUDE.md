# CLAUDE.md — core:designsystem

Theme tokens only, seven files under `theme/`. No composables beyond `QuestLogTheme`.

- **`@QuestLogPreviews`** (`QuestLogPreviews.kt`) — multipreview annotation (`@PreviewLightDark` under the
  hood) that renders a preview in both light and dark theme. Use it instead of `@Preview` on every preview
  composable; wrap the content in a bare `QuestLogTheme { }` so it resolves from the `uiMode` the
  annotation sets. Do not pass `darkTheme` in previews.

- **`QuestLogTheme(darkTheme: Boolean = isSystemInDarkTheme(), content)`** branches between
  `darkColorScheme` and `lightColorScheme`, plus a matching light/dark `AppColors` instance. There is no
  dynamic color. The default follows the system, which is what previews rely on. The app itself always
  passes `darkTheme` explicitly, because the manual-vs-system `AppearanceMode` resolution happens in the
  caller; see `AppearanceMode` in `:core:model` and `MainActivity`'s `setContent`.
- **`MaterialTheme.spacing`** (`Spacing.kt`) — `default 0`, `extraSmall 2`, `small 4`, `smallMedium 6`,
  `medium 8`, `mediumLarge 12`, `large 16`, `extraLarge 24`, `doubleLarge 32` dp.
  Need a value that is not there? Add a token here rather than hardcoding a new `dp` in a composable.
- **`MaterialTheme.appColors`** (`AppColors.kt`) — 17 semantic color slots provided through
  `LocalAppColors`, exposed as a `@Composable @ReadOnlyComposable` extension on `MaterialTheme`.
- **`MaterialTheme.isDarkTheme`** — the resolved `darkTheme` flag `QuestLogTheme` was composed with.
  Read this rather than re-deriving light/dark further down the tree (e.g. from a color's luminance
  or by re-reading the `AppearanceMode` preference, which alone doesn't resolve "follow system").
- Raw palette constants live in `Color.kt`, prebuilt Material 3 `*Colors` objects in
  `AppComponentsColors.kt`, and typography in `Type.kt` (`AppTypography`).
- Every theme holder is an `@Immutable data class` provided via `staticCompositionLocalOf`. Keep that
  pattern when adding a new token group, and provide it inside `QuestLogTheme`.
